package org.theguide.ui.phone

import androidx.compose.ui.text.withStyle
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import design.Palette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import org.theguide.data.Part
import org.theguide.data.Song
import org.theguide.data.Songbook
import org.theguide.data.Drop
import org.theguide.data.Songs
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import songs.link
import org.theguide.ui.*

/** Spine looks from the boards (colour, ink, height, width), repeating; your own songs are the brass one. */
internal val spineLooks = listOf(Triple(0xFF7A2E2E, 0xFFF2D9A8, 230f to 42f), Triple(0xFF23365E, 0xFFF3C969, 210f to 34f), Triple(0xFF2F4A36, 0xFFE8DCC0, 196f to 38f))
internal val mineLook = Triple(0xFFB9893A, 0xFF2A1C08, 168f to 30f)
internal fun spineOf(b: Songbook, i: Int) = if (b.mine) mineLook else spineLooks[i % spineLooks.size]
/** Shop covers: (colour, band, ink). */
internal val shopLooks = listOf(Triple(0xFF3A2A52, 0xFFF3C969, 0xFFE9C7FF), Triple(0xFF6B2B22, 0xFFF2D9A8, 0xFFF2D9A8), Triple(0xFF1E4250, 0xFFBFE4EA, 0xFFBFE4EA), Triple(0xFF4A5A2A, 0xFFEDE6B8, 0xFFEDE6B8))

/** PhoneSongs board (phones can write songs; the TV can't). */
@Composable
fun PhoneSongs(haze: HazeState) {
    var tab by remember { mutableStateOf("library") }
    var book by remember { mutableIntStateOf(0) }
    var song by remember { mutableStateOf<Song?>(null) }
    var sheet by remember { mutableStateOf("") }
    var flip by remember { mutableIntStateOf(0) }
    val swapA by oneShot(flip, 500)
    val books = Songs.books
    val b = books.getOrNull(book) ?: books.last()
    LaunchedEffect(Unit) { Songs.refreshShop() }
    val screen by oneShot(Unit, 500)

    Box(Modifier.fillMaxSize().background(C.Night).screenGlow(gold(.12f))) {
        Column(Modifier.fillMaxSize().hazeSource(haze).padding(top = statusTop()).verticalScroll(rememberScrollState()).enter({ screen })) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 140.dp)) {
                HRow(gap = 8f) {
                    S("Songs", 32f, C.Ink, Modifier.weight(1f), weight = 300, tracking = -.4f)
                    RoundButton({ sheet = "transfer" }, "Import or export songs") { Ico(Icons.Transfer, 18f, C.Text) }
                    RoundButton({ sheet = "create" }, "Write a new song", bg = C.Gold) { Ico(Icons.Plus, 18f, C.OnGold) }
                }
                Segmented(listOf("library" to "Library", "shop" to "Shop"), tab, Look(design.ReadingThemes.Night, design.Tones.Lamp), Modifier.padding(top = 14.dp),
                    pillShadow = false, bg = white(.06f), pill = white(.14f), on = C.Ink, off = C.Sub) { tab = it; flip++ }
                Column(Modifier.enter({ swapA }, Ease.Swap)) {
                    if (tab == "library") {
                        Shelf(books, book) { book = it; flip++ }
                        S(b.name, 21f, C.Ink, Modifier.padding(top = 26.dp))
                        T("${b.songs.size} songs, on this phone", 12.5f, C.Sub, Modifier.padding(top = 2.dp, bottom = 12.dp))
                        if (b.songs.isEmpty()) S(if (b.mine) "No songs of yours yet. Tap + to write one." else "This song book is empty.", 16f, C.Meta, Modifier.padding(vertical = 20.dp), italic = true)
                        b.songs.forEachIndexed { i, s ->
                            val t by oneShot(book to i, 450, i * 50)
                            Row(
                                Modifier.rise({ t }, Ease.Swap).fillMaxWidth().drawBehind { drawLine(white(.06f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
                                    .press({ song = s }).padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                S(s.n, 22f, if (b.mine) C.Gold else C.Faint, Modifier.width(38.dp), align = TextAlign.End)
                                Column(Modifier.weight(1f)) { S(s.title, 17f, C.Ink); T(s.first, 12.5f, C.Meta, maxLines = 1, ellipsis = true) }
                                if (b.mine) T("Yours", 11.5f, C.Gold)
                            }
                        }
                    } else Shop()
                }
            }
        }
        song?.let { s -> SongPage(s, b.name) { song = null } }
        when (sheet) {
            "create" -> CreateSheet(haze) { sheet = ""; tab = "library"; book = books.lastIndex; flip++ }
            "transfer" -> TransferSheet(haze, tv = false) { sheet = "" }
        }
    }
}

