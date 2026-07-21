package com.cortex.dl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.cortex.dl.ui.theme.CortexBlue
import com.cortex.dl.ui.theme.CortexCyan
import com.cortex.dl.ui.theme.CortexDarkBackground
import com.cortex.dl.ui.theme.CortexSurface
import com.cortex.dl.ui.theme.CortexSurfaceBorder
import com.cortex.dl.ui.theme.CortexTextPrimary
import com.cortex.dl.ui.theme.CortexTextSecondary
import com.cortex.dl.util.DownloadUtil
import com.cortex.dl.util.Format
import com.cortex.dl.util.VideoInfo

enum class MediaType { VIDEO, AUDIO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoInfoBottomSheet(
    videoInfo: VideoInfo,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit,
    onStartDownload: (DownloadUtil.DownloadPreferences) -> Unit,
) {
    val resolutions = remember(videoInfo) {
        videoInfo.formats.orEmpty().filter { it.containsVideo() }.mapNotNull { format: Format -> format.height?.toInt() }
            .filter { it > 0 }.distinct().sortedDescending().ifEmpty { listOf(1080, 720, 480) }
    }
    var mediaType by rememberSaveable { mutableStateOf(MediaType.VIDEO) }
    var resolution by rememberSaveable(resolutions) { mutableStateOf(resolutions.first()) }
    var qualityExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E293B),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Download details", color = CortexTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Close", tint = CortexTextSecondary) }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(CortexCyan, CortexBlue))))
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(88.dp, 60.dp).clip(RoundedCornerShape(10.dp)).background(CortexSurface), contentAlignment = Alignment.Center) {
                    if (!videoInfo.thumbnail.isNullOrBlank()) AsyncImage(videoInfo.thumbnail, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else Icon(Icons.Outlined.VideoFile, null, tint = CortexTextSecondary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(videoInfo.title, color = CortexTextPrimary, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(videoInfo.uploader ?: videoInfo.channel.orEmpty(), color = CortexTextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    videoInfo.durationString?.let { Text("Duration: $it", color = CortexTextSecondary, fontSize = 12.sp) }
                }
            }
            Spacer(Modifier.height(20.dp))
            FieldLabel("Download type")
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TypeChip("Video", Icons.Outlined.PlayArrow, mediaType == MediaType.VIDEO, Modifier.weight(1f)) { mediaType = MediaType.VIDEO }
                TypeChip("Audio only", Icons.Outlined.AudioFile, mediaType == MediaType.AUDIO, Modifier.weight(1f)) { mediaType = MediaType.AUDIO }
            }

            if (mediaType == MediaType.VIDEO) {
                Spacer(Modifier.height(16.dp))
                FieldLabel("Quality / Resolution")
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(resolutions) { item ->
                        val isSelected = item == resolution
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) Brush.linearGradient(listOf(CortexCyan, CortexBlue))
                                    else SolidColor(CortexDarkBackground)
                                )
                                .border(
                                    if (isSelected) 0.dp else 1.dp,
                                    CortexSurfaceBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { resolution = item }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${item}p" + if (item >= 1080) " HD" else "",
                                color = if (isSelected) CortexDarkBackground else CortexTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(26.dp))
            Button(
                onClick = { onStartDownload(buildPreferences(mediaType, resolution)) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), contentPadding = PaddingValues(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(54.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(CortexCyan, CortexBlue))),
            ) { Text("Start download", color = CortexDarkBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}

@Composable private fun FieldLabel(text: String) = Text(text, color = CortexTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 7.dp))

@Composable
private fun Selector(text: String, expanded: Boolean, onClick: () -> Unit, onDismiss: () -> Unit, menu: @Composable () -> Unit) {
    Box(Modifier.padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CortexDarkBackground).border(1.dp, CortexSurfaceBorder, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = CortexTextPrimary, fontWeight = FontWeight.Medium)
            Icon(Icons.Outlined.ExpandMore, null, tint = CortexTextSecondary)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, modifier = Modifier.background(CortexSurface)) { menu() }
    }
}

@Composable
private fun TypeChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.clip(RoundedCornerShape(12.dp)).background(if (selected) Brush.linearGradient(listOf(CortexCyan, CortexBlue)) else Brush.linearGradient(listOf(CortexSurface, CortexSurface))).border(if (selected) 0.dp else 1.dp, CortexSurfaceBorder, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 13.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (selected) CortexDarkBackground else CortexTextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp)); Text(label, color = if (selected) CortexDarkBackground else CortexTextSecondary, fontWeight = FontWeight.Bold)
    }
}

private fun buildPreferences(mediaType: MediaType, resolution: Int): DownloadUtil.DownloadPreferences {
    val base = DownloadUtil.DownloadPreferences.createFromPreferences()
    val resolutionCode = when (resolution) { 2160 -> 1; 1440 -> 2; 1080 -> 3; 720 -> 4; 480 -> 5; 360 -> 6; else -> 0 }
    return base.copy(
        extractAudio = mediaType == MediaType.AUDIO,
        videoResolution = if (mediaType == MediaType.VIDEO) resolutionCode else 0,
        formatIdString = "", // Clear stale format ID to allow yt-dlp to automatically fetch best video + audio streams
        mergeAudioStream = if (mediaType == MediaType.VIDEO) true else base.mergeAudioStream,
        mergeToMkv = if (mediaType == MediaType.VIDEO) false else base.mergeToMkv,
    )
}
