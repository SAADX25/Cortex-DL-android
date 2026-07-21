package com.cortex.dl.download

import android.app.PendingIntent
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.cortex.dl.App
import com.cortex.dl.R
import com.cortex.dl.download.Task.DownloadState
import com.cortex.dl.download.Task.DownloadState.Canceled
import com.cortex.dl.download.Task.DownloadState.Completed
import com.cortex.dl.download.Task.DownloadState.Error
import com.cortex.dl.download.Task.DownloadState.FetchingInfo
import com.cortex.dl.download.Task.DownloadState.Idle
import com.cortex.dl.download.Task.DownloadState.Paused
import com.cortex.dl.download.Task.DownloadState.ReadyWithInfo
import com.cortex.dl.download.Task.DownloadState.Running
import com.cortex.dl.download.Task.RestartableAction.Download
import com.cortex.dl.download.Task.RestartableAction.FetchInfo
import com.cortex.dl.download.Task.TypeInfo
import com.cortex.dl.download.Task.PauseReason
import com.cortex.dl.util.DownloadUtil
import com.cortex.dl.util.FileUtil
import com.cortex.dl.util.MAX_CONCURRENT_DOWNLOADS
import com.cortex.dl.util.NotificationUtil
import com.cortex.dl.util.PreferenceUtil
import com.cortex.dl.util.PreferenceUtil.getInt
import com.cortex.dl.util.VideoInfo
import com.yausername.youtubedl_android.YoutubeDL
import kotlin.collections.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "CortexEngine"

// ─────────────────────────────────────────────────────────────────────────────
// Public interface — consumed by UI and DI
// ─────────────────────────────────────────────────────────────────────────────

interface DownloaderV2 {
    fun getTaskStateMap(): SnapshotStateMap<Task, Task.State>

    fun cancel(task: Task): Boolean
    fun cancel(taskId: String): Boolean =
        getTaskStateMap().keys.find { it.id == taskId }?.let { cancel(it) } ?: false

    fun pause(task: Task): Boolean
    fun pause(taskId: String): Boolean =
        getTaskStateMap().keys.find { it.id == taskId }?.let { pause(it) } ?: false

    fun resume(task: Task): Boolean
    fun resume(taskId: String): Boolean =
        getTaskStateMap().keys.find { it.id == taskId }?.let { resume(it) } ?: false

    fun restart(task: Task)

    fun enqueue(task: Task)
    fun enqueue(task: Task, state: Task.State)
    fun enqueue(taskWithState: TaskFactory.TaskWithState) {
        enqueue(taskWithState.task, taskWithState.state)
    }

    fun remove(task: Task): Boolean
    fun cleanup() {}
}

// ─────────────────────────────────────────────────────────────────────────────
// Engine implementation
// ─────────────────────────────────────────────────────────────────────────────

class DownloaderV2Impl(private val appContext: Context) : DownloaderV2, KoinComponent {

    // ── Core state ──────────────────────────────────────────────────────────
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val taskStateMap = mutableStateMapOf<Task, Task.State>()
    private val snapshotFlow = snapshotFlow { taskStateMap.toMap() }

    // ── Progress tracking ───────────────────────────────────────────────────
    private val resumedProgressMap = ConcurrentHashMap<String, Float>()
    private val lastUiProgressUpdates = ConcurrentHashMap<String, Long>()
    private val lastUiProgressPercentages = ConcurrentHashMap<String, Float>()

    // ── Retry logic ─────────────────────────────────────────────────────────
    private val retryCountMap = ConcurrentHashMap<String, Int>()

    // ── Network monitoring ──────────────────────────────────────────────────
    private var networkPauseJob: Job? = null
    @Volatile private var networkDegradedAtMs: Long = 0L

    companion object {
        private const val MAX_AUTO_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 3_000L
        /** Progress updates are throttled to this interval to avoid flooding Compose. */
        private const val PROGRESS_THROTTLE_MS = 1000L
        /** Error messages matching these indicate a transient network issue. */
        private val NETWORK_ERROR_PATTERNS = listOf(
            "Unable to connect", "Connection reset", "timed out",
            "HTTP Error 5", "Network is unreachable",
            "Failed to establish", "RemoteDisconnected", "SSLError"
        )
    }



