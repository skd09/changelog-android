package com.sharvari.changelog.data.request

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackRequest(
    val type: String,
    val message: String,
    val platform: String,
)
