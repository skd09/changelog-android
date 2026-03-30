package com.sharvari.changelog.data.request

import kotlinx.serialization.Serializable

@Serializable
data class VoteRequest(
    val direction: String,  // "up" or "down"
)
