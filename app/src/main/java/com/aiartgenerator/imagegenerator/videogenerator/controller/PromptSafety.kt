package com.aiartgenerator.imagegenerator.videogenerator.controller

import android.content.Context
import java.io.IOException
import java.util.Locale

/**
 * Shared safety filter for image, video, music, and wallpaper generation.
 * Must run before any provider/fallback API request.
 */
internal object PromptSafety {
    const val UNSAFE_MESSAGE =
        "This prompt is not allowed. Please use safe and respectful language."

    private const val ASSET_NAME = "prompt_safety_blocked_words.txt"

    private val leetMap = charArrayOf(
        '0', 'o', '1', 'i', '3', 'e', '4', 'a', '5', 's', '7', 't', '@', 'a', '$', 's',
    )

    @Volatile
    private var singleWords: Set<String> = emptySet()

    @Volatile
    private var phrases: List<String> = emptyList()

    fun init(context: Context) {
        context.assets.open(ASSET_NAME).bufferedReader().use { reader ->
            loadTerms(reader.readLines())
        }
    }

    const val VISUAL_SAFETY_SUFFIX =
        "fully clothed, wearing modest clothing that covers the body, no nudity, no nsfw, safe for work"

    const val VISUAL_NEGATIVE_PROMPT =
        "nude, naked, nsfw, nudity, nudes, topless, bottomless, undressed, underwear, lingerie, exposed skin, sexual, porn"

    fun containsUnsafeContent(prompt: String): Boolean {
        if (prompt.isBlank()) return false
        val variants = normalizeVariants(prompt)
        if (variants.any { matchesProhibitedScene(it) || matchesNudityIntent(it) }) return true
        return variants.any { matchesBlockedTerms(it) }
    }

    fun withVisualSafety(prompt: String): String {
        val clean = prompt.trim()
        if (clean.contains("fully clothed", ignoreCase = true)) return clean
        return "$clean, $VISUAL_SAFETY_SUFFIX"
    }

    fun requireSafe(vararg texts: String) {
        if (texts.any { containsUnsafeContent(it) }) {
            throw IOException(UNSAFE_MESSAGE)
        }
    }

    internal fun loadTerms(terms: Collection<String>) {
        val cleaned = terms
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { tokenize(it) }
            .filter { it.isNotEmpty() }
            .distinct()
        phrases = cleaned
            .filter { ' ' in it }
            .sortedByDescending { it.length }
        singleWords = cleaned.filter { ' ' !in it }.toSet()
    }

    private fun matchesBlockedTerms(normalized: String): Boolean {
        if (normalized.isEmpty()) return false
        val padded = " $normalized "
        for (phrase in phrases) {
            if (padded.contains(" $phrase ")) return true
        }
        return normalized.split(' ').any { it in singleWords }
    }

