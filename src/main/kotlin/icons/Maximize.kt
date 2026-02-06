package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val maximizeCache = mutableMapOf<Color, ImageVector>()

fun maximize(strokeColor: Color): ImageVector {
    return maximizeCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "maximize",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(strokeColor),
                strokeLineWidth = 1.5f
            ) {
                moveTo(2.5f, 10.5f)
                curveTo(2.5f, 2.5f, 2.5f, 2.5f, 10.5f, 2.5f)
                moveTo(2.5f, 13.5f)
                curveTo(2.5f, 21.5f, 2.5f, 21.5f, 10.5f, 21.5f)
                moveTo(21.5f, 13.5f)
                curveTo(21.5f, 21.5f, 21.5f, 21.5f, 13.5f, 21.5f)
                moveTo(21.5f, 10.5f)
                curveTo(21.5f, 2.5f, 21.5f, 2.5f, 13.5f, 2.5f)
            }
        }.build()
    }
}

