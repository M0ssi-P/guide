package org.theguide.ui.tv

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import design.Palette
import kotlinx.coroutines.launch
import org.theguide.data.Drop
import org.theguide.data.Songs
import org.theguide.ui.phone.LinkEntry
import songs.link
import org.theguide.ui.*
import org.theguide.ui.phone.Shelf
import org.theguide.ui.phone.SongParts
import org.theguide.ui.phone.shopLooks
import org.theguide.ui.phone.spineOf

/** TVSongs board. Songs arrive here from the shop, a USB drive or a file; writing songs is on the phone only. */
@Composable
fun TvSongs() {
    var tab by remember { mutableStateOf("library") }
    var flip by remember { mutableIntStateOf(0) }
    var link by remember { mutableStateOf(false) }
    val screen by oneShot(Unit, 600)
    val swapA by oneShot(flip, 550)
    LaunchedEffect(Unit) { Songs.refreshShop() }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Box(Modifier.fillMaxSize().background(C.Backdrop).ambient(gold(.12f), .7f, .4f)) {
        Box(Modifier.fillMaxSize().enter({ screen })) {
            Row(Modifier.padding(start = 130.dp, end = 60.dp, top = 40.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                S("Songs", 48f, c(Palette.Title), Modifier.weight(1f), weight = 300, tracking = -.6f)
                listOf("library" to "Library", "shop" to "Shop", "move" to "Import and export").forEachIndexed { i, (id, label) ->
                    val on = tab == id
                    Box(Modifier.height(50.dp).tvFocus(RoundedCornerShape(25.dp), { tab = id; flip++ }, Role.Tab).clip(RoundedCornerShape(25.dp)).background(if (on) gold(.16f) else white(.05f))
                        .then(if (i == 0) Modifier.focusRequester(first) else Modifier).padding(horizontal = 24.dp), contentAlignment = Alignment.Center) { T(label, 17f, if (on) C.Gold else C.Soft, weight = 600) }
                }
            }
            Box(Modifier.padding(start = 130.dp, end = 60.dp, top = 130.dp, bottom = 40.dp).fillMaxSize().enter({ swapA }, Ease.Swap)) {
                when (tab) { "library" -> Library(); "shop" -> Shop(); else -> Move { link = true } }
            }
        }
        if (link) TvPanel(c(Palette.TvPanel), { link = false }) { LinkEntry(tv = true) { link = false } }
    }
}

@Composable
private fun Library() {
    var book by remember { mutableIntStateOf(0) }
    var song by remember { mutableIntStateOf(0) }
    val books = Songs.books
    val b = books.getOrNull(book) ?: books.last()
    val s = b.songs.getOrNull(song)
    val swap by oneShot(book * 7 + song, 600)
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(50.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Shelf(books, book, 170f, .7f, 16f, tv = true) { book = it; song = 0 }
            Column(Modifier.padding(top = 30.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                b.songs.withIndex().chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                        row.forEach { (i, sg) ->
                            Row(
                                Modifier.weight(1f).tvFocus(RoundedCornerShape(18.dp), { song = i }).clip(RoundedCornerShape(18.dp)).background(if (i == song) gold(.12f) else Color.Transparent)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                S(sg.n, 26f, if (i == song) C.Gold else C.Faint, Modifier.width(44.dp), align = TextAlign.End)
                                Column(Modifier.weight(1f)) { S(sg.title, 20f, c(Palette.Title), maxLines = 1, ellipsis = true); T(sg.first, 14f, C.Meta, maxLines = 1, ellipsis = true) }
                            }
                        }
                        if (row.size == 1) Box(Modifier.weight(1f))
                    }
                }
                if (b.songs.isEmpty()) S(if (b.mine) "Songs you write on the phone show here once you move them over." else "This song book is empty.", 20f, C.Meta, Modifier.padding(16.dp), italic = true)
            }
        }
        Box(
            Modifier.width(430.dp).fillMaxHeight()
                .then(if ((book * 7 + song) % 2 == 1) Modifier.unfold({ swap }, Ease.Swap, TransformOrigin(0f, .5f)) else Modifier.enter({ swap }, Ease.Swap))
                .cssShadow(0f, 40f, 60f, -30f, Color.Black.copy(alpha = .9f), corners(4f, 16f, 16f, 4f)).clip(corners(4f, 16f, 16f, 4f)).background(C.Paper).grain(.3f)
                .drawBehind { drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = .35f), Color.Transparent), 0f, 18.dp.toPx())) }
                .verticalScroll(rememberScrollState()).padding(horizontal = 34.dp, vertical = 30.dp)
        ) {
            Column {
                S(s?.n ?: "", 50f, C.Wax, weight = 300, lineHeight = 1f)
                S(s?.title ?: "", 28f, C.PaperInk, Modifier.padding(top = 6.dp, bottom = 18.dp), lineHeight = 1.1f)
                SongParts(s?.parts ?: emptyList(), 18f, 24f, 12f, 14f, 20f)
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
        S(if (ids == null) "Looking in the store…" else "No song books in the store yet. They will show here when there are some.", 24f, C.Meta, Modifier.padding(top = 60.dp), italic = true, weight = 300)
        return
    }
    val id = ids[pick.coerceIn(0, ids.lastIndex)]
    val look = shopLooks[pick % shopLooks.size]
    val owned = Songs.books.any { it.id == id }
    val p = Songs.progress[id]
    val ft by oneShot(pick, 700)
    val time by clock()
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(60.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(360.dp).height(420.dp)) {
            Box(
                Modifier.offset(30.dp, 10.dp).size(270.dp, 360.dp).then(if (pick % 2 == 1) Modifier.pop({ ft }, Ease.O12) else Modifier.enter({ ft }, Ease.O12))
                    .graphicsLayer { rotationY = -16f; rotationZ = -2f; cameraDistance = perspective(1000f) * density }
                    .cssShadow(40f, 40f, 70f, -20f, Color.Black.copy(alpha = .9f), corners(8f, 20f, 20f, 8f)).clip(corners(8f, 20f, 20f, 8f))
                    .background(Brush.linearGradient(listOf(c(look.first), C.Backdrop), Offset.Zero, Offset(270f * 3, 360f * 3))).grain(.4f)
                    .drawBehind {
                        drawRect(Color.Black.copy(alpha = .18f), size = androidx.compose.ui.geometry.Size(14.dp.toPx(), size.height))
                        drawRoundRect(c(look.second).copy(alpha = .53f), Offset(34.dp.toPx(), 36.dp.toPx()), androidx.compose.ui.geometry.Size(size.width - 58.dp.toPx(), size.height - 72.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()), style = Stroke(1.dp.toPx()))
                        val x = -300f + 600f * keyframes(phase(time, 3500), Ease.InOut, 0f to 0f, 1f to 1f)
                        drawRect(Brush.linearGradient(listOf(Color.Transparent, white(.22f), Color.Transparent), Offset(x * density, 0f), Offset((x + 220) * density, 80f * density)))
                    }
            ) { S(Songs.nameOf(id), 32f, c(look.third), Modifier.padding(start = 44.dp, end = 34.dp, top = 100.dp).fillMaxWidth(), lineHeight = 1.15f, align = TextAlign.Center) }
        }
        Column(Modifier.weight(1f)) {
            S(Songs.nameOf(id), 44f, c(Palette.Title), weight = 300)
            T("From the Guide store, free", 18f, C.Sub, Modifier.padding(top = 6.dp, bottom = 20.dp))
            val fill = animate((p ?: 0) / 100f, 400, Ease.Linear)
            Box(
                Modifier.height(56.dp).widthIn(min = 220.dp).clip(RoundedCornerShape(28.dp)).background(if (owned || p != null) white(.08f) else C.Gold)
                    .drawBehind { drawRect(gold(.35f), size = size.copy(width = size.width * fill)) }
                    .tvFocus(RoundedCornerShape(28.dp), { if (!owned && p == null) scope.launch { Songs.get(id) } }).padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center,
            ) { T(when { owned -> "In your library"; p != null -> "Downloading $p%"; else -> "Get it" }, 18f, if (owned || p != null) C.Gold else C.OnGold, weight = 600) }
            Row(Modifier.padding(top = 40.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ids.take(6).forEachIndexed { i, sid ->
                    val l = shopLooks[i % shopLooks.size]
                    val lift = animate(if (i == pick) 1f else 0f, 400, Ease.O13)
                    Box(
                        Modifier.size(130.dp, 170.dp).graphicsLayer { translationY = -8f * lift * density; rotationZ = -1.5f * lift }
                            .cssShadow(0f, 20f, 30f, -14f, Color.Black.copy(alpha = .9f), corners(5f, 12f, 12f, 5f)).clip(corners(5f, 12f, 12f, 5f))
                            .background(Brush.linearGradient(listOf(c(l.first), C.Backdrop), Offset.Zero, Offset(130f * 4, 170f * 4))).grain(.35f)
                            .drawBehind { drawRect(Color.Black.copy(alpha = .18f), size = androidx.compose.ui.geometry.Size(8.dp.toPx(), size.height)) }
                            .tvFocus(corners(5f, 12f, 12f, 5f), { pick = i }).label(Songs.nameOf(sid))
                    ) { S(Songs.nameOf(sid), 16f, c(l.third), Modifier.padding(start = 16.dp, end = 10.dp, top = 46.dp).fillMaxWidth(), lineHeight = 1.15f, align = TextAlign.Center) }
                }
            }
        }
    }
}

