package com.example.myapplication.safety

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class PromptSafetyTest {

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
        assertFalse(PromptSafety.containsUnsafeContent("A cinematic portrait of a fox in a misty forest at golden hour"))
        assertFalse(PromptSafety.containsUnsafeContent("A cinematic drone shot flying over misty mountains at sunrise"))
        assertFalse(PromptSafety.containsUnsafeContent("A chill song in rainy weather with soft piano and distant thunder"))
    }

    @Test
    fun requireSafeThrowsBeforeGeneration() {
        val error = assertFailsWith<IllegalArgumentException> {
            PromptSafety.requireSafe("fully nude character")
        }
        assertTrue(error.message?.contains("not allowed") == true)
    }
}
