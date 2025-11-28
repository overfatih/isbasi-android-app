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
    private val _dateEnd = mutableStateOf("")
    val dateEnd: State<String> = _dateEnd

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
        // Kullanıcı kolaylığı: Başlangıç seçilince, bitişi de otomatik olarak o gün yap (varsayılan 1 gün)
        // Eğer bitiş tarihi boşsa veya başlangıçtan önceyse güncelle.
        if (_dateEnd.value.isBlank() || selectedDateMillis > convertDateToMillis(_dateEnd.value)) {
            _dateEnd.value = _dateStart.value
        }
    }
    fun onDateEndChange(selectedDateMillis: Long) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        _dateEnd.value = dateFormat.format(Date(selectedDateMillis))
    }

    // String tarihi Long'a çeviren yardımcı (karşılaştırma için)
    private fun convertDateToMillis(dateStr: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) { 0L }
    }

    // --- Ana İşlem Fonksiyonu ---

    fun publishJob() {
        viewModelScope.launch {
            // dateEnd kontrolünü de ekle
            if (title.value.isBlank() || location.value.isBlank() || dateStart.value.isBlank() || dateEnd.value.isBlank()) {
                _uiState.value = CreateJobUiState.Error("Lütfen tüm zorunlu alanları doldurun.")
                return@launch
            }
            // Tarih mantık kontrolü: Bitiş, başlangıçtan önce olamaz
            if (convertDateToMillis(_dateEnd.value) < convertDateToMillis(_dateStart.value)) {
                _uiState.value = CreateJobUiState.Error("Bitiş tarihi, başlangıç tarihinden önce olamaz.")
                return@launch
            }
            _uiState.value = CreateJobUiState.Loading
            val ratingFloat = _minRating.value.toFloatOrNull()
            val success = repository.createJob(
                title = _title.value,
                description = _description.value,
                location = _location.value,
                dateStart = _dateStart.value,
                dateEnd = _dateEnd.value, // <-- GÖNDERİLİYOR
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
        _dateEnd.value = ""
        _minRating.value = ""
    }

    // Hata mesajı gösterildikten sonra state'i Idle'a çekmek için
    fun resetStateToIdle() {
        _uiState.value = CreateJobUiState.Idle
    }
}