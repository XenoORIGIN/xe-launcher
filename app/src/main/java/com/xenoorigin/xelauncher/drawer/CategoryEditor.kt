package com.xenoorigin.xelauncher.drawer

import com.xenoorigin.xelauncher.design.appear

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import com.xenoorigin.xelauncher.design.ReorderableColumn
import com.xenoorigin.xelauncher.design.glassTile
import com.xenoorigin.xelauncher.design.LocalGlassStyle

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.categories.BuiltInCategory
import com.xenoorigin.xelauncher.data.CategoryEntity
import com.xenoorigin.xelauncher.design.GlassIconButton
import com.xenoorigin.xelauncher.design.GlassPillButton
import com.xenoorigin.xelauncher.design.GlassSheet
import com.xenoorigin.xelauncher.design.GlassSurface
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.XeIcons
import com.xenoorigin.xelauncher.design.XeType

/** Icons offered for categories. */
val CategoryIconChoices = listOf(
    "message-circle", "affiliate", "device-gamepad-2", "photo", "settings", "category", "briefcase", "school", "book", "music",
    "video", "camera", "heart", "star", "home", "shopping-bag", "wallet", "currency-baht", "map-pin", "plane", "car", "run",
    "ball-football", "heartbeat", "code", "terminal", "cpu", "brush", "palette", "shirt", "coffee", "pizza", "gift", "rocket",
    "leaf", "paw", "flame", "bolt", "cloud", "shield", "news", "users", "world", "sparkles", "mood-smile", "wand",
    "tool", "apps", "player-play", "building-bank", "cash", "shopping-cart", "plane-departure", "device-mobile", "users-group",
)

data class EditorActions(
    val add: (String, String) -> Unit,
    val rename: (Long, String) -> Unit,
    val setIcon: (Long, String) -> Unit,
    val delete: (Long) -> Unit,
    val reorder: (List<Long>) -> Unit,
    val setHidden: (Long, Boolean) -> Unit = { _, _ -> },
    val resetAutomatic: (Long?) -> Unit = {},
    val resetAll: () -> Unit = {},
    val manageApps: (Long) -> Unit = {},
)

/** Full-screen glass page for adding, renaming, re-iconing, reordering and deleting categories. */
@Composable
fun CategoryEditor(
    categories: List<Pair<CategoryEntity, String>>, actions: EditorActions, startAdding: Boolean, onClose: () -> Unit,
    hidden: Set<Long> = emptySet(), counts: Map<Long, Int> = emptyMap(),
    background: @Composable () -> Unit = { Box(Modifier.fillMaxSize().background(Color(0xF2000000))) },
) {
    val scheme = LocalXeScheme.current
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var adding by remember { mutableStateOf(startAdding) }
    var confirmReset by remember { mutableStateOf(false) }
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().clickable(enabled = false) {}) {
        background()
        Column(Modifier.fillMaxSize().appear(0, distance = 120f, scaleFrom = .98f).statusBarsPadding().padding(horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(R.drawable.ti_arrow_left, stringResource(R.string.done), onClose)
                Text(stringResource(R.string.edit_categories), Modifier.weight(1f).padding(start = 14.dp), style = XeType.Title, color = scheme.text)
                GlassIconButton(R.drawable.ti_plus, stringResource(R.string.add_category), { adding = true })
            }
            Text(stringResource(R.string.drag_to_reorder), style = XeType.Caption, color = scheme.textSecondary, modifier = Modifier.padding(bottom = 10.dp))
            Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            ReorderableColumn(
                categories, key = { it.first.id }, rowHeight = 64.dp, spacing = 10.dp,
                onMoved = { list -> actions.reorder(list.map { it.first.id }) },
            ) { (c, title), handle, dragging ->
                val isHidden = c.id in hidden
                Row(
                    Modifier.fillMaxSize().appear(categories.indexOfFirst { it.first.id == c.id } + 1, distance = 50f)
                        .glassTile(RoundedCornerShape(22.dp), LocalGlassStyle.current, Color.White.copy(alpha = if (dragging) .2f else .1f))
                        .clickable { editing = c }
                        .padding(start = 4.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(handle.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ti_grip_vertical), stringResource(R.string.drag_to_reorder), Modifier.size(22.dp), tint = scheme.textSecondary)
                    }
                    Icon(painterResource(XeIcons.res(c.iconName)), null, Modifier.size(24.dp), tint = if (isHidden) scheme.textSecondary else scheme.text)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(title, style = XeType.Headline, color = if (isHidden) scheme.textSecondary else scheme.text, maxLines = 1)
                        Text(
                            if (isHidden) stringResource(R.string.category_hidden) else stringResource(R.string.n_apps, counts[c.id] ?: 0),
                            style = XeType.Caption, color = scheme.textSecondary,
                        )
                    }
                    if (c.builtIn != BuiltInCategory.OTHER.name) {
                        Box(Modifier.size(40.dp).clip(CircleShape).clickable { actions.setHidden(c.id, !isHidden) }, contentAlignment = Alignment.Center) {
                            Icon(painterResource(if (isHidden) R.drawable.ti_eye_off else R.drawable.ti_eye), stringResource(R.string.show_in_bar), Modifier.size(20.dp), tint = scheme.textSecondary)
                        }
                    }
                    Icon(painterResource(R.drawable.ti_chevron_right), null, Modifier.size(20.dp), tint = scheme.textSecondary)
                }
            }
            Text(stringResource(R.string.more_options), style = XeType.Label, color = scheme.textSecondary, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
            GlassPillButton(stringResource(R.string.reset_auto_all), { actions.resetAutomatic(null) }, Modifier.fillMaxWidth(), icon = R.drawable.ti_wand)
            GlassPillButton(stringResource(R.string.reset_categories), { confirmReset = true }, Modifier.fillMaxWidth().padding(top = 10.dp), icon = R.drawable.ti_refresh)
            }
        }
        val target = editing
        CategoryForm(
            visible = adding || target != null,
            initialName = target?.let { c -> categories.firstOrNull { it.first.id == c.id }?.second }.orEmpty(),
            initialIcon = target?.iconName ?: "star",
            deletable = target != null && target.builtIn != BuiltInCategory.OTHER.name,
            onDismiss = { adding = false; editing = null },
            onSave = { name, icon ->
                if (target == null) actions.add(name.ifBlank { "…" }, icon) else { actions.rename(target.id, name); actions.setIcon(target.id, icon) }
                adding = false; editing = null
            },
            onDelete = { target?.let { actions.delete(it.id) }; editing = null },
            extra = if (target == null) null else ({
                GlassPillButton(stringResource(R.string.choose_apps), { actions.manageApps(target.id); editing = null }, Modifier.fillMaxWidth().padding(top = 6.dp), icon = R.drawable.ti_apps)
                GlassPillButton(stringResource(R.string.reset_auto_this), { actions.resetAutomatic(target.id); editing = null }, Modifier.fillMaxWidth().padding(top = 10.dp), icon = R.drawable.ti_wand)
            }),
        )
        GlassSheet(confirmReset, { confirmReset = false }) {
            Text(stringResource(R.string.reset_categories), style = XeType.Title, color = scheme.text, modifier = Modifier.padding(vertical = 8.dp))
            Text(stringResource(R.string.reset_categories_warning), style = XeType.Body, color = scheme.textSecondary)
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassPillButton(stringResource(R.string.cancel), { confirmReset = false }, Modifier.weight(1f))
                GlassPillButton(stringResource(R.string.reset), { actions.resetAll(); confirmReset = false }, Modifier.weight(1f), icon = R.drawable.ti_refresh)
            }
        }
    }
}

