package com.xenoorigin.xelauncher.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.design.GlassIconButton
import com.xenoorigin.xelauncher.design.LocalGlassStyle
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.ReorderableColumn
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.design.glassTile
import com.xenoorigin.xelauncher.icons.IconShape
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xelauncher.icons.IconTile

/** Drag apps of one category into the order you want; saving switches the drawer to "My own order". */
@Composable
fun ArrangeApps(
    title: String, apps: List<AppEntry>, styleOf: (AppEntry) -> IconStyle, shape: IconShape,
    onSave: (List<AppEntry>) -> Unit, onClose: () -> Unit,
) {
    val scheme = LocalXeScheme.current
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)).clickable(enabled = false) {}) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(R.drawable.ti_arrow_left, stringResource(R.string.done), onClose)
                Text(title, Modifier.weight(1f).padding(start = 14.dp), style = XeType.Title, color = scheme.text)
            }
            Text(stringResource(R.string.drag_to_reorder), style = XeType.Caption, color = scheme.textSecondary, modifier = Modifier.padding(bottom = 10.dp))
            ReorderableColumn(
                apps, key = { it.key.encode() }, rowHeight = 60.dp, spacing = 8.dp, onMoved = onSave,
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            ) { app, handle, dragging ->
                Row(
                    Modifier.fillMaxSize().glassTile(RoundedCornerShape(20.dp), LocalGlassStyle.current, Color.White.copy(alpha = if (dragging) .2f else .08f))
                        .padding(start = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconTile(app, 40.dp, styleOf(app), shape)
                    Text(app.label, Modifier.weight(1f).padding(start = 14.dp), style = XeType.Body, color = scheme.text, maxLines = 1)
                    Box(handle.size(56.dp), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ti_grip_vertical), stringResource(R.string.drag_to_reorder), Modifier.size(22.dp), tint = scheme.textSecondary)
                    }
                }
            }
        }
    }
}
