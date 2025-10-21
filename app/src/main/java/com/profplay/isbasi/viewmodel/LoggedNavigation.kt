package com.profplay.isbasi.viewmodel

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.profplay.isbasi.IsbasiApp
import com.profplay.isbasi.data.repository.SupabaseRepository
import com.profplay.isbasi.ui.screens.AdminScreen
import com.profplay.isbasi.ui.screens.AuthScreen
import com.profplay.isbasi.ui.screens.CreateJobScreen
import com.profplay.isbasi.ui.screens.EmployeeScreen
import com.profplay.isbasi.ui.screens.EmployerScreen
import com.profplay.isbasi.ui.screens.ProfileScreen
import com.profplay.isbasi.ui.screens.SupervisorScreen

@Composable
fun LoggedInNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val currentUserId by authViewModel.currentUserId
    val loggedRole by authViewModel.loggedRole
    val isLoadingAuth by authViewModel.isLoading

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {

            // Yükleme ekranı
            if (isLoadingAuth) { // AuthViewModel hazır olana kadar bekle
                Box( modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center ) {
                    CircularProgressIndicator()
                }
                // UserID null ise (giriş yapılmamışsa bu blok çalışmaz zaten ama güvenlik)
            } else if (currentUserId == null) {
                Text("Hata: Kullanıcı ID'si yok!") // Veya Auth ekranına yönlendir
            }
            else {
                // KULLANICI ID'Sİ VAR VE YÜKLEME BİTTİ
                val repository = SupabaseRepository(IsbasiApp.supabase)

                // ViewModel'ler KEY OLMADAN, SABİT olarak oluşturuluyor
                val humansViewModel: HumansViewModel = viewModel( factory = HumansViewModelFactory(repository) )
                val profileViewModel: ProfileViewModel = viewModel( factory = ProfileViewModelFactory(repository) )
                val createJobViewModel: CreateJobViewModel = viewModel( factory = CreateJobViewModelFactory(repository) )

                // --- LaunchedEffect currentUserId'yi DİNLİYOR ---
                // currentUserId null'dan farklı bir değere değiştiğinde ÇALIŞIR
                LaunchedEffect(key1 = currentUserId) {
                    // currentUserId null değilse (ki bu blok çalıştığında zaten null olmaz)
                    Log.d("LoggedInNavigation", "LaunchedEffect($currentUserId): DEĞİŞİKLİK ALGILANDI. Veri yükleniyor.")
                    humansViewModel.loadCurrentUserProfile() // Yeni public fonksiyonu çağır
                    profileViewModel.loadProfileData()      // Yeni public fonksiyonu çağır
                }
                // ---------------------------------------------

                val startDestination = if (loggedRole.isNotEmpty()) loggedRole else "profile"

                NavHost(navController = navController, startDestination = startDestination) {                    // 'humans' rotası sildiysek buraya ekleme
                    composable("admin") { AdminScreen(navController, humansViewModel, authViewModel) }
                    composable("employee") { EmployeeScreen(navController, humansViewModel, authViewModel) }
                    composable("employer") { EmployerScreen(navController, humansViewModel, authViewModel) }
                    composable("supervisor") { SupervisorScreen(navController, humansViewModel, authViewModel) }
                    composable("profile") { ProfileScreen(navController, profileViewModel) } // humanViewModel parametresi yoktu
                    composable("create_job") { CreateJobScreen(navController, createJobViewModel) }
                }
            }
        }
    }
}
@Composable
fun LoggedOutNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "auth") {
        composable("auth") {
            AuthScreen(
                viewModel = authViewModel,
                onAuthSuccess = { /* Bu boş kalabilir, çünkü state değişikliği navigasyonu zaten tetikleyecek */ }
            )
        }
    }
}
