package com.manga.translate

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingBitmapDecoderTest {

    @Test
    fun `long image sampling preserves readable width`() {
        val sample = ReadingBitmapDecoder.calculateInSampleSize(
            sourceWidth = 1080,
            sourceHeight = 24000,
            targetWidth = 2160,
            targetHeight = 4800
        )

        assertEquals(1, sample)
    }

    @Test
    fun `regular image sampling still respects long edge guard`() {
        val sample = ReadingBitmapDecoder.calculateInSampleSize(
            sourceWidth = 12000,
            sourceHeight = 6000,
            targetWidth = 2160,
            targetHeight = 4800
        )

        assertEquals(2, sample)
    }
}
