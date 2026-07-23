package com.cortex.dl.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CookieParserTest {

    @Test
    fun parseCookieContent_emptyContent_returnsEmptyList() {
        val cookies = CookieParser.parseCookieContent("https://youtube.com", "")
        assertTrue(cookies.isEmpty())
    }

    @Test
    fun parseNetscapeCookies_validContent_parsesCorrectly() {
        val netscapeText = """
            # Netscape HTTP Cookie File
            .youtube.com	TRUE	/	TRUE	2147483647	VISITOR_INFO1_LIVE	abc123xyz
            .youtube.com	FALSE	/watch	FALSE	0	PREF	f1=50000
        """.trimIndent()

        val cookies = CookieParser.parseNetscapeCookies(netscapeText)

        assertEquals(2, cookies.size)
        assertEquals(".youtube.com", cookies[0].domain)
        assertEquals("VISITOR_INFO1_LIVE", cookies[0].name)
        assertEquals("abc123xyz", cookies[0].value)
        assertTrue(cookies[0].includeSubdomains)
        assertTrue(cookies[0].secure)

        assertEquals("PREF", cookies[1].name)
        assertEquals("f1=50000", cookies[1].value)
    }

    @Test
    fun parseHeaderCookies_validHeader_parsesCorrectly() {
        val headerText = "Cookie: session_id=12345; user_token=abcdef"
        val cookies = CookieParser.parseHeaderCookies("https://example.com", headerText)

        assertEquals(2, cookies.size)
        assertEquals("session_id", cookies[0].name)
        assertEquals("12345", cookies[0].value)
        assertEquals("user_token", cookies[1].name)
        assertEquals("abcdef", cookies[1].value)
    }
}
