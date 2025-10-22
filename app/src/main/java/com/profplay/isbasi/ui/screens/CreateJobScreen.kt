package com.profplay.isbasi.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.profplay.isbasi.viewmodel.CreateJobUiState
import com.profplay.isbasi.viewmodel.CreateJobViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateJobScreen(
    navController: NavController,
    viewModel: CreateJobViewModel
) {
    val title by viewModel.title
    val description by viewModel.description
    val location by viewModel.location
    val dateStart by viewModel.dateStart
    val minRating by viewModel.minRating
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Date Picker (Tarih Seçici) için state'ler
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    // UI state değişikliklerini dinle (Snackbar göstermek için)
    LaunchedEffect(key1 = uiState) {
        when (val state = uiState) {
            is CreateJobUiState.Success -> {
                scope.launch {
                    snackbarHostState.showSnackbar("İlan başarıyla yayınlandı!")
                }
                // Başarıdan sonra formu temizledik (VM'de), şimdi ekrandan geri çıkalım
                navController.popBackStack()
                viewModel.resetStateToIdle()
            }
            is CreateJobUiState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar("Hata: ${state.message}")
                }
                // Hata sonrası state'i Idle'a çek ki kullanıcı tekrar deneyebilsin
                viewModel.resetStateToIdle()
            }
            else -> {} // Idle veya Loading
        }
    }

    // Tarih Seçici Dialog'u
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        showDatePicker = false
                        // Seçilen tarihi al (milisaniye olarak)
                        val selectedDateMillis = datePickerState.selectedDateMillis
                        if (selectedDateMillis != null) {
                            // ViewModel'e tarihi Long olarak gönder (VM bunu formatlayacak)
                            viewModel.onDateStartChange(selectedDateMillis)
                        }
                    }
                ) {
                    Text("Tamam")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("İptal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // --- Ekranın Ana Tasarımı ---
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Yeni İş İlanı Oluştur") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { // Geri tuşu
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // TopAppBar için padding
                .padding(16.dp) // Ekranın genel padding'i
                .verticalScroll(rememberScrollState()), // Ekran dolarsa kaydırma özelliği
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextField(
                value = title,
                onValueChange = viewModel::onTitleChange, // Kısayol: viewModel.onTitleChange(it)
                label = { Text("Başlık (Örn: Üzüm Kesim Elemanı)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            TextField(
                value = description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Açıklama (İşin detayları, ücret vb.)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )

            TextField(
                value = location,
                onValueChange = viewModel::onLocationChange,
                label = { Text("Konum (Örn: Salihli, Adala Köyü)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Tarih Seçme Alanı
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true } // Tıklama olayı artık Box'ta
            ) {
                TextField(
                    value = dateStart,
                    onValueChange = {},
                    label = { Text("İş Başlama Tarihi") },
                    enabled = false, // Tıklama almasın diye pasif yapıyoruz
                    modifier = Modifier.fillMaxWidth(), // Tıklama modifiyesi burada değil
                    trailingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = "Tarih Seç")
                    },
                    // Pasif (disabled) olduğunda soluk gri DEĞİL, normal görünmesi için renkleri eziyoruz
                    colors = TextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        // Eğer OutlinedTextField kullanıyorsanız, buraya 'disabledBorderColor' da eklemeniz gerekir.
                        // Normal TextField için bu kadarı yeterlidir.
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest // Hafif bir arka plan
                    )
                )
            }

            TextField(
                value = minRating,
                onValueChange = viewModel::onMinRatingChange,
                label = { Text("Minimum Puan (Örn: 4.5) (Opsiyonel)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.weight(1f)) // Butonu olabildiğince aşağı iter

            // Yayınla Butonu
            Button(
                onClick = { viewModel.publishJob() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is CreateJobUiState.Loading // Yüklerken tıklanamaz
            ) {
                if (uiState is CreateJobUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("İlanı Yayınla")
                }
            }
        }
    }
}