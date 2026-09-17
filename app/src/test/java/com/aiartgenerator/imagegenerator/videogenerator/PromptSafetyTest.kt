package com.aiartgenerator.imagegenerator.videogenerator

import com.aiartgenerator.imagegenerator.videogenerator.controller.PromptSafety
import com.aiartgenerator.imagegenerator.videogenerator.model.GeneratorDefaultPrompts
import java.io.File
import java.io.IOException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PromptSafetyTest {

    @Before
    fun loadBlockedWords() {
        val file = blockedWordsFile()
        assertTrue("blocked words asset missing at ${file.absolutePath}", file.isFile)
        PromptSafety.loadTerms(file.readLines())
    }

    @Test
    fun blocksListedNudityTermsCaseInsensitively() {
        assertTrue(PromptSafety.containsUnsafeContent("A NUDE portrait on the beach"))
        assertTrue(PromptSafety.containsUnsafeContent("without clothes"))
        assertTrue(PromptSafety.containsUnsafeContent("spread legs"))
        assertTrue(PromptSafety.containsUnsafeContent("She doesn't wear anything"))
        assertTrue(PromptSafety.containsUnsafeContent("woman with legs spread apart"))
        assertTrue(PromptSafety.containsUnsafeContent("nangi larki"))
        assertTrue(PromptSafety.containsUnsafeContent("nudegirl on beach"))
    }

    @Test
    fun blocksLeetAndSpacedObfuscation() {
        assertTrue(PromptSafety.containsUnsafeContent("nud3 person"))
        assertTrue(PromptSafety.containsUnsafeContent("n u d e person"))
    }

    @Test
    fun blocksReferenceBedSceneEvenWithoutExactListPhrases() {
        assertTrue(
            PromptSafety.containsUnsafeContent(
                "She is on the mattress with her thighs apart, telling her husband to come here and let his feelings out",
            ),
        )
        assertTrue(
            PromptSafety.containsUnsafeContent(
                "Wo bed pe leti hai awr tangy pelaye hai awr husband ko keh rahi hai k ajawo awr apna emotion nikalo",
            ),
        )
    }

    @Test
    fun allowsSafePromptsAndDoesNotMatchInsideWords() {
        assertFalse(PromptSafety.containsUnsafeContent("A traveler on a mountain ridge at golden hour"))
        assertFalse(PromptSafety.containsUnsafeContent("A crystal class display in a museum hall"))
        assertFalse(PromptSafety.containsUnsafeContent(""))
    }

    @Test
    fun defaultGeneratorPromptsRemainSafe() {
        assertFalse(PromptSafety.containsUnsafeContent(GeneratorDefaultPrompts.VIDEO))
        assertFalse(PromptSafety.containsUnsafeContent(GeneratorDefaultPrompts.MUSIC))
        assertFalse(PromptSafety.containsUnsafeContent(GeneratorDefaultPrompts.forImageStyle("realistic")))
        assertFalse(PromptSafety.containsUnsafeContent(GeneratorDefaultPrompts.forImageStyle("cyber")))
        assertFalse(PromptSafety.containsUnsafeContent(GeneratorDefaultPrompts.forImageStyle("anime")))
    }

    @Test
    fun requireSafeThrowsBeforeGeneration() {
        val error = assertThrows(IOException::class.java) {
            PromptSafety.requireSafe("fully nude character")
        }
        assertTrue(error.message?.contains("not allowed") == true)
    }

    private fun blockedWordsFile(): File {
        val candidates = listOf(
            File("src/main/assets/prompt_safety_blocked_words.txt"),
            File("app/src/main/assets/prompt_safety_blocked_words.txt"),
        )
        return candidates.firstOrNull { it.isFile } ?: candidates.first()
    }
}
