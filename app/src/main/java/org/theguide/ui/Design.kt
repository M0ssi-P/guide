package org.theguide.ui

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import design.Bezier
import design.Palette
import design.ReadingLook
import design.ReadingTheme
import design.Tone
import kotlin.math.sqrt
import kotlin.random.Random

// ---------- colours ----------

fun c(argb: Long) = Color(argb)
/** rgba(255,255,255,a) */
fun white(a: Float) = Color.White.copy(alpha = a)
/** rgba(0,0,0,a) */
fun black(a: Float) = Color.Black.copy(alpha = a)
/** rgba(243,201,105,a), the lamp gold */
fun gold(a: Float) = Color(0xFFF3C969).copy(alpha = a)
fun rgb(rgb: Long, a: Float = 1f) = Color(0xFF000000 or rgb).copy(alpha = a)

object C {
    val Backdrop = c(Palette.Backdrop); val Night = c(Palette.Night); val Sheet = c(Palette.Sheet); val Card = c(Palette.Card)
    val Ink = c(Palette.Ink); val Title = c(Palette.Title); val Text = c(Palette.Text); val Soft = c(Palette.Soft)
    val Sub = c(Palette.Sub); val Meta = c(Palette.Meta); val Faint = c(Palette.Faint); val Brass = c(Palette.Brass)
    val Gold = c(Palette.Gold); val OnGold = c(Palette.OnGold); val Amber = c(Palette.Amber); val Flame = c(Palette.Flame)
    val Honey = c(Palette.Honey); val English = c(Palette.English); val EnglishDeep = c(Palette.EnglishDeep)
    val Coral = c(Palette.Coral); val CoralSoft = c(Palette.CoralSoft)
    val Paper = c(Palette.Paper); val PaperInk = c(Palette.PaperInk); val PaperBody = c(Palette.PaperBody)
    val PaperMuted = c(Palette.PaperMuted); val PaperSign = c(Palette.PaperSign); val Wax = c(Palette.Wax)
}

// ---------- reading looks (theme + tone) ----------

/** The boards' `tokens(s)` for reading screens, as Compose colours. */
@Immutable
class Look(val theme: ReadingTheme, val tone: Tone) {
    private val l = ReadingLook(theme, tone)
    val light = l.light
    val bg = c(theme.bg); val ink = c(theme.ink); val tx = c(theme.ink); val sub = c(theme.sub); val sheet = c(theme.sheet); val fold = c(theme.fold)
    val acc = c(l.accent)
    val glow = rgb(tone.rgb, l.glowAlpha)
    val line = if (light) black(l.lineAlpha) else white(l.lineAlpha)
    val chip = if (light) black(l.chipAlpha) else white(l.chipAlpha)
    val hair = if (light) black(l.hairAlpha) else white(l.hairAlpha)
    val litStart = rgb(tone.rgb, l.litStartAlpha)
    val litEnd = rgb(tone.rgb, l.litEndAlpha)
    val pill = if (light) Color.White else white(l.pillNightAlpha)
    val deck = c(l.deck)
    val pageBg = c(l.pageBg); val pageEdge = c(l.pageEdge); val pageEdge2 = c(l.pageEdge2); val curl = c(l.curl)
    val selected = if (light) black(l.selectedAlpha) else white(l.selectedAlpha)
    val toneSelected = if (light) black(l.toneSelectedAlpha) else white(l.toneSelectedAlpha)
    val track = if (light) black(l.trackAlpha) else white(l.trackAlpha)
    /** Text on the accent (chapter chips): white on paper, dark on night. */
    val onAcc = if (light) Color.White else C.OnGold
}

// ---------- easing ----------

val Bezier.easing: Easing get() = CubicBezierEasing(x1, y1, x2, y2)

// ---------- CSS box-shadow ----------

/**
 * CSS `box-shadow: x y blur spread color` behind [shape]. CSS blur is twice the Gaussian sigma; Android's blur radius
 * is about sigma / 0.57735.
 */
