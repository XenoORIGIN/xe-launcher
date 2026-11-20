package com.xenoorigin.xelauncher.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.xenoorigin.xelauncher.design.GlassPillButton
import com.xenoorigin.xelauncher.design.LocalGlassStyle
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.design.glassTile
import com.xenoorigin.xelauncher.home.IconStylePicker
import com.xenoorigin.xelauncher.icons.IconShape
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xelauncher.icons.IconTile

/** Pick several apps, then give them all one icon style (or put them back on the default). */
@Composable
fun BulkIcons(
    apps: List<AppEntry>, styleOf: (AppEntry) -> IconStyle, shape: IconShape,
    onApply: (keys: Set<String>, style: IconStyle?) -> Unit, onClose: () -> Unit,
) {
    val scheme = LocalXeScheme.current
    val sorted = remember(apps) { apps.sortedWith(AppSorting.alphabetical) }
    var picked by remember { mutableStateOf(emptySet<String>()) }
    var style by remember { mutableStateOf(IconStyle.GLASS_DARK) }
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .6f)).clickable(enabled = false) {}) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(R.drawable.ti_arrow_left, stringResource(R.string.done), onClose)
                Text(stringResource(R.string.bulk_icons), Modifier.weight(1f).padding(start = 14.dp), style = XeType.Title, color = scheme.text)
                GlassIconButton(
                    if (picked.size == sorted.size) R.drawable.ti_square else R.drawable.ti_square_check,
                    stringResource(R.string.select_all),
                    { picked = if (picked.size == sorted.size) emptySet() else sorted.map { it.key.encode() }.toSet() },
                )
            }
            Text(stringResource(R.string.bulk_icons_hint, picked.size), style = XeType.Caption, color = scheme.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(sorted, key = { it.key.encode() }) { app ->
                    val k = app.key.encode()
                    val on = k in picked
                    Row(
                        Modifier.fillMaxWidth().height(58.dp)
                            .glassTile(RoundedCornerShape(18.dp), LocalGlassStyle.current, Color.White.copy(alpha = if (on) .18f else .06f))
                            .clickable { picked = if (on) picked - k else picked + k }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconTile(app, 38.dp, if (on) style else styleOf(app), shape)
                        Text(app.label, Modifier.weight(1f).padding(start = 12.dp), style = XeType.Body, color = scheme.text, maxLines = 1)
                        Icon(painterResource(if (on) R.drawable.ti_square_check else R.drawable.ti_square), null, Modifier.size(22.dp), tint = if (on) scheme.text else scheme.textSecondary)
                    }
                }
            }
            IconStylePicker(sorted.firstOrNull { it.key.encode() in picked } ?: sorted.firstOrNull(), style, shape, { style = it })
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassPillButton(stringResource(R.string.icon_style_default), { if (picked.isNotEmpty()) { onApply(picked, null); picked = emptySet() } }, Modifier.weight(1f), icon = R.drawable.ti_refresh)
                GlassPillButton(stringResource(R.string.apply_to_n, picked.size), { if (picked.isNotEmpty()) { onApply(picked, style); picked = emptySet() } }, Modifier.weight(1f), icon = R.drawable.ti_check)
            }
        }
    }
}
