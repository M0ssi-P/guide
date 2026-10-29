package components.global

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.globeIcon
import org.jetbrains.jewel.foundation.modifier.onHover
import ui.screens.thisweek.hand
import ui.theme.Inter
import ui.theme.LocalTheme
import ui.theme.ZillaSlab

// Shared pieces of the language / translation windows (Table languages, Bible translations).

private val Ink = Color(0xFF2A1A0E)
private val Tick = Color(0xFF7FD9A8)

/** One language or translation in a picker list. [progress] is 0..1 while it downloads, with [stage] saying what step it is on. */
data class PickItem(
    val key: String,
    val code: String,
    val name: String,
    val local: String = "",
    val meta: String = "",
    val progress: Float? = null,
    val stage: String? = null,
)

/** Globe + code chip that opens a picker. The code slides in when it changes. */
@Composable
fun LanguageChip(code: String, label: String, modifier: Modifier = Modifier, height: Dp = 36.dp, onClick: () -> Unit) {
    val theme = LocalTheme.current
    var hovered by remember { mutableStateOf(false) }
    val lift by animateFloatAsState(if (hovered) -1f else 0f, tween(200))
    Row(
        modifier.height(height).graphicsLayer { translationY = lift * density }.clip(RoundedCornerShape(4.dp))
            .background(if (hovered) theme.colors.menuHoverColor else theme.colors.menu)
            .border(1.dp, theme.colors.border, RoundedCornerShape(4.dp)).onHover { hovered = it }
            .semantics { contentDescription = label }.clickable(role = Role.Button, onClick = onClick).hand().padding(horizontal = if (height < 34.dp) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(globeIcon(theme.colors.primaryBright), null, tint = Color.Unspecified, modifier = Modifier.size(if (height < 34.dp) 14.dp else 16.dp))
        AnimatedContent(code, transitionSpec = { (fadeIn(tween(300)) + slideInVertically(tween(300)) { -it / 3 }) togetherWith fadeOut(tween(120)) }) {
            Text(it, color = theme.colors.primaryText, fontSize = if (height < 34.dp) 12.sp else 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }
        Text("▾", color = theme.colors.text, fontSize = 11.sp)
    }
}

/** Fades and un-blurs content in whenever [key] changes: the switch animation after a new language is picked. */
@Composable
fun Modifier.crossfadeOn(key: Any?): Modifier {
    val a = remember { Animatable(1f) }
    var last by remember { mutableStateOf(key) }
    LaunchedEffect(key) {
        if (key == last) return@LaunchedEffect
        last = key
        a.snapTo(0f); a.animateTo(1f, tween(550, easing = SoftEase))
    }
    return graphicsLayer { alpha = a.value }.blur((4 * (1 - a.value)).dp)
}

/** The window itself: title, close, search, then the caller's sections. Shown through LocalModal. */
@Composable
fun PickerModal(
    title: String, subtitle: String, query: String, onQuery: (String) -> Unit, placeholder: String, onDismiss: () -> Unit,
    top: @Composable ColumnScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit,
) {
    val theme = LocalTheme.current
    val shape = RoundedCornerShape(4.dp)
    Column(
        Modifier.widthIn(max = 600.dp).fillMaxWidth(0.9f).heightIn(max = 720.dp).clip(shape).background(theme.colors.popupLayer).border(1.dp, theme.colors.border, shape)
            .clickable(enabled = false) {}.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(title, color = theme.colors.primaryText, fontFamily = ZillaSlab, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Text(subtitle, color = theme.colors.text, fontSize = 12.5.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(theme.colors.menu).border(1.dp, theme.colors.border, CircleShape)
                    .clickable(role = Role.Button, onClickLabel = "Close", onClick = onDismiss).hand(),
                contentAlignment = Alignment.Center
            ) { Text("×", color = theme.colors.primaryText, fontSize = 16.sp) }
        }
        top()
        var focused by remember { mutableStateOf(false) }
        Row(
            Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(4.dp)).background(theme.colors.surface.copy(alpha = 0.6f))
                .border(1.dp, if (focused) theme.colors.primary else theme.colors.border, RoundedCornerShape(4.dp)).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("⌕", color = theme.colors.text, fontSize = 15.sp)
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text(placeholder, color = theme.colors.text, fontSize = 13.5.sp)
                BasicTextField(query, onQuery, singleLine = true, textStyle = TextStyle(color = theme.colors.primaryText, fontSize = 13.5.sp, fontFamily = Inter),
                    cursorBrush = SolidColor(theme.colors.primary), modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused })
            }
        }
        content()
    }
}

@Composable
fun PickerSection(label: String, trailing: (@Composable () -> Unit)? = null) {
    val theme = LocalTheme.current
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = theme.colors.text, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        trailing?.invoke()
    }
}

@Composable
fun PickerNote(text: String) = Text(text, color = LocalTheme.current.colors.text, fontSize = 12.5.sp, modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp))

