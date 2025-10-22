package com.profplay.isbasi.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.profplay.isbasi.data.model.Job // Job modelini import et
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
    employerIdForFilter: String? = null // Sadece .MyJobs modunda kullanılır
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
                            items(state.jobs) { job ->
                                JobItem(job = job)
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
fun JobItem(job: Job) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(job.title, style = MaterialTheme.typography.titleMedium)
            Text("Konum: ${job.location}", style = MaterialTheme.typography.bodyMedium)
            Text("Başlangıç: ${job.dateStart}", style = MaterialTheme.typography.bodySmall)
            job.minRating?.let { // Eğer minRating null değilse göster
                Text("Min. Puan: $it", style = MaterialTheme.typography.bodySmall)
            }
            // İleride buraya tıklama özelliği ekleyip detay sayfasına gidebiliriz
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