package parsers.vgr.models

import kotlinx.serialization.Serializable

@Serializable
data class Segment(
    val style: List<Style>,
    val index: List<Int>? = listOf(),
    val start: Double? = null,
    val end: Double? = null,
    val content: String
): java.io.Serializable