/** The pinned "reading now" card. */
@Composable
fun CurrentPick(item: PickItem, label: String = "READING NOW") {
    val theme = LocalTheme.current
    Row(
        Modifier.fillMaxWidth().crossfadeOn(item.key).clip(RoundedCornerShape(4.dp)).background(theme.colors.primary.copy(alpha = 0.08f))
            .border(1.dp, theme.colors.primary, RoundedCornerShape(4.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(theme.colors.primary), contentAlignment = Alignment.Center) {
            Text(item.code.take(4), color = Ink, fontFamily = ZillaSlab, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(label, color = theme.colors.primaryBright, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Text(item.name, color = theme.colors.primaryText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (item.local.isNotBlank()) Text(item.local, color = theme.colors.text, fontSize = 12.sp)
        }
    }
}

@Composable
private fun RowShell(i: Int, selected: Boolean = false, onClick: (() -> Unit)? = null, role: Role? = null, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    val theme = LocalTheme.current
    var hovered by remember { mutableStateOf(false) }
    val bg by animateColorAsState(
        when { selected -> theme.colors.primary.copy(alpha = 0.08f); hovered -> theme.colors.menuHoverColor; else -> Color.Transparent }, tween(200)
    )
    Row(
        Modifier.fillMaxWidth().rise(i * 30, 8.dp).clip(RoundedCornerShape(4.dp)).background(bg)
            .border(1.dp, if (selected) theme.colors.primary else theme.colors.border, RoundedCornerShape(4.dp)).onHover { hovered = it }
            .then(if (onClick != null) Modifier.clickable(role = role ?: Role.Button, onClick = onClick).hand() else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) { content() }
}

@Composable
private fun Names(item: PickItem, modifier: Modifier) {
    val theme = LocalTheme.current
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.name, color = theme.colors.primaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (item.local.isNotBlank() && item.local != item.name) Text(" · ${item.local}", color = theme.colors.text, fontSize = 14.sp, maxLines = 1)
        }
        if (item.meta.isNotBlank()) Text(item.meta, color = theme.colors.text, fontSize = 11.5.sp, maxLines = 1)
        item.progress?.let { p ->
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val w by animateFloatAsState(p.coerceIn(0f, 1f), tween(300))
                Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(4.dp)).background(theme.colors.border)) {
                    Box(Modifier.fillMaxWidth(w).fillMaxHeight().background(Brush.horizontalGradient(listOf(theme.colors.primary, theme.colors.primaryBright))))
                }
                Text(item.stage ?: "${(p * 100).toInt()}%", color = theme.colors.text, fontSize = 11.sp)
            }
        }
    }
}

/** An installed language as a radio row; the current one is ringed. */
@Composable
fun InstalledPick(item: PickItem, selected: Boolean, index: Int, onPick: () -> Unit) {
    val theme = LocalTheme.current
    RowShell(index, selected, onPick, Role.RadioButton) {
        val ring by animateColorAsState(if (selected) theme.colors.primary else theme.colors.text.copy(alpha = 0.5f), tween(200))
        Box(Modifier.size(20.dp).clip(CircleShape).border(2.dp, ring, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Box(Modifier.size(9.dp).popIn().clip(CircleShape).background(theme.colors.primary))
        }
        Names(item, Modifier.weight(1f))
        Text(item.code, color = theme.colors.primaryText.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp,
            modifier = Modifier.clip(CircleShape).background(theme.colors.menu).padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

/** A downloaded translation: code column, names, green tick. */
@Composable
fun DownloadedPick(item: PickItem, index: Int, onPick: () -> Unit) {
    val theme = LocalTheme.current
    RowShell(index, onClick = onPick, role = Role.RadioButton) {
        Text(item.code, color = theme.colors.primaryBright, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp), maxLines = 1)
        Names(item, Modifier.weight(1f))
        Box(Modifier.size(22.dp).clip(CircleShape).background(Tick.copy(alpha = 0.16f)).semantics { contentDescription = "Downloaded" }, contentAlignment = Alignment.Center) {
            Text("✓", color = Tick, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Not on this computer yet: Download, or progress with Cancel while it comes in. */
@Composable
fun AvailablePick(item: PickItem, index: Int, showCode: Boolean, onGet: () -> Unit, onCancel: (() -> Unit)?, action: String = "Download", onPick: (() -> Unit)? = null) {
    val theme = LocalTheme.current
    RowShell(index, onClick = onPick) {
        if (showCode) Text(item.code, color = theme.colors.primaryText.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp), maxLines = 1)
        Names(item, Modifier.weight(1f))
        if (item.progress == null) Text(
            action, color = Ink, fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(theme.colors.primary).clickable(role = Role.Button, onClick = onGet).hand().padding(horizontal = 12.dp, vertical = 8.dp)
        ) else if (onCancel != null) Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)).border(1.dp, theme.colors.border, RoundedCornerShape(4.dp))
                .clickable(role = Role.Button, onClickLabel = "Cancel download", onClick = onCancel).hand(),
            contentAlignment = Alignment.Center
        ) { Text("×", color = theme.colors.text, fontSize = 15.sp) }
    }
}

/** Filter chips (e.g. by language). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PickerChips(labels: List<String>, selected: String, onPick: (String) -> Unit) {
    val theme = LocalTheme.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEach { l ->
            val on = l == selected
            val bg by animateColorAsState(if (on) theme.colors.primary else theme.colors.menu, tween(200))
            Text(l, color = if (on) Ink else theme.colors.primaryText.copy(alpha = 0.85f), fontSize = 12.sp,
                modifier = Modifier.height(28.dp).clip(CircleShape).background(bg).border(1.dp, if (on) theme.colors.primary else theme.colors.border, CircleShape)
                    .clickable(role = Role.Tab) { onPick(l) }.hand().padding(horizontal = 11.dp, vertical = 6.dp))
        }
    }
}
