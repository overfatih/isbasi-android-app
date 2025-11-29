package com.profplay.isbasi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.profplay.isbasi.viewmodel.AuthViewModel
import com.profplay.isbasi.viewmodel.HumansViewModel

@Composable
fun EmployerScreen(
    navController: NavController,
    humanViewModel: HumansViewModel,
    authViewModel: AuthViewModel
) {
    val humanMe by humanViewModel.humanMe.collectAsState()
    val currentUserId = humanMe?.id

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- ÜST BİLGİ KARTI ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Hoş geldin, İşveren",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = humanMe?.name ?: "Kullanıcı",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // --- MENÜ IZGARASI (2 Sütunlu) ---
        Text(
            "İşlemler",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2), // 2 Sütun
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // 1. Yeni İlan
            item {
                DashboardCard(
                    title = "Yeni İlan Ver",
                    icon = Icons.Default.AddCircle,
                    onClick = { navController.navigate("create_job") }
                )
            }

            // 2. İlanlarım
            item {
                DashboardCard(
                    title = "İlanlarım",
                    icon = Icons.Default.List,
                    onClick = {
                        if (currentUserId != null) {
                            navController.navigate("job_list/MyJobs?employerId=$currentUserId")
                        }
                    }
                )
            }

            // 3. Profil
            item {
                DashboardCard(
                    title = "Profilim",
                    icon = Icons.Default.Person,
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = { navController.navigate("profile") }
                )
            }

            // 4. Hava Durumu (Şimdilik boş)
            item {
                DashboardCard(
                    title = "Hava Durumu",
                    icon = Icons.Default.WbSunny,
                    backgroundColor = Color(0xFFFFF9C4), // Açık Sarı
                    contentColor = Color(0xFFF57F17),    // Turuncu
                    onClick = { navController.navigate("weather") }
                )
            }

            // 5. Ayarlar (Eski 'Çıkış'ın yerine veya üstüne)
            item {
                DashboardCard(
                    title = "Ayarlar",
                    icon = Icons.Default.Settings,
                    onClick = { navController.navigate("settings") }
                )
            }

            // 6. Çıkış Yap (Kırmızımsı)
            item {
                DashboardCard(
                    title = "Çıkış Yap",
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    backgroundColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    onClick = { authViewModel.logout() }
                )
            }
        }
    }
}