package com.profplay.isbasi.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.profplay.isbasi.data.model.Job
import com.profplay.isbasi.data.model.JobWithStatus
import com.profplay.isbasi.data.model.Review
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
    // Puanlama Dialog State'leri
    var showRatingDialog by remember { mutableStateOf(false) }
    var selectedJobForRating by remember { mutableStateOf<Job?>(null) }
    var showMyReviewDialog by remember { mutableStateOf<Review?>(null) }
    var showEmployerDialog by remember { mutableStateOf(false) }
    val selectedEmployer by viewModel.selectedEmployer.collectAsState()
    val selectedEmployerReviews by viewModel.selectedEmployerReviews.collectAsState()
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
                                            isArchiveMode = showArchive,
                                            onApplyClick = { jobId -> viewModel.applyToJob(jobId) },
                                            onCancelClick = { jobId ->
                                                viewModel.cancelApplication(
                                                    jobId
                                                )
                                            },
                                            onJobClick = onJobClick,
                                            onRateClick = {
                                                selectedJobForRating = jobWithStatus.job
                                                showRatingDialog = true
                                            },
                                            onShowMyReview = { review -> showMyReviewDialog = review },
                                            onEmployerInfoClick = { employerId ->
                                                viewModel.loadEmployerInfo(employerId)
                                                showEmployerDialog = true
                                            }
                                        )
                                    }
                                }
                            }

                            // --- ALT BUTON (GEÇİŞ) ---
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
                is JobListUiState.Idle -> Text("İş listesi yükleniyor...")
            }
        }
    }
    if (showRatingDialog && selectedJobForRating != null) {
        RateUserDialog(
            onDismiss = { showRatingDialog = false },
            onSubmit = { score, comment ->
                // ViewModel'i çağır
                viewModel.submitReview(
                    jobId = selectedJobForRating!!.id!!,
                    revieweeId = selectedJobForRating!!.employerId, // İşvereni puanlıyoruz
                    score = score,
                    comment = comment
                )
                showRatingDialog = false
            }
        )
    }
    if (showMyReviewDialog != null) {
        AlertDialog(
            onDismissRequest = { showMyReviewDialog = null },
            title = { Text("Değerlendirmeniz") },
            text = {
                Column {
                    Text("Verdiğiniz Puan: " + "⭐".repeat(showMyReviewDialog!!.score))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Yorumunuz:")
                    Text(showMyReviewDialog!!.comment ?: "Yorum yok.", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showMyReviewDialog = null }) { Text("Kapat") }
            }
        )
    }
    if (showEmployerDialog) {
        AlertDialog(
            onDismissRequest = { showEmployerDialog = false },
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            title = { Text(selectedEmployer?.name ?: "İşveren Bilgisi") },
            text = {
                if (selectedEmployer == null) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column {
                        // Puan Bilgisi
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Genel Puan:", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("⭐ ${selectedEmployer?.rating ?: "Henüz yok"}")
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Text("Yorumlar:", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Yorum Listesi
                        if (selectedEmployerReviews.isEmpty()) {
                            Text("Henüz yorum yapılmamış.", fontStyle = FontStyle.Italic)
                        } else {
                            LazyColumn(modifier = Modifier.height(250.dp)) {
                                items(selectedEmployerReviews) { item ->
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text(item.reviewerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                        Row {
                                            repeat(item.review.score) { Text("⭐", fontSize = 10.sp) }
                                        }
                                        if (!item.review.comment.isNullOrBlank()) {
                                            Text(item.review.comment, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Divider(color = Color.LightGray, thickness = 0.5.dp, modifier = Modifier.padding(top = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEmployerDialog = false }) { Text("Kapat") }
            }
        )
    }
}

// Liste elemanını gösterecek basit bir Composable
@Composable
fun JobItem(
    jobWithStatus: JobWithStatus, // JobWithStatus objesi ile başla
    showApplyButton: Boolean,
    isArchiveMode: Boolean,
    onRateClick: () -> Unit,
    onApplyClick: (String) -> Unit,
    onCancelClick: (String) -> Unit,
    onJobClick: (String) -> Unit = {},
    onShowMyReview: (Review) -> Unit,
    onEmployerInfoClick: (String) -> Unit
) {

    val job = jobWithStatus.job
    val status = jobWithStatus.applicationStatus
    val hasConflict = jobWithStatus.hasConflict
    var showDetailsDialog by remember { mutableStateOf(false) }
    val employerRating = jobWithStatus.employerRating // <-- İşveren Puanı
    val myReview = jobWithStatus.myReview           // <-- Benim Yorumum

    // Tarih ve Süre Kontrolü
    val formatter = DateTimeFormatter.ISO_DATE
    val endDateStr = job.dateEnd?.takeIf { it.isNotBlank() } ?: job.dateStart
    val isExpired = try {
        LocalDate.parse(endDateStr, formatter).isBefore(LocalDate.now())
    } catch (e: Exception) { false }

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
            // --- ÜST SATIR: İşveren Puanı ve Başlık ---
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(job.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))

                // İşveren Puanı ve Yorum İkonu
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { onEmployerInfoClick(job.employerId) }
                ) {
                    Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⭐ ${employerRating ?: "-"}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Email, contentDescription = "Yorumlar", modifier = Modifier.size(16.dp))
                    }
                }
            }
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
                if (isArchiveMode) {
                    // ARŞİV MODU

                    // KURAL: Onaylanmışsa VEYA (Beklemede VE Süresi Geçmişse)
                    val canRate = (status == "approved") || (status == "pending" && isExpired)

                    if (canRate) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (myReview != null) {
                            // Zaten puanlamış -> Göster
                            Button(
                                onClick = { onShowMyReview(myReview) }, // <-- Parametreyi kullan
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                            ) { Text("Puanımı Gör") }
                        } else {
                            // Puanlamamış -> Puanla
                            Button(
                                onClick = onRateClick,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("İşvereni Puanla / Yorum Yap") }
                        }
                    } else if (status == "rejected") {
                        Text("❌ REDDEDİLDİ", color = Color.Red, fontWeight = FontWeight.Bold)
                    } else if (hasConflict) {
                        Text("⚠️ ÇAKIŞMA NEDENİYLE İPTAL", color = Color.Gray)
                    } else {
                        Text("Süresi Doldu", color = Color.Gray)
                    }
                } else {
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

@Composable
fun RateUserDialog(
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit // Puan, Yorum
) {
    var score by remember { mutableIntStateOf(0) } // 0 hiçbiri seçili değil
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Değerlendir ve Yorum Yaz") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Yıldızlar
                Row(modifier = Modifier.padding(vertical = 8.dp)) {
                    for (i in 1..5) {
                        IconButton(onClick = { score = i }) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "$i Yıldız",
                                tint = if (i <= score) Color(0xFFFFD700) else Color.Gray, // Altın rengi veya Gri
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Yorum Alanı
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Yorumunuz (Opsiyonel)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(score, comment) },
                enabled = score > 0 // Puan vermeden gönderemesin
            ) {
                Text("Gönder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}