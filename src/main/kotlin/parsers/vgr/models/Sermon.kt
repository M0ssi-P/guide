package parsers.vgr.models

data class Sermon(
    val id: Long?,
    val date: String,
    val dateCode: String,
    val language: String,
    val location: String,
    val nextSermonDate: String = "",
    val prevSermonDate: String = "",
    val title: String,
    val sortDate: String,
    val minutes: Int? = null,
    val isCab: Boolean = false,
    val sections: List<Section>? = null,
    val totalSections: Int? = null,
    val c: Int,
    val i: Int,
    val ct: String,
    val updatedAt: Long,
)