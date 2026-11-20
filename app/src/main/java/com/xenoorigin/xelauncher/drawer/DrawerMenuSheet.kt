package com.xenoorigin.xelauncher.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.design.GlassPillButton
import com.xenoorigin.xelauncher.design.GlassSheet
import com.xenoorigin.xelauncher.design.GlassSwitch
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.SectionTitle
import com.xenoorigin.xelauncher.design.XeRow
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.settings.DrawerSort
import com.xenoorigin.xelauncher.settings.XeSettings

@Composable
fun DrawerMenuSheet(
    visible: Boolean,
    settings: XeSettings,
    onDismiss: () -> Unit,
    onUpdate: ((XeSettings) -> XeSettings) -> Unit,
    onAddCategory: () -> Unit,
    onEditCategories: () -> Unit,
    onHiddenApps: () -> Unit,
    onAllSettings: () -> Unit,
) {
    GlassSheet(visible, onDismiss) {
        Text(stringResource(R.string.drawer_menu_title), style = XeType.Title, color = LocalXeScheme.current.text, modifier = Modifier.padding(vertical = 8.dp))
        androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassPillButton(stringResource(R.string.add_category), onAddCategory, Modifier.fillMaxWidth(), icon = R.drawable.ti_plus)
            GlassPillButton(stringResource(R.string.edit_categories), onEditCategories, Modifier.fillMaxWidth(), icon = R.drawable.ti_edit)
        }
        XeRow(stringResource(R.string.category_bar), icon = R.drawable.ti_layout_grid) {
            GlassSwitch(settings.categoryBar, { v -> onUpdate { it.copy(categoryBar = v) } })
        }
        SectionTitle(stringResource(R.string.sorting))
        listOf(DrawerSort.ALPHABETICAL to R.string.sort_alpha, DrawerSort.MOST_USED to R.string.sort_most_used, DrawerSort.RECENTLY_INSTALLED to R.string.sort_recent)
            .forEach { (sort, label) ->
                XeRow(stringResource(label), icon = if (settings.drawerSort == sort) R.drawable.ti_check else R.drawable.ti_arrows_sort,
                    onClick = { onUpdate { it.copy(drawerSort = sort) } })
            }
        SectionTitle(" ")
        XeRow(stringResource(R.string.hidden_apps), icon = R.drawable.ti_eye_off, onClick = onHiddenApps)
        XeRow(stringResource(R.string.lock_layout), icon = if (settings.layoutLocked) R.drawable.ti_lock else R.drawable.ti_lock_open) {
            GlassSwitch(settings.layoutLocked, { v -> onUpdate { it.copy(layoutLocked = v) } })
        }
        XeRow(stringResource(R.string.all_settings), icon = R.drawable.ti_settings, onClick = onAllSettings)
    }
}
