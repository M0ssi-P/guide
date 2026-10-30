package org.theguide.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** The boards' inline SVG icons as ImageVectors (24×24 unless said), drawn in one colour with [Ico]. */
object Icons {
    private fun stroke(d: String, width: Float = 1.8f, round: Boolean = true, vw: Float = 24f, vh: Float = 24f) =
        ImageVector.Builder(defaultWidth = vw.dp, defaultHeight = vh.dp, viewportWidth = vw, viewportHeight = vh).addPath(
            addPathNodes(d), stroke = SolidColor(Color.Black), strokeLineWidth = width,
            strokeLineCap = if (round) StrokeCap.Round else StrokeCap.Butt, strokeLineJoin = if (round) StrokeJoin.Round else StrokeJoin.Miter,
        ).build()

    private fun fill(d: String) =
        ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(addPathNodes(d), fill = SolidColor(Color.Black)).build()

    /** Filled and outlined at once (the saved bookmark). */
    private fun both(d: String, width: Float) =
        ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(addPathNodes(d), fill = SolidColor(Color.Black), stroke = SolidColor(Color.Black), strokeLineWidth = width, strokeLineJoin = StrokeJoin.Round)
            .build()

    val Back = stroke("M15 6l-6 6 6 6", 2f)
    val Forward = stroke("M9 6l6 6-6 6", 2f)
    val HostArrow = stroke("M9 5l7 7-7 7", 2f)
    val Down = stroke("M6 9l6 6 6-6", 2.5f, round = false)
    val DownRound = stroke("M6 9l6 6 6-6", 2f)
    val Search = stroke("M18 11a7 7 0 1 1-14 0a7 7 0 1 1 14 0M20 20l-3.5-3.5", 2f)
    val Download = stroke("M12 3v12M7 10l5 5 5-5M5 21h14")
    val DownloadTv = stroke("M12 4v11M7 10l5 5 5-5M5 20h14", 2.2f)
    val Play = fill("M8 5.5v13a1 1 0 0 0 1.5.86l10.4-6.5a1 1 0 0 0 0-1.72L9.5 4.64A1 1 0 0 0 8 5.5z")
    val Pause = fill("M6 5h4v14H6zM14 5h4v14h-4z")
    val Bookmark = stroke("M6 3h12v18l-6-4-6 4z", round = false)
    val BookmarkFilled = both("M6 3h12v18l-6-4-6 4z", 1.8f)
    val Share = stroke("M12 3v13M7 8l5-5 5 5M5 14v5a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-5")
    val ShareSmall = stroke("M12 3v13M7 8l5-5 5 5M5 14v5h14v-5", 2f)
    val Queue = stroke("M4 6h16M4 12h10M4 18h7M17 15l4 3-4 3z")
    val PrevPara = stroke("M18 6L9 12l9 6zM6 6v12")
    val NextPara = stroke("M6 6l9 6-9 6zM18 6v12")
    val Tick = stroke("M5 12l5 5 9-10", 3.5f)
    val TickThin = stroke("M5 12l5 5 9-10", 2.2f)
    val Transfer = stroke("M7 4v13M3 13l4 4 4-4M17 20V7M13 11l4-4 4 4")
    val Plus = stroke("M12 5v14M5 12h14", 2.2f)
    val Close = stroke("M6 6l12 12M18 6L6 18", 2.4f)
    val VolumeLow = stroke("M4 9v6h4l5 4V5L8 9z")
    val VolumeHigh = stroke("M4 9v6h4l5 4V5L8 9zM16 8.5a5 5 0 0 1 0 7M19 6a8.5 8.5 0 0 1 0 12")
    val Resync = stroke("M20 11a8 8 0 0 0-14.6-4.5L4 8M4 4v4h4M4 13a8 8 0 0 0 14.6 4.5L20 16M20 20v-4h-4", 2.2f)
    val ResyncTv = stroke("M20 11a8 8 0 0 0-14.6-4.5L4 8M4 4v4h4M4 13a8 8 0 0 0 14.6 4.5L20 16M20 20v-4h-4")
    val WordsOnly = stroke("M5 6h14M5 10h14M5 14h10M5 18h7")
    val AudioBack = stroke("M4 9v6h4l5 4V5L8 9zM16 8.5a5 5 0 0 1 0 7")
    val Language = stroke("M4 5h8M8 3v2M6 5c0 4 3 7 6 8M10 5c-1 4-4 7-6 8M13 21l4-9 4 9M14.5 18h5")
    val Settings = stroke("M4 7h10M18 7h2M4 17h4M12 17h8M14 4v6M8 14v6")
    val Leave = stroke("M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3M10 17l-5-5 5-5M5 12h11")
    val Cloud = stroke("M6.5 16h11a4 4 0 0 0 .6-7.95A6 6 0 0 0 6.6 7.1 4.5 4.5 0 0 0 6.5 16z", 1.2f, vh = 18f)
    val CloudSlash = stroke("M4 2l16 15", 1.2f, vh = 18f)

    // phone tab bar (1.8) and TV rail (1.7)
    val TabHome = stroke("M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z")
    val TabTable = stroke("M4 5h7a3 3 0 0 1 3 3v12a2 2 0 0 0-2-2H4zM20 5h-4a3 3 0 0 0-2 1")
    val TabBible = stroke("M12 3v18M7 8h10M5 3h14v18H5z")
    val TabSongs = stroke("M9 18V5l11-2v13M9 18a3 3 0 1 1-3-3 3 3 0 0 1 3 3zM20 16a3 3 0 1 1-3-3 3 3 0 0 1 3 3z")
    val TabLibrary = stroke("M4 4h6v16H4zM14 4l5 1-3 15-5-1z")
    val RailHome = stroke("M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z", 1.7f)
    val RailTable = stroke("M4 5h16M6 5v14M18 5v14M3 19h18", 1.7f)
    val RailBible = stroke("M5 4h11a3 3 0 0 1 3 3v13H8a3 3 0 0 1-3-3zM12 8v6M9 11h6", 1.7f)
    val RailLive = stroke("M12 12m-2 0a2 2 0 1 0 4 0a2 2 0 1 0-4 0M7.8 7.8a6 6 0 0 0 0 8.4M16.2 7.8a6 6 0 0 1 0 8.4M5 5a10 10 0 0 0 0 14M19 5a10 10 0 0 1 0 14", 1.7f)
    val RailSongs = stroke("M9 18V5l11-2v13M9 18a3 3 0 1 1-6 0 3 3 0 0 1 6 0zM20 16a3 3 0 1 1-6 0 3 3 0 0 1 6 0z", 1.7f)
    val RailLibrary = stroke("M4 4h4v16H4zM10 4h4v16h-4zM16 5l3.5-1 3 15.5-3.5 1z", 1.7f)
}

/** An icon at [size] board px in one [color] (the SVG's currentColor / fill / stroke). */
@Composable
fun Ico(icon: ImageVector, size: Float, color: Color, modifier: Modifier = Modifier, height: Float = size * icon.defaultHeight.value / icon.defaultWidth.value) =
    Image(icon, null, modifier.size(size.dp, height.dp), colorFilter = ColorFilter.tint(color))
