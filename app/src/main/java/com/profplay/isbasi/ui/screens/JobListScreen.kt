package com.profplay.isbasi.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.profplay.isbasi.data.model.Job // Job modelini import et
import com.profplay.isbasi.data.model.JobWithStatus
import com.profplay.isbasi.viewmodel.JobListUiState
import com.profplay.isbasi.viewmodel.JobListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobListScreen(
    navController: NavController,
    viewModel: JobListViewModel,
    // Ekranın hangi modda açıldığını belirlemek için bir parametre ekleyelim
    // Bu, ViewModel'deki doğru fonksiyonu tetiklememize yardımcı olacak.
    // Başlangıçta hangi listeyi yükleyeceğimizi bilmemiz lazım.
    loadMode: JobListLoadMode,
    // Eğer işverene göre yüklüyorsak ID'yi de almalıyız
    employerIdForFilter: String? = null, // Sadece .MyJobs modunda kullanılır
    onJobClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Ekran ilk açıldığında veya loadMode değiştiğinde veriyi yükle
    LaunchedEffect(key1 = loadMode, key2 = employerIdForFilter) {
        when (loadMode) {
            JobListLoadMode.AllJobs -> viewModel.loadAllJobs()
            JobListLoadMode.MyJobs -> {
                if (employerIdForFilter != null) {
                    viewModel.loadJobsForEmployer(employerIdForFilter)
                } else {
                    // Hata durumu, ID gelmeliydi
                    // viewModel.setErrorState("İşveren ID'si bulunamadı.") // Örnek hata yönetimi
                }
            }
            JobListLoadMode.DateRange -> {
                // Tarih aralığı modu için başlangıçta yükleme yapmayabiliriz,
                // Kullanıcının tarih seçmesini bekleyebiliriz.
                // Veya varsayılan bir aralık yükleyebiliriz (örn: bugünün işleri)
                // Şimdilik boş bırakalım, tarih seçicileri ekleyince doldururuz.
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(determineTitle(loadMode)) }, // Başlığı moda göre belirle
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center // Yükleme ve Hata mesajlarını ortalamak için
        ) {
            // UI Durumuna göre içeriği göster
            when (val state = uiState) {
                is JobListUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is JobListUiState.Error -> {
                    Text("Hata: ${state.message}")
                    // Belki bir "Tekrar Dene" butonu eklenebilir
                }
                is JobListUiState.Success -> {
                    if (state.jobs.isEmpty()) {
                        Text("Gösterilecek iş ilanı bulunamadı.")
                    } else {
                        // İş listesini göster
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.jobs) { jobWithStatus ->
                                JobItem(
                                    jobWithStatus = jobWithStatus, // <-- Bu ismin JobItem tanımındaki isimle eşleştiğinden emin ol!
                                    showApplyButton = loadMode == JobListLoadMode.AllJobs,
                                    onApplyClick = { jobId ->
                                        viewModel.applyToJob(jobId)
                                    },
                                    onCancelClick = { jobId ->
                                        viewModel.cancelApplication(jobId)
                                    },
                                    onJobClick = onJobClick
                                )
                            }
                        }
                    }
                }
                is JobListUiState.Idle -> {
                    // Idle durumu genellikle Loading'e geçmeden önceki kısa an
                    // veya DateRange modunda ilk açılış olabilir.
                    // Şimdilik boş bırakabiliriz veya bir mesaj gösterebiliriz.
                    Text("İş listesi yükleniyor...") // Veya moda göre farklı mesaj
                }
            }
        }
    }
}

// Liste elemanını gösterecek basit bir Composable
@Composable
fun JobItem(
    jobWithStatus: JobWithStatus, // JobWithStatus objesi ile başla
    showApplyButton: Boolean,
    onApplyClick: (String) -> Unit,
    onCancelClick: (String) -> Unit,
    onJobClick: (String) -> Unit
) {
    val job = jobWithStatus.job
    val status = jobWithStatus.applicationStatus

    // Duruma göre rengi belirle
    val cardColor = when (status) {
        "pending" -> Color(0xFFFFCC00) // Sarı/Turuncu
        "approved" -> Color(0xFF4CAF50) // Yeşil
        else -> MaterialTheme.colorScheme.surfaceVariant // Varsayılan renk
    }
    Card(
        modifier = Modifier.fillMaxWidth()
            .then(if (!showApplyButton) Modifier.clickable {
                // Tıklama olayını yukarı (JobListScreen'e) taşıman lazım
                job.id?.let { onJobClick(it) }
            } else Modifier),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(job.title, style = MaterialTheme.typography.titleMedium)
            Text("Konum: ${job.location}", style = MaterialTheme.typography.bodyMedium)
            Text("Başlangıç: ${job.dateStart}", style = MaterialTheme.typography.bodySmall)
            job.minRating?.let { // Eğer minRating null değilse göster
                Text("Min. Puan: $it", style = MaterialTheme.typography.bodySmall)
            }
            // İleride buraya tıklama özelliği ekleyip detay sayfasına gidebiliriz
            if (showApplyButton) {
                when (status) {
                    "pending" -> {
                        Button(
                            // İptal Butonuna tıklandığında Job'ın ID'sini geri gönder
                            onClick = { onCancelClick(job.id!!) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Text("İptal Et", color = Color.White)
                        }
                    }
                    "approved" -> {
                        Text("ONAYLANDI", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    else -> {
                        // Talip Ol Butonuna tıklandığında Job'ın ID'sini geri gönder
                        Button(
                            onClick = { onApplyClick(job.id!!) }
                        ) {
                            Text("Talip Ol")
                        }
                    }
                }
            }
        }
    }
}

// Hangi modda veri yükleneceğini belirten bir enum sınıfı
enum class JobListLoadMode {
    AllJobs,      // Tüm işler (İşçi için)
    MyJobs,       // Sadece benim işlerim (İşveren için)
    DateRange     // Tarih aralığına göre tüm işler (İşveren için)
}

// TopAppBar başlığını belirleyen yardımcı fonksiyon
@Composable
private fun determineTitle(loadMode: JobListLoadMode): String {
    return when (loadMode) {
        JobListLoadMode.AllJobs -> "Tüm İş İlanları"
        JobListLoadMode.MyJobs -> "Yayınladığım İlanlar"
        JobListLoadMode.DateRange -> "Tarihe Göre İşler"
    }
}