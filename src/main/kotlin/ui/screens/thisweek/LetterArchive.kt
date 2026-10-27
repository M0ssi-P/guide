package ui.screens.thisweek

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.graphicsLayer
import components.global.SoftEase
import components.global.rise
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.searchIcon
import kotlinx.coroutines.launch
import navigation.LocalTabs
import navigation.Screen
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import parsers.bt.LetterHit
import parsers.bt.LetterQuery
import parsers.bt.Letters
import parsers.bt.LookIn
import ui.theme.Fraunces
import ui.theme.Inter
import ui.theme.LocalTheme
import java.time.LocalDate

private enum class DateRange(val label: String) { ANY("Any time"), THIS_YEAR("This year"), FIVE_YEARS("Last 5 years"), CUSTOM("") }

private val dmy = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
private fun parseDmy(s: String): LocalDate? = runCatching { LocalDate.parse(s.trim(), dmy) }.getOrNull()

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun LetterArchive() {
    val theme = LocalTheme.current
    val tab = LocalTabs.current.current
    val scope = rememberCoroutineScope()
    val letters by Letters.all.collectAsState()
    val sync by Letters.sync.collectAsState()
    LaunchedEffect(Unit) { Letters.sync() }

    var text by remember { mutableStateOf("") }
    var wholeWord by remember { mutableStateOf(false) }
    var range by remember { mutableStateOf(DateRange.ANY) }
    var fromText by remember { mutableStateOf("") }
    var toText by remember { mutableStateOf("") }
    var code by remember { mutableStateOf<String?>(null) }
    var kind by remember { mutableStateOf<String?>(null) }
    var look by remember { mutableStateOf(LookIn.ALL) }
    var newestFirst by remember { mutableStateOf(true) }

    val today = remember { LocalDate.now() }
    val (from, to) = when (range) {
        DateRange.ANY -> null to null
        DateRange.THIS_YEAR -> LocalDate.of(today.year, 1, 1) to null
        DateRange.FIVE_YEARS -> today.minusYears(5) to null
        DateRange.CUSTOM -> parseDmy(fromText) to parseDmy(toText)
    }
    val query = LetterQuery(text, wholeWord, from, to, code, kind, look, newestFirst)
    val hits = remember(letters, query) { Letters.search(letters, query) }
    val totalMatches = hits.sumOf { it.matches }

    val sermons = remember(letters) { letters.distinctBy { it.code }.map { it.code to it.title } }
    val kinds = remember(letters) { (letters.mapNotNull { it.kind } + listOf("HOME", "YFYC")).distinct().sorted() }

    val clearAll = { text = ""; range = DateRange.ANY; fromText = ""; toText = ""; code = null; kind = null; look = LookIn.ALL }
    val active = buildList {
        if (text.isNotBlank()) add("“${text.trim()}”" to { text = "" })
        if (range != DateRange.ANY && (range != DateRange.CUSTOM || from != null || to != null))
            add((if (range == DateRange.CUSTOM) "${fromText.ifBlank { "…" }} – ${toText.ifBlank { "…" }}" else range.label) to { range = DateRange.ANY; fromText = ""; toText = "" })
        code?.let { c -> add(c to { code = null }) }
        kind?.let { k -> add(k to { kind = null }) }
        if (look != LookIn.ALL) add("In ${look.label.lowercase()}" to { look = LookIn.ALL })
    }

    val list = rememberLazyListState()
    val searchFocus = remember { FocusRequester() }
    Box(Modifier.fillMaxSize().onPreviewKeyEvent { e ->
        // Ctrl+F jumps to the search field, as the hint says
        if (e.type == KeyEventType.KeyDown && e.isCtrlPressed && e.key == Key.F) { searchFocus.requestFocus(); true } else false
    }) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 56.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(Modifier.widthIn(max = 1040.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.rise()) {
                            ActionButton("← This Week") { tab.navigate(Screen.ThisWeek) }
                            Text(
                                "Every letter, preliminary and closing from Brother Joseph Branham",
                                color = theme.colors.text, fontSize = 13.sp, modifier = Modifier.padding(top = 18.dp)
                            )
                            Text("Letter archive", color = theme.colors.primaryText, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 36.sp)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                when {
                                    sync.running -> "Downloading the archive… page ${sync.page}"
                                    sync.error != null -> sync.error!!
                                    else -> "${letters.size} letters saved on this computer"
                                },
                                color = if (sync.error != null) theme.colors.crimson else theme.colors.text, fontSize = 12.sp
                            )
                            if (!sync.running) ActionButton("Download full archive") { scope.launch { Letters.sync(full = true) } }
                        }
                    }

                    SearchField(text, searchFocus, onChange = { text = it }) {
                        Chip("Whole word", selected = wholeWord) { wholeWord = !wholeWord }
                        Text(
                            "Ctrl F", color = theme.colors.text, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).border(1.dp, theme.colors.border, RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(theme.colors.menu)
                            .border(1.dp, theme.colors.border, RoundedCornerShape(16.dp)).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            FilterRow("Date", Modifier.weight(1.4f)) {
                                DateField(fromText, "From") { fromText = it; range = DateRange.CUSTOM }
                                Text("to", color = theme.colors.text, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                                DateField(toText, "To") { toText = it; range = DateRange.CUSTOM }
                                listOf(DateRange.THIS_YEAR, DateRange.FIVE_YEARS, DateRange.ANY).forEach { r ->
                                    Chip(r.label, selected = range == r) { range = r; fromText = ""; toText = "" }
                                }
                            }
                            FilterRow("Sermon", Modifier.weight(1f)) { SermonPicker(sermons, code) { code = it } }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            FilterRow("Service", Modifier.weight(1.4f)) {
                                Chip("All", selected = kind == null) { kind = null }
                                kinds.forEach { k -> Chip(k, selected = kind == k) { kind = k } }
                            }
                            FilterRow("Look in", Modifier.weight(1f)) {
                                LookIn.entries.forEach { l -> Chip(l.label, selected = look == l) { look = l } }
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            Text(
                                plural(hits.size, "letter") + if (totalMatches > 0) " · " + plural(totalMatches, "match") else "",
                                color = theme.colors.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                                modifier = Modifier.padding(top = 8.dp, end = 6.dp)
                            )
                            active.forEach { (label, clear) -> Chip("$label  ×", selected = false, onClick = clear) }
                            if (active.isNotEmpty()) Chip("Clear all", selected = false, onClick = clearAll)
                        }
                        Chip(if (newestFirst) "Newest first ↕" else "Oldest first ↕", selected = false) { newestFirst = !newestFirst }
                    }
                }
            }

            if (hits.isEmpty()) item {
                Column(
                    Modifier.widthIn(max = 1040.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .border(1.dp, theme.colors.border, RoundedCornerShape(20.dp)).padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (letters.isEmpty() && sync.running) "Downloading letters…" else "No letters match",
                        fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontSize = 22.sp, color = theme.colors.primaryBright
                    )
                    if (letters.isNotEmpty()) {
                        Text("Try another word, or widen the dates.", color = theme.colors.text, fontSize = 13.sp)
                        ActionButton("Clear filters", primary = true, onClick = clearAll)
                    }
                }
            }

            hits.groupBy { it.letter.letterDate.year }.forEach { (y, group) ->
                stickyHeader(key = "y$y") {
                    Row(
                        Modifier.widthIn(max = 1040.dp).fillMaxWidth().background(theme.colors.surface).padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(y.toString(), fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = theme.colors.primaryText)
                        Box(Modifier.weight(1f).height(1.dp).background(theme.colors.border))
                        Text(plural(group.size, "letter"), color = theme.colors.text, fontSize = 12.sp)
                    }
                }
                items(group, key = { it.letter.id }) { hit -> ResultRow(hit) { tab.navigate(Screen.Letter(hit.letter.id)) } }
            }
        }

        VerticalScrollbar(scrollState = list, modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

private fun plural(n: Int, word: String) = "$n $word" + if (n == 1) "" else if (word.endsWith("ch")) "es" else "s"

@Composable
private fun SearchField(value: String, focusRequester: FocusRequester, onChange: (String) -> Unit, trailing: @Composable () -> Unit) {
    val theme = LocalTheme.current
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().height(56.dp).clip(shape).background(theme.colors.menu)
            .border(1.dp, if (focused) theme.colors.primaryText.copy(alpha = 0.25f) else theme.colors.border, shape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(searchIcon(theme.colors.primary), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(18.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("Search a word or phrase, like family or adoption", color = theme.colors.text, fontSize = 15.sp)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = theme.colors.primaryText, fontSize = 15.sp, fontFamily = Inter),
                cursorBrush = SolidColor(theme.colors.primary),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).onFocusChanged { focused = it.isFocused }
            )
        }
        trailing()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterRow(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(label)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}

/** A dd/mm/yyyy box; the filter applies once the text parses as a date. */
@Composable
private fun DateField(value: String, label: String, onChange: (String) -> Unit) {
    val theme = LocalTheme.current
    val bad = value.isNotBlank() && parseDmy(value) == null
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier.width(118.dp).height(36.dp).clip(shape).background(theme.colors.surface)
            .border(1.dp, if (bad) theme.colors.crimson else theme.colors.border, shape).padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) Text("dd/mm/yyyy", color = theme.colors.text, fontSize = 12.5.sp, fontFamily = FontFamily.Monospace)
        BasicTextField(
            value, { onChange(it.filter { c -> c.isDigit() || c == '/' }.take(10)) }, singleLine = true,
            textStyle = TextStyle(color = theme.colors.primaryText, fontSize = 12.5.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(theme.colors.primary),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label }
        )
    }
}

