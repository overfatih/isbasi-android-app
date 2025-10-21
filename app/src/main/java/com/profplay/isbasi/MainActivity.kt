package com.profplay.isbasi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.profplay.isbasi.ui.theme.IsbasiTheme
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.profplay.isbasi.data.repository.SupabaseRepository
import com.profplay.isbasi.viewmodel.AuthViewModel
import com.profplay.isbasi.viewmodel.AuthViewModelFactory
import com.profplay.isbasi.viewmodel.LoggedInNavigation
import com.profplay.isbasi.viewmodel.LoggedOutNavigation

// MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() // Bunu da Theme'in içine alabilirsin

        setContent {
            IsbasiTheme {
                enableEdgeToEdge() // enableEdgeToEdge'i buraya almak daha güvenli
                val repository = SupabaseRepository(IsbasiApp.supabase)
                val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(repository))
                val isLoggedIn by authViewModel.isLoggedIn
                if (isLoggedIn) {
                    LoggedInNavigation(authViewModel = authViewModel)
                } else {
                    LoggedOutNavigation(authViewModel = authViewModel)
                }
            }
        }
    }
}