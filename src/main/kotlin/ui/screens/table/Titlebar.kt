package ui.screens.table

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.book
import com.composables.hamburger
import com.composables.searchIcon
import ui.theme.Inter
import ui.theme.LocalTheme

@Composable
fun Titlebar(
    state: TableState,
    onSearchClick: () -> Unit,
    onBrowseClick: () -> Unit,
    onMenuClick: () -> Unit,
    languageCode: String,
    onLanguageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalTheme.current

    Row(
        modifier = modifier.fillMaxWidth().height(44.dp).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource("src/the_table.png"),
                contentDescription = "The table icon",
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "THE TABLE",
                fontFamily = Inter,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = theme.colors.text,
                letterSpacing = 0.3.sp
            )
        }
        ModeSwitch(
            mode = state.mode,
            onModeChange = { state.mode = it },
            modifier = Modifier.padding(top = 6.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            components.global.LanguageChip(languageCode, "Table language: $languageCode", Modifier.padding(end = 4.dp), height = 30.dp, onClick = onLanguageClick)
            TitlebarIcon(::searchIcon, "Search", onSearchClick)
            TitlebarIcon(::book, "Browse", onBrowseClick)
            TitlebarIcon(::hamburger, "Menu", onMenuClick)
        }
    }
}

@Composable
private fun TitlebarIcon(icon: (c: Color) -> ImageVector, label: String, onClick: () -> Unit) {
    val theme = LocalTheme.current

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable { onClick() }
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon(theme.colors.text), contentDescription = label, tint = Color.Unspecified, modifier = Modifier.size(16.dp))
    }
}
