package parsers.vgr.models

import kotlinx.serialization.Serializable
import parsers.vgr.models.IRepoSERMONS.ILanguageTag
import parsers.vgr.models.IRepoSERMONS.ISermons

@Serializable
data class IRepoSERMONS(
    val Request: ILanguageTag,
    val Result: ISermons
) {
    @Serializable
    data class ILanguageTag(
        val Language: String
    )

    @Serializable
    data class ISermons(
        val Sermons: List<ISermon>
    )

    @Serializable
    data class ISermon(
        val c: Int,
        val cab: Boolean,
        val ct: String,
        val i: Int,
        val m: Int? = null,
        val p: String,
        val t: String,
    )
}

@Serializable
data class IHubResponse(
    val date: String,
    val dateCode: String,
    val id: Long,
    val languageCode: String,
    val location: String,
    val nextDateCode: String,
    val previousDateCode: String,
    val sortDate: String,
    val title: String,
    val updatedOn: Long
)

@Serializable
data class LanguageRequest(
    val Language: String
)

@Serializable
data class SermonRequest(
    val GetAllContent: Boolean,
    val HighlightQuery: String?,
    val Language: String,
    val SermonProductIdentityId: Int
)

@Serializable
data class FormVars(
    val formVars: List<Fields>
) {
    @Serializable
    data class Fields(
        val name: String,
        val value: String
    )

    @Serializable
    data class FormRawResponse(
        val d: String
    )
}

@Serializable
data class IHubSermonResponse(
    val sermon: IHubSermon
) {
    @Serializable
    data class IHubSermon(
        val id: Long,
        val languageCode: String,
        val dateCode: String,
        val sortDate: String,
        val title: String,
        val location: String,
        val date: String,
        val updatedOn: Long,
        val nextDateCode: String,
        val previousDateCode: String,
        val blocks: List<IBlock>
    )

    @Serializable
    data class IBlock(
        val blockId: Long,
        val blockNumber: Int,
        val blockText: String
    )
}

@Serializable
data class ISermonResponse(
    val Request: ISermonTag,
    val Result: ISermo
) {
    @Serializable
    data class ISermonTag(
        val GetAllContent: Boolean,
        val Language: String,
        val SermonProductIdentityId: Int
    )

    @Serializable
    data class ISermo(
        val BookTitle: String,
        val DateCode: String,
        val IsCAB: Boolean,
        val Sections: List<IParagraph>,
        val Title: String,
        val TotalSections: Int
    )

    @Serializable
    data class IParagraph(
        val Content: String,
        val IsHit: Boolean,
        val Paragraph: String,
    )
}