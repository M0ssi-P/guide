package player

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

/** A chapter's recordings with their verse timings, as the Bible reader's highlighting reads them. */
fun chaptersFrom(content: parsers.bible.models.IChapterContent?): List<Chapter>? = content?.audio?.map { a ->
    Chapter(
        audioName = a.title, title = a.title, voiceId = a.versionId, versionId = content.version?.id ?: 0, default = a.default,
        fileId = "${a.id}", next = null, prev = null,
        lyrics = a.timings.map { Lyric("${it.verse}", Timing(it.startMs / 1000.0, it.endMs / 1000.0, 0, 0)) },
    )
}
