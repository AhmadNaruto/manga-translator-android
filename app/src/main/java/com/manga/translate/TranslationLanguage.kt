package com.manga.translate

enum class TranslationLanguage(
    val prefValue: String,
    val displayNameResId: Int
) {
    JA_TO_ZH("ja_to_zh", R.string.folder_language_ja_to_zh),
    EN_TO_ZH("en_to_zh", R.string.folder_language_en_to_zh),
    KO_TO_ZH("ko_to_zh", R.string.folder_language_ko_to_zh);

    companion object {
        fun fromPref(value: String?): TranslationLanguage {
            return when (value) {
                EN_TO_ZH.prefValue,
                EN_TO_ZH.name -> EN_TO_ZH
                KO_TO_ZH.prefValue,
                KO_TO_ZH.name -> KO_TO_ZH
                JA_TO_ZH.prefValue,
                JA_TO_ZH.name -> JA_TO_ZH
                else -> JA_TO_ZH
            }
        }

        fun fromString(value: String?): TranslationLanguage = fromPref(value)
    }
}
