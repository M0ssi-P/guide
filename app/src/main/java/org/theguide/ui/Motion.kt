package org.theguide.ui

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import design.Durations
import design.Easings
import kotlinx.coroutines.delay

/** The boards' timing functions. */
object Ease {
    val Swap = Easings.Swap.easing
    val Sheet = Easings.Sheet.easing
    val Press = Easings.Press.easing
    val O10 = Easings.Overshoot10.easing
    val O115 = Easings.Overshoot115.easing
    val O12 = Easings.Overshoot12.easing
    val O13 = Easings.Overshoot13.easing
    val O14 = Easings.Overshoot14.easing
    val O16 = Easings.Overshoot16.easing
    /** CSS `ease`, the default when a board names none. */
    val Css = Easings.CssEase.easing
    val InOut = Easings.CssEaseInOut.easing
    val Out = Easings.CssEaseOut.easing
    val Linear = LinearEasing
}

fun <T> tw(ms: Int, easing: Easing = Ease.Css, delay: Int = 0): AnimationSpec<T> = tween(ms, delay, easing)

/** A CSS transition on a number: `transition: prop [ms] [easing]`. */
@Composable
fun animate(target: Float, ms: Int, easing: Easing = Ease.Css, label: String = "t"): Float =
    animateFloatAsState(target, tween(ms, 0, easing), label = label).value

// ---------- one-shot animations (CSS `animation: name dur easing delay both`) ----------

/**
 * 0→1 over [ms] after [delayMs], restarted whenever [key] changes, like a CSS animation whose name flips
 * ("enter"/"enterB") to replay. Holds 0 during the delay (fill-mode both). [easing] is applied by the caller's
 * keyframes, so this returns linear time.
 */
@Composable
fun oneShot(key: Any?, ms: Int, delayMs: Int = 0): State<Float> {
    val a = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        if (delayMs > 0) delay(delayMs.toLong())
        a.animateTo(1f, tween(ms, 0, LinearEasing))
    }
    return a.asState()
}

/** Piecewise keyframes over 0..1 with [easing] inside every segment, as CSS applies it. */
fun keyframes(t: Float, easing: Easing, vararg points: Pair<Float, Float>): Float {
    if (t <= points.first().first) return points.first().second
    for (i in 1 until points.size) {
        val (t1, v1) = points[i]
        if (t <= t1) {
            val (t0, v0) = points[i - 1]
            val e = easing.transform(((t - t0) / (t1 - t0)).coerceIn(0f, 1f))
            return v0 + (v1 - v0) * e
        }
    }
    return points.last().second
}

private fun blurLayer(radiusPx: Float) =
    if (Build.VERSION.SDK_INT >= 31 && radiusPx > 0.2f) RenderEffect.createBlurEffect(radiusPx, radiusPx, Shader.TileMode.DECAL).asComposeRenderEffect() else null

/** `@keyframes enter`: from opacity 0, blur 8px, 10px down. */
fun Modifier.enter(t: () -> Float, easing: Easing = Ease.Swap) = graphicsLayer {
    val p = easing.transform(t())
    alpha = p
    translationY = (1 - p) * 10.dp.toPx()
    renderEffect = blurLayer((1 - p) * 8.dp.toPx() / 2f)
}

/** `@keyframes rise`: from opacity 0, 16px down. */
fun Modifier.rise(t: () -> Float, easing: Easing = Ease.Css) = graphicsLayer {
    val p = easing.transform(t())
    alpha = p
    translationY = (1 - p) * 16.dp.toPx()
}

/** `@keyframes fade`. */
fun Modifier.fade(t: () -> Float, easing: Easing = Ease.Css) = graphicsLayer { alpha = easing.transform(t()) }

/** `@keyframes pop`: scale .6 and hidden, 1.08 at 70%, then 1. */
fun Modifier.pop(t: () -> Float, easing: Easing = Ease.Css) = graphicsLayer {
    val v = t()
    val s = keyframes(v, easing, 0f to .6f, .7f to 1.08f, 1f to 1f)
    scaleX = s; scaleY = s
    alpha = keyframes(v, easing, 0f to 0f, .7f to 1f, 1f to 1f)
}

/** CSS perspective in px → Compose camera distance (Skia keeps 72 units an inch). */
fun perspective(px: Float) = px / 72f

/** `@keyframes unfold`: perspective(900px) rotateX(-70deg), hidden → flat; origin top centre. */
fun Modifier.unfold(t: () -> Float, easing: Easing = Ease.Swap, origin: TransformOrigin = TransformOrigin(.5f, 0f)) = graphicsLayer {
    val p = easing.transform(t())
    transformOrigin = origin
    cameraDistance = perspective(900f) * density
    rotationX = -70f * (1 - p)
    alpha = p
}

