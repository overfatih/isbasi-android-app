package com.profplay.isbasi.data.model
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Application(
    val id: String? = null,
    @SerialName("job_id")
    val jobId: String, // Başvurulan işin ID'si
    @SerialName("worker_id")
    val workerId: String, // Başvuru yapan işçinin ID'si
    val status: String // "pending", "approved", "rejected"
)