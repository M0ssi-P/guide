package ui.screens.table

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.closeIcon
import com.composables.play
import com.composables.searchIcon
import navigation.LocalModal
import navigation.dismiss
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import parsers.vgr.models.Sermon
import ui.modifier.stroke.BorderSide
import ui.modifier.stroke.newBorder
import ui.theme.Inter
import ui.theme.LocalTheme
import kotlin.collections.buildList

/** A single sermon tile in the Browse modal (`.tile` in the CSS). */
data class SermonTile(val code: String, val title: String)

/** One row in the Search modal's result list (`.result-card`). */
data class SearchResult(val reference: String, val snippet: String)

data class ContentHit(
    val code: String,
    val title: String,
    val paragraphNumber: Int,
    val snippet: String
)

enum class SearchMode { CONTENT, SERMONS }

private sealed class ResultItem {
    data class Pinned(val sermon: Sermon) : ResultItem()
    data class SermonRow(val sermon: Sermon) : ResultItem()
    data class ContentRow(val hit: ContentHit) : ResultItem()
}

/** Centered modal shell (`.modal`/`.modal-head`) shared by Browse and Search. */
@Composable
private fun ModalShell(title: String, onClose: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val theme = LocalTheme.current
    val modal = LocalModal.current

    Column(
        modifier = modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth(0.92f)
            .heightIn(max = 640.dp)
            .fillMaxHeight(0.82f)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Escape -> {
                        modal.dismiss(); onClose(); true
                    }
                    else -> false
                }
            }
            .dropShadow(
                shape = RoundedCornerShape(18.dp),
                block = {
                    color = theme.colors.border
                    spread = 1f
                    offset = Offset(0f, 0f)
                }
            )
            .dropShadow(
                shape = RoundedCornerShape(18.dp),
                block = {
                    color = Color.Black.copy(alpha = 0.12f)
                    alpha = 1f
                    spread = -6f
                    radius = 28f
                    offset = Offset(0f, 14f)
                }
            )
            .clip(RoundedCornerShape(18.dp))
            .background(theme.colors.popupLayer)
            .border(1.dp, theme.colors.border, RoundedCornerShape(18.dp))
    ) {
        PanelHeader(title, onClose = onClose)
        Box(Modifier.fillMaxWidth().height(1.dp).background(theme.colors.border))
        content()
        PanelFooter(12)
    }
}

/** The Browse modal (`#browseModal`): a grid of sermon tiles. */
@Composable
fun BrowseModal(tiles: List<SermonTile>, onClose: () -> Unit, onTileClick: (SermonTile) -> Unit, modifier: Modifier = Modifier) {
    ModalShell(title = "Browse", onClose = onClose, modifier = modifier) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            // .weight(1f) (not fillMaxSize()) so this takes exactly the space
            // left over after the fixed-height header above it, instead of
            // asking for the whole modal's height and overflowing it.
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(tiles) { tile ->
                Column(
                    modifier = Modifier
                        .aspectRatio(1.4f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(TableColors.Gold.copy(alpha = 0.18f), TableColors.Ember.copy(alpha = 0.10f))
                            )
                        )
                        .border(1.dp, TableColors.Divider, RoundedCornerShape(14.dp))
                        .clickable { onTileClick(tile) }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(tile.code, style = TextStyle(fontFamily = InterFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TableColors.GoldBright))
                    Text(tile.title, style = TextStyle(fontFamily = FrauncesFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TableColors.Ink))
                }
            }
        }
    }
}

@Composable
internal fun PanelHeader(title: String, onClose: () -> Unit) {
    val theme = LocalTheme.current
    Row(
        Modifier.fillMaxWidth().padding(20.dp, 18.dp, 16.dp, 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.Medium, fontStyle = FontStyle.Italic,fontSize = 19.sp, color = theme.colors.text))
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = closeIcon(theme.colors.text.copy(alpha = 0.6f)),
                contentDescription = "Close",
                tint = Color.Unspecified,
                modifier = Modifier.size(16.dp).clickable { onClose() }
            )
        }
    }
}

