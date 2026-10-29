package ui.screens.bible

import activity.SavedSource
import activity.SavedStore
import activity.SavedTarget
import activity.Tone
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.projectorIcon
import components.global.BibleTextStyle
import components.global.BookChapterSelector
import components.global.SoftEase
import components.global.popIn
import components.global.rise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import loadData
import saveData
import mvvm.BibleViewModel
import mvvm.ShareViewModels
import navigation.LocalModal
import navigation.LocalTabs
import navigation.Screen
import navigation.dismiss
import navigation.show
import org.jetbrains.jewel.foundation.modifier.onHover
import parsers.bible.Bible
import parsers.bible.models.ChapterSection
import parsers.bible.models.IBibleVersion
import parsers.bible.models.IChapterContent
import parsers.bible.models.VerseLineEnum
import perVerseMode
import presentation.DeckItem
import presentation.DeckSource
import presentation.ProjectionDeck
import presentation.sentences
import ui.screens.reading.ReadStyle
import ui.screens.reading.ReadingPrefs
import ui.screens.reading.ReadingSettingsModal
import ui.screens.reading.ReadingStores
import ui.screens.reading.BibleOptions
import ui.screens.saved.NoteDialog
import ui.screens.thisweek.hand
import ui.theme.Fraunces
import ui.theme.Inter
import ui.theme.LocalTheme

private val Ink = Color(0xFF2A1A0E)

/** The reading-settings look handed to the scroll view's ChapterContent. */
internal fun bibleStyle(p: ReadingPrefs) = BibleTextStyle(
    family = p.family, weight = p.weight, size = p.size.toFloat(), lineHeight = p.spacing.lh,
    fg = p.theme.fgC, number = p.theme.accentC, jesus = if (p.red) p.theme.jesusC else p.theme.fgC, head = p.theme.headC,
    numbers = p.numbers, headings = p.headings, perLine = p.perLine,
)

internal fun ChapterSection.IBibleVerse.plain() =
    content.filter { it.type != VerseLineEnum.Reference }.joinToString(" ") { it.content ?: "" }.replace(Regex("\\s+"), " ").trim()

private fun ChapterSection.IBibleVerse.rich(fg: Color, jesus: Color): AnnotatedString = buildAnnotatedString {
    content.filter { it.type != VerseLineEnum.Reference }.forEach { seg ->
        withStyle(SpanStyle(color = if (seg.isJesus) jesus else fg)) { append((seg.content ?: "") + " ") }
    }
}

internal fun BibleViewModel.bookName() = currentBook?.human ?: currentChapter?.usfm?.substringBefore(".") ?: ""

/** Previous / next chapter, running on into the neighbouring book. */
internal fun BibleViewModel.stepChapter(delta: Int) {
    val ch = currentChapter ?: return
    val i = chapters.indexOfFirst { it.usfm == ch.usfm }
    val target = chapters.getOrNull(i + delta)
    if (target != null) { onChapterClicked(target); return }
    val books = currentVersion?.books ?: return
    val b = books.indexOfFirst { it.usfm == currentBook?.usfm }
    val nextBook = books.getOrNull(b + delta) ?: return
    putCurrentBook(nextBook)
    onChapterClicked(if (delta > 0) nextBook.chapters.firstOrNull() else nextBook.chapters.lastOrNull())
}

