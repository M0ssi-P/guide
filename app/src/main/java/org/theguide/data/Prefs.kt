package org.theguide.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.theguide.App
import java.time.LocalDate

/** Small settings kept on the device. Each setter saves at once. */
object Prefs {
    private val sp by lazy { App.context.getSharedPreferences("guide", Context.MODE_PRIVATE) }
    private val gson = Gson()

    private fun str(key: String, def: String) = sp.getString(key, def) ?: def
    private fun set(key: String, v: Any?) = sp.edit().apply {
        when (v) {
            null -> remove(key)
            is String -> putString(key, v)
            is Int -> putInt(key, v)
            is Boolean -> putBoolean(key, v)
            is Long -> putLong(key, v)
            is Float -> putFloat(key, v)
            is Set<*> -> putStringSet(key, v.map { it.toString() }.toSet())
            else -> putString(key, gson.toJson(v))
        }
    }.apply()

    var setupDone by mutableStateOf(sp.getBoolean("setup_done", false)); private set
    fun finishSetup() { setupDone = true; set("setup_done", true) }

    /** Table languages to keep (ISO 639-3, e.g. "swa"). */
    var tableLangs: Set<String>
        get() = sp.getStringSet("table_langs", emptySet()) ?: emptySet()
        set(v) = set("table_langs", v)
    var lettersOn: Boolean
        get() = sp.getBoolean("letters_on", true)
        set(v) = set("letters_on", v)
    /** Letter languages (the site's two-letter codes). */
    var letterLangs: Set<String>
        get() = sp.getStringSet("letter_langs", setOf("en")) ?: setOf("en")
        set(v) = set("letter_langs", v)
    var wifiOnly by mutableStateOf(sp.getBoolean("wifi_only", true)); private set
    fun updateWifiOnly(v: Boolean) { wifiOnly = v; set("wifi_only", v) }

    /** The Table language being read, ISO3. */
    var readingLang: String
        get() = str("reading_lang", tableLangs.firstOrNull { it != "eng" } ?: tableLangs.firstOrNull() ?: "eng")
        set(v) = set("reading_lang", v)
    /** The letter language shown first. */
    var letterLang: String
        get() = str("letter_lang", letterLangs.firstOrNull() ?: "en")
        set(v) = set("letter_lang", v)

    // the English voice underneath a translation
    var mix by mutableStateOf(sp.getInt("voice_mix", 15)); private set
    fun updateMix(v: Int) { mix = (Math.round(v / 5f) * 5).coerceIn(10, 90); set("voice_mix", mix) }
    var split by mutableStateOf(sp.getBoolean("voice_split", true)); private set
    fun updateSplit(v: Boolean) { split = v; set("voice_split", v) }

    var liveName: String
        get() = str("live_name", "")
        set(v) = set("live_name", v)

    // reading look, per surface ("table", "bible", "live", "tv-table", ...)
    fun theme(surface: String) = str("$surface.theme", "night")
    fun tone(surface: String) = str("$surface.tone", "lamp")
    fun size(surface: String, def: Int) = sp.getInt("$surface.size", def)
    fun face(surface: String) = str("$surface.face", "serif")
    fun gap(surface: String, def: String = "snug") = str("$surface.gap", def)
    fun saveLook(surface: String, theme: String, tone: String, size: Int, face: String, gap: String) {
        sp.edit().putString("$surface.theme", theme).putString("$surface.tone", tone).putInt("$surface.size", size)
            .putString("$surface.face", face).putString("$surface.gap", gap).apply()
    }

    // ---------- reading days (the lamp) ----------
    var readDays by mutableStateOf(sp.getStringSet("read_days", emptySet())?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()); private set

    /** Marks today as a day in the Word (reading or listening). */
    fun markRead(day: LocalDate = LocalDate.now()) {
        val d = day.toEpochDay()
        if (d in readDays) return
        readDays = readDays + d
        set("read_days", readDays.map { it.toString() }.toSet())
    }

