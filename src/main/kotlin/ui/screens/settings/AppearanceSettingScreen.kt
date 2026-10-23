package ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.ui.component.Text
import ui.theme.AppTheme
import ui.theme.AppThemes
import ui.theme.Fraunces
import ui.theme.LocalAccentTheme
import ui.theme.LocalTheme
import ui.theme.LocalUi
import ui.theme.updateUiSettings

@Composable
fun AppearanceSettingsScreen(
    appStats: AppStatsInfo = AppStatsInfo(),
    modifier: Modifier = Modifier
) {
    val theme = LocalTheme.current
    var activeSection by remember { mutableStateOf(SettingsSection.Appearance) }
    var prefs by remember { mutableStateOf(AppearancePrefs()) }

    val uiState = LocalUi.current
    val accentState = LocalAccentTheme.current

    val removableThemeIds = remember { AppThemes.map { it.id }.toSet() - setOf("default", "gold") }

    Row(modifier = modifier.fillMaxSize().background(theme.colors.surface)) {
        SettingsSidebar(
            activeSection = activeSection,
            onSectionClick = { activeSection = it },
            appStats = appStats,
            onAdminPanelClick = { /* hook up your actual health-check call here */ }
        )

        AppearanceContent(
            prefs = prefs,
            onPrefsChange = { prefs = it },
            modifier = Modifier.weight(1f).fillMaxHeight()
        )

        ThemePickerPanel(
            selectedId = accentState.value,
            onSelect = { updateUiSettings(ui = uiState, accentTheme = accentState, accentThemeId = it.id) },
            removableIds = removableThemeIds,
            onRemove = { /* e.g. hide this theme from the picker, or no-op if you'd rather it stay */ }
        )
    }
}

@Composable
private fun LiterarySectionHeader(
    label: String,
    modifier: Modifier = Modifier
) {
    val theme = LocalTheme.current
    Column(modifier = modifier.padding(bottom = 16.dp)) {
        Text(
            label,
            style = TextStyle(
                fontFamily = Fraunces,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = theme.colors.primaryText
            )
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .width(36.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(theme.colors.primary)
        )
    }
}

enum class SettingsSection(val label: String) {
    AllSettings("All Settings"),
    Account("Account"),
    Preferences("Preferences"),
    Appearance("Appearance"),
    Narration("Narration"),
    Connections("Connections"),
    Import("Import")
}

data class AppStatsInfo(
    val hostname: String = "yourapp.reader",
    val backendUrl: String = "backend.yourapp.reader",
    val backendSecure: Boolean = true,
    val userId: String? = null,
    val appVersion: String = "1.0.0",
    val backendVersion: String = "1.0.0",
    val diagnosticsLabel: String = "Run diagnostics"
)

@Composable
fun SettingsSidebar(
    activeSection: SettingsSection,
    onSectionClick: (SettingsSection) -> Unit,
    appStats: AppStatsInfo,
    onAdminPanelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalTheme.current
    Column(
        modifier = modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(theme.colors.menu)
            .padding(20.dp)
    ) {
        SettingsSectionHeader("Settings")
        SettingsSection.values().forEach { section ->
            SidebarItem(
                label = section.label,
                active = section == activeSection,
                onClick = { onSectionClick(section) }
            )
        }

        Spacer(Modifier.height(28.dp))
        SettingsSectionHeader("App Stats")

        AppStatRow(label = "Hostname", value = appStats.hostname)
        Spacer(Modifier.height(10.dp))
        AppStatRow(
            label = "Backend URL",
            value = appStats.backendUrl,
            badge = if (appStats.backendSecure) "Secure" else null
        )
        Spacer(Modifier.height(10.dp))
        AppStatRow(
            label = "User ID",
            value = appStats.userId ?: "You are not logged in"
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            AppStatRow("App version", appStats.appVersion, modifier = Modifier.weight(1f))
            AppStatRow("Backend version", appStats.backendVersion, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        SettingsSectionHeader("Admin panel")
        AdminPanelButton(
            text = appStats.diagnosticsLabel,
            onClick = onAdminPanelClick
        )
    }
}

@Composable
private fun SidebarItem(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) theme.colors.menuHoverColor else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Text(label, style = theme.typography.h4.copy(color = if (active) theme.colors.primary else theme.colors.text))
    }
}

@Composable
private fun AppStatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    badge: String? = null
) {
    val theme = LocalTheme.current
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = theme.typography.subs.copy(color = theme.colors.text))
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(theme.colors.menuHoverColor)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(badge, style = theme.typography.tab.copy(color = theme.colors.text, fontSize = 10.sp))
                }
            }
        }
        Text(
            value,
            style = theme.typography.h4BOLD.copy(color = theme.colors.primaryText),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun AdminPanelButton(
    text: String,
    onClick: () -> Unit
) {
    val theme = LocalTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(theme.colors.menu)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = theme.typography.button.copy(color = theme.colors.primaryText))
    }
}

