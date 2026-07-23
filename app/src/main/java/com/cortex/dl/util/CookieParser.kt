package com.cortex.dl.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Handles parsing of user-supplied cookie text in multiple formats:
 * 1. Netscape / cookies.txt (tab-separated 7-field lines)
 * 2. JSON (array/object exported by browser extensions like Cookie-Editor / EditThisCookie)
 * 3. Header / name=value (raw HTTP Cookie request header string)
 */
object CookieParser {

    private val jsonFormat = Json { ignoreUnknownKeys = true }

    /** JSON shape produced by "Cookie-Editor" and "EditThisCookie" browser extensions. */
    @Serializable
    private data class CookieJson(
        val name: String = "",
        val value: String = "",
        val domain: String = "",
        val path: String = "/",
        val secure: Boolean = false,
        @SerialName("httpOnly") val httpOnly: Boolean = false,
        val expirationDate: Double = 0.0,
        val expires: Double = 0.0,
        val session: Boolean = true,
    )

    private fun extractHost(url: String): String {
        if (url.isBlank()) return ""
        return url.removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
            .substringBefore(':')
    }

    /**
     * Parses manually-pasted cookie text into a [List<Cookie>].
     *
     * @param profileUrl The URL of the CookieProfile — used to derive host/domain when missing.
     * @param content The raw text supplied by the user.
     */
    fun parseCookieContent(profileUrl: String, content: String): List<Cookie> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return emptyList()

        return runCatching {
            when {
                // JSON: starts with [ (array) or { (single object)
                trimmed.startsWith('[') || trimmed.startsWith('{') ->
                    parseJsonCookies(profileUrl, trimmed)

                // Netscape: contains at least one non-comment line with a tab character
                trimmed.lines().any { it.isNotBlank() && !it.startsWith('#') && '\t' in it } ->
                    parseNetscapeCookies(trimmed)

                // Fallback: treat as raw Cookie header "name=val; name=val"
                else -> parseHeaderCookies(profileUrl, trimmed)
            }
        }.getOrElse {
            runCatching { parseHeaderCookies(profileUrl, trimmed) }.getOrDefault(emptyList())
        }
    }

    fun parseJsonCookies(profileUrl: String, json: String): List<Cookie> {
        val normalised = if (json.trimStart().startsWith('{')) "[$json]" else json
        val items = jsonFormat.decodeFromString<List<CookieJson>>(normalised)
        val now = System.currentTimeMillis() / 1000L
        val host = extractHost(profileUrl).removePrefix("www.")
        val fallbackDomain = if (host.isNotEmpty()) ".$host" else ""

        return items.mapNotNull { c ->
            if (c.name.isEmpty()) return@mapNotNull null
            val expiry = when {
                c.expirationDate > 0 -> c.expirationDate.toLong()
                c.expires > 0        -> c.expires.toLong()
                else                 -> 0L
            }
            if (expiry > 0L && expiry < now) return@mapNotNull null // expired
            val domain = c.domain.let { d ->
                when {
                    d.isBlank()       -> fallbackDomain
                    d.startsWith('.') -> d
                    else              -> ".$d"
                }
            }
            Cookie(
                domain            = domain,
                name              = c.name,
                value             = c.value,
                includeSubdomains = true,
                path              = c.path.ifEmpty { "/" },
                secure            = c.secure,
                expiry            = expiry,
                isHttpOnly        = c.httpOnly,
            )
        }
    }

    fun parseNetscapeCookies(text: String): List<Cookie> {
        val now = System.currentTimeMillis() / 1000L
        val cookies = mutableListOf<Cookie>()
        text.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith('#')) return@forEach
            val parts = trimmed.split('\t')
            if (parts.size < 7) return@forEach
            val domain            = parts[0]
            val includeSubdomains = parts[1].uppercase() == "TRUE"
            val path              = parts[2]
            val secure            = parts[3].uppercase() == "TRUE"
            val expiry            = parts[4].toLongOrNull() ?: 0L
            val name              = parts[5]
            val value             = parts.drop(6).joinToString("\t")
            if (name.isEmpty()) return@forEach
            if (expiry > 0L && expiry < now) return@forEach // skip expired
            cookies.add(
                Cookie(
                    domain            = if (domain.startsWith('.')) domain else ".$domain",
                    name              = name,
                    value             = value,
                    includeSubdomains = includeSubdomains,
                    path              = path,
                    secure            = secure,
                    expiry            = expiry,
                    isHttpOnly        = false,
                )
            )
        }
        return cookies
    }

    fun parseHeaderCookies(profileUrl: String, header: String): List<Cookie> {
        val host   = extractHost(profileUrl)
        val cleanHost = host.removePrefix("www.")
        val domain = if (cleanHost.isNotEmpty()) ".$cleanHost" else ""
        val cookies = mutableListOf<Cookie>()
        val cookiePart = header.removePrefix("Cookie:").removePrefix("cookie:").trim()
        cookiePart.split(';').forEach { pair ->
            val eqIdx = pair.indexOf('=')
            if (eqIdx < 0) return@forEach
            val name  = pair.substring(0, eqIdx).trim()
            val value = pair.substring(eqIdx + 1).trim()
            if (name.isEmpty()) return@forEach
            cookies.add(
                Cookie(
                    domain            = domain,
                    name              = name,
                    value             = value,
                    includeSubdomains = true,
                    path              = "/",
                    secure            = false,
                    expiry            = 0L,
                    isHttpOnly        = false,
                )
            )
        }
        return cookies
    }
}
