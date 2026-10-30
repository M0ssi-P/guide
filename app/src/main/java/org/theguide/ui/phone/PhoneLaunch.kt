package org.theguide.ui.phone

import androidx.compose.ui.text.withStyle
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import design.Palette
import org.theguide.data.Setup
import org.theguide.data.Setup.Step
import org.theguide.data.SetupItem
import org.theguide.data.TableOffer
import org.theguide.ui.C
import org.theguide.ui.Ease
import org.theguide.ui.Face
import org.theguide.ui.Gap
import org.theguide.ui.GoldButton
import org.theguide.ui.GrowingLamp
import org.theguide.ui.Ico
import org.theguide.ui.Icons
import org.theguide.ui.RoundButton
import org.theguide.ui.S
import org.theguide.ui.Spine
import org.theguide.ui.StepBars
import org.theguide.ui.Switch
import org.theguide.ui.T
import org.theguide.ui.TickBox
import org.theguide.ui.animate
import org.theguide.ui.c
import org.theguide.ui.choice
import org.theguide.ui.corners
import org.theguide.ui.cssShadow
import org.theguide.ui.enter
import org.theguide.ui.gold
import org.theguide.ui.insetRing
import org.theguide.ui.label
import org.theguide.ui.oneShot
import org.theguide.ui.pop
import org.theguide.ui.press
import org.theguide.ui.rise
import org.theguide.ui.screenGlow
import org.theguide.ui.style
import org.theguide.ui.svgPath
import org.theguide.ui.white
import kotlin.math.cos
import kotlin.math.sin

/** How lit the lamp is for the current step (the board's `lit`). */
internal fun launchLit(step: Step, overall: Float) = when (step) {
    Step.Done -> 1f
    Step.Download, Step.Offline -> .35f + .65f * overall
    Step.Welcome -> .55f
    else -> .4f
}

/** Download totals: (items done, overall 0..1 weighted by size, total MB). */
internal fun launchTotals(items: List<SetupItem>, progress: Map<String, Int>): Triple<Int, Float, Double> {
    val mb = items.sumOf { it.mb }
    val done = items.count { (progress[it.id] ?: 0) >= 100 }
    val overall = if (items.isEmpty() || mb <= 0) 0f else (items.sumOf { minOf(100, progress[it.id] ?: 0) * it.mb } / (mb * 100)).toFloat()
    return Triple(done, overall, mb)
}

internal fun offerMeta(o: TableOffer): String {
    val mb = o.bytes?.let { "${Math.round(it / 1e6)} MB of text" } ?: "Text"
    val voice = when {
        o.iso3 == "eng" -> "brother Branham’s own voice"
        o.voices == null -> null
        o.voices > 0 -> "voice for ${"%,d".format(o.voices)} sermons"
        else -> "text only"
    }
    return listOfNotNull(mb, voice).joinToString(", ")
}

internal fun offerTip(o: TableOffer, device: String) = when (o.iso3) {
    Setup.phoneIso3 -> "Your $device’s language. "
    "eng" -> "For the voice underneath. "
    else -> ""
}

/** PhoneLaunch board: first launch. */
@Composable
fun PhoneLaunch(onOpen: () -> Unit) {
    val step = Setup.step
    LaunchedEffect(Unit) { Setup.loadOffers() }
    val items = Setup.items
    val (done, overall, mb) = launchTotals(items, Setup.progress)
    val lit = launchLit(step, overall)
    val glowA = animate(.08f + .14f * lit, 1200)
    val screen by oneShot(Unit, 500)
    val swap by oneShot(Setup.flip, if (step == Step.Welcome || step == Step.Done || step == Step.Offline) 600 else 550)
    val order = listOf(Step.Table, Step.Letters, Step.Download)
    val ix = order.indexOf(step)

    BoxWithConstraints(Modifier.fillMaxSize().background(C.Backdrop).screenGlow(gold(glowA))) {
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val minH = maxOf(780.dp, maxHeight - top)
        Column(
            Modifier.fillMaxSize().padding(top = top).verticalScroll(rememberScrollState()).enter({ screen })
        ) {
            Column(Modifier.fillMaxWidth().heightIn(min = minH).padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 34.dp)) {
                if (ix >= 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoundButton({ Setup.go(if (ix > 0) order[ix - 1] else Step.Welcome) }, "Back") { Ico(Icons.Back, 16f, C.Text) }
                    StepBars(3, ix, Modifier.weight(1f).semanticsLabel("Step ${ix + 1} of 3"))
                }
                Column(Modifier.weight(1f).fillMaxWidth().enter({ swap }, Ease.Css)) {
                    when (step) {
                        Step.Welcome -> Welcome(lit)
                        Step.Table -> TableStep()
                        Step.Letters -> LettersStep(mb)
                        Step.Download -> DownloadStep(lit, items, done, overall, mb)
                        Step.Done -> DoneStep(lit, onOpen)
                        Step.Offline -> OfflineStep(done, items.size)
                    }
                }
            }
        }
    }
}