data class AppearancePrefs(
    val discoverShelf: Boolean = true,
    val featuredBooks: Boolean = false,
    val detailsModal: Boolean = false,
    val coverArtTitles: Boolean = true,
    val shelfView: Boolean = false,
    val minimalCovers: Boolean = false,
    val readingOverlay: Boolean = false,
    val compactChapters: Boolean = false,
    val disableAds: Boolean = false
)

@Composable
fun AppearanceContent(
    prefs: AppearancePrefs,
    onPrefsChange: (AppearancePrefs) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalTheme.current
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(28.dp)
    ) {
        LiterarySectionHeader("Your Library")

        SettingsToggleRow(
            title = "Discover shelf",
            description = "Show the Discover shelf on your Library page, below your bookmarked books.",
            checked = prefs.discoverShelf,
            onCheckedChange = { onPrefsChange(prefs.copy(discoverShelf = it)) }
        )
        SettingsToggleRow(
            title = "Featured books",
            description = "Show a carousel of featured books and authors at the top of your Library.",
            checked = prefs.featuredBooks,
            onCheckedChange = { onPrefsChange(prefs.copy(featuredBooks = it)) },
            indent = true
        )
        SettingsToggleRow(
            title = "Book details modal",
            description = "Show a book's details in a modal when you click its cover, instead of opening the full reading page.",
            checked = prefs.detailsModal,
            onCheckedChange = { onPrefsChange(prefs.copy(detailsModal = it)) }
        )
        SettingsToggleRow(
            title = "Cover art titles",
            description = "Show cover art instead of text titles in the details modal and featured shelf.",
            checked = prefs.coverArtTitles,
            onCheckedChange = { onPrefsChange(prefs.copy(coverArtTitles = it)) }
        )
        Text(
            "Most covers are English editions. Other languages might want to disable this!",
            style = theme.typography.semiText.copy(color = theme.colors.text),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        SettingsToggleRow(
            title = "Shelf view",
            description = "Display your currently reading and bookmarked books as carousels instead of a grid.",
            checked = prefs.shelfView,
            onCheckedChange = { onPrefsChange(prefs.copy(shelfView = it)) }
        )
        SettingsToggleRow(
            title = "Minimal covers",
            description = "Hide text content (title, author, year) on book covers, showing only the artwork.",
            checked = prefs.minimalCovers,
            onCheckedChange = { onPrefsChange(prefs.copy(minimalCovers = it)) }
        )

        Spacer(Modifier.height(6.dp))
        Text("Library section order", style = theme.typography.h4BOLD.copy(color = theme.colors.primaryText))
        Text(
            "Drag and drop to reorder the reading and bookmark sections on your Library. Group order can be edited from the Library page.",
            style = theme.typography.body.copy(color = theme.colors.text),
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(theme.colors.menu)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Currently Reading", style = theme.typography.h4BOLD.copy(color = theme.colors.primaryText))
                Text("≡", style = theme.typography.h3.copy(color = theme.colors.text))
            }
        }

        Spacer(Modifier.height(28.dp))
        LiterarySectionHeader("Reading")
        SettingsToggleRow(
            title = "Reading overlay",
            description = "Show a soft overlay with the current chapter and progress when narration is paused.",
            checked = prefs.readingOverlay,
            onCheckedChange = { onPrefsChange(prefs.copy(readingOverlay = it)) }
        )
        SettingsToggleRow(
            title = "Compact chapters",
            description = "Make the chapter list in the reader use the \"classic\" compact vertical view.",
            checked = prefs.compactChapters,
            onCheckedChange = { onPrefsChange(prefs.copy(compactChapters = it)) }
        )

        Spacer(Modifier.height(28.dp))
        LiterarySectionHeader("Advertisements")
        SettingsToggleRow(
            title = "Disable advertisements",
            description = "Hide ads across the site.",
            checked = prefs.disableAds,
            onCheckedChange = { onPrefsChange(prefs.copy(disableAds = it)) }
        )
    }
}

