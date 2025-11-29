package com.profplay.isbasi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.profplay.isbasi.data.model.Application
import com.profplay.isbasi.data.model.Review
import com.profplay.isbasi.data.model.ReviewWithReviewer
import com.profplay.isbasi.data.model.User
import com.profplay.isbasi.viewmodel.ApplicantsUiState
import com.profplay.isbasi.viewmodel.JobApplicantsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobApplicantsScreen(
    navController: NavController,
    viewModel: JobApplicantsViewModel,
    jobId: String
) {
    val uiState by viewModel.uiState.collectAsState()
    val reviewsMap by viewModel.reviewsState.collectAsState()
    val myReviewsMap by viewModel.myReviewsForJob.collectAsState()
    val currentJob by viewModel.currentJob.collectAsState()
    LaunchedEffect(jobId) {
        viewModel.loadApplicants(jobId)
        viewModel.loadJobDetails(jobId)
    }

    val isJobExpired = remember(currentJob) {
        if (currentJob == null) false else {
            try {
                val formatter = DateTimeFormatter.ISO_DATE
                val endDateStr = currentJob!!.dateEnd?.takeIf { it.isNotBlank() } ?: currentJob!!.dateStart
                val endDate = LocalDate.parse(endDateStr, formatter)
                !LocalDate.now().isBefore(endDate) // Bugün >= Bitiş
            } catch (e: Exception) { false }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Başvurular") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val state = uiState) {
                is ApplicantsUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is ApplicantsUiState.Error -> Text("Hata: ${state.message}", Modifier.align(Alignment.Center))
                is ApplicantsUiState.Success -> {
                    if (state.applicants.isEmpty()) {
                        Text("Henüz başvuru yok.", Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(contentPadding = PaddingValues(16.dp)) {
                            items(state.applicants) { (application, worker) ->
                                ApplicantItem(
                                    application = application,
                                    worker = worker,
                                    isJobExpired = isJobExpired,
                                    myReview = myReviewsMap[worker.id],
                                    onUpdateStatus = { status ->
                                        // application.id null olamaz, veritabanından geliyor
                                        viewModel.updateStatus(application.id!!, status, jobId)
                                    },
                                    // Yorumları getir isteği
                                    onRequestReviews = { workerId ->
                                        viewModel.loadReviews(workerId)
                                    },
                                    reviews = reviewsMap[worker.id],
                                    onRateWorker = { score, comment ->
                                        viewModel.submitReview(jobId, worker.id, score, comment)
                                    }
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun ApplicantItem(
    application: Application,
    worker: User,
    isJobExpired: Boolean,
    myReview: Review?,
    onUpdateStatus: (String) -> Unit,
    onRequestReviews: (String) -> Unit,
    reviews: List<ReviewWithReviewer>?,
    onRateWorker: (Int, String) -> Unit
) {
    // Dialog State'leri
    var showBioDialog by remember { mutableStateOf(false) }
    var showReviewsDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var showMyReviewDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // --- 1. BAŞLIK: İSİM ve PUAN ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = worker.name ?: "İsimsiz İşçi",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                // Puan ve Yorumlar Butonu
                TextButton(onClick = {
                    onRequestReviews(worker.id)
                    showReviewsDialog = true
                }) {
                    Text(text = "⭐ ${worker.rating ?: "-"}")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Email, contentDescription = "Yorumlar", modifier = Modifier.size(16.dp))
                }
            }

            // --- 2. BİO BUTONU ---
            TextButton(
                onClick = { showBioDialog = true },
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("İşçi Detaylarını Gör (Bio)")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // --- 3. AKSİYON BUTONLARI (TEK BİR ROW İÇİNDE) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End, // Sağa yasla
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Duruma göre TEK BİR set buton göster
                when (application.status) {
                    "pending" -> {
                        // Sadece Beklemedeyse: Onayla ve Reddet butonları
                        Button(
                            onClick = { onUpdateStatus("approved") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("Onayla")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { onUpdateStatus("rejected") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Reddet")
                        }
                    }

                    "rejected" -> {
                        // Reddedildiyse sadece yazı
                        Text("❌ REDDEDİLDİ", color = Color.Red, fontWeight = FontWeight.Bold)
                    }

                    "approved" -> {
                        // Onaylandıysa
                        Column(horizontalAlignment = Alignment.End) {
                            Text("✅ ONAYLANDI", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)

                            Spacer(modifier = Modifier.height(4.dp))

                            // Puanlama Mantığı
                            if (myReview != null) {
                                // Zaten puanlamışsak -> Puanı Gör
                                Button(
                                    onClick = { showMyReviewDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                                ) {
                                    Text("Verdiğin Puanı Gör")
                                }
                            } else {
                                // Henüz puanlamamışsak -> Oy Ver
                                Button(
                                    onClick = { showRatingDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    // Tarih bitmişse normal buton, bitmemişse uyarıcı metin
                                    Text(if (isJobExpired) "İşçiye Oy Ver" else "Erken Oy Ver")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DİALOGLAR (AYNI KALIYOR) ---

    if (showBioDialog) {
        AlertDialog(
            onDismissRequest = { showBioDialog = false },
            title = { Text(worker.name ?: "İşçi Detayı") },
            text = { Text(worker.bio?.takeIf { it.isNotBlank() } ?: "Biyografi yok.") },
            confirmButton = { TextButton(onClick = { showBioDialog = false }) { Text("Kapat") } }
        )
    }

    if (showReviewsDialog) {
        AlertDialog(
            onDismissRequest = { showReviewsDialog = false },
            title = { Text("${worker.name} Hakkında Yorumlar") },
            text = {
                if (reviews == null) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                else if (reviews.isEmpty()) Text("Henüz yorum yok.")
                else {
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(reviews) { item ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(item.reviewerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("⭐".repeat(item.review.score), fontSize = 12.sp)
                                if (!item.review.comment.isNullOrBlank()) Text(item.review.comment, fontSize = 14.sp)
                                Divider(modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showReviewsDialog = false }) { Text("Kapat") } }
        )
    }

    if (showRatingDialog) {
        RateUserDialog(
            onDismiss = { showRatingDialog = false },
            onSubmit = { score, comment ->
                onRateWorker(score, comment)
                showRatingDialog = false
            }
        )
    }

    if (showMyReviewDialog && myReview != null) {
        AlertDialog(
            onDismissRequest = { showMyReviewDialog = false },
            title = { Text("Bu İşçiye Verdiğin Puan") },
            text = {
                Column {
                    Text("Puan: " + "⭐".repeat(myReview.score))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Yorumun:")
                    Text(myReview.comment ?: "Yorum yazılmamış.", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = { TextButton(onClick = { showMyReviewDialog = false }) { Text("Kapat") } }
        )
    }
}