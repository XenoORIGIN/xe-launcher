package com.xenoorigin.xelauncher.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.design.GlassIconButton
import com.xenoorigin.xelauncher.design.GlassPillButton

/** Bar shown at the top while editing Home. */
@Composable
fun EditBar(onWidgets: () -> Unit, onWallpaper: () -> Unit, onSettings: () -> Unit, onAddPage: () -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GlassIconButton(R.drawable.ti_layout_dashboard, stringResource(R.string.widgets), onWidgets)
        GlassIconButton(R.drawable.ti_wallpaper, stringResource(R.string.wallpaper), onWallpaper)
        GlassIconButton(R.drawable.ti_settings, stringResource(R.string.settings), onSettings)
        GlassIconButton(R.drawable.ti_plus, stringResource(R.string.add_page), onAddPage)
        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        GlassPillButton(stringResource(R.string.done), onDone)
    }
}
