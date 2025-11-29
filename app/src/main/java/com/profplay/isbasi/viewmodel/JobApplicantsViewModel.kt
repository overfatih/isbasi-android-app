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

// UI State: Yükleniyor, Liste (Başvuru + İşçi), Hata
sealed interface ApplicantsUiState {
    object Loading : ApplicantsUiState
    data class Success(val applicants: List<Pair<Application, User>>) : ApplicantsUiState
    data class Error(val message: String) : ApplicantsUiState
    object Idle : ApplicantsUiState
}

class JobApplicantsViewModel(private val repository: SupabaseRepository) : ViewModel() {
    private val _reviewsState = MutableStateFlow<Map<String, List<ReviewWithReviewer>>>(emptyMap())
    val reviewsState: StateFlow<Map<String, List<ReviewWithReviewer>>> = _reviewsState
    private val _uiState = MutableStateFlow<ApplicantsUiState>(ApplicantsUiState.Idle)
    val uiState: StateFlow<ApplicantsUiState> = _uiState

    fun loadApplicants(jobId: String) {
        viewModelScope.launch {
            _uiState.value = ApplicantsUiState.Loading
            val list = withContext(Dispatchers.IO) {
                repository.getApplicationsForJob(jobId)
            }
            _uiState.value = ApplicantsUiState.Success(list)
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
}