package navigation

sealed class Screen(route: String) {
    object Home: Screen("/home")
    object Saved: Screen("/saved")
    object MyActivity: Screen("/activity")
    object Setting: Screen("/setting")
    data class Library(
        val page: LibraryScreen
    ): Screen("/library")

    companion object {
        fun fromRoute(route: String): Screen =
            when {
                route == "/home" -> Home
                route.startsWith("/library") -> {
                    val subRoute = route.removePrefix("/library")

                    Library(
                        page = LibraryScreen.fromRoute(subRoute)
                    )
                }
                else -> Home
            }
    }
}

fun Screen.fullRoute(): String = when (this) {
    Screen.Home -> "/home"
    Screen.Saved -> "/saved"
    Screen.MyActivity -> "/activity"
    Screen.Setting -> "/setting"
    is Screen.Library -> "/library/${page}"
}

sealed class LibraryScreen(route: String) {
    data class Table(val page: TableScreen = TableScreen.Home): LibraryScreen("/table")
    data class Bible(val page: BibleScreen = BibleScreen.Home): LibraryScreen("/bible")
    data class Hymns(val page: SongScreen = SongScreen.Home): LibraryScreen("/hymns")

    companion object {
        fun fromRoute(route: String): LibraryScreen =
            when {
                route.startsWith("/table") -> {
                    val subRoute = route
                        .removePrefix("/table")

                    Table(
                        page = TableScreen.fromRoute(subRoute)
                    )
                }

                route.startsWith("/bible") -> {
                    val subRoute = route
                        .removePrefix("/bible")

                    Bible(
                        page = BibleScreen.fromRoute(subRoute)
                    )
                }

                route.startsWith("/hymns") -> {
                    val subRoute = route
                        .removePrefix("/hymns")

                    Hymns(
                        page = SongScreen.fromRoute(subRoute)
                    )
                }

                else -> {}
            } as LibraryScreen
    }
}

sealed class TableScreen(route: String) {
    object Home: TableScreen("/home")
    data class Sermon(val sermonId: String): TableScreen("/sermon/$sermonId")

    companion object {
        fun fromRoute(route: String): TableScreen =
            when {
                route == "/home" -> Screen.Home
                route.startsWith("/library") -> {
                    val subRoute = route
                        .removePrefix("/library")
                        .removePrefix("/")

                    Sermon(
                        sermonId = subRoute
                    )
                }
                else -> {}
            } as TableScreen
    }
}

sealed class BibleScreen(route: String) {
    object Home: BibleScreen("/")
    data class Read(
        val versionId: String,
        val bookId: String,
        val chapterId: String,
    ): BibleScreen("/read/$versionId/$bookId.$chapterId.$versionId")

    companion object {
        fun fromRoute(route: String): BibleScreen =
            when {
                route == "/home" -> BibleScreen.Home
                route.startsWith("/read") -> {
                    val versionId = route.split("/")[1]
                    val bookId = route.split("/")[2].split(".")[0]
                    val chapterId = route.split("/")[2].split(".")[1]
                    val versionTag = route.split("/")[2].split(".")[2]

                    Read(
                        versionId,
                        bookId,
                        chapterId
                    )
                }

                else -> {}
            } as BibleScreen
    }
}

sealed class SongScreen(route: String) {
    object Home: SongScreen("/home")
    data class Song(val book: String? = null, val songId: String): SongScreen("/song/$book/$songId")

    companion object {
        fun fromRoute(route: String): SongScreen =
            when {
                route == "/home" -> Home
                route.startsWith("/song") -> {
                    val subRoute = route.substringAfter("/song/")
                    val book = subRoute.split('/')[0]
                    val songId = subRoute.substringAfter("$book/")

                    Song(book, songId)
                }

                else -> {}
            } as SongScreen
    }
}