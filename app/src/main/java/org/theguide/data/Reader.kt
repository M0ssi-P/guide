package org.theguide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.theguide.audio.Audio
import parsers.vgr.models.Sermon

/**
 * The sermon open in the Table: which language, which sermon and paragraph. While its recording plays, the place comes
 * from the audio; otherwise from what the reader chose.
 */
object Reader {
    var iso3 by mutableStateOf(Prefs.readingLang); private set
    var sermon by mutableStateOf<Sermon?>(null); private set
    var items by mutableStateOf<List<Sentence>>(emptyList()); private set
    /** The paragraph chosen by hand (the audio's wins while it plays this sermon). */
    var paragraph by mutableIntStateOf(1)
    var flip by mutableIntStateOf(0); private set

    /** True when the player has this very sermon in this language loaded. */
    val audioHere get() = Audio.track?.let { it.iso3 == iso3 && it.sermon.dateCode == sermon?.dateCode } == true

    /** The paragraph being read: the spoken one when playing here. */
    val current: Int get() = if (audioHere) Audio.currentSentence()?.number ?: paragraph else paragraph

    suspend fun ensure() {
        if (sermon != null) return
        val t = Audio.track
        if (t != null) { iso3 = t.iso3; sermon = t.sermon; items = t.items; return }
        val r = Prefs.resume
        val lang = (r?.iso3 ?: Prefs.readingLang).takeIf { Tables.has(it) } ?: Tables.installed().firstOrNull() ?: return
        val s = r?.let { Tables.sermon(lang, it.code) } ?: Tables.sermons(lang).firstOrNull() ?: return
        open(lang, s, r?.paragraph ?: 1)
    }

    suspend fun open(lang: String, s: Sermon, para: Int = 1) {
        val full = Tables.full(lang, s)
        iso3 = lang; Prefs.readingLang = lang
        sermon = full; items = full.sentences(); paragraph = para; flip++
        Prefs.log("Read ${full.title}", "read")
    }

    /** The same sermon in another language; the place is kept. */
    suspend fun switchLanguage(lang: String) {
        val code = sermon?.dateCode ?: return
        val s = Tables.sermon(lang, code) ?: return
        val para = current
        val wasPlaying = audioHere && Audio.playing
        val at = if (audioHere) Audio.timeline() else null
        open(lang, s, para)
        if (wasPlaying) { Audio.play(lang, s); at?.let { Audio.seek(Audio.recordingTime(it)) } }
    }

    /** Play from paragraph [n] (loads the recording when needed). */
    suspend fun playFrom(n: Int) {
        val s = sermon ?: return
        paragraph = n
        if (!audioHere) Audio.play(iso3, s)
        Audio.playParagraph(n)
    }

    suspend fun toggle() {
        val s = sermon ?: return
        if (audioHere) Audio.toggle() else { Audio.play(iso3, s); Audio.playParagraph(paragraph) }
    }
}
