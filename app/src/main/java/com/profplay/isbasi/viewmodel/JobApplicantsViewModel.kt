package com.profplay.isbasi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.profplay.isbasi.data.model.Application
import com.profplay.isbasi.data.model.ReviewWithReviewer
import com.profplay.isbasi.data.model.User
import com.profplay.isbasi.data.repository.SupabaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.profplay.isbasi.data.model.Job
import com.profplay.isbasi.data.model.Review
import kotlinx.coroutines.async // <-- BU IMPORT ÖNEMLİ

// UI State: Yükleniyor, Liste (Başvuru + İşçi), Hata
sealed interface ApplicantsUiState {
    object Loading : ApplicantsUiState
    data class Success(val applicants: List<Pair<Application, User>>) : ApplicantsUiState
    data class Error(val message: String) : ApplicantsUiState
    object Idle : ApplicantsUiState
}

class JobApplicantsViewModel(private val repository: SupabaseRepository) : ViewModel() {

    // İşçilere yapılan genel yorumlar (Map: UserId -> List<Review>)
    private val _reviewsState = MutableStateFlow<Map<String, List<ReviewWithReviewer>>>(emptyMap())
    val reviewsState: StateFlow<Map<String, List<ReviewWithReviewer>>> = _reviewsState

    // UI Durumu (Başvuru Listesi)
    private val _uiState = MutableStateFlow<ApplicantsUiState>(ApplicantsUiState.Idle)
    val uiState: StateFlow<ApplicantsUiState> = _uiState

    // Seçili İşin Detayları
    private val _currentJob = MutableStateFlow<Job?>(null)
    val currentJob: StateFlow<Job?> = _currentJob

    // Benim (İşverenin) bu işteki işçilere yaptığım yorumlar (Map: WorkerId -> Review)
    private val _myReviewsForJob = MutableStateFlow<Map<String, Review>>(emptyMap())
    val myReviewsForJob: StateFlow<Map<String, Review>> = _myReviewsForJob

    // --- 1. DÜZELTİLEN FONKSİYON: Hem başvuruları hem yorumlarımı çek ---
    fun loadApplicants(jobId: String) {
        viewModelScope.launch {
            _uiState.value = ApplicantsUiState.Loading

            try {
                // 1. Paralel Veri Çekimi Başlat
                // Dispatchers.IO kullanarak arka planda çalıştırıyoruz
                val result = withContext(Dispatchers.IO) {
                    // Başvuru listesini çek
                    val applicantsDeferred = async { repository.getApplicationsForJob(jobId) }

                    // Benim yorumlarımı çek (Mevcut kullanıcı ID'sini alarak)
                    val currentUserId = repository.currentUserId()
                    val myReviewsDeferred = async {
                        if (currentUserId != null) {
                            repository.getReviewsByJobAndReviewer(jobId, currentUserId)
                        } else {
                            emptyList()
                        }
                    }

                    // İkisinin de bitmesini bekle
                    applicantsDeferred.await() to myReviewsDeferred.await()
                }

                val (applicants, myReviews) = result

                // 2. Yorumları Map'e çevir (Hızlı erişim için: WorkerId -> Review)
                // revieweeId burada workerId oluyor
                _myReviewsForJob.value = myReviews.associateBy { it.revieweeId }

                // 3. UI State'i güncelle
                _uiState.value = ApplicantsUiState.Success(applicants)

            } catch (e: Exception) {
                Log.e("JobApplicantsViewModel", "loadApplicants HATA: ${e.message}", e)
                _uiState.value = ApplicantsUiState.Error("Veriler yüklenemedi: ${e.message}")
            }
        }
    }

    fun updateStatus(applicationId: String, newStatus: String, jobId: String) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                repository.updateApplicationStatus(applicationId, newStatus)
            }
            if (success) {
                // Listeyi yenile ki güncel durum görünsün
                loadApplicants(jobId)
            } else {
                // Hata mesajı eklenebilir
            }
        }
    }

    fun loadReviews(userId: String) {
        viewModelScope.launch {
            // Eğer daha önce çekildiyse tekrar çekme (Performans)
            if (_reviewsState.value.containsKey(userId)) return@launch

            val reviews = withContext(Dispatchers.IO) {
                repository.getReviewsForUser(userId)
            }
            // Mevcut haritaya ekle
            _reviewsState.value = _reviewsState.value + (userId to reviews)
        }
    }

    fun loadJobDetails(jobId: String) {
        viewModelScope.launch {
            val job = withContext(Dispatchers.IO) {
                repository.getJobById(jobId)
            }
            _currentJob.value = job
        }
    }

    // --- 2. DÜZELTİLEN FONKSİYON: Puan verince listeyi yenile ---
    fun submitReview(jobId: String, revieweeId: String, score: Int, comment: String) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                repository.submitReview(jobId, revieweeId, score, comment)
            }
            if (success) {
                Log.i("JobApplicantsViewModel", "İşçi puanlandı.")
                val currentMap = _reviewsState.value.toMutableMap()
                currentMap.remove(revieweeId)
                _reviewsState.value = currentMap
                loadApplicants(jobId)
            } else {
                Log.e("JobApplicantsViewModel", "Puan gönderilemedi.")
            }
        }
    }
}