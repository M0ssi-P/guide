package parsers.vgr

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VgrFileNameTest {
    @Test fun languageFromThePrefix() {
        assertEquals("ENG", vgrFileLanguage("62-1014E A Guide VGR.opus", "62-1014E"))
        assertEquals("SWA", vgrFileLanguage("SWA62-1014E A Guide VGR.opus", "62-1014E"))
        assertEquals("SWA", vgrFileLanguage("swa62-1014e Kiongozi vgr.OPUS", "62-1014E")) // case doesn't matter
        assertNull(vgrFileLanguage("62-1014M The Stature Of A Perfect Man VGR.opus", "62-1014E")) // other sermon
        assertEquals("ENG", vgrFileLanguage("62-1014E A Guide VGR.m4a", "62-1014E"))
        assertNull(vgrFileLanguage("62-1014E A Guide.opus", "62-1014E"))
    }

    /** The Table's folders: Windows Music/MessageAudio/<ISO3>/Sermons, macOS Music/org.branham.thetable.macos/<ISO3>/Sermons. */
    @Test fun findsTheRightLanguageInBothLayouts() {
        val home = Files.createTempDirectory("vgr").toFile()
        try {
            for (root in listOf("Music/MessageAudio", "Music/org.branham.thetable.macos")) {
                val eng = File(home, "$root/ENG/Sermons").apply { mkdirs() }
                val swa = File(home, "$root/SWA/Sermons").apply { mkdirs() }
                File(eng, "62-1014E A Guide VGR.opus").writeBytes(ByteArray(12))
                File(swa, "SWA62-1014E A Guide VGR.opus").writeBytes(ByteArray(15))
                File(swa, "SWA62-1014M The Stature Of A Perfect Man VGR.opus").writeBytes(ByteArray(9))
                // an unprefixed copy in the Swahili folder (how our older downloads were named) loses to the prefixed one
                File(swa, "62-1014E A Guide VGR.opus").writeBytes(ByteArray(12))

                assertEquals("62-1014E A Guide VGR.opus", SermonMedia.findVgrFile(eng, "ENG", "62-1014E")?.name)
                assertEquals("SWA62-1014E A Guide VGR.opus", SermonMedia.findVgrFile(swa, "SWA", "62-1014E")?.name)
                // an unprefixed file whose size isn't the Swahili one from the catalog isn't taken as Swahili
                assertNull(SermonMedia.findVgrFile(eng, "SWA", "62-1014E", sizes = setOf(15L)))
            }
        } finally { home.deleteRecursively() }
    }

    @Test fun catalogSizeDecidesBetweenTwoCandidates() {
        val dir = Files.createTempDirectory("vgr").toFile()
        try {
            File(dir, "SWA62-1014E A Guide VGR.opus").writeBytes(ByteArray(10))
            File(dir, "SWA62-1014E Kiongozi VGR.opus").writeBytes(ByteArray(15))
            assertEquals("SWA62-1014E Kiongozi VGR.opus", SermonMedia.findVgrFile(dir, "SWA", "62-1014E", sizes = setOf(15L))?.name)
            assertEquals("SWA62-1014E A Guide VGR.opus", SermonMedia.findVgrFile(dir, "SWA", "62-1014E", title = "A Guide")?.name)
        } finally { dir.deleteRecursively() }
    }

    @Test fun titlesWithCharactersWindowsDrops() {
        assertEquals("What Is The Holy Ghost", vgrSafeTitle("What Is The Holy Ghost?"))
        assertEquals("Jehovah-Jireh 1", vgrSafeTitle("Jehovah-Jireh #1"))
        assertEquals("Hear Ye Him", vgrSafeTitle("Hear Ye Him!"))
        assertEquals("Gods Way Thats Been Made For Us", vgrSafeTitle("God's Way That's Been Made For Us"))
        assertEquals("Questions And Answers COD", vgrSafeTitle("Questions And Answers, C.O.D.;"))
        assertEquals("Ndugu Woolman", vgrSafeTitle("Ndugu  Woolman"))
    }

    /** The bug: a downloaded "What Is The Holy Ghost?" was reported missing because its file name has no "?". */
    @Test fun aSermonWithPunctuationIsFoundOnDisk() {
        val dir = Files.createTempDirectory("vgr").toFile()
        try {
            for ((code, title) in listOf("59-1216" to "What Is The Holy Ghost?", "57-0410" to "Jehovah-Jireh #1", "62-0121E" to "Hear Ye Him!", "52-0900" to "God's Way That's Been Made For Us")) {
                File(dir, "$code ${vgrSafeTitle(title)} VGR.opus").writeBytes(ByteArray(3))
                assertEquals("$code ${vgrSafeTitle(title)} VGR.opus", SermonMedia.findVgrFile(dir, "ENG", code, title = title)?.name)
                // and a non-English one, saved the same way
                File(dir, "SWA$code ${vgrSafeTitle(title)} VGR.m4a").writeBytes(ByteArray(3))
                assertEquals("SWA$code ${vgrSafeTitle(title)} VGR.m4a", SermonMedia.findVgrFile(dir, "SWA", code, title = title)?.name)
            }
        } finally { dir.deleteRecursively() }
    }
}
