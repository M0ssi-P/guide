package org.theguide.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.zIndex
import design.Durations
import design.Metrics

/**
 * The TV boards' `.tvf` for the remote's D-pad: when focused, scale 1.05 over .35s (cubic-bezier(.2,.9,.1,1.2)), a 3 px
 * gold ring outside the shape and a deep shadow (0 26px 50px −20px rgba(0,0,0,.9)), drawn above its neighbours.
 * OK / Enter on the remote clicks.
 */
fun Modifier.tvFocus(shape: Shape, onClick: () -> Unit, role: Role? = Role.Button, onFocus: (Boolean) -> Unit = {}): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val s = animate(if (focused) Metrics.TvFocusScale else 1f, Durations.TvFocus, Ease.O12, "tvf")
    val ring by animateColorAsState(if (focused) C.Gold else Color.Transparent, tw(Durations.TvFocus), label = "tvfRing")
    val shadow = animate(if (focused) 1f else 0f, Durations.TvFocus, Ease.Css, "tvfShadow")
    zIndex(if (focused) 2f else 0f)
        .graphicsLayer { scaleX = s; scaleY = s }
        .then(if (shadow > 0f) Modifier.cssShadow(0f, 26f, 50f, -20f, Color.Black.copy(alpha = .9f * shadow), shape) else Modifier)
        .drawWithContent {
            drawContent()
            if (ring.alpha > 0f) {
                val w = Metrics.TvFocusRing * density
                val o = shape.createOutline(Size(size.width + w, size.height + w), layoutDirection, this)
                translate(-w / 2, -w / 2) { drawOutline(o, ring, style = Stroke(w)) }
            }
        }
        .onFocusChanged { focused = it.isFocused; onFocus(it.isFocused) }
        .clickable(remember { MutableInteractionSource() }, indication = null, role = role, onClick = onClick)
}

