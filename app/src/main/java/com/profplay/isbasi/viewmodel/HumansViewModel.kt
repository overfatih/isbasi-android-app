package com.profplay.isbasi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.profplay.isbasi.data.model.User
import com.profplay.isbasi.data.repository.SupabaseRepository
import kotlinx.coroutines.Dispatchers // -> Gerekli import
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext // -> Gerekli import

class HumansViewModel(
    private val repository: SupabaseRepository
) : ViewModel() {

    private val _humanMe = MutableStateFlow<User?>(null)
    val humanMe: StateFlow<User?> = _humanMe
    // Bu, LoggedInNavigation'dan çağrılacak
    fun loadCurrentUserProfile() {
        Log.d("HumansViewModel", "loadCurrentUserProfile: Çağrıldı. Veri çekiliyor...")
        // Eski özel fonksiyonu çağır
        // Coroutine'i burada başlatalım
        viewModelScope.launch {
            fetchHumanMeInternal()
        }
    }
    // ------------------------------------

    // Bu fonksiyon suspend kalmalı
    private suspend fun fetchHumanMeInternal() {
        Log.d("HumansViewModel", "fetchHumanMeInternal: BAŞLADI.")
        // ... (Repository null kontrolü)
        try {
            Log.d("HumansViewModel", "fetchHumanMeInternal: TRY bloğuna girildi...")
            val data = withContext(Dispatchers.IO) {
                repository.getHuman() // Bu hala o anki giriş yapmış kullanıcıyı alır
            }
            _humanMe.value = data
            // ... (Başarı/Hata logları)
        } catch (e: Exception) {
            Log.e("HumansViewModel", "fetchHumanMeInternal CATCH'E DÜŞTÜ! Hata: ${e.message}", e)
            _humanMe.value = null
        }
    }
}