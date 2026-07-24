package com.cortex.dl

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerNotificationManager
import androidx.media3.ui.PlayerView
import com.cortex.dl.ui.theme.CortexCyan
import com.cortex.dl.ui.theme.CortexDarkBackground
import com.cortex.dl.ui.theme.CortexSurface
import com.cortex.dl.ui.theme.CortexSurfaceBorder
import com.cortex.dl.ui.theme.CortexTextPrimary
import com.cortex.dl.ui.theme.CortexTextSecondary
import com.cortex.dl.util.FileUtil
import com.cortex.dl.util.MediaMetadataUtils
import com.cortex.dl.util.VideoMetadata
import java.io.File

class PlayerActivity : ComponentActivity() {

    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH) ?: ""
        val videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: ""

        if (videoPath.isBlank()) {
            finish()
            return
        }

        setContent {
            PlayerScreen(
                videoPath = videoPath,
                initialTitle = videoTitle,
                onBack = { finish() },
                onOpenExternal = {
                    FileUtil.openFile(videoPath) {}
                },
            )
        }
    }

    companion object {
        private const val EXTRA_VIDEO_PATH = "extra_video_path"
        private const val EXTRA_VIDEO_TITLE = "extra_video_title"

        fun start(context: Context, videoPath: String, videoTitle: String = "") {
            val intent = Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_PATH, videoPath)
                putExtra(EXTRA_VIDEO_TITLE, videoTitle)
            }
            context.startActivity(intent)
        }
    }
}

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    videoPath: String,
    initialTitle: String,
    onBack: () -> Unit,
    onOpenExternal: () -> Unit,
) {
    val context = LocalContext.current
    var showInfoSheet by remember { mutableStateOf(false) }
    var metadata by remember { mutableStateOf(VideoMetadata()) }

    // Initialize ExoPlayer
    val exoPlayer = remember(context, videoPath) {
        ExoPlayer.Builder(context).build().apply {
            val uri = if (File(videoPath).exists()) Uri.fromFile(File(videoPath)) else Uri.parse(videoPath)
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Extract metadata asynchronously
    LaunchedEffect(videoPath) {
        metadata = MediaMetadataUtils.extractMetadata(context, videoPath, initialTitle)
    }

    // Update FPS dynamically if detected by ExoPlayer listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                super.onTracksChanged(tracks)
                val format = exoPlayer.videoFormat
                if (format != null && format.frameRate > 0f) {
                    val realFps = "${String.format(java.util.Locale.US, "%.1f", format.frameRate)} FPS"
                    val realRes = if (format.width > 0 && format.height > 0) {
                        "${format.width} × ${format.height}"
                    } else metadata.resolution

                    metadata = metadata.copy(
                        frameRate = realFps,
                        resolution = realRes,
                        width = if (format.width > 0) format.width else metadata.width,
                        height = if (format.height > 0) format.height else metadata.height,
                    )
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    Scaffold(
        containerColor = CortexDarkBackground,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black),
        ) {
            // Player View
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Top Bar Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CortexTextPrimary,
                    )
                }

                Text(
                    text = metadata.title.ifBlank { initialTitle.ifBlank { "Video Player" } },
                    color = CortexTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )

                // Info / Metadata Button
                IconButton(onClick = { showInfoSheet = true }) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Video Information",
                        tint = CortexCyan,
                    )
                }

                // Open with External App Button
                IconButton(onClick = onOpenExternal) {
                    Icon(
                        imageVector = Icons.Filled.OpenInNew,
                        contentDescription = "Open in external app",
                        tint = CortexTextPrimary,
                    )
                }
            }
        }

        // Info / Metadata Bottom Sheet
        if (showInfoSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showInfoSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF1E293B),
                dragHandle = null,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            ) {
                VideoMetadataSheet(
                    metadata = metadata,
                    videoPath = videoPath,
                    onDismiss = { showInfoSheet = false },
                )
            }
        }
    }
}

@Composable
fun VideoMetadataSheet(
    metadata: VideoMetadata,
    videoPath: String,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "تفاصيل الفيديو | Video Info",
                color = CortexCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Outlined.Close, contentDescription = "Close", tint = CortexTextSecondary)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Title item
        MetadataDetailRow(label = "العنوان / Title", value = metadata.title)
        Spacer(Modifier.height(8.dp))

        // Grid / Row items
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                MetadataDetailRow(label = "📐 الدقة (Resolution)", value = metadata.resolution)
            }
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                MetadataDetailRow(label = "🎞️ الفريمات (FPS)", value = metadata.frameRate)
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                MetadataDetailRow(label = "💾 الحجم (File Size)", value = metadata.fileSizeFormatted)
            }
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                MetadataDetailRow(label = "⏱️ المدة (Duration)", value = metadata.durationFormatted)
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                MetadataDetailRow(label = "⚙️ الترميز (Codec)", value = metadata.videoCodec)
            }
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                MetadataDetailRow(label = "📊 البت ريت (Bitrate)", value = metadata.bitrateFormatted)
            }
        }

        Spacer(Modifier.height(8.dp))

        MetadataDetailRow(label = "📁 المسار (Path)", value = videoPath)

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MetadataDetailRow(label: String, value: String) {
    Surface(
        color = CortexSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CortexSurfaceBorder, RoundedCornerShape(12.dp)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                color = CortexTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value.ifBlank { "Unknown" },
                color = CortexTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
