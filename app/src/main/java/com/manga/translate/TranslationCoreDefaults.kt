package com.manga.translate

object TranslationCoreDefaults {
    const val DefaultDetectionInputSize = 640
    const val DefaultLineDetectionInputSize = 960

    const val TextDetectorConfThreshold = 0.4f
    const val TextDetectorNmsIouThreshold = 0.5f
    const val TextDetectorModelDefaultNmsIouThreshold = 0.6f
    const val TextDetectorOutputExpandRatio = 0.08f
    const val TextDetectorOutputExpandMin = 1.0f

    const val BubbleDetectorNmsIouThreshold = 0.5f

    const val PageRegionTextIouThreshold = 0.2f
    const val PageRegionMaskExpandRatio = 0.1f
    const val PageRegionMaskExpandMin = 4f
    const val TinyBubbleShortSideMinPx = 26f
    const val TinyBubbleLongSideMinPx = 56f
    const val TinyBubbleShortSideRatio = 0.032f
    const val TinyBubbleLongSideRatio = 0.075f
    const val TinyBubbleMaxAreaRatio = 0.0022f
    const val BubbleDedupIouThreshold = 0.65f

    const val VlBubbleExpandRatio = 0.1f
    const val VlBubbleExpandMin = 4f
}
