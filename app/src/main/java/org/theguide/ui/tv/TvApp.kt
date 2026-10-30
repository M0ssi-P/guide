package org.theguide.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.theguide.data.Prefs
import org.theguide.data.Updates
import org.theguide.live.LiveClient
import org.theguide.ui.*
import org.theguide.ui.Nav.Tab

/**
 * The TV app. The TV boards are drawn 1280 px wide; here 1 dp is one board px on any TV (1280 dp across the screen),
 * so every size is the board's.
 */
@Composable
fun TvApp() {
    BoxWithConstraints(Modifier.fillMaxSize().background(C.Backdrop)) {
        val d = LocalDensity.current
        // a 1280×720 stage, as large as the screen allows (letterboxed on screens that aren't 16:9)
        val scale = minOf(with(d) { maxWidth.toPx() } / 1280f, with(d) { maxHeight.toPx() } / 720f)
        CompositionLocalProvider(LocalDensity provides Density(scale, 1f)) {
            Box(Modifier.align(Alignment.Center).size(1280.dp, 720.dp).clip(androidx.compose.ui.graphics.RectangleShape)) {
                var opened by remember { mutableStateOf(Prefs.setupDone) }
                if (!opened) TvLaunch { opened = true } else TvShell()
            }
        }
    }
}

/**
 * The screen's ambient light: an ellipse 55%×60% of the screen at [at], fading out at 70%, over a box 20% larger
 * than the screen on every side.
 */
fun Modifier.ambient(color: Color, atX: Float, atY: Float) = drawBehind {
    val w = size.width * 1.4f; val h = size.height * 1.4f
    val ox = -size.width * .2f; val oy = -size.height * .2f
    val c0 = Offset(ox + w * atX, oy + h * atY)
    val rx = w * .55f; val ry = h * .6f
    scale(1f, ry / rx, pivot = c0) {
        drawCircle(Brush.radialGradient(0f to color, .7f to Color.Transparent, center = c0, radius = rx), radius = rx, center = c0)
    }
}

private val rail = listOf(
    Tab.Home to Icons.RailHome, Tab.Table to Icons.RailTable, Tab.Bible to Icons.RailBible,
    Tab.Live to Icons.RailLive, Tab.Songs to Icons.RailSongs, Tab.Library to Icons.RailLibrary,
)

@Composable
private fun TvShell() {
    LaunchedEffect(Unit) { Updates.check(); LiveClient.discover() }
    BackHandler(enabled = Nav.tab != Tab.Home) { Nav.go(Tab.Home) }
    val railFocus = remember { FocusRequester() }
    val focus = LocalFocusManager.current
    // Left with nothing further left (low on the screen, where no rail icon lines up) goes to the rail
    Box(Modifier.fillMaxSize().background(C.Backdrop).onKeyEvent { e ->
        if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionLeft && !focus.moveFocus(FocusDirection.Left)) { runCatching { railFocus.requestFocus() }; true } else false
    }) {
        when (Nav.tab) {
            Tab.Home -> TvHome()
            Tab.Table -> TvTable()
            Tab.Bible -> TvBible()
            Tab.Live -> TvJoin()
            Tab.Songs -> TvSongs()
            Tab.Library -> TvLibrary()
        }
        Rail(railFocus)
    }
}

/** The left rail: 100 wide, a faint shade, the six places as 54 px round icons, the current one lit gold. */
@Composable
private fun BoxScope.Rail(railFocus: FocusRequester) {
    Column(
        Modifier.width(100.dp).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = .16f), Color.Transparent)))
            .padding(start = 24.dp, top = 40.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rail.forEach { (tab, icon) ->
            val on = Nav.tab == tab
            Box(
                Modifier.size(54.dp).tvFocus(RoundedCornerShape(27.dp), { Nav.go(tab) }).clip(RoundedCornerShape(27.dp)).background(if (on) gold(.16f) else Color.Transparent)
                    .then(if (on) Modifier.focusRequester(railFocus) else Modifier).label(tab.name),
                contentAlignment = Alignment.Center,
            ) { Ico(icon, 24f, if (on) C.Gold else C.Faint) }
        }
    }
}
