package ui.screens.saved

import activity.SavedStore
import activity.SavedTarget
import activity.Tone
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import icons.bookmarkIcon
import com.composables.noteIcon
import com.composables.projectorIcon
import navigation.LocalModal
import navigation.dismiss
import navigation.show
import org.jetbrains.jewel.foundation.modifier.onHover
import ui.screens.thisweek.ActionButton
import ui.screens.thisweek.hand
import ui.theme.Fraunces
import ui.theme.Inter
import ui.theme.LocalTheme

/**
 * Highlight / bookmark / note / project row shown on a hovered verse or paragraph.
 * [title] and [meta] describe the bookmark; [progress] is how far into the chapter or sermon it is.
 */
@Composable
fun SaveToolbar(
    target: SavedTarget,
    text: String,
    ref: String,
    title: String,
    meta: String,
    progress: Float,
    modifier: Modifier = Modifier,
    onProject: (() -> Unit)? = null,
) {
    val theme = LocalTheme.current
    val modal = LocalModal.current
    val highlights by SavedStore.highlights
    val bookmarks by SavedStore.bookmarks
    val notes by SavedStore.notes
    val tone = highlights.firstOrNull { it.target.key == target.key }?.tone
    val marked = bookmarks.any { it.target.key == target.key }
    val hasNote = notes.any { it.target.key == target.key }
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier.clip(shape).background(theme.colors.popupLayer).border(1.dp, theme.colors.border, shape).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Tone.entries.forEach { t ->
            Box(
                Modifier.size(22.dp).clip(CircleShape).background(t.color)
                    .border(2.dp, if (tone == t) theme.colors.primaryText else Color.Transparent, CircleShape)
                    .clickable(role = Role.Checkbox) { SavedStore.highlight(target, t, text, ref) }.hand()
            )
        }
        Box(Modifier.width(1.dp).height(20.dp).background(theme.colors.border))
        ToolIcon(::bookmarkIcon, if (marked) "Remove bookmark" else "Bookmark", marked) { SavedStore.toggleBookmark(target, title, meta, progress) }
        ToolIcon(::noteIcon, if (hasNote) "Edit note" else "Add note", hasNote) {
            modal.show { NoteDialog(target, ref, onDismiss = { modal.dismiss() }) }
        }
        if (onProject != null) ToolIcon(::projectorIcon, "Project to screen", false, onProject)
    }
}

@Composable
private fun ToolIcon(icon: (Color) -> ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val theme = LocalTheme.current
    var hovered by remember { mutableStateOf(false) }
    val bg by animateColorAsState(if (hovered) theme.colors.menuHoverColor else Color.Transparent, tween(150))
    Box(
        Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(bg).onHover { hovered = it }
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick).hand(),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon(if (active) theme.colors.primary else theme.colors.primaryText), contentDescription = label, tint = Color.Unspecified, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun NoteDialog(target: SavedTarget, ref: String, onDismiss: () -> Unit) {
    val theme = LocalTheme.current
    val existing = remember { SavedStore.noteFor(target) }
    var title by remember { mutableStateOf(existing?.title?.takeIf { it != ref } ?: "") }
    var body by remember { mutableStateOf(existing?.body ?: "") }
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.width(460.dp).clip(shape).background(theme.colors.popup).border(1.dp, theme.colors.border, shape)
            .clickable(enabled = false) {}.padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Note", color = theme.colors.primaryText, fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontSize = 22.sp)
        Text(ref, color = theme.colors.text, fontSize = 12.5.sp)
        Field(title, { title = it }, "Title (optional)", single = true)
        Field(body, { body = it }, "Write your note…", single = false)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
            ActionButton("Cancel", onClick = onDismiss)
            ActionButton("Save note", primary = true) {
                SavedStore.saveNote(target, title, ref, body)
                onDismiss()
            }
        }
    }
}

@Composable
internal fun Field(value: String, onChange: (String) -> Unit, placeholder: String, single: Boolean, modifier: Modifier = Modifier) {
    val theme = LocalTheme.current
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier.fillMaxWidth().heightIn(min = if (single) 42.dp else 120.dp).clip(shape).background(theme.colors.menu)
            .border(1.dp, if (focused) theme.colors.primary else theme.colors.border, shape).padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        if (value.isEmpty()) Text(placeholder, color = theme.colors.text, fontSize = 13.5.sp, fontFamily = Inter)
        BasicTextField(
            value, onChange, singleLine = single,
            textStyle = TextStyle(color = theme.colors.primaryText, fontSize = 13.5.sp, fontFamily = Inter, fontWeight = FontWeight.Normal),
            cursorBrush = SolidColor(theme.colors.primary),
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }
        )
    }
}
