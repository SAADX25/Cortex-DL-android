package com.cortex.dl.database.backup

import com.cortex.dl.database.objects.CommandTemplate
import com.cortex.dl.database.objects.DownloadedVideoInfo
import com.cortex.dl.database.objects.OptionShortcut
import kotlinx.serialization.Serializable

@Serializable
data class Backup(
    val templates: List<CommandTemplate>? = null,
    val shortcuts: List<OptionShortcut>? = null,
    val downloadHistory: List<DownloadedVideoInfo>? = null,
)
