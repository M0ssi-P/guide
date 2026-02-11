import androidx.compose.foundation.ExperimentalFoundationApi
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
import backblazeb2.BackBlazeB2
import backblazeb2.actions.B2Credentials
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
import presentation.PresentationHost
import presentation.PresentationWindowHost
import ui.db.DBConfig
import ui.screens.NavHost
import ui.theme.LocalTheme
import ui.theme.ThemeUI

@OptIn(ExperimentalFoundationApi::class)
fun main() {
     initialiseNetwork()

     application {
         val ui by rememberUpdatedState(IntUiThemes.Dark)
         val textStyle = JewelTheme.createDefaultTextStyle()
         val windowState = rememberWindowState(size = DpSize(1390.dp, 865.dp))
         val configViewModel = remember { ConfigViewModel() }
         val shouldHide = configViewModel.shouldHide.collectAsState()
         val shouldHideSidebar = remember { mutableStateOf(
             loadData<Boolean>("should_hide_sidebar") ?: false
         ) }
         val isHideSidebarHovered = remember { mutableStateOf(false) }

         val themeDefinition =
             if (ui.isDark()) {
                 JewelTheme.darkThemeDefinition(defaultTextStyle = textStyle)
             } else {
                 JewelTheme.lightThemeDefinition(defaultTextStyle = textStyle)
             }

         ThemeUI(ui.isDark()) {
             val theme = LocalTheme.current

             IntUiTheme(
                 theme = themeDefinition,
                 styling = ComponentStyling.default().decoratedWindow(
                     titleBarStyle =
                         when(ui) {
                             IntUiThemes.Light -> TitleBarStyle.light()
                             IntUiThemes.Dark -> {
                                 TitleBarStyle.dark(
                                     colors = TitleBarColors(
                                         background = theme.colors.menu,
                                         inactiveBackground = theme.colors.menu,
                                         content = theme.colors.text, // This is what sets icon + title color
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
                                 )
                             }
                             IntUiThemes.System ->
                                 if (ui.isDark()) {
                                     TitleBarStyle.dark()
                                 } else {
                                     TitleBarStyle.light()
                                 }
                         }
                 ),
                 swingCompatMode = false
             ) {
                 PresentationHost {
                     DecoratedWindow(
                         onCloseRequest = { exitApplication() },
                         state = windowState,
                         title = "The Guide",
                         icon = painterResource("icons/logo.png")
                     ) {
                         NavSystem {
                             val navigator = LocalTabs.current

                             ModalListener {
                                 Row(modifier = Modifier.fillMaxSize()) {
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