/** Song books standing on a shelf; the chosen one leans out. */
@Composable
internal fun Shelf(books: List<Songbook>, sel: Int, height: Float = 160f, scale: Float = .68f, nameSize: Float = 13f, tv: Boolean = false, onPick: (Int) -> Unit) {
    Box(Modifier.padding(top = 20.dp).fillMaxWidth().height(height.dp)) {
        Box(Modifier.align(Alignment.BottomCenter).offset(y = 8.dp).fillMaxWidth().height(10.dp)
            .cssShadow(0f, 10f, 18f, -6f, Color.Black.copy(alpha = .8f), RoundedCornerShape(3.dp)).clip(RoundedCornerShape(3.dp))
            .background(Brush.verticalGradient(listOf(c(Palette.ShelfTop), c(Palette.ShelfBottom)))))
        Row(Modifier.align(Alignment.BottomCenter).padding(horizontal = 6.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            books.forEachIndexed { i, bk ->
                val (bg, ink, hw) = spineOf(bk, i)
                val on = i == sel
                val lift = animate(if (on) 1f else 0f, 450, Ease.O13)
                Box(
                    Modifier.size(hw.second.dp, (hw.first * scale).dp)
                        .graphicsLayer { transformOrigin = TransformOrigin(0f, 1f); translationY = -14f * lift * density; rotationZ = -4f * lift }
                        .clip(corners(4f, 4f, 2f, 2f)).background(c(bg)).grain(.3f)
                        .drawBehind {
                            drawRect(white(.12f), size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height))
                            drawRect(Color.Black.copy(alpha = .25f), Offset(size.width - 4.dp.toPx(), 0f), androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height))
                            val band = c(ink).copy(alpha = .4f)
                            drawRect(band, Offset(4.dp.toPx(), 10.dp.toPx()), androidx.compose.ui.geometry.Size(size.width - 8.dp.toPx(), 2.dp.toPx()))
                            drawRect(band, Offset(4.dp.toPx(), size.height - 12.dp.toPx()), androidx.compose.ui.geometry.Size(size.width - 8.dp.toPx(), 2.dp.toPx()))
                        }
                        .choiceOrFocus(tv, corners(4f, 4f, 2f, 2f), { onPick(i) }, Role.Tab).label("${bk.name}, ${bk.songs.size} songs"),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.graphicsLayer { rotationZ = -90f }.width((hw.first * scale - 16).dp), contentAlignment = Alignment.Center) {
                        S(bk.name, nameSize, c(ink), maxLines = 1, ellipsis = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun Shop() {
    val scope = rememberCoroutineScope()
    val ids = Songs.shop
    var pick by remember { mutableIntStateOf(0) }
    if (ids.isNullOrEmpty()) {
        S(if (ids == null) "Looking in the store…" else "No song books in the store yet. They will show here when there are some.", 17f, C.Meta, Modifier.padding(top = 40.dp).fillMaxWidth(), italic = true, align = TextAlign.Center)
        return
    }
    val id = ids[pick.coerceIn(0, ids.lastIndex)]
    val look = shopLooks[pick % shopLooks.size]
    val owned = Songs.books.any { it.id == id }
    val p = Songs.progress[id]
    val t by oneShot(pick, 700)
    val shimmerT by clock()
    Box(Modifier.padding(top = 22.dp).fillMaxWidth().height(240.dp)) {
        Box(
            Modifier.align(Alignment.TopCenter).offset(y = 6.dp).size(172.dp, 222.dp).pop({ t }, Ease.O12)
                .graphicsLayer { rotationY = -14f; rotationZ = -2f; cameraDistance = perspective(900f) * density }
                .cssShadow(30f, 30f, 50f, -20f, Color.Black.copy(alpha = .9f), corners(6f, 14f, 14f, 6f))
                .clip(corners(6f, 14f, 14f, 6f)).background(Brush.linearGradient(listOf(c(look.first), C.Backdrop), Offset(0f, 0f), Offset(240f * 3, 300f * 3))).grain(.4f)
                .drawBehind {
                    drawRect(Color.Black.copy(alpha = .18f), size = androidx.compose.ui.geometry.Size(10.dp.toPx(), size.height))
                    drawRect(white(.08f), Offset(10.dp.toPx(), 0f), androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height))
                    drawRoundRect(c(look.second).copy(alpha = .53f), Offset(22.dp.toPx(), 26.dp.toPx()), androidx.compose.ui.geometry.Size(size.width - 38.dp.toPx(), size.height - 52.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()), style = Stroke(1.dp.toPx()))
                    val x = -200f + 400f * keyframes(phase(shimmerT, 3500), Ease.InOut, 0f to 0f, 1f to 1f)
                    drawRect(Brush.linearGradient(listOf(Color.Transparent, white(.22f), Color.Transparent), Offset(x * density, 0f), Offset((x + 150) * density, 60f * density)))
                }
        ) {
            S(Songs.nameOf(id), 19f, c(look.third), Modifier.padding(start = 30.dp, end = 22.dp, top = 54.dp).fillMaxWidth(), lineHeight = 1.15f, align = TextAlign.Center)
            S("a songbook", 12f, c(look.third).copy(alpha = .75f), Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp), italic = true)
        }
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        S(Songs.nameOf(id), 22f, C.Ink, Modifier.padding(top = 6.dp))
        T("From the Guide store", 12.5f, C.Sub, Modifier.padding(top = 2.dp, bottom = 14.dp))
        val fill = animate((p ?: 0) / 100f, 400, Ease.Linear)
        Box(
            Modifier.height(44.dp).width(androidx.compose.foundation.layout.IntrinsicSize.Max).clip(RoundedCornerShape(22.dp)).background(if (owned || p != null) white(.08f) else C.Gold)
                .drawBehind { drawRect(gold(.35f), size = size.copy(width = size.width * fill)) }
                .press({ if (!owned && p == null) scope.launch { Songs.get(id) } }).padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center,
        ) { T(when { owned -> "In your library"; p != null -> "Downloading $p%"; else -> "Get it, free" }, 14f, if (owned || p != null) C.Gold else C.OnGold, Modifier.width(IntrinsicSize.Max).padding(horizontal = 0.dp), weight = 600, align = TextAlign.Center) }
    }
    S("More songbooks", 20f, C.Ink, Modifier.padding(top = 28.dp, bottom = 10.dp))
    Row(Modifier.offset(x = (-20).dp).width(LocalConfiguration.current.screenWidthDp.dp).horizontalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        ids.forEachIndexed { i, sid ->
            val l = shopLooks[i % shopLooks.size]
            val lift = animate(if (i == pick) 1f else 0f, 400, Ease.O13)
            Column(Modifier.width(118.dp).press({ pick = i })) {
                Box(
                    Modifier.fillMaxWidth().height(150.dp).graphicsLayer { translationY = -6f * lift * density; rotationZ = -1.5f * lift }
                        .cssShadow(0f, 18f, 26f, -14f, Color.Black.copy(alpha = .9f), corners(4f, 10f, 10f, 4f)).clip(corners(4f, 10f, 10f, 4f))
                        .background(Brush.linearGradient(listOf(c(l.first), C.Backdrop), Offset(0f, 0f), Offset(118f * 4, 150f * 4))).grain(.35f)
                        .drawBehind { drawRect(Color.Black.copy(alpha = .18f), size = androidx.compose.ui.geometry.Size(7.dp.toPx(), size.height)) }
                ) { S(Songs.nameOf(sid), 15f, c(l.third), Modifier.padding(start = 14.dp, end = 8.dp, top = 40.dp).fillMaxWidth(), lineHeight = 1.15f, align = TextAlign.Center) }
                S(Songs.nameOf(sid), 14.5f, C.Ink, Modifier.padding(top = 10.dp))
                T(if (Songs.books.any { it.id == sid }) "In your library" else "Free", 12f, C.Meta)
            }
        }
    }
}

/** A song opened on paper, the chorus indented and in italics. */
@Composable
internal fun SongParts(parts: List<Part>, size: Float = 19f, markW: Float = 22f, gap: Float = 14f, bottom: Float = 20f, indent: Float = 18f) {
    parts.forEach { pt ->
        Row(Modifier.padding(start = (if (pt.chorus) indent else 0f).dp, bottom = bottom.dp), horizontalArrangement = Arrangement.spacedBy(gap.dp)) {
            S(pt.mark, 14f, C.Wax, Modifier.width(markW.dp).padding(top = 3.dp))
            S(pt.text, size, C.PaperInk, italic = pt.chorus, lineHeight = 1.55f)
        }
    }
}

@Composable
private fun SongPage(s: Song, book: String, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val t by oneShot(s, 600)
    Box(Modifier.fillMaxSize().unfold({ t }, Ease.Swap).background(C.Paper).grain(.3f).clickable0()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 28.dp, end = 28.dp, top = 64.dp, bottom = 60.dp)) {
            S(s.n, 54f, C.Wax, weight = 300, lineHeight = 1f)
            S(s.title, 30f, C.PaperInk, Modifier.padding(top = 6.dp, end = 60.dp), lineHeight = 1.1f)
            S(book, 14f, C.PaperMuted, Modifier.padding(top = 6.dp, bottom = 26.dp), italic = true)
            SongParts(s.parts)
        }
        Box(Modifier.align(Alignment.TopEnd).padding(top = 60.dp, end = 20.dp).size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = .06f)).press(onClose, label = "Close the song"), contentAlignment = Alignment.Center) {
            Ico(Icons.Close, 14f, C.PaperInk)
        }
    }
}

