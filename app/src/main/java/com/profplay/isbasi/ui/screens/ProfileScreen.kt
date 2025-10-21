package com.profplay.isbasi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.profplay.isbasi.viewmodel.ProfileUiState
import com.profplay.isbasi.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel // Sadece bu kaldı
) {
    // ViewModel'den anlık state'leri (verileri) al
    val name by profileViewModel.name
    val bio by profileViewModel.bio
    val uiState by profileViewModel.uiState.collectAsState()

    // Ekranın UI durumuna göre Snackbar (bildirim çubuğu) göstermek için
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // uiState her değiştiğinde (örn: Success, Error) bu blok çalışır
    LaunchedEffect(key1 = uiState) {
        when (val state = uiState) {
            is ProfileUiState.Success -> {
                // Başarı durumunda Snackbar göster
                scope.launch {
                    snackbarHostState.showSnackbar("Profil başarıyla güncellendi!")
                }
                // NOT: Başarıdan sonra ViewModel'deki state'i Idle'a çekmek
                // (resetStateToIdle() gibi bir fonksiyonla) iyi bir pratiktir.
            }
            is ProfileUiState.Error -> {
                // Hata durumunda Snackbar göster
                scope.launch {
                    snackbarHostState.showSnackbar("Hata: ${state.message}")
                }
            }
            else -> {} // Idle veya Loading durumunda bir şey yapma
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Profili Düzenle") },
                navigationIcon = {
                    // Geri tuşu
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // TopAppBar için padding
                .padding(16.dp), // Ekranın genel padding'i
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Adı için metin alanı
            TextField(
                value = name,
                onValueChange = { profileViewModel.onNameChange(it) }, // Değişikliği VM'e bildir
                label = { Text("Adınız") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Biyografi için metin alanı
            TextField(
                value = bio,
                onValueChange = { profileViewModel.onBioChange(it) }, // Değişikliği VM'e bildir
                label = { Text("Biyografi (Kendinizden bahsedin)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp) // Çok satırlı alan için yükseklik
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Kaydet Butonu
            Button(
                onClick = {
                    profileViewModel.saveProfile() // VM'deki kaydetme fonksiyonunu çağır
                },
                modifier = Modifier.fillMaxWidth(),
                // Yükleme sırasında butonu pasif (tıklanamaz) yap
                enabled = uiState !is ProfileUiState.Loading
            ) {
                Text("Kaydet")
            }

            // Yükleme durumu göstergesi
            if (uiState is ProfileUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}