    /** React to state-type changes: schedule new work & manage foreground service. */
    private fun observeStateChanges() {
        scope.launch(Dispatchers.Default) {
            snapshotFlow
                .map { map -> map.mapValues { (_, s) -> s.downloadState::class } }
                .distinctUntilChanged()
                .onEach { scheduleWork() }
                .map { it.values.count { cls -> cls == Running::class || cls == FetchingInfo::class } }
                .distinctUntilChanged()
                .collect { active -> if (active > 0) App.startService() else App.stopService() }
        }
    }

    /** Persist non-completed tasks to MMKV whenever a structural change occurs. */
    private fun observeBackupWrites() {
        scope.launch(Dispatchers.IO) {
            // Restore persisted tasks first
            restoreFromBackup()

            snapshotFlow
                .map { map ->
                    map.filter { (_, s) -> s.downloadState !is Completed }
                        .mapValues { (_, s) ->
                            s.copy(downloadState = when (val ds = s.downloadState) {
                                is Running -> ds.copy(progress = -1f, progressText = "")
                                else -> ds
                            })
                        }
                }
                .distinctUntilChanged()
                .collect {
                    val original = taskStateMap.filter { (_, s) -> s.downloadState !is Completed }
                    PreferenceUtil.encodeTaskListBackup(original)
                }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Backup / Restore
    // ─────────────────────────────────────────────────────────────────────────

    private fun restoreFromBackup() {
        PreferenceUtil.decodeTaskListBackup()
            .filter { it.value.downloadState !is Completed }
            .mapValues { (_, state) ->
                val restored = when (val ds = state.downloadState) {
                    is FetchingInfo, Idle -> Canceled(action = FetchInfo)
                    is Running -> Paused(action = Download, progress = ds.progress)
                    ReadyWithInfo -> Paused(action = Download, progress = null)
                    is Paused -> ds
                    else -> ds
                }
                state.copy(downloadState = restored)
            }
            .forEach(::enqueue)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    override fun getTaskStateMap(): SnapshotStateMap<Task, Task.State> = taskStateMap

    override fun enqueue(task: Task) {
        taskStateMap += task to Task.State(Idle, null, Task.ViewState(url = task.url, title = task.url))
    }

    override fun enqueue(task: Task, state: Task.State) {
        taskStateMap += task to state
    }

    override fun remove(task: Task): Boolean = taskStateMap.remove(task) != null

    override fun cancel(task: Task): Boolean = task.cancelImpl()
    override fun pause(task: Task): Boolean = task.pauseImpl()
    override fun resume(task: Task): Boolean = task.resumeImpl()
    override fun restart(task: Task) = task.restartImpl()

    override fun cleanup() {
        runCatching { App.connectivityManager.unregisterNetworkCallback(networkCallback) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // State accessors (extension properties for cleaner code)
    // ─────────────────────────────────────────────────────────────────────────

    private var Task.state: Task.State
        get() = taskStateMap[this]!!
        set(value) { taskStateMap[this] = value }

    private var Task.downloadState: DownloadState
        get() = state.downloadState
        set(value) { taskStateMap[this] = state.copy(downloadState = value) }

    private var Task.info: VideoInfo?
        get() = state.videoInfo
        set(value) { taskStateMap[this] = state.copy(videoInfo = value) }

    private var Task.viewState: Task.ViewState
        get() = state.viewState
        set(value) { taskStateMap[this] = state.copy(viewState = value) }

    private val Task.notificationId: Int get() = id.hashCode()

    // ─────────────────────────────────────────────────────────────────────────
    // Work scheduler — the heart of the engine
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scans the task map and starts the next eligible task if concurrency allows.
     * Respects network restrictions and max concurrent download limits.
     */
    private fun scheduleWork() {
        if (!PreferenceUtil.isNetworkAvailableForDownload()) return

        val maxConcurrency = MAX_CONCURRENT_DOWNLOADS.getInt()
        val effectiveLimit = if (maxConcurrency == 0) Int.MAX_VALUE else maxConcurrency
        val activeCount = taskStateMap.count { (_, s) ->
            s.downloadState is Running || s.downloadState is FetchingInfo
        }
        if (activeCount >= effectiveLimit) return

        taskStateMap.entries
            .sortedBy { (_, s) -> s.downloadState }
            .firstOrNull { (_, s) -> s.downloadState == ReadyWithInfo || s.downloadState == Idle }
            ?.let { (task, s) ->
                when (s.downloadState) {
                    Idle -> task.prepare()
                    ReadyWithInfo -> task.download()
                    else -> error("Unexpected state in scheduleWork")
                }
            }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Task lifecycle: Prepare → Fetch Info → Download
    // ─────────────────────────────────────────────────────────────────────────

    private fun Task.prepare() {
        check(downloadState == Idle)
        if (type is TypeInfo.CustomCommand) execute() else fetchInfo()
    }

    private fun Task.fetchInfo() {
        check(downloadState == Idle)
        val task = this
        val playlistIndex = (type as? TypeInfo.Playlist)?.index

        scope.launch(Dispatchers.Default) {
            DownloadUtil.fetchVideoInfoFromUrl(
                url = url,
                playlistIndex = playlistIndex,
                preferences = preferences,
                taskKey = id,
            ).onSuccess { videoInfo ->
                info = videoInfo
                downloadState = ReadyWithInfo
                viewState = Task.ViewState.fromVideoInfo(videoInfo)
            }.onFailure { throwable ->
                if (throwable is YoutubeDL.CanceledException) return@onFailure
                task.handleFetchError(throwable)
            }
        }.also { job ->
            downloadState = FetchingInfo(job = job, taskId = id)
        }
    }

    private fun Task.download() {
        check(downloadState == ReadyWithInfo && info != null)
        if (type is TypeInfo.CustomCommand) { execute(); return }

        scope.launch(Dispatchers.Default) {
            DownloadUtil.downloadVideo(
                videoInfo = info,
                taskId = id,
                downloadPreferences = preferences,
                progressCallback = { pct, _, text ->
                    onProgress(pct, text)
                },
            ).onSuccess { pathList ->
                onDownloadSuccess(pathList)
            }.onFailure { throwable ->
                if (throwable is YoutubeDL.CanceledException) return@onFailure
                handleDownloadError(throwable)
            }
        }.also { job ->
            val initialProgress = resumedProgressMap.remove(id) ?: -1f
            downloadState = Running(job = job, taskId = id, progress = initialProgress)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Progress & Completion handlers
    // ─────────────────────────────────────────────────────────────────────────

    private fun Task.onProgress(percentageRaw: Float, rawText: String) {
        val progress = percentageRaw / 100f
        val cleanText = rawText
            .removePrefix("[download] ")
            .removePrefix("[download]")
            .trim()

        val preState = downloadState
        if (preState !is Running) return

        // Throttle UI updates to avoid flooding Compose recomposition
        val now = System.currentTimeMillis()
        val previousTime = lastUiProgressUpdates[id] ?: 0L
        val previousPercentage = lastUiProgressPercentages[id] ?: 0f

        val timeElapsed = now - previousTime >= PROGRESS_THROTTLE_MS
        val progressIncreased = (percentageRaw - previousPercentage) >= 5f

        if (percentageRaw >= 100f || timeElapsed || progressIncreased) {
            lastUiProgressUpdates[id] = now
            lastUiProgressPercentages[id] = percentageRaw
            downloadState = preState.copy(progress = progress, progressText = cleanText)

            val notification = NotificationUtil.notifyProgress(
                notificationId = notificationId,
                progress = percentageRaw.toInt(),
                text = cleanText,
                title = viewState.title,
                taskId = id,
            )
            if (notification != null) {
                com.cortex.dl.DownloadService.updateForegroundNotification(notificationId, notification)
            }
        }
    }

    private fun Task.onDownloadSuccess(pathList: List<String>) {
        lastUiProgressUpdates.remove(id)
        lastUiProgressPercentages.remove(id)
        retryCountMap.remove(id)
        downloadState = Completed(pathList.firstOrNull())

        val text = appContext.getString(
            if (pathList.isEmpty()) R.string.status_completed
            else R.string.download_finish_notification
        )
        FileUtil.createIntentForOpeningFile(pathList.firstOrNull()).run {
            NotificationUtil.finishNotification(
                notificationId,
                title = viewState.title,
                text = text,
                intent = if (this != null)
                    PendingIntent.getActivity(appContext, 0, this, PendingIntent.FLAG_IMMUTABLE)
                else null,
            )
        }
        if (preferences.downloadDocs && info != null) {
            DownloadUtil.writeDocsTextFile(info!!)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Error handling with exponential backoff retry
    // ─────────────────────────────────────────────────────────────────────────

    private fun isNetworkError(throwable: Throwable): Boolean =
        throwable.message?.let { msg ->
            NETWORK_ERROR_PATTERNS.any { msg.contains(it, ignoreCase = true) }
        } ?: false

    private fun Task.handleFetchError(throwable: Throwable) {
        if (!PreferenceUtil.isNetworkAvailableForDownload()) {
            handleNetworkUnavailable(FetchInfo, null)
            return
        }
        downloadState = Error(throwable = throwable, action = FetchInfo)
        NotificationUtil.notifyError(
            title = viewState.title,
            textId = R.string.fetch_info_error_msg,
            notificationId = notificationId,
            report = throwable.message ?: "Unknown error",
        )
    }

    private fun Task.handleDownloadError(throwable: Throwable) {
        val retries = retryCountMap.getOrDefault(id, 0)
        val isNetErr = isNetworkError(throwable)
        val networkUnavailable = !PreferenceUtil.isNetworkAvailableForDownload()

        when {
            // Network completely lost → pause for network
            networkUnavailable -> {
                ensureNetworkDegradedStart()
                retryCountMap.remove(id)
                val progress = (downloadState as? Running)?.progress
                handleNetworkUnavailable(Download, progress)
            }
            // Transient network error & retries remain → exponential backoff
            isNetErr && retries < MAX_AUTO_RETRIES -> {
                val attempt = retries + 1
                retryCountMap[id] = attempt
                Log.d(TAG, "Retry $attempt/$MAX_AUTO_RETRIES for ${viewState.title}")

                val preState = downloadState
                if (preState is Running) {
                    downloadState = preState.copy(
                        progressText = "Retrying ($attempt/$MAX_AUTO_RETRIES)..."
                    )
                }
                scope.launch {
                    delay(INITIAL_RETRY_DELAY_MS * attempt) // exponential backoff
                    if (downloadState is Running) downloadState = ReadyWithInfo
                }
            }
            // Non-recoverable error
            else -> {
                retryCountMap.remove(id)
                downloadState = Error(throwable = throwable, action = Download)
                NotificationUtil.notifyError(
                    title = viewState.title,
                    textId = R.string.download_error_msg,
                    notificationId = notificationId,
                    report = throwable.message ?: "Unknown error",
                    errorMessage = throwable.message ?: "Unknown error",
                )
            }
        }
    }

    /**
     * Transitions the current task into a network-paused state.
     * Called when a download or fetch fails due to no connectivity.
     */
    private fun Task.handleNetworkUnavailable(action: Task.RestartableAction, progress: Float?) {
        ensureNetworkDegradedStart()
        val preState = downloadState
        if (preState is DownloadState.Cancelable) {
            pauseForReason(preState, PauseReason.Network)
        } else {
            downloadState = Paused(action = action, progress = progress, reason = PauseReason.Network)
        }
        NotificationUtil.updateNotification(
            notificationId = notificationId,
            title = viewState.title,
            text = appContext.getString(R.string.status_paused),
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pause / Resume / Cancel / Restart
    // ─────────────────────────────────────────────────────────────────────────

    private fun Task.pauseImpl(): Boolean {
        val preState = downloadState
        return if (preState is DownloadState.Cancelable) {
            pauseForReason(preState, PauseReason.User)
        } else false
    }

    private fun Task.pauseForReason(
        preState: DownloadState.Cancelable,
        reason: PauseReason,
    ): Boolean {
        val destroyed = YoutubeDL.destroyProcessById(preState.taskId)
        if (destroyed) {
            preState.job.cancel()
            val progress = (preState as? Running)?.progress
            NotificationUtil.updateNotification(
                notificationId = notificationId,
                title = viewState.title,
                text = appContext.getString(R.string.status_paused),
            )
            downloadState = Paused(action = preState.action, progress = progress, reason = reason)
        }
        return destroyed
    }

    private fun Task.resumeImpl(): Boolean {
        val preState = downloadState
        if (preState !is Paused) return false

        preState.progress?.let { resumedProgressMap[id] = it }
        downloadState = when (preState.action) {
            Download -> ReadyWithInfo
            FetchInfo -> Idle
        }
        return true
    }

    private fun Task.cancelImpl(): Boolean {
        when (val preState = downloadState) {
            is DownloadState.Cancelable -> {
                val destroyed = YoutubeDL.destroyProcessById(preState.taskId)
                if (destroyed) {
                    preState.job.cancel()
                    NotificationUtil.cancelNotification(notificationId)
                    downloadState = Canceled(
                        action = preState.action,
                        progress = (preState as? Running)?.progress
                    )
                }
                return destroyed
            }
            Idle -> downloadState = Canceled(action = FetchInfo)
            ReadyWithInfo -> downloadState = Canceled(action = Download)
            is Paused -> {
                NotificationUtil.cancelNotification(notificationId)
                downloadState = Canceled(action = preState.action, progress = preState.progress)
                return true
            }
            else -> return false
        }
        return true
    }

    private fun Task.restartImpl() {
        val preState = downloadState
        check(preState is DownloadState.Restartable)
        downloadState = when (preState.action) {
            Download -> ReadyWithInfo
            FetchInfo -> Idle
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Custom command execution
    // ─────────────────────────────────────────────────────────────────────────

    private fun Task.execute() {
        check(downloadState == Idle)
        check(type is TypeInfo.CustomCommand)
        val template = type.template

        scope.launch {
            DownloadUtil.executeCustomCommandTask(url, id, template, preferences) { pct, _, text ->
                val progress = pct / 100f
                val preState = downloadState
                if (preState is Running) {
                    downloadState = preState.copy(progress = progress, progressText = text)
                    NotificationUtil.makeNotificationForCustomCommand(
                        notificationId = notificationId,
                        taskId = id,
                        progress = pct.toInt(),
                        templateName = template.name,
                        taskUrl = url,
                        text = text,
                    )
                }
            }.onFailure { throwable ->
                if (throwable is YoutubeDL.CanceledException) return@onFailure
                downloadState = Error(throwable = throwable, action = Download)
                NotificationUtil.notifyError(
                    title = viewState.title,
                    textId = R.string.download_error_msg,
                    notificationId = notificationId,
                    report = throwable.message ?: "Unknown error",
                    errorMessage = throwable.message ?: "Unknown error",
                )
            }.onSuccess {
                downloadState = Completed(null)
                NotificationUtil.finishNotification(
                    notificationId = notificationId,
                    title = viewState.title,
                    text = appContext.getString(R.string.status_completed),
                    intent = null,
                )
            }
        }.also { downloadState = Running(job = it, taskId = id) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Network monitoring
    // ─────────────────────────────────────────────────────────────────────────

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            clearNetworkDegraded()
            resumeNetworkPausedTasks()
            scheduleWork()
        }

        override fun onLost(network: Network) {
            markNetworkDegraded()
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            if (validated) {
                clearNetworkDegraded()
                resumeNetworkPausedTasks()
                scheduleWork()
            } else {
                markNetworkDegraded()
            }
        }
    }

    private fun markNetworkDegraded() {
        if (networkDegradedAtMs == 0L) networkDegradedAtMs = System.currentTimeMillis()
        scheduleNetworkPause()
    }

    private fun ensureNetworkDegradedStart() {
        if (networkDegradedAtMs == 0L) {
            networkDegradedAtMs = System.currentTimeMillis()
            scheduleNetworkPause()
        }
    }

    private fun clearNetworkDegraded() {
        networkDegradedAtMs = 0L
        networkPauseJob?.cancel()
    }

    private fun scheduleNetworkPause() {
        networkPauseJob?.cancel()
        val startAt = networkDegradedAtMs
        if (startAt == 0L) return
        val pauseDelayMs = PreferenceUtil.getNetworkPauseDelayMs()

        networkPauseJob = scope.launch {
            val elapsed = System.currentTimeMillis() - startAt
            delay((pauseDelayMs - elapsed).coerceAtLeast(0L))
            if (!PreferenceUtil.isNetworkAvailableForDownload() && networkDegradedAtMs == startAt) {
                pauseAllRunningForNetwork()
            }
        }
    }

    private fun pauseAllRunningForNetwork() {
        taskStateMap.entries.forEach { (task, state) ->
            when (val ds = state.downloadState) {
                is DownloadState.Cancelable -> task.pauseForReason(ds, PauseReason.Network)
                is ReadyWithInfo -> task.downloadState = Paused(action = Download, progress = null, reason = PauseReason.Network)
                is Idle -> task.downloadState = Paused(action = FetchInfo, progress = null, reason = PauseReason.Network)
                else -> { /* already paused/cancelled/completed/error — skip */ }
            }
        }
    }

    private fun resumeNetworkPausedTasks() {
        if (!PreferenceUtil.isNetworkAvailableForDownload()) return
        taskStateMap.entries.forEach { (task, state) ->
            val ds = state.downloadState
            if (ds is Paused && ds.reason == PauseReason.Network) {
                task.downloadState = when (ds.action) {
                    Download -> ReadyWithInfo
                    FetchInfo -> Idle
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Initialization
    // ─────────────────────────────────────────────────────────────────────────

    init {
        App.connectivityManager.registerDefaultNetworkCallback(networkCallback)
        observeStateChanges()
        observeBackupWrites()
    }
}
