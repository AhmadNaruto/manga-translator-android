package com.manga.translate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun `long image decode is planned as short source tiles`() {
        val tiles = ReadingBitmapDecoder.planSourceTiles(
            sourceWidth = 1080,
            sourceHeight = 24000,
            sampleSize = 1
        )

        assertEquals(7, tiles.size)
        assertEquals(0, tiles.first().top)
        assertEquals(1080, tiles.first().right)
        assertEquals(24000, tiles.last().bottom)
        tiles.forEach { tile ->
            val height = tile.bottom - tile.top
            assertTrue(height in 1..3883)
        }
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
