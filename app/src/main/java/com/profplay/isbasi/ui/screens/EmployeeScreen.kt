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

// EmployeeScreen.kt
@Composable
fun EmployeeScreen(
    navController: NavController,
    humanViewModel: HumansViewModel, // Hala kendi profilini göstermek için gerekli
    authViewModel: AuthViewModel
) {
    val humanMe by humanViewModel.humanMe.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Çalışan Ekranı", style = MaterialTheme.typography.titleLarge)

        // Kendi bilgilerini göster
        Text("Hoş geldin, ${humanMe?.name ?: humanMe?.id ?: "Kullanıcı"}")
        Text("Rol: ${humanMe?.role}")

        Spacer(modifier = Modifier.weight(1f)) // Butonları aşağı iter

        // YENİ BUTON: Tüm işleri listelemek için
        Button(
            onClick = {
                // JobListScreen'e AllJobs moduyla git
                navController.navigate("job_list/${JobListLoadMode.AllJobs.name}")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tüm İş İlanlarını Gör")
        }

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