@Composable
internal fun BibleReaderToolbar(model: BibleViewModel, parallel: Boolean, onParallel: () -> Unit, onProject: () -> Unit) {
    val theme = LocalTheme.current
    val modal = LocalModal.current
    val p by ReadingStores.bible.prefs
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ToolButton("‹", "Previous chapter") { model.stepChapter(-1) }
            BookChapterSelector(model)
            ToolButton("›", "Next chapter") { model.stepChapter(1) }
            val code = model.currentVersion?.localAbbreviation ?: ""
            components.global.LanguageChip(code, "Translation: ${model.currentVersion?.localTitle ?: code}") {
                modal.show { BibleTranslationsModal(model) { modal.dismiss() } }
            }
        }
        Row(Modifier.clip(RoundedCornerShape(12.dp)).background(theme.colors.menu).border(1.dp, theme.colors.border, RoundedCornerShape(12.dp)).padding(3.dp)) {
            listOf(ReadStyle.SCROLL to "☰", ReadStyle.PAGES to "▯▯", ReadStyle.VERSE to "◻").forEach { (st, glyph) ->
                val on = !parallel && p.style == st
                val bg by animateColorAsState(if (on) theme.colors.primary else Color.Transparent, tween(200))
                Row(
                    Modifier.height(30.dp).clip(RoundedCornerShape(9.dp)).background(bg).clickable(role = Role.RadioButton) {
                        if (parallel) onParallel()
                        ReadingStores.bible.set(p.copy(style = st))
                    }.hand().padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(glyph, color = if (on) Ink else theme.colors.primaryText.copy(alpha = 0.8f), fontSize = 12.sp)
                    Text(st.label, color = if (on) Ink else theme.colors.primaryText.copy(alpha = 0.8f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.height(36.dp).clip(RoundedCornerShape(10.dp)).background(if (parallel) theme.colors.primary.copy(alpha = 0.14f) else theme.colors.menu)
                    .border(1.dp, if (parallel) theme.colors.primary else theme.colors.border, RoundedCornerShape(10.dp))
                    .clickable(role = Role.Switch, onClick = onParallel).hand().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(2) { Box(Modifier.width(6.dp).height(14.dp).clip(RoundedCornerShape(1.5.dp)).border(1.5.dp, if (parallel) theme.colors.primaryBright else theme.colors.primaryText, RoundedCornerShape(1.5.dp))) }
                }
                Text("Parallel", color = if (parallel) theme.colors.primaryBright else theme.colors.primaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(
                Modifier.height(36.dp).clip(RoundedCornerShape(10.dp)).background(theme.colors.menu).border(1.dp, theme.colors.border, RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClickLabel = "Reading settings") {
                        modal.show { ReadingSettingsModal(ReadingStores.bible, BibleOptions, onDismiss = { modal.dismiss() }) { BiblePreview(it) } }
                    }.hand().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text("A", color = theme.colors.primaryText, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                Text("a", color = theme.colors.primaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 9.dp))
            }
            Row(
                Modifier.height(36.dp).clip(RoundedCornerShape(10.dp)).background(theme.colors.primary).clickable(role = Role.Button, onClick = onProject).hand().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(projectorIcon(Ink), null, tint = Color.Unspecified, modifier = Modifier.size(15.dp))
                Text("Project", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BiblePreview(p: ReadingPrefs) {
    val jesus = if (p.red) p.theme.jesusC else p.theme.fgC
    Text("Matthew 11", color = p.theme.headC, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 24.sp)
    Text(
        buildAnnotatedString {
            if (p.numbers) withStyle(SpanStyle(color = p.theme.accentC, fontSize = (p.size * 0.55f).sp, fontWeight = FontWeight.Bold, fontFamily = Inter)) { append("28 ") }
            withStyle(SpanStyle(color = jesus)) { append("Come unto me, all ye that labour and are heavy laden, and I will give you rest.") }
            append(if (p.perLine) "\n" else " ")
            if (p.numbers) withStyle(SpanStyle(color = p.theme.accentC, fontSize = (p.size * 0.55f).sp, fontWeight = FontWeight.Bold, fontFamily = Inter)) { append("29 ") }
            withStyle(SpanStyle(color = jesus)) { append("Take my yoke upon you, and learn of me; for I am meek and lowly in heart.") }
        },
        fontFamily = p.family, fontWeight = p.weight, fontSize = p.size.sp, lineHeight = (p.size * p.spacing.lh).sp, color = p.theme.fgC
    )
}

@Composable
private fun ToolButton(label: String, desc: String, onClick: () -> Unit) {
    val theme = LocalTheme.current
    var hovered by remember { mutableStateOf(false) }
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(if (hovered) theme.colors.menuHoverColor else theme.colors.menu)
            .border(1.dp, theme.colors.border, RoundedCornerShape(10.dp)).onHover { hovered = it }
            .clickable(role = Role.Button, onClickLabel = desc, onClick = onClick).hand(),
        contentAlignment = Alignment.Center
    ) { Text(label, color = theme.colors.primaryText, fontSize = 16.sp) }
}

/** Selects [v] the way the scroll view does: a tap starts a selection, a second tap extends it, tapping it again clears. */
internal fun toggleVerse(sel: MutableState<MutableList<Int>>, v: Int) {
    val cur = sel.value
    sel.value = when {
        cur.isEmpty() -> mutableListOf(v)
        cur.size == 1 && cur.first() == v -> mutableListOf()
        else -> (minOf(cur.first(), v)..maxOf(cur.first(), v)).toMutableList()
    }
}

/** Pages, Verse and Parallel; Scroll stays the original view with its audio word highlighting. */
@Composable
internal fun BibleAltView(model: BibleViewModel, parallel: Boolean, selected: MutableState<MutableList<Int>>) {
    val p by ReadingStores.bible.prefs
    val verses = remember(model.chapterContent) { perVerseMode(model.chapterContent?.chapter) }
    if (verses.isEmpty()) return
    when {
        parallel -> BibleParallel(model, verses, selected)
        p.style == ReadStyle.PAGES -> BiblePages(model, verses, selected)
        else -> BibleVerse(model, verses, selected)
    }
}

@Composable
private fun BiblePages(model: BibleViewModel, verses: List<ChapterSection.IBibleVerse>, selected: MutableState<MutableList<Int>>) {
    val theme = LocalTheme.current
    val p by ReadingStores.bible.prefs
    val t = p.theme
    // shortcut: pages are filled by a character budget for the text size, not measured; switch to TextMeasurer if pages overflow
    val budget = (1300 * (20f / p.size) * (20f / p.size) / (p.spacing.lh / 1.75f)).toInt()
    val pages = remember(verses, budget) {
        val out = mutableListOf<MutableList<ChapterSection.IBibleVerse>>(mutableListOf())
        var used = 0
        verses.forEach { v -> val n = v.plain().length; if (used + n > budget && out.last().isNotEmpty()) { out += mutableListOf<ChapterSection.IBibleVerse>(); used = 0 }; out.last() += v; used += n }
        out
    }
    val spreads = (pages.size + 1) / 2
    var spread by remember(model.currentChapter?.usfm) { mutableStateOf(0) }
    val shift by animateFloatAsState(spread.toFloat(), tween(550, easing = SoftEase))
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    fun go(d: Int) { spread = (spread + d).coerceIn(0, spreads - 1) }
    val nudge = rememberInfiniteTransition()
    val n by nudge.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse))
    Column(
        Modifier.fillMaxSize().focusRequester(focus).focusable().onPreviewKeyEvent { e ->
            if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionRight) { go(1); true }
            else if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionLeft) { go(-1); true } else false
        }.padding(horizontal = 36.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.widthIn(max = 1060.dp).weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            RoundArrow("‹", "Previous pages") { go(-1) }
            BoxWithConstraints(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(18.dp)).pointerInput(spreads) {
                    var dx = 0f
                    detectHorizontalDragGestures(onDragEnd = { if (dx < -80) go(1) else if (dx > 80) go(-1); dx = 0f }) { c, d -> c.consume(); dx += d }
                }
            ) {
                val half = maxWidth / 2
                pages.forEachIndexed { i, page ->
                    val x = half * (i - shift * 2)
                    if (x > -half * 2 && x < maxWidth * 1.5f) Column(
                        Modifier.offset(x = x).width(half).fillMaxHeight().background(t.paperC)
                            .background(Brush.horizontalGradient(if (i % 2 == 0) listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.18f)) else listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent, Color.Transparent)))
                            .padding(start = 34.dp, end = 34.dp, top = 34.dp, bottom = 46.dp)
                    ) {
                        Text(
                            "${model.bookName().uppercase()} ${model.currentChapter?.human ?: ""}" + if (i % 2 == 1) ":${page.first().number}–${page.last().number}" else "",
                            color = t.mutedC, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 14.dp)
                        )
                        Text(
                            buildAnnotatedString {
                                page.forEach { v ->
                                    val on = v.number in selected.value
                                    withStyle(SpanStyle(background = if (on) t.selC else verseTone(model, v.number) ?: Color.Transparent)) {
                                        if (p.numbers) withStyle(SpanStyle(color = t.accentC, fontSize = (p.size * 0.55f).sp, fontWeight = FontWeight.Bold, fontFamily = Inter)) { append("${v.number} ") }
                                        append(v.rich(t.fgC, if (p.red) t.jesusC else t.fgC))
                                    }
                                    append(if (p.perLine) "\n" else "")
                                }
                            },
                            fontFamily = p.family, fontWeight = p.weight, fontSize = p.size.sp, lineHeight = (p.size * p.spacing.lh).sp, color = t.fgC,
                            modifier = Modifier.weight(1f).clickable { toggleVerse(selected, page.first().number) }
                        )
                        Text("${i + 1}", color = t.mutedC, fontSize = 11.5.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            }
            Box(Modifier.offset(x = (5 * n).dp)) { RoundArrow("›", "Next pages") { go(1) } }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(spreads) { i ->
                val w by animateDpAsState(if (i == spread) 26.dp else 8.dp, tween(300))
                Box(Modifier.width(w).height(8.dp).clip(CircleShape).background(if (i == spread) theme.colors.primary else t.mutedC.copy(alpha = 0.5f)).clickable(onClickLabel = "Pages ${i * 2 + 1}–${i * 2 + 2}") { spread = i }.hand())
            }
            Text("Swipe, scroll sideways or use ← →", color = t.mutedC, fontSize = 12.sp, modifier = Modifier.padding(start = 10.dp))
        }
    }
}