@Composable
private fun BoxScope.CreateSheet(haze: HazeState, onSaved: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var lyrics by remember { mutableStateOf("") }
    var into by remember { mutableStateOf(Songs.MINE) }
    Sheet({ onSaved() }, haze) {
        S("A new song", 24f, C.Text)
        Box(Modifier.padding(top = 14.dp).fillMaxWidth().height(50.dp).drawBehind { drawLine(white(.14f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }, contentAlignment = Alignment.CenterStart) {
            if (title.isEmpty()) S("Title", 22f, Color(0xFF757575))
            BasicTextField(title, { title = it }, Modifier.fillMaxWidth(), textStyle = style(22f, Face.Serif, color = C.Ink), singleLine = true, cursorBrush = SolidColor(C.Ink))
        }
        Row(Modifier.padding(top = 14.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Songs.books.forEach { bk -> Chip(bk.name, into == bk.id, height = 34f, size = 12.5f) { into = bk.id } }
        }
        Row(Modifier.padding(top = 14.dp).height(230.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(18.dp)).background(white(.05f)).padding(14.dp)) {
                if (lyrics.isEmpty()) T("Type the verses. Start a part with Chorus to mark it.", 13.5f, Color(0xFF757575), lineHeight = 1.5f)
                BasicTextField(lyrics, { lyrics = it }, Modifier.fillMaxSize(), textStyle = style(13.5f, color = C.Text, lineHeight = 1.5f), cursorBrush = SolidColor(C.Text))
            }
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(corners(6f, 14f, 14f, 6f)).background(C.Paper)
                    .drawBehind { drawRect(Color.Black.copy(alpha = .08f), size = androidx.compose.ui.geometry.Size(6.dp.toPx(), size.height)) }.padding(14.dp).label("Preview")
            ) {
                S(title.ifBlank { "Untitled" }, 15f, C.PaperInk, Modifier.padding(bottom = 8.dp))
                Songs.parse(lyrics).take(4).forEach { pv ->
                    androidx.compose.foundation.text.BasicText(androidx.compose.ui.text.buildAnnotatedString {
                        withStyle(style(11.5f, Face.Serif, color = C.Wax).toSpanStyle()) { append("${pv.mark} ") }; append(pv.text)
                    }, Modifier.padding(start = if (pv.chorus) 18.dp else 0.dp, bottom = 8.dp), style = style(11.5f, Face.Serif, italic = pv.chorus, color = C.PaperInk, lineHeight = 1.45f))
                }
            }
        }
        GoldButton("Add to ${Songs.books.firstOrNull { it.id == into }?.name ?: "My songs"}", { Songs.addMine(title, lyrics, into); onSaved() }, Modifier.padding(top = 16.dp).fillMaxWidth(), height = 52f, size = 15f, weight = 600, shadow = false)
    }
}

/** Bring songs in / send them out; the TV variant names its own sources. */
@Composable
internal fun BoxScope.TransferSheet(haze: HazeState, tv: Boolean, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var dir by remember { mutableStateOf("import") }
    var out by remember { mutableStateOf(Songs.books.filter { it.songs.isNotEmpty() }.map { it.id }.toSet()) }
    var fmt by remember { mutableStateOf("guide") }
    var exp by remember { mutableIntStateOf(0) }
    val time by clock()
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { scope.launch { Songs.import(it); onDismiss() } } }
    // "nearby": this phone waits for songs over the Wi-Fi; "link": a pasted share link
    var view by remember { mutableStateOf("") }
    // the device last sent to, and how it went (null while sending)
    var sent by remember { mutableStateOf<Pair<String, Boolean?>?>(null) }
    fun sendTo(r: songs.DropReceiver) {
        if (out.isEmpty() || (sent != null && sent?.second == null)) return
        sent = r.name to null
        scope.launch { sent = r.name to Drop.send(r, out.toList()) }
    }
    val scan = rememberLauncherForActivityResult(com.journeyapps.barcodescanner.ScanContract()) { r ->
        val text = r.contents ?: return@rememberLauncherForActivityResult
        songs.parseDropLink(text)?.let(::sendTo) ?: run { sent = "that code" to false }
    }
    Sheet(onDismiss, haze) {
      // registered after the sheet's own, so Back steps out of a sub-view before it closes the sheet
      BackHandler(view.isNotEmpty()) { view = "" }
      when (view) {
        "nearby" -> NearbyReceive()
        "link" -> LinkEntry(tv = false) { onDismiss() }
        else -> {
        S("Move songs", 24f, C.Text)
        Segmented(listOf("import" to "Bring in", "export" to "Send out"), dir, Look(design.ReadingThemes.Night, design.Tones.Lamp), Modifier.padding(top = 14.dp),
            pillShadow = false, bg = white(.06f), pill = white(.14f), on = C.Ink, off = C.Sub) { dir = it; exp = 0 }
        if (dir == "import") {
            Box(
                Modifier.padding(top = 18.dp).fillMaxWidth().height(170.dp).clip(RoundedCornerShape(26.dp))
                    .drawBehind {
                        ellipseGradient(.5f, 1f, 0f to gold(.14f), .7f to Color.Transparent)
                        drawRoundRect(gold(.4f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(26.dp.toPx()), style = Stroke(1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
                    }.press({ pickFile.launch(arrayOf("*/*")) }),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val r = keyframes(phase(time, 3200), Ease.Css, 0f to 0f, .5f to 1f, 1f to 0f)
                    S("♪", 40f, C.Gold, Modifier.graphicsLayer { alpha = r; translationY = (1 - r) * 16 * density })
                    S("Choose a file", 17f, C.Ink); T("Guide songbooks, text or OpenLyrics", 12f, C.Sub)
                }
            }
            listOf(Triple("▤", "From Files", "A songbook someone sent you"), Triple("⌁", "From a computer nearby", "The Guide on the same Wi-Fi"), Triple("☁︎", "From a link", "Paste a share link from a friend")).forEachIndexed { i, (icon, name, meta) ->
                Row(
                    Modifier.fillMaxWidth().drawBehind { drawLine(white(.06f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
                        .press({ when (i) { 0 -> pickFile.launch(arrayOf("*/*")); 1 -> view = "nearby"; else -> view = "link" } }).padding(horizontal = 4.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(white(.06f)), contentAlignment = Alignment.Center) { T(icon, 16f, C.Text) }
                    Column(Modifier.weight(1f)) { T(name, 14.5f, C.Text); T(meta, 12f, C.Meta) }
                }
            }
        } else {
            T("Which songbooks", 13f, C.Sub, Modifier.padding(top = 18.dp, bottom = 8.dp))
            Songs.books.forEachIndexed { i, bk ->
                val on = bk.id in out
                Row(Modifier.fillMaxWidth().press({ out = if (on) out - bk.id else out + bk.id; exp = 0 }, role = Role.Checkbox).padding(horizontal = 4.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(10.dp, 34.dp).clip(RoundedCornerShape(2.dp)).background(c(spineOf(bk, i).first)))
                    S(bk.name, 16f, C.Text, Modifier.weight(1f))
                    TickBox(on, 22f, 7f, 12f)
                }
            }
            T("As", 13f, C.Sub, Modifier.padding(top = 14.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("guide" to "Guide file", "text" to "Plain text", "pdf" to "PDF").forEach { (id, name) ->
                    Box(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(21.dp)).background(if (fmt == id) gold(.16f) else white(.05f)).press({ fmt = id; exp = 0 }, role = Role.RadioButton), contentAlignment = Alignment.Center) {
                        T(name, 12.5f, if (fmt == id) C.Gold else C.Soft, weight = 600)
                    }
                }
            }
            val fill = animate(if (exp >= 100) 0f else exp / 100f, 400, Ease.Linear)
            Box(
                Modifier.padding(top = 18.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(C.Gold)
                    .drawBehind { drawRect(white(.35f), size = size.copy(width = size.width * fill)) }
                    .press({
                        if (exp == 0) scope.launch {
                            exp = 5
                            val f = Songs.export(out.toList(), fmt) { exp = it.coerceAtLeast(5) }
                            exp = 100
                            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
                            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType(if (fmt == "pdf") "application/pdf" else if (fmt == "text") "text/plain" else "application/zip")
                                .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null))
                        }
                    }),
                contentAlignment = Alignment.Center,
            ) { T(if (exp >= 100) "Ready to share" else if (exp > 0) "Packing $exp%" else "Export ${out.size} songbooks", 15f, C.OnGold, weight = 600) }
            // or straight to a TV or phone on the same Wi-Fi: one it hears, or the one whose code the camera reads
            DisposableEffect(Unit) { Drop.look(); onDispose { Drop.stopLooking() } }
            T("Or send to a device nearby", 13f, C.Sub, Modifier.padding(top = 20.dp, bottom = 4.dp))
            (Drop.nearby.map { Triple("▭", it.name, it) } + Triple("⌗", "Scan a code", null)).forEach { (icon, name, r) ->
                val state = sent?.takeIf { r != null && it.first == r.name }
                Row(
                    Modifier.fillMaxWidth().drawBehind { drawLine(white(.06f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
                        .press({ if (r != null) sendTo(r) else scan.launch(com.journeyapps.barcodescanner.ScanOptions().setPrompt("Point at the code on the TV").setBeepEnabled(false).setOrientationLocked(false)) }, enabled = out.isNotEmpty())
                        .padding(horizontal = 4.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(white(.06f)), contentAlignment = Alignment.Center) { T(icon, 16f, C.Text) }
                    Column(Modifier.weight(1f)) {
                        T(name, 14.5f, C.Text)
                        val (meta, tint) = when {
                            r == null -> "The code on a TV's Import and export" to C.Meta
                            state == null -> "The Guide on the same Wi-Fi" to C.Meta
                            state.second == null -> "Sending ${out.size} songbooks…" to C.Meta
                            state.second == true -> "Sent" to C.Gold
                            else -> "Couldn't reach it" to c(0xFFF28B8B)
                        }
                        T(meta, 12f, tint)
                    }
                }
            }
            if (sent?.first == "that code") T("That code isn't from The Guide.", 12f, c(0xFFF28B8B), Modifier.padding(top = 6.dp))
            if (Drop.nearby.isEmpty()) T("Open Import and export on a TV, or From a computer nearby on another phone, and it shows up here.", 12f, C.Meta, Modifier.padding(top = 8.dp), lineHeight = 18f)
        }
        }
      }
    }
}

/** This phone waiting for songs from a computer (or another phone) on the same Wi-Fi. */
@Composable
private fun NearbyReceive() {
    DisposableEffect(Unit) { Drop.receive(); onDispose { Drop.stopReceiving() } }
    val time by clock()
    val me = Drop.waiting
    S("From a computer nearby", 24f, C.Text)
    T("On the computer, open Songs, then Export & import, and choose Send to a phone or TV nearby. Pick this phone.", 13.5f, C.Sub, Modifier.padding(top = 8.dp), lineHeight = 20f)
    Box(Modifier.padding(top = 22.dp).fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        if (me != null) repeat(3) { k -> Box(Modifier.size(110.dp).ripple({ time }, 2400, k * 800).clip(CircleShape).background(gold(.10f))) }
        QrTile(me?.link() ?: "theguide://drop", 150f)
    }
    S(me?.name ?: "No Wi-Fi", 20f, C.Ink, Modifier.fillMaxWidth().padding(top = 10.dp), align = TextAlign.Center)
    T(if (me != null) "Waiting · code ${me.code}" else "Join a Wi-Fi network to receive songs.", 13f, C.Meta, Modifier.fillMaxWidth(), align = TextAlign.Center)
    Drop.arrived.forEach { name ->
        val a by oneShot(name, 450)
        Row(
            Modifier.padding(top = 10.dp).fillMaxWidth().pop({ a }, Ease.O13).clip(RoundedCornerShape(16.dp)).background(gold(.10f)).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) { T("♪", 16f, C.Gold); T(name, 14.5f, C.Text, Modifier.weight(1f)); T("Arrived", 12f, C.Gold, weight = 600) }
    }
}

/** A share link (or, on the TV, a short share code) to bring songs in from. */
@Composable
internal fun LinkEntry(tv: Boolean, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<String?>(null) } // null idle, "" fetching, "!" failed, else what arrived
    val f = if (tv) 1.3f else 1f
    val shape = RoundedCornerShape((26 * f).dp)
    S("From a link", 24f * f, C.Text)
    T(if (tv) "Type a share link, or the short code of a songbook in the shop." else "Paste a share link from a friend: a Guide songbook, a text or an OpenLyrics file.", 13.5f * f, C.Sub, Modifier.padding(top = 8.dp), lineHeight = 20f * f)
    val field = remember { androidx.compose.ui.focus.FocusRequester() }
    val button = remember { androidx.compose.ui.focus.FocusRequester() }
    var typing by remember { mutableStateOf(false) }
    val done = state != null && state != "" && state != "!"
    fun go() { if (done) onDone() else if (text.isNotBlank() && state != "") scope.launch { state = ""; state = Songs.importLinkOrCode(text) ?: "!" } }
    if (!tv) LaunchedEffect(Unit) { runCatching { field.requestFocus() } }
    // the field itself takes D-pad focus on the TV (gold ring while typing); Down or Go moves on to the button
    Box(
        Modifier.padding(top = 18.dp).fillMaxWidth().height((52 * f).dp)
            .then(if (tv) Modifier.drawBehind { if (typing) drawRoundRect(C.Gold, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2), style = Stroke(3.dp.toPx())) } else Modifier.press({ runCatching { field.requestFocus() } }))
            .clip(shape).background(white(.06f)).padding(horizontal = (18 * f).dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (text.isEmpty()) T(if (tv) "Link or code" else "https://", 15f * f, C.Faint)
        BasicTextField(
            text, { text = it.trim(); state = null },
            Modifier.fillMaxWidth().focusRequester(field).onFocusChanged { typing = it.isFocused }.focusProperties { down = button },
            textStyle = style(15f * f, color = C.Ink), singleLine = true, cursorBrush = SolidColor(C.Ink),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri, imeAction = androidx.compose.ui.text.input.ImeAction.Go),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = { if (tv) runCatching { button.requestFocus() }; go() }),
        )
    }
    if (!tv) {
        val clip = androidx.compose.ui.platform.LocalClipboardManager.current
        T("Paste", 13f, C.Gold, Modifier.padding(top = 10.dp).press({ clip.getText()?.text?.let { text = it.trim(); state = null } }), weight = 600)
    }
    when (val st = state) {
        null, "" -> {}
        "!" -> T("Couldn't read that ${if (tv) "link or code" else "link"}.", 13f * f, c(0xFFF28B8B), Modifier.padding(top = 12.dp))
        else -> T("“$st” is in your songs", 13f * f, C.Gold, Modifier.padding(top = 12.dp), weight = 600)
    }
    Box(
        Modifier.padding(top = 18.dp).fillMaxWidth().height((52 * f).dp).focusRequester(button).choiceOrFocus(tv, shape, ::go).clip(shape).background(if (text.isBlank() && !done) gold(.35f) else C.Gold),
        contentAlignment = Alignment.Center,
    ) { T(when { state == "" -> "Fetching…"; done -> "Done"; else -> "Bring it in" }, 15f * f, C.OnGold, weight = 600) }
}