private fun Modifier.semanticsLabel(s: String) = this.label(s)

@Composable
private fun ColumnScope.Welcome(lit: Float) {
    val r1 by oneShot(Setup.flip, 1000, 300); val r2 by oneShot(Setup.flip, 1000, 500); val r3 by oneShot(Setup.flip, 1000, 600)
    val r4 by oneShot(Setup.flip, 1000, 800); val r5 by oneShot(Setup.flip, 1000, 900)
    Box(Modifier.weight(1f))
    GrowingLamp(lit, Modifier.align(Alignment.CenterHorizontally))
    S("The Guide", 46f, c(Palette.Title), Modifier.fillMaxWidth().padding(top = 26.dp).rise({ r1 }), weight = 300, tracking = -.8f, align = TextAlign.Center)
    S("“Thy word is a lamp unto my feet, and a light unto my path.”", 18f, c(Palette.Brass),
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp).rise({ r2 }), italic = true, lineHeight = 1.5f, align = TextAlign.Center)
    T("Psalm 119:105", 13f, C.Meta, Modifier.fillMaxWidth().padding(top = 6.dp).rise({ r3 }), align = TextAlign.Center)
    Box(Modifier.weight(1.2f))
    T("Two short choices, then everything is kept on this phone so it works without internet.", 13.5f, C.Sub,
        Modifier.fillMaxWidth().padding(bottom = 14.dp).rise({ r4 }), lineHeight = 20f, align = TextAlign.Center)
    GoldButton("Begin", { Setup.go(Step.Table) }, Modifier.fillMaxWidth().rise({ r5 }))
}

@Composable
private fun ColumnScope.TableStep() {
    S("Which languages for the Table?", 34f, c(Palette.Title), Modifier.padding(top = 20.dp), weight = 300, lineHeight = 1.08f, tracking = -.4f)
    T("Every sermon’s words in each language you pick. Voices download later, one sermon at a time, as you listen.", 14f, C.Sub, Modifier.padding(top = 8.dp), lineHeight = 20f)
    SearchField(Setup.query, { Setup.query = it }, "Search ${Setup.offers.size} languages", Modifier.padding(top = 16.dp))
    val q = Setup.query.trim().lowercase()
    val shown = Setup.offers.withIndex().filter { (_, o) -> q.isEmpty() || "${o.localName} ${o.name}".lowercase().contains(q) }
    Column(
        Modifier.weight(1f, fill = false).heightIn(max = 410.dp).padding(top = 10.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (Setup.offers.isEmpty()) T(if (Setup.offersError) "Couldn’t reach the Table store. Check the internet and come back to this step." else "Looking up the languages…", 13f, C.Meta, Modifier.padding(top = 12.dp))
        shown.forEach { (i, o) -> TableLangRow(o, i, o.iso3 in Setup.tables) { Setup.toggleTable(o.iso3) } }
    }
    Box(Modifier.weight(.001f))
    val n = Setup.tables.size
    GoldButton(if (n > 0) "Continue with $n ${if (n == 1) "language" else "languages"}" else "Pick at least one", { if (n > 0) Setup.go(Step.Letters) },
        Modifier.fillMaxWidth().padding(top = 12.dp), alpha = if (n > 0) 1f else .4f)
}

@Composable
internal fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, height: Float = 46f, size: Float = 15f, bg: Color = white(.06f), ink: Color = c(Palette.Title), iconColor: Color = C.Sub, iconSize: Float = 16f) {
    Row(
        modifier.fillMaxWidth().height(height.dp).clip(RoundedCornerShape((height / 2).dp)).background(bg).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Ico(Icons.Search, iconSize, iconColor)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) T(placeholder, size, Color(0xFF757575))
            BasicTextField(value, onChange, Modifier.fillMaxWidth(), textStyle = style(size, color = ink), singleLine = true, cursorBrush = SolidColor(ink))
        }
    }
}

