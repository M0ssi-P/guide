package org.theguide.ui.phone

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import design.Palette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import org.theguide.data.Updates
import org.theguide.live.LiveClient
import org.theguide.ui.C
import org.theguide.ui.Icons
import org.theguide.ui.Ico
import org.theguide.ui.Nav
import org.theguide.ui.Nav.Tab
import org.theguide.ui.T
import org.theguide.ui.c
import org.theguide.ui.cssShadow
import org.theguide.ui.ellipseGradient
import org.theguide.ui.gold
import org.theguide.ui.press
import org.theguide.ui.topHighlight
import org.theguide.ui.white

/** The phone after first launch: the tab screens, the nav bar on the tab roots, and the player over everything. */
@Composable
fun PhoneShell() {
    val haze = remember { HazeState() }
    LaunchedEffect(Unit) { Updates.check(); LiveClient.discover() }
    BackHandler(enabled = Nav.player || Nav.tab != Tab.Home) { if (Nav.player) Nav.player = false else Nav.go(Tab.Home) }
    Box(Modifier.fillMaxSize().background(C.Night)) {
        Box(Modifier.fillMaxSize()) {
            when (Nav.tab) {
                Tab.Home -> PhoneHome(haze)
                Tab.Table -> PhoneTable(haze)
                Tab.Bible -> PhoneBible(haze)
                Tab.Live -> PhoneLive(haze)
                Tab.Songs -> PhoneSongs(haze)
                Tab.Library -> PhoneLibrary(haze)
            }
        }
        if (Nav.tab in setOf(Tab.Home, Tab.Songs, Tab.Library) && Nav.sheets == 0) NavBar(haze)
        if (Nav.player) PhonePlayer(haze)
    }
}

private val tabs = listOf(
    Triple(Tab.Home, "Home", Icons.TabHome), Triple(Tab.Table, "Table", Icons.TabTable), Triple(Tab.Bible, "Bible", Icons.TabBible),
    Triple(Tab.Songs, "Songs", Icons.TabSongs), Triple(Tab.Library, "Library", Icons.TabLibrary),
)

/** The frosted bar: 64 tall, 14 from the sides and 26 from the bottom, the lit tab glowing from below. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.NavBar(haze: HazeState) {
    val shape = RoundedCornerShape(32.dp)
    val current = tabs.indexOfFirst { it.first == Nav.tab }
    BoxWithConstraints(
        Modifier.align(Alignment.BottomCenter).padding(start = 14.dp, end = 14.dp, bottom = 26.dp + (bottomInset() - 8.dp).coerceAtLeast(0.dp))
            .fillMaxWidth().height(64.dp)
            .cssShadow(0f, 20f, 40f, -18f, Color.Black.copy(alpha = .9f), shape)
            .clip(shape)
            .hazeEffect(haze, HazeStyle(C.Night, HazeTint(c(Palette.NavGlass)), 18.dp, noiseFactor = 0f))
            .topHighlight(white(.08f), shape)
            .padding(horizontal = 6.dp)
    ) {
        val w = maxWidth / 5
        if (current >= 0) Box(
            Modifier.offset(x = w * current).width(w).padding(vertical = 8.dp).fillMaxHeight().clip(RoundedCornerShape(24.dp))
                .drawBehind { ellipseGradient(.5f, 1.2f, 0f to gold(.45f), .7f to gold(.08f)) }
        )
        Row(Modifier.fillMaxSize()) {
            tabs.forEach { (tab, label, icon) ->
                val on = tab == Nav.tab
                Column(
                    Modifier.weight(1f).fillMaxHeight().press({ Nav.go(tab) }, role = Role.Tab, label = label),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                ) {
                    val col = if (on) C.Ink else c(Palette.TabIdle)
                    Ico(icon, 22f, col)
                    T(label, 10.5f, col, weight = 600)
                }
            }
        }
    }
}
