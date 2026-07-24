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
                runCatching {
                    YoutubeDL.getInstance().version(this@App)?.let { version ->
                        PreferenceUtil.encodeString(YT_DLP_VERSION, version)
                    }
                }
                if (YT_DLP_AUTO_UPDATE.getBoolean()) {
                    runCatching { UpdateUtil.updateYtDlp() }
                }
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

        @SuppressLint("DiscouragedApi")
        fun getVersionReport(): String {
            return "App Version: ${packageInfo.versionName} (${packageInfo.versionCode})\n" +
                "Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
                "Device: ${Build.MANUFACTURER} ${Build.MODEL}\n" +
                "Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString()}"
        }

        val privateDownloadDir: String
            get() = getExternalPrivateDownloadDirectory().absolutePath

        fun isDebugBuild(): Boolean {
            return ((packageInfo.applicationInfo?.flags ?: 0) and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        }

        fun updateDownloadDir(uri: Uri, directory: Directory) {
            val contentResolver = context.contentResolver
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { contentResolver.takePersistableUriPermission(uri, takeFlags) }
            val path = FileUtil.getRealPath(uri)
            when (directory) {
                Directory.VIDEO -> {
                    VIDEO_DIRECTORY.updateString(path)
                    videoDownloadDir = path
                }
                Directory.AUDIO -> {
                    AUDIO_DIRECTORY.updateString(path)
                    audioDownloadDir = path
                }
                Directory.CUSTOM_COMMAND -> {
                    COMMAND_DIRECTORY.updateString(path)
                }
                Directory.SDCARD -> {
                    SDCARD_URI.updateString(uri.toString())
                }
            }
        }

        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }
}
