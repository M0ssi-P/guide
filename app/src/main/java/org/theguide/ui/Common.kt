package org.theguide.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import design.Durations
import design.Palette
import kotlin.math.min
import kotlin.math.tan

fun svgPath(d: String): Path = PathParser().parsePathString(d).toPath()

// ---------- the lamp ----------

private val flameOuter = svgPath("M27 8c5 7 8 11 8 16a8 8 0 0 1-16 0c0-5 3-9 8-16z")
private val flameInner = svgPath("M27 18c2 3 3.5 5 3.5 7a3.5 3.5 0 0 1-7 0c0-2 1.5-4 3.5-7z")
private val lampBody = svgPath("M6 40c6-3 36-3 42 0l-4 10c-1 3-4 5-8 5H18c-4 0-7-2-8-5z")
private val lampHandle = svgPath("M48 41c4 0 6 2 6 4s-2 3-5 3")
private val flameBrush = Brush.verticalGradient(listOf(c(Palette.Flame), c(Palette.Amber)), 8f, 32f)
private val bodyBrush = Brush.verticalGradient(listOf(c(Palette.LampTop), c(Palette.LampBottom)), 37.75f, 55f)

/** The boards' lamp (viewBox 54×62): flame scaled by [flame] about (27,30), flickering with [time]. */
fun DrawScope.drawLamp(flame: Float, timeMs: Long) {
    val k = min(size.width / 54f, size.height / 62f)
    translate((size.width - 54f * k) / 2f, (size.height - 62f * k) / 2f) {
        scale(k, k, pivot = Offset.Zero) {
            // @keyframes flicker: scaleY / skewX at 0, 25, 50, 75, 100%, ease-in-out
            val p = phase(timeMs, Durations.Flicker)
            val sy = keyframes(p, Ease.InOut, 0f to 1f, .25f to 1.06f, .5f to .95f, .75f to 1.03f, 1f to 1f)
            val sk = keyframes(p, Ease.InOut, 0f to 0f, .25f to -2f, .5f to 1.5f, .75f to -1f, 1f to 0f)
            withTransform({
                scale(flame, flame, pivot = Offset(27f, 30f))
                transform(Matrix().apply {
                    translate(27f, 30f)
                    val m = Matrix(floatArrayOf(1f, 0f, 0f, 0f, tan(Math.toRadians(sk.toDouble())).toFloat(), sy, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f))
                    timesAssign(m)
                    translate(-27f, -30f)
                })
            }) {
                drawPath(flameOuter, flameBrush)
                drawPath(flameInner, c(Palette.FlameCore))
            }
            drawPath(lampBody, bodyBrush)
            drawPath(lampHandle, c(Palette.LampHandle), style = Stroke(2.5f))
            drawRoundRect(c(Palette.LampBottom), Offset(22f, 33f), androidx.compose.ui.geometry.Size(10f, 7f), androidx.compose.ui.geometry.CornerRadius(2f))
        }
    }
}

/**
 * The growing lamp of the Launch boards: lit 0..1 sets its size (70+40·lit × 81+46·lit), the halo (90+110·lit at
 * .25+.35·lit) and the flame (.35+.65·lit), each easing in as the board's transitions do.
 */
@Composable
fun GrowingLamp(lit: Float, modifier: Modifier = Modifier) {
    val time by clock()
    val w = animate(70 + 40 * lit, 800)
    val h = animate(81 + 46 * lit, 800)
    val halo = animate(90 + 110 * lit, 1000)
    val flame = animate(.35f + .65f * lit, 1000, Ease.O12)
    val haloA = .25f + .35f * lit
    Box(modifier.size(w.dp, h.dp)) {
        Box(
            Modifier.align(Alignment.TopCenter).requiredSize(halo.dp).halo({ time })
                .background(Brush.radialGradient(0f to gold(haloA), .65f to Color.Transparent), CircleShape)
        )
        Canvas(Modifier.fillMaxSize()) { drawLamp(flame, time) }
    }
}

