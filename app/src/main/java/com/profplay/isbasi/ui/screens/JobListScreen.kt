package com.profplay.isbasi.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.profplay.isbasi.data.model.JobWithStatus
import com.profplay.isbasi.viewmodel.JobListUiState
import com.profplay.isbasi.viewmodel.JobListViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobListScreen(
    navController: NavController,
    viewModel: JobListViewModel,
    loadMode: JobListLoadMode,
    employerIdForFilter: String? = null,
    onJobClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showArchive by remember { mutableStateOf(false) }
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
                title = { Text(if (showArchive) "Arşivlenmiş İlanlar" else determineTitle(loadMode)) },
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
                        val filteredJobs = if (showArchive) {
                            // Arşiv Modu: Aktif OLMAYANLARI getir
                            state.jobs.filter { !isJobActive(it) }
                        } else {
                            // Güncel Mod: Aktif OLANLARI getir
                            state.jobs.filter { isJobActive(it) }
                        }
                        // İş listesini göster
                        Column(modifier = Modifier.fillMaxSize()) {

                            // Liste Bölümü (Ağırlıklı alan)
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f) // Kalan tüm alanı kapla
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (filteredJobs.isEmpty()) {
                                    item {
                                        Text(
                                            text = if (showArchive) "Arşivde ilan yok." else "Güncel ilan bulunamadı.",
                                            modifier = Modifier.padding(16.dp),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                } else {
                                    items(filteredJobs) { jobWithStatus ->
                                        JobItem(
                                            jobWithStatus = jobWithStatus,
                                            showApplyButton = loadMode == JobListLoadMode.AllJobs,
                                            onApplyClick = { jobId -> viewModel.applyToJob(jobId) },
                                            onCancelClick = { jobId ->
                                                viewModel.cancelApplication(
                                                    jobId
                                                )
                                            },
                                            onJobClick = onJobClick
                                        )
                                    }
                                }
                            }

                            // --- ALT BUTON (GEÇİŞ) ---
                            // Sadece "AllJobs" (İşçi) modundaysak bu mantığı işletelim.
                            // İşveren modunda da isteyebilirsin, o zaman if'i kaldır.
                            if (loadMode == JobListLoadMode.AllJobs) {
                                Button(
                                    onClick = { showArchive = !showArchive }, // Tersi ile değiştir
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (showArchive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Text(if (showArchive) "Güncel İlanlara Dön" else "Arşivlenmiş İlanları Gör")
                                }
                            }
                        }
                    }
                }
                is JobListUiState.Idle -> Text("İş listesi yükleniyor...")
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
    onJobClick: (String) -> Unit = {}
) {

    val job = jobWithStatus.job
    val status = jobWithStatus.applicationStatus
    val hasConflict = jobWithStatus.hasConflict
    var showDetailsDialog by remember { mutableStateOf(false) }

    // Duruma göre rengi belirle
    val cardColor = when {
        status == "approved" -> Color(0xFF4CAF50) // Yeşil (Onaylandı)
        status == "rejected" -> Color(0xFFFFCDD2) // Açık Kırmızı (Reddedildi)
        hasConflict -> Color.LightGray
        status == "pending" -> Color(0xFFFFCC00) // Sarı (Beklemede)
        else -> MaterialTheme.colorScheme.surfaceVariant // Gri/Varsayılan
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
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
            // Eğer bitiş tarihi varsa aralık göster, yoksa sadece başlangıcı göster
            val dateDisplay = if (!job.dateEnd.isNullOrBlank() && job.dateStart != job.dateEnd) {
                "${job.dateStart} - ${job.dateEnd}"
            } else {
                job.dateStart
            }
            Text("Tarih: $dateDisplay", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            job.minRating?.let { // Eğer minRating null değilse göster
                Text("Min. Puan: $it", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(
                onClick = { showDetailsDialog = true },
                contentPadding = PaddingValues(0.dp) // Sıkışık görünüm için
            ) {
                Icon(Icons.Default.Info, contentDescription = "Detay")
                Spacer(modifier = Modifier.width(4.dp))
                Text("İş Açıklamasını Oku")
            }
            // İleride buraya tıklama özelliği ekleyip detay sayfasına gidebiliriz
            if (showApplyButton) {
                when {
                    status == "approved" -> {
                        Text("ONAYLANDI", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    status == "rejected" -> {
                        Text("❌ REDDEDİLDİ", color = Color.Red, fontWeight = FontWeight.Bold)
                        // Buraya buton koymuyoruz, böylece tekrar başvuramaz.
                    }
                    hasConflict -> {
                        // Çakışma varsa uyarı göster ve buton koyma (veya pasif buton koy)
                        Text("⚠️ TARİH ÇAKIŞMASI", color = Color.Red, fontWeight = FontWeight.Bold)
                        Text("Başka bir onaylı işle çakışıyor.", style = MaterialTheme.typography.bodySmall)
                    }
                    status == "pending" -> {
                        Button(
                            // İptal Butonuna tıklandığında Job'ın ID'sini geri gönder
                            onClick = { onCancelClick(job.id!!) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Text("İptal Et", color = Color.White)
                        }
                    }
                    else -> {
                        // Talip Ol Butonuna tıklandığında Job'ın ID'sini geri gönder
                        Button(
                            onClick = { onApplyClick(job.id!!) }
                        ) {
                            Text("Talep Et")
                        }
                    }
                }
            }
        }
    }
    if (showDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text(text = job.title) },
            text = {
                Column {
                    Text("İş Açıklaması:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = job.description.ifBlank { "Açıklama girilmemiş." })
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showDetailsDialog = false }
                ) {
                    Text("Kapat")
                }
            }
        )
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

fun isJobActive(jobWithStatus: JobWithStatus): Boolean {
    val job = jobWithStatus.job

    // 1. Tarih Kontrolü: Bugün > Bitiş Tarihi ise süresi geçmiştir (Arşivlik)
    val formatter = DateTimeFormatter.ISO_DATE
    val endDateStr = job.dateEnd?.takeIf { it.isNotBlank() } ?: job.dateStart
    val endDate = try {
        LocalDate.parse(endDateStr, formatter)
    } catch (e: Exception) {
        LocalDate.now() // Hata olursa bugünü al (gösterilsin diye)
    }

    val isExpired = LocalDate.now().isAfter(endDate)

    // 2. Reddedilme Kontrolü
    val isRejected = jobWithStatus.applicationStatus == "rejected"

    // 3. Çakışma Kontrolü
    val hasConflict = jobWithStatus.hasConflict

    // Eğer süresi geçmemişse VE reddedilmemişse VE çakışma yoksa -> GÜNCELDİR
    return !isExpired && !isRejected && !hasConflict
}