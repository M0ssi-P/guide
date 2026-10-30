package parsers.vgr.models

import kotlinx.serialization.Serializable

@Serializable
data class LanguageRoot(
    val c: String,
    val e: String,
    val f: String,
    val i: String
)
