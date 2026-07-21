package com.cortex.dl

import android.annotation.SuppressLint
import android.app.Application
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.content.getSystemService
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.google.android.material.color.DynamicColors
import com.cortex.dl.download.DownloaderV2
import com.cortex.dl.download.DownloaderV2Impl
import com.cortex.dl.util.AUDIO_DIRECTORY
import com.cortex.dl.util.COMMAND_DIRECTORY
import com.cortex.dl.util.DownloadUtil
import com.cortex.dl.util.FileUtil
import com.cortex.dl.util.FileUtil.createEmptyFile
import com.cortex.dl.util.FileUtil.getCookiesFile
import com.cortex.dl.util.FileUtil.getExternalDownloadDirectory
import com.cortex.dl.util.FileUtil.getExternalPrivateDownloadDirectory
import com.cortex.dl.util.NotificationUtil
import com.cortex.dl.util.PreferenceUtil
import com.cortex.dl.util.PreferenceUtil.getString
import com.cortex.dl.util.PreferenceUtil.updateString
import com.cortex.dl.util.makeToast
import com.cortex.dl.util.SDCARD_URI
import com.cortex.dl.util.UpdateUtil
import com.cortex.dl.util.VIDEO_DIRECTORY
import com.cortex.dl.util.YT_DLP_VERSION
import com.cortex.dl.util.YT_DLP_AUTO_UPDATE
import com.cortex.dl.util.PreferenceUtil.getBoolean
import com.tencent.mmkv.MMKV
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module

enum class Directory { VIDEO, AUDIO, CUSTOM_COMMAND, SDCARD }

class App : Application(), SingletonImageLoader.Factory {

