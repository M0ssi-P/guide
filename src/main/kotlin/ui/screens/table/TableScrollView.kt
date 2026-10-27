package ui.screens.table

import activity.ActivityLog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.global.NumberedParagraph
import navigation.LocalModal
import navigation.dismiss
import navigation.show
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import parsers.vgr.models.Sermon
import ui.screens.reading.ReadingPrefs
import ui.screens.reading.ReadingSettingsModal
import ui.screens.reading.ReadingStores
import ui.screens.reading.TableOptions
import ui.screens.thisweek.hand
import ui.theme.Fraunces
import ui.theme.LocalTheme

/** The Table as one long scrolling sermon, using the reading settings; the book view stays the default. */
@Composable
fun TableScrollView(sermon: Sermon, prefs: ReadingPrefs, modifier: Modifier = Modifier) {
    val t = prefs.theme
    val blocks = remember(sermon.stringId) { sectionsToBlocks(sermon.sections ?: emptyList()) }
    val numbered = remember(blocks) { blocks.filterIsInstance<ContentBlock.ParagraphBlock>().filter { it.number != null && !it.isQuestion } }
    val list = rememberLazyListState()
    // paragraphs scrolled past count as read; reaching the end finishes the sermon
    LaunchedEffect(sermon.stringId) {
        var furthest = 0
        snapshotFlow { list.firstVisibleItemIndex + list.layoutInfo.visibleItemsInfo.size }.collect { seen ->
            if (seen > furthest) { ActivityLog.paragraphsRead(seen - furthest); furthest = seen }
            if (seen >= blocks.size) ActivityLog.finishedSermon(sermon.stringId, sermon.title, sermon.dateCode)
        }
    }
    Box(modifier.fillMaxSize().background(t.bgC)) {
        LazyColumn(
            state = list, modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 36.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(if (prefs.perLine) 28.dp else 16.dp)
        ) {
            item {
                Column(Modifier.widthIn(max = prefs.width.dp.dp).fillMaxWidth().padding(horizontal = 24.dp)) {
                    Text(sermon.dateCode + " · " + sermon.location, color = t.mutedC, fontSize = 12.5.sp, letterSpacing = 1.sp)
                    Text(sermon.title, color = t.headC, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 40.sp, modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))
                }
            }
            items(blocks) { b ->
                Box(Modifier.widthIn(max = prefs.width.dp.dp).fillMaxWidth().padding(horizontal = 24.dp)) {
                    when (b) {
                        is ContentBlock.Heading -> Text(b.text, color = t.accentC, fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontSize = 19.sp, modifier = Modifier.padding(top = 12.dp))
                        is ContentBlock.ParagraphBlock -> {
                            val color = if (b.isQuestion) t.fgC.copy(alpha = 0.72f) else t.fgC
                            val size = prefs.size.sp
                            val lh = (prefs.size * prefs.spacing.lh).sp
                            if (b.number != null && !b.isQuestion) SavableParagraph(b, sermon, numbered) {
                                if (prefs.numbers) NumberedParagraph(b.number, b.text, color, t.accentC, size, lh, Modifier.fillMaxWidth(), prefs.family)
                                else Text(b.text, color = color, fontSize = size, lineHeight = lh, fontFamily = prefs.family, fontWeight = prefs.weight)
                            } else Text(
                                b.text, color = color, fontSize = size, lineHeight = lh, fontFamily = prefs.family, fontWeight = prefs.weight,
                                fontStyle = if (b.isQuestion) FontStyle.Italic else FontStyle.Normal
                            )
                        }
                    }
                }
            }
        }
        VerticalScrollbar(scrollState = list, modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

/** The "Aa" button that opens the shared reading settings with The Table's wording. */
@Composable
fun TableSettingsButton(modifier: Modifier = Modifier) {
    val theme = LocalTheme.current
    val modal = LocalModal.current
    Row(
        modifier.height(36.dp).clip(RoundedCornerShape(10.dp)).background(theme.colors.menu).border(1.dp, theme.colors.border, RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClickLabel = "Reading settings") {
                modal.show { ReadingSettingsModal(ReadingStores.table, TableOptions, onDismiss = { modal.dismiss() }) { TablePreview(it) } }
            }.hand().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("A", color = theme.colors.primaryText, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("a", color = theme.colors.primaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TablePreview(p: ReadingPrefs) {
    Text("Communion", color = p.theme.headC, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 24.sp)
    val text = androidx.compose.ui.text.AnnotatedString("What is the body of Christ? It’s the body of believers that’s associated with the Holy Spirit. Not an idol, not a piece of bread, but a Spirit that’s in the believer.")
    if (p.numbers) NumberedParagraph(14, text, p.theme.fgC, p.theme.accentC, p.size.sp, (p.size * p.spacing.lh).sp, Modifier.fillMaxWidth(), p.family)
    else Text(text, color = p.theme.fgC, fontSize = p.size.sp, lineHeight = (p.size * p.spacing.lh).sp, fontFamily = p.family)
}

