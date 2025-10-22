package com.profplay.isbasi.viewmodel

import com.profplay.isbasi.data.model.Job // Job modelini import et

sealed interface JobListUiState {
    object Loading : JobListUiState                   // Yükleniyor
    data class Success(val jobs: List<Job>) : JobListUiState // Başarılı, iş listesini içerir
    data class Error(val message: String) : JobListUiState // Hata durumu
    object Idle : JobListUiState // Başlangıç durumu veya boş durum
}