/** The small fixed lamp beside the reading streak (54×62, halo 60 at .45, 2.4s). */
@Composable
fun StreakLamp(w: Float = 54f, h: Float = 62f, haloSize: Float = 60f, haloTop: Float = 2f) {
    val time by clock()
    Box(Modifier.size(w.dp, h.dp)) {
        Box(
            Modifier.align(Alignment.TopCenter).offset(y = haloTop.dp).requiredSize(haloSize.dp).halo({ time }, 2400)
                .background(Brush.radialGradient(0f to gold(.45f), .65f to Color.Transparent), CircleShape)
        )
        Canvas(Modifier.fillMaxSize()) { drawLamp(1f, time) }
    }
}

// ---------- page glow ----------

/**
 * The light over every screen: `left -40%, top -30%, width 180%, height 90%`, an ellipse at 50% 30% fading out at
 * 60%, its colour easing over 1.2s.
 */
fun Modifier.screenGlow(color: Color) = drawBehind {
    val box = androidx.compose.ui.geometry.Size(size.width * 1.8f, size.height * .9f)
    ellipseGradient(.5f, .3f, 0f to color, .6f to Color.Transparent, box = box, origin = Offset(-size.width * .4f, -size.height * .3f))
}

// ---------- buttons ----------

/** The gold pill: 56 tall, #F3C969, bold dark text, with its warm shadow. */
@Composable
fun GoldButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Float = 56f, size: Float = 16f, weight: Int = 700, enabled: Boolean = true, alpha: Float = 1f, shadow: Boolean = true) {
    val shape = RoundedCornerShape((height / 2).dp)
    Box(
        modifier.height(height.dp).graphicsLayer { this.alpha = alpha }
            .then(if (shadow) Modifier.cssShadow(0f, 14f, 36f, -12f, gold(.7f), shape) else Modifier)
            .clip(shape).background(C.Gold).press(onClick, enabled),
        contentAlignment = Alignment.Center,
    ) { T(label, size, C.OnGold, weight = weight, align = TextAlign.Center) }
}

/** A round 40 px icon button on a faint chip. */
@Composable
fun RoundButton(onClick: () -> Unit, label: String, modifier: Modifier = Modifier, size: Float = 40f, bg: Color = white(.07f), content: @Composable BoxScope.() -> Unit) =
    Box(
        modifier.size(size.dp).clip(CircleShape).background(bg).press(onClick, label = label).semantics { contentDescription = label },
        contentAlignment = Alignment.Center, content = content,
    )

/** The boards' switch: a [w]×[h] track, gold when on, with a knob that springs across. */
@Composable
fun Switch(on: Boolean, w: Float = 46f, h: Float = 28f, knob: Float = 22f, inset: Float = 3f, offColor: Color = white(.15f)) {
    val x = animate(if (on) w - knob - inset else inset, 300, Ease.O13)
    val bg = androidx.compose.animation.animateColorAsState(if (on) C.Gold else offColor, tw(300), label = "sw").value
    Box(Modifier.size(w.dp, h.dp).clip(RoundedCornerShape((h / 2).dp)).background(bg)) {
        Box(Modifier.offset(x.dp, inset.dp).size(knob.dp).clip(CircleShape).background(C.Ink))
    }
}

/** The step bars at the top of Launch and Live: gold up to the current step. */
@Composable
fun StepBars(count: Int, current: Int, modifier: Modifier = Modifier, height: Float = 4f, gap: Float = 6f) {
    androidx.compose.foundation.layout.Row(modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(gap.dp)) {
        repeat(count) { i ->
            val col = androidx.compose.animation.animateColorAsState(if (i <= current) C.Gold else white(.12f), tw(500), label = "bar").value
            Box(Modifier.weight(1f).height(height.dp).clip(RoundedCornerShape((height / 2).dp)).background(col))
        }
    }
}

