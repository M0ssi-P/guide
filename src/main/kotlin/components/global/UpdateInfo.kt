package components.global

import DesktopPlatform
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.shieldIcon
import kotlinx.coroutines.delay
import mvvm.UpdateViewModel
import navigation.LocalModal
import navigation.dismiss
import navigation.show
import org.jetbrains.jewel.foundation.modifier.onHover
import ui.screens.thisweek.hand
import ui.theme.LocalTheme
import ui.theme.ZillaSlab
import update.R4
import update.ShimmerBar
import update.UGreen
import update.UInk
import update.UpdButton
import update.UpdateStage
import update.Updates
import update.popOnce
import update.pulse
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class Note(val title: String, val text: String)

/** Bullet lines of the release body; "**Title.** rest" keeps the bold title. Falls back to the plain text. */
private fun notesOf(md: String?): List<Note> {
    val lines = md.orEmpty().lines().map { it.trim() }
    val bullets = lines.filter { it.startsWith("- ") || it.startsWith("* ") }.map { it.drop(2).trim() }
    if (bullets.isEmpty()) return lines.filter { it.isNotBlank() && !it.startsWith("#") }.take(6).map { Note("", it.replace("**", "")) }
    return bullets.map { b ->
        Regex("""^\*\*(.+?)\*\*\s*(.*)""").find(b)?.let { Note(it.groupValues[1], it.groupValues[2]) } ?: Note("", b.replace("**", ""))
    }
}

private fun mb(bytes: Long) = (bytes / 1_048_576.0).let { if (it < 10) "%.1f".format(it) else "${it.toInt()}" }

