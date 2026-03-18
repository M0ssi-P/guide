package components.global

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.composables.closeIcon
import com.composables.tabIcon
import db.controller.user.IImages
import mvvm.ShareViewModels
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.skiko.Cursor
import ui.theme.LocalTheme
import java.io.File

@Composable
fun PostsGrid(data: List<IImages>) {
    val theme = LocalTheme.current
    val userModal = ShareViewModels.userModal

    LazyVerticalGrid(
        columns = GridCells.Fixed(3), // 3 columns
        modifier = Modifier.widthIn(min = 200.dp, max = 1200.dp)
            .fillMaxSize()
            .padding(horizontal = 30.dp, vertical = 30.dp)
    ) {
        items(data) { image ->
            PopoverAnchored(
                modifier = Modifier
                    .dropShadow(
                        shape = RoundedCornerShape(6.dp),
                        block = {
                            color = theme.colors.border
                            spread = 1f
                            offset = Offset(0f, 0f)
                        }
                    )
                    .dropShadow(
                        shape = RoundedCornerShape(6.dp),
                        block = {
                            color = Color.Black.copy(alpha = 0.12f)
                            alpha = 1f
                            spread = -6f
                            radius = 28f
                            offset = Offset(0f, 14f)
                        }
                    )
                    .clip(RoundedCornerShape(6.dp))
                    .background(theme.colors.popup),
                hoverEnabled = true,
                popup = { state ->
                    ScreensModal(state, image, "")
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(theme.colors.popup)
                ) {
                    AsyncImageFromFile(file = File(image.location))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF071108),
                                    Color(0xFF071108).copy(alpha = 0.25f)
                                )
                            ))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row {
                                Text("Untitled", style = theme.typography.button, color = Color.White)

                                Spacer(Modifier.width(10.dp))

                                Button(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(28.dp))
                                        .background(Color(0xFFFFD23F))
                                        .padding(top = 3.dp, bottom = 3.dp, start = 10.dp, end = 3.dp),
                                    fillMaxSize = false
                                ) {
                                    Text("Text", style = theme.typography.button, color = Color.White)
                                }
                            }

                            Button(
                                modifier = Modifier
                                    .clip(shape = RoundedCornerShape(21.dp))
                                    .background(theme.colors.popup)
                                    .size(24.dp).pointerHoverIcon(
                                        PointerIcon(
                                            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                        )
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(),
                                        onClick = {
                                            userModal.deleteImage(image)
                                        }
                                    )
                            ) {
                                Icon(
                                    imageVector = closeIcon(theme.colors.blue3),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}