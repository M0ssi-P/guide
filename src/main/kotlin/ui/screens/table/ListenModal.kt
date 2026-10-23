package ui.screens.table

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.checkIcon
import com.composables.downloadArrowIcon
import com.composables.musicNoteIcon
import com.composables.play
import com.composables.streamIcon
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import loadData
import logError
import parsers.vgr.SermonMedia
import parsers.vgr.models.Sermon
import saveData
import ui.modifier.stroke.BorderSide
import ui.modifier.stroke.newBorder
import ui.theme.LocalTheme

enum class ListenChoice { DOWNLOAD, STREAM }

private sealed interface ListenStep {
    data object Choose : ListenStep
    data object Fetching : ListenStep
    data object Downloading : ListenStep
    data class Failed(val message: String) : ListenStep
}

/**
 * Asks whether to download or stream a sermon that isn't on disk yet.
 * [onPlay] gets a local file path (download) or a remote url (stream).
 */
@Composable
fun ListenModal(sermon: Sermon, onPlay: (String) -> Unit, onDismiss: () -> Unit) {
    val theme = LocalTheme.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    val remembered = remember { loadData<String>("listen_choice")?.let { saved -> ListenChoice.entries.find { it.name == saved } } }
    var choice by remember { mutableStateOf(remembered ?: ListenChoice.DOWNLOAD) }
    var rememberChoice by remember { mutableStateOf(remembered != null) }
    var step by remember { mutableStateOf<ListenStep>(ListenStep.Choose) }
    var progress by remember { mutableStateOf(0f) }
    var job by remember { mutableStateOf<Job?>(null) }

    fun start() {
        if (job?.isActive == true) return
        saveData("listen_choice", if (rememberChoice) choice.name else null)
        job = scope.launch {
            try {
                step = ListenStep.Fetching
                val url = SermonMedia.audioUrl(sermon) ?: error("Couldn't get the audio link for this sermon.")
                when (choice) {
                    ListenChoice.STREAM -> onPlay(url)
                    ListenChoice.DOWNLOAD -> {
                        progress = 0f
                        step = ListenStep.Downloading
                        val file = SermonMedia.audioFile(sermon)
                        SermonMedia.download(url, file) { progress = it }
                        onPlay(file.absolutePath)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logError(e)
                step = ListenStep.Failed(e.message ?: "Something went wrong.")
            }
        }
    }

    fun cancel() {
        job?.cancel()
        step = ListenStep.Choose
    }

    // A remembered choice skips the question; the modal still shows progress.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        if (remembered != null) start()
    }
    // Closing the modal mid-download cancels it (SermonMedia cleans up the partial file).
    DisposableEffect(Unit) { onDispose { job?.cancel() } }

    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .width(480.dp)
            .dropShadow(shape = shape, block = {
                color = theme.colors.border
                spread = 1f
                offset = Offset(0f, 0f)
            })
            .dropShadow(shape = shape, block = {
                color = Color.Black.copy(alpha = 0.12f)
                alpha = 1f
                spread = -6f
                radius = 28f
                offset = Offset(0f, 14f)
            })
            .clip(shape)
            .background(theme.colors.popupLayer)
            .border(1.dp, theme.colors.border, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val choosing = step == ListenStep.Choose || step is ListenStep.Failed
                when (event.key) {
                    Key.Escape -> { onDismiss(); true }
                    Key.One, Key.NumPad1 -> { if (choosing) choice = ListenChoice.DOWNLOAD; choosing }
                    Key.Two, Key.NumPad2 -> { if (choosing) choice = ListenChoice.STREAM; choosing }
                    Key.Enter, Key.NumPadEnter -> { if (choosing) start(); choosing }
                    else -> false
                }
            }
    ) {
        PanelHeader("Listen", onClose = onDismiss)
        Box(Modifier.fillMaxWidth().height(1.dp).background(theme.colors.border))

        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SermonCard(sermon)

            when (val s = step) {
                ListenStep.Choose, is ListenStep.Failed -> {
                    Text(
                        if (s is ListenStep.Failed) s.message else "This sermon isn't on your device yet. How do you want to listen?",
                        color = if (s is ListenStep.Failed) theme.colors.crimson else theme.colors.text.copy(alpha = 0.75f),
                        fontSize = 13.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(OptionCardHeight)) {
                        OptionCard(
                            icon = ::downloadArrowIcon, title = "Download", hint = "1",
                            body = "Save it, then play. Works offline next time.",
                            selected = choice == ListenChoice.DOWNLOAD,
                            modifier = Modifier.weight(1f)
                        ) { choice = ListenChoice.DOWNLOAD }
                        OptionCard(
                            icon = ::streamIcon, title = "Stream", hint = "2",
                            body = "Start playing right away. Needs internet.",
                            selected = choice == ListenChoice.STREAM,
                            modifier = Modifier.weight(1f)
                        ) { choice = ListenChoice.STREAM }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RememberToggle(rememberChoice) { rememberChoice = it }
                        Row(
                            Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(theme.colors.primary)
                                .clickable(role = Role.Button, onClick = ::start)
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(play(theme.colors.night), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(12.dp))
                            Text(
                                when {
                                    s is ListenStep.Failed -> "Try again"
                                    choice == ListenChoice.DOWNLOAD -> "Download & play"
                                    else -> "Stream now"
                                },
                                color = theme.colors.night, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                ListenStep.Fetching -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    CircularProgressIndicator(color = theme.colors.primary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Column {
                        Text(
                            if (choice == ListenChoice.STREAM) "Getting the stream link…" else "Preparing download…",
                            color = theme.colors.text, fontSize = 13.sp, fontWeight = FontWeight.Medium
                        )
                        Text("This usually takes a second.", color = theme.colors.text.copy(alpha = 0.65f), fontSize = 11.5.sp)
                    }
                }

                ListenStep.Downloading -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Downloading…", color = theme.colors.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("${(progress * 100).toInt()}%", color = theme.colors.primary, fontSize = 12.sp)
                    }
                    Box(
                        Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp))
                            .background(theme.colors.text.copy(alpha = 0.1f))
                    ) {
                        Box(
                            Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight()
                                .clip(RoundedCornerShape(999.dp))
                                .background(Brush.horizontalGradient(listOf(theme.colors.primary, theme.colors.primaryBright)))
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("It will start playing as soon as it finishes.", color = theme.colors.text.copy(alpha = 0.65f), fontSize = 11.5.sp)
                        Box(
                            Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, theme.colors.border, RoundedCornerShape(10.dp))
                                .clickable(role = Role.Button, onClick = ::cancel)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancel", color = theme.colors.text, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .newBorder(theme.colors.border, 1.dp, sides = setOf(BorderSide.Top))
                .padding(horizontal = 20.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                KeyHint("1 2", "Choose")
                KeyHint("↵", "Confirm")
                KeyHint("Esc", "Close")
            }
            Text("TheGuide/media", color = theme.colors.text.copy(alpha = 0.4f), fontSize = 10.5.sp)
        }
    }
}

private val OptionCardHeight = 124.dp

@Composable
private fun SermonCard(sermon: Sermon) {
    val theme = LocalTheme.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(theme.colors.primary.copy(alpha = 0.12f))
            .border(1.dp, theme.colors.primary.copy(alpha = 0.35f), RoundedCornerShape(13.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(11.dp))
                .background(theme.colors.primary.copy(alpha = 0.16f))
                .border(1.dp, theme.colors.primary.copy(alpha = 0.4f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(musicNoteIcon(theme.colors.primary), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(sermon.title, color = theme.colors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text("${sermon.dateCode} · ${sermon.location}", color = theme.colors.text.copy(alpha = 0.6f), fontSize = 11.5.sp, maxLines = 1)
        }
        Text(
            sermon.language.uppercase().ifEmpty { "ENG" },
            color = theme.colors.primary, fontSize = 10.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier
                .background(theme.colors.primary.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
                .border(1.dp, theme.colors.primary.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun OptionCard(
    icon: (Color) -> ImageVector,
    title: String,
    hint: String,
    body: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val theme = LocalTheme.current
    val shape = RoundedCornerShape(13.dp)
    Column(
        modifier
            .fillMaxHeight()
            .clip(shape)
            .background(if (selected) theme.colors.primary.copy(alpha = 0.08f) else theme.colors.surface.copy(alpha = 0.6f))
            .border(1.dp, if (selected) theme.colors.primary.copy(alpha = 0.55f) else theme.colors.border, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                    .background(theme.colors.surface)
                    .border(1.dp, theme.colors.border, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon(theme.colors.primary), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(16.dp))
            }
            Text(
                hint, color = theme.colors.text.copy(alpha = 0.6f), fontSize = 10.5.sp,
                modifier = Modifier.border(1.dp, theme.colors.border, RoundedCornerShape(5.dp)).padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
        Column {
            Text(title, color = theme.colors.text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Text(body, color = theme.colors.text.copy(alpha = 0.7f), fontSize = 11.5.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun RememberToggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    val theme = LocalTheme.current
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.size(16.dp).clip(RoundedCornerShape(4.dp))
                .background(if (checked) theme.colors.primary else Color.Transparent)
                .border(1.dp, if (checked) theme.colors.primary else theme.colors.text.copy(alpha = 0.35f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) Icon(checkIcon(theme.colors.night), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(11.dp))
        }
        Text("Remember my choice", color = theme.colors.text.copy(alpha = 0.75f), fontSize = 12.sp)
    }
}
