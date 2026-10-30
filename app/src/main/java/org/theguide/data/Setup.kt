package org.theguide.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.theguide.App
import parsers.bt.Letters
import java.util.Locale

/** One thing first launch downloads: a Table language or a language of letters. */
data class SetupItem(val id: String, val letter: Boolean, val code: String, val name: String, val sub: String, val mb: Double)

/**
 * First launch, as on the Launch boards: welcome → Table languages → letters → one download (the lamp grows with it) →
 * done, with "Waiting for internet" whenever the connection drops. Downloads carry on in the background after
 * "Start reading while it finishes".
 */
object Setup {
    enum class Step { Welcome, Table, Letters, Download, Offline, Done }

    /** The ten languages Branham Tabernacle publishes letters in: site code, own name, about how many MB. */
    val letterLanguages = listOf(
        Triple("en", "English", 3.1), Triple("fr", "Français", 2.9), Triple("es", "Español", 3.0), Triple("pt", "Português", 2.9),
        Triple("de", "Deutsch", 2.8), Triple("it", "Italiano", 2.7), Triple("no", "Norsk", 2.4), Triple("ro", "Română", 2.6),
        Triple("ru", "Русский", 3.3), Triple("uk", "Українська", 3.2),
    )

    var step by mutableStateOf(Step.Welcome); private set
    /** Bumped on every step change; the boards replay the enter animation by flipping its name. */
    var flip by mutableIntStateOf(0); private set
    var offers by mutableStateOf<List<TableOffer>>(emptyList()); private set
    var offersError by mutableStateOf(false); private set
    var query by mutableStateOf("")
    var tables by mutableStateOf<Set<String>>(emptySet())
    var lettersOn by mutableStateOf(true)
    var letterLangs by mutableStateOf<Set<String>>(emptySet())
    val progress = mutableStateMapOf<String, Int>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var watching = false

    /** The phone's language, as ISO3 and as the letters site's code. */
    private val phone = Locale.getDefault()
    val phoneIso3: String = runCatching { phone.isO3Language }.getOrDefault("eng")
    val phoneLetterLang: String = phone.language

    fun go(to: Step) { if (step != to) { step = to; flip++ } }

    init {
        tables = Prefs.tableLangs.ifEmpty { setOf(phoneIso3, "eng") }
        lettersOn = Prefs.lettersOn
        letterLangs = Prefs.letterLangs.takeIf { Prefs.tableLangs.isNotEmpty() }
            ?: (setOf("en") + listOfNotNull(phoneLetterLang.takeIf { l -> letterLanguages.any { it.first == l } }))
    }

    /** Fetches what the store offers (and, once the catalog is in, how many voices each has). */
    fun loadOffers() {
        if (offers.isNotEmpty()) return
        scope.launch {
            offersError = false
            runCatching { Tables.offers(withVoices = false) }.onSuccess { o ->
                offers = o
                // only ticks the phone's language and English where the store has them
                tables = tables.filter { t -> o.any { it.iso3 == t } }.toSet().ifEmpty { o.map { it.iso3 }.filter { it == "eng" }.toSet() }
                runCatching { Tables.offers(withVoices = true) }.onSuccess { offers = it }
            }.onFailure { offersError = true }
        }
    }

    fun toggleTable(iso3: String) { tables = if (iso3 in tables) tables - iso3 else tables + iso3 }
    fun toggleLetterLang(code: String) { letterLangs = if (code in letterLangs) letterLangs - code else letterLangs + code }

    val items: List<SetupItem>
        get() {
            val t = offers.filter { it.iso3 in tables }.map { o ->
                SetupItem("t${o.iso3}", false, o.iso3, o.localName, "The Table, every sermon’s text", (o.bytes ?: 0L) / 1e6)
            }
            val l = if (lettersOn) letterLanguages.filter { it.first in letterLangs }.map { (code, name, mb) ->
                SetupItem("l$code", true, code, name, "Bro. Joseph’s letters, 2015 to today", mb)
            } else emptyList()
            return t + l
        }

    /** Starts (or picks up) the one download of everything chosen. */
    fun start() {
        Prefs.tableLangs = tables
        Prefs.lettersOn = lettersOn
        Prefs.letterLangs = letterLangs
        Prefs.readingLang = tables.firstOrNull { it != "eng" } ?: tables.first()
        Prefs.letterLang = letterLangs.firstOrNull { it != "en" && it == phoneLetterLang } ?: "en"
        if (step != Step.Done) go(Step.Download)
        watchNetwork()
        run()
    }

    private fun run() {
        if (job?.isActive == true) return
        val list = items
        job = scope.launch {
            if (!online()) { go(Step.Offline); return@launch }
            val running = list.mapIndexed { i, it ->
                launch {
                    // the next one starts once the one before is a third of the way (as on the board)
                    if (i > 0) while ((progress[list[i - 1].id] ?: 0) <= 35) delay(250)
                    fetch(it)
                }
            }
            running.forEach { it.join() }
            if (list.all { (progress[it.id] ?: 0) >= 100 }) { if (step == Step.Download || step == Step.Offline) go(Step.Done) }
            else if (step == Step.Download) go(Step.Offline)
        }
    }

    private suspend fun fetch(it: SetupItem) {
        if ((progress[it.id] ?: 0) >= 100) return
        try {
            if (!it.letter) {
                if (Tables.has(it.code)) { progress[it.id] = 100; return }
                while (Prefs.wifiOnly && !onWifi()) delay(1000)
                Tables.download(it.code) { read, total -> if (total > 0) progress[it.id] = (read * 99 / total).toInt().coerceAtLeast(1) }
            } else {
                val book = Letters.book(it.code)
                val watch = scope.launch {
                    // the archive has about 60 pages; count them as they come
                    book.sync.collect { s -> if (s.running) progress[it.id] = (s.page * 100 / 60).coerceIn(1, 95) }
                }
                withContext(Dispatchers.IO) { book.sync(full = true) }
                watch.cancel()
                if (book.sync.value.error != null || book.all.value.isEmpty()) throw java.io.IOException(book.sync.value.error)
            }
            progress[it.id] = 100
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
        }
    }

    fun retry() { go(Step.Download); run() }

    /** "Open The Guide": setup is over; downloads still running carry on. */
    fun finish() = Prefs.finishSetup()

    private val cm get() = App.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    fun online(): Boolean = cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    private fun onWifi(): Boolean = cm.getNetworkCapabilities(cm.activeNetwork)?.let {
        it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    } == true

    /** Back online while waiting: carry on by itself. */
    private fun watchNetwork() {
        if (watching) return
        watching = true
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { scope.launch { if (step == Step.Offline) retry() } }
        })
    }
}