@Composable
private fun PanelFooter(resultCount: Int) {
    val theme = LocalTheme.current
    Row(
        Modifier.fillMaxWidth().newBorder(theme.colors.border, 1.dp, sides = setOf(BorderSide.Top) ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                KeyHint("↑↓", "Navigate")
                KeyHint("↵", "Open")
                KeyHint("Esc", "Close")
            }
            Text(
                "$resultCount ${if (resultCount == 1) "result" else "results"}",
                color = theme.colors.text.copy(alpha = 0.4f), fontSize = 10.5.sp
            )
        }
    }
}

@Composable
private fun ModeToggle(mode: SearchMode, onModeChange: (SearchMode) -> Unit) {
    val theme = LocalTheme.current

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp)
            .height(40.dp)
            .background(theme.colors.surface.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .border(1.dp, theme.colors.border, RoundedCornerShape(12.dp))
            .padding(3.dp)
    ) {
        val segmentWidth = maxWidth / 2
        val offset by animateDpAsState(
            targetValue = if (mode == SearchMode.SERMONS) segmentWidth else 0.dp,
            animationSpec = spring(stiffness = 420f, dampingRatio = 0.88f)
        )
        Box(
            Modifier
                .offset(x = offset)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(9.dp))
                .background(theme.colors.primary.copy(alpha = 0.16f))
                .border(1.dp, theme.colors.primary.copy(alpha = 0.4f), RoundedCornerShape(9.dp))
        )
        Row(Modifier.fillMaxSize()) {
            ModeTab("Content", mode == SearchMode.CONTENT, Modifier.weight(1f)) { onModeChange(SearchMode.CONTENT) }
            ModeTab("Sermons", mode == SearchMode.SERMONS, Modifier.weight(1f)) { onModeChange(SearchMode.SERMONS) }
        }
    }
}

