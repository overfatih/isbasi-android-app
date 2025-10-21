package com.profplay.isbasi.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.profplay.isbasi.data.repository.SupabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.State

class AuthViewModel(
    private val repository: SupabaseRepository
) : ViewModel() {

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading
    private val _isLoggedIn = mutableStateOf(false)
    private val _loggedRole = mutableStateOf("")
    val isLoggedIn: State<Boolean> = _isLoggedIn
    val loggedRole: State<String> = _loggedRole
    private val _currentUserId = mutableStateOf<String?>(null)
    val currentUserId: State<String?> = _currentUserId

    init {
        viewModelScope.launch {
            val currentUserId = repository.currentUserId()
            if (currentUserId != null) {
                _isLoggedIn.value = true
                _loggedRole.value = repository.getUserRole(currentUserId)
                _currentUserId.value = currentUserId
            } else {
                _isLoggedIn.value = false
            }
            _isLoading.value = false
        }
    }


    fun refreshSession() {
        _isLoggedIn.value = repository.isLoggedIn()
    }
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    fun signUp(email: String, password: String, role: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val newUserId = repository.signUp(email, password, role)
                if (newUserId != null) {
                    _loggedRole.value = role
                    _isLoggedIn.value = true
                    _currentUserId.value = newUserId
                    _uiState.value = AuthUiState.Success(role)
                } else {
                    _uiState.value = AuthUiState.Error("Kayıt başarılı ama ID alınamadı.")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Bir hata oluştu")
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                // 1. Giriş yap ve kullanıcı ID'sini al (String? dönecek)
                val userId = repository.signIn(email, password)

                if (userId != null) {
                    // 2. ID ile kullanıcının rolünü al
                    val userRole = repository.getUserRole(userId)
                    _loggedRole.value = userRole
                    _currentUserId.value = userId
                    _uiState.value = AuthUiState.Success(userRole)
                    _isLoggedIn.value = true
                } else {
                    _uiState.value = AuthUiState.Error("E-posta veya şifre hatalı.")
                    _isLoggedIn.value = false
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Giriş sırasında bir hata oluştu.")
                _isLoggedIn.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                repository.signOut()
                _uiState.value = AuthUiState.Idle
                _isLoggedIn.value = false
                _loggedRole.value = ""
                _currentUserId.value = null
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Çıkış hatası")
            }
        }
    }
}