// ---------------------------------------------------------------------------
// Theme picker
// ---------------------------------------------------------------------------

@Composable
fun ThemePickerPanel(
    selectedId: String,
    onSelect: (AppTheme) -> Unit,
    modifier: Modifier = Modifier,
    themes: List<AppTheme> = AppThemes,
    removableIds: Set<String> = emptySet(),
    onRemove: (AppTheme) -> Unit = {}
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(20.dp),
        modifier = modifier.width(420.dp).fillMaxHeight()
    ) {
        items(themes, key = { it.id }) { appTheme ->
            ThemeSwatchCard(
                theme = appTheme,
                active = appTheme.id == selectedId,
                removable = appTheme.id in removableIds,
                onClick = { onSelect(appTheme) },
                onRemove = { onRemove(appTheme) }
            )
        }
    }
}

/**
 * Each card's preview is a tiny book cover — an accent-colored "spine" block
 * plus title/author bars — rather than the movie-poster style of the
 * original reference, so the picker itself reads as a reading app's, not a
 * streaming app's.
 */
@Composable
private fun ThemeSwatchCard(
    theme: AppTheme,
    active: Boolean,
    removable: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = LocalTheme.current.colors
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(mixCardBackground(colors.menu, theme.swatch))
                .border(
                    width = if (active) 2.dp else 1.dp,
                    color = if (active) theme.swatch else colors.border,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onClick() }
                .padding(14.dp)
        ) {
            // "···" overflow-menu dots, top-right.
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(3) {
                    Box(
                        Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(colors.text.copy(alpha = 0.5f))
                    )
                }
            }

            // A miniature book cover: an accent-colored spine on the left,
            // title/author bars and a tiny progress dot beside it.
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier
                        .width(30.dp)
                        .height(60.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 3.dp,
                                bottomStart = 3.dp,
                                topEnd = 8.dp,
                                bottomEnd = 8.dp
                            )
                        )
                        .background(theme.swatch)
                )
                Column {
                    Box(
                        Modifier
                            .width(66.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(colors.text.copy(alpha = 0.4f))
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .width(44.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(colors.text.copy(alpha = 0.24f))
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(4.dp).clip(CircleShape).background(theme.swatch))
                        Box(
                            Modifier
                                .width(28.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(colors.text.copy(alpha = 0.24f))
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                theme.label,
                style = TextStyle(
                    fontFamily = Fraunces,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.primaryText
                )
            )
            when {
                active -> Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.menuHoverColor)
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text("Active", style = TextStyle(fontSize = 11.sp, color = colors.primaryText))
                }
                removable -> Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .clickable { onRemove() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", style = TextStyle(fontSize = 12.sp, color = colors.text))
                }
                else -> Unit
            }
        }
    }
}

private fun mixCardBackground(base: Color, accent: Color, amount: Float = 0.14f): Color = Color(
    red = base.red + (accent.red - base.red) * amount,
    green = base.green + (accent.green - base.green) * amount,
    blue = base.blue + (accent.blue - base.blue) * amount,
    alpha = base.alpha
)