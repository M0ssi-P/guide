package presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
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
import components.global.HoverScaleButton
import org.jetbrains.jewel.ui.component.Icon
import ui.theme.Inter
import ui.theme.LocalTheme

@Composable
fun WindowPresentationPreview() {
    val theme = LocalTheme.current;
    val windowController = LocalWindowController.current
    val model = windowController.model.collectAsState().value
    val currentSong = model.currentSong.collectAsState()
    val currentLine = model.currentLine.collectAsState()

    CompositionLocalProvider(
        LocalContentColor provides theme.colors.light,
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(theme.colors.night)
        ) {
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
                                val shouldWindowMinimize = windowController.currentScreen == 0;

                                windowController.toFullScreen(if(shouldWindowMinimize) {
                                    !windowController.fullScreen
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
                        color = theme.colors.light
                    )
                    Row {
                        HoverScaleButton(
                            onClick = {
                                windowController.closePresentation()
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
                    Text(
                        (currentLine.value?.lines ?: emptyList()).joinToString("\n"),
                        style = TextStyle(
                            fontFamily = Inter,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = theme.colors.light,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp, vertical = 30.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Number: #${currentSong.value!!.songNumber}",
                        style = theme.typography.h2,
                        color = theme.colors.light
                    )
                }
            }
        }
    }
}