package com.cortex.dl.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cortex.dl.domain.usecase.FetchVideoInfoUseCase
import com.cortex.dl.util.DownloadUtil
import com.cortex.dl.util.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "MainViewModel"

/** Sealed hierarchy representing the UI state of the main download screen. */
sealed interface MainUiState {
    /** Initial / after reset — ready to accept a URL. */
    data object Idle : MainUiState

    /** yt-dlp fetch is in progress — show loading indicator. */
    data object Loading : MainUiState

    /** Fetch succeeded — show the format/quality bottom sheet. */
    data class ReadyWithInfo(val videoInfo: VideoInfo) : MainUiState

    /** Fetch or network error — show error message. */
    data class Error(val message: String) : MainUiState
}

class MainViewModel(
    private val fetchVideoInfoUseCase: FetchVideoInfoUseCase = FetchVideoInfoUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Idle)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _sharedUrl = MutableStateFlow<String?>(null)
    val sharedUrl: StateFlow<String?> = _sharedUrl.asStateFlow()

    fun onSharedUrlReceived(url: String) {
        _sharedUrl.value = url
        fetchVideoInfo(url)
    }

    fun clearSharedUrl() {
        _sharedUrl.value = null
    }

    /**
     * Validates [url], then fetches video metadata in the background via Use Case.
     * On success transitions to [MainUiState.ReadyWithInfo].
     * On failure transitions to [MainUiState.Error] — never crashes.
     */
    fun fetchVideoInfo(url: String) {
        _uiState.value = MainUiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = fetchVideoInfoUseCase(url = url)
                result
                    .onSuccess { info ->
                        Log.d(TAG, "fetchVideoInfo succeeded: ${info.title}")
                        _uiState.value = MainUiState.ReadyWithInfo(info)
                    }
                    .onFailure { th ->
                        Log.e(TAG, "fetchVideoInfo failed", th)
                        _uiState.value = MainUiState.Error(buildErrorMessage(th))
                    }
            } catch (th: Throwable) {
                Log.e(TAG, "fetchVideoInfo unexpected error", th)
                _uiState.value = MainUiState.Error(buildErrorMessage(th))
            }
        }
    }

    /**
     * Hands off [videoInfo] with selected [preferences] to DownloaderV2.
     * Resets UI state back to [MainUiState.Idle] immediately.
     */
    fun startDownload(
        videoInfo: VideoInfo,
        preferences: DownloadUtil.DownloadPreferences,
    ) {
        val downloader = org.koin.java.KoinJavaComponent.getKoin().get<com.cortex.dl.download.DownloaderV2>()
        val task = com.cortex.dl.download.Task(
            url = videoInfo.originalUrl.toString(),
            preferences = preferences
        )
        val state = com.cortex.dl.download.Task.State(
            downloadState = com.cortex.dl.download.Task.DownloadState.ReadyWithInfo,
            videoInfo = videoInfo,
            viewState = com.cortex.dl.download.Task.ViewState.fromVideoInfo(videoInfo)
        )
        downloader.enqueue(task, state)
        _uiState.value = MainUiState.Idle
    }

    /** Dismiss the bottom sheet / error and go back to idle. */
    fun resetState() {
        _uiState.value = MainUiState.Idle
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Error message builder
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildErrorMessage(th: Throwable): String {
        val msg = th.message ?: th.javaClass.simpleName
        return when {
            msg.contains("Unable to extract", ignoreCase = true) ->
                "Could not parse the URL. Make sure the link is supported and works in a browser."
            msg.contains("network", ignoreCase = true) ||
            msg.contains("connect", ignoreCase = true) ||
            msg.contains("timeout", ignoreCase = true) ->
                "Network error. Check your internet connection and try again."
            msg.contains("Video unavailable", ignoreCase = true) ||
            msg.contains("private", ignoreCase = true) ->
                "Video is unavailable or private. It may have been deleted."
            msg.contains("Sign in", ignoreCase = true) ->
                "This video requires login. Enable Cookies in Settings."
            else -> "Failed to fetch video info:\n$msg"
        }
    }
}

