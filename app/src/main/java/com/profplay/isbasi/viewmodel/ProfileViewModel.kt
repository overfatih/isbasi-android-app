package com.profplay.isbasi.viewmodel

import android.util.Log // Ekle
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.profplay.isbasi.data.repository.SupabaseRepository
import kotlinx.coroutines.Dispatchers // Ekle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.State
import kotlinx.coroutines.withContext // Ekle

class ProfileViewModel(
    private val repository: SupabaseRepository
) : ViewModel() {

    private val _name = mutableStateOf("")
    val name: State<String> = _name
    private val _bio = mutableStateOf("")
    val bio: State<String> = _bio
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState
    private var currentUserId: String? = null // Kaydetme için hala gerekli


    fun loadProfileData() {
        Log.d("ProfileViewModel", "loadProfileData: Çağrıldı. Veri çekiliyor...")
        // Coroutine'i burada başlatalım
        viewModelScope.launch {
            fetchProfileDataInternal() // Eski içeriği taşıdığımız yeni suspend fonksiyon
        }
    }
    // ------------------------------------

    // Eski loadProfileData içeriğini buraya taşıdık ve suspend yaptık
    private suspend fun fetchProfileDataInternal() {
        // Dispatchers.IO'ya geçiş
        withContext(Dispatchers.IO) {
            Log.d("ProfileViewModel", "fetchProfileDataInternal: Coroutine BAŞLADI (IO).")
            // State'i Main'de güncellememiz gerektiği için önce Loading'e alalım
            withContext(Dispatchers.Main) { _uiState.value = ProfileUiState.Loading }
            try {
                Log.d("ProfileViewModel", "fetchProfileDataInternal: repository.getHuman() çağrılıyor...")
                val userProfile = repository.getHuman() // IO üzerinde çalışıyor

                // Sonucu Main thread'de işle
                withContext(Dispatchers.Main) {
                    if (userProfile != null) {
                        currentUserId = userProfile.id
                        _name.value = userProfile.name ?: ""
                        _bio.value = userProfile.bio ?: ""
                        _uiState.value = ProfileUiState.Idle
                        Log.i("ProfileViewModel", "fetchProfileDataInternal: Başarıyla profil yüklendi.")
                    } else {
                        _uiState.value = ProfileUiState.Error("Profil bilgileri yüklenemedi (null).")
                        Log.e("ProfileViewModel", "fetchProfileDataInternal: BAŞARILI ama NULL döndü.")
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "fetchProfileDataInternal CATCH'E DÜŞTÜ! Hata: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = ProfileUiState.Error("Hata: ${e.message}")
                }
            }
        }
    }

    fun onNameChange(newName: String) { _name.value = newName }
    fun onBioChange(newBio: String) { _bio.value = newBio }

    fun saveProfile() {
        // currentUserId artık init'te doluyor olmalı
        if (currentUserId == null) {
            Log.e("ProfileViewModel", "saveProfile: currentUserId hala null, kaydetme yapılamıyor.")
            _uiState.value = ProfileUiState.Error("Kullanıcı ID'si bulunamadı, kaydedilemiyor.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) { // IO Dispatcher
            withContext(Dispatchers.Main) { _uiState.value = ProfileUiState.Loading }
            Log.d("ProfileViewModel", "saveProfile: Kaydediliyor... Name='${name.value}', Bio='${bio.value}'")
            val success = repository.updateProfile(
                userId = currentUserId!!,
                name = name.value.takeIf { it.isNotBlank() }, // Boşsa null gönder
                bio = bio.value.takeIf { it.isNotBlank() }   // Boşsa null gönder
            )
            withContext(Dispatchers.Main) {
                if (success) {
                    _uiState.value = ProfileUiState.Success
                    Log.i("ProfileViewModel", "saveProfile: Başarıyla kaydedildi.")
                } else {
                    _uiState.value = ProfileUiState.Error("Profil güncellenemedi.")
                    Log.e("ProfileViewModel", "saveProfile: Kaydetme BAŞARISIZ OLDU (RLS veya başka bir hata?).")
                }
            }
        }
    }
}