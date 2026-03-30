package com.sharvari.changelog.model.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val maintenance: MaintenanceConfig,
    @SerialName("force_update") val forceUpdate: ForceUpdateConfig,
)

@Serializable
data class MaintenanceConfig(
    val enabled: Boolean = false,
    val message: String = "We'll be back shortly.",
    val eta: String? = null,
)

@Serializable
data class ForceUpdateConfig(
    val enabled: Boolean = false,
    @SerialName("min_version")   val minVersion: String = "0.0.0",
    @SerialName("app_store_url") val appStoreUrl: String = "",
    val message: String = "Please update to continue.",
)
