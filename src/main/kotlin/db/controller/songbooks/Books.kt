package db.controller.songbooks

import db.generateId
import db.run
import db.runAndReturn
import kotlinx.serialization.json.Json
import models.IProviderStats
import models.ISongDetails
import models.ISongLyricLine
import models.LyricType
import java.sql.Connection

object Books {
    fun Connection.importBook(book: IProviderStats, getId: (id: String) -> Unit) {
        val generatedId = generateId()

        run(
            """
                INSERT OR IGNORE INTO books (
                    id,
                    name,
                    language,
                    base_url,
                    save_name,
                    is_working
                ) VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            generatedId,
            book.name,
            book.language,
            book.baseURI,
            book.saveName,
            book.isWorking
        )

        getId(generatedId)
    }

    fun Connection.importSong(song: ISongDetails, bookId: String, idMapper: (id: String, lyrics: List<ISongLyricLine>) -> Unit) {
        val generatedId = generateId()

        run(
            """
                INSERT OR IGNORE INTO songs (
                    id,
                    book_id,
                    titles,
                    number
                ) VALUES (?, ?, ?, ?)
            """.trimIndent(),
            generatedId,
            bookId,
            Json.encodeToString(song.titles),
            song.songNumber,
        )

        idMapper(generatedId, song.lyrics)
    }

    fun Connection.importLyrics(lyrics: List<ISongLyricLine>, songId: String) {
        lyrics.forEach { lyric ->
            val generatedId = generateId()

            run(
                """
                INSERT OR IGNORE INTO lyrics (
                    id,
                    song_id,
                    type,
                    number,
                    gamme,
                    lines
                ) VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
                generatedId,
                songId,
                lyric.type,
                lyric.verseNumber,
                lyric.key,
                Json.encodeToString(lyric.lines),
            )
        }
    }

    fun Connection.getBooks(): List<IProviderStats> {
        return runAndReturn(
            "SELECT * FROM books",
        ) { rs ->
            IProviderStats(
                id = rs.getString("id"),
                language = rs.getString("language"),
                name = rs.getString("name"),
                saveName = rs.getString("save_name"),
                baseURI = rs.getString("base_url"),
                isWorking = rs.getBoolean("is_working")
            )
        }
    }

    fun Connection.getSongs(bookId: String): List<ISongDetails> {
        return runAndReturn(
            "SELECT * FROM songs WHERE book_id = ?",
            bookId
        ) { rs ->
            val lyrics = runAndReturn(
                "SELECT * FROM lyrics WHERE song_id = ?",
                rs.getString("id")
            ) { lyric ->
                ISongLyricLine(
                    id = lyric.getString("id"),
                    songId = lyric.getString("song_id"),
                    type = Json.decodeFromString<LyricType>("\"${lyric.getString("type")}\""),
                    key = lyric.getString("gamme"),
                    verseNumber = lyric.getInt("number"),
                    lines = Json.decodeFromString(lyric.getString("lines"))
                )
            }

            ISongDetails(
                id = rs.getString("id"),
                titles = Json.decodeFromString(rs.getString("titles")),
                songNumber = rs.getInt("number"),
                lyrics = lyrics
            )
        }
    }
}