@Composable
private fun SermonPicker(sermons: List<Pair<String, String>>, selected: String?, onPick: (String?) -> Unit) {
    val theme = LocalTheme.current
    var open by remember { mutableStateOf(false) }
    Box {
        Chip(selected?.let { c -> "$c  ${sermons.firstOrNull { it.first == c }?.second.orEmpty()}" } ?: "All sermons  ▾", selected = selected != null) { open = true }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.background(theme.colors.popupLayer).heightIn(max = 420.dp)
        ) {
            DropdownMenuItem(onClick = { onPick(null); open = false }) { Text("All sermons", color = theme.colors.primaryText, fontSize = 13.sp) }
            sermons.forEach { (c, title) ->
                DropdownMenuItem(onClick = { onPick(c); open = false }) {
                    Text(c, color = theme.colors.primaryBright, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.width(86.dp))
                    Text(title, color = theme.colors.primaryText, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ResultRow(hit: LetterHit, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val l = hit.letter
    var hovered by remember { mutableStateOf(false) }
    val lift by animateDpAsState(if (hovered) (-3).dp else 0.dp, tween(300, easing = SoftEase))
    val go by animateFloatAsState(if (hovered) 1f else 0f, tween(300))
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.widthIn(max = 1040.dp).fillMaxWidth().rise().offset(y = lift).clip(shape)
            .background(if (hovered) theme.colors.menuHoverColor else theme.colors.menu)
            .border(1.dp, if (hovered) theme.colors.primary.copy(alpha = 0.35f) else theme.colors.border, shape)
            .onHover { hovered = it }
            .clickable(onClick = onClick).hand()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(Modifier.width(60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Letters.monthName(l.letterDate).take(3).uppercase(), color = theme.colors.text, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(l.letterDate.dayOfMonth.toString(), fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = theme.colors.primaryText)
            l.kind?.let {
                Text(
                    it, color = theme.colors.primaryBright, fontSize = 9.5.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(theme.colors.primary.copy(alpha = 0.14f)).padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(l.code, color = theme.colors.primaryBright, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Text(l.title, color = theme.colors.primaryText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                if (hit.matches > 0) Text(
                    plural(hit.matches, "match"), color = theme.colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(theme.colors.primary.copy(alpha = 0.16f)).padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            l.salutation?.let { Text(it, fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontSize = 15.sp, color = theme.colors.primaryText.copy(alpha = 0.8f)) }
            Text(
                highlighted(hit.snippet, hit.ranges, theme.colors.primary.copy(alpha = 0.32f), theme.colors.primaryText),
                fontFamily = Fraunces, fontSize = 15.sp, lineHeight = 25.sp, color = theme.colors.text
            )
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                l.sections.forEach { s ->
                    Text(
                        s.part.label, color = theme.colors.text, fontSize = 11.sp,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).border(1.dp, theme.colors.border, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
        Text("→", color = theme.colors.primary, fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterVertically).graphicsLayer { translationX = 4.dp.toPx() * go; alpha = 0.5f + 0.5f * go })
    }
}
