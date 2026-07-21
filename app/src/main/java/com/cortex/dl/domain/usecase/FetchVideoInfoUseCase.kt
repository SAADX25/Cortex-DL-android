package com.cortex.dl.domain.usecase

import com.cortex.dl.domain.repository.DownloadRepository
import com.cortex.dl.domain.repository.DownloadRepositoryImpl
import com.cortex.dl.util.DownloadUtil
import com.cortex.dl.util.VideoInfo

class FetchVideoInfoUseCase(
    private val repository: DownloadRepository = DownloadRepositoryImpl()
) {
    suspend operator fun invoke(
        url: String,
        preferences: DownloadUtil.DownloadPreferences = DownloadUtil.DownloadPreferences.createFromPreferences()
    ): Result<VideoInfo> {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) {
            return Result.failure(IllegalArgumentException("URL is empty. Please enter a valid video URL."))
        }
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            return Result.failure(IllegalArgumentException("Invalid URL. Must start with http:// or https://"))
        }
        return repository.fetchVideoInfo(cleanUrl, preferences)
    }
}
