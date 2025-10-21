package com.profplay.isbasi.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val name: String? = null,
    val role: String? = null,
    val rating: Double? = null,
    val bio: String? = null
)