    /**
     * Coil 3 does NOT bundle a network fetcher by default — without an explicit network
     * component every remote image (e.g. video thumbnails) silently fails to load, leaving
     * blank poster areas in the format/configure screens. We register the OkHttp fetcher and
     * attach a desktop browser User-Agent so thumbnail CDNs that reject the default client
     * (returning 403) still serve the image.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val callFactory = {
            OkHttpClient.Builder()
                .addNetworkInterceptor(
                    Interceptor { chain ->
                        val request =
                            chain
                                .request()
                                .newBuilder()
                                .header(
                                    "User-Agent",
                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                        "Chrome/124.0.0.0 Safari/537.36",
                                )
                                .build()
                        chain.proceed(request)
                    }
                )
                .build()
        }
        return ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = callFactory)) }
            .crossfade(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(
                module {
                    single<DownloaderV2> { DownloaderV2Impl(androidContext()) }
                }
            )
        }

        context = applicationContext
        packageInfo =
            packageManager.run {
                if (Build.VERSION.SDK_INT >= 33)
                    getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                else getPackageInfo(packageName, 0)
            }
        applicationScope = CoroutineScope(SupervisorJob())
        DynamicColors.applyToActivitiesIfAvailable(this)

        clipboard = getSystemService()!!
        connectivityManager = getSystemService()!!

        applicationScope.launch((Dispatchers.IO)) {
            try {
                YoutubeDL.init(this@App)
                FFmpeg.init(this@App)
                Aria2c.init(this@App)
                // Keep the extractor engine current before the user can enqueue a download.
                // This is intentionally best-effort: a failed network check must never block
                // opening the application or using the bundled version offline.
                if (YT_DLP_AUTO_UPDATE.getBoolean()) {
                    runCatching { UpdateUtil.updateYtDlp() }
                }
                // Pre-build the Netscape cookie file so it exists before the first
                // download. Wrapped in runCatching so a disk-full IOException here
                // does NOT propagate into the catch(Throwable) block above and
                // accidentally show the crash-report screen on the first app launch.
                runCatching {
                    DownloadUtil.getCookiesContentFromDatabase().getOrNull()?.let {
                        FileUtil.writeContentToFile(it, getCookiesFile())
                    }
                }
                UpdateUtil.deleteOutdatedApk()
                runCatching { FileUtil.cleanStaleTempFiles() }
            } catch (e: com.yausername.youtubedl_android.YoutubeDLException) {
                withContext(Dispatchers.Main) { startCrashReportActivity(e) }
            } catch (th: Throwable) {
                withContext(Dispatchers.Main) { startCrashReportActivity(th) }
            }
        }

        videoDownloadDir = VIDEO_DIRECTORY.getString(getExternalDownloadDirectory().absolutePath)

        audioDownloadDir = AUDIO_DIRECTORY.getString(File(videoDownloadDir, "Audio").absolutePath)
        if (!PreferenceUtil.containsKey(COMMAND_DIRECTORY)) {
            COMMAND_DIRECTORY.updateString(videoDownloadDir)
        }
        if (Build.VERSION.SDK_INT >= 26) NotificationUtil.createNotificationChannel()

        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            try {
                GlobalContext.getOrNull()?.get<DownloaderV2>()?.cleanup()
                startCrashReportActivity(e)
            } catch (secondary: Throwable) {
                secondary.printStackTrace()
            } finally {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        GlobalContext.getOrNull()?.get<DownloaderV2>()?.cleanup()
    }

    private fun startCrashReportActivity(th: Throwable) {
        th.printStackTrace()
        startActivity(
            Intent(this, CrashReportActivity::class.java)
                .setAction("$packageName.error_report")
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("error_report", getVersionReport() + "\n" + th.stackTraceToString())
                }
        )
    }

    companion object {
        lateinit var clipboard: ClipboardManager
        lateinit var videoDownloadDir: String
        lateinit var audioDownloadDir: String
        lateinit var applicationScope: CoroutineScope
        lateinit var connectivityManager: ConnectivityManager
        lateinit var packageInfo: PackageInfo

        @Volatile var isServiceRunning = false

        private val connection =
            object : ServiceConnection {
                override fun onServiceConnected(className: ComponentName, service: IBinder) {
                    val binder = service as DownloadService.DownloadServiceBinder
                    isServiceRunning = true
                }

                override fun onServiceDisconnected(arg0: ComponentName) {
                    // OS killed the service unexpectedly — allow startService() to restart it.
                    isServiceRunning = false
                }
            }

        fun startService() {
            if (isServiceRunning) return
            Intent(context.applicationContext, DownloadService::class.java).also { intent ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.applicationContext.startForegroundService(intent)
                } else {
                    context.applicationContext.startService(intent)
                }
                context.applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        }

        fun stopService() {
            if (!isServiceRunning) return
            try {
                isServiceRunning = false
                context.applicationContext.run { 
                    unbindService(connection) 
                    stopService(Intent(this, DownloadService::class.java))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val privateDownloadDir: String
            get() =
                getExternalPrivateDownloadDirectory().run {
                    createEmptyFile(".nomedia")
                    absolutePath
                }

        fun updateDownloadDir(uri: Uri, directoryType: Directory) {
            when (directoryType) {
                Directory.AUDIO -> {
                    if (!FileUtil.isPrimaryStorageUri(uri)) {
                        context.makeToast(R.string.directory_not_supported)
                        return
                    }
                    val path = FileUtil.getRealPath(uri)
                    audioDownloadDir = path
                    PreferenceUtil.encodeString(AUDIO_DIRECTORY, path)
                }

                Directory.VIDEO -> {
                    if (!FileUtil.isPrimaryStorageUri(uri)) {
                        context.makeToast(R.string.directory_not_supported)
                        return
                    }
                    val path = FileUtil.getRealPath(uri)
                    videoDownloadDir = path
                    PreferenceUtil.encodeString(VIDEO_DIRECTORY, path)
                }

                Directory.CUSTOM_COMMAND -> {
                    if (!FileUtil.isPrimaryStorageUri(uri)) {
                        context.makeToast(R.string.directory_not_supported)
                        return
                    }
                    val path = FileUtil.getRealPath(uri)
                    PreferenceUtil.encodeString(COMMAND_DIRECTORY, path)
                }

                Directory.SDCARD -> {
                    context.contentResolver?.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                    PreferenceUtil.encodeString(SDCARD_URI, uri.toString())
                }
            }
        }

        fun getVersionReport(): String {
            val versionName = packageInfo.versionName
            val page = packageInfo
            val versionCode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    packageInfo.versionCode.toLong()
                }
            val release =
                if (Build.VERSION.SDK_INT >= 30) {
                    Build.VERSION.RELEASE_OR_CODENAME
                } else {
                    Build.VERSION.RELEASE
                }
            return StringBuilder()
                .append("App version: $versionName ($versionCode)\n")
                .append("Device information: Android $release (API ${Build.VERSION.SDK_INT})\n")
                .append("Supported ABIs: ${Build.SUPPORTED_ABIS.contentToString()}\n")
                .append("Yt-dlp version: ${YT_DLP_VERSION.getString()}\n")
                .toString()
        }


        fun isDebugBuild(): Boolean = BuildConfig.DEBUG

        @SuppressLint("StaticFieldLeak") lateinit var context: Context
    }
}