@Composable
private fun TableLangRow(o: TableOffer, i: Int, on: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) gold(.09f) else white(.03f), org.theguide.ui.tw(300), label = "bg")
    val ring by animateColorAsState(if (on) gold(.45f) else Color.Transparent, org.theguide.ui.tw(300), label = "ring")
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(bg).insetRing(1.5.dp, ring, shape).choice(onClick, Role.Checkbox).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spine(c(Palette.Spines[i % 10]), 14f, 42f, on)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                S(o.localName, 18f, c(Palette.Title))
                if (o.localName != o.name) T(" ${o.name}", 12f, C.Meta, Modifier.padding(bottom = 3.dp))
            }
            val tip = offerTip(o, "phone")
            androidx.compose.foundation.text.BasicText(
                androidx.compose.ui.text.buildAnnotatedString {
                    if (tip.isNotEmpty()) withStyle(style(12f, weight = 600, color = C.Gold).toSpanStyle()) { append(tip) }
                    append(offerMeta(o))
                },
                style = style(12f, color = C.Meta),
            )
        }
        TickBox(on)
    }
}

@Composable
internal fun Envelopes(width: Float = 220f, height: Float = 118f, seal: Float = 32f, sealTop: Float = 42f, sealFont: Float = 11f, animated: Boolean = true, boxH: Float = 150f, centered: Boolean = true, left: Float = 0f) {
    Box(Modifier.fillMaxWidth().height(boxH.dp)) {
        repeat(4) { i ->
            val t by oneShot(Setup.flip, 700, i * 120)
            val rot = -6f + i * 4f
            val x = (i - 1.5f) * 10f
            val rad = Math.toRadians(rot.toDouble())
            Box(
                Modifier.align(if (centered) Alignment.TopCenter else Alignment.TopStart)
                    .offset(x = left.dp, y = (6 + i * 9).dp)
                    .graphicsLayer { rotationZ = rot; translationX = (x * cos(rad)).toFloat() * density; translationY = (x * sin(rad)).toFloat() * density }
                    .then(if (animated) Modifier.rise({ t }, Ease.Swap) else Modifier)
                    .size(width.dp, height.dp)
                    .cssShadow(0f, 14f, 30f, -14f, Color.Black.copy(alpha = .8f), RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp)).background(c(Palette.EnvelopePapers[i]))
                    .drawBehind {
                        val p = androidx.compose.ui.graphics.Path().apply { moveTo(0f, 0f); lineTo(size.width / 2, size.height * 58f / 118f); lineTo(size.width, 0f) }
                        drawPath(p, Color(42, 36, 24).copy(alpha = .16f), style = Stroke(1.2.dp.toPx()))
                    }
            ) {
                WaxSeal(seal, sealFont, Modifier.align(Alignment.TopCenter).offset(y = sealTop.dp))
            }
        }
    }
}

