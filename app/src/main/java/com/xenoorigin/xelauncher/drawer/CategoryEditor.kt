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
)

data class EditorActions(
    val add: (String, String) -> Unit,
    val rename: (Long, String) -> Unit,
    val setIcon: (Long, String) -> Unit,
    val delete: (Long) -> Unit,
    val reorder: (List<Long>) -> Unit,
)

/** Full-screen glass page for adding, renaming, re-iconing, reordering and deleting categories. */
@Composable
fun CategoryEditor(categories: List<Pair<CategoryEntity, String>>, actions: EditorActions, startAdding: Boolean, onClose: () -> Unit) {
    val scheme = LocalXeScheme.current
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var adding by remember { mutableStateOf(startAdding) }
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .45f)).clickable(enabled = false) {}) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(R.drawable.ti_arrow_left, stringResource(R.string.done), onClose)
                Text(stringResource(R.string.edit_categories), Modifier.weight(1f).padding(start = 14.dp), style = XeType.Title, color = scheme.text)
                GlassIconButton(R.drawable.ti_plus, stringResource(R.string.add_category), { adding = true })
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(categories, key = { _, c -> c.first.id }) { i, (c, title) ->
                    GlassSurface(RoundedCornerShape(22.dp), Modifier.fillMaxWidth().height(64.dp).clickable { editing = c }) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(XeIcons.res(c.iconName)), null, Modifier.size(24.dp), tint = scheme.text)
                            Text(title, Modifier.weight(1f).padding(start = 14.dp), style = XeType.Headline, color = scheme.text)
                            val ids = categories.map { it.first.id }
                            if (i > 0) GlassIconButton(R.drawable.ti_chevron_up, stringResource(R.string.move_up), {
                                actions.reorder(ids.toMutableList().apply { add(i - 1, removeAt(i)) })
                            }, size = 36.dp)
                            if (i < categories.lastIndex) GlassIconButton(R.drawable.ti_chevron_down, stringResource(R.string.move_down), {
                                actions.reorder(ids.toMutableList().apply { add(i + 1, removeAt(i)) })
                            }, Modifier.padding(start = 6.dp), size = 36.dp)
                        }
                    }
                }
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
        )
    }
}

@Composable
private fun CategoryForm(
    visible: Boolean, initialName: String, initialIcon: String, deletable: Boolean,
    onDismiss: () -> Unit, onSave: (String, String) -> Unit, onDelete: () -> Unit,
) {
    val scheme = LocalXeScheme.current
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
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (deletable) GlassPillButton(stringResource(R.string.delete), onDelete, Modifier.weight(1f), icon = R.drawable.ti_trash)
            GlassPillButton(stringResource(R.string.save), { onSave(name.trim(), icon) }, Modifier.weight(1f), icon = R.drawable.ti_check)
        }
    }
}
