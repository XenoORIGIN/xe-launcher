package com.xenoorigin.xelauncher.drawer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
    onArrange: () -> Unit = {},
    onBulkIcons: () -> Unit = {},
) {
    GlassSheet(visible, onDismiss) {
        Text(stringResource(R.string.drawer_menu_title), style = XeType.Title, color = LocalXeScheme.current.text, modifier = Modifier.padding(vertical = 8.dp))
        androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassPillButton(stringResource(R.string.add_category), onAddCategory, Modifier.fillMaxWidth(), icon = R.drawable.ti_plus)
            GlassPillButton(stringResource(R.string.edit_categories), onEditCategories, Modifier.fillMaxWidth(), icon = R.drawable.ti_edit)
            GlassPillButton(stringResource(R.string.arrange_apps), onArrange, Modifier.fillMaxWidth(), icon = R.drawable.ti_arrows_sort)
            GlassPillButton(stringResource(R.string.bulk_icons), onBulkIcons, Modifier.fillMaxWidth(), icon = R.drawable.ti_palette)
        }
        XeRow(stringResource(R.string.category_bar), icon = R.drawable.ti_layout_grid) {
            GlassSwitch(settings.categoryBar, { v -> onUpdate { it.copy(categoryBar = v) } })
        }
        SectionTitle(stringResource(R.string.sorting))
        listOf(DrawerSort.ALPHABETICAL to R.string.sort_alpha, DrawerSort.MOST_USED to R.string.sort_most_used, DrawerSort.RECENTLY_INSTALLED to R.string.sort_recent, DrawerSort.CUSTOM to R.string.sort_custom)
            .forEach { (sort, label) ->
                XeRow(stringResource(label), icon = if (settings.drawerSort == sort) R.drawable.ti_check else R.drawable.ti_arrows_sort,
                    onClick = { onUpdate { it.copy(drawerSort = sort) } })
            }
        UsageAccessRow(settings.drawerSort == DrawerSort.MOST_USED)
        SectionTitle(" ")
        XeRow(stringResource(R.string.hidden_apps), icon = R.drawable.ti_eye_off, onClick = onHiddenApps)
        XeRow(stringResource(R.string.lock_layout), icon = if (settings.layoutLocked) R.drawable.ti_lock else R.drawable.ti_lock_open) {
            GlassSwitch(settings.layoutLocked, { v -> onUpdate { it.copy(layoutLocked = v) } })
        }
        XeRow(stringResource(R.string.all_settings), icon = R.drawable.ti_settings, onClick = onAllSettings)
    }
}

/** Asks for Usage access when "Used longest" is chosen and Xe can't see time spent yet. */
@Composable
fun UsageAccessRow(show: Boolean) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var granted by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(com.xenoorigin.xelauncher.apps.UsageTime.hasAccess(ctx)) }
    // Re-check when the user comes back from Android's settings.
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    androidx.compose.runtime.DisposableEffect(lifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) granted = com.xenoorigin.xelauncher.apps.UsageTime.hasAccess(ctx)
        }
        lifecycle.addObserver(obs); onDispose { lifecycle.removeObserver(obs) }
    }
    if (!show || granted) return
    XeRow(
        stringResource(R.string.usage_access_title), subtitle = stringResource(R.string.usage_access_desc), icon = R.drawable.ti_clock,
        onClick = { com.xenoorigin.xelauncher.apps.UsageTime.requestAccess(ctx) },
    )
}
