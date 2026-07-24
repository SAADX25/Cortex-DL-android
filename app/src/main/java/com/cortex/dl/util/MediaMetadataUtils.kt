package com.cortex.dl.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.util.Locale

data class VideoMetadata(
    val title: String = "",
    val resolution: String = "Unknown",
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: String = "Unknown",
    val fileSizeFormatted: String = "Unknown",
    val fileSizeBytes: Long = 0L,
    val durationFormatted: String = "00:00",
    val durationMillis: Long = 0L,
    val mimeType: String = "Unknown",
    val videoCodec: String = "Unknown",
    val bitrateFormatted: String = "Unknown",
)

object MediaMetadataUtils {
    fun extractMetadata(context: Context, videoPath: String, fallbackTitle: String = ""): VideoMetadata {
        val file = File(videoPath)
        val fileSizeBytes = if (file.exists()) file.length() else 0L
        val fileSizeFormatted = formatFileSize(fileSizeBytes)

        val retriever = MediaMetadataRetriever()
        return try {
            if (file.exists()) {
                retriever.setDataSource(videoPath)
            } else {
                retriever.setDataSource(context, Uri.parse(videoPath))
            }

            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val width = widthStr?.toIntOrNull() ?: 0
            val height = heightStr?.toIntOrNull() ?: 0

            val resolution = if (width > 0 && height > 0) {
                val label = when {
                    height >= 2160 || width >= 3840 -> "4K Ultra HD"
                    height >= 1440 || width >= 2560 -> "2K QHD"
                    height >= 1080 || width >= 1920 -> "1080p Full HD"
                    height >= 720 || width >= 1280 -> "720p HD"
                    height >= 480 -> "480p SD"
                    else -> "${height}p"
                }
                "${width} × ${height} ($label)"
            } else "Unknown"

            val fpsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
            val fpsVal = fpsStr?.toFloatOrNull()
            val frameRate = if (fpsVal != null && fpsVal > 0f) {
                "${String.format(Locale.US, "%.1f", fpsVal)} FPS"
            } else {
                "Standard (24-60 FPS)"
            }

            val durationMsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationMsStr?.toLongOrNull() ?: 0L
            val durationFormatted = formatDuration(durationMs)

            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "video/mp4"

            val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            val bitrateVal = bitrateStr?.toLongOrNull()
            val bitrateFormatted = if (bitrateVal != null && bitrateVal > 0L) {
                val kbps = bitrateVal / 1000
                if (kbps >= 1000) {
                    "${String.format(Locale.US, "%.1f", kbps / 1000.0)} Mbps"
                } else {
                    "$kbps Kbps"
                }
            } else "Unknown"

            val extractedTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() } ?: fallbackTitle.ifBlank { file.name }

            VideoMetadata(
                title = extractedTitle,
                resolution = resolution,
                width = width,
                height = height,
                frameRate = frameRate,
                fileSizeFormatted = fileSizeFormatted,
                fileSizeBytes = fileSizeBytes,
                durationFormatted = durationFormatted,
                durationMillis = durationMs,
                mimeType = mimeType,
                videoCodec = mimeType.removePrefix("video/").uppercase(Locale.US),
                bitrateFormatted = bitrateFormatted,
            )
        } catch (e: Exception) {
            VideoMetadata(
                title = fallbackTitle.ifBlank { file.name },
                fileSizeFormatted = fileSizeFormatted,
                fileSizeBytes = fileSizeBytes,
            )
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    private fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = millis / (1000 * 60 * 60)
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
