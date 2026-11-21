package com.xenoorigin.xelauncher.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.data.CategoryEntity
import com.xenoorigin.xelauncher.design.GlassPillButton
import com.xenoorigin.xelauncher.design.GlassSheet
import com.xenoorigin.xelauncher.design.LocalGlassStyle
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.XeIcons
import com.xenoorigin.xelauncher.design.XeRow
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.design.glassTile
import com.xenoorigin.xelauncher.icons.IconShape
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xelauncher.icons.IconTile

@Composable
fun MoveToCategorySheet(app: AppEntry?, categories: List<Pair<CategoryEntity, String>>, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    GlassSheet(app != null, onDismiss) {
        Text(stringResource(R.string.move_to_category), style = XeType.Title, color = LocalXeScheme.current.text, modifier = Modifier.padding(vertical = 8.dp))
        app?.let { Text(it.label, style = XeType.Caption, color = LocalXeScheme.current.textSecondary) }
        categories.forEach { (c, title) -> XeRow(title, icon = XeIcons.res(c.iconName), onClick = { onPick(c.id) }) }
    }
}

/** Every icon style, labelled, in the order shown to the user. */
val IconStyleChoices = listOf(
    IconStyle.GLASS_DARK to R.string.style_glass_dark,
    IconStyle.MONO to R.string.style_mono,
    IconStyle.GLASS_COLOR to R.string.style_glass_color,
    IconStyle.GLASS_CLEAR to R.string.style_glass_clear,
    IconStyle.ORIGINAL to R.string.style_original,
)

/** A row of live previews, one per style; [selected] null means "follow the default". */
@Composable
fun IconStylePicker(app: AppEntry?, selected: IconStyle?, shape: IconShape, onPick: (IconStyle) -> Unit, modifier: Modifier = Modifier) {
    val scheme = LocalXeScheme.current
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // "From Xe Studio" is offered once a theme has been applied there.
        val choices = if (com.xenoorigin.xelauncher.icons.LocalStudioThemeVersion.current != null) IconStyleChoices + (IconStyle.STUDIO to R.string.style_studio) else IconStyleChoices
        choices.forEach { (st, label) ->
            val sel = st == selected
            Column(
                Modifier.width(76.dp)
                    .glassTile(RoundedCornerShape(18.dp), LocalGlassStyle.current, Color.White.copy(alpha = if (sel) .22f else .06f))
                    .border(if (sel) 1.5.dp else 0.dp, if (sel) Color.White else Color.Transparent, RoundedCornerShape(18.dp))
                    .clickable { onPick(st) }.padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (app != null) IconTile(app, 48.dp, st, shape)
                Text(stringResource(label), style = XeType.Label, color = if (sel) scheme.text else scheme.textSecondary, textAlign = TextAlign.Center, maxLines = 2)
            }
        }
    }
}

/** Rename an app, pick its icon style, or use your own picture as its icon. */
@Composable
fun EditAppSheet(
    app: AppEntry?, originalLabel: String?, currentStyle: IconStyle?, hasCustomIcon: Boolean, shape: IconShape,
    onSave: (name: String?, style: IconStyle?) -> Unit, onPickImage: () -> Unit, onResetImage: () -> Unit, onDismiss: () -> Unit,
) {
    val scheme = LocalXeScheme.current
    var name by remember(app) { mutableStateOf(app?.label.orEmpty()) }
    var style by remember(app) { mutableStateOf(currentStyle) }
    GlassSheet(app != null, onDismiss) {
        Text(stringResource(R.string.edit_app), style = XeType.Title, color = scheme.text, modifier = Modifier.padding(vertical = 8.dp))
        Text(stringResource(R.string.app_name_label), style = XeType.Label, color = scheme.textSecondary)
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp).height(52.dp)
                .glassTile(RoundedCornerShape(16.dp), LocalGlassStyle.current, Color.White.copy(alpha = .08f)).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                name, { name = it.take(30) }, Modifier.weight(1f),
                textStyle = XeType.Headline.copy(color = scheme.text), singleLine = true, cursorBrush = SolidColor(scheme.text),
            )
        }
        if (originalLabel != null && name != originalLabel) {
            Text(
                stringResource(R.string.reset_name, originalLabel), style = XeType.Caption, color = scheme.textSecondary,
                modifier = Modifier.clickable { name = originalLabel }.padding(vertical = 4.dp),
            )
        }
        Text(stringResource(R.string.icon_style), style = XeType.Label, color = scheme.textSecondary, modifier = Modifier.padding(top = 10.dp))
        IconStylePicker(app, style, shape, { style = it })
        if (style != null) Text(
            stringResource(R.string.icon_style_default), style = XeType.Caption, color = scheme.textSecondary,
            modifier = Modifier.clickable { style = null }.padding(vertical = 4.dp),
        )
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassPillButton(stringResource(R.string.choose_picture), onPickImage, Modifier.weight(1f), icon = R.drawable.ti_photo)
            if (hasCustomIcon) GlassPillButton(stringResource(R.string.reset_picture), onResetImage, Modifier.weight(1f), icon = R.drawable.ti_refresh)
        }
        GlassPillButton(
            stringResource(R.string.save),
            { onSave(name.trim().takeIf { it.isNotEmpty() && it != originalLabel }, style) },
            Modifier.fillMaxWidth().padding(vertical = 12.dp), icon = R.drawable.ti_check,
        )
    }
}
