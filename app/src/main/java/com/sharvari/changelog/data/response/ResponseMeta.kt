package com.sharvari.changelog.data.response

import kotlinx.serialization.Serializable

@Serializable
data class ResponseMeta(
    val limit: Int,
    val offset: Int,
    val count: Int,
)
