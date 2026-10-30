package org.theguide.ui.tv

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import design.Palette
import org.theguide.data.Setup
import org.theguide.data.Setup.Step
import org.theguide.data.TableOffer
import org.theguide.ui.*
import org.theguide.ui.phone.DownloadRow
import org.theguide.ui.phone.Envelopes
import org.theguide.ui.phone.launchLit
import org.theguide.ui.phone.launchTotals
import org.theguide.ui.phone.offerMeta
import org.theguide.ui.phone.offerTip

/** The TV's gold pill: 62 tall, 19 px bold, with its warm shadow. */
@Composable
internal fun TvGold(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Float = 62f, size: Float = 19f, padX: Float = 34f, alpha: Float = 1f, weight: Int = 700) {
    val shape = RoundedCornerShape((height / 2).dp)
    Box(
        modifier.height(height.dp).graphicsLayer { this.alpha = alpha }.cssShadow(0f, 18f, 40f, -14f, gold(.7f), shape).tvFocus(shape, onClick).clip(shape).background(C.Gold)
            .padding(horizontal = padX.dp),
        contentAlignment = Alignment.Center,
    ) { T(label, size, C.OnGold, weight = weight) }
}

/** The TV's quiet pill: white .08, 18 px semibold. */
@Composable
internal fun TvGhost(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Float = 62f, size: Float = 18f, padX: Float = 30f, fg: Color = c(Palette.Title), bg: Color = white(.08f)) {
    val shape = RoundedCornerShape((height / 2).dp)
    Box(modifier.height(height.dp).tvFocus(shape, onClick).clip(shape).background(bg).padding(horizontal = padX.dp), contentAlignment = Alignment.Center) { T(label, size, fg, weight = 600) }
}