/** The tick box of the language lists: [size] square, gold when on, the tick springing in. */
@Composable
fun TickBox(on: Boolean, size: Float = 24f, radius: Float = 8f, tick: Float = 13f) {
    val bg = androidx.compose.animation.animateColorAsState(if (on) C.Gold else white(.1f), tw(300), label = "box").value
    val s = animate(if (on) 1f else 0f, 300, Ease.O14)
    val a = animate(if (on) 1f else 0f, 200)
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(radius.dp)).background(bg), contentAlignment = Alignment.Center) {
        Ico(Icons.Tick, tick, C.OnGold, Modifier.graphicsLayer { scaleX = s; scaleY = s; alpha = a })
    }
}

/** A book spine (Table languages): [color] with its highlight and shadow edges and the gilt band. */
@Composable
fun Spine(color: Color, w: Float, h: Float, lifted: Boolean, band: Boolean = true) {
    val ty = animate(if (lifted) -4f else 0f, 400, Ease.O14)
    val rot = animate(if (lifted) -6f else 0f, 400, Ease.O14)
    Box(
        Modifier.size(w.dp, h.dp).graphicsLayer { translationY = ty * density; rotationZ = rot }
            .clip(corners(3f, 3f, 2f, 2f)).background(color)
            .drawBehind {
                drawRect(white(.15f), size = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height))
                drawRect(black(.25f), topLeft = Offset(size.width - 3.dp.toPx(), 0f), size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height))
                if (band) drawRect(Color(255, 240, 200).copy(alpha = .6f), topLeft = Offset(3.dp.toPx(), 6.dp.toPx()), size = androidx.compose.ui.geometry.Size(size.width - 6.dp.toPx(), 1.5.dp.toPx()))
            }
    )
}

/** Fills the parent with a semantic label for screen readers, for decorative groups that still need a name. */
fun Modifier.label(text: String) = semantics { contentDescription = text }

/** A clickable that reads as a [Role.Checkbox] / [Role.RadioButton] / [Role.Switch] to screen readers. */
fun Modifier.choice(onClick: () -> Unit, role: Role) = press(onClick, role = role)

/** `width: 100%` shorthand used everywhere on the boards. */
val Full = Modifier.fillMaxWidth()

@Composable
fun Gap(h: Float) = Box(Modifier.height(h.dp))

@Composable
fun HGap(w: Float) = Box(Modifier.width(w.dp))

fun Modifier.pad(l: Float = 0f, t: Float = 0f, r: Float = 0f, b: Float = 0f) = padding(l.dp, t.dp, r.dp, b.dp)

/**
 * The TV board's code tile (180 px, cream, 14 px in, dark rounded modules, gold glow) holding a real QR code of
 * [text]. The board sketches 13 modules with 2 px gaps; a scannable code needs its own module count and no gaps.
 */
@Composable
fun QrTile(text: String, size: Float = 180f) {
    val bits = remember(text) {
        runCatching { com.google.zxing.qrcode.QRCodeWriter().encode(text, com.google.zxing.BarcodeFormat.QR_CODE, 0, 0, mapOf(com.google.zxing.EncodeHintType.MARGIN to 0)) }.getOrNull()
    }
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier.size(size.dp).cssShadow(0f, 0f, 50f, 0f, gold(.25f), shape).cssShadow(0f, 0f, 0f, 6f, gold(.2f), shape)
            .clip(shape).background(c(design.Palette.Ink)).padding(14.dp).drawBehind {
            val m = bits ?: return@drawBehind
            val cell = this.size.width / m.width
            for (y in 0 until m.height) for (x in 0 until m.width) if (m[x, y])
                drawRect(Color(0xFF121829), Offset(x * cell, y * cell), androidx.compose.ui.geometry.Size(cell + .5f, cell + .5f))
        },
    )
}
