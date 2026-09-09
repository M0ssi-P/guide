package icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val filledFireIcon = mutableMapOf<Color, ImageVector>()

fun FilledFireIcon(strokeColor: Color): ImageVector {
    return filledFireIcon.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "Fire",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(strokeColor),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(16.7818f, 20.0399f)
                curveTo(16.4938f, 19.7049f, 15.9888f, 19.2529f, 15.5838f, 18.8899f)
                curveTo(15.3688f, 18.6979f, 15.1728f, 18.5219f, 15.0258f, 18.3809f)
                curveTo(13.7838f, 17.1879f, 13.4168f, 15.7039f, 13.9278f, 13.9509f)
                curveTo(10.8918f, 15.9809f, 11.6878f, 18.8339f, 11.7288f, 18.9729f)
                curveTo(11.8458f, 19.3689f, 11.6228f, 19.7869f, 11.2278f, 19.9059f)
                curveTo(10.8308f, 20.0249f, 10.4168f, 19.8059f, 10.2948f, 19.4119f)
                curveTo(10.2778f, 19.3589f, 8.73579f, 14.0909f, 15.1978f, 11.6389f)
                curveTo(15.4928f, 11.5269f, 15.8318f, 11.6139f, 16.0368f, 11.8559f)
                curveTo(16.2418f, 12.0999f, 16.2708f, 12.4459f, 16.1098f, 12.7199f)
                curveTo(14.5538f, 15.3699f, 15.3398f, 16.6019f, 16.0648f, 17.2989f)
                curveTo(16.2018f, 17.4309f, 16.3848f, 17.5939f, 16.5838f, 17.7719f)
                curveTo(17.0408f, 18.1809f, 17.5568f, 18.6439f, 17.9098f, 19.0509f)
                curveTo(18.9668f, 17.8139f, 19.6008f, 16.2129f, 19.5738f, 14.4749f)
                curveTo(19.5738f, 14.4549f, 19.5738f, 14.4189f, 19.5718f, 14.3839f)
                curveTo(19.6058f, 11.8099f, 18.7448f, 9.28993f, 17.1298f, 7.26893f)
                curveTo(15.2978f, 5.19393f, 12.9808f, 3.55793f, 10.4278f, 2.53593f)
                curveTo(10.2408f, 2.45993f, 10.0228f, 2.50693f, 9.88179f, 2.65393f)
                curveTo(9.73979f, 2.80093f, 9.70179f, 3.01893f, 9.78579f, 3.20493f)
                curveTo(9.78579f, 3.20493f, 10.2928f, 4.33293f, 10.3738f, 4.52793f)
                curveTo(10.9378f, 6.00793f, 10.8128f, 7.64593f, 10.0408f, 9.00793f)
                curveTo(9.90979f, 9.21993f, 9.73979f, 9.47193f, 9.55279f, 9.74893f)
                curveTo(9.07379f, 10.4579f, 8.52179f, 11.2729f, 8.16279f, 12.0719f)
                curveTo(7.83779f, 11.0039f, 7.47779f, 9.43293f, 7.40679f, 8.60793f)
                curveTo(7.39079f, 8.40793f, 7.25479f, 8.23693f, 7.06379f, 8.17493f)
                curveTo(6.87279f, 8.11193f, 6.66279f, 8.17093f, 6.53179f, 8.32293f)
                curveTo(5.08979f, 9.98593f, 4.34379f, 12.1119f, 4.43179f, 14.3159f)
                curveTo(4.54279f, 16.5319f, 5.61979f, 18.5839f, 7.40179f, 19.9579f)
                curveTo(8.70979f, 20.8869f, 10.2558f, 21.4059f, 11.8338f, 21.4569f)
                curveTo(12.1308f, 21.4889f, 12.4288f, 21.4999f, 12.7288f, 21.4999f)
                horizontalLineTo(12.7378f)
                curveTo(14.2768f, 21.4739f, 15.6878f, 20.9409f, 16.8268f, 20.0729f)
                curveTo(16.8128f, 20.0599f, 16.7948f, 20.0549f, 16.7818f, 20.0399f)
                close()
            }
        }.build()
    }
}