@Composable
private fun CategoryForm(
    visible: Boolean, initialName: String, initialIcon: String, deletable: Boolean,
    onDismiss: () -> Unit, onSave: (String, String) -> Unit, onDelete: () -> Unit,
    extra: (@Composable () -> Unit)? = null,
) {
    val scheme = LocalXeScheme.current
    var confirmDelete by remember(visible) { mutableStateOf(false) }
    var name by remember(visible, initialName) { mutableStateOf(initialName) }
    var icon by remember(visible, initialIcon) { mutableStateOf(initialIcon) }
    GlassSheet(visible, onDismiss) {
        Text(stringResource(R.string.category_name), style = XeType.Label, color = scheme.textSecondary)
        GlassSurface(RoundedCornerShape(16.dp), Modifier.fillMaxWidth().padding(vertical = 8.dp).height(52.dp)) {
            BasicTextField(
                name, { name = it.take(24) }, Modifier.align(Alignment.CenterStart).fillMaxWidth().padding(horizontal = 16.dp),
                textStyle = XeType.Headline.copy(color = scheme.text), singleLine = true, cursorBrush = SolidColor(scheme.text),
            )
        }
        Text(stringResource(R.string.choose_icon), style = XeType.Label, color = scheme.textSecondary, modifier = Modifier.padding(top = 10.dp))
        LazyVerticalGrid(GridCells.Fixed(6), Modifier.fillMaxWidth().height(260.dp).padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CategoryIconChoices) { n ->
                val sel = n == icon
                Box(
                    Modifier.size(46.dp).background(if (sel) Color.White.copy(alpha = .22f) else Color.Transparent, CircleShape).clickable { icon = n },
                    contentAlignment = Alignment.Center,
                ) { Icon(painterResource(XeIcons.res(n)), n, Modifier.size(24.dp), tint = if (sel) scheme.text else scheme.textSecondary) }
            }
        }
        extra?.invoke()
        if (confirmDelete) Text(stringResource(R.string.delete_category_warning), style = XeType.Caption, color = Color(0xFFFF6B6B), modifier = Modifier.padding(top = 8.dp))
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (deletable) GlassPillButton(
                stringResource(if (confirmDelete) R.string.delete_confirm else R.string.delete),
                { if (confirmDelete) onDelete() else confirmDelete = true }, Modifier.weight(1f), icon = R.drawable.ti_trash,
            )
            GlassPillButton(stringResource(R.string.save), { onSave(name.trim(), icon) }, Modifier.weight(1f), icon = R.drawable.ti_check)
        }
    }
}
