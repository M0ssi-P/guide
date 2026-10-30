package parsers.vgr.models

data class Calendar(
    val currentMonth: Boolean,
    val activeDays: List<ActiveDay>
) {
    data class ActiveDay(
        val isCurrent: Boolean,
        val day: Int,
        val id: Int
    )
}