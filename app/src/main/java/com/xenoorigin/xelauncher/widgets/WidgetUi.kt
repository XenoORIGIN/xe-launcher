package com.xenoorigin.xelauncher.widgets

import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import com.xenoorigin.xelauncher.design.glassTile
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.design.GlassIconButton
import com.xenoorigin.xelauncher.design.GlassPillButton
import com.xenoorigin.xelauncher.design.GlassSheet
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.home.WidgetCell

/** Shows one home-screen widget: Xe glass widget, or another app's widget through the host. */
@Composable
fun WidgetContent(cell: WidgetCell, host: XeWidgetHost) {
    val kind = GlassWidgetKind.of(cell.kind)
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current
    if (kind != null) {
        // The widget lays itself out for the size it was given on Home.
        androidx.compose.runtime.CompositionLocalProvider(LocalWidgetSpan provides kind.clamp(Span(cell.spanX, cell.spanY))) { GlassWidget(kind, cell.id) }
        return
    }
    AndroidView(
        factory = { c -> host.viewFor(c, cell.widgetId) ?: TextView(c).apply { text = c.getString(R.string.widget_failed); setTextColor(0xB3FFFFFF.toInt()) } },
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)).onGloballyPositioned {
            bounds = it.boundsInWindow()
        },
        update = { v ->
            (v as? XeWidgetHostView)?.let { hv ->
                if (bounds.width > 0) with(density) { hv.setSizeDp(bounds.width.toDp().value, bounds.height.toDp().value) }
            }
        },
    )
}

/** Width and height steppers for a widget; [onResize] returns false when there isn't room. */
@Composable
fun WidgetResizeSheet(cell: WidgetCell?, columns: Int, rows: Int, onResize: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    val scheme = LocalXeScheme.current
    GlassSheet(cell != null, onDismiss) {
        if (cell == null) return@GlassSheet
        Text(stringResource(R.string.widget_resize), style = XeType.Title, color = scheme.text, modifier = Modifier.padding(vertical = 8.dp))
        @Composable
        fun stepper(label: Int, value: Int, max: Int, set: (Int) -> Unit) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(label), Modifier.weight(1f), style = XeType.Headline, color = scheme.text)
                GlassIconButton(R.drawable.ti_minus, "-", { if (value > 1) set(value - 1) })
                Box(Modifier.width(56.dp), contentAlignment = Alignment.Center) { Text("$value", style = XeType.Title, color = scheme.text) }
                GlassIconButton(R.drawable.ti_plus, "+", { if (value < max) set(value + 1) })
            }
        }
        stepper(R.string.widget_width, cell.spanX, columns) { onResize(it, cell.spanY) }
        stepper(R.string.widget_height, cell.spanY, rows) { onResize(cell.spanX, it) }
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassPillButton(stringResource(R.string.done), onDismiss, Modifier.weight(1f), icon = R.drawable.ti_check)
        }
    }
}

/** Write or change the text of a note widget. */
@Composable
fun NoteSheet(initial: String?, visible: Boolean, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val scheme = LocalXeScheme.current
    var text by remember(visible) { mutableStateOf(initial.orEmpty()) }
    GlassSheet(visible, onDismiss) {
        Text(stringResource(R.string.edit_note), style = XeType.Title, color = scheme.text, modifier = Modifier.padding(vertical = 8.dp))
        Box(
            Modifier.fillMaxWidth().padding(vertical = 8.dp).heightIn(min = 140.dp)
                .glassTile(RoundedCornerShape(18.dp), com.xenoorigin.xelauncher.design.LocalGlassStyle.current, androidx.compose.ui.graphics.Color.White.copy(alpha = .08f))
                .padding(16.dp),
        ) {
            androidx.compose.foundation.text.BasicTextField(
                text, { text = it.take(400) }, Modifier.fillMaxWidth(),
                textStyle = XeType.Body.copy(color = scheme.text), cursorBrush = androidx.compose.ui.graphics.SolidColor(scheme.text),
            )
        }
        GlassPillButton(stringResource(R.string.save), { onSave(text.trim()) }, Modifier.fillMaxWidth().padding(vertical = 10.dp), icon = R.drawable.ti_check)
    }
}
