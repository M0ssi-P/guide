package parsers.vgr.models

data class IQotd(
    val ufsm: String,
    val bookHuman: String,
    val bookUfsm: String,
    val chapter: Int,
    val verseNumbers: List<Int> = emptyList(),
    val ReqDate: Long? = null,
    val content: String,
    val TheTable: ITheTable
) {
    data class ITheTable(
        val title: String,
        val code: String,
        val year: Int,
        val month: Int,
        val day: Int,
        val dayWhen: String? = null,
        val text: String
    )
}