/** The update dialog (shown through LocalModal): available → downloading → checking → ready. */
@Composable
fun UpdateInfo(viewModel: UpdateViewModel) {
    val theme = LocalTheme.current
    val modal = LocalModal.current
    val r = Updates.release ?: Updates.notes ?: return
    val stage = Updates.stage
    val win = DesktopPlatform.Current == DesktopPlatform.Windows
    val cur = update.currentVersion
    val new = Updates.newVersion
    val asset = Updates.asset
    val released = runCatching { OffsetDateTime.parse(r.published_at).format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)) }.getOrNull()
    val notes = remember(r.body) { notesOf(r.body) }
    val notesOnly = Updates.release == null

    Column(
        Modifier.widthIn(max = 620.dp).fillMaxWidth(0.9f).rise(0, 16.dp).background(theme.colors.popupLayer, R4).border(1.dp, theme.colors.border, R4)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(52.dp).pulse(theme.colors.primary, circle = false).clip(R4)) {
                Image(painterResource("icons/logo.png"), null, Modifier.fillMaxSize())
            }
            Column(Modifier.weight(1f)) {
                Text(
                    when { notesOnly -> "What’s new in $cur"; stage == UpdateStage.READY -> "Ready to install $new"; else -> "A new version of The Guide is ready" },
                    color = theme.colors.primaryText, fontFamily = ZillaSlab, fontWeight = FontWeight.Bold, fontSize = 24.sp
                )
                if (!notesOnly) Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("You have $cur", color = theme.colors.text, fontSize = 12.5.sp,
                        modifier = Modifier.background(theme.colors.menu, R4).border(1.dp, theme.colors.border, R4).padding(horizontal = 9.dp, vertical = 3.dp))
                    Text("→", color = theme.colors.text, fontSize = 12.5.sp)
                    Text("New $new", color = theme.colors.primaryBright, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.background(theme.colors.primary.copy(alpha = 0.14f), R4).border(1.dp, theme.colors.primary.copy(alpha = 0.4f), R4).padding(horizontal = 9.dp, vertical = 3.dp))
                    Text(listOfNotNull(released?.let { "released $it" }, asset?.let { "${mb(it.size)} MB" }).joinToString(" · ", prefix = "· "), color = theme.colors.text, fontSize = 12.5.sp)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(theme.colors.border))

        Column(Modifier.fillMaxWidth().heightIn(max = 250.dp).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp)) {
            Text("WHAT'S NEW", color = theme.colors.text, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                notes.forEachIndexed { i, n ->
                    // the first two lines are the headline features
                    val star = i < 2 && n.title.isNotBlank()
                    Row(Modifier.rise(50 + i * 60, 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (star) "★" else "•", color = if (star) theme.colors.primaryBright else theme.colors.text, fontSize = 14.sp)
                        Text(buildAnnotatedString {
                            if (n.title.isNotBlank()) { withStyle(SpanStyle(color = theme.colors.primaryText, fontWeight = FontWeight.SemiBold)) { append(n.title) }; append(" ") }
                            append(n.text)
                        }, color = theme.colors.primaryText.copy(alpha = 0.85f), fontSize = 14.sp, lineHeight = 21.sp)
                    }
                }
            }
            if (!notesOnly) Row(
                Modifier.padding(top = 14.dp).fillMaxWidth().background(theme.colors.menu, R4).border(1.dp, theme.colors.border, R4).padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(shieldIcon(UGreen), null, tint = Color.Unspecified, modifier = Modifier.size(16.dp).padding(top = 1.dp))
                Text("Safe update: your current version is backed up first. If anything goes wrong, The Guide puts $cur back automatically. Your library, settings and downloads in ~/TheGuide are never touched.",
                    color = theme.colors.text, fontSize = 12.5.sp, lineHeight = 19.sp)
            }
        }

        Column(Modifier.fillMaxWidth().background(theme.colors.menu).padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 20.dp)) {
            when {
                notesOnly -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { UpdButton("Close") { modal.dismiss() } }
                stage == UpdateStage.AVAILABLE || stage == UpdateStage.IDLE -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UpdButton("Skip this version", ghost = true) { Updates.skip(); modal.dismiss() }
                    Spacer(Modifier.weight(1f))
                    UpdButton("Remind me tomorrow") { Updates.remindTomorrow(); modal.dismiss() }
                    UpdButton("Download update", primary = true) { Updates.startDownload(viewModel) }
                }
                stage == UpdateStage.DOWNLOADING || stage == UpdateStage.CHECKING || stage == UpdateStage.PREPARING -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val pct = if (Updates.total > 0) Updates.read.toFloat() / Updates.total else 0f
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(when (stage) {
                            UpdateStage.CHECKING -> "Checking the download is complete…"
                            UpdateStage.PREPARING -> "Backing up $cur before installing…"
                            else -> "Downloading ${asset?.name ?: "the update"}"
                        }, color = theme.colors.text, fontSize = 12.5.sp)
                        if (stage == UpdateStage.DOWNLOADING) Text("${mb(Updates.read)} of ${mb(Updates.total)} MB", color = theme.colors.text, fontSize = 12.5.sp)
                    }
                    Box(Modifier.semantics { contentDescription = "Download progress ${(pct * 100).toInt()}%" }) {
                        ShimmerBar(if (stage == UpdateStage.DOWNLOADING) pct else 1f, theme.colors.border, listOf(theme.colors.primary, theme.colors.primaryBright))
                    }
                    if (stage != UpdateStage.PREPARING) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("You can keep using The Guide while it downloads.", color = theme.colors.text, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        UpdButton("Cancel", ghost = true, height = 34.dp) { Updates.cancelDownload() }
                        UpdButton("Hide", height = 34.dp) { modal.dismiss() }
                    }
                }
                stage == UpdateStage.READY -> Row(Modifier.rise(0, 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(26.dp).popOnce().background(UGreen.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) { Text("✓", color = UGreen, fontSize = 13.sp) }
                    Text("Downloaded and checked. ${if (win) "Windows will ask for permission once." else "macOS may ask for your password."}", color = theme.colors.primaryText.copy(alpha = 0.85f), fontSize = 13.sp, modifier = Modifier.weight(1f))
                    UpdButton("Install when I quit") { Updates.installOnQuit = true; Updates.say("$new will install next time you quit The Guide."); modal.dismiss() }
                    UpdButton("Restart and install", primary = true) { Updates.restartAndInstall() }
                }
            }
        }
    }
}

