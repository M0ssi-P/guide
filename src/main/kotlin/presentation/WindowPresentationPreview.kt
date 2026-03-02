package presentation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.jewel.ui.component.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.closeIcon
import com.composables.hamburger
import com.composables.maximize
import components.global.AsyncImageFromFile
import components.global.HoverScaleButton
import kotlinx.coroutines.delay
import mvvm.PostsViewModal
import mvvm.SongBookViewModal
import org.jetbrains.jewel.ui.component.Icon
import ui.theme.Inter
import ui.theme.LocalTheme
import java.io.File


@Composable
fun WindowPresentationPreview(key: Int? = null) {
    val theme = LocalTheme.current;
    val windowController = LocalWindowController.current
    val model = if (key != null) {
        windowController.availableProjection[key]!!.viewModel
    } else {
        windowController.model.collectAsState().value
    }

    var visible by remember { mutableStateOf(true) }

    CompositionLocalProvider(
        LocalContentColor provides theme.colors.light,
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
                .background(Color.Black)
        ) {
            val vw = maxWidth / 100

            when (val vm = model) {
                is SongBookViewModal -> {
                    val currentSong = model.currentSong.collectAsState()
                    val currentLine = model.currentLine.collectAsState()

                    LaunchedEffect(currentLine.value) {
                        visible = false
                        delay(50)
                        visible = true
                    }

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 30.dp, vertical = 30.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HoverScaleButton {
                                    Icon(
                                        imageVector = hamburger(theme.colors.light),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                HoverScaleButton(
                                    onClick = {
                                        if(key == null) return@HoverScaleButton
                                        val shouldWindowMinimize = key == 0;

                                        windowController.toFullScreen(key, if(shouldWindowMinimize) {
                                            !windowController.availableProjection[key]!!.fullScreen
                                        } else true)
                                    }
                                ){
                                    Icon(
                                        imageVector = maximize(theme.colors.light),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Text(
                                currentSong.value?.titles?.first()?.title ?: "Title",
                                style = theme.typography.h2,
                                fontSize = (vw * 2).value.sp,
                                color = theme.colors.light
                            )
                            Row {
                                HoverScaleButton(
                                    onClick = {
                                        if(key == null) return@HoverScaleButton
                                        windowController.removeScreen(key)
                                    }
                                ) {
                                    Icon(
                                        imageVector = closeIcon(theme.colors.light),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            var fontSize by remember { mutableStateOf((vw * 5).value.sp) }

                            androidx.compose.animation.AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn(animationSpec = tween(500)),
                                exit = fadeOut(animationSpec = tween(300))
                            ) {
                                Text(
                                    (currentLine.value?.lines ?: emptyList()).joinToString("\n"),
                                    style = TextStyle(
                                        fontFamily = Inter,
                                        fontSize = fontSize,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = theme.colors.light,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    onTextLayout = { textLayoutResult ->
                                        if (textLayoutResult.hasVisualOverflow) {
                                            if (fontSize > 10.sp) {
                                                fontSize *= 0.9f
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 30.dp, vertical = 30.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Number: #${currentSong.value?.songNumber}",
                                style = theme.typography.h2,
                                color = theme.colors.light,
                                fontSize = (vw * 2).value.sp
                            )
                        }
                    }
                }
                is PostsViewModal -> {
                    val currentPost = vm.currentPost
                    if (currentPost != null) {
                        val file = remember(currentPost.location) { File(currentPost.location) }

                        AsyncImageFromFile(file)
                    }
                }
                else -> {}
            }
        }
    }
}