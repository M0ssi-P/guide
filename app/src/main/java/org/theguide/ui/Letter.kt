package org.theguide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import design.Palette
import kotlinx.coroutines.delay
import org.theguide.App
import org.theguide.data.Prefs
import parsers.bt.Letter
import parsers.bt.Letters
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The Sunday service that goes with a letter: its own small player, so the sermon player keeps its place. */
object ServicePlayer {
    private val player by lazy { ExoPlayer.Builder(App.context).build() }
    var url by mutableStateOf<String?>(null); private set
    var playing by mutableStateOf(false); private set
    var fraction by mutableDoubleStateOf(0.0); private set
    var minutes by mutableStateOf<Int?>(null); private set

    fun toggle(src: String) {
        if (url != src) { url = src; player.setMediaItem(MediaItem.fromUri(src)); player.prepare(); player.play(); return }
        if (player.isPlaying) player.pause() else player.play()
    }

    suspend fun poll() {
        while (true) {
            playing = player.isPlaying
            val d = player.duration
            if (d > 0) { fraction = player.currentPosition.toDouble() / d; minutes = (d / 60000).toInt() }
            delay(400)
        }
    }
}

/** Sizes of one letter layout (phone home, phone library, TV home, TV library…), all from the boards. */
class LetterSpec(
    val chipH: Float, val chipPad: Float, val chipFont: Float, val chipsBottom: Float,
    val date: Float, val greetingTop: Float, val greeting: Float,
    val p: Float, val pTop: Float, val q: Float, val qTop: Float, val qPad: Float, val qMark: Float, val qMarkLeft: Float, val qMarkTop: Float,
    val signTop: Float, val sign: Float, val gridTop: Float, val gridPad: Float, val gridFont: Float, val gridValue: Float, val gridColGap: Float, val gridRowGap: Float,
    val svcTop: Float, val svcBottom: Float, val svcFont: Float, val rowPadY: Float, val rowPadX: Float, val rowRadius: Float, val rowGap: Float,
    val play: Float, val playIcon: Float, val title: Float, val meta: Float, val pillH: Float, val pillPad: Float, val pillFont: Float,
)

object LetterSpecs {
    val PhoneHome = LetterSpec(30f, 12f, 12f, 18f, 14f, 10f, 25f, 16f, 14f, 18f, 16f, 18f, 46f, 8f, -14f, 20f, 18f, 18f, 14f, 14f, 15f, 14f, 6f,
        22f, 8f, 12f, 12f, 14f, 22f, 12f, 44f, 16f, 16f, 12f, 22f, 9f, 10f)
    val PhoneLibrary = LetterSpec(28f, 11f, 11f, 17f, 13f, 10f, 24f, 16f, 13f, 17f, 15f, 17f, 44f, 8f, -13f, 19f, 17f, 17f, 13f, 13f, 14f, 13f, 6f,
        21f, 8f, 12f, 11f, 13f, 21f, 11f, 42f, 15f, 15f, 11f, 21f, 9f, 10f)
    val TvHome = LetterSpec(36f, 14f, 14f, 22f, 17f, 12f, 30f, 20f, 17f, 21f, 19f, 22f, 55f, 10f, -17f, 24f, 22f, 22f, 17f, 16f, 18f, 17f, 7f,
        26f, 10f, 15f, 14f, 17f, 26f, 14f, 53f, 19f, 19f, 14f, 26f, 11f, 13f)
    val TvWeek = LetterSpec(34f, 14f, 14f, 21f, 16f, 12f, 29f, 19f, 16f, 20f, 18f, 21f, 53f, 9f, -16f, 23f, 21f, 21f, 16f, 16f, 17f, 16f, 7f,
        25f, 9f, 14f, 14f, 16f, 25f, 14f, 51f, 18f, 18f, 14f, 25f, 10f, 12f)
    val TvLetters = LetterSpec(33f, 13f, 13f, 20f, 15f, 11f, 28f, 18f, 15f, 19f, 18f, 20f, 51f, 9f, -15f, 22f, 20f, 20f, 15f, 15f, 16f, 15f, 7f,
        24f, 9f, 14f, 13f, 15f, 24f, 13f, 48f, 18f, 18f, 13f, 24f, 10f, 12f)
}

