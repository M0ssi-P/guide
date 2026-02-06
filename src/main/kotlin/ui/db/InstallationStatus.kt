package ui.db

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import db.ConfigViewModel
import org.jetbrains.jewel.ui.component.CircularProgressIndicatorBig
import org.jetbrains.jewel.ui.component.Text
import ui.theme.LocalTheme

@Composable
fun InstallationStatus(model: ConfigViewModel) {
    val theme = LocalTheme.current
    val header = model.installationStatusHeader.collectAsState()
    val status = model.installationStatus.collectAsState()

    Column (
        modifier = Modifier
            .padding(80.dp, 0.dp, 80.dp, 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(header.value, style = theme.typography.h1, color = theme.colors.primaryText)
        Text("Please wait while we finalise all configurations.", style = theme.typography.tab, color = theme.colors.text,)
        Spacer(Modifier.height(40.dp))
        CircularProgressIndicatorBig()
        Spacer(Modifier.height(40.dp))
        Text(status.value, style = theme.typography.tab, color = theme.colors.text,)
    }
}