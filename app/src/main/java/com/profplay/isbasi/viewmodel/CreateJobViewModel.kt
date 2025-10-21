package com.profplay.isbasi.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.profplay.isbasi.data.repository.SupabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.State
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreateJobViewModel(
    private val repository: SupabaseRepository
) : ViewModel() {

    // UI'daki TextField'lara bağlanacak state'ler
    private val _title = mutableStateOf("")
    val title: State<String> = _title

    private val _description = mutableStateOf("")
    val description: State<String> = _description

    private val _location = mutableStateOf("")
    val location: State<String> = _location

    // Tarihi "yyyy-MM-dd" formatında tutacağız (Supabase'in 'date' formatı)
    private val _dateStart = mutableStateOf("")
    val dateStart: State<String> = _dateStart

    // Puanı String olarak alıp Float'a çevireceğiz, böylece "boş" olabilir
    private val _minRating = mutableStateOf("")
    val minRating: State<String> = _minRating

    // Ekranın genel durumunu tutacak state
    private val _uiState = MutableStateFlow<CreateJobUiState>(CreateJobUiState.Idle)
    val uiState: StateFlow<CreateJobUiState> = _uiState

    // --- State Güncelleme Fonksiyonları (UI'dan çağrılacak) ---

    fun onTitleChange(newTitle: String) { _title.value = newTitle }
    fun onDescriptionChange(newDesc: String) { _description.value = newDesc }
    fun onLocationChange(newLocation: String) { _location.value = newLocation }
    fun onMinRatingChange(newRating: String) { _minRating.value = newRating }

    // Tarih seçiciden (date picker) gelen Long (milisaniye) değerini formatlar
    fun onDateStartChange(selectedDateMillis: Long) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        _dateStart.value = dateFormat.format(Date(selectedDateMillis))
    }

    // --- Ana İşlem Fonksiyonu ---

    fun publishJob() {
        viewModelScope.launch {
            // Basit bir zorunlu alan kontrolü
            if (title.value.isBlank() || description.value.isBlank() || location.value.isBlank() || dateStart.value.isBlank()) {
                _uiState.value = CreateJobUiState.Error("Lütfen tüm zorunlu alanları doldurun.")
                return@launch
            }

            _uiState.value = CreateJobUiState.Loading

            // Min Puan'ı Float'a çevirmeyi dene, olmazsa null olsun
            val ratingFloat = _minRating.value.toFloatOrNull()

            // Repository'deki yeni createJob fonksiyonunu çağır
            val success = repository.createJob(
                title = _title.value,
                description = _description.value,
                location = _location.value,
                dateStart = _dateStart.value,
                minRating = ratingFloat
            )

            if (success) {
                _uiState.value = CreateJobUiState.Success
                clearForm() // Başarılıysa formu temizle
            } else {
                _uiState.value = CreateJobUiState.Error("İlan yayınlanamadı. (RLS hatası olabilir)")
            }
        }
    }

    // Başarı durumunda formu sıfırlamak için
    private fun clearForm() {
        _title.value = ""
        _description.value = ""
        _location.value = ""
        _dateStart.value = ""
        _minRating.value = ""
    }

    // Hata mesajı gösterildikten sonra state'i Idle'a çekmek için
    fun resetStateToIdle() {
        _uiState.value = CreateJobUiState.Idle
    }
}