    /** Days in a row up to today (or yesterday, while today isn't read yet). */
    fun streak(today: LocalDate = LocalDate.now()): Int {
        var d = today.toEpochDay().let { if (it in readDays) it else it - 1 }
        var n = 0
        while (d in readDays) { n++; d-- }
        return n
    }

    // ---------- saved quotes, verses and highlights ----------
    data class Saved(val kind: String, val text: String, val source: String, val key: String, val at: Long = System.currentTimeMillis())
    var saved by mutableStateOf<List<Saved>>(sp.getString("saved", null)?.let { runCatching { gson.fromJson<List<Saved>>(it, object : TypeToken<List<Saved>>() {}.type) }.getOrNull() } ?: emptyList())
        private set
    fun isSaved(key: String) = saved.any { it.key == key }
    fun toggleSaved(item: Saved) { saved = if (isSaved(item.key)) saved.filterNot { it.key == item.key } else listOf(item) + saved; set("saved", saved) }
    fun removeSaved(key: String) { saved = saved.filterNot { it.key == key }; set("saved", saved) }

    /** Bible highlight colour per verse key ("JHN.3.16" in a version). */
    var highlights by mutableStateOf<Map<String, String>>(sp.getString("highlights", null)?.let { runCatching { gson.fromJson<Map<String, String>>(it, object : TypeToken<Map<String, String>>() {}.type) }.getOrNull() } ?: emptyMap())
        private set
    fun setHighlight(key: String, color: String?) { highlights = if (color == null) highlights - key else highlights + (key to color); set("highlights", highlights) }

    // ---------- activity ----------
    data class Activity(val what: String, val kind: String, val at: Long, val minutes: Int = 0)
    var activity by mutableStateOf<List<Activity>>(sp.getString("activity", null)?.let { runCatching { gson.fromJson<List<Activity>>(it, object : TypeToken<List<Activity>>() {}.type) }.getOrNull() } ?: emptyList())
        private set
    fun log(what: String, kind: String, minutes: Int = 0) {
        activity = (listOf(Activity(what, kind, System.currentTimeMillis(), minutes)) + activity).take(200)
        set("activity", activity)
        markRead()
    }

    // ---------- where listening stopped ----------
    data class Resume(val iso3: String, val code: String, val title: String, val seconds: Double, val duration: Double, val paragraph: Int)
    var resume by mutableStateOf<Resume?>(sp.getString("resume", null)?.let { runCatching { gson.fromJson(it, Resume::class.java) }.getOrNull() })
        private set
    fun saveResume(r: Resume?) { resume = r; set("resume", r) }

    // ---------- Bible ----------
    var bibleVersion: Int
        get() = sp.getInt("bible_version", 0)
        set(v) = set("bible_version", v)
    var bibleOther: Int
        get() = sp.getInt("bible_other", 1)
        set(v) = set("bible_other", v)
    var bibleBook: String
        get() = str("bible_book", "JHN")
        set(v) = set("bible_book", v)
    var bibleChapter: Int
        get() = sp.getInt("bible_chapter", 3)
        set(v) = set("bible_chapter", v)
    var bibleMode: String
        get() = str("bible_mode", "scroll")
        set(v) = set("bible_mode", v)

    // ---------- updates ----------
    var updateSeen: String
        get() = str("update_seen", "")
        set(v) = set("update_seen", v)

    // ---------- letters opened ----------
    var lettersRead by mutableStateOf(sp.getStringSet("letters_read", emptySet()) ?: emptySet()); private set
    fun isLetterRead(id: String) = id in lettersRead
    fun markLetterRead(id: String) { lettersRead = lettersRead + id; set("letters_read", lettersRead) }

    // ---------- songs ----------
    var songbooks: Set<String>
        get() = sp.getStringSet("songbooks", emptySet()) ?: emptySet()
        set(v) = set("songbooks", v)
}