    private fun normalizeVariants(raw: String): List<String> {
        val lower = raw.lowercase(Locale.ROOT)
        val deLeet = decodeLeet(lower)
        return listOf(lower, deLeet)
            .map { tokenize(it) }
            .flatMap { spaced -> listOf(spaced, collapseSpacedLetters(spaced)) }
            .map { it.replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    private fun tokenize(text: String): String =
        text
            .replace('$', 's')
            .replace('@', 'a')
            .replace('_', ' ')
            .replace('-', ' ')
            .replace('.', ' ')
            .replace(Regex("[^a-z0-9+& ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun decodeLeet(text: String): String {
        val chars = text.toCharArray()
        var i = 0
        while (i < leetMap.size) {
            val from = leetMap[i]
            val to = leetMap[i + 1]
            for (index in chars.indices) {
                if (chars[index] == from) chars[index] = to
            }
            i += 2
        }
        return String(chars)
    }

    private fun collapseSpacedLetters(text: String): String {
        val tokens = text.split(' ')
        if (tokens.size < 3) return text
        val out = ArrayList<String>(tokens.size)
        var index = 0
        while (index < tokens.size) {
            val token = tokens[index]
            if (token.length == 1 && token[0].isLetter()) {
                val buffer = StringBuilder()
                while (index < tokens.size && tokens[index].length == 1 && tokens[index][0].isLetter()) {
                    buffer.append(tokens[index])
                    index++
                }
                if (buffer.length >= 3) {
                    out += buffer.toString()
                } else {
                    out += buffer.toString().toCharArray().map { it.toString() }
                }
            } else {
                out += token
                index++
            }
        }
        return out.joinToString(" ")
    }

    /**
     * Blocks the attached reference scene even when the prompt avoids exact list terms:
     * a person lying on a bed with legs spread, inviting a partner to "come here"
     * and "let your emotions out" (including romanized Urdu/Hindi wording).
     */
    private fun matchesNudityIntent(text: String): Boolean {
        if (Regex("""\b(nangi|nanga|nangai|nange|nangiya)\b""").containsMatchIn(text)) return true
        if (Regex(
                """\b(doesn t|doesnt|does not|don t|dont|do not|isn t|isnt|not)\b.{0,16}\b(wear|wearing|wears)\b.{0,20}\b(anything|nothing|clothes|clothing|dress|kapre|kapron|kapde|kapra)\b""",
            ).containsMatchIn(text)
        ) {
            return true
        }
        if (Regex(
                """\b(without|bina|baghair|bagair)\b.{0,20}\b(clothes|clothing|dress|cloth|kapre|kapron|kapde|kapra|anything)\b""",
            ).containsMatchIn(text)
        ) {
            return true
        }
        val compacted = text.replace(" ", "")
        return compactNudityTerms.any { compacted.contains(it) }
    }

    private val compactNudityTerms = listOf(
        "nude", "nudes", "naked", "nudity", "nsfw", "porn", "porno",
        "topless", "bottomless", "undressed", "nangi", "nanga",
    )

    private fun matchesProhibitedScene(text: String): Boolean {
        val hasBed = containsAny(
            text,
            "bed", "beds", "bedroom", "mattress", "bedtime", "bed sheet", "bedsheets",
        )
        val hasLying = Regex(
            """\b(lying|laying|laid|lies|leti|let rahi|let raha)\b""",
        ).containsMatchIn(text) ||
            text.contains("on the bed") ||
            text.contains("bed pe") ||
            text.contains("pe leti") ||
            text.contains("pe leta")
        val hasSpreadLegs = Regex(
            """\b(leg|legs|thigh|thighs|tang|tangy|tangen)\b.{0,48}\b(spread|open|apart|wide|phail|phaila|phailaye|pela|pelaye)\b""" +
                """|\b(spread|open|apart|wide|phail|phaila|phailaye|pela|pelaye)\b.{0,48}\b(leg|legs|thigh|thighs|tang|tangy|tangen)\b""",
        ).containsMatchIn(text)
        val hasInvite = Regex(
            """\b(husband|wife|partner|come here|come to me|aa jao|aajao|ajawo|aajao|emotion nikalo|""" +
                """let (your|his|her|my) emotions? out|apna emotion)\b""",
        ).containsMatchIn(text) ||
            text.contains("ajawo") ||
            text.contains("aa jao") ||
            text.contains("aajao") ||
            text.contains("emotion nikalo") ||
            text.contains("apna emotion")
        val hasSeductivePose = Regex(
            """\b(seductive pose|provocative pose|suggestive pose|sultry pose)\b""",
        ).containsMatchIn(text)

        if (hasSpreadLegs && (hasBed || hasLying || hasInvite)) return true
        if (hasBed && hasLying && hasInvite) return true
        if (hasSpreadLegs && hasInvite) return true
        if (hasBed && hasSeductivePose) return true
        if (hasLying && hasSpreadLegs) return true
        return false
    }

    private fun containsAny(text: String, vararg words: String): Boolean {
        val padded = " $text "
        return words.any { padded.contains(" $it ") }
    }
}
