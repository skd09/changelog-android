package com.sharvari.changelog.data.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerStats(
    @SerialName("total_reads")       val totalReads: Int = 0,
    @SerialName("total_skips")       val totalSkips: Int = 0,
    @SerialName("total_sessions")    val totalSessions: Int = 0,
    @SerialName("avg_read_time")     val avgReadTime: Int = 0,
    @SerialName("avg_session_time")  val avgSessionTime: Int = 0,
)
