package icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val volumeFullCache = mutableMapOf<Color, ImageVector>()

fun VolumeFull(strokeColor: Color): ImageVector {
    return volumeFullCache.getOrPut(strokeColor) {
        ImageVector.Builder(
            name = "VolumeFull",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(strokeColor),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(19.018f, 5.15148f)
                curveTo(19.3621f, 4.92084f, 19.828f, 5.01279f, 20.0586f, 5.35686f)
                curveTo(22.7213f, 9.32899f, 22.7293f, 14.5681f, 20.0585f, 18.5495f)
                curveTo(19.8277f, 18.8935f, 19.3618f, 18.9853f, 19.0178f, 18.7546f)
                curveTo(18.6738f, 18.5238f, 18.582f, 18.0579f, 18.8128f, 17.7139f)
                curveTo(21.1438f, 14.239f, 21.1373f, 9.65993f, 18.8126f, 6.19207f)
                curveTo(18.582f, 5.848f, 18.674f, 5.38212f, 19.018f, 5.15148f)
                close()
            }
            path(
                fill = SolidColor(strokeColor),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(16.6265f, 7.56053f)
                curveTo(16.9892f, 7.3604f, 17.4454f, 7.49215f, 17.6456f, 7.8548f)
                curveTo(19.0516f, 10.4027f, 19.0522f, 13.5113f, 17.645f, 16.0524f)
                curveTo(17.4443f, 16.4147f, 16.9879f, 16.5458f, 16.6256f, 16.3451f)
                curveTo(16.2632f, 16.1444f, 16.1321f, 15.688f, 16.3328f, 15.3256f)
                curveTo(17.4892f, 13.2376f, 17.4898f, 10.6771f, 16.3323f, 8.57956f)
                curveTo(16.1321f, 8.2169f, 16.2639f, 7.76067f, 16.6265f, 7.56053f)
                close()
            }
            path(
                fill = SolidColor(strokeColor),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(8.83799f, 5.20604f)
                curveTo(9.79013f, 4.58739f, 11.1159f, 4.01076f, 12.5328f, 4.85041f)
                curveTo(12.5506f, 4.86093f, 12.5679f, 4.87217f, 12.5847f, 4.88411f)
                curveTo(13.321f, 5.40692f, 13.817f, 6.14019f, 14.1214f, 7.26781f)
                curveTo(14.416f, 8.35929f, 14.5405f, 9.8534f, 14.5405f, 11.9529f)
                curveTo(14.5405f, 14.0546f, 14.4098f, 15.5525f, 14.1121f, 16.6458f)
                curveTo(13.8054f, 17.7721f, 13.3106f, 18.506f, 12.5848f, 19.0216f)
                curveTo(12.5681f, 19.0335f, 12.5508f, 19.0448f, 12.533f, 19.0553f)
                curveTo(11.1161f, 19.8955f, 9.79028f, 19.3192f, 8.83799f, 18.7004f)
                curveTo(8.47797f, 18.4665f, 8.12333f, 18.1947f, 7.81327f, 17.9571f)
                curveTo(7.7151f, 17.8819f, 7.62139f, 17.81f, 7.5334f, 17.7439f)
                curveTo(7.129f, 17.4401f, 6.85456f, 17.2647f, 6.65488f, 17.1989f)
                curveTo(6.22743f, 17.0583f, 5.89366f, 17.0183f, 5.51675f, 16.9732f)
                curveTo(5.37842f, 16.9566f, 5.23427f, 16.9394f, 5.07754f, 16.9162f)
                curveTo(4.49312f, 16.8297f, 3.88269f, 16.6641f, 3.22596f, 16.1064f)
                curveTo(2.54215f, 15.5259f, 2.22496f, 14.7663f, 2.07513f, 14.0339f)
                curveTo(1.92928f, 13.321f, 1.93093f, 12.5725f, 1.93227f, 11.9645f)
                lineTo(1.9323f, 11.9545f)
                verticalLineTo(11.9513f)
                lineTo(1.93227f, 11.9413f)
                curveTo(1.93093f, 11.3333f, 1.92928f, 10.5848f, 2.07513f, 9.8719f)
                curveTo(2.22496f, 9.13951f, 2.54216f, 8.37987f, 3.226f, 7.79933f)
                lineTo(3.22625f, 7.79912f)
                curveTo(3.88304f, 7.24204f, 4.49343f, 7.0767f, 5.07754f, 6.99029f)
                curveTo(5.23352f, 6.96722f, 5.37708f, 6.95f, 5.51488f, 6.93347f)
                curveTo(5.89243f, 6.88819f, 6.22673f, 6.84809f, 6.65473f, 6.70698f)
                lineTo(6.65532f, 6.70679f)
                curveTo(6.85481f, 6.6412f, 7.12914f, 6.46597f, 7.53352f, 6.1623f)
                curveTo(7.62171f, 6.09607f, 7.71565f, 6.02409f, 7.81409f, 5.94866f)
                curveTo(8.12394f, 5.71126f, 8.47832f, 5.43973f, 8.83799f, 5.20604f)
                close()
                moveTo(7.12382f, 8.13174f)
                curveTo(6.53934f, 8.32437f, 6.0057f, 8.38756f, 5.60227f, 8.43533f)
                curveTo(5.48984f, 8.44865f, 5.38753f, 8.46076f, 5.29705f, 8.47415f)
                curveTo(4.88262f, 8.53545f, 4.56987f, 8.62645f, 4.19677f, 8.94284f)
                curveTo(3.85153f, 9.23593f, 3.65282f, 9.64402f, 3.54469f, 10.1725f)
                curveTo(3.43365f, 10.7153f, 3.4309f, 11.3152f, 3.4323f, 11.9513f)
                verticalLineTo(11.9545f)
                curveTo(3.4309f, 12.5906f, 3.43365f, 13.1905f, 3.54469f, 13.7333f)
                curveTo(3.65282f, 14.2618f, 3.85153f, 14.6699f, 4.19677f, 14.963f)
                lineTo(4.19682f, 14.963f)
                curveTo(4.57008f, 15.2799f, 4.88284f, 15.3711f, 5.29705f, 15.4323f)
                curveTo(5.38772f, 15.4458f, 5.49033f, 15.4579f, 5.60313f, 15.4712f)
                curveTo(6.00641f, 15.5189f, 6.53999f, 15.582f, 7.12397f, 15.7741f)
                lineTo(7.12426f, 15.7742f)
                curveTo(7.59095f, 15.928f, 8.04957f, 16.2556f, 8.43437f, 16.5447f)
                curveTo(8.5464f, 16.6288f, 8.65543f, 16.7123f, 8.76294f, 16.7947f)
                curveTo(9.0617f, 17.0236f, 9.34867f, 17.2434f, 9.65524f, 17.4426f)
                curveTo(10.4735f, 17.9743f, 11.1056f, 18.143f, 11.7417f, 17.7804f)
                curveTo(12.1192f, 17.5043f, 12.4353f, 17.0943f, 12.6648f, 16.2516f)
                curveTo(12.9084f, 15.357f, 13.0405f, 14.0221f, 13.0405f, 11.9529f)
                curveTo(13.0405f, 9.8815f, 12.9141f, 8.55106f, 12.6732f, 7.65868f)
                curveTo(12.447f, 6.82074f, 12.132f, 6.41025f, 11.7419f, 6.12562f)
                curveTo(11.1059f, 5.76325f, 10.4737f, 5.93208f, 9.65524f, 6.46385f)
                curveTo(9.34892f, 6.66289f, 9.06222f, 6.88246f, 8.76376f, 7.11105f)
                curveTo(8.65596f, 7.19362f, 8.54662f, 7.27736f, 8.43426f, 7.36174f)
                curveTo(8.04943f, 7.65074f, 7.5907f, 7.97824f, 7.12382f, 8.13174f)
                close()
            }
        }.build()
    }
}

