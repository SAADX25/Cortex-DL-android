package com.cortex.dl.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cortex.dl.database.objects.CommandTemplate
import com.cortex.dl.database.objects.CookieProfile
import com.cortex.dl.database.objects.DownloadedVideoInfo
import com.cortex.dl.database.objects.OptionShortcut

@Database(
    entities =
        [
            DownloadedVideoInfo::class,
            CommandTemplate::class,
            CookieProfile::class,
            OptionShortcut::class,
        ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoInfoDao(): VideoInfoDao
}