package app.kitra.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageSizingTest {

    @Test
    fun keepsSmallImages() {
        assertEquals(ImageSizing.Size(800, 600), ImageSizing.targetSize(800, 600))
        assertEquals(ImageSizing.Size(1600, 900), ImageSizing.targetSize(1600, 900))
    }

    @Test
    fun scalesTypicalPhoneScreenshotKeepingAspectRatio() {
        assertEquals(ImageSizing.Size(720, 1600), ImageSizing.targetSize(1080, 2400))
        assertEquals(ImageSizing.Size(1600, 720), ImageSizing.targetSize(2400, 1080))
    }

    @Test
    fun sampleSizeNeverGoesBelowTarget() {
        assertEquals(1, ImageSizing.sampleSize(1080, 2400))
        assertEquals(2, ImageSizing.sampleSize(1600, 3200))
        assertEquals(2, ImageSizing.sampleSize(3000, 4000))
        assertEquals(4, ImageSizing.sampleSize(6400, 6400))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidSize() {
        ImageSizing.targetSize(0, 100)
    }
}
