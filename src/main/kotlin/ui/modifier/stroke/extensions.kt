package ui.modifier.stroke

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter

enum class BorderSide {
    Top, Bottom, Left, Right
}

val InsetX = setOf(BorderSide.Left, BorderSide.Right)
val InsetY = setOf(BorderSide.Top, BorderSide.Bottom)

fun Modifier.newBorder(
    brush: Brush,
    width: Dp = 1.dp,
    sides: Set<BorderSide> = setOf(BorderSide.Top, BorderSide.Bottom, BorderSide.Left, BorderSide.Right),
    shape: Shape = RectangleShape
): Modifier = this.then(Modifier.drawWithContent {
    drawContent()

    val stroke = width.toPx()
    val outline = shape.createOutline(size, layoutDirection, this)

    if(sides.containsAll(BorderSide.entries)) {
        drawOutline(
            outline = outline,
            brush = brush,
            style = Stroke(width = stroke)
        )
    } else {
        sides.forEach { side ->
            when (side) {
                BorderSide.Top -> drawLine(
                    brush = brush,
                    start = Offset(0f, stroke / 2),
                    end = Offset(size.width, stroke / 2),
                    strokeWidth = stroke
                )

                BorderSide.Bottom -> drawLine(
                    brush = brush,
                    start = Offset(0f, size.height - stroke / 2),
                    end = Offset(size.width, size.height - stroke / 2),
                    strokeWidth = stroke
                )

                BorderSide.Left -> drawLine(
                    brush = brush,
                    start = Offset(stroke / 2, 0f),
                    end = Offset(stroke / 2, size.height),
                    strokeWidth = stroke
                )

                BorderSide.Right -> drawLine(
                    brush = brush,
                    start = Offset(size.width - stroke / 2, 0f),
                    end = Offset(size.width - stroke / 2, size.height),
                    strokeWidth = stroke
                )
            }
        }
    }
})

fun Modifier.newBorder(
    color: Color,
    width: Dp,
    sides: Set<BorderSide> = setOf(BorderSide.Bottom),
    shape: Shape = RectangleShape
): Modifier = newBorder(
    brush = SolidColor(color),
    width = width,
    sides = sides,
    shape = shape
)