/**
 * Lives over the whole window: opens the dialog once a day, the bottom-right pill while it downloads or waits, the
 * "You're on X" card after an update, and the toasts.
 */
@Composable
fun UpdateOverlay() = Box(Modifier.fillMaxSize()) {
    val theme = LocalTheme.current
    val modal = LocalModal.current
    val vm = remember { UpdateViewModel() }
    val stage = Updates.stage
    val modalOpen = modal.Composition.value != null
    LaunchedEffect(Updates.release, stage) {
        if (Updates.shouldAutoShow() && !modalOpen) modal.show { UpdateInfo(vm) }
    }
    // a hidden download brings the dialog back once it's ready
    val wasChecking = remember { booleanArrayOf(false) }
    LaunchedEffect(stage) {
        if (stage == UpdateStage.READY && wasChecking[0] && modal.Composition.value == null) modal.show { UpdateInfo(vm) }
        wasChecking[0] = stage == UpdateStage.CHECKING || stage == UpdateStage.DOWNLOADING
    }
    LaunchedEffect(Updates.toast) { if (Updates.toast != null) { delay(2600); Updates.toast = null } }

    val pillOn = !modalOpen && (stage == UpdateStage.DOWNLOADING || stage == UpdateStage.CHECKING || stage == UpdateStage.READY)
    AnimatedVisibility(pillOn, Modifier.align(Alignment.BottomEnd).padding(20.dp), enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut()) {
        val ready = stage == UpdateStage.READY
        val pct = if (Updates.total > 0) Updates.read.toFloat() / Updates.total else 0f
        Row(
            Modifier.height(40.dp).background(theme.colors.menu, R4).border(1.dp, theme.colors.border, R4)
                .semantics { contentDescription = if (ready) "Update ready, open to install" else "Show update download" }
                .clickable { modal.show { UpdateInfo(vm) } }.hand().padding(start = 10.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                val track = theme.colors.border; val fill = theme.colors.primary
                Canvas(Modifier.size(22.dp)) {
                    val w = 3.dp.toPx()
                    drawCircle(if (ready) UGreen else track, radius = size.minDimension / 2 - w / 2, style = Stroke(w))
                    if (!ready) drawArc(fill, -90f, 360f * pct, false, topLeft = Offset(w / 2, w / 2), size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w), style = Stroke(w))
                }
                Text(if (ready) "✓" else "⇩", color = theme.colors.primaryBright, fontSize = 9.sp)
            }
            Text(if (ready) "Update ready · Restart" else "Downloading update · ${(pct * 100).toInt()}%", color = theme.colors.primaryText, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    AnimatedVisibility(Updates.justUpdated != null, Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 24.dp), enter = fadeIn() + slideInVertically { it / 4 }, exit = fadeOut()) {
        Column(Modifier.width(340.dp).rise(250, 16.dp).background(theme.colors.popupLayer, R4).border(1.dp, theme.colors.border, R4).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(34.dp).popOnce(500).background(UGreen.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) { Text("✓", color = UGreen, fontSize = 16.sp) }
                Column {
                    Text("You're on ${Updates.justUpdated}", color = theme.colors.primaryText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Updated just now · backup removed", color = theme.colors.text, fontSize = 12.sp)
                }
            }
            Text("Everything is where you left it.", color = theme.colors.text, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 12.dp))
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UpdButton("Got it", height = 34.dp) { Updates.justUpdated = null }
                if (Updates.notes != null) UpdButton("What's new", ghost = true, height = 34.dp) { modal.show { UpdateInfo(vm) } }
            }
        }
    }

    AnimatedVisibility(Updates.toast != null, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp), enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut() + slideOutVertically { it / 2 }) {
        Text(Updates.toast ?: "", color = Color(0xFF1E1B1A), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.background(Color(0xFFF6F1EC), R4).padding(horizontal = 16.dp, vertical = 10.dp))
    }
}