/** The red wax seal with "BT" pressed in. */
@Composable
internal fun WaxSeal(diameter: Float, font: Float, modifier: Modifier = Modifier, shadow: Boolean = true) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier.size(diameter.dp)
            .then(if (shadow) Modifier.cssShadow(0f, 2f, 4f, 0f, Color(60, 10, 15).copy(alpha = .4f), shape) else Modifier)
            .clip(shape)
            .drawBehind {
                drawCircle(Brush.radialGradient(0f to c(Palette.WaxLight), .65f to c(Palette.Wax), center = Offset(size.width * .38f, size.height * .32f), radius = size.width * 1.02f))
            },
        contentAlignment = Alignment.Center,
    ) { S("BT", font, Color(255, 220, 200).copy(alpha = .75f)) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.LettersStep(mb: Double) {
    Box(Modifier.padding(top = 10.dp)) { Envelopes() }
    S("Bro. Joseph’s weekly letters", 32f, c(Palette.Title), Modifier.padding(top = 14.dp), weight = 300, lineHeight = 1.08f, tracking = -.4f)
    T("Every letter from Branham Tabernacle, with the quotes, scriptures and the Sunday service it goes with. New letters arrive each Saturday.", 14f, C.Sub, Modifier.padding(top = 8.dp), lineHeight = 20f)
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp).choice({ Setup.lettersOn = !Setup.lettersOn }, Role.Switch),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        T("Keep the whole archive on this phone", 15f, c(Palette.Title), Modifier.weight(1f))
        Switch(Setup.lettersOn)
    }
    val open = animate(if (Setup.lettersOn) 1f else 0f, 500, Ease.Sheet)
    val op = animate(if (Setup.lettersOn) 1f else 0f, 300)
    Box(Modifier.fillMaxWidth().heightIn(max = (220 * open).dp).graphicsLayer { alpha = op; clip = true }) {
        Column {
            T("In these languages", 13f, C.Sub, Modifier.padding(top = 16.dp, bottom = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Setup.letterLanguages.forEach { (code, name) -> Chip(name, code in Setup.letterLangs) { Setup.toggleLetterLang(code) } }
            }
        }
    }
    Box(Modifier.weight(1f))
    val nT = Setup.tables.size; val nL = Setup.letterLangs.size
    Row(Modifier.fillMaxWidth().padding(start = 2.dp, end = 2.dp, top = 16.dp, bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        T("$nT Table ${if (nT == 1) "language" else "languages"}${if (Setup.lettersOn && nL > 0) ", letters in $nL" else ""}", 13f, C.Sub)
        S("${Math.round(mb)} MB", 17f, c(Palette.Title))
    }
    GoldButton("Download", { Setup.start() }, Modifier.fillMaxWidth())
}

/** A choice chip: gold when on (letter languages, filters). */
@Composable
internal fun Chip(label: String, on: Boolean, height: Float = 36f, padX: Float = 14f, size: Float = 13f, onBg: Color = C.Gold, offBg: Color = white(.06f), onFg: Color = C.OnGold, offFg: Color = C.Soft, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) onBg else offBg, org.theguide.ui.tw(300), label = "chip")
    val fg by animateColorAsState(if (on) onFg else offFg, org.theguide.ui.tw(300), label = "chipFg")
    Box(
        Modifier.height(height.dp).clip(RoundedCornerShape((height / 2).dp)).background(bg).choice(onClick, Role.Checkbox).padding(horizontal = padX.dp),
        contentAlignment = Alignment.Center,
    ) { T(label, size, fg, weight = 600) }
}

@Composable
private fun ColumnScope.DownloadStep(lit: Float, items: List<SetupItem>, done: Int, overall: Float, mb: Double) {
    GrowingLamp(lit, Modifier.padding(top = 26.dp).align(Alignment.CenterHorizontally))
    S(if (overall >= 1f) "All done" else "Lighting the lamp", 30f, c(Palette.Title), Modifier.fillMaxWidth().padding(top = 18.dp), weight = 300, align = TextAlign.Center)
    T("$done of ${items.size} ready, ${Math.round(overall * mb)} of ${Math.round(mb)} MB", 13.5f, C.Sub, Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 18.dp), align = TextAlign.Center)
    items.forEach { DownloadRow(it, Setup.progress[it.id] ?: 0) }
    Box(Modifier.weight(1f))
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp).choice({ org.theguide.data.Prefs.updateWifiOnly(!org.theguide.data.Prefs.wifiOnly) }, Role.Switch),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        T("Only on Wi-Fi", 13.5f, C.Soft, Modifier.weight(1f))
        Switch(org.theguide.data.Prefs.wifiOnly, 40f, 24f, 18f)
    }
    Box(
        Modifier.padding(top = 12.dp).fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp)).background(white(.07f)).press({ Setup.go(Step.Done) }),
        contentAlignment = Alignment.Center,
    ) { T("Start reading while it finishes", 15f, c(Palette.Title), weight = 600) }
}

