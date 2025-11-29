package com.profplay.isbasi.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    val main: MainInfo,
    val weather: List<WeatherDescription>,
    val name: String, // Şehir adı
    val wind: WindInfo
)

@Serializable
data class MainInfo(
    val temp: Double,      // Sıcaklık
    val humidity: Int,     // Nem
    @SerialName("feels_like") val feelsLike: Double
)

@Serializable
data class WeatherDescription(
    val main: String,        // Örn: Rain, Clear
    val description: String, // Örn: hafif yağmur
    val icon: String         // İkon kodu (01d, 10n vs)
)

@Serializable
data class WindInfo(
    val speed: Double // Rüzgar hızı
)