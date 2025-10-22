package com.profplay.isbasi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.profplay.isbasi.viewmodel.AuthViewModel
import com.profplay.isbasi.viewmodel.HumansViewModel

// EmployerScreen.kt
@Composable
fun EmployerScreen(
    navController: NavController,
    humanViewModel: HumansViewModel, // Kendi profilini göstermek için
    authViewModel: AuthViewModel
) {
    val humanMe by humanViewModel.humanMe.collectAsState()
    // Giriş yapan işverenin ID'sine ihtiyacımız var
    val currentUserId = humanMe?.id

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("İşveren Ekranı", style = MaterialTheme.typography.titleLarge)

        Text("Hoş geldin, ${humanMe?.name ?: currentUserId ?: "Kullanıcı"}")
        Text("Rol: ${humanMe?.role}")

        Spacer(modifier = Modifier.weight(1f)) // Butonları aşağı iter

        // YENİ BUTON: Kendi işlerini listelemek için
        Button(
            onClick = {
                if (currentUserId != null) {
                    // JobListScreen'e MyJobs moduyla ve KENDİ ID'siyle git
                    navController.navigate("job_list/${JobListLoadMode.MyJobs.name}?employerId=$currentUserId")
                } else {
                    // ID henüz yüklenmediyse bir uyarı verilebilir
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = currentUserId != null // ID yüklenene kadar pasif
        ) {
            Text("Yayınladığım İlanları Gör")
        }

        Button(
            onClick = { navController.navigate("create_job") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Yeni İş İlanı Yayınla") }

        Button(
            onClick = { navController.navigate("profile") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Profilimi Düzenle") }

        Button(
            onClick = { authViewModel.logout() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Çıkış Yap") }
    }
}