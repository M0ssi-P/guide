package ui.screens.thisweek

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import parsers.bt.Letter
import parsers.bt.LetterPart
import ui.theme.Fraunces
import ui.theme.LocalTheme

private val focusSizes = listOf(17, 19, 22, 25, 28)

/** Distraction-free reader shown through the modal layer. Esc closes, Ctrl/Cmd +/- resizes. */
@Composable
fun LetterFocus(letter: Letter, onDismiss: () -> Unit) {
    val theme = LocalTheme.current
    val clipboard = LocalClipboardManager.current
    val scroll = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    var size by remember { mutableStateOf(1) }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val progress = if (scroll.maxValue > 0) scroll.value / scroll.maxValue.toFloat() else 0f
    val shape = RoundedCornerShape(24.dp)

    Column(
        Modifier
            .width(780.dp)
            .fillMaxHeight(0.88f)
            .clip(shape)
            .background(theme.colors.popupLayer)
            .border(1.dp, theme.colors.border, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val mod = e.isCtrlPressed || e.isMetaPressed
                when {
                    e.key == Key.Escape -> { onDismiss(); true }
                    mod && (e.key == Key.Equals || e.key == Key.Plus) -> { size = (size + 1).coerceAtMost(focusSizes.lastIndex); true }
                    mod && e.key == Key.Minus -> { size = (size - 1).coerceAtLeast(0); true }
                    else -> false
                }
            }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("FOCUS", color = theme.colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                Text("${letter.code} · ${letter.title}", color = theme.colors.text, fontSize = 12.5.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionButton("A−") { size = (size - 1).coerceAtLeast(0) }
                ActionButton("A+") { size = (size + 1).coerceAtMost(focusSizes.lastIndex) }
                ActionButton("Done  Esc", onClick = onDismiss)
            }
        }
        // reading progress, follows the scroll
        Box(Modifier.fillMaxWidth().height(2.dp).background(theme.colors.border)) {
            Box(Modifier.fillMaxWidth(progress).height(2.dp).background(theme.colors.primary))
        }

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(horizontal = 56.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            val fontSize = focusSizes[size]
            letter.sections.forEach { s ->
                if (letter.sections.size > 1) SectionLabel(s.part.label)
                if (s.part == LetterPart.LETTER) letter.salutation?.let {
                    Text(it, fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontSize = (fontSize + 5).sp, color = theme.colors.primaryBright)
                }
                s.blocks.forEach { LetterBlockView(it, fontSize) { t -> clipboard.setText(AnnotatedString(t)) } }
                if (s.part == LetterPart.LETTER) Text("Bro. Joseph Branham", fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontSize = fontSize.sp, color = theme.colors.primaryBright)
            }
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.End) {
                ActionButton("Finish & mark letter read", primary = true) {
                    LetterPrefs.markRead(letter.id)
                    onDismiss()
                }
            }
        }
    }
}
