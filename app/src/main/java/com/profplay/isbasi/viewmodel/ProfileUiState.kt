package com.profplay.isbasi.viewmodel

sealed interface ProfileUiState {
    object Idle : ProfileUiState      // Boşta
    object Loading : ProfileUiState   // Yükleniyor
    object Success : ProfileUiState   // İşlem başarılı
    data class Error(val message: String) : ProfileUiState // Hata durumu
}