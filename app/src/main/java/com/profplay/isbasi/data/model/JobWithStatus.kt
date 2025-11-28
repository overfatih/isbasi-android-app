package com.profplay.isbasi.data.model

data class JobWithStatus(
    val job: Job,
    val applicationStatus: String?, // null, "pending", "approved", "rejected"
    val hasConflict: Boolean = false
)