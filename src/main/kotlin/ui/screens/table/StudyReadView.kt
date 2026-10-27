package ui.screens.table

import ui.screens.reading.ReadingPrefs
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Popup
import components.global.rise
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import activity.ActivityLog
import activity.SavedSource
import activity.SavedStore
import activity.SavedTarget
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.composables.play
import kotlinx.coroutines.delay
import navigation.LocalTabs
import navigation.Screen
import org.jetbrains.jewel.foundation.modifier.onHover
import parsers.vgr.models.Sermon
import presentation.DeckItem
import presentation.DeckSource
import presentation.ProjectionDeck
import ui.screens.projection.deckItems
import ui.screens.saved.SaveToolbar
import components.global.DropCapParagraph
import components.global.NumberedParagraph
import ui.theme.LocalTheme
import parsers.vgr.models.Section
import ui.theme.Fraunces

// TODO move into Theme.colors once you add an accent token
private val AccentGold = Color(0xFFCF9F52)
private val HighlightAmber = Color(0xFFD3A24D)
private val HighlightRose = Color(0xFFE0748C)
private val HighlightTeal = Color(0xFF57C2BB)
private val HighlightViolet = Color(0xFFA48AF0)

private const val READING_COLUMN_WIDTH_DP = 760

data class StudyDocument(
    val title: String,
    val sessionLabel: String,
    val pullQuote: String,
    val sections: List<Section>,   // Sermon.sections — the real content tree
    val outline: List<StudySection> // the right-rail "In This Study" list (unrelated to Section above)
)

data class StudySection(val label: String, val state: SectionState)
enum class SectionState { DONE, CURRENT, UPCOMING }

@Composable
fun StudyReadView(
    document: StudyDocument,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    sermon: Sermon? = null,
    prefs: ReadingPrefs? = null
) {
    val theme = LocalTheme.current
    val scope = rememberCoroutineScope()
    val blocks = remember(document.sections) { sectionsToBlocks(document.sections) }
    val numbered = remember(blocks) { blocks.filterIsInstance<ContentBlock.ParagraphBlock>().filter { it.number != null && !it.isQuestion } }
    // pages already counted towards "paragraphs read" for this sermon, so turning back doesn't count twice
    val counted = remember(sermon?.stringId) { mutableSetOf<Int>() }

    // the reading settings change size, spacing and typeface; pages are re-measured with them
    val bodySize = prefs?.size?.toFloat() ?: 19f
    val bodyLh = prefs?.let { it.size * it.spacing.lh } ?: 34f
    val bodyStyle = TextStyle(fontSize = bodySize.sp, lineHeight = bodyLh.sp, fontFamily = prefs?.family)
    val headingStyle = TextStyle(fontSize = bodySize.sp, lineHeight = bodyLh.sp, fontWeight = FontWeight.Bold, fontFamily = prefs?.family)

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val columnAvailableHeight = maxHeight - 80.dp /* vertical padding */ - 32.dp /* footer */
            val pages = rememberPaginatedBlocks(
                blocks = blocks,
                availableWidth = READING_COLUMN_WIDTH_DP.dp,
                availableHeight = columnAvailableHeight,
                bodyStyle = bodyStyle,
                headingStyle = headingStyle,
                blockSpacing = 20.dp,
                firstPageReserved = 190.dp
            )
            val pager = rememberBookPagerState(pageCount = pages.size, initialPage = initialPage.coerceIn(0, pages.size - 1))

            BookPageTurn(
                state = pager,
                modifier = Modifier.fillMaxSize(),
                shadowColor = theme.colors.popup
            ) { index ->
                StudyPageContent(
                    document = document,
                    page = pages[index],
                    isFirstPage = index == 0,
                    pageNumber = index + 1,
                    pageCount = pages.size,
                    sermon = sermon,
                    numbered = numbered,
                    counted = counted,
                    prefs = prefs,
                    onPrevPage = { pager.previous(scope) },
                    onNextPage = { pager.next(scope) }
                )
            }
        }
    }
}

