package com.sharvari.changelog.data.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceRegisterRequest(
    val platform: String,
    @SerialName("app_version") val appVersion: String,
    @SerialName("fcm_token")   val fcmToken: String? = null,
)
