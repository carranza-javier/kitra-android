package com.kettenki.kitra.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotFilterTest {
    private val now = 1_800_000_000L

    @Test
    fun acceptsCommonScreenshotFolders() {
        listOf(
            "Pictures/Screenshots/",
            "DCIM/Screenshots/",
            "Pictures/Screenshot/",
            "DCIM/Screen_shot/",
            "Pictures/ScreenCapture/",
        ).forEach { assertTrue(it, ScreenshotFilter.isScreenshot(it, "a.png", now - 2, now)) }
    }

    @Test
    fun rejectsOtherFolders() {
        listOf("DCIM/Camera/", "Pictures/WhatsApp/", "Download/", "Pictures/").forEach {
            assertFalse(it, ScreenshotFilter.isScreenshot(it, "Screenshot_1.png", now - 2, now))
        }
    }

    @Test
    fun rejectsOldImages() {
        assertFalse(ScreenshotFilter.isScreenshot("Pictures/Screenshots/", "s.png", now - 3600, now))
        assertFalse(ScreenshotFilter.isScreenshot("Pictures/Screenshots/", "s.png", now - 61, now))
    }

    @Test
    fun toleratesSmallClockSkew() {
        assertTrue(ScreenshotFilter.isScreenshot("Pictures/Screenshots/", "s.png", now + 5, now))
        assertFalse(ScreenshotFilter.isScreenshot("Pictures/Screenshots/", "s.png", now + 60, now))
    }

    @Test
    fun fallsBackToNameWhenPathMissing() {
        assertTrue(ScreenshotFilter.isScreenshot(null, "Screenshot_20261017.png", now, now))
        assertFalse(ScreenshotFilter.isScreenshot(null, "IMG_0001.jpg", now, now))
    }
}