fun Modifier.cssShadow(x: Float, y: Float, blur: Float, spread: Float, color: Color, shape: Shape = RectangleShape): Modifier = drawWithCache {
    val shadow = shadowBitmap(size, this, layoutDirection, blur, spread, color, shape)
    onDrawBehind { shadow?.let { (img, pad) -> drawImage(img, Offset(x * density - pad, y * density - pad)) } }
}

/**
 * The blurred shape rendered once on a software canvas (the hardware canvas ignores blur mask filters), with the
 * padding it needs around the box.
 */
private fun shadowBitmap(size: Size, density: androidx.compose.ui.unit.Density, layoutDirection: androidx.compose.ui.unit.LayoutDirection, blur: Float, spread: Float, color: Color, shape: Shape): Pair<ImageBitmap, Float>? {
    val d = density.density
    val w = size.width + 2 * spread * d
    val h = size.height + 2 * spread * d
    if (w <= 0 || h <= 0) return null
    val sigma = blur * d / 2f
    val pad = sigma * 3 + maxOf(spread, 0f) * d + 2f
    val bw = (size.width + 2 * pad).toInt().coerceAtLeast(1); val bh = (size.height + 2 * pad).toInt().coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
    val cv = android.graphics.Canvas(bmp)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        if (sigma > 0.5f) maskFilter = BlurMaskFilter((sigma / 0.57735f), BlurMaskFilter.Blur.NORMAL)
    }
    val path = androidx.compose.ui.graphics.Path().apply { addOutline(shape.createOutline(Size(w, h), layoutDirection, density)) }
    cv.translate(pad - spread * d, pad - spread * d)
    cv.drawPath(path.asAndroidPath(), paint)
    return bmp.asImageBitmap() to pad
}

fun DrawScope.drawCssShadow(x: Float, y: Float, blur: Float, spread: Float, color: Color, shape: Shape) {
    shadowBitmap(size, this, layoutDirection, blur, spread, color, shape)?.let { (img, pad) -> drawImage(img, Offset(x * density - pad, y * density - pad)) }
}

/** CSS `box-shadow: inset 0 0 0 w color`: a ring drawn just inside the edge. */
fun Modifier.insetRing(width: Dp, color: Color, shape: Shape) = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    onDrawWithContent {
        drawContent()
        if (color.alpha > 0f) {
            val wpx = width.toPx()
            clipOutline(outline) { drawOutline(outline, color, style = Stroke(wpx * 2)) }
        }
    }
}

private inline fun DrawScope.clipOutline(outline: Outline, block: DrawScope.() -> Unit) {
    val path = androidx.compose.ui.graphics.Path().apply { addOutline(outline) }
    drawContext.canvas.save()
    drawContext.canvas.clipPath(path)
    block()
    drawContext.canvas.restore()
}

/** CSS `box-shadow: inset 0 1px 0 color`: the lit top edge of glass and cards. */
fun Modifier.topHighlight(color: Color, shape: Shape) = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = androidx.compose.ui.graphics.Path().apply { addOutline(outline) }
    onDrawWithContent {
        drawContent()
        drawContext.canvas.save()
        drawContext.canvas.clipPath(path)
        drawContext.canvas.translate(0f, 1.dp.toPx())
        drawOutline(outline, color, style = Stroke(2.dp.toPx()), blendMode = BlendMode.SrcOver)
        drawContext.canvas.restore()
    }
}

// ---------- CSS gradients ----------

/**
 * CSS `radial-gradient(ellipse at cx cy, stops)` over a box of [size]: the default "farthest-corner" ellipse, which
 * keeps the closest-side aspect ratio and reaches the farthest corner.
 */
fun DrawScope.ellipseGradient(cx: Float, cy: Float, vararg stops: Pair<Float, Color>, box: Size = size, origin: Offset = Offset.Zero) {
    val px = box.width * cx; val py = box.height * cy
    val rx0 = minOf(px, box.width - px).coerceAtLeast(1f); val ry0 = minOf(py, box.height - py).coerceAtLeast(1f)
    val dx = maxOf(px, box.width - px); val dy = maxOf(py, box.height - py)
    val k = sqrt((dx / rx0) * (dx / rx0) + (dy / ry0) * (dy / ry0))
    val rx = rx0 * k; val ry = ry0 * k
    val center = Offset(origin.x + px, origin.y + py)
    scale(1f, ry / rx, pivot = center) {
        drawCircle(Brush.radialGradient(*stops, center = center, radius = rx), radius = rx * 2, center = center)
    }
}

