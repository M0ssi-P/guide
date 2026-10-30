package songs

import models.IProviderStats
import models.ISongDetails
import models.ISongLyricLine
import models.ISongTitle
import models.LyricType
import okHttpClient
import okhttp3.OkHttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SongDropTest {
    private val book = Book(
        IProviderStats(language = "en", name = "Songs of Zion", saveName = "zion", baseURI = "", isWorking = true),
        listOf(ISongDetails(titles = listOf(ISongTitle("en", "en", "Amazing grace")), songNumber = 1, lyrics = listOf(ISongLyricLine(type = LyricType.VERSE, key = null, verseNumber = 1, lines = listOf("Amazing grace", "how sweet the sound"))))),
    )

    @Test fun linkRoundTrips() {
        val r = DropReceiver("192.168.1.20", 50123, "TV · Living room & hall", "4821")
        assertEquals(r, parseDropLink(r.link()))
        assertNull(parseDropLink("https://example.com"))
        assertNull(parseDropLink("theguide://drop?host=1.2.3.4&code=1"))
    }

    @Test fun readsAnnouncements() {
        assertEquals(DropReceiver("10.0.0.5", 4000, "Pixel", "1234", 7), parseDropAnnouncement("THEGUIDE-DROP|1|4000|Pixel|1234", "10.0.0.5", 7))
        assertNull(parseDropAnnouncement("THEGUIDE-LIVE|2|4000|Pixel|1234", "10.0.0.5", 7))
        assertNull(parseDropAnnouncement("THEGUIDE-DROP|1|x|Pixel|1234", "10.0.0.5", 7))
    }

    @Test fun zipRoundTrips() {
        val back = readGuideZip(guideZip(listOf(book, Book(book.stat.copy(name = "Two", saveName = "two"), emptyList()))))
        assertEquals(listOf("Songs of Zion", "Two"), back.map { it.stat.name })
        assertEquals(book.songs, back[0].songs)
    }

    @Test fun sendsToReceiverWithItsCodeOnly() {
        okHttpClient = OkHttpClient()
        var got: ByteArray? = null
        val rx = SongDropReceiver("Test") { got = it }
        val me = rx.start().copy(address = "127.0.0.1")
        try {
            assertFalse(SongDropSender.send(me.copy(code = "0000".takeIf { it != me.code } ?: "1111"), byteArrayOf(1)))
            val file = guideZip(listOf(book))
            assertTrue(SongDropSender.send(me, file))
            assertTrue(file.contentEquals(got))
        } finally { rx.stop() }
    }
}