private fun verseTone(model: BibleViewModel, n: Int): Color? = SavedStore.highlights.value.firstOrNull {
    it.target.source == SavedSource.BIBLE && it.target.bibleVersionId == model.currentVersion?.id && it.target.chapterUsfm == model.currentChapter?.usfm && it.target.number == n
}?.tone?.color?.copy(alpha = 0.32f)

@Composable
private fun RoundArrow(label: String, desc: String, onClick: () -> Unit) {
    val theme = LocalTheme.current
    var hovered by remember { mutableStateOf(false) }
    val s by animateFloatAsState(if (hovered) 1.06f else 1f, tween(200))
    Box(
        Modifier.size(44.dp).graphicsLayer { scaleX = s; scaleY = s }.clip(CircleShape).background(if (hovered) theme.colors.menuHoverColor else theme.colors.menu)
            .border(1.dp, theme.colors.border, CircleShape).onHover { hovered = it }.clickable(role = Role.Button, onClickLabel = desc, onClick = onClick).hand(),
        contentAlignment = Alignment.Center
    ) { Text(label, color = theme.colors.primaryText, fontSize = 18.sp) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BibleVerse(model: BibleViewModel, verses: List<ChapterSection.IBibleVerse>, selected: MutableState<MutableList<Int>>) {
    val theme = LocalTheme.current
    val p by ReadingStores.bible.prefs
    val t = p.theme
    var at by remember(model.currentChapter?.usfm) { mutableStateOf((selected.value.firstOrNull()?.let { n -> verses.indexOfFirst { it.number == n } } ?: 0).coerceAtLeast(0)) }
    var forward by remember { mutableStateOf(true) }
    fun go(i: Int) { val to = i.coerceIn(0, verses.size - 1); forward = to >= at; at = to; selected.value = mutableListOf(verses[to].number) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(
        Modifier.fillMaxSize().focusRequester(focus).focusable().onPreviewKeyEvent { e ->
            if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionRight) { go(at + 1); true }
            else if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionLeft) { go(at - 1); true } else false
        }.padding(horizontal = 36.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Text("${model.bookName().uppercase()} ${model.currentChapter?.human ?: ""} · ONE VERSE AT A TIME", color = t.mutedC, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(Modifier.widthIn(max = 980.dp).weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            RoundArrow("‹", "Previous verse") { go(at - 1) }
            AnimatedContent(
                at, Modifier.weight(1f).pointerInput(verses.size) {
                    var dx = 0f
                    detectHorizontalDragGestures(onDragEnd = { if (dx < -80) go(at + 1) else if (dx > 80) go(at - 1); dx = 0f }) { c, d -> c.consume(); dx += d }
                },
                transitionSpec = { (slideInHorizontally(tween(550, easing = SoftEase)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(500))) togetherWith fadeOut(tween(250)) }
            ) { i ->
                val v = verses[i]
                Column(Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically)) {
                    Text("${v.number}", color = t.accentC, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 64.sp, lineHeight = 64.sp)
                    Text(v.rich(t.headC, if (p.red) t.jesusC else t.headC), fontFamily = p.family, fontWeight = p.weight, fontSize = (p.size * 1.6f).sp, lineHeight = (p.size * 1.6f * 1.45f).sp)
                    Text("${model.bookName()} ${model.currentChapter?.human}:${v.number} · ${model.currentVersion?.localAbbreviation ?: ""}", color = t.mutedC, fontSize = 13.sp)
                }
            }
            RoundArrow("›", "Next verse") { go(at + 1) }
        }
        FlowRow(Modifier.widthIn(max = 980.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            verses.forEachIndexed { i, v ->
                val on = i == at
                val bg by animateColorAsState(if (on) theme.colors.primary else theme.colors.menu, tween(200))
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(bg).border(1.dp, if (on) theme.colors.primary else theme.colors.border, RoundedCornerShape(10.dp))
                        .clickable(role = Role.Tab, onClickLabel = "Verse ${v.number}") { go(i) }.hand(),
                    contentAlignment = Alignment.Center
                ) { Text("${v.number}", color = if (on) Ink else theme.colors.primaryText.copy(alpha = 0.8f), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

/** Other translations of the chapter on screen, loaded once and cached like the reader's own. */
internal suspend fun chapterIn(version: IBibleVersion, usfm: String): IChapterContent? = withContext(Dispatchers.IO) {
    val key = "${version.id}.$usfm"
    ShareViewModels.bibleChapterContent[key] ?: runCatching {
        loadData<IChapterContent>(key) ?: Bible().getChapterDoc(version, usfm.substringBefore("."), usfm.substringAfter(".").toInt()).also { saveData(key, it) }
    }.getOrNull()?.also { ShareViewModels.bibleChapterContent[key] = it }
}

private object ParallelState {
    val extra = mutableStateOf<List<IBibleVersion>>(emptyList())
    var stacked by mutableStateOf(false)
    var together by mutableStateOf(true)
}

@Composable
private fun BibleParallel(model: BibleViewModel, verses: List<ChapterSection.IBibleVerse>, selected: MutableState<MutableList<Int>>) {
    val theme = LocalTheme.current
    val p by ReadingStores.bible.prefs
    val t = p.theme
    val usfm = model.currentChapter?.usfm ?: return
    val main = model.currentVersion ?: return
    var extra by ParallelState.extra
    val candidates = remember(model.versions) { (model.versions + ShareViewModels.bibleVersions.values.flatten()).distinctBy { it.id }.filter { it.id != main.id } }
    if (extra.isEmpty() && candidates.isNotEmpty()) extra = listOf(candidates.first())
    val loaded = remember { mutableStateMapOf<Int, List<ChapterSection.IBibleVerse>>() }
    extra.forEach { v -> LaunchedEffect(v.id, usfm) { loaded[v.id] = perVerseMode(chapterIn(v, usfm)?.chapter) } }
    val cols = listOf(main to verses) + extra.map { it to (loaded[it.id] ?: emptyList()) }
    val size = (p.size - 3).coerceAtLeast(15)
    var adding by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 30.dp).rise(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${model.bookName()} ${model.currentChapter?.human}", color = t.headC, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 34.sp)
                Text("  side by side", color = t.mutedC, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ui.screens.reading.Segmented(listOf("Side by side", "Stacked"), if (ParallelState.stacked) 1 else 0, theme.colors.primary, Modifier.width(220.dp)) { ParallelState.stacked = it == 1 }
                Text(
                    if (ParallelState.together) "⇅ Scroll together: on" else "⇅ Scroll together: off", color = theme.colors.primaryText, fontSize = 12.5.sp,
                    modifier = Modifier.height(34.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, theme.colors.border, RoundedCornerShape(10.dp))
                        .clickable(role = Role.Switch) { ParallelState.together = !ParallelState.together }.hand().padding(horizontal = 12.dp, vertical = 9.dp)
                )
            }
        }
        Column(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp)).border(1.dp, t.ruleC, RoundedCornerShape(16.dp))) {
            // headers
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(t.paperC)) {
                if (ParallelState.stacked) HeaderCell(cols.joinToString(" + ") { it.first.localAbbreviation }, "Stacked, verse by verse", null, Modifier.weight(1f), t)
                else cols.forEachIndexed { i, (v, _) ->
                    if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(t.ruleC))
                    HeaderCell(v.localAbbreviation, v.localTitle, if (i > 0) ({ extra = extra.filterNot { it.id == v.id } }) else null, Modifier.weight(1f), t)
                }
                if (!ParallelState.stacked && cols.size < 3) {
                    Box(Modifier.width(1.dp).fillMaxHeight().background(t.ruleC))
                    Box(Modifier.weight(1f)) {
                        Text("+ Add a version", color = theme.colors.primaryBright, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth().clickable { adding = true }.hand().padding(horizontal = 16.dp, vertical = 14.dp))
                        DropdownMenu(adding, { adding = false }, Modifier.background(theme.colors.popupLayer).heightIn(max = 360.dp)) {
                            candidates.filter { c -> extra.none { it.id == c.id } }.forEach { c ->
                                DropdownMenuItem(onClick = { extra = extra + c; adding = false }) {
                                    Text("${c.localAbbreviation}  ", color = theme.colors.primaryBright, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(c.localTitle, color = theme.colors.primaryText, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(t.ruleC))
            val numbers = verses.map { it.number }
            val rowCell: @Composable (Int, Int, Modifier) -> Unit = { col, n, m ->
                val (ver, list) = cols[col]
                val v = list.firstOrNull { it.number == n }
                Text(
                    buildAnnotatedString {
                        if (ParallelState.stacked) withStyle(SpanStyle(color = t.mutedC, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = Inter)) { append("${ver.localAbbreviation}  ") }
                        withStyle(SpanStyle(color = t.accentC, fontSize = (size * 0.6f).sp, fontWeight = FontWeight.Bold, fontFamily = Inter)) { append("$n ") }
                        append(v?.rich(t.fgC, if (p.red) t.jesusC else t.fgC) ?: AnnotatedString(if (list.isEmpty()) "…" else ""))
                    },
                    fontFamily = p.family, fontSize = size.sp, lineHeight = (size * p.spacing.lh).sp, color = t.fgC, modifier = m
                )
            }
            if (ParallelState.stacked || ParallelState.together) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(numbers) { n ->
                        val on = n in selected.value
                        var hovered by remember { mutableStateOf(false) }
                        val bg = if (on) t.selC else if (hovered) t.accentC.copy(alpha = 0.07f) else Color.Transparent
                        if (ParallelState.stacked) Column(Modifier.fillMaxWidth().background(bg).onHover { hovered = it }.clickable { toggleVerse(selected, n) }) {
                            if (n != numbers.first()) Box(Modifier.fillMaxWidth().height(1.dp).background(t.ruleC))
                            cols.indices.forEach { c -> rowCell(c, n, Modifier.padding(start = 18.dp, end = 18.dp, top = if (c == 0) 14.dp else 2.dp, bottom = if (c == cols.lastIndex) 14.dp else 4.dp)) }
                        } else Column(Modifier.fillMaxWidth().background(bg).onHover { hovered = it }.clickable { toggleVerse(selected, n) }) {
                            if (n != numbers.first()) Box(Modifier.fillMaxWidth().height(1.dp).background(t.ruleC))
                            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                                cols.indices.forEach { c ->
                                    if (c > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(t.ruleC))
                                    rowCell(c, n, Modifier.weight(1f).padding(horizontal = 18.dp, vertical = 14.dp))
                                }
                                if (cols.size < 3) { Box(Modifier.width(1.dp).fillMaxHeight().background(t.ruleC)); Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            } else Row(Modifier.fillMaxSize()) {
                cols.indices.forEach { c ->
                    if (c > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(t.ruleC))
                    LazyColumn(Modifier.weight(1f).fillMaxHeight()) {
                        items(numbers) { n ->
                            Box(Modifier.fillMaxWidth().background(if (n in selected.value) t.selC else Color.Transparent).clickable { toggleVerse(selected, n) }) {
                                rowCell(c, n, Modifier.padding(horizontal = 18.dp, vertical = 14.dp))
                            }
                        }
                    }
                }
                if (cols.size < 3) { Box(Modifier.width(1.dp).fillMaxHeight().background(t.ruleC)); Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun HeaderCell(code: String, name: String, onRemove: (() -> Unit)?, modifier: Modifier, t: ui.screens.reading.ReadTheme) {
    Row(modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(code, color = t.headC, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(name, color = t.mutedC, fontSize = 11.5.sp, maxLines = 1)
        }
        if (onRemove != null) Text("×", color = t.mutedC, fontSize = 16.sp, modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Remove version", onClick = onRemove).hand().padding(4.dp))
    }
}

/** The floating bar for selected verses: highlight colours, Copy, Compare, Note, Project. */
@Composable
internal fun BibleSelectionBar(model: BibleViewModel, selected: MutableState<MutableList<Int>>, onCompare: () -> Unit, modifier: Modifier) {
    val theme = LocalTheme.current
    val nav = LocalTabs.current.current
    val modal = LocalModal.current
    val clipboard = LocalClipboardManager.current
    val highlights by SavedStore.highlights
    val sel = selected.value
    val version = model.currentVersion
    val chapter = model.currentChapter
    AnimatedVisibility(
        sel.isNotEmpty() && version != null && chapter != null, modifier,
        enter = fadeIn(tween(180)) + slideInVertically(tween(300, easing = SoftEase)) { it / 2 }, exit = fadeOut(tween(150))
    ) {
        if (version == null || chapter == null || sel.isEmpty()) return@AnimatedVisibility
        val verses = remember(model.chapterContent) { perVerseMode(model.chapterContent?.chapter) }
        val first = sel.min(); val last = sel.max()
        val book = model.bookName()
        val ref = "$book ${chapter.human}:$first" + if (last > first) "–$last" else ""
        val text = (first..last).joinToString(" ") { n -> verses.firstOrNull { it.number == n }?.plain() ?: "" }
        val target = SavedTarget(SavedSource.BIBLE, bibleVersionId = version.id, bibleVersion = version.localAbbreviation, chapterUsfm = chapter.usfm, number = first)
        val tone = highlights.firstOrNull { it.target.key == target.key }?.tone
        Row(
            Modifier.popIn().clip(RoundedCornerShape(14.dp)).background(theme.colors.popupLayer).border(1.dp, theme.colors.primary.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(ref, color = theme.colors.primaryBright, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
            Tone.entries.forEach { t ->
                var hovered by remember { mutableStateOf(false) }
                val s by animateFloatAsState(if (hovered) 1.08f else 1f, tween(200))
                Box(
                    Modifier.size(22.dp).graphicsLayer { scaleX = s; scaleY = s }.clip(CircleShape).background(t.color)
                        .border(2.dp, if (tone == t) Color.White else Color.Transparent, CircleShape).onHover { hovered = it }
                        .clickable(role = Role.Button, onClickLabel = "Highlight ${t.label}") { SavedStore.highlight(target, t, text, "$ref · ${version.localAbbreviation}") }.hand()
                )
            }
            Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(22.dp).background(theme.colors.border))
            BarText("Copy") { clipboard.setText(AnnotatedString("$text\n$ref · ${version.localAbbreviation}")) }
            BarText("Compare", onClick = onCompare)
            BarText("Note") { modal.show { NoteDialog(target, "$ref · ${version.localAbbreviation}", onDismiss = { modal.dismiss() }) } }
            Text(
                "Project", color = Ink, fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.height(30.dp).clip(RoundedCornerShape(8.dp)).background(theme.colors.primary).clickable(role = Role.Button) {
                    val deck = verses.flatMap { DeckItem(it.number, "$book ${chapter.human}:${it.number}", it.plain()).sentences() }
                    ProjectionDeck.load(DeckSource.BIBLE, "$book ${chapter.human}", "${version.localAbbreviation} · ${verses.size} verses", deck, deck.indexOfFirst { it.number == first }.coerceAtLeast(0))
                    nav.navigate(Screen.Projection)
                }.hand().padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun BarText(label: String, onClick: () -> Unit) {
    val theme = LocalTheme.current
    var hovered by remember { mutableStateOf(false) }
    Text(
        label, color = theme.colors.primaryText, fontSize = 12.5.sp,
        modifier = Modifier.height(30.dp).clip(RoundedCornerShape(8.dp)).background(if (hovered) theme.colors.menuHoverColor else Color.Transparent)
            .onHover { hovered = it }.clickable(role = Role.Button, onClick = onClick).hand().padding(horizontal = 10.dp, vertical = 7.dp)
    )
}

/** Opens the projection screen on the chapter being read, at the first selected verse. */
internal fun projectChapter(model: BibleViewModel, selected: List<Int>, navigate: () -> Unit) {
    val chapter = model.currentChapter ?: return
    val version = model.currentVersion ?: return
    val verses = perVerseMode(model.chapterContent?.chapter)
    val book = model.bookName()
    val deck = verses.flatMap { DeckItem(it.number, "$book ${chapter.human}:${it.number}", it.plain()).sentences() }
    ProjectionDeck.load(DeckSource.BIBLE, "$book ${chapter.human}", "${version.localAbbreviation} · ${verses.size} verses", deck, deck.indexOfFirst { it.number == (selected.minOrNull() ?: 1) }.coerceAtLeast(0))
    navigate()
}

