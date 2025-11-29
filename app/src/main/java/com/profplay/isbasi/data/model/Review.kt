package com.profplay.isbasi.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Review(
    val id: String? = null,
    @SerialName("job_id") val jobId: String,
    @SerialName("reviewer_id") val reviewerId: String,
    @SerialName("reviewee_id") val revieweeId: String,
    val score: Int,
    val comment: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)