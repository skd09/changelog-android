package com.sharvari.changelog.data.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FCMTokenRequest(
    @SerialName("fcm_token") val fcmToken: String,
)
