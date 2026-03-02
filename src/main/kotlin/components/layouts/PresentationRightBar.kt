package components.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.composables.play
import components.global.Button
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.skiko.Cursor
import presentation.WindowPresentationPreview
import ui.theme.LocalTheme

val aspectRatio = 16f / 9f

@Composable
fun PresentationRightBar() {
    val theme = LocalTheme.current

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(theme.colors.gray2nd)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    modifier = Modifier.background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF109DF5), Color(0xFF1B55F7))
                        ),
                        shape = RoundedCornerShape(21.dp)
                    ).size(20.dp)
                ) {
                    Icon(
                        imageVector = play(theme.colors.light),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(8.dp)
                    )
                }
                Text("Preview", style = theme.typography.body, color = theme.colors.deeming)
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {}
                    .aspectRatio(aspectRatio)
                    .clip(RoundedCornerShape(4.dp))
                    .background(theme.colors.gray2nd)
            ) {
                val density = LocalDensity.current

                // original content size
                val originalWidth = 1280.dp
                val originalHeight = 720.dp

                val scale = with(density) {
                    maxWidth.toPx() / originalWidth.toPx()
                }

                // Centered scaled content
                Box(
                    modifier = Modifier
                        .requiredSize(originalWidth, originalHeight)
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)

                            layout((placeable.width * scale).toInt(), (placeable.height * scale).toInt()) {
                                placeable.placeRelativeWithLayer(0, 0) {
                                    scaleX = scale
                                    scaleY = scale
                                    transformOrigin = TransformOrigin(0f, 0f)
                                }
                            }
                        },
                    contentAlignment = Alignment.TopStart
                ) {
                    WindowPresentationPreview()
                }
            }
        }
    }
}