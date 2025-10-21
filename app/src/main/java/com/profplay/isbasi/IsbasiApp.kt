package com.profplay.isbasi

import android.app.Application
import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest


class IsbasiApp: Application(){
    companion object {
        lateinit var supabase: SupabaseClient
            private set
    }

    override fun onCreate() {
        super.onCreate()
        // Log ekleyelim
        println("Application onCreate: Supabase başlatılıyor...") // System.out daha garantili olabilir
        Log.d("IsbasiApp", "Application onCreate: Supabase başlatılıyor...")
        // Supabase client oluşturuluyor
        supabase = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }
        Log.i("IsbasiApp", "Supabase BAŞARIYLA başlatıldı!")
        println("Supabase BAŞARIYLA başlatıldı!")
        println("Application started")
    }
}