private val labels = mapOf(
    "en" to Triple("Message:", "Scripture:", "Associated Services"),
    "fr" to Triple("Message :", "Écriture :", "Services associés"),
    "es" to Triple("Mensaje:", "Escritura:", "Servicios asociados"),
)

/** "Saturday, October 24, 2026" in the letter's own language. */
fun letterDate(l: Letter, lang: String): String {
    val loc = Locale.forLanguageTag(lang)
    val f = when (lang) {
        "en" -> DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)
        "es", "pt" -> DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", loc)
        else -> DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", loc)
    }
    return l.letterDate.format(f).replaceFirstChar { it.titlecase(loc) }
}

/** The same letter in another language (the service is the same; the site gives each language its own page). */
fun letterIn(l: Letter, lang: String): Letter? {
    val all = Letters.book(lang).all.value
    return all.firstOrNull { it.id == l.id } ?: all.firstOrNull { it.code == l.code && it.letterDate == l.letterDate }
}

/** Scripture references in the letter's quotes and text ("Ephesians 1:3-5"), for the Scripture line. */
private val scriptureRef = Regex("""\b(?:[1-3] )?[A-Z][a-z]+\.? \d{1,3}:\d{1,3}(?:[-–]\d{1,3})?""")

/**
 * A letter from Bro. Joseph on paper: language chips, the date, greeting, paragraphs and quotes, the signature, the
 * Message and Scripture lines, and the Sunday service to play.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LetterBody(letter: Letter, lang: String, onLang: (String) -> Unit, spec: LetterSpec, tv: Boolean = false) {
    val (msgLabel, scrLabel, svcLabel) = labels[lang] ?: labels.getValue("en")
    var detailed by androidx.compose.runtime.remember(letter.id, lang) { mutableStateOf(letter) }
    LaunchedEffect(letter.id, lang) { if (!letter.detailed) Letters.book(lang).detail(letter.id)?.let { detailed = it } }
    LaunchedEffect(Unit) { ServicePlayer.poll() }
    val l = detailed
    val swap by oneShot(lang, 500)
    Column {
        FlowRow(Modifier.padding(bottom = spec.chipsBottom.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Prefs.letterLangs.sortedBy { o -> listOf("en", "fr", "es", "pt", "de", "it", "no", "ro", "ru", "uk").indexOf(o) }.forEach { code ->
                val sel = code == lang
                val name = org.theguide.data.Setup.letterLanguages.firstOrNull { it.first == code }?.second ?: code
                Box(
                    Modifier.height(spec.chipH.dp).clip(RoundedCornerShape((spec.chipH / 2).dp))
                        .background(if (sel) C.PaperInk else Color(42, 36, 24).copy(alpha = .08f))
                        .then(if (tv) Modifier.tvFocus(RoundedCornerShape((spec.chipH / 2).dp), { onLang(code) }) else Modifier.press({ onLang(code) }, role = Role.RadioButton))
                        .padding(horizontal = spec.chipPad.dp),
                    contentAlignment = Alignment.Center,
                ) { T(name, spec.chipFont, if (sel) c(Palette.PaperChipText) else C.PaperSign, weight = 600) }
            }
        }
        Column(Modifier.enter({ swap }, Ease.Swap)) {
            S(letterDate(l, lang), spec.date, C.PaperMuted, italic = true)
            S(l.salutation ?: "", spec.greeting, C.PaperInk, Modifier.padding(top = spec.greetingTop.dp), lineHeight = 1.2f)
            l.sections.flatMap { it.blocks }.forEach { b ->
                if (!b.isQuote) S(b.text, spec.p, C.PaperBody, Modifier.padding(top = spec.pTop.dp), lineHeight = 1.7f)
                else Box(Modifier.padding(top = spec.qTop.dp).fillMaxWidth().height(IntrinsicSize.Min)) {
                    Box(Modifier.width(3.dp).fillMaxHeight().background(C.Wax))
                    S("“", spec.qMark, C.Wax.copy(alpha = .18f), Modifier.offset(spec.qMarkLeft.dp, spec.qMarkTop.dp))
                    S(b.text, spec.q, C.PaperInk, Modifier.padding(start = spec.qPad.dp, top = 4.dp, bottom = 4.dp), italic = true, lineHeight = 1.6f)
                }
            }
            S("Bro. Joseph Branham", spec.sign, C.PaperSign, Modifier.padding(top = spec.signTop.dp), italic = true)
            val scripture = scriptureRef.findAll(l.plainText).map { it.value }.distinct().take(4).joinToString(", ")
            Column(
                Modifier.padding(top = spec.gridTop.dp).fillMaxWidth().drawBehind {
                    drawLine(Color(42, 36, 24).copy(alpha = .25f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())))
                }.padding(top = spec.gridPad.dp),
                verticalArrangement = Arrangement.spacedBy(spec.gridRowGap.dp),
            ) {
                GridRow(msgLabel, "${l.code} ${l.title}", spec)
                if (scripture.isNotEmpty()) GridRow(scrLabel, scripture, spec)
            }
        }
        T(svcLabel, spec.svcFont, C.PaperMuted, Modifier.padding(top = spec.svcTop.dp, bottom = spec.svcBottom.dp), weight = 600)
        ServiceRow(l, spec, tv)
    }
}

@Composable
private fun GridRow(label: String, value: String, spec: LetterSpec) = Row(horizontalArrangement = Arrangement.spacedBy(spec.gridColGap.dp)) {
    T(label, spec.gridFont, C.Wax, weight = 600)
    S(value, spec.gridValue, C.PaperSign, Modifier.weight(1f))
}

@Composable
private fun ServiceRow(l: Letter, spec: LetterSpec, tv: Boolean) {
    val src = l.audioUrl
    val mine = src != null && ServicePlayer.url == src
    val on = mine && ServicePlayer.playing
    val pct = if (mine) ServicePlayer.fraction.toFloat() else 0f
    val fill = animate(pct, 450, Ease.Linear)
    val shape = RoundedCornerShape(spec.rowRadius.dp)
    val sunday = (l.serviceDate ?: l.letterDate.plusDays(1)).format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH))
    val len = ServicePlayer.minutes?.takeIf { mine }?.let { m -> if (m >= 60) "${m / 60} h ${m % 60} min" else "$m min" }
    Row(
        Modifier.fillMaxWidth().clip(shape).background(C.PaperInk)
            .drawBehind { drawRect(gold(.14f), size = size.copy(width = size.width * fill)) }
            .padding(horizontal = spec.rowPadX.dp, vertical = spec.rowPadY.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spec.rowGap.dp),
    ) {
        Box(
            Modifier.size(spec.play.dp).clip(CircleShape).background(C.Gold)
                .then(if (tv) Modifier.tvFocus(CircleShape, { src?.let(ServicePlayer::toggle) }) else Modifier.press({ src?.let(ServicePlayer::toggle) }, label = if (on) "Pause the service" else "Play the service")),
            contentAlignment = Alignment.Center,
        ) { Ico(if (on) Icons.Pause else Icons.Play, spec.playIcon, C.OnGold) }
        Column(Modifier.weight(1f)) {
            S("$sunday: ${l.code} ${l.title}", spec.title, c(Palette.PaperChipText), maxLines = 2)
            T(
                when { src == null -> "Sunday service"; on -> "Playing, ${Math.round(pct * 100)}%${len?.let { " of $it" } ?: ""}"; else -> "Sunday service${len?.let { ", $it" } ?: ""}" },
                spec.meta, c(Palette.PaperPill),
            )
        }
        l.kind?.let { kind ->
            Box(Modifier.height(spec.pillH.dp).clip(RoundedCornerShape((spec.pillH / 2).dp)).background(gold(.16f)).padding(horizontal = spec.pillPad.dp), contentAlignment = Alignment.Center) {
                T(kind, spec.pillFont, C.Gold, weight = 700, tracking = .5f)
            }
        }
    }
}
