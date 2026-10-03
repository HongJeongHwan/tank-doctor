package io.github.hongjeonghwan.tankdoctor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateTest {
    @Test
    fun comparesNumbersNotText() {
        assertTrue(AppUpdate.isNewer("1.10.0", "1.9.2"))
        assertTrue(AppUpdate.isNewer("1.3.1", "1.3.0"))
        assertTrue(AppUpdate.isNewer("2.0", "1.9.9"))
        assertFalse(AppUpdate.isNewer("1.3.0", "1.3.0"))
        assertFalse(AppUpdate.isNewer("1.2.9", "1.3.0"))
        assertFalse(AppUpdate.isNewer("1.3", "1.3.0"))
    }

    @Test
    fun devBuildsCompareByNumbers() {
        assertTrue(AppUpdate.isNewer("1.3.0", "0.0.42-dev"))
    }

    private fun release(tag: String, vararg assets: String) = """
        {"tag_name":"$tag","body":"고친 점",
         "assets":[${assets.joinToString(",") { """{"name":"$it","browser_download_url":"https://x/$it"}""" }}]}
    """.trimIndent()

    @Test
    fun picksFixedNameApk() {
        val r = AppUpdate.parseRelease(release("v1.4.0", "TankDoctor-1.4.0.apk", "TankDoctor.apk"), "1.3.0")!!
        assertEquals("1.4.0", r.version)
        assertEquals("https://x/TankDoctor.apk", r.apkUrl)
        assertEquals("고친 점", r.notes)
    }

    @Test
    fun nothingWhenNotNewerOrNoApk() {
        assertNull(AppUpdate.parseRelease(release("v1.3.0", "TankDoctor.apk"), "1.3.0"))
        assertNull(AppUpdate.parseRelease(release("v1.4.0", "other.zip"), "1.3.0"))
    }
}
