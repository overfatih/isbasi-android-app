package com.profplay.isbasi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            .padding(16.dp)
    ) {
        // --- ÜST BİLGİ KARTI ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Hoş geldin, Emekçi",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = humanMe?.name ?: "Kullanıcı",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // --- MENÜ IZGARASI ---
        Text(
            "Menü",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // 1. İş Ara
            item {
                DashboardCard(
                    title = "İş Ara / Başvur",
                    icon = Icons.Default.Search,
                    onClick = { navController.navigate("job_list/AllJobs") }
                )
            }

            // 2. Profil
            item {
                DashboardCard(
                    title = "Profilim",
                    icon = Icons.Default.Person,
                    onClick = { navController.navigate("profile") }
                )
            }

            // 3. Hava Durumu
            item {
                DashboardCard(
                    title = "Hava Durumu",
                    icon = Icons.Default.WbSunny,
                    backgroundColor = Color(0xFFFFF9C4),
                    contentColor = Color(0xFFF57F17),
                    onClick = { navController.navigate("weather") }
                )
            }
            // 4. Ayarlar (Eski 'Çıkış'ın yerine veya üstüne)
            item {
                DashboardCard(
                    title = "Ayarlar",
                    icon = Icons.Default.Settings,
                    onClick = { navController.navigate("settings") }
                )
            }
            // 5. Çıkış
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