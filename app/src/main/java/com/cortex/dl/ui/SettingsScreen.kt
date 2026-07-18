package com.cortex.dl.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.cortex.dl.App
import com.cortex.dl.Directory
import com.cortex.dl.R
import com.cortex.dl.download.DownloaderV2
import com.cortex.dl.download.Task
import com.cortex.dl.ui.theme.CortexCyan
import com.cortex.dl.ui.theme.CortexDarkBackground
import com.cortex.dl.ui.theme.CortexSurface
import com.cortex.dl.ui.theme.CortexTextPrimary
import com.cortex.dl.ui.theme.CortexTextSecondary
import com.cortex.dl.util.PreferenceUtil.getString
import com.cortex.dl.util.VIDEO_DIRECTORY
import com.cortex.dl.util.YT_DLP_VERSION
import com.cortex.dl.util.UpdateUtil
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private data class LanguageOption(val tag: String, val nativeName: String, val displayName: String)

private val supportedLanguages = listOf(
    LanguageOption("ar", "العربية", "Arabic"),
    LanguageOption("en", "English", "English"),
    LanguageOption("es", "Español", "Spanish"),
    LanguageOption("fr", "Français", "French"),
    LanguageOption("de", "Deutsch", "German"),
    LanguageOption("tr", "Türkçe", "Turkish"),
    LanguageOption("ru", "Русский", "Russian"),
    LanguageOption("zh", "中文", "Chinese"),
)

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloader = koinInject<DownloaderV2>()
    val defaultPathText = stringResource(R.string.defaults)
    var currentPath by remember { mutableStateOf(VIDEO_DIRECTORY.getString(default = defaultPathText)) }
    var showLanguages by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }
    val currentLanguageTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifBlank { "en" }
    val selectedLanguage = supportedLanguages.firstOrNull { currentLanguageTag.startsWith(it.tag) } ?: supportedLanguages[1]
    val hasActiveDownload = downloader.getTaskStateMap().values.any {
        it.downloadState is Task.DownloadState.Running || it.downloadState is Task.DownloadState.FetchingInfo
    }

    val directoryPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        App.updateDownloadDir(uri, Directory.VIDEO)
        currentPath = App.videoDownloadDir
    }

    Column(
        modifier = Modifier.fillMaxSize().background(CortexDarkBackground).padding(16.dp),
    ) {
        Text(stringResource(R.string.settings), color = CortexCyan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        SettingsItem(
            icon = { Icon(Icons.Default.Language, null, tint = CortexCyan) },
            title = stringResource(R.string.language),
            subtitle = selectedLanguage.nativeName,
            onClick = { showLanguages = true },
        )
        Spacer(Modifier.height(12.dp))
        SettingsItem(
            icon = { Icon(Icons.Default.Folder, null, tint = CortexCyan) },
            title = stringResource(R.string.download_directory),
            subtitle = currentPath,
            onClick = { directoryPicker.launch(null) },
        )
        Spacer(Modifier.height(12.dp))
        SettingsItem(
            enabled = !isUpdating && !hasActiveDownload,
            icon = {
                if (isUpdating) CircularProgressIndicator(color = CortexCyan, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                else Icon(Icons.Default.Refresh, null, tint = CortexCyan)
            },
            title = if (isUpdating) "Updating download engine…" else "Update download engine",
            subtitle = when {
                hasActiveDownload -> "Available after current downloads finish"
                else -> "yt-dlp ${YT_DLP_VERSION.getString(default = "checking…")}" 
            },
            onClick = {
                if (hasActiveDownload) return@SettingsItem
                isUpdating = true
                scope.launch {
                    val message = runCatching { UpdateUtil.updateYtDlp() }.fold(
                        onSuccess = { status ->
                            when (status) {
                                YoutubeDL.UpdateStatus.DONE -> "Download engine updated successfully"
                                YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> "Download engine is already up to date"
                                else -> "The download engine could not be updated"
                            }
                        },
                        onFailure = { "Could not update the download engine. Check your connection and try again." },
                    )
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                    isUpdating = false
                }
            },
        )
    }

    if (showLanguages) {
        LanguagePickerSheet(
            selectedTag = selectedLanguage.tag,
            onSelect = { tag ->
                showLanguages = false
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            },
            onDismiss = { showLanguages = false },
        )
    }
}

@Composable
private fun SettingsItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CortexSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            icon()
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(title, color = if (enabled) CortexTextPrimary else CortexTextSecondary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = CortexTextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePickerSheet(selectedTag: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CortexSurface,
        contentColor = CortexTextPrimary,
    ) {
        Text("Choose display language", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(supportedLanguages, key = { it.tag }) { language ->
                val selected = selectedTag.startsWith(language.tag)
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (selected) CortexCyan.copy(alpha = 0.18f) else CortexDarkBackground),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(language.tag) },
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(language.nativeName, color = CortexTextPrimary, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        Text(language.displayName, color = if (selected) CortexCyan else CortexTextSecondary)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
