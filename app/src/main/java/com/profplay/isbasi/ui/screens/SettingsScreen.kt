package com.profplay.isbasi.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.profplay.isbasi.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    // Dialog State'leri
    var showAboutDialog by remember { mutableStateOf(false) }
    var showFaqDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    // Google Play ve Hukuki Linkler (Kendi linklerinle değiştirmelisin)
    val privacyPolicyUrl = "https://profplay.com/apps/isbasi/privacy-policy-isbasi.html"
    val termsConditionUrl = "https://profplay.com/apps/isbasi/terms-conditions-isbasi.html"
    val dataDeletionUrl = "https://profplay.com/delete_account.html"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayarlar") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {

            // --- BÖLÜM 1: GENEL ---
            SettingsSectionTitle("Genel")

            SettingsItem(
                title = "Hakkımızda",
                icon = Icons.Default.Info,
                onClick = { showAboutDialog = true }
            )

            SettingsItem(
                title = "Sıkça Sorulan Sorular (Kılavuz)",
                icon = Icons.AutoMirrored.Filled.Help,
                onClick = { showFaqDialog = true }
            )

            Divider()

            // --- BÖLÜM 2: YASAL & GÜVENLİK ---
            SettingsSectionTitle("Yasal ve Güvenlik")

            SettingsItem(
                title = "Gizlilik Politikası",
                icon = Icons.Default.Security,
                onClick = { uriHandler.openUri(privacyPolicyUrl) }
            )

            SettingsItem(
                title = "Şartlar ve Koşullar",
                icon = Icons.Default.Lock,
                onClick = { uriHandler.openUri(termsConditionUrl) } // Genelde gizlilik politikası içindedir
            )

            SettingsItem(
                title = "Hesabımı ve Verilerimi Sil",
                icon = Icons.Default.Delete,
                iconTint = Color.Red,
                textColor = Color.Red,
                onClick = {
                    // Google Play kuralı: Hem uygulama içi yol hem de web linki sunulmalı.
                    // Şimdilik linke yönlendiriyoruz veya uyarı gösteriyoruz.
                    showDeleteAccountDialog = true
                }
            )

            Divider()

            // --- BÖLÜM 3: HESAP ---
            SettingsSectionTitle("Oturum")

            SettingsItem(
                title = "Çıkış Yap",
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                onClick = { authViewModel.logout() }
            )
        }
    }

    // --- DİALOGLAR ---

    // 1. Hakkımızda Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text("İşbaşı Hakkında") },
            text = { Text("İşbaşı Uygulaması v1.0\n\nTarım sektöründe işveren ve işçiyi buluşturan dijital çözüm ortağınız.\n\nGeliştirici: ProfPlay") },
            confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("Kapat") } }
        )
    }

    // 2. SSS Dialog
    if (showFaqDialog) {
        AlertDialog(
            onDismissRequest = { showFaqDialog = false },
            title = { Text("Kullanım Kılavuzu / SSS") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("S: Nasıl iş ilanı veririm?", fontWeight = FontWeight.Bold)
                    Text("C: Ana ekrandaki 'Yeni İlan Ver' butonunu kullanarak ilanı oluşturabilirsiniz.\n")

                    Text("S: Başvuru nasıl yaparım?", fontWeight = FontWeight.Bold)
                    Text("C: 'İş Ara' menüsünden ilanları listeleyip 'Talep Et' butonuna basarak başvurabilirsiniz.\n")

                    Text("S: Puanlama ne zaman açılır?", fontWeight = FontWeight.Bold)
                    Text("C: İşveren başvurunuzu onayladıktan ve işin tarihi geçtikten sonra puanlama yapabilirsiniz.")
                }
            },
            confirmButton = { TextButton(onClick = { showFaqDialog = false }) { Text("Anladım") } }
        )
    }

    // 3. Hesap Silme Uyarısı
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Hesap Silme") },
            text = { Text("Hesabınızı ve verilerinizi silmek için talep formumuza yönlendirileceksiniz. Devam etmek istiyor musunuz?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAccountDialog = false
                    uriHandler.openUri(dataDeletionUrl) // Web sitesine yönlendir
                }) { Text("Siteye Git", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) { Text("İptal") }
            }
        )
    }
}

// Yardımcı Composable: Liste Elemanı
@Composable
fun SettingsItem(
    title: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = textColor)
    }
}

// Yardımcı Composable: Bölüm Başlığı
@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}