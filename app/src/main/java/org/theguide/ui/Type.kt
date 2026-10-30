package org.theguide.ui

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.theguide.R
import java.util.concurrent.ConcurrentHashMap

/** The boards' two faces: Fraunces (serif, with optical sizes) and Inter. */
enum class Face { Serif, Sans }

/**
 * Variable-font families. Browsers set Fraunces' and Inter's optical size ("opsz") to the font size in px
 * (font-optical-sizing: auto), so each size gets its own instance, as on the boards.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
object Fonts {
    private val cache = ConcurrentHashMap<String, FontFamily>()

    fun family(face: Face, weight: Int, italic: Boolean, size: Float): FontFamily {
        val opsz = if (face == Face.Serif) size.coerceIn(9f, 144f) else size.coerceIn(14f, 32f)
        val key = "$face/$weight/$italic/${"%.1f".format(opsz)}"
        return cache.getOrPut(key) {
            val res = when {
                face == Face.Sans -> R.font.inter
                italic -> R.font.fraunces_italic
                else -> R.font.fraunces
            }
            FontFamily(
                Font(
                    res, FontWeight(weight), if (italic) FontStyle.Italic else FontStyle.Normal,
                    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.Setting("opsz", opsz)),
                )
            )
        }
    }
}

/**
 * A text style in board units. [lineHeight]: a multiplier below 4 (CSS `line-height: 1.28`), otherwise px (`22px`);
 * null keeps the font's normal spacing. [tracking] is CSS letter-spacing in px.
 */
fun style(
    size: Float,
    face: Face = Face.Sans,
    weight: Int = 400,
    italic: Boolean = false,
    color: Color = Color.Unspecified,
    lineHeight: Float? = null,
    tracking: Float = 0f,
    align: TextAlign = TextAlign.Unspecified,
    tabular: Boolean = false,
): TextStyle = TextStyle(
    fontFamily = Fonts.family(face, weight, italic, size),
    fontWeight = FontWeight(weight),
    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    fontSize = size.sp,
    color = color,
    letterSpacing = if (tracking == 0f) TextUnit.Unspecified else (tracking / size).em,
    lineHeight = when { lineHeight == null -> TextUnit.Unspecified; lineHeight < 4f -> lineHeight.em; else -> lineHeight.sp },
    // CSS puts half the leading above and half below every line, the first and last included
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    textAlign = align,
    fontFeatureSettings = if (tabular) "tnum" else null,
)

/** Text in board units: the one text call every screen uses. */
@Composable
fun T(
    text: String,
    size: Float,
    color: Color,
    modifier: Modifier = Modifier,
    face: Face = Face.Sans,
    weight: Int = 400,
    italic: Boolean = false,
    lineHeight: Float? = null,
    tracking: Float = 0f,
    align: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    ellipsis: Boolean = false,
    tabular: Boolean = false,
) = BasicText(
    text, modifier,
    style = style(size, face, weight, italic, color, lineHeight, tracking, align, tabular),
    maxLines = maxLines,
    overflow = if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip,
    softWrap = maxLines != 1,
)

/** Serif shorthand (Fraunces). */
@Composable
fun S(
    text: String, size: Float, color: Color, modifier: Modifier = Modifier, weight: Int = 400, italic: Boolean = false,
    lineHeight: Float? = null, tracking: Float = 0f, align: TextAlign = TextAlign.Unspecified, maxLines: Int = Int.MAX_VALUE, ellipsis: Boolean = false,
) = T(text, size, color, modifier, Face.Serif, weight, italic, lineHeight, tracking, align, maxLines, ellipsis)
