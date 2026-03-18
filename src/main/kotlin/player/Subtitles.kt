package player

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Chapter(
    val audioName: String,
    val title: String,
    val voiceId: Int,
    val versionId: Int,
    val default: Boolean,
    val fileId: String,
    val next: List<String>?,
    val prev: List<String>?,
    val lyrics: List<Lyric>
)

@Serializable
data class Lyric(
    val verse: String,
    val timing: Timing
)

@Serializable
data class Timing(
    val start: Double,
    val end: Double,
    val startOffset: Int,
    val endOffset: Int
)

fun loadChapters(path: String?): List<Chapter>? {

    if(path == null) return null

    val path = "C:\\Users\\god1t\\OneDrive\\Documents\\coding\\theguide_esentials\\src\\datastore\\bible\\NKJV-114\\subtitles\\${path}.json"
    val file = File(path)
    if (!file.exists()) return emptyList()
    val jsonText = file.readText()
    return Json.decodeFromString(jsonText)
}