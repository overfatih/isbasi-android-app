package com.profplay.isbasi.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Job(
    val id: String? = null,
    @SerialName("employer_id")
    val employerId: String,
    val title: String,
    val description: String,
    val location: String,
    @SerialName("date_start")
    val dateStart: String,
    @SerialName("min_rating")
    val minRating: Float? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)