package presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import org.jetbrains.jewel.window.DecoratedWindow
import java.awt.Rectangle

var firstScreenHandle: Boolean = false;

@Composable
fun PresentationWindowHost() {
    val windowController = LocalWindowController.current

    if(!firstScreenHandle && windowController.availableProjection.containsKey(0)) {
        windowController.toFullScreen(0, false)
        firstScreenHandle = true
    }

    if(windowController.presentation) {
        for ((key, value) in windowController.availableProjection ) {
            if(!value.fullScreen) {
                DecoratedWindow(
                    onCloseRequest = { windowController.removeScreen(key) },
                    state = rememberWindowState(),
                    title = "The Guide - Presentation",
                    icon = painterResource("icons/logo.png")
                ) {
                    WindowPresentationPreview(key)
                }
            } else {
                Window(
                    onCloseRequest = { windowController.removeScreen(key) },
                    state = rememberWindowState(),
                    undecorated = true,
                    title = "The Guide - Presentation",
                    resizable = false,
                    icon = painterResource("icons/logo.png"),
                ) {
                    LaunchedEffect(Unit) {
                        window.bounds = value.bounds
                    }

                    WindowPresentationPreview(key)
                }
            }
        }
    }
}