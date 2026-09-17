package com.aiartgenerator.imagegenerator.videogenerator.model

data class LanguageOption(
    val id: String,
    val name: String,
    val flagEmoji: String,
)

object LanguageCatalog {
    const val DEFAULT_ID = "en"

    val languages = listOf(
        LanguageOption("pt", "Portuguese", "🇧🇷"),
        LanguageOption("en", "English", "🇬🇧"),
        LanguageOption("es", "Spanish", "🇪🇸"),
        LanguageOption("de", "German", "🇩🇪"),
        LanguageOption("fr", "France", "🇫🇷"),
        LanguageOption("ar", "Arabic", "🇸🇦"),
        LanguageOption("id", "Indonesian", "🇮🇩"),
        LanguageOption("ur", "Urdu", "🇵🇰"),
        LanguageOption("vi", "Vietnam", "🇻🇳"),
        LanguageOption("th", "Thailand", "🇹🇭"),
        LanguageOption("ms", "Malaysia", "🇲🇾"),
    )

    fun findById(id: String): LanguageOption? = languages.find { it.id == id }
}
