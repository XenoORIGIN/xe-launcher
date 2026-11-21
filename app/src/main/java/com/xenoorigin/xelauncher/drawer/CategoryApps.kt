package com.xenoorigin.xelauncher.drawer

import com.xenoorigin.xelauncher.design.appear

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.categories.AppSorting
import com.xenoorigin.xelauncher.design.GlassIconButton
import com.xenoorigin.xelauncher.design.LocalGlassStyle
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.design.glassTile
import com.xenoorigin.xelauncher.icons.IconShape
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xelauncher.icons.IconTile

/**
 * Every app, with the ones in this category ticked. Ticking moves an app here; unticking hands it
 * back to Xe's automatic sorting. [where] names each app's current category.
 */
@Composable
fun CategoryApps(
    title: String, apps: List<AppEntry>, inCategory: Set<String>, where: (AppEntry) -> String,
    styleOf: (AppEntry) -> IconStyle, shape: IconShape,
    onToggle: (AppEntry, Boolean) -> Unit, onClose: () -> Unit, background: @Composable () -> Unit,
) {
    val scheme = LocalXeScheme.current
    // Apps already here first, then the rest, each alphabetical.
    val sorted = remember(apps, inCategory) {
        apps.sortedWith(compareBy<AppEntry> { it.key.encode() !in inCategory }.then(AppSorting.alphabetical))
    }
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().clickable(enabled = false) {}) {
        background()
        Column(Modifier.fillMaxSize().appear(0, distance = 120f, scaleFrom = .98f).statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(R.drawable.ti_arrow_left, stringResource(R.string.done), onClose)
                Text(title, Modifier.weight(1f).padding(start = 14.dp), style = XeType.Title, color = scheme.text, maxLines = 1)
            }
            Text(stringResource(R.string.choose_apps_hint), style = XeType.Caption, color = scheme.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(sorted, key = { it.key.encode() }) { app ->
                    val on = app.key.encode() in inCategory
                    Row(
                        Modifier.fillMaxWidth().height(60.dp)
                            .glassTile(RoundedCornerShape(18.dp), LocalGlassStyle.current, Color.White.copy(alpha = if (on) .16f else .05f))
                            .clickable { onToggle(app, !on) }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconTile(app, 38.dp, styleOf(app), shape)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(app.label, style = XeType.Body, color = scheme.text, maxLines = 1)
                            if (!on) Text(where(app), style = XeType.Caption, color = scheme.textSecondary, maxLines = 1)
                        }
                        Icon(painterResource(if (on) R.drawable.ti_square_check else R.drawable.ti_square), null, Modifier.size(22.dp), tint = if (on) scheme.text else scheme.textSecondary)
                    }
                }
                item { Box(Modifier.height(24.dp)) }
            }
        }
    }
}
