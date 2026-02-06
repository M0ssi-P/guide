package parsers.bible.models

import java.io.Serializable

data class ILanguage(
    val id: Int?,
    val dbId: String? = null,
    val iso63901: String?,
    val iso63903: String?,
    val name: String,
    val localName: String,
    val tag: String,
    val hasAudio: Boolean?,
    val hasText: Boolean?,
    val totalVersions: Int?,
    val textDirection: String,
    val font: String?,
) : Serializable
