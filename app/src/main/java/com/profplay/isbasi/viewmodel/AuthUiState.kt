package com.profplay.isbasi.viewmodel

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val userRole: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
}
