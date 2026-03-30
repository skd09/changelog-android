package com.sharvari.changelog.data.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceRegisterResponse(
    val token: String,
    val stats: ServerStats? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)