@Composable
internal fun DownloadRow(it: SetupItem, p: Int, scale: Float = 1f, nameSize: Float = 16.5f, subSize: Float = 12f, stSize: Float = 12.5f, gap: Float = 14f, padY: Float = 10f) {
    val fill = animate(p / 100f, 300, Ease.Linear)
    val letter = it.letter
    Row(
        Modifier.fillMaxWidth().drawBehind { drawLine(white(.06f), Offset(0f, size.height - .5f.dp.toPx()), Offset(size.width, size.height - .5f.dp.toPx()), 1.dp.toPx()) }
            .padding(vertical = padY.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(gap.dp),
    ) {
        val w = if (letter) 30f else 14f; val h = if (letter) 22f else 40f
        Box(
            Modifier.size(w.dp, h.dp).clip(if (letter) RoundedCornerShape(3.dp) else corners(3f, 3f, 2f, 2f)).background(white(.08f))
                .drawBehind {
                    val fh = size.height * fill
                    drawRect(
                        Brush.verticalGradient(if (letter) listOf(c(0xFFE8DCC0), c(0xFFC9B48D)) else listOf(C.Flame, C.Amber), size.height - fh, size.height),
                        topLeft = Offset(0f, size.height - fh), size = androidx.compose.ui.geometry.Size(size.width, fh),
                    )
                    drawRect(white(.12f), size = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height))
                }
        )
        Column(Modifier.weight(1f)) {
            S(it.name, nameSize, c(Palette.Title))
            T(it.sub, subSize, C.Meta)
        }
        T(if (p >= 100) "Ready" else if (p > 0) "$p%" else "Waiting", stSize, if (p >= 100) C.Gold else C.Meta, align = TextAlign.End, tabular = true)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.DoneStep(lit: Float, onOpen: () -> Unit) {
    val nT = Setup.tables.size
    val letters = Setup.lettersOn && Setup.letterLangs.isNotEmpty()
    Box(Modifier.weight(1f))
    GrowingLamp(lit, Modifier.align(Alignment.CenterHorizontally))
    S("The lamp is lit", 40f, c(Palette.Title), Modifier.fillMaxWidth().padding(top = 26.dp), weight = 300, tracking = -.6f, align = TextAlign.Center)
    T("The Table in $nT ${if (nT == 1) "language" else "languages"}${if (letters) " and every letter from Bro. Joseph" else ""} are on this phone, ready without internet.",
        14.5f, C.Sub, Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 10.dp), lineHeight = 22f, align = TextAlign.Center)
    val chips = Setup.offers.filter { it.iso3 in Setup.tables }.map { it.localName } + if (letters) listOf("Letters") else emptyList()
    FlowRow(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        chips.forEachIndexed { i, name ->
            val t by oneShot(Setup.flip, 500, 200 + i * 100)
            Box(Modifier.pop({ t }, Ease.O13).height(30.dp).clip(RoundedCornerShape(15.dp)).background(gold(.12f)).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                T(name, 12.5f, C.Gold, weight = 600)
            }
        }
    }
    Box(Modifier.weight(1.2f))
    GoldButton("Open The Guide", { Setup.finish(); onOpen() }, Modifier.fillMaxWidth())
    T("Add languages any time in Settings.", 12.5f, C.Faint, Modifier.fillMaxWidth().padding(top = 12.dp), align = TextAlign.Center)
}

@Composable
internal fun OfflineCloud(w: Float, h: Float) {
    Box(Modifier.size(w.dp, h.dp)) {
        Ico(Icons.Cloud, w, C.Faint, height = h)
        Ico(Icons.CloudSlash, w, C.Coral, height = h)
    }
}

@Composable
private fun ColumnScope.OfflineStep(done: Int, total: Int) {
    Box(Modifier.weight(1f))
    Box(Modifier.align(Alignment.CenterHorizontally)) { OfflineCloud(96f, 70f) }
    S("Waiting for internet", 34f, c(Palette.Title), Modifier.fillMaxWidth().padding(top = 10.dp), weight = 300, align = TextAlign.Center)
    T("$done of $total are already on this phone. The rest will carry on by itself when you are back online. Nothing is lost.", 14.5f, C.Sub,
        Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 10.dp), lineHeight = 22f, align = TextAlign.Center)
    Box(Modifier.weight(1.2f))
    GoldButton("Try again now", { Setup.retry() }, Modifier.fillMaxWidth())
}
