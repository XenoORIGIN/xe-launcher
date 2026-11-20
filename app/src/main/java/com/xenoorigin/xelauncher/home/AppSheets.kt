package com.xenoorigin.xelauncher.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.data.CategoryEntity
import com.xenoorigin.xelauncher.design.GlassSheet
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.XeIcons
import com.xenoorigin.xelauncher.design.XeRow
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.icons.IconStyle

@Composable
fun MoveToCategorySheet(app: AppEntry?, categories: List<Pair<CategoryEntity, String>>, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    GlassSheet(app != null, onDismiss) {
        Text(stringResource(R.string.move_to_category), style = XeType.Title, color = LocalXeScheme.current.text, modifier = Modifier.padding(vertical = 8.dp))
        app?.let { Text(it.label, style = XeType.Caption, color = LocalXeScheme.current.textSecondary) }
        categories.forEach { (c, title) -> XeRow(title, icon = XeIcons.res(c.iconName), onClick = { onPick(c.id) }) }
    }
}

@Composable
fun EditIconSheet(app: AppEntry?, current: IconStyle?, onPick: (IconStyle?) -> Unit, onDismiss: () -> Unit) {
    GlassSheet(app != null, onDismiss) {
        Text(stringResource(R.string.edit_icon), style = XeType.Title, color = LocalXeScheme.current.text, modifier = Modifier.padding(vertical = 8.dp))
        app?.let { Text(it.label, style = XeType.Caption, color = LocalXeScheme.current.textSecondary) }
        XeRow(stringResource(R.string.icon_style_default), icon = if (current == null) R.drawable.ti_check else R.drawable.ti_refresh, onClick = { onPick(null) })
        listOf(
            IconStyle.GLASS_DARK to R.string.style_glass_dark, IconStyle.GLASS_COLOR to R.string.style_glass_color,
            IconStyle.GLASS_CLEAR to R.string.style_glass_clear, IconStyle.ORIGINAL to R.string.style_original,
        ).forEach { (s, label) -> XeRow(stringResource(label), icon = if (current == s) R.drawable.ti_check else R.drawable.ti_square_rounded, onClick = { onPick(s) }) }
    }
}
