package com.manga.translate

import android.graphics.RectF
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PageRegionDetectorTest {

    @Test
    fun `long image tiling only enables for threshold-matching vertical pages`() {
        assertFalse(shouldUseLongImageTiling(pageWidth = 1400, pageHeight = 4095))
        assertFalse(shouldUseLongImageTiling(pageWidth = 1400, pageHeight = 4199))
        assertTrue(shouldUseLongImageTiling(pageWidth = 1400, pageHeight = 4200))
        assertTrue(shouldUseLongImageTiling(pageWidth = 1000, pageHeight = 4096))
    }

    @Test
    fun `long image tile plan fully covers page with overlap and unique starts`() {
        val tiles = planLongImageDetectionTiles(pageWidth = 1000, pageHeight = 7000)

        assertEquals(listOf(0, 1845, 3690, 5535), tiles.map { it.top })
        assertEquals(7000, tiles.last().bottom)
        assertEquals(7000, tiles.maxOf { it.bottom })
        assertEquals(tiles.map { it.top }.distinct().size, tiles.size)
        assertTrue(tiles.zipWithNext().all { (a, b) -> b.top < a.bottom })
    }

    @Test
    fun `tile mask contour remaps from tile normalized coordinates to page normalized coordinates`() {
        val remapped = remapTileMaskContourToPage(
            contour = floatArrayOf(0f, 0f, 1f, 1f, 0.5f, 0.5f),
            tileTop = 2000,
            tileHeight = 2500,
            pageWidth = 1000,
            pageHeight = 7000
        )

        assertArrayEquals(
            floatArrayOf(
                0f, 2000f / 7000f,
                1f, 4500f / 7000f,
                0.5f, 3250f / 7000f
            ),
            remapped,
            1e-4f
        )
    }

    @Test
    fun `bubble priority prefers higher confidence when gap exceeds threshold`() {
        val best = choosePreferredBubbleCandidateIndex(
            listOf(
                BubblePriorityCandidate(confidence = 0.70f, hasMaskContour = false, area = 100f),
                BubblePriorityCandidate(confidence = 0.73f, hasMaskContour = false, area = 80f)
            )
        )

        assertEquals(1, best)
    }

    @Test
    fun `bubble priority prefers contour then area when confidence gap is small`() {
        val contourPreferred = choosePreferredBubbleCandidateIndex(
            listOf(
                BubblePriorityCandidate(confidence = 0.80f, hasMaskContour = false, area = 200f),
                BubblePriorityCandidate(confidence = 0.81f, hasMaskContour = true, area = 150f)
            )
        )
        val areaPreferred = choosePreferredBubbleCandidateIndex(
            listOf(
                BubblePriorityCandidate(confidence = 0.80f, hasMaskContour = true, area = 120f),
                BubblePriorityCandidate(confidence = 0.80f, hasMaskContour = true, area = 180f)
            )
        )

        assertEquals(1, contourPreferred)
        assertEquals(1, areaPreferred)
    }

    @Test
    fun `bubble dedup matches highly overlapping or contained rectangles`() {
        val overlappingA = RectF(0f, 0f, 100f, 100f)
        val overlappingB = RectF(5f, 5f, 95f, 95f)
        val container = RectF(0f, 0f, 100f, 100f)
        val inside = RectF(5f, 5f, 95f, 95f)
        val separate = RectF(150f, 0f, 250f, 100f)

        assertTrue(shouldTreatRectsAsSameBubbleForDedup(overlappingA, overlappingB))
        assertTrue(shouldTreatRectsAsSameBubbleForDedup(container, inside))
        assertFalse(shouldTreatRectsAsSameBubbleForDedup(overlappingA, separate))
    }

    @Test
    fun `detection strategy tag switches between full and tiled modes`() {
        assertEquals("det_full_v1", buildDetectionStrategyTag(pageWidth = 1600, pageHeight = 3000))
        assertEquals("det_tiled_long_v1", buildDetectionStrategyTag(pageWidth = 1000, pageHeight = 4096))
    }
}