/** `@keyframes sheetUp`: from 105% of its own height below. */
fun Modifier.sheetUp(t: () -> Float, easing: Easing = Ease.Sheet) = graphicsLayer { translationY = size.height * 1.05f * (1 - easing.transform(t())) }

/** `@keyframes sheetDown`: from 140% of its own height above. */
fun Modifier.sheetDown(t: () -> Float, easing: Easing = Ease.Sheet) = graphicsLayer { translationY = -size.height * 1.4f * (1 - easing.transform(t())) }

/** `@keyframes slideL`: from 105% of its own width to the right. */
fun Modifier.slideL(t: () -> Float, easing: Easing = Ease.Sheet) = graphicsLayer { translationX = size.width * 1.05f * (1 - easing.transform(t())) }

// ---------- looping animations ----------

/** Milliseconds since this composable appeared, every frame. */
@Composable
fun clock(): State<Long> {
    val now = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        while (true) withFrameMillis { now.longValue = it - start }
    }
    return now
}

/** Phase 0..1 of a loop of [ms], shifted by [offsetMs] (a CSS animation-delay; negative starts part-way in). */
fun phase(timeMs: Long, ms: Int, offsetMs: Int = 0): Float = (((timeMs - offsetMs) % ms + ms) % ms) / ms.toFloat()

/** `@keyframes halo`: opacity .55 → .85, scale 1 → 1.08 and back, ease-in-out. */
fun Modifier.halo(time: () -> Long, ms: Int = Durations.Halo) = graphicsLayer {
    val p = phase(time(), ms)
    alpha = keyframes(p, Ease.InOut, 0f to .55f, .5f to .85f, 1f to .55f)
    val s = keyframes(p, Ease.InOut, 0f to 1f, .5f to 1.08f, 1f to 1f)
    scaleX = s; scaleY = s
}

/** `@keyframes beat`: scale 1 → 1.3 → 1. */
fun Modifier.beat(time: () -> Long, ms: Int = Durations.Beat) = graphicsLayer {
    val s = keyframes(phase(time(), ms), Ease.InOut, 0f to 1f, .5f to 1.3f, 1f to 1f)
    scaleX = s; scaleY = s
}

/** `@keyframes spin`, linear; [running] false holds the angle (animation-play-state: paused). */
fun Modifier.spin(angle: () -> Float) = graphicsLayer { rotationZ = angle() }

/** `@keyframes swing`: −3° → −1.2° → −3° about [origin]. */
fun Modifier.swing(time: () -> Long, ms: Int = Durations.Swing, origin: TransformOrigin) = graphicsLayer {
    transformOrigin = origin
    rotationZ = keyframes(phase(time(), ms), Ease.InOut, 0f to -3f, .5f to -1.2f, 1f to -3f)
}

/** `@keyframes ripple`: a ring from scale .4 at .55 opacity out to 2.6 and gone, ease-out. */
fun Modifier.ripple(time: () -> Long, ms: Int, offsetMs: Int = 0) = graphicsLayer {
    val p = Ease.Out.transform(phase(time(), ms, offsetMs))
    val s = .4f + 2.2f * p
    scaleX = s; scaleY = s
    alpha = .55f * (1 - p)
}

/** `@keyframes wave`: scaleY .3 → 1 → .3 from the centre. */
fun Modifier.wave(time: () -> Long, ms: Int = Durations.Wave, offsetMs: Int = 0) = graphicsLayer {
    scaleY = keyframes(phase(time(), ms, offsetMs), Ease.InOut, 0f to .3f, .5f to 1f, 1f to .3f)
}

/** `@keyframes hint`: opacity .5 → 1 → .5. */
fun Modifier.hint(time: () -> Long, ms: Int = 3000) = graphicsLayer {
    alpha = keyframes(phase(time(), ms), Ease.InOut, 0f to .5f, .5f to 1f, 1f to .5f)
}

/** A spinning angle that keeps its place while [running] is false (CSS animation-play-state). */
@Composable
fun spinAngle(periodMs: Float, running: Boolean): State<Float> {
    val angle = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    LaunchedEffect(running, periodMs) {
        if (!running) return@LaunchedEffect
        var last = withFrameMillis { it }
        while (true) withFrameMillis { now ->
            angle.floatValue = (angle.floatValue + (now - last) / periodMs * 360f) % 360f
            last = now
        }
    }
    return angle
}

// ---------- press ----------

/**
 * The boards' `.press`: tapping scales to .96 over .18s (cubic-bezier(.3,.7,.2,1)); no ripple. Clickable with
 * [role] for accessibility.
 */
fun Modifier.press(onClick: () -> Unit, enabled: Boolean = true, role: Role? = Role.Button, label: String? = null) = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val s = animate(if (pressed) .96f else 1f, Durations.Press, Ease.Press, "press")
    graphicsLayer { scaleX = s; scaleY = s }
        .clickable(source, indication = null, enabled = enabled, onClickLabel = label, role = role, onClick = onClick)
}
