package com.aiartgenerator.imagegenerator.videogenerator.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.aiartgenerator.imagegenerator.videogenerator.R

enum class EditToolBadge(val label: String) {
    Ai("AI"),
    Hd("HD"),
    Upscale("8X"),
    New("New"),
}

data class EditTool(
    val id: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val prompt: String,
    val styleHint: String = "Auto",
    val badge: EditToolBadge? = null,
)

object EditCatalog {
    val tools = listOf(
        EditTool(
            id = "remove_bg",
            labelRes = R.string.edit_tool_remove_bg,
            iconRes = R.drawable.ic_edit_remove_bg,
            prompt = "Remove the background and keep the subject clean with a transparent-style cutout look",
        ),
        EditTool(
            id = "magic_eraser",
            labelRes = R.string.edit_tool_magic_eraser,
            iconRes = R.drawable.ic_edit_magic_eraser,
            prompt = "Erase unwanted objects seamlessly and fill the area naturally",
        ),
        EditTool(
            id = "ai_enhance",
            labelRes = R.string.edit_tool_ai_enhance,
            iconRes = R.drawable.ic_edit_ai_enhance,
            prompt = "Enhance details, lighting, and clarity for a professional HD finish",
            styleHint = "Enhance",
        ),
        EditTool(
            id = "replace_obj",
            labelRes = R.string.edit_tool_replace_obj,
            iconRes = R.drawable.ic_edit_replace_obj,
            prompt = "Replace the main object with a better matching alternative while keeping the scene natural",
        ),
        EditTool(
            id = "upscaler",
            labelRes = R.string.edit_tool_upscaler,
            iconRes = R.drawable.ic_edit_upscaler,
            prompt = "Upscale the image with sharper edges, richer texture, and minimal artifacts",
            styleHint = "Enhance",
        ),
        EditTool(
            id = "face_enhance",
            labelRes = R.string.edit_tool_face_enhance,
            iconRes = R.drawable.ic_edit_face_enhance,
            prompt = "Enhance facial features with natural skin tone, balanced lighting, and crisp eyes",
            styleHint = "Realistic",
        ),
        EditTool(
            id = "sky_replace",
            labelRes = R.string.edit_tool_sky_replace,
            iconRes = R.drawable.ic_edit_sky_replace,
            prompt = "Replace the sky with a dramatic sunset while matching lighting on the scene",
        ),
        EditTool(
            id = "colorize",
            labelRes = R.string.edit_tool_colorize,
            iconRes = R.drawable.ic_edit_colorize,
            prompt = "Colorize the image with rich natural colors and balanced saturation",
        ),
        EditTool(
            id = "bg_generator",
            labelRes = R.string.edit_tool_bg_generator,
            iconRes = R.drawable.ic_edit_bg_generator,
            prompt = "Generate a beautiful new background that matches the subject and lighting",
        ),
        EditTool(
            id = "ai_expand",
            labelRes = R.string.edit_tool_ai_expand,
            iconRes = R.drawable.ic_edit_ai_expand,
            prompt = "Expand the canvas outward with coherent scenery and preserved subject proportions",
        ),
    )

    fun findById(id: String): EditTool? = tools.firstOrNull { it.id == id }
}
