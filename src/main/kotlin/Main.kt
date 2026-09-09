import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.mossip.auraplayer.engine.AuraPlayer
import components.FullscreenTitleBarView
import components.layouts.Sidebar
import components.titleBarView
import db.ConfigViewModel
import mvvm.ShareViewModels
import navigation.LocalTabs
import navigation.ModalListener
import navigation.NavSystem
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.intui.standalone.theme.*
import org.jetbrains.jewel.intui.window.decoratedWindow
import org.jetbrains.jewel.intui.window.styling.dark
import org.jetbrains.jewel.intui.window.styling.light
import org.jetbrains.jewel.ui.ComponentStyling
import org.jetbrains.jewel.window.DecoratedWindow
import org.jetbrains.jewel.window.styling.TitleBarColors
import org.jetbrains.jewel.window.styling.TitleBarStyle
import player.GlobalPlayerProvider
import player.LocalPlayer
import player.PlayerUI
import presentation.PresentationHost
import presentation.PresentationWindowHost
import ui.db.DBConfig
import ui.screens.NavHost
import ui.theme.LocalTheme
import ui.theme.LocalUi
import ui.theme.ThemeUI

@OptIn(ExperimentalFoundationApi::class)
fun main() {
     initialiseNetwork()

     application {
         val textStyle = JewelTheme.createDefaultTextStyle()
         val engine = remember { AuraPlayer() }
         val windowState = rememberWindowState(size = DpSize(1390.dp, 865.dp))
         val configViewModel = ShareViewModels.globalViewModel
         val shouldHide = configViewModel.shouldHide.collectAsState()
         val shouldHideSidebar = remember { mutableStateOf(
             loadData<Boolean>("should_hide_sidebar") ?: false
         ) }
         val isHideSidebarHovered = remember { mutableStateOf(false) }
         var wasPlayerOn by configViewModel.wasPlayerOn
         val localFullscreen by configViewModel.localFullscreen.collectAsState()

         ThemeUI {
             val theme = LocalTheme.current
             val ui = LocalUi.current

             val themeDefinition =
                 if (ui.value.isDark()) {
                     JewelTheme.darkThemeDefinition(defaultTextStyle = textStyle)
                 } else {
                     JewelTheme.lightThemeDefinition(defaultTextStyle = textStyle)
                 }

             val titleStyle = TitleBarColors(
                 background = theme.colors.menu,
                 inactiveBackground = theme.colors.menu,
                 content = theme.colors.text,
                 border = Color.Unspecified,

                 fullscreenControlButtonsBackground = theme.colors.text,
                 titlePaneButtonHoveredBackground = theme.colors.text,
                 titlePaneButtonPressedBackground = theme.colors.text,
                 titlePaneCloseButtonHoveredBackground = theme.colors.text,
                 titlePaneCloseButtonPressedBackground = theme.colors.text,

                 iconButtonHoveredBackground = theme.colors.text,
                 iconButtonPressedBackground = theme.colors.text,

                 dropdownHoveredBackground = theme.colors.text,
                 dropdownPressedBackground = theme.colors.text
             )

             IntUiTheme(
                 theme = themeDefinition,
                 styling = ComponentStyling.default().decoratedWindow(
                     titleBarStyle =
                         when(ui.value) {
                             IntUiThemes.Light -> TitleBarStyle.light(
                                 colors = titleStyle
                             )
                             IntUiThemes.Dark -> {
                                 TitleBarStyle.dark(
                                     colors = titleStyle
                                 )
                             }
                             IntUiThemes.System ->
                                 if (ui.value.isDark()) {
                                     TitleBarStyle.dark(
                                         colors = titleStyle
                                     )
                                 } else {
                                     TitleBarStyle.light(
                                         colors = titleStyle
                                     )
                                 }
                         }
                 ),
                 swingCompatMode = false
             ) {
                 PresentationHost {
                     GlobalPlayerProvider(engine) {
                         NavSystem {
                             val navigator = LocalTabs.current
                             val localPlayer = LocalPlayer.current

                             DecoratedWindow(
                                 onCloseRequest = {
                                     engine.release()
                                     exitApplication()
                                 },
                                 onPreviewKeyEvent = { keyEvent ->
                                     if (keyEvent.key == Key.F11 && keyEvent.type == KeyEventType.KeyDown) {
                                         configViewModel.toggleLocalFullscreen()
                                         true
                                     } else false
                                 },
                                 visible = true,
                                 state = windowState,
                                 title = "The Guide",
                                 icon = painterResource("icons/logo.png"),
                             ) {
                                 val awtColor = java.awt.Color(18, 18, 18)
                                 window.background = awtColor
                                 window.rootPane.background = awtColor
                                 window.contentPane.background = awtColor

                                 ModalListener(window) {
                                     Row(modifier = Modifier.fillMaxSize().background(theme.colors.surface)) {
                                         if(!shouldHide.value && !shouldHideSidebar.value) {
                                             Sidebar(shouldHideSidebar, shouldHide.value, isHideSidebarHovered)
                                         }
                                         Column(modifier = Modifier.fillMaxSize()){
                                             if(localFullscreen) {
                                                 FullscreenTitleBarView()
                                             } else {
                                                 titleBarView(if (shouldHide.value) 1.dp else 38.dp, shouldHideSidebar, onHovered = isHideSidebarHovered)
                                             }
                                             Box(
                                                 modifier = Modifier.fillMaxSize(),
                                                 contentAlignment = Alignment.Center
                                             ) {
                                                 if(shouldHide.value) DBConfig(configViewModel) else NavHost(navigator)
                                                 if(localFullscreen) {
                                                     Box(Modifier.align(Alignment.BottomStart)) {
                                                         PlayerUI(engine)
                                                     }
                                                 }
                                             }
                                         }
                                     }

                                     if(shouldHideSidebar.value) {
                                         Sidebar(shouldHideSidebar, shouldHide.value, isHideSidebarHovered)
                                     }
                                 }
                             }

//                             if(!localFullscreen) {
//
//                             } else {
//                                 Window(
//                                     onCloseRequest = {
//                                         engine.release()
//                                         exitApplication()
//                                     },
//                                     onPreviewKeyEvent = { keyEvent ->
//                                         if (keyEvent.key == Key.F11 && keyEvent.type == KeyEventType.KeyDown) {
//                                             if(localPlayer.isPlaying.value) {
//                                                 engine.setPause(true)
//                                                 wasPlayerOn = true
//                                             }
//                                             configViewModel.toggleLocalFullscreen()
//                                             true
//                                         } else false
//                                     },
//                                     undecorated = true,
//                                     resizable = false,
//                                     state = rememberWindowState(
//                                         placement = WindowPlacement.Fullscreen,
//                                     ),
//                                     icon = painterResource("icons/logo.png")
//                                 ) {
//                                     LaunchedEffect(wasPlayerOn) {
//                                         if(wasPlayerOn) {
//                                             wasPlayerOn = false
//                                             engine.setPause(false)
//                                         }
//                                     }
//
//                                     Row(modifier = Modifier.fillMaxSize().background(theme.colors.surface)) {
//                                         if(!shouldHide.value && !shouldHideSidebar.value) {
//                                             Sidebar(shouldHideSidebar, shouldHide.value, isHideSidebarHovered)
//                                         }
//                                         Column(modifier = Modifier.fillMaxSize()){
//                                             FullscreenTitleBarView()
//                                             Box(
//                                                 modifier = Modifier.fillMaxSize(),
//                                                 contentAlignment = Alignment.Center
//                                             ) {
//                                                 NavHost(navigator)
//                                                 Box(Modifier.align(Alignment.BottomStart)) {
//                                                     PlayerUI(engine)
//                                                 }
//                                             }
//                                         }
//                                     }
//
//                                     if(shouldHideSidebar.value) {
//                                         Sidebar(shouldHideSidebar, shouldHide.value, isHideSidebarHovered)
//                                     }
//                                 }
//                             }
                         }

                         PresentationWindowHost()
                     }
                 }
             }
         }
     }
}
