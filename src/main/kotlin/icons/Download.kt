package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val downloadCache = mutableMapOf<Color, ImageVector>()

fun downloadIcon(strokeColor: Color): ImageVector {
    return downloadCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "download",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(strokeColor),
                strokeLineWidth = 1.5f
            ) {
                moveTo(7f, 14f)
                curveTo(12f, 18.5f, 12f, 18.5f, 17f, 14f)
                moveTo(12f, 17.5f)
                verticalLineTo(5.65735f)
                moveTo(3f, 12f)
                curveTo(3f, 5f, 5f, 3f, 12f, 3f)
                curveTo(19f, 3f, 21f, 5f, 21f, 12f)
                curveTo(21f, 19f, 19f, 21f, 12f, 21f)
                curveTo(5f, 21f, 3f, 19f, 3f, 12f)
                close()
            }
        }.build()
    }
}

