package com.composables

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val soundCache = mutableMapOf<Color, ImageVector>()

fun soundIcon(strokeColor: Color): ImageVector {
    return soundCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "sound",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(strokeColor),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(5.016f, 6.969f)
                arcToRelative(3.047f, 3.047f, 0f, false, false, -3.047f, 3.047f)
                verticalLineToRelative(3.968f)
                arcToRelative(3.047f, 3.047f, 0f, false, false, 3.047f, 3.047f)
                horizontalLineToRelative(1.12f)
                lineToRelative(2.12f, 2.204f)
                curveToRelative(1.426f, 1.483f, 3.931f, 0.473f, 3.931f, -1.584f)
                verticalLineTo(6.673f)
                curveToRelative(0f, -1.991f, -2.368f, -3.03f, -3.833f, -1.681f)
                lineTo(6.207f, 6.969f)
                horizontalLineTo(5.016f)
                close()
                moveToRelative(5.14f, 10.682f)
                curveToRelative(0f, 0.229f, -0.278f, 0.34f, -0.437f, 0.176f)
                lineTo(7.3f, 15.311f)
                arcTo(1.016f, 1.016f, 0f, false, false, 6.569f, 15f)
                horizontalLineTo(5.016f)
                curveTo(4.455f, 15f, 4f, 14.545f, 4f, 13.984f)
                verticalLineToRelative(-3.968f)
                curveTo(4f, 9.455f, 4.455f, 9f, 5.016f, 9f)
                horizontalLineToRelative(1.588f)
                curveToRelative(0.255f, 0f, 0.5f, -0.096f, 0.688f, -0.268f)
                lineTo(9.73f, 6.486f)
                arcToRelative(0.254f, 0.254f, 0f, false, true, 0.426f, 0.187f)
                verticalLineTo(17.65f)
                close()
            }
            path(
                fill = SolidColor(strokeColor)
            ) {
                moveTo(17.26f, 17.484f)
                curveToRelative(0.275f, 0.48f, 0.893f, 0.65f, 1.33f, 0.31f)
                arcToRelative(7.11f, 7.11f, 0f, false, false, 0.02f, -11.197f)
                curveToRelative(-0.435f, -0.342f, -1.053f, -0.174f, -1.33f, 0.305f)
                curveToRelative(-0.278f, 0.479f, -0.106f, 1.085f, 0.31f, 1.45f)
                arcToRelative(5.104f, 5.104f, 0f, false, true, -0.015f, 7.683f)
                curveToRelative(-0.417f, 0.364f, -0.59f, 0.97f, -0.315f, 1.45f)
                close()
            }
            path(
                fill = SolidColor(strokeColor)
            ) {
                moveTo(14.135f, 15.121f)
                curveToRelative(0.165f, 0.518f, 0.728f, 0.814f, 1.191f, 0.53f)
                arcToRelative(4.064f, 4.064f, 0f, false, false, 0.03f, -6.91f)
                curveToRelative(-0.462f, -0.287f, -1.028f, 0.004f, -1.197f, 0.52f)
                curveToRelative(-0.168f, 0.518f, 0.141f, 1.063f, 0.526f, 1.448f)
                arcToRelative(2.094f, 2.094f, 0f, false, true, -0.013f, 2.97f)
                curveToRelative(-0.387f, 0.381f, -0.702f, 0.924f, -0.537f, 1.442f)
                close()
            }
        }.build()
    }
}

