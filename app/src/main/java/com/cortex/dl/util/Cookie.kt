package com.cortex.dl.util

/**
 * Represents a single HTTP cookie, compatible with the Netscape cookies.txt format
 * used by yt-dlp.
 *
 * Moved from ui.page.settings.network to util so that DownloadUtil can reference
 * it without depending on any UI layer.
 */
data class Cookie(
    val domain: String,
    val name: String,
    val value: String,
    val includeSubdomains: Boolean = true,
    val path: String = "/",
    val secure: Boolean = false,
    val expiry: Long = 0L,
    val isHttpOnly: Boolean = false,
) {
    /** Serialises this cookie to a single Netscape / cookies.txt line. */
    fun toNetscapeLine(): String = toNetscapeCookieString()

    /** Alias for [toNetscapeLine] — used by DownloadUtil.toCookiesFileContent(). */
    fun toNetscapeCookieString(): String =
        listOf(
            domain,
            if (includeSubdomains) "TRUE" else "FALSE",
            path,
            if (secure) "TRUE" else "FALSE",
            expiry.toString(),
            name,
            value,
        ).joinToString("\t")
}
