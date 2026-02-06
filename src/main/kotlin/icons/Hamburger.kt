package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val hamburgerCache = mutableMapOf<Color, ImageVector>()

fun hamburger(strokeColor: Color): ImageVector {
    return hamburgerCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "hamburger",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(strokeColor),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(3f, 20.5f)
                horizontalLineTo(21f)
                moveTo(3f, 12f)
                horizontalLineTo(21f)
                moveTo(3f, 3.5f)
                horizontalLineTo(21f)
            }
        }.build()
    }
}