/** CSS `radial-gradient(circle [at x y], stops)` with "farthest-corner" sizing. */
fun circleGradient(size: Size, cx: Float = .5f, cy: Float = .5f, vararg stops: Pair<Float, Color>): Brush {
    val px = size.width * cx; val py = size.height * cy
    val r = sqrt(maxOf(px, size.width - px).let { it * it } + maxOf(py, size.height - py).let { it * it })
    return Brush.radialGradient(*stops, center = Offset(px, py), radius = r.coerceAtLeast(1f))
}

/** CSS `radial-gradient(circle, a 0 r, ...)` ring helper: stops in absolute px from the centre. */
fun ringBrush(size: Size, density: Float, vararg stops: Pair<Float, Color>): Brush {
    val r = sqrt(size.width * size.width + size.height * size.height) / 2f
    return Brush.radialGradient(*stops.map { (px, col) -> (px * density / r).coerceIn(0f, 1f) to col }.toTypedArray(), center = Offset(size.width / 2, size.height / 2), radius = r)
}

// ---------- shapes ----------

/** CSS `border-radius: a b c d` (top-left, top-right, bottom-right, bottom-left), in board px. */
fun corners(tl: Float, tr: Float, br: Float, bl: Float) = RoundedCornerShape(tl.dp, tr.dp, br.dp, bl.dp)

// ---------- paper grain ----------

/**
 * The boards' `.grain`: SVG fractal noise (baseFrequency .9, two octaves) as a 160 px tile, drawn over paper with
 * multiply or over night with overlay.
 */
object Grain {
    private var tile: ImageBitmap? = null

    private fun make(): ImageBitmap {
        val n = 160
        val rnd = Random(7)
        val g = Array(n + 1) { FloatArray(n + 1) { rnd.nextFloat() } }
        fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
        fun noise(x: Float, y: Float): Float {
            val xi = x.toInt() % n; val yi = y.toInt() % n; val fx = x - x.toInt(); val fy = y - y.toInt()
            return lerp(lerp(g[yi][xi], g[yi][xi + 1], fx), lerp(g[yi + 1][xi], g[yi + 1][xi + 1], fx), fy)
        }
        val px = IntArray(n * n)
        for (y in 0 until n) for (x in 0 until n) {
            val v = (noise(x * .9f, y * .9f) * .66f + noise(x * 1.8f, y * 1.8f) * .34f)
            // feColorMatrix keeps only alpha = 0.5: grey noise at half strength
            val a = (v * 0.5f * 255).toInt().coerceIn(0, 255)
            val lum = (v * 255).toInt().coerceIn(0, 255)
            px[y * n + x] = (a shl 24) or (lum shl 16) or (lum shl 8) or lum
        }
        return Bitmap.createBitmap(px, n, n, Bitmap.Config.ARGB_8888).asImageBitmap()
    }

    fun brush(): ShaderBrush = ShaderBrush(ImageShader(tile ?: make().also { tile = it }, TileMode.Repeated, TileMode.Repeated))
}

/**
 * `<span class="grain" style="opacity: [opacity]; mix-blend-mode: [multiply|overlay]">` as the box's first child: over
 * the background, under the content. Put it after the background modifier.
 */
fun Modifier.grain(opacity: Float, multiply: Boolean = false, shape: Shape = RectangleShape) = drawWithCache {
    val brush = Grain.brush()
    val outline = shape.createOutline(size, layoutDirection, this)
    onDrawBehind { drawOutline(outline, brush, alpha = opacity, blendMode = if (multiply) BlendMode.Multiply else BlendMode.Overlay) }
}

fun Color.argbInt() = toArgb()
