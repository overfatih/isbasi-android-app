package com.profplay.isbasi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.profplay.isbasi.data.model.Application
import com.profplay.isbasi.data.model.User
import com.profplay.isbasi.viewmodel.ApplicantsUiState
import com.profplay.isbasi.viewmodel.JobApplicantsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobApplicantsScreen(
    navController: NavController,
    viewModel: JobApplicantsViewModel,
    jobId: String
) {
    val uiState by viewModel.uiState.collectAsState()

    // Ekran açılınca verileri yükle
    LaunchedEffect(jobId) {
        viewModel.loadApplicants(jobId)
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
                                    onUpdateStatus = { status ->
                                        // application.id null olamaz, veritabanından geliyor
                                        viewModel.updateStatus(application.id!!, status, jobId)
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
    onUpdateStatus: (String) -> Unit
) {
    var showBioDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = worker.name ?: "İsimsiz İşçi",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                // Puanı göster
                Text(
                    text = "⭐ ${worker.rating ?: "-"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bio Butonu
            TextButton(onClick = { showBioDialog = true }) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("İşçi Detaylarını Gör (Bio)")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Durum ve Butonlar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (application.status) {
                    "pending" -> {
                        Button(
                            onClick = { onUpdateStatus("approved") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) { Text("Onayla") }

                        Button(
                            onClick = { onUpdateStatus("rejected") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) { Text("Reddet") }
                    }
                    "approved" -> Text("✅ ONAYLANDI", color = Color(0xFF4CAF50))
                    "rejected" -> Text("❌ REDDEDİLDİ", color = Color.Red)
                }
            }
        }
    }

    // Bio Dialog
    if (showBioDialog) {
        AlertDialog(
            onDismissRequest = { showBioDialog = false },
            title = { Text(worker.name ?: "İşçi Detayı") },
            text = { Text(worker.bio ?: "Bu kullanıcı henüz bir biyografi eklememiş.") },
            confirmButton = {
                TextButton(onClick = { showBioDialog = false }) { Text("Kapat") }
            }
        )
    }
}