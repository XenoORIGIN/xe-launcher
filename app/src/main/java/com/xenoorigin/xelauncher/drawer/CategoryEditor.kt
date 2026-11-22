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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import com.xenoorigin.xelauncher.design.swipeToToggle
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

/** Accent colours offered for categories (null in the picker = the category's own hue). */
val CategoryAccents: List<Long> = listOf(
    0xFFFF6B6B, 0xFFFF9F43, 0xFFFFD166, 0xFF2ED47A, 0xFF1FC8E0, 0xFF4D8BFF, 0xFF9B6BFF, 0xFFFF6BCB,
)

/** The order apps use inside category [id]. */
fun sortForCategory(id: Long, s: com.xenoorigin.xelauncher.settings.XeSettings): com.xenoorigin.xelauncher.settings.DrawerSort =
    when (s.categorySort[id] ?: com.xenoorigin.xelauncher.settings.CategorySort.FOLLOW) {
        com.xenoorigin.xelauncher.settings.CategorySort.FOLLOW -> s.drawerSort
        com.xenoorigin.xelauncher.settings.CategorySort.ALPHABETICAL -> com.xenoorigin.xelauncher.settings.DrawerSort.ALPHABETICAL
        com.xenoorigin.xelauncher.settings.CategorySort.TIME -> com.xenoorigin.xelauncher.settings.DrawerSort.MOST_USED
        com.xenoorigin.xelauncher.settings.CategorySort.NEWEST -> com.xenoorigin.xelauncher.settings.DrawerSort.RECENTLY_INSTALLED
    }

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
    val setAccent: (Long, Long?) -> Unit = { _, _ -> },
    val setSort: (Long, com.xenoorigin.xelauncher.settings.CategorySort) -> Unit = { _, _ -> },
)

