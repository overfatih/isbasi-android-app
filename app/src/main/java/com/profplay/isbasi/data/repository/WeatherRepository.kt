package com.profplay.isbasi.data.repository

import android.util.Log
import com.profplay.isbasi.BuildConfig
import com.profplay.isbasi.data.model.WeatherResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class WeatherRepository {

    // Basit bir HTTP istemcisi oluşturuyoruz
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true }) // Gereksiz alanları görmezden gel
        }
    }

    suspend fun getWeather(city: String): WeatherResponse? {
        return try {
            val apiKey = BuildConfig.WEATHER_API_KEY
            // URL: Metric (Santigrat) ve Türkçe (lang=tr) ayarlı
            val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$apiKey&units=metric&lang=tr"

            Log.d("WeatherRepo", "Hava durumu çekiliyor: $city")
            val response: WeatherResponse = client.get(url).body()
            response
        } catch (e: Exception) {
            Log.e("WeatherRepo", "Hata: ${e.message}")
            null
        }
    }
}