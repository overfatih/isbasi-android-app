package com.profplay.isbasi.viewmodel

sealed interface CreateJobUiState {
    object Idle : CreateJobUiState      // Boşta, form dolduruluyor
    object Loading : CreateJobUiState   // İlan yayınlanıyor...
    object Success : CreateJobUiState   // Başarıyla yayınlandı
    data class Error(val message: String) : CreateJobUiState // Hata durumu
}