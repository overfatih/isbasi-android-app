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

@Composable
fun EmployeeScreen(
    navController: NavController,
    humanViewModel: HumansViewModel,
    authViewModel: AuthViewModel
) {
    val humanMe by humanViewModel.humanMe.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Çalışan Ekranı",
            style = MaterialTheme.typography.titleLarge
        )

        // Butonlar
        Button(
            onClick = { navController.navigate("profile") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Profilimi Düzenle") }

        Button(
            onClick = { authViewModel.logout() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Çıkış Yap") }

        Spacer(modifier = Modifier.height(16.dp))

        // Profil bilgisi (humanMe)
        Column(modifier = Modifier.padding(8.dp)) {
            Text("ID: ${humanMe?.name}")
            Text("Role: ${humanMe?.role}")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}