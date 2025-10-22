package com.profplay.isbasi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.profplay.isbasi.data.repository.SupabaseRepository
import kotlinx.coroutines.Dispatchers
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

    /** İşçi için: Tüm iş ilanlarını yükler. */
    fun loadAllJobs() {
        Log.d("JobListViewModel", "loadAllJobs çağrıldı.")
        viewModelScope.launch {
            _uiState.value = JobListUiState.Loading
            try {
                val jobs = withContext(Dispatchers.IO) {
                    repository.getAllJobs()
                }
                _uiState.value = JobListUiState.Success(jobs)
                Log.i("JobListViewModel", "loadAllJobs: Başarıyla ${jobs.size} iş yüklendi.")
            } catch (e: Exception) {
                Log.e("JobListViewModel", "loadAllJobs HATA: ${e.message}", e)
                _uiState.value = JobListUiState.Error("İş listesi yüklenemedi: ${e.message}")
            }
        }
    }

    /** İşveren için: Sadece kendi iş ilanlarını yükler. */
    fun loadJobsForEmployer(employerId: String) {
        Log.d("JobListViewModel", "loadJobsForEmployer çağrıldı (ID: $employerId).")
        viewModelScope.launch {
            _uiState.value = JobListUiState.Loading
            try {
                val jobs = withContext(Dispatchers.IO) {
                    repository.getJobsByEmployerId(employerId)
                }
                _uiState.value = JobListUiState.Success(jobs)
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
                _uiState.value = JobListUiState.Success(jobs)
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
}