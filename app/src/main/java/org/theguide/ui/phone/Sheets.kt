package org.theguide.ui.phone

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import design.Palette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import org.theguide.ui.Ease
import org.theguide.ui.cssShadow
import org.theguide.ui.fade
import org.theguide.ui.oneShot
import org.theguide.ui.sheetUp
import org.theguide.ui.topHighlight
import org.theguide.ui.white

/** The phone's top inset: the board's 54 px status area becomes the real status bar. */
@Composable
fun statusTop() = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

/** Where the boards put things 8 px off the bottom edge: above the gesture bar. */
@Composable
fun bottomInset() = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/** The dimmed, softly blurred page behind sheets (`rgba(5,8,16,.5)`, blur 3px), fading in over .3s; tapping it closes. */
@Composable
fun Scrim(color: Color, haze: HazeState?, blur: Float, base: Color = Color(Palette.Night), onClick: () -> Unit) {
    val t by oneShot(Unit, 300)
    Box(
        Modifier.fillMaxSize().fade({ t })
            .then(if (haze != null) Modifier.hazeEffect(haze, HazeStyle(base, HazeTint(color), blur.dp, noiseFactor = 0f)) else Modifier.background(color))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick)
    )
}

/**
 * A bottom sheet as on the boards: 8 px from the edges, 38 px corners, the 40×5 handle, sliding up over .5s
 * (cubic-bezier(.2,.9,.1,1)). [bg]/[line] follow the reading theme.
 */
@Composable
fun BoxScope.Sheet(
    onDismiss: () -> Unit, haze: HazeState?, bg: Color = Color(Palette.Sheet), line: Color = white(.18f),
    scrim: Color = Color(Palette.Scrim), scrimBlur: Float = 3f, padTop: Float = 12f, padX: Float = 20f, padBottom: Float = 26f,
    handleBottom: Float = 12f, maxHeight: Float = 720f, highlight: Boolean = true, shadow: Boolean = true, base: Color = Color(Palette.Night),
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(onBack = onDismiss)
    androidx.compose.runtime.DisposableEffect(Unit) { org.theguide.ui.Nav.sheets++; onDispose { org.theguide.ui.Nav.sheets-- } }
    Scrim(scrim, haze, scrimBlur, base, onDismiss)
    val t by oneShot(Unit, 500)
    val shape = RoundedCornerShape(38.dp)
    // above the keyboard while typing in a sheet (share link, a new song)
    val inset = maxOf(bottomInset(), WindowInsets.ime.asPaddingValues().calculateBottomPadding())
    Column(
        Modifier.align(Alignment.BottomCenter).padding(start = 8.dp, end = 8.dp, bottom = 8.dp + inset).fillMaxWidth()
            .heightIn(max = maxHeight.dp).sheetUp({ t }, Ease.Sheet)
            .then(if (shadow) Modifier.cssShadow(0f, -30f, 60f, -30f, Color.Black.copy(alpha = .6f), shape) else Modifier)
            .clip(shape).background(bg).then(if (highlight) Modifier.topHighlight(white(.08f), shape) else Modifier)
            .clickable(remember { MutableInteractionSource() }, indication = null) {}
            .verticalScroll(rememberScrollState())
            .padding(start = padX.dp, end = padX.dp, top = padTop.dp, bottom = padBottom.dp),
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = handleBottom.dp).size(40.dp, 5.dp).clip(RoundedCornerShape(3.dp)).background(line))
        content()
    }
}

@Composable
fun HRow(modifier: Modifier = Modifier, gap: Float = 0f, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) =
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(gap.dp), content = content)

