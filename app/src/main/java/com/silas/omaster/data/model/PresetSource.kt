package com.silas.omaster.data.model

import com.silas.omaster.model.MasterPreset
import kotlinx.serialization.Serializable

@Serializable
data class PresetSource(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean = true,
    val lastUpdated: Long? = null
)

@Serializable
data class PresetSourceConfig(
    val sources: List<PresetSource> = emptyList()
)

@Serializable
data class PresetSourceResponse(
    val presets: List<MasterPreset>? = null,
    val version: String? = null,
    val updateTime: String? = null
)