@Composable
private fun ModeTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val theme = LocalTheme.current
    Box(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) theme.colors.primary else theme.colors.text.copy(alpha = 0.6f),
            style = theme.typography.body,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    mode: SearchMode,
    focusRequester: FocusRequester
) {
    val theme = LocalTheme.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 14.dp)
            .height(48.dp)
            .background(theme.colors.surface.copy(alpha = 0.6f), RoundedCornerShape(13.dp))
            .border(1.dp, theme.colors.border, RoundedCornerShape(13.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(searchIcon(theme.colors.text), contentDescription = null,
            tint = Color.Unspecified, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    if (mode == SearchMode.CONTENT) "Search what's being said…" else "Search by title, date or place…",
                    color = theme.colors.text.copy(alpha = 0.32f),
                    fontSize = 14.5.sp
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = theme.typography.body.copy(color = theme.colors.text),
                cursorBrush = SolidColor(theme.colors.primary),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
        }
        if (query.isNotEmpty()) {
            Text(
                "clear",
                color = theme.colors.text,
                style = theme.typography.tab,
                modifier = Modifier
                    .border(1.dp, theme.colors.border, RoundedCornerShape(6.dp))
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onQueryChange("") }
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
internal fun KeyHint(key: String, label: String) {
    val theme = LocalTheme.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            key,
            color = theme.colors.text.copy(alpha = 0.6f),
            style = theme.typography.tab,
            modifier = Modifier
                .background(theme.colors.surface.copy(alpha = 0.6f), RoundedCornerShape(5.dp))
                .border(BorderStroke(1.dp, theme.colors.border), RoundedCornerShape(5.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
        Text(label, color = theme.colors.text.copy(alpha = 0.4f), fontSize = 11.sp)
    }
}

/** The Search modal (`#searchModal`): a search field plus result cards. */
@Composable
fun SearchModal(
    mode: SearchMode,
    onModeChange: (SearchMode) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    currentSermon: Sermon?,
    sermonResults: List<Sermon>,
    contentResults: List<ContentHit>,
    onDismiss: () -> Unit,
    onOpenSermon: (Sermon) -> Unit,
    onOpenContentHit: (ContentHit) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    var activeIndex by remember { mutableStateOf(0) }

    val items: List<ResultItem> = remember(mode, currentSermon, sermonResults, contentResults) {
        when (mode) {
            SearchMode.SERMONS -> buildList {
                currentSermon?.let { add(ResultItem.Pinned(it)) }
                sermonResults.forEach { add(ResultItem.SermonRow(it)) }
            }
            SearchMode.CONTENT -> contentResults.map { ResultItem.ContentRow(it) }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(activeIndex, items.size) {
        if (items.isEmpty()) return@LaunchedEffect
        val target = activeIndex.coerceIn(0, items.lastIndex)
        val visible = listState.layoutInfo.visibleItemsInfo
        val alreadyVisible = visible.any { it.index == target }
        if (alreadyVisible) return@LaunchedEffect

        val isBelowViewport = visible.isNotEmpty() && target > visible.last().index
        if (isBelowViewport) {
            val viewportItemCount = visible.size.coerceAtLeast(1)
            listState.animateScrollToItem((target - viewportItemCount + 1).coerceAtLeast(0))
        } else {
            listState.animateScrollToItem(target)
        }
    }

    ModalShell(title = "Search", onClose = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> {
                            if (items.isNotEmpty()) activeIndex = (activeIndex + 1).coerceAtMost(items.lastIndex)
                            true
                        }

                        Key.DirectionUp -> {
                            activeIndex = (activeIndex - 1).coerceAtLeast(0)
                            true
                        }

                        Key.Enter, Key.NumPadEnter -> {
                            items.getOrNull(activeIndex)?.let { item ->
                                when (item) {
                                    is ResultItem.Pinned -> onOpenSermon(item.sermon)
                                    is ResultItem.SermonRow -> onOpenSermon(item.sermon)
                                    is ResultItem.ContentRow -> onOpenContentHit(item.hit)
                                }
                                onDismiss()
                            }
                            true
                        }

                        else -> false
                    }
                }
        ) {
            ModeToggle(mode = mode, onModeChange = onModeChange)
            SearchField(
                query = query,
                onQueryChange = onQueryChange,
                mode = mode,
                focusRequester = focusRequester
            )
            ResultsList(
                items = items,
                query = query,
                activeIndex = activeIndex,
                listState = listState,
                onHover = { activeIndex = it },
                onClickItem = { item ->
                    when (item) {
                        is ResultItem.Pinned -> onOpenSermon(item.sermon)
                        is ResultItem.SermonRow -> onOpenSermon(item.sermon)
                        is ResultItem.ContentRow -> onOpenContentHit(item.hit)
                    }
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ResultsList(
    items: List<ResultItem>,
    query: String,
    activeIndex: Int,
    listState: LazyListState,
    onHover: (Int) -> Unit,
    onClickItem: (ResultItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalTheme.current

    if (items.isEmpty()) {
        Box(modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(searchIcon(theme.colors.text.copy(alpha = 0.25f)), contentDescription = null,
                    tint = Color.Unspecified, modifier = Modifier.size(26.dp))
                Spacer(Modifier.height(10.dp))
                Text(
                    if (query.isBlank()) "Nothing to show yet" else "Nothing found for “$query”",
                    color = theme.colors.text.copy(alpha = 0.55f), fontSize = 13.5.sp
                )
            }
        }
        return
    }

    LazyColumn(modifier.padding(horizontal = 20.dp), state = listState) {
        itemsIndexed(items) { index, item ->
            val isActive = index == activeIndex
            when (item) {
                is ResultItem.Pinned -> {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "CURRENTLY READING",
                        modifier = Modifier.padding(horizontal = 10.dp),
                        color = theme.colors.text.copy(alpha = 0.55f), style = theme.typography.button
                    )
                    Spacer(Modifier.height(10.dp))
                    PinnedSermonCard(item.sermon, isActive) {
                        onHover(index); onClickItem(item)
                    }
                }
                is ResultItem.SermonRow -> {
                    SermonRowItem(item.sermon, isActive) {
                        onHover(index); onClickItem(item)
                    }
                }
                is ResultItem.ContentRow -> ContentRowItem(item.hit, query, isActive) {
                    onHover(index); onClickItem(item)
                }
            }
        }
    }
}

private inline fun LazyListScope.itemsIndexed(
    items: List<ResultItem>,
    crossinline content: @Composable (Int, ResultItem) -> Unit
) {
    items(items.size) { i -> content(i, items[i]) }
}

@Composable
private fun PinnedSermonCard(sermon: Sermon, active: Boolean, onClick: () -> Unit) {
    val theme = LocalTheme.current

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(theme.colors.primary.copy(alpha = if (active) 0.2f else 0.12f))
            .border(1.dp, theme.colors.primary.copy(alpha = 0.35f), RoundedCornerShape(13.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                .background(theme.colors.primary.copy(alpha = 0.16f))
                .border(1.dp, theme.colors.primary.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(play(theme.colors.primary), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(sermon.title, color = LocalTheme.current.colors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("${sermon.dateCode} · ${sermon.location}",
                color = LocalTheme.current.colors.text.copy(alpha = 0.6f), fontSize = 11.5.sp)
        }
        Text(
            "NOW",
            color = theme.colors.primary, fontSize = 10.sp, fontFamily = Inter, fontWeight = FontWeight.Medium,
            modifier = Modifier
                .background(theme.colors.primary.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
                .border(1.dp, theme.colors.primary.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                .padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun SermonRowItem(sermon: Sermon, active: Boolean, onClick: () -> Unit) {
    val theme = LocalTheme.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(if (active) theme.colors.menuHoverColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                .background(theme.colors.surface.copy(alpha = 0.6f))
                .border(1.dp, theme.colors.border, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(play(theme.colors.text), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(sermon.title, color = theme.colors.text, style = theme.typography.body)
                Spacer(Modifier.width(8.dp))
                Text(sermon.dateCode, color = theme.colors.primary, style = theme.typography.tab)
            }
            Text(sermon.location, color = theme.colors.text.copy(alpha = 0.45f), style = theme.typography.tab)
        }
    }
}

@Composable
private fun ContentRowItem(hit: ContentHit, query: String, active: Boolean, onClick: () -> Unit) {
    val theme = LocalTheme.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(if (active) theme.colors.menuHoverColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(Modifier.padding(top = 3.dp).size(6.dp).clip(CircleShape).background(theme.colors.primary.copy(alpha = 0.7f)))
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(hit.code, color = theme.colors.text, fontSize = 13.5.sp)
                Spacer(Modifier.width(8.dp))
                Text("Para. ${hit.paragraphNumber}", color = theme.colors.primary, style = theme.typography.tab)
            }
            Text(
                highlightMatches(hit.snippet, query, theme.colors.primary),
                color = theme.colors.text.copy(alpha = 0.65f),
                style = theme.typography.tab,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(hit.title, color = theme.colors.text.copy(alpha = 0.4f), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

private fun highlightMatches(text: String, query: String, color: Color): AnnotatedString = buildAnnotatedString {
    if (query.isBlank()) {
        append(text)
        return@buildAnnotatedString
    }
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    var i = 0
    while (i < text.length) {
        val found = lowerText.indexOf(lowerQuery, i)
        if (found == -1) {
            append(text.substring(i))
            break
        }
        append(text.substring(i, found))
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.Medium)) {
            append(text.substring(found, found + query.length))
        }
        i = found + query.length
    }
}

/** One entry in the right-hand menu drawer (`.menu-row`). */
data class MenuItem(val label: String, val icon: (c: Color) -> ImageVector)

/** The Menu drawer (`#menuPanel`). */
@Composable
fun MenuDrawer(items: List<MenuItem>, onClose: () -> Unit, onItemClick: (MenuItem) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.width(300.dp).fillMaxSize().background(TableColors.BgElev)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(20.dp)
        ) {
            Text(
                "Menu",
                style = TextStyle(fontFamily = FrauncesFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TableColors.Ink),
                modifier = Modifier.weight(1f)
            )
            Icon(closeIcon(TableColors.InkMid), contentDescription = "Close", tint = Color.Unspecified, modifier = Modifier.size(16.dp).clickable { onClose() })
        }
        items.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(item) }
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Icon(item.icon(TableColors.InkMid), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(18.dp))
                Text(item.label, style = TextStyle(fontFamily = InterFamily, fontSize = 13.5.sp, color = TableColors.Ink))
            }
        }
    }
}
