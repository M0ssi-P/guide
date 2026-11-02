package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IProviderStats(
    val id: String = "",
    val language: String,
    val name: String,
    val saveName: String,
    val baseURI: String,
    val isWorking: Boolean
)

@Serializable
data class ISongTitle(
    val language: String,
    val languageCode: String,
    val title: String
)

@Serializable
enum class LyricType {
    @SerialName("VERSE") VERSE,
    @SerialName("CHORUS") CHORUS,
    @SerialName("BRIDGE") BRIDGE,
    @SerialName("OTHER") OTHER,
}

@Serializable
data class ISongLyricLine(
    val id: String = "",
    val songId: String = "",
    val type: LyricType,
    val key: String?,
    val verseNumber: Int?,
    val lines: List<String>
)

@Serializable
data class ISongDetails(
    val id: String = "",
    val titles: List<ISongTitle>,
    val songNumber: Int,
    val lyrics: List<ISongLyricLine>,
    // from the song store: its category, author (with year), English title and a recording of how it's sung
    val englishTitle: String? = null,
    val category: String? = null,
    val author: String? = null,
    val year: String? = null,
    val songUrl: String? = null,
    /** The number as printed when it carries a letter ("25B"); songs sharing 25 sort by it. */
    val label: String? = null,
) {
    /** The song's number as people know it: "25", or "25B". */
    val no: String get() = label ?: songNumber.toString()
}