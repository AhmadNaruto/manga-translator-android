package com.manga.translate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmClientEndpointTest {
    @Test
    fun `openai compatible chat endpoint keeps v1 base behavior`() {
        assertEquals(
            "https://api.siliconflow.cn/v1/chat/completions",
            LlmClient.buildOpenAiCompatibleChatEndpoint("https://api.siliconflow.cn/v1")
        )
    }

    @Test
    fun `openai compatible chat endpoint supports zhipu paas v4 base`() {
        assertEquals(
            "https://open.bigmodel.cn/api/paas/v4/chat/completions",
            LlmClient.buildOpenAiCompatibleChatEndpoint("https://open.bigmodel.cn/api/paas/v4")
        )
        assertEquals(
            "https://open.bigmodel.cn/api/coding/paas/v4/chat/completions",
            LlmClient.buildOpenAiCompatibleChatEndpoint("https://open.bigmodel.cn/api/coding/paas/v4")
        )
    }

    @Test
    fun `openai compatible chat endpoint normalizes full chat url`() {
        assertEquals(
            "https://open.bigmodel.cn/api/paas/v4/chat/completions",
            LlmClient.buildOpenAiCompatibleChatEndpoint(
                "https://open.bigmodel.cn/api/paas/v4/chat/completions"
            )
        )
    }

    @Test
    fun `openai compatible model list endpoint remains available for v1 providers`() {
        assertEquals(
            "https://api.siliconflow.cn/v1/models",
            LlmClient.buildOpenAiCompatibleModelsEndpoint("https://api.siliconflow.cn/v1")
        )
    }

    @Test
    fun `openai compatible model list endpoint is disabled for zhipu paas v4`() {
        assertNull(LlmClient.buildOpenAiCompatibleModelsEndpoint("https://open.bigmodel.cn/api/paas/v4"))
        assertTrue(LlmClient.isBigModelOpenAiCompatibleBaseUrl("https://open.bigmodel.cn/api/paas/v4"))
        assertTrue(LlmClient.isBigModelOpenAiCompatibleBaseUrl("https://open.bigmodel.cn/api/coding/paas/v4"))
        assertFalse(LlmClient.isBigModelOpenAiCompatibleBaseUrl("https://api.siliconflow.cn/v1"))
    }
}
