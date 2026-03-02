package components.layouts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.composables.safeHome
import com.composables.savedIcon
import com.composables.searchIcon
import com.composables.settingsIcon
import com.composables.statsIcon
import com.composables.tabIcon
import components.global.NavItem
import components.global.NavigationButton
import navigation.LibraryScreen
import navigation.LocalTabs
import navigation.Screen
import org.jetbrains.jewel.foundation.modifier.onHover
import saveData
import ui.theme.LocalTheme

data class MenuItem(
    val name: String,
    val matchPath: Screen,
    val imageVector: (Color) -> ImageVector
)

data class BooksItem(
    val name: String,
    val matchPath: Screen,
    val imageVector: String
)

val menuBarItems = mutableListOf<MenuItem>(
    MenuItem(
        name = "Search",
        matchPath = Screen.Library(LibraryScreen.Table()),
        imageVector = ::searchIcon
    ),
    MenuItem(
        name = "Home",
        matchPath = Screen.Home,
        imageVector = ::safeHome
    ),
    MenuItem(
        name = "Saved",
        matchPath = Screen.Saved,
        imageVector = ::savedIcon
    ),
    MenuItem(
        name = "My Activity",
        matchPath = Screen.MyActivity,
        imageVector = ::statsIcon
    ),
)

val bookItems = mutableListOf<BooksItem>(
    BooksItem(
        name = "The Table",
        matchPath = Screen.Library(LibraryScreen.Table()),
        imageVector = "src/the_table.png"
    ),
    BooksItem(
        name = "Bible",
        matchPath = Screen.Library(LibraryScreen.Bible()),
        imageVector = "src/the_bible.png"
    ),
    BooksItem(
        name = "Song Books",
        matchPath = Screen.Library(LibraryScreen.Hymns()),
        imageVector = "src/jesus_cloud.png"
    )
)

@Composable
fun Sidebar(shouldHideSidebar: MutableState<Boolean>, shouldHide: Boolean, onHoveredSidebar: MutableState<Boolean>) {
    val theme = LocalTheme.current
    val sidebarWidth = 241.dp
    val interactionSource = remember { MutableInteractionSource() }

    val paddingTop by animateDpAsState(
        targetValue = if (!shouldHideSidebar.value) 0.dp else 100.dp,
        animationSpec = tween(
            durationMillis = 500,
            easing = LinearOutSlowInEasing
        )
    )
    val paddingBottom by animateDpAsState(
        targetValue = if (!shouldHideSidebar.value) 0.dp else 60.dp,
        animationSpec = tween(
            durationMillis = 500,
            easing = LinearOutSlowInEasing
        )
    )

    val shape = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = if(shouldHideSidebar.value) 6.dp else 0.dp,
        bottomStart = 0.dp,
        bottomEnd = if(shouldHideSidebar.value) 6.dp else 0.dp
    )

    AnimatedVisibility(
        visible = !shouldHide && !shouldHideSidebar.value || onHoveredSidebar.value,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(
                220,
            )
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(
                durationMillis = 220,
                delayMillis = 100,
            )
        )
    ) {
        Box(
            modifier = Modifier
                .width(sidebarWidth)
                .fillMaxHeight()
                .zIndex(10f)
                .padding(top = paddingTop, bottom = paddingBottom)
        ) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .then(if (shouldHideSidebar.value) Modifier.dropShadow(
                    shape = RoundedCornerShape(6.dp),
                    block = {
                        color = Color.Black.copy(alpha = 0.12f)
                        alpha = 1f
                        spread = -6f
                        radius = 28f
                        offset = Offset(0f, 14f)
                    }
                ) else Modifier)
                .background(
                    theme.colors.menu,
                    shape
                )
                .border(1.dp, color = theme.colors.border, shape)
                .onHover { hover ->
                    onHoveredSidebar.value = hover
                    false
                }
            ) {
                if(!shouldHideSidebar.value) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .height(38.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Spacer(modifier = Modifier.width(10.dp))
                        NavigationButton(contentColor = theme.colors.text, modifier = Modifier.size(30.dp),  padding = PaddingValues()) {
                            Icon(
                                painter = painterResource("icons/logo.png"),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        NavigationButton(contentColor = theme.colors.text,
                            modifier =  Modifier.size(30.dp)
                                .onHover { hovered -> onHoveredSidebar.value = hovered},
                            onClick = {
                                val newValue = !shouldHideSidebar.value;

                                shouldHideSidebar.value = newValue
                                saveData("should_hide_sidebar", newValue)
                            },
                            padding = PaddingValues())
                        {
                            Icon(
                                imageVector = tabIcon(theme.colors.text),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize().padding(10.dp, 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    val tab = LocalTabs.current
                    val navigation = tab.current

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        menuBarItems.mapIndexed { i, item ->
                            val isActive = navigation.current == item.matchPath
                            NavItem(
                                isActive,
                                key = i,
                                name = item.name,
                                icon = item.imageVector,
                                onClick = {
                                    navigation.navigate(item.matchPath)
                                }
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("Books", style = theme.typography.tab, color = theme.colors.text, fontSize = 10.sp, modifier = Modifier.padding(vertical = 10.dp))
                        bookItems.mapIndexed { i, item ->
                            val isActive = navigation.current == item.matchPath
                            NavItem(
                                isActive,
                                key = i,
                                name = item.name,
                                iconString = item.imageVector,
                                onClick = {
                                    navigation.navigate(item.matchPath)
                                }
                            )
                        }
                    }

                    NavItem(
                        isActive = navigation.current == Screen.Setting,
                        key = 4,
                        name = "Setting",
                        icon = ::settingsIcon,
                        onClick = {
                            navigation.navigate(Screen.Setting)
                        }
                    )
                }
            }
        }
    }
}