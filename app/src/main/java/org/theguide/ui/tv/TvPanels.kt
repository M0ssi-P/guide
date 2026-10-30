package org.theguide.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.theguide.ui.*

/**
 * A TV side panel: 470 px (or [width]) from the right edge, 18 px in, sliding in over .55s, with the room dimmed from
 * left to right behind it. Back closes it; focus starts inside.
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun BoxScope.TvPanel(bg: Color, onClose: () -> Unit, width: Float = 470f, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onClose)
    val t by oneShot(Unit, 550)
    val s by oneShot(Unit, 350)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Box(Modifier.fillMaxSize().fade({ s }).background(Brush.horizontalGradient(listOf(Color(5, 8, 16).copy(alpha = .2f), Color(5, 8, 16).copy(alpha = .75f)))))
    val shape = RoundedCornerShape(30.dp)
    Column(
        Modifier.align(Alignment.CenterEnd).padding(18.dp).width(width.dp).fillMaxHeight().slideL({ t }, Ease.Sheet)
            .cssShadow(-30f, 0f, 80f, -30f, Color.Black.copy(alpha = .8f), shape).clip(shape).background(bg).topHighlight(white(.08f), shape)
            .focusRequester(focus).focusRestorer()
            .verticalScroll(rememberScrollState()).padding(34.dp),
        content = content,
    )
}