@Composable
private fun Move(onLink: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var out by remember { mutableStateOf(Songs.books.filter { it.songs.isNotEmpty() }.map { it.id }.toSet()) }
    var fmt by remember { mutableStateOf("guide") }
    var exp by remember { mutableIntStateOf(0) }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { scope.launch { Songs.import(it) } } }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        uri?.let { target -> scope.launch {
            exp = 5
            val f = Songs.export(out.toList(), fmt) { exp = it.coerceAtLeast(5) }
            ctx.contentResolver.openOutputStream(target)?.use { o -> f.inputStream().use { it.copyTo(o) } }
            exp = 100
        } }
    }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(60.dp)) {
        Column(Modifier.weight(1f)) {
            S("Bring songs in", 30f, c(Palette.Title))
            // while this tab is open the TV waits on the Wi-Fi; its QR code is the link a phone scans to send here
            DisposableEffect(Unit) { Drop.receive(); onDispose { Drop.stopReceiving() } }
            Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                QrTile(Drop.waiting?.link() ?: "theguide://drop")
                Column(Modifier.weight(1f)) {
                    T("Open The Guide on your phone, choose Move songs, and point it here. Your songbooks arrive over the Wi-Fi.", 17f, c(Palette.Stone), lineHeight = 26f)
                    val last = Drop.arrived.lastOrNull()
                    val came by oneShot(last, 450)
                    when {
                        last != null -> T("“$last” arrived", 15f, C.Gold, Modifier.padding(top = 10.dp).pop({ came }, Ease.O13), weight = 600)
                        Drop.waiting == null -> T("Connect the TV to Wi-Fi to receive songs.", 15f, C.Meta, Modifier.padding(top = 10.dp))
                    }
                }
            }
            listOf(Triple("▣", "From a USB drive", "Plug it into the TV"), Triple("☁︎", "From a link", "Type a short share code")).forEachIndexed { i, (icon, name, meta) ->
                Row(
                    Modifier.padding(top = 12.dp).fillMaxWidth().tvFocus(RoundedCornerShape(20.dp), { if (i == 0) open.launch(arrayOf("*/*")) else onLink() }).clip(RoundedCornerShape(20.dp)).background(white(.05f)).padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(Modifier.size(46.dp).clip(CircleShape).background(white(.07f)), contentAlignment = Alignment.Center) { T(icon, 20f, c(Palette.Title)) }
                    Column(Modifier.weight(1f)) { T(name, 18f, c(Palette.Title)); T(meta, 14f, C.Meta) }
                }
            }
        }
        Column(Modifier.weight(1f)) {
            S("Send songs out", 30f, c(Palette.Title))
            Column(Modifier.padding(top = 14.dp)) {
                Songs.books.forEachIndexed { i, bk ->
                    val on = bk.id in out
                    Row(Modifier.fillMaxWidth().tvFocus(RoundedCornerShape(16.dp), { out = if (on) out - bk.id else out + bk.id; exp = 0 }, Role.Checkbox).clip(RoundedCornerShape(16.dp)).padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(12.dp, 40.dp).clip(RoundedCornerShape(2.dp)).background(c(spineOf(bk, i).first)))
                        S(bk.name, 20f, c(Palette.Title), Modifier.weight(1f))
                        TickBox(on, 28f, 9f, 15f)
                    }
                }
            }
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("guide" to "Guide file", "text" to "Plain text", "pdf" to "PDF").forEach { (id, name) ->
                    Box(Modifier.weight(1f).height(50.dp).tvFocus(RoundedCornerShape(25.dp), { fmt = id; exp = 0 }, Role.RadioButton).clip(RoundedCornerShape(25.dp)).background(if (fmt == id) gold(.16f) else white(.05f)), contentAlignment = Alignment.Center) {
                        T(name, 15f, if (fmt == id) C.Gold else C.Soft, weight = 600)
                    }
                }
            }
            val fill = animate(if (exp >= 100) 0f else exp / 100f, 400, Ease.Linear)
            Box(
                Modifier.padding(top = 16.dp).fillMaxWidth().height(58.dp).clip(RoundedCornerShape(29.dp)).background(C.Gold).drawBehind { drawRect(white(.35f), size = size.copy(width = size.width * fill)) }
                    .tvFocus(RoundedCornerShape(29.dp), { if (exp == 0) save.launch("songbooks." + when (fmt) { "pdf" -> "pdf"; "text" -> "txt"; else -> "zip" }) }),
                contentAlignment = Alignment.Center,
            ) { T(if (exp >= 100) "Saved to the USB drive" else if (exp > 0) "Packing $exp%" else "Save ${out.size} songbooks to USB", 18f, C.OnGold, weight = 600) }
        }
    }
}

