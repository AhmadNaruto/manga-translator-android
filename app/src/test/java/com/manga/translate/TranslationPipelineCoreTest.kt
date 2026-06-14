package com.manga.translate

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test
import android.graphics.RectF

class TranslationPipelineCoreTest {
    @Test
    fun `page ocr result drops empty bubbles`() {
        val page = PageOcrResult(
            imageFile = File("page.jpg"),
            width = 1000,
            height = 1600,
            bubbles = listOf(
                OcrBubble(1, RectF(0f, 0f, 10f, 10f), "hello"),
                OcrBubble(2, RectF(0f, 0f, 10f, 10f), "")
            )
        )

        val filtered = page.withRecognizedTextBubblesOnly()

        assertEquals(1, filtered.bubbles.size)
    }

}
