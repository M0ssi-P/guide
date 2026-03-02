import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import components.layouts.Sidebar
import components.titleBarView
import db.ConfigViewModel
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
         val windowState = rememberWindowState(size = DpSize(1390.dp, 865.dp))
         val configViewModel = remember { ConfigViewModel() }
         val shouldHide = configViewModel.shouldHide.collectAsState()
         val shouldHideSidebar = remember { mutableStateOf(
             loadData<Boolean>("should_hide_sidebar") ?: false
         ) }
         val isHideSidebarHovered = remember { mutableStateOf(false) }

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
                     GlobalPlayerProvider {
                         DecoratedWindow(
                             onCloseRequest = { exitApplication() },
                             state = windowState,
                             title = "The Guide",
                             icon = painterResource("icons/logo.png")
                         ) {
                             val awtColor = java.awt.Color(18, 18, 18)

                             window.background = awtColor
                             window.rootPane.background = awtColor
                             window.contentPane.background = awtColor

                             NavSystem {
                                 val navigator = LocalTabs.current

                                 ModalListener {
                                     Row(modifier = Modifier.fillMaxSize().background(theme.colors.surface)) {
                                         if(!shouldHide.value && !shouldHideSidebar.value) {
                                             Sidebar(shouldHideSidebar, shouldHide.value, isHideSidebarHovered)
                                         }
                                         Column(modifier = Modifier.fillMaxSize()){
                                             titleBarView(if (shouldHide.value) 1.dp else 38.dp, shouldHideSidebar, onHovered = isHideSidebarHovered)
                                             Box(
                                                 modifier = Modifier.fillMaxSize(),
                                                 contentAlignment = Alignment.Center
                                             ) {
                                                 if(shouldHide.value) DBConfig(configViewModel) else NavHost(navigator)
                                                 Box(Modifier.align(Alignment.BottomStart)) {
                                                     PlayerUI()
                                                 }
                                             }
                                         }
                                     }

                                     if(shouldHideSidebar.value) {
                                         Sidebar(shouldHideSidebar, shouldHide.value, isHideSidebarHovered)
                                     }
                                 }
                             }
                         }

                         PresentationWindowHost()
                     }
                 }
             }
         }
     }
}
