package com.aiartgenerator.imagegenerator.videogenerator.model

/** Editable starter prompts shown in generator text fields. */
object GeneratorDefaultPrompts {
    const val VIDEO =
        "Cinematic drone shot flying over misty mountains at sunrise, soft golden light, smooth camera motion, ultra detailed, 4K"

    const val MUSIC =
        "Upbeat lo-fi hip hop with soft piano, gentle rain ambience, cozy coffee shop vibes, calm 90 BPM beat"

    fun forImageStyle(styleId: String): String =
        when (styleId) {
            "anime" ->
                "A young anime hero standing on a rooftop at dusk, glowing city lights below, wind in their hair, vibrant colors, ultra detailed"
            "oil" ->
                "An oil painting of a quiet countryside cottage by a lake, rich brush strokes, warm sunset colors, classic fine art look"
            "cartoon" ->
                "A cheerful cartoon fox explorer with a tiny backpack, colorful forest background, clean lines, playful storybook style"
            "cyber" ->
                "A futuristic cyberpunk street at night with neon signs reflecting on rain-slick pavement, flying cars, ultra detailed, cinematic"
            else ->
                "A photorealistic portrait of a traveler on a mountain ridge at golden hour, natural lighting, sharp details, 4K"
        }
}
