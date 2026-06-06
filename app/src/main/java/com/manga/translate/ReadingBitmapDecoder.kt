package com.manga.translate

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.os.Build
import kotlin.math.max

data class DecodedReadingBitmap(
    val drawable: ReadingTiledBitmapDrawable,
    val bitmap: Bitmap?,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val displayWidth: Int,
    val displayHeight: Int,
    val isTiled: Boolean,
    val regionSource: ReadingRegionImageSource? = null
)

data class ReadingRegionImageSource(
    val imageFile: java.io.File,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val sampleSize: Int
)

internal data class ReadingSourceTile(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val height: Int
        get() = bottom - top

    fun toRect(): Rect = Rect(left, top, right, bottom)
}

object ReadingBitmapDecoder {
    private const val DETAIL_MULTIPLIER = 2
    private const val MAX_LONG_EDGE = 8192
    private const val MAX_TOTAL_PIXELS = 16_777_216 // ~16MP
    private const val TILE_DECODE_MIN_SOURCE_HEIGHT = 8192
    private const val TILE_OUTPUT_PIXEL_BUDGET = 4_194_304 // ~4MP per tile

    suspend fun decode(imageFile: java.io.File, targetWidth: Int, targetHeight: Int): DecodedReadingBitmap? {
        if (ImageFileSupport.isAvifFile(imageFile.name)) {
            val safeWidth = targetWidth.coerceAtLeast(1) * DETAIL_MULTIPLIER
            val safeHeight = targetHeight.coerceAtLeast(1) * DETAIL_MULTIPLIER
            val (bitmap, size) = AvifBitmapDecoder.decodeSampled(imageFile, safeWidth, safeHeight)
            if (bitmap == null || size == null) return null
            return toDecodedReadingBitmap(
                bitmap = bitmap,
                sourceWidth = size.width,
                sourceHeight = size.height
            )
        }
        val safeTargetWidth = targetWidth.coerceAtLeast(1)
        val safeTargetHeight = targetHeight.coerceAtLeast(1)
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(imageFile.absolutePath, bounds)
        val sourceWidth = bounds.outWidth
        val sourceHeight = bounds.outHeight
        if (sourceWidth <= 0 || sourceHeight <= 0) return null
        val sampleSize = calculateInSampleSize(
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            targetWidth = safeTargetWidth * DETAIL_MULTIPLIER,
            targetHeight = safeTargetHeight * DETAIL_MULTIPLIER
        )
        if (shouldUseTiledDecode(sourceWidth, sourceHeight, sampleSize)) {
            val displayWidth = ceilDiv(sourceWidth, sampleSize)
            val displayHeight = ceilDiv(sourceHeight, sampleSize)
            return DecodedReadingBitmap(
                drawable = ReadingTiledBitmapDrawable.empty(displayWidth, displayHeight),
                bitmap = null,
                sourceWidth = sourceWidth,
                sourceHeight = sourceHeight,
                displayWidth = displayWidth,
                displayHeight = displayHeight,
                isTiled = true,
                regionSource = ReadingRegionImageSource(
                    imageFile = imageFile,
                    sourceWidth = sourceWidth,
                    sourceHeight = sourceHeight,
                    sampleSize = sampleSize
                )
            )
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bitmap = ImageProcessingGuards.withDecodePermit(
            width = sourceWidth,
            height = sourceHeight,
            tag = "ReadingDecoder"
        ) {
            BitmapFactory.decodeFile(imageFile.absolutePath, options)
        } ?: return null
        return toDecodedReadingBitmap(
            bitmap = bitmap,
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight
        )
    }

    private fun toDecodedReadingBitmap(
        bitmap: Bitmap,
        sourceWidth: Int,
        sourceHeight: Int
    ): DecodedReadingBitmap {
        bitmap.density = Bitmap.DENSITY_NONE
        return DecodedReadingBitmap(
            drawable = ReadingTiledBitmapDrawable.single(bitmap),
            bitmap = bitmap,
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            displayWidth = bitmap.width,
            displayHeight = bitmap.height,
            isTiled = false
        )
    }

    internal fun calculateInSampleSize(
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        val preserveReadableWidth = shouldUseLongImageTiling(sourceWidth, sourceHeight)
        val minReadableWidth = (targetWidth / DETAIL_MULTIPLIER).coerceAtLeast(1)
        var sample = 1
        while (
            sourceWidth / (sample * 2) >= targetWidth &&
            sourceHeight / (sample * 2) >= targetHeight
        ) {
            sample *= 2
        }
        while (
            sourceWidth / (sample * 2) >= MAX_LONG_EDGE ||
            sourceHeight / (sample * 2) >= MAX_LONG_EDGE
        ) {
            if (preserveReadableWidth && sourceWidth / (sample * 2) < minReadableWidth) {
                break
            }
            sample *= 2
        }
        while (
            sourceWidth.toLong() * sourceHeight.toLong() / ((sample * 2).toLong() * (sample * 2).toLong())
            > MAX_TOTAL_PIXELS
        ) {
            if (preserveReadableWidth && sourceWidth / (sample * 2) < minReadableWidth) {
                break
            }
            sample *= 2
        }
        return max(sample, 1)
    }

    internal fun planSourceTiles(
        sourceWidth: Int,
        sourceHeight: Int,
        sampleSize: Int
    ): List<ReadingSourceTile> {
        if (sourceWidth <= 0 || sourceHeight <= 0 || sampleSize <= 0) return emptyList()
        val sourceTileHeight = computeSourceTileHeight(
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            sampleSize = sampleSize
        )
        val tiles = ArrayList<ReadingSourceTile>()
        var sourceTop = 0
        while (sourceTop < sourceHeight) {
            val sourceBottom = minOf(sourceTop + sourceTileHeight, sourceHeight)
            tiles += ReadingSourceTile(
                left = 0,
                top = sourceTop,
                right = sourceWidth,
                bottom = sourceBottom
            )
            sourceTop = sourceBottom
        }
        return tiles
    }

    private fun shouldUseTiledDecode(sourceWidth: Int, sourceHeight: Int, sampleSize: Int): Boolean {
        if (sampleSize <= 1) return sourceHeight >= TILE_DECODE_MIN_SOURCE_HEIGHT
        val decodedHeight = ceilDiv(sourceHeight, sampleSize)
        return sourceHeight >= TILE_DECODE_MIN_SOURCE_HEIGHT || decodedHeight >= TILE_DECODE_MIN_SOURCE_HEIGHT / 2
    }

    private suspend fun decodeTiled(
        imageFile: java.io.File,
        sourceWidth: Int,
        sourceHeight: Int,
        sampleSize: Int
    ): DecodedReadingBitmap? {
        val outputWidth = ceilDiv(sourceWidth, sampleSize)
        val outputHeight = ceilDiv(sourceHeight, sampleSize)
        return ImageProcessingGuards.withDecodePermit(
            width = outputWidth,
            height = outputHeight,
            tag = "ReadingDecoderTiled"
        ) {
            val regionDecoder = runCatching {
                createBitmapRegionDecoder(imageFile)
            }.getOrNull() ?: return@withDecodePermit null
            val tiles = ArrayList<ReadingBitmapTile>()
            try {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                var outputTop = 0
                for (sourceTile in planSourceTiles(sourceWidth, sourceHeight, sampleSize)) {
                    val tile = runCatching {
                        regionDecoder.decodeRegion(sourceTile.toRect(), options)
                    }.getOrNull()
                    if (tile == null) {
                        tiles.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
                        return@withDecodePermit null
                    }
                    tile.density = Bitmap.DENSITY_NONE
                    tiles += ReadingBitmapTile(bitmap = tile, top = outputTop)
                    outputTop += tile.height
                }
                if (tiles.isEmpty()) return@withDecodePermit null
                DecodedReadingBitmap(
                    drawable = ReadingTiledBitmapDrawable(
                        tiles = tiles,
                        imageWidth = tiles.maxOf { it.bitmap.width },
                        imageHeight = outputTop
                    ),
                    bitmap = null,
                    sourceWidth = sourceWidth,
                    sourceHeight = sourceHeight,
                    displayWidth = tiles.maxOf { it.bitmap.width },
                    displayHeight = outputTop,
                    isTiled = true
                )
            } finally {
                regionDecoder.recycle()
            }
        }
    }

    private fun computeSourceTileHeight(
        sourceWidth: Int,
        sourceHeight: Int,
        sampleSize: Int
    ): Int {
        val safeWidth = sourceWidth.coerceAtLeast(1)
        val sampledBudget = TILE_OUTPUT_PIXEL_BUDGET.toLong() * sampleSize.toLong() * sampleSize.toLong()
        val rawHeight = (sampledBudget / safeWidth).toInt()
        val roundedHeight = (rawHeight / sampleSize).coerceAtLeast(1) * sampleSize
        return roundedHeight.coerceAtLeast(sampleSize * 256).coerceAtMost(sourceHeight)
    }

    private fun createBitmapRegionDecoder(imageFile: java.io.File): BitmapRegionDecoder {
        val path = imageFile.absolutePath
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BitmapRegionDecoder.newInstance(path)
        } else {
            @Suppress("DEPRECATION")
            BitmapRegionDecoder.newInstance(path, false)
        }
    }

    private fun ceilDiv(value: Int, divisor: Int): Int {
        return (value + divisor - 1) / divisor
    }
}
