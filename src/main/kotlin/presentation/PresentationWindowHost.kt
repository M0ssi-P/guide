package presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import org.jetbrains.jewel.window.DecoratedWindow
import java.awt.Rectangle

var firstScreenHandle: Boolean = false;

@Composable
fun PresentationWindowHost() {
    val windowController = LocalWindowController.current
    val screens = rememberScreens()
    val density = LocalDensity.current

    if (screens.size < 2) {
        windowController.switchScreen(0)
    }

    if(!firstScreenHandle && windowController.currentScreen == 0) {
        windowController.toFullScreen(false)
        firstScreenHandle = true
    }

    val secondMonitor = screens[windowController.currentScreen]
    val bounds: Rectangle = secondMonitor.defaultConfiguration.bounds

    val windowSizeDp = with(density) {
        DpSize(
            width = if (windowController.currentScreen == 0) 1390.dp else bounds.width.toDp(),
            height = if (windowController.currentScreen == 0) 865.dp else bounds.height.toDp()
        )
    }

    if(windowController.presentation) {
        if(!windowController.fullScreen) {
            Window(
                onCloseRequest = { windowController.closePresentation() },
                state = rememberWindowState(
                    size = windowSizeDp
                ),
                undecorated = false,
                title = "The Guide - Presentation",
                resizable = false,
                icon = painterResource("icons/logo.png")
            ) {
                WindowPresentationPreview()
            }
        } else {
            Window(
                onCloseRequest = { windowController.closePresentation() },
                state = rememberWindowState(
                    size = windowSizeDp
                ),
                undecorated = true,
                title = "The Guide - Presentation",
                resizable = false,
                icon = painterResource("icons/logo.png")
            ) {
                LaunchedEffect(Unit) {
                    window.setLocation(bounds.x, bounds.y)
                    window.placement = WindowPlacement.Fullscreen
                }

                WindowPresentationPreview()
            }
        }
    }
}