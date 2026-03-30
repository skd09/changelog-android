package com.sharvari.changelog.data.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationPreferencesRequest(
    @SerialName("frequency")    val frequency: String,
    @SerialName("time")         val time: String,
    @SerialName("category_ids") val categoryIds: List<String>?,
)
