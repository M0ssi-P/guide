package parsers.bible.models

data class ILanguage(
    val id: Int?,
    val iso63901: String?,
    val iso63903: String?,
    val name: String,
    val localName: String,
    val tag: String,
    val hasAudio: Boolean?,
    val hasText: Boolean?,
    val totalVersions: Int?,
    val textDirection: String,
    val font: Any?,
)
