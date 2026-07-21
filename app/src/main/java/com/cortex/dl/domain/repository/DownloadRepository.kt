package com.cortex.dl.domain.repository

import com.cortex.dl.util.DownloadUtil
import com.cortex.dl.util.VideoInfo

interface DownloadRepository {
    suspend fun fetchVideoInfo(
        url: String,
        preferences: DownloadUtil.DownloadPreferences
    ): Result<VideoInfo>
}

class DownloadRepositoryImpl : DownloadRepository {
    override suspend fun fetchVideoInfo(
        url: String,
        preferences: DownloadUtil.DownloadPreferences
    ): Result<VideoInfo> {
        return DownloadUtil.fetchVideoInfoFromUrl(url = url, preferences = preferences)
    }
}
