package com.profplay.isbasi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.profplay.isbasi.data.model.Job
import com.profplay.isbasi.data.model.JobWithStatus
import com.profplay.isbasi.data.repository.SupabaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat // Tarih formatlama için
import java.time.format.DateTimeFormatter
import java.util.Date // Tarih işlemleri için
import java.util.Locale // Bölge ayarları için
import java.time.LocalDate
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
                // 1. Kullanıcı ID'sini al (Hata vermemesi için güvenli çağrı)
                val currentUserId = repository.currentUserId()
                if (currentUserId == null) {
                    _uiState.value = JobListUiState.Error("Kullanıcı oturumu bulunamadı.")
                    return@launch
                }

                // 2. Verileri Paralel Çek (Async/Await)
                // Bu blok bize bir Pair(Çift) döndürecek: (List<Job>, List<Application>)
                val (allJobs, applications) = withContext(Dispatchers.IO) {
                    val allJobsDeferred = async { repository.getAllJobs() }
                    val applicationsDeferred = async { repository.getWorkerApplications(currentUserId) }

                    // İki işlemin de bitmesini bekle ve sonuçları çift olarak döndür
                    allJobsDeferred.await() to applicationsDeferred.await()
                }

                // 3. Başvuruları Map'e çevir (Hızlı erişim için)
                val applicationMap = applications.associate { it.jobId to it.status }

                // 4. Onaylanmış işlerin listesini çıkar (Çakışma kontrolü için)
                // Status'u "approved" olan başvuruların job_id'lerini alıyoruz
                val approvedJobIds = applications
                    .filter { it.status == "approved" }
                    .map { it.jobId }
                    .toSet()

                // Onaylanmış işlerin tam detaylarını bul
                val approvedJobs = allJobs.filter { it.id in approvedJobIds }

                // 5. İş Listesini Oluştur ve Çakışmaları Kontrol Et
                val jobsWithStatus = allJobs.map { job ->
                    val status = applicationMap[job.id]

                    // Çakışma var mı?
                    var isConflict = false

                    // Eğer bu işe zaten onay almadıysak ve beklemede değilsek çakışma kontrolü yap
                    // (Onaylı veya beklemede olan işin kendisiyle çakışması önemli değil)
                    if (status != "approved") {
                        isConflict = approvedJobs.any { approvedJob ->
                            // approvedJob kendisi değilse kontrol et
                            if (job.id != approvedJob.id) {
                                checkOverlap(job, approvedJob)
                            } else false
                        }
                    }

                    JobWithStatus(
                        job = job,
                        applicationStatus = status,
                        hasConflict = isConflict
                    )
                }

                // 6. UI State'i Güncelle
                _uiState.value = JobListUiState.Success(jobsWithStatus)
                Log.i("JobListViewModel", "loadAllJobs: ${jobsWithStatus.size} iş yüklendi.")

            } catch (e: Exception) {
                Log.e("JobListViewModel", "loadAllJobs HATA: ${e.message}", e)
                _uiState.value = JobListUiState.Error("İş listesi yüklenemedi: ${e.message}")
            }
        }
    }

    // Tarih çakışmasını kontrol eden yardımcı fonksiyon
    private fun checkOverlap(job1: Job, job2: Job): Boolean {
        return try {
            val formatter = DateTimeFormatter.ISO_DATE

            // job1 için tarihleri hazırla (Mevcut Listedeki İş)
            val start1 = LocalDate.parse(job1.dateStart, formatter)
            // Eğer dateEnd boşsa veya null ise, bitiş tarihini başlangıç tarihi kabul et (1 günlük iş)
            val end1 = if (!job1.dateEnd.isNullOrBlank()) {
                LocalDate.parse(job1.dateEnd, formatter)
            } else {
                start1
            }

            // job2 için tarihleri hazırla (Onaylanmış İş)
            val start2 = LocalDate.parse(job2.dateStart, formatter)
            val end2 = if (!job2.dateEnd.isNullOrBlank()) {
                LocalDate.parse(job2.dateEnd, formatter)
            } else {
                start2
            }

            // --- ÇAKIŞMA MANTIĞI ---
            // (Start1 <= End2) VE (End1 >= Start2)
            // Kotlin LocalDate ile:
            // !start1.isAfter(end2) && !end1.isBefore(start2)

            val isOverlapping = !start1.isAfter(end2) && !end1.isBefore(start2)

            // Hata ayıklama için log (Logcat'te 'OverlapCheck' araması yap)
            if (isOverlapping) {
                Log.w("OverlapCheck", "ÇAKIŞMA BULUNDU: ${job1.title} ($start1-$end1) <-> ${job2.title} ($start2-$end2)")
            }

            return isOverlapping

        } catch (e: Exception) {
            Log.e("OverlapCheck", "Tarih hatası: ${e.message} (Job1: ${job1.dateStart}-${job1.dateEnd}, Job2: ${job2.dateStart}-${job2.dateEnd})")
            false
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