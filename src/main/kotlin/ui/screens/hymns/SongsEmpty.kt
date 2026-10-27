package ui.screens.hymns

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.global.bob
import components.global.rise
import kotlinx.coroutines.launch
import mvvm.SongBookViewModal
import navigation.LocalTabs
import navigation.Screen
import ui.screens.thisweek.hand
import ui.theme.Fraunces
import ui.theme.LocalTheme

/** Shown in Songs when no song book is installed: get one from the Shop, make one, or import a PDF. */
@Composable
fun SongsEmpty() {
    val theme = LocalTheme.current
    val nav = LocalTabs.current.current
    val scope = rememberCoroutineScope()
    val shop by SongShop.books
    LaunchedEffect(Unit) { if (SongShop.books.value == null) SongShop.refresh() }
    fun open(t: WorkshopTab) { WorkshopState.tab = t; WorkshopState.bookId = null; nav.navigate(Screen.SongWorkshop) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(200.dp, 130.dp).rise(0), contentAlignment = Alignment.Center) {
            Box(Modifier.offset((-34).dp, 6.dp).rotate(-10f).bob().size(62.dp, 84.dp).clip(RoundedCornerShape(4.dp)).background(coverFor("Hymns of praise")))
            Box(Modifier.offset(34.dp, 6.dp).rotate(10f).bob().size(62.dp, 84.dp).clip(RoundedCornerShape(4.dp)).background(coverFor("Songs of zion")))
            Box(Modifier.bob().size(70.dp, 94.dp).clip(RoundedCornerShape(4.dp)).background(coverFor("Gospel songs")), contentAlignment = Alignment.Center) {
                Text("♪", color = Color(0xFFFFF6E2), fontSize = 26.sp)
            }
            Text("♫", color = theme.colors.primaryBright, fontSize = 18.sp, modifier = Modifier.align(Alignment.TopEnd).bob())
            Text("♪", color = theme.colors.primary, fontSize = 14.sp, modifier = Modifier.align(Alignment.TopStart).bob())
        }
        Text("No song books yet", color = theme.colors.primaryText, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 32.sp, modifier = Modifier.padding(top = 18.dp).rise(80))
        Text("Download a song book from the shop, or make your own with your church’s songs.", color = theme.colors.text, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp).widthIn(max = 460.dp).rise(120))

        Row(Modifier.padding(top = 26.dp).widthIn(max = 640.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Choice("Get a song book", "Browse the shop and download in one click.", "Open the shop →", Modifier.weight(1f).rise(160)) { open(WorkshopTab.SHOP) }
            Choice("Create your own", "Type your songs, add a cover, export a PDF.", "Start a book →", Modifier.weight(1f).rise(200)) { open(WorkshopTab.CREATE) }
        }

        val popular = shop.orEmpty().take(4)
        if (popular.isNotEmpty()) {
            Text("POPULAR · ONE CLICK", color = theme.colors.text, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, modifier = Modifier.padding(top = 28.dp, bottom = 10.dp))
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                popular.forEachIndexed { i, b ->
                    val p = SongShop.progress[b.id]
                    Row(
                        Modifier.fillMaxWidth().rise(240 + i * 40).clip(RoundedCornerShape(4.dp)).background(theme.colors.menu).border(1.dp, theme.colors.border, RoundedCornerShape(4.dp)).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BookCover(b.name, null, Modifier.size(34.dp, 44.dp).clip(RoundedCornerShape(4.dp)), mono = 24.dp, showName = false)
                        Text(b.name, color = theme.colors.primaryText, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (p != null) Box(Modifier.width(140.dp)) { Progress(p, "Downloading") }
                        else SmallBtn("Get") {
                            scope.launch {
                                if (SongShop.download(b)) nav.viewModelStore.getOrCreate("SongBookVM-${nav.id}") { SongBookViewModal().apply { initilize() } }.refreshBooks()
                            }
                        }
                    }
                }
            }
        }
        Text("Have a song book PDF from another computer? Import it", color = theme.colors.primaryBright, fontSize = 13.sp,
            modifier = Modifier.padding(top = 24.dp).clickable { open(WorkshopTab.TRANSFER) }.hand())
    }
}

@Composable
private fun Choice(title: String, body: String, cta: String, modifier: Modifier, onClick: () -> Unit) {
    val theme = LocalTheme.current
    Column(
        modifier.height(150.dp).clip(RoundedCornerShape(4.dp)).background(theme.colors.menu).border(1.dp, theme.colors.border, RoundedCornerShape(4.dp)).clickable(onClick = onClick).hand().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, color = theme.colors.primaryText, fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 19.sp)
        Text(body, color = theme.colors.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(cta, color = theme.colors.primaryBright, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