/** Full-screen glass page for adding, renaming, re-iconing, reordering and deleting categories. */
@Composable
fun CategoryEditor(
    categories: List<Pair<CategoryEntity, String>>, actions: EditorActions, startAdding: Boolean, onClose: () -> Unit,
    hidden: Set<Long> = emptySet(), counts: Map<Long, Int> = emptyMap(),
    accents: Map<Long, Long> = emptyMap(), sorts: Map<Long, com.xenoorigin.xelauncher.settings.CategorySort> = emptyMap(),
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
                categories, key = { it.first.id }, rowHeight = 78.dp, spacing = 10.dp,
                onMoved = { list -> actions.reorder(list.map { it.first.id }) },
            ) { (c, title), handle, dragging ->
                val isHidden = c.id in hidden
                val canHide = c.builtIn != BuiltInCategory.OTHER.name
                Row(
                    Modifier.fillMaxSize().appear(categories.indexOfFirst { it.first.id == c.id } + 1, distance = 50f)
                        .swipeToToggle(enabled = canHide) { actions.setHidden(c.id, !isHidden) }
                        .glassTile(RoundedCornerShape(22.dp), LocalGlassStyle.current, Color.White.copy(alpha = if (dragging) .2f else .1f))
                        .clickable { editing = c }
                        .testTag("cat_row_${c.id}")
                        .padding(start = 4.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(handle.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ti_grip_vertical), stringResource(R.string.drag_to_reorder), Modifier.size(22.dp), tint = scheme.textSecondary)
                    }
                    val accent = accents[c.id]?.let { Color(it) }
                    Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        if (accent != null) Box(Modifier.size(34.dp).background(accent.copy(alpha = .28f), CircleShape))
                        Icon(painterResource(XeIcons.res(c.iconName)), null, Modifier.size(24.dp), tint = if (isHidden) scheme.textSecondary else accent ?: scheme.text)
                    }
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        // Long names wrap to a second line and end in "…" rather than being clipped mid-word.
                        Text(title, style = XeType.Headline, color = if (isHidden) scheme.textSecondary else scheme.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (isHidden) stringResource(R.string.category_hidden) else stringResource(R.string.n_apps, counts[c.id] ?: 0),
                            style = XeType.Caption, color = scheme.textSecondary,
                        )
                    }
                    if (canHide) {
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
            initialAccent = target?.let { accents[it.id] },
            initialSort = target?.let { sorts[it.id] } ?: com.xenoorigin.xelauncher.settings.CategorySort.FOLLOW,
            showStyle = target != null,
            deletable = target != null && target.builtIn != BuiltInCategory.OTHER.name,
            onDismiss = { adding = false; editing = null },
            onSave = { name, icon, accent, sort ->
                if (target == null) actions.add(name.ifBlank { "…" }, icon) else {
                    actions.rename(target.id, name); actions.setIcon(target.id, icon)
                    actions.setAccent(target.id, accent); actions.setSort(target.id, sort)
                }
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
    onDismiss: () -> Unit, onSave: (String, String, Long?, com.xenoorigin.xelauncher.settings.CategorySort) -> Unit, onDelete: () -> Unit,
    initialAccent: Long? = null, initialSort: com.xenoorigin.xelauncher.settings.CategorySort = com.xenoorigin.xelauncher.settings.CategorySort.FOLLOW,
    showStyle: Boolean = false,
    extra: (@Composable () -> Unit)? = null,
) {
    val scheme = LocalXeScheme.current
    var confirmDelete by remember(visible) { mutableStateOf(false) }
    var name by remember(visible, initialName) { mutableStateOf(initialName) }
    var icon by remember(visible, initialIcon) { mutableStateOf(initialIcon) }
    var accent by remember(visible, initialAccent) { mutableStateOf(initialAccent) }
    var sort by remember(visible, initialSort) { mutableStateOf(initialSort) }
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
        if (showStyle) {
            Text(stringResource(R.string.category_colour), style = XeType.Label, color = scheme.textSecondary, modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = .08f)).border(if (accent == null) 2.dp else 1.dp, if (accent == null) scheme.text else Color.White.copy(alpha = .2f), CircleShape)
                        .clickable { accent = null }.testTag("accent_none"),
                    contentAlignment = Alignment.Center,
                ) { Icon(painterResource(R.drawable.ti_wand), null, Modifier.size(16.dp), tint = scheme.textSecondary) }
                CategoryAccents.forEachIndexed { i, c ->
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(Color(c)).border(if (accent == c) 3.dp else 0.dp, Color.White, CircleShape)
                            .clickable { accent = c }.testTag("accent_$i"),
                    )
                }
            }
            Text(stringResource(R.string.category_sort), style = XeType.Label, color = scheme.textSecondary, modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val labels = listOf(
                    com.xenoorigin.xelauncher.settings.CategorySort.FOLLOW to R.string.cat_sort_follow,
                    com.xenoorigin.xelauncher.settings.CategorySort.ALPHABETICAL to R.string.cat_sort_az,
                    com.xenoorigin.xelauncher.settings.CategorySort.TIME to R.string.cat_sort_time,
                    com.xenoorigin.xelauncher.settings.CategorySort.NEWEST to R.string.cat_sort_newest,
                )
                labels.forEach { (so, l) ->
                    val on = sort == so
                    Box(
                        Modifier.clip(RoundedCornerShape(18.dp)).background(if (on) Color.White else Color.White.copy(alpha = .08f))
                            .clickable { sort = so }.padding(horizontal = 14.dp, vertical = 9.dp).testTag("sort_${so.name}"),
                    ) { Text(stringResource(l), style = XeType.Label, color = if (on) Color.Black else scheme.text) }
                }
            }
        }
        extra?.invoke()
        if (confirmDelete) Text(stringResource(R.string.delete_category_warning), style = XeType.Caption, color = Color(0xFFFF6B6B), modifier = Modifier.padding(top = 8.dp))
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (deletable) GlassPillButton(
                stringResource(if (confirmDelete) R.string.delete_confirm else R.string.delete),
                { if (confirmDelete) onDelete() else confirmDelete = true }, Modifier.weight(1f), icon = R.drawable.ti_trash,
            )
            GlassPillButton(stringResource(R.string.save), { onSave(name.trim(), icon, accent, sort) }, Modifier.weight(1f).testTag("category_save"), icon = R.drawable.ti_check)
        }
    }
}
