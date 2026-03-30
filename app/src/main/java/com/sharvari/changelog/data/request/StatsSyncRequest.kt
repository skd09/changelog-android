package com.sharvari.changelog.data.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatsSyncRequest(
    val reads: Int,
    val skips: Int,
    val sessions: Int,
    @SerialName("read_seconds")    val readSeconds: Int,
    @SerialName("session_seconds") val sessionSeconds: Int,
)
