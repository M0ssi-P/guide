package components.global

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import db.controller.user.IImages
import models.ISongDetails
import models.ISongLyricLine
import mvvm.PostsViewModal
import mvvm.SongBookViewModal
import org.jetbrains.jewel.ui.component.Text
import presentation.LocalWindowController
import presentation.ProjectionEntry
import presentation.rememberScreens
import ui.theme.LocalTheme

@Composable
fun ScreensModal(state: MutableState<Boolean>, song: Any, line: Any) {
    val theme = LocalTheme.current
    val windowController = LocalWindowController.current
    val screens = rememberScreens()

    Column(
        modifier = Modifier
            .width(205.dp)
            .padding(10.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {

            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        ) {
            Text(
                text = "Select Screen...",
                color = theme.colors.primaryText
            )
        }
        screens.forEachIndexed { index, screen ->
            val bounds = screen.defaultConfiguration.bounds
            SwitchThemeOption("Display $index - ${bounds.width}x${bounds.height}", number = index + 1, false, onClick = {
                val hadAlreadyExisted = windowController.containsKey(index)
                val model = windowController.getOrCreate(index) {
                    ProjectionEntry(
                        device = screen,
                        bounds = bounds,
                        fullScreen = true,
                        viewModel = when(song) {
                            is ISongDetails -> {
                                windowController.model.value
                            }
                            is IImages -> {
                                PostsViewModal()
                            }
                            else -> {
                                windowController.model.value
                            }
                        }
                    )
                }

                val diredVM =  when(song) {
                    is ISongDetails -> {
                        SongBookViewModal::class
                    }
                    is IImages -> {
                        PostsViewModal::class
                    }
                    else -> {
                        SongBookViewModal::class
                    }
                }

                if(hadAlreadyExisted && !diredVM.isInstance(model.viewModel)) {
                    val updatedVM = model.copy(
                        viewModel = when(song) {
                            is ISongDetails -> {
                                windowController.model.value
                            }
                            is IImages -> {
                                PostsViewModal()
                            }
                            else -> {
                                windowController.model.value
                            }
                        }
                    )

                    windowController.updateProjection(index, updatedVM)
                }

                when (val activeVM = windowController.availableProjection[index]!!.viewModel) {
                    is SongBookViewModal -> {
                        activeVM.setCurrentSong(song as ISongDetails)
                        activeVM.setCurrentLine(line as ISongLyricLine)
                    }
                    is PostsViewModal -> {
                        activeVM.setPost(song as IImages)
                    }
                }
                state.value = false
            })
        }
    }
}