@Composable
private fun StudyPageContent(
    document: StudyDocument,
    page: PaginatedPage,
    isFirstPage: Boolean,
    pageNumber: Int,
    pageCount: Int,
    sermon: Sermon?,
    numbered: List<ContentBlock.ParagraphBlock>,
    counted: MutableSet<Int>,
    prefs: ReadingPrefs?,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit
) {
    val theme = LocalTheme.current
    val scrollState = remember(pageNumber) { ScrollState(0) }

    // a page left open for a few seconds counts as read; the last page finishes the sermon
    LaunchedEffect(pageNumber, sermon?.stringId) {
        if (sermon == null) return@LaunchedEffect
        delay(5000)
        if (counted.add(pageNumber)) ActivityLog.paragraphsRead(page.blocks.count { it is ContentBlock.ParagraphBlock })
        if (pageNumber == pageCount) ActivityLog.finishedSermon(sermon.stringId, sermon.title, sermon.dateCode)
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
//            .background(theme.colors.surface)
            .padding(horizontal = 48.dp, vertical = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // main reading column
        Column(
            modifier = Modifier.width(READING_COLUMN_WIDTH_DP.dp).fillMaxHeight().verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            if (isFirstPage) {
                MetaRow(document.title, document.sessionLabel)
                PullQuote(document.pullQuote)
            }
            VerseBody(page.blocks, isFirstPage = isFirstPage, sermon = sermon, numbered = numbered, prefs = prefs)
            Spacer(Modifier.weight(1f))
            PageFooter(
                pageNumber = pageNumber,
                pageCount = pageCount,
                onPrevPage = onPrevPage,
                onNextPage = onNextPage
            )
        }

        // right rail — the space that used to just sit empty
        Row(modifier = Modifier.fillMaxHeight()) {
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(theme.colors.border)
            )
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .padding(start = 36.dp),
                verticalArrangement = Arrangement.spacedBy(36.dp)
            ) {
                StudyOutline(document.outline)
                ReadAlongWaveform()
            }
        }
    }
}

@Composable
private fun MetaRow(title: String, sessionLabel: String) {
    val theme = LocalTheme.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(theme.colors.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = play(theme.colors.primary),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(16.dp)
            )
        }
        Column {
            Text(title, color = theme.colors.text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Text(sessionLabel, color = theme.colors.text.copy(alpha = 0.55f), fontSize = 12.sp)
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf(HighlightAmber, HighlightRose, HighlightTeal, HighlightViolet).forEach { c ->
                Box(Modifier.size(10.dp).clip(CircleShape).background(c))
            }
        }
    }
}

@Composable
private fun PullQuote(quote: String) {
    val theme = LocalTheme.current
    Column {
        Box(Modifier.width(40.dp).height(1.dp).background(theme.colors.primary.copy(alpha = 0.55f)))
        Spacer(Modifier.height(16.dp))
        Text(
            text = "“$quote”",
            fontFamily = Fraunces,
            fontStyle = FontStyle.Italic,
            fontSize = 29.sp,
            lineHeight = 42.sp,
            color = Color(0xFFF5F1E8),
            modifier = Modifier.widthIn(max = 640.dp)
        )
        Spacer(Modifier.height(16.dp))
        Box(Modifier.width(40.dp).height(1.dp).background(theme.colors.primary.copy(alpha = 0.55f)))
    }
}

@Composable
private fun VerseBody(blocks: List<ContentBlock>, isFirstPage: Boolean, sermon: Sermon?, numbered: List<ContentBlock.ParagraphBlock>, prefs: ReadingPrefs?) {
    val theme = LocalTheme.current
    val dropCapIndex = if (isFirstPage) {
        blocks.indexOfFirst { it is ContentBlock.ParagraphBlock && it.isFirst }
    } else -1

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        blocks.forEachIndexed { i, block ->
            when (block) {
                is ContentBlock.Heading -> {
//                    Text(
//                        block.text,
//                        color = theme.colors.text,
//                        fontSize = 19.sp,
//                        fontWeight = FontWeight.Bold,
//                        lineHeight = 34.sp
//                    )
                }
                is ContentBlock.ParagraphBlock -> {
                    val base = prefs?.theme?.fgC ?: theme.colors.text
                    val bodyColor = if (block.isQuestion) base.copy(alpha = 0.72f) else base.copy(alpha = 0.86f)
                    val size = prefs?.size?.sp ?: 19.sp
                    val lh = prefs?.let { (it.size * it.spacing.lh).sp } ?: 34.sp

                    if (i == dropCapIndex && block.text.isNotEmpty()) {
                        DropCapParagraph(
                            block.text,
                            bodyColor = bodyColor,
                            bodyFontSize = size,
                            bodyLineHeight = lh,
                            fontFamily = prefs?.family ?: ui.theme.Fraunces,
                        )
                    } else {
                        if (block.number != null && !block.isQuestion) {
                            SavableParagraph(block, sermon, numbered) {
                                if (prefs?.numbers == false) Text(block.text, color = bodyColor, fontSize = size, lineHeight = lh, fontFamily = prefs.family, modifier = Modifier.widthIn(max = 640.dp))
                                else NumberedParagraph(
                                    number = block.number,
                                    text = block.text,
                                    numColor = prefs?.theme?.accentC ?: theme.colors.primary.copy(alpha = 0.7f),
                                    bodyColor = bodyColor,
                                    bodyFontSize = size,
                                    bodyLineHeight = lh,
                                    modifier = Modifier.widthIn(max = 640.dp),
                                    fontFamily = prefs?.family
                                )
                            }
                        } else {
                            Text(
                                block.text,
                                color = bodyColor,
                                fontSize = 19.sp,
                                lineHeight = 34.sp,
                                fontStyle = if (block.isQuestion) FontStyle.Italic else FontStyle.Normal,
                                modifier = Modifier.widthIn(max = 640.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageFooter(
    pageNumber: Int,
    pageCount: Int,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit
) {
    val theme = LocalTheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(pageCount.coerceAtMost(6)) { i ->
                    val active = i == (pageNumber - 1).coerceIn(0, pageCount - 1)
                    Box(
                        Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (active) theme.colors.primary else theme.colors.text.copy(alpha = 0.16f))
                    )
                }
            }
            Text("Page $pageNumber of $pageCount", color = theme.colors.text.copy(alpha = 0.45f), fontSize = 11.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("prev", color = theme.colors.text.copy(alpha = 0.45f), fontSize = 11.sp,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onPrevPage).padding(4.dp))
            Text("next", color = theme.colors.text.copy(alpha = 0.45f), fontSize = 11.sp,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onNextPage).padding(4.dp))
        }
    }
}

@Composable
private fun StudyOutline(sections: List<StudySection>) {
    val theme = LocalTheme.current
    Column {
        Text(
            "IN THIS STUDY",
            color = theme.colors.text.copy(alpha = 0.45f),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            sections.forEach { section ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (section.state) {
                        SectionState.DONE -> Box(Modifier.size(10.dp).clip(CircleShape).background(AccentGold.copy(alpha = 0.4f)))
                        SectionState.CURRENT -> Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(AccentGold)
                        )
                        SectionState.UPCOMING -> Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.Transparent)
                        )
                    }
                    Text(
                        section.label,
                        color = if (section.state == SectionState.CURRENT) theme.colors.text else theme.colors.text.copy(alpha = 0.45f),
                        fontSize = if (section.state == SectionState.CURRENT) 13.sp else 12.5.sp,
                        fontWeight = if (section.state == SectionState.CURRENT) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadAlongWaveform() {
    val theme = LocalTheme.current
    val heights = listOf(14, 26, 18, 32, 20, 12, 24, 10, 28, 16, 22, 12, 30, 14)
    Column {
        Text(
            "READ-ALONG SYNC",
            color = theme.colors.text.copy(alpha = 0.45f),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 14.dp)
        )
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            heights.forEachIndexed { i, h ->
                Box(
                    Modifier
                        .width(3.dp)
                        .height(h.dp)
                        .background(
                            if (i < 5) AccentGold.copy(alpha = if (i < 3) 1f else 0.6f)
                            else theme.colors.text.copy(alpha = 0.12f)
                        )
                )
            }
        }
        Text(
            "Tap a moment to follow along",
            color = theme.colors.text.copy(alpha = 0.45f),
            fontStyle = FontStyle.Italic,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
/** A numbered paragraph that shows its highlight and, at the pointer, the highlight / bookmark / note / project tools. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SavableParagraph(
    block: ContentBlock.ParagraphBlock,
    sermon: Sermon?,
    numbered: List<ContentBlock.ParagraphBlock>,
    content: @Composable () -> Unit
) {
    if (sermon == null || block.number == null) return content()
    val nav = LocalTabs.current.current
    val highlights by SavedStore.highlights
    // the picker opens where the pointer first rests and stays put, so it can be reached;
    // it closes 250ms after the pointer has left both the paragraph and the picker
    var anchor by remember { mutableStateOf<IntOffset?>(null) }
    var overParagraph by remember { mutableStateOf(false) }
    var overPicker by remember { mutableStateOf(false) }
    LaunchedEffect(overParagraph, overPicker) { if (!overParagraph && !overPicker) { delay(250); anchor = null } }
    val density = LocalDensity.current
    val lift = with(density) { 48.dp.roundToPx() }
    val target = remember(sermon.stringId, block.number) { SavedTarget(SavedSource.TABLE, sermon = sermon.copy(sections = null), number = block.number) }
    val tone = highlights.firstOrNull { it.target.key == target.key }?.tone
    val tint by animateColorAsState(tone?.color?.copy(alpha = 0.16f) ?: Color.Transparent, tween(250))
    val ref = "${sermon.title} · ${sermon.dateCode} · ¶ ${block.number}"
    Box(
        Modifier.widthIn(max = 680.dp)
            .onPointerEvent(PointerEventType.Enter) { overParagraph = true }
            .onPointerEvent(PointerEventType.Exit) { overParagraph = false }
            .onPointerEvent(PointerEventType.Move) { e -> if (anchor == null) anchor = e.changes.first().position.round() }
            .onPointerEvent(PointerEventType.Press) { e -> anchor = e.changes.first().position.round() }
    ) {
        Box(Modifier.clip(RoundedCornerShape(10.dp)).background(tint).padding(horizontal = 6.dp, vertical = 2.dp)) { content() }
        anchor?.let { at ->
            Popup(offset = IntOffset((at.x - lift * 2).coerceAtLeast(0), at.y - lift), onDismissRequest = { anchor = null }) {
                Box(Modifier.rise(distance = 6.dp).onPointerEvent(PointerEventType.Enter) { overPicker = true }.onPointerEvent(PointerEventType.Exit) { overPicker = false }) {
                    SaveToolbar(
                        target = target,
                        text = block.text.text,
                        ref = ref,
                        title = sermon.title,
                        meta = "The Table · ${sermon.dateCode} · \u00B6 ${block.number} of ${numbered.size}",
                        progress = (numbered.indexOf(block) + 1f) / numbered.size.coerceAtLeast(1),
                        onProject = {
                            val deck = sermon.deckItems()
                            ProjectionDeck.load(
                                DeckSource.TABLE, sermon.title, "${sermon.dateCode} · ${sermon.location}",
                                deck, deck.indexOfFirst { it.number == block.number }.coerceAtLeast(0), sermon.copy(sections = null)
                            )
                            nav.navigate(Screen.Projection)
                        }
                    )
                }
            }
        }
    }
}
