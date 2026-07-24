package com.cortex.dl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.cortex.dl.R
import com.cortex.dl.download.DownloaderV2
import com.cortex.dl.download.Task
import com.cortex.dl.ui.theme.CortexCyan
import com.cortex.dl.ui.theme.CortexDarkBackground
import com.cortex.dl.ui.theme.CortexSurface
import com.cortex.dl.ui.theme.CortexTextPrimary
import com.cortex.dl.ui.theme.CortexTextSecondary
import com.cortex.dl.util.DatabaseUtil
import com.cortex.dl.util.FileUtil
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun DownloadsHistoryScreen() {
    val history by DatabaseUtil.getVisibleDownloadHistoryFlow().collectAsState(initial = emptyList())
    val downloader = koinInject<DownloaderV2>()
    val activeTasks = downloader.getTaskStateMap().entries.filter { (_, state) ->
        state.downloadState !is Task.DownloadState.Completed
    }
    var itemPendingDeletion by remember { mutableStateOf<com.cortex.dl.database.objects.DownloadedVideoInfo?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(CortexDarkBackground)) {
        if (history.isEmpty() && activeTasks.isEmpty()) {
            Text(
                text = stringResource(R.string.no_downloaded_media),
                color = CortexTextSecondary,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (activeTasks.isNotEmpty()) {
                    item { SectionTitle("In progress") }
                    items(activeTasks, key = { it.key.id }) { (task, state) ->
                        ActiveDownloadCard(task = task, state = state, downloader = downloader)
                    }
                }
                if (history.isNotEmpty()) {
                    item { SectionTitle(stringResource(R.string.download_history)) }
                    items(history.reversed(), key = { it.videoPath }) { info ->
                        CompletedDownloadCard(info = info, onDelete = { itemPendingDeletion = info })
                    }
                }
            }
        }
    }

    itemPendingDeletion?.let { info ->
        val scope = rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = { itemPendingDeletion = null },
            title = { Text(stringResource(R.string.delete_info)) },
            text = { Text(stringResource(R.string.delete_info_msg, info.videoTitle)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { DatabaseUtil.deleteInfoList(listOf(info), deleteFile = true) }
                    itemPendingDeletion = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { itemPendingDeletion = null }) { Text(stringResource(R.string.dismiss)) } },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = CortexCyan,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun ActiveDownloadCard(task: Task, state: Task.State, downloader: DownloaderV2) {
    val downloadState = state.downloadState
    val running = downloadState as? Task.DownloadState.Running
    val progress = (running?.progress ?: 0f).coerceIn(0f, 1f)
    val status = when (downloadState) {
        is Task.DownloadState.Running -> downloadState.progressText.ifBlank { "Downloading" }
        is Task.DownloadState.Paused -> "Paused"
        is Task.DownloadState.Error -> "Download failed"
        is Task.DownloadState.FetchingInfo -> "Preparing download"
        is Task.DownloadState.Canceled -> "Cancelled"
        else -> "Waiting in queue"
    }
    val canResume = downloadState is Task.DownloadState.Paused
    val canRetry = downloadState is Task.DownloadState.Error || downloadState is Task.DownloadState.Canceled

    Card(colors = CardDefaults.cardColors(containerColor = CortexSurface), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = state.viewState.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(80.dp, 60.dp).clip(RoundedCornerShape(8.dp)).background(CortexDarkBackground),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.viewState.title.ifBlank { "Preparing download" },
                        color = CortexTextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(status, color = CortexTextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                when {
                    canRetry -> IconButton(onClick = { downloader.restart(task) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Retry", tint = CortexCyan)
                    }
                    canResume -> IconButton(onClick = { downloader.resume(task) }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Resume", tint = CortexCyan)
                    }
                    downloadState is Task.DownloadState.Running -> IconButton(onClick = { downloader.pause(task) }) {
                        Icon(Icons.Filled.Pause, contentDescription = "Pause", tint = CortexCyan)
                    }
                }
                IconButton(onClick = { downloader.cancel(task); downloader.remove(task) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = Color(0xFFFF8A80))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = CortexCyan,
                    trackColor = CortexDarkBackground,
                )
                Spacer(Modifier.width(8.dp))
                Text("${(progress * 100).toInt()}%", color = CortexTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CompletedDownloadCard(
    info: com.cortex.dl.database.objects.DownloadedVideoInfo,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val errorMessage = stringResource(R.string.download_error)
    Card(colors = CardDefaults.cardColors(containerColor = CortexSurface), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = info.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(80.dp, 60.dp).clip(RoundedCornerShape(8.dp)).background(CortexDarkBackground),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(info.videoTitle, color = CortexTextPrimary, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(info.videoAuthor, color = CortexTextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = {
                if (java.io.File(info.videoPath).exists()) {
                    com.cortex.dl.PlayerActivity.start(context, info.videoPath, info.videoTitle)
                } else {
                    FileUtil.openFile(info.videoPath) {
                        android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }) { Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.open_file), tint = CortexCyan) }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), tint = Color(0xFFFF8A80)) }
        }
    }
}
