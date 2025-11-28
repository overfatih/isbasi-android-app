package com.profplay.isbasi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.profplay.isbasi.data.model.JobWithStatus
import com.profplay.isbasi.data.repository.SupabaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat // Tarih formatlama için
import java.util.Date // Tarih işlemleri için
import java.util.Locale // Bölge ayarları için

class JobListViewModel(
    private val repository: SupabaseRepository
) : ViewModel() {

    // UI state'ini tutacak Flow
    private val _uiState = MutableStateFlow<JobListUiState>(JobListUiState.Idle)
    val uiState: StateFlow<JobListUiState> = _uiState

    // --- Veri Yükleme Fonksiyonları ---
    /** İşveren için: Sadece kendi iş ilanlarını yükler. */
    fun loadJobsForEmployer(employerId: String) {
        Log.d("JobListViewModel", "loadJobsForEmployer çağrıldı (ID: $employerId).")
        viewModelScope.launch {
            _uiState.value = JobListUiState.Loading
            try {
                val jobs = withContext(Dispatchers.IO) {
                    repository.getJobsByEmployerId(employerId)
                }
                val jobsWithStatus = jobs.map { job ->
                    JobWithStatus(job = job, applicationStatus = null)
                }
                _uiState.value = JobListUiState.Success(jobsWithStatus)
                Log.i("JobListViewModel", "loadJobsForEmployer: Başarıyla ${jobs.size} iş yüklendi.")
            } catch (e: Exception) {
                Log.e("JobListViewModel", "loadJobsForEmployer HATA: ${e.message}", e)
                _uiState.value = JobListUiState.Error("İş listesi yüklenemedi: ${e.message}")
            }
        }
    }

    /** İşveren için: Belirli tarih aralığındaki TÜM işleri yükler. */
    fun loadJobsByDateRange(startDateMillis: Long, endDateMillis: Long) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDateStr = dateFormat.format(Date(startDateMillis))
        val endDateStr = dateFormat.format(Date(endDateMillis))

        Log.d("JobListViewModel", "loadJobsByDateRange çağrıldı ($startDateStr - $endDateStr).")
        viewModelScope.launch {
            _uiState.value = JobListUiState.Loading
            try {
                val jobs = withContext(Dispatchers.IO) {
                    repository.getJobsByDateRange(startDateStr, endDateStr)
                }
                val jobsWithStatus = jobs.map { job ->
                    JobWithStatus(job = job, applicationStatus = null)
                }
                _uiState.value = JobListUiState.Success(jobsWithStatus)
                Log.i("JobListViewModel", "loadJobsByDateRange: Başarıyla ${jobs.size} iş yüklendi.")
            } catch (e: Exception) {
                Log.e("JobListViewModel", "loadJobsByDateRange HATA: ${e.message}", e)
                _uiState.value = JobListUiState.Error("İş listesi yüklenemedi: ${e.message}")
            }
        }
    }

    // Hata gösterildikten sonra state'i Idle'a çekmek için (opsiyonel)
    fun resetStateToIdle() {
        _uiState.value = JobListUiState.Idle
    }

    fun applyToJob(jobId: String) {
        viewModelScope.launch {
            // ID'yi ViewModel'de güvenli bir şekilde al
            val currentUserId = repository.currentUserId()
            if (currentUserId == null) {
                _uiState.value = JobListUiState.Error("Kullanıcı oturumu bulunamadı. Lütfen tekrar giriş yapın.")
                return@launch // İşlemi burada sonlandır
            }

            val success = withContext(Dispatchers.IO) {
                repository.applyForJob(jobId, currentUserId) // <-- ID'yi gönder!
            }

            if (success) {
                Log.i("JobListViewModel", "Başvuru başarıyla alındı: $jobId")
                loadAllJobs()
            } else {
                Log.e("JobListViewModel", "Başvuru başarısız oldu: $jobId")
                _uiState.value = JobListUiState.Error("Başvuru yapılamadı. Daha önce başvurmuş olabilirsiniz.")
            }
        }
    }
    fun loadAllJobs() {
        Log.d("JobListViewModel", "loadAllJobs çağrıldı.")
        viewModelScope.launch {
            _uiState.value = JobListUiState.Loading
            try {
                val currentUserId = withContext(Dispatchers.IO) { repository.currentUserId() } ?: run {
                    _uiState.value = JobListUiState.Error("Kullanıcı oturumu bulunamadı.")
                    return@launch
                }

                val (allJobs, applications) = withContext(Dispatchers.IO) {
                    // Eş zamanlı olarak 2 ağ isteği yap (daha hızlı)
                    val allJobsDeferred = async { repository.getAllJobs() }
                    val applicationsDeferred = async { repository.getWorkerApplications(currentUserId) }

                    allJobsDeferred.await() to applicationsDeferred.await()
                }

                // Başvuruları hızlı arama için Map'e çevir: Map<jobId, status>
                val applicationMap = applications.associate { it.jobId to it.status }

                // İşleri statüleriyle birleştir
                val jobsWithStatus = allJobs.map { job ->
                    JobWithStatus(
                        job = job,
                        applicationStatus = applicationMap[job.id] // Eğer Map'te yoksa null döner
                    )
                }

                _uiState.value = JobListUiState.Success(jobsWithStatus) // List<JobWithStatus> gönder
                Log.i("JobListViewModel", "loadAllJobs: ${jobsWithStatus.size} iş yüklendi.")
            } catch (e: Exception) {
                Log.e("JobListViewModel", "loadAllJobs HATA: ${e.message}", e)
                _uiState.value = JobListUiState.Error("İş listesi yüklenemedi: ${e.message}")
            }
        }
    }

    /** İptal butonu için: Başvuruyu iptal eder ve listeyi yeniden yükler. */
    fun cancelApplication(jobId: String) {
        Log.d("JobListViewModel", "cancelApplication çağrıldı (JobID: $jobId).")
        viewModelScope.launch {
            val currentUserId = repository.currentUserId()
            if (currentUserId == null) {
                _uiState.value = JobListUiState.Error("Kullanıcı oturumu bulunamadı. Lütfen tekrar giriş yapın.")
                return@launch // İşlemi burada sonlandır
            }
            val success = withContext(Dispatchers.IO) {
                repository.cancelApplication(jobId, currentUserId) // <-- ID'yi gönder!
            }

            if (success) {
                Log.i("JobListViewModel", "Başvuru başarıyla iptal edildi: $jobId. Liste güncelleniyor...")
                // Başarılıysa, listeyi yenile ki güncel durum (butonun kaybolması/değişmesi) görülsün
                loadAllJobs()
            } else {
                Log.e("JobListViewModel", "Başvuru iptali başarısız oldu: $jobId")
                _uiState.value = JobListUiState.Error("Başvuru iptal edilemedi.")
                // Hata durumunda da listeyi yeniden yükleyebiliriz
                loadAllJobs()
            }
        }
    }
}