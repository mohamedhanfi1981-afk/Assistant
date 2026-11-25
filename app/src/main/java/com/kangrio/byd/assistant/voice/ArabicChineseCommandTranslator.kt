package com.kangrio.byd.assistant.voice

import java.text.Normalizer
import java.util.Locale

/**
 * Offline, deterministic Arabic-to-Mandarin phrase mapping for a small set
 * of vehicle commands.
 *
 * This is deliberately NOT presented as speech recognition or a general
 * translator. Feed it recognized Arabic text and inspect the returned
 * command before sending anything to the vehicle.
 *
 * The Chinese phrases are candidate natural-language commands. They must be
 * validated against the exact Toyota voice assistant / vehicle language
 * configuration before any automatic execution is enabled.
 */
object ArabicChineseCommandTranslator {

    enum class Action {
        AIR_CONDITIONING_ON,
        AIR_CONDITIONING_OFF,
        TEMPERATURE_UP,
        TEMPERATURE_DOWN,
        FAN_SPEED_UP,
        FAN_SPEED_DOWN,
        DRIVER_WINDOW_OPEN,
        DRIVER_WINDOW_CLOSE,
        ALL_WINDOWS_OPEN,
        ALL_WINDOWS_CLOSE,
        SUNROOF_OPEN,
        SUNROOF_CLOSE,
        AIR_RECIRCULATION_ON,
        FRONT_DEFOGGER_ON
    }

    data class Translation(
        val action: Action,
        val recognizedArabic: String,
        val chineseText: String
    )

    private data class Rule(
        val action: Action,
        val chineseText: String,
        val phrases: List<String>
    )

    private val rules = listOf(
        Rule(
            Action.AIR_CONDITIONING_ON,
            "打开空调",
            listOf("شغل التكييف", "افتح التكييف", "شغل التبريد")
        ),
        Rule(
            Action.AIR_CONDITIONING_OFF,
            "关闭空调",
            listOf("اقفل التكييف", "اطفي التكييف", "اغلق التكييف")
        ),
        Rule(
            Action.TEMPERATURE_UP,
            "把温度调高一点",
            listOf("علي درجة الحرارة", "ارفع درجة الحرارة", "زود الحرارة")
        ),
        Rule(
            Action.TEMPERATURE_DOWN,
            "把温度调低一点",
            listOf("وطي درجة الحرارة", "خفض درجة الحرارة", "قلل الحرارة")
        ),
        Rule(
            Action.FAN_SPEED_UP,
            "把风量调大一点",
            listOf("زود سرعة المروحة", "علي سرعة المروحة", "زود الهوا")
        ),
        Rule(
            Action.FAN_SPEED_DOWN,
            "把风量调小一点",
            listOf("قلل سرعة المروحة", "وطي سرعة المروحة", "قلل الهوا")
        ),
        Rule(
            Action.DRIVER_WINDOW_OPEN,
            "打开驾驶员侧车窗",
            listOf("افتح شباك السواق", "افتح شباك السائق")
        ),
        Rule(
            Action.DRIVER_WINDOW_CLOSE,
            "关闭驾驶员侧车窗",
            listOf("اقفل شباك السواق", "اغلق شباك السواق", "اقفل شباك السائق")
        ),
        Rule(
            Action.ALL_WINDOWS_OPEN,
            "打开所有车窗",
            listOf("افتح كل الشبابيك", "افتح جميع الشبابيك")
        ),
        Rule(
            Action.ALL_WINDOWS_CLOSE,
            "关闭所有车窗",
            listOf("اقفل كل الشبابيك", "اقفل جميع الشبابيك", "اغلق كل الشبابيك")
        ),
        Rule(
            Action.SUNROOF_OPEN,
            "打开天窗",
            listOf("افتح فتحة السقف", "افتح السقف")
        ),
        Rule(
            Action.SUNROOF_CLOSE,
            "关闭天窗",
            listOf("اقفل فتحة السقف", "اقفل السقف", "اغلق فتحة السقف")
        ),
        Rule(
            Action.AIR_RECIRCULATION_ON,
            "打开内循环",
            listOf("شغل تدوير الهواء", "شغل اعادة تدوير الهواء", "شغل الهواء الداخلي")
        ),
        Rule(
            Action.FRONT_DEFOGGER_ON,
            "打开前挡风玻璃除雾",
            listOf("شغل مزيل الضباب الامامي", "شغل إزالة الضباب الامامي", "شغل سخان الزجاج الامامي")
        )
    )

    private val normalizedRules: List<Pair<Rule, List<String>>> by lazy {
        rules.map { rule -> rule to rule.phrases.map(::normalizeArabic) }
    }

    /**
     * Returns null when the recognized Arabic phrase does not exactly match
     * one of the supported command phrases. Unknown speech is never guessed.
     */
    fun translate(recognizedArabic: String): Translation? {
        val normalizedInput = normalizeArabic(recognizedArabic)
        if (normalizedInput.isBlank()) return null

        val match = normalizedRules.firstOrNull { (_, phrases) ->
            normalizedInput in phrases
        }?.first ?: return null

        return Translation(
            action = match.action,
            recognizedArabic = recognizedArabic,
            chineseText = match.chineseText
        )
    }

    /**
     * Exposes the supported Arabic phrases for UI/help and manual testing.
     */
    fun supportedArabicPhrases(): List<String> =
        rules.flatMap { it.phrases }.distinct()

    private fun normalizeArabic(value: String): String {
        val withoutDiacritics = Normalizer.normalize(value, Normalizer.Form.NFKC)
            .replace(ARABIC_DIACRITICS, "")

        return withoutDiacritics
            .lowercase(Locale.ROOT)
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ى', 'ي')
            .replace('ة', 'ه')
            .replace(ARABIC_TATWEEL, "")
            .replace(NON_LETTER_OR_DIGIT, " ")
            .trim()
            .replace(MULTIPLE_SPACES, " ")
    }

    private val ARABIC_DIACRITICS = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")
    private val ARABIC_TATWEEL = Regex("[\\u0640]")
    private val NON_LETTER_OR_DIGIT = Regex("[^\\p{L}\\p{N}\\s]")
    private val MULTIPLE_SPACES = Regex("\\s+")
}
