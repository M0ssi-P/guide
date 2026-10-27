package presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.global.AsyncImageFromFile
import components.global.SoftEase
import ui.theme.Fraunces
import ui.theme.Inter
import java.io.File

private val Stage = Color(0xFF050608)

/**
 * The projected slide, drawn the same way in the control-screen preview, the display cards and the projector windows.
 * Focus style centres the live sentence with the karaoke glow; paragraph style shows its whole paragraph or verse;
 * Pictures fill or fit the screen with a slow zoom. [offset] 1 shows what comes next (a stage monitor).
 * Sizes follow the design's 860px-wide preview and scale with the available width.
 */
@Composable
fun DeckSlide(accent: Color, lit: Color, modifier: Modifier = Modifier, offset: Int = 0) {
    val deck = ProjectionDeck
    val idx = (deck.index + offset).coerceIn(0, (deck.items.size - 1).coerceAtLeast(0))
    val progress = if (offset == 0) deck.progress else 0f
    BoxWithConstraints(
        modifier.background(Brush.radialGradient(listOf(Color(0xFF1A1610), Stage), radius = 1400f))
    ) {
        if (deck.blank) {
            Box(Modifier.fillMaxSize().background(Color.Black))
            return@BoxWithConstraints
        }
        val k = maxWidth.value / 860f
        val w = maxWidth
        val h = maxHeight
        val item = deck.items.getOrNull(idx)
        when {
            deck.source == DeckSource.IMAGES -> item?.image?.let { PictureSlide(it, deck.fill) }
            deck.effectiveStyle == DeckStyle.FOCUS -> {
                FocusReader(deck.items, idx, progress, lit, Modifier.fillMaxSize().padding(horizontal = w * 0.1f), scale = k)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Stage, 0.28f to Color.Transparent, 0.72f to Color.Transparent, 1f to Stage)))
            }
            else -> {
                val shown by animateFloatAsState(progress, tween(if (progress == 0f) 0 else 400, easing = LinearEasing))
                AnimatedContent(
                    targetState = item?.number,
                    transitionSpec = { (slideInVertically(tween(700, easing = SoftEase)) { (26 * k).toInt() } + fadeIn(tween(700))) togetherWith fadeOut(tween(250)) },
                    modifier = Modifier.fillMaxSize()
                ) { _ ->
                    val songs = deck.source == DeckSource.SONGS
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = w * 0.09f, vertical = h * 0.08f),
                        verticalArrangement = Arrangement.spacedBy((14 * k).dp, Alignment.CenterVertically),
                        horizontalAlignment = if (songs) Alignment.CenterHorizontally else Alignment.Start
                    ) {
                        Text(item?.label?.uppercase() ?: "", color = accent, fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = (12 * k).sp, letterSpacing = (1.4 * k).sp)
                        Text(
                            if (songs) AnnotatedString(item?.text ?: "")
                            else paragraphGlow(deck.items, idx, if (offset == 0) shown else 0f, lit, Color(0xFFF4EEDF)),
                            color = Color(0xFFF4EEDF), fontFamily = Fraunces, fontSize = (30 * k).sp, lineHeight = (42 * k).sp,
                            textAlign = if (songs) TextAlign.Center else TextAlign.Start
                        )
                    }
                }
            }
        }
        if (deck.source != DeckSource.IMAGES) Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = w * 0.09f, end = w * 0.09f, bottom = h * 0.05f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(deck.footer, color = Color(0xFFA3ABBB), fontSize = (11 * k).sp, letterSpacing = (0.5 * k).sp)
            Text(item?.label ?: "", color = Color(0xFFA3ABBB), fontSize = (11 * k).sp, letterSpacing = (0.5 * k).sp)
        }
    }
}

/** A picture that fades in and zooms very slowly (1 → 1.06 over 12s), as in the design. */
@Composable
fun PictureSlide(path: String, fill: Boolean, modifier: Modifier = Modifier) {
    AnimatedContent(path, modifier.fillMaxSize(), transitionSpec = { fadeIn(tween(900)) togetherWith fadeOut(tween(600)) }) { p ->
        val zoom = remember(p) { Animatable(1f) }
        LaunchedEffect(p) { zoom.animateTo(1.06f, tween(12000, easing = LinearEasing)) }
        Box(Modifier.fillMaxSize().background(Color.Black).graphicsLayer { scaleX = zoom.value; scaleY = zoom.value }) {
            AsyncImageFromFile(File(p), false, if (fill) ContentScale.Crop else ContentScale.Fit)
        }
    }
}

/** A held slide: text that no longer follows the deck (a display set to "Pinned"). */
@Composable
fun PinnedSlide(item: DeckItem, footer: String, accent: Color, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.background(Brush.radialGradient(listOf(Color(0xFF1A1610), Stage), radius = 1400f))) {
        val k = maxWidth.value / 860f
        if (item.image != null) { PictureSlide(item.image, true); return@BoxWithConstraints }
        Column(
            Modifier.fillMaxSize().padding(horizontal = maxWidth * 0.09f, vertical = maxHeight * 0.08f),
            verticalArrangement = Arrangement.spacedBy((14 * k).dp, Alignment.CenterVertically)
        ) {
            Text(item.label.uppercase(), color = accent, fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = (12 * k).sp, letterSpacing = (1.4 * k).sp)
            Text(item.text, color = Color(0xFFF4EEDF), fontFamily = Fraunces, fontSize = (30 * k).sp, lineHeight = (42 * k).sp)
        }
        Text(footer, color = Color(0xFFA3ABBB), fontSize = (11 * k).sp, modifier = Modifier.align(Alignment.BottomStart).padding(start = maxWidth * 0.09f, bottom = maxHeight * 0.05f))
    }
}