/** TVLaunch board: the lamp and name on the left, the step on the right. */
@Composable
fun TvLaunch(onOpen: () -> Unit) {
    val step = Setup.step
    LaunchedEffect(Unit) { Setup.loadOffers() }
    val items = Setup.items
    val (done, overall, mb) = launchTotals(items, Setup.progress)
    val lit = launchLit(step, overall)
    val glowA = animate(.08f + .14f * lit, 1000)
    val screen by oneShot(Unit, 600)
    val swapA by oneShot(Setup.flip, if (step in setOf(Step.Welcome, Step.Done, Step.Offline)) 600 else 550)
    val order = listOf(Step.Table, Step.Letters, Step.Download)
    val ix = order.indexOf(step)
    val first = remember(step) { FocusRequester() }
    LaunchedEffect(step) { runCatching { first.requestFocus() } }

    Box(Modifier.fillMaxSize().background(C.Backdrop).ambient(gold(glowA), .2f, .5f).enter({ screen })) {
        Column(Modifier.width(470.dp).fillMaxHeight().padding(horizontal = 50.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            GrowingLamp(lit)
            S("The Guide", 50f, c(Palette.Title), Modifier.padding(top = 26.dp), weight = 300, tracking = -.8f)
            if (ix >= 0) StepBars(3, ix, Modifier.padding(top = 22.dp).width(220.dp), height = 5f, gap = 8f)
        }
        Column(Modifier.padding(start = 470.dp, end = 70.dp, top = 60.dp, bottom = 50.dp).fillMaxSize().enter({ swapA }, Ease.Css)) {
            when (step) {
                Step.Welcome -> {
                    Box(Modifier.weight(1f))
                    S("“Thy word is a lamp unto my feet, and a light unto my path.”", 40f, c(Palette.Title), italic = true, weight = 300, lineHeight = 1.3f)
                    T("Psalm 119:105", 18f, C.Brass, Modifier.padding(top = 12.dp))
                    T("Two short choices, then everything is kept on this TV so it works without internet.", 19f, C.Sub, Modifier.padding(top = 40.dp, bottom = 24.dp), lineHeight = 29f)
                    TvGold("Begin", { Setup.go(Step.Table) }, Modifier.focusRequester(first))
                    Box(Modifier.weight(1f))
                }
                Step.Table -> TableStep(first)
                Step.Letters -> LettersStep(first, mb)
                Step.Download -> {
                    Box(Modifier.weight(1f))
                    S(if (overall >= 1f) "All done" else "Lighting the lamp", 46f, c(Palette.Title), weight = 300)
                    T("$done of ${items.size} ready, ${Math.round(overall * mb)} of ${Math.round(mb)} MB", 18f, C.Sub, Modifier.padding(top = 6.dp, bottom = 24.dp))
                    items.forEach { DownloadRow(it, Setup.progress[it.id] ?: 0, nameSize = 21f, subSize = 14f, stSize = 16f, gap = 18f, padY = 12f) }
                    TvGhost("Start reading while it finishes", { Setup.go(Step.Done) }, Modifier.padding(top = 26.dp).focusRequester(first))
                    Box(Modifier.weight(1f))
                }
                Step.Done -> DoneStep(first, onOpen)
                Step.Offline -> {
                    Box(Modifier.weight(1f))
                    S("Waiting for internet", 52f, c(Palette.Title), weight = 300)
                    T("$done of ${items.size} are already on this TV. The rest carries on by itself when the TV is back online. Nothing is lost.", 19f, C.Sub, Modifier.padding(top = 12.dp), lineHeight = 29f)
                    TvGold("Try again now", { Setup.retry() }, Modifier.padding(top = 34.dp).focusRequester(first))
                    Box(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.TableStep(first: FocusRequester) {
    S("Which languages for the Table?", 46f, c(Palette.Title), weight = 300, tracking = -.6f)
    T("Every sermon’s words. Voices download later as you listen.", 18f, C.Sub, Modifier.padding(top = 8.dp, bottom = 22.dp))
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (Setup.offers.isEmpty()) T(if (Setup.offersError) "Couldn’t reach the Table store. Check the internet." else "Looking up the languages…", 16f, C.Meta)
        Setup.offers.withIndex().chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (i, o) -> TvLangCard(o, i, o.iso3 in Setup.tables, Modifier.weight(1f).then(if (i == 0) Modifier.focusRequester(first) else Modifier)) { Setup.toggleTable(o.iso3) } }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
    val n = Setup.tables.size
    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TvGhost("Back", { Setup.go(Step.Welcome) })
        TvGold(if (n > 0) "Continue with $n ${if (n == 1) "language" else "languages"}" else "Pick at least one", { if (n > 0) Setup.go(Step.Letters) }, alpha = if (n > 0) 1f else .4f)
    }
}

@Composable
private fun TvLangCard(o: TableOffer, i: Int, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) gold(.09f) else white(.03f), tw(300), label = "bg")
    val ring by animateColorAsState(if (on) gold(.45f) else Color.Transparent, tw(300), label = "ring")
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier.tvFocus(shape, onClick, Role.Checkbox).clip(shape).background(bg).insetRing(2.dp, ring, shape).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spine(c(Palette.Spines[i % 10]), 14f, 40f, on, band = false)
        Column(Modifier.weight(1f)) {
            S(o.localName, 21f, c(Palette.Title))
            val tip = offerTip(o, "TV")
            androidx.compose.foundation.text.BasicText(androidx.compose.ui.text.buildAnnotatedString {
                if (tip.isNotEmpty()) pushStyle(style(13.5f, weight = 600, color = C.Gold).toSpanStyle()).also { append(tip); pop() }
                append(offerMeta(o))
            }, style = style(13.5f, color = C.Meta), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        TickBox(on, 28f, 9f, 15f)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.LettersStep(first: FocusRequester, mb: Double) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(34.dp)) {
        Box(Modifier.size(250.dp, 170.dp)) { Envelopes(230f, 124f, 34f, 44f, 12f, animated = false, boxH = 170f, centered = false, left = 10f) }
        Column {
            S("Bro. Joseph’s weekly letters", 44f, c(Palette.Title), weight = 300, tracking = -.6f)
            T("Every letter from Branham Tabernacle, with its quotes, scriptures and the Sunday service it goes with. New letters arrive each Saturday.", 17f, C.Sub, Modifier.padding(top = 8.dp), lineHeight = 26f)
        }
    }
    Row(
        Modifier.padding(top = 24.dp).fillMaxWidth().tvFocus(RoundedCornerShape(18.dp), { Setup.lettersOn = !Setup.lettersOn }, Role.Switch).clip(RoundedCornerShape(18.dp)).background(white(.04f))
            .focusRequester(first).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) { T("Keep the whole archive on this TV", 18f, c(Palette.Title), Modifier.weight(1f)); Switch(Setup.lettersOn, 54f, 32f, 24f, 4f) }
    val open = animate(if (Setup.lettersOn) 1f else 0f, 500)
    val op = animate(if (Setup.lettersOn) 1f else 0f, 300)
    Box(Modifier.heightIn(max = (220 * open).dp).graphicsLayer { alpha = op; clip = true }) {
        Column {
            T("In these languages", 16f, C.Sub, Modifier.padding(top = 18.dp, bottom = 10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Setup.letterLanguages.forEach { (code, name) ->
                    val on = code in Setup.letterLangs
                    Box(Modifier.height(46.dp).tvFocus(RoundedCornerShape(23.dp), { Setup.toggleLetterLang(code) }, Role.Checkbox).clip(RoundedCornerShape(23.dp)).background(if (on) C.Gold else white(.06f))
                        .padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                        T(name, 16f, if (on) C.OnGold else C.Soft, weight = 600)
                    }
                }
            }
        }
    }
    Box(Modifier.weight(1f))
    val nT = Setup.tables.size; val nL = Setup.letterLangs.size
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TvGhost("Back", { Setup.go(Step.Table) })
        TvGold("Download ${Math.round(mb)} MB", { Setup.start() })
        T("$nT Table ${if (nT == 1) "language" else "languages"}${if (Setup.lettersOn && nL > 0) ", letters in $nL" else ""}", 16f, C.Sub, Modifier.padding(start = 10.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.DoneStep(first: FocusRequester, onOpen: () -> Unit) {
    val nT = Setup.tables.size
    val letters = Setup.lettersOn && Setup.letterLangs.isNotEmpty()
    Box(Modifier.weight(1f))
    S("The lamp is lit", 56f, c(Palette.Title), weight = 300, tracking = -.8f)
    T("The Table in $nT ${if (nT == 1) "language" else "languages"}${if (letters) " and every letter from Bro. Joseph" else ""} are on this TV, ready without internet.", 19f, C.Sub, Modifier.padding(top = 12.dp), lineHeight = 29f)
    FlowRow(Modifier.padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (Setup.offers.filter { it.iso3 in Setup.tables }.map { it.localName } + if (letters) listOf("Letters") else emptyList()).forEachIndexed { i, name ->
            val t by oneShot(Setup.flip, 500, 200 + i * 100)
            Box(Modifier.pop({ t }, Ease.O13).height(38.dp).clip(RoundedCornerShape(19.dp)).background(gold(.12f)).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) { T(name, 15f, C.Gold, weight = 600) }
        }
    }
    TvGold("Open The Guide", { Setup.finish(); onOpen() }, Modifier.padding(top = 36.dp).focusRequester(first))
    Box(Modifier.weight(1f))
}

