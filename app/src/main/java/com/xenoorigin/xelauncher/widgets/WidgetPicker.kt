package com.xenoorigin.xelauncher.widgets

import com.xenoorigin.xelauncher.design.appear

import android.appwidget.AppWidgetProviderInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.design.GlassIconButton
import com.xenoorigin.xelauncher.design.LocalGlassStyle
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.design.glassTile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What the user picked: one of Xe's widgets or an app's widget. */
sealed interface WidgetChoice {
    data class Glass(val kind: GlassWidgetKind) : WidgetChoice
    data class Provider(val info: AppWidgetProviderInfo) : WidgetChoice
}

@Composable
fun WidgetPicker(host: XeWidgetHost, cellSize: (AppWidgetProviderInfo) -> Pair<Int, Int>, onPick: (WidgetChoice) -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scheme = LocalXeScheme.current
    BackHandler(onBack = onClose)
    val groups by produceState<List<Pair<String, List<AppWidgetProviderInfo>>>?>(null) {
        value = withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            host.providers().groupBy { it.provider.packageName }.map { (pkg, list) ->
                val app = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
                app to list.sortedBy { runCatching { it.loadLabel(pm) }.getOrDefault("") }
            }.sortedWith(compareBy(java.text.Collator.getInstance()) { it.first })
        }
    }
    Box(Modifier.fillMaxSize().background(Color(0xF2050507)).clickable(enabled = false) {}) {
        Column(Modifier.fillMaxSize().appear(0, distance = 120f, scaleFrom = .98f).statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(R.drawable.ti_arrow_left, stringResource(R.string.back), onClose)
                Text(stringResource(R.string.widgets), Modifier.weight(1f).padding(start = 14.dp), style = XeType.Title, color = scheme.text)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text(stringResource(R.string.xe_widgets), style = XeType.Label, color = scheme.textSecondary) }
                // Previews at their real proportions: wide widgets on their own row, small ones in pairs.
                val rows = buildList<List<GlassWidgetKind>> {
                    val small = ArrayList<GlassWidgetKind>()
                    GlassWidgetKind.entries.forEach { k -> if (k.spanX >= 4) add(listOf(k)) else { small += k; if (small.size == 2) { add(small.toList()); small.clear() } } }
                    if (small.isNotEmpty()) add(small.toList())
                }
                rows.forEachIndexed { index, row ->
                    item(key = "xe$index") {
                        val appear = remember { androidx.compose.animation.core.Animatable(0f) }
                        LaunchedEffect(Unit) { kotlinx.coroutines.delay(index * 45L); appear.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = .7f, stiffness = 300f)) }
                        Row(
                            Modifier.fillMaxWidth().graphicsLayer { alpha = appear.value; translationY = (1f - appear.value) * 60f },
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { k ->
                                Column(Modifier.weight(k.spanX.toFloat()).clickable { onPick(WidgetChoice.Glass(k)) }) {
                                    Box(Modifier.fillMaxWidth().aspectRatio(k.spanX / (k.spanY * 1.05f))) { GlassWidget(k) }
                                    Text(stringResource(k.title), style = XeType.Label, color = scheme.text, modifier = Modifier.padding(top = 6.dp, start = 6.dp))
                                }
                            }
                            if (row.size == 1 && row[0].spanX < 4) Box(Modifier.weight(2f))
                        }
                    }
                }
                item { Text(stringResource(R.string.app_widgets), style = XeType.Label, color = scheme.textSecondary, modifier = Modifier.padding(top = 12.dp)) }
                val g = groups
                if (g == null) item { Text(stringResource(R.string.loading), color = scheme.textSecondary) }
                else if (g.isEmpty()) item { Text(stringResource(R.string.no_widgets), color = scheme.textSecondary) }
                else g.forEach { (app, list) ->
                    item(key = "h$app") { Text(app, style = XeType.Headline, color = scheme.text, modifier = Modifier.padding(top = 6.dp)) }
                    items(list, key = { it.provider.flattenToString() + it.profile.hashCode() }) { info ->
                        ProviderRow(info, cellSize(info)) { onPick(WidgetChoice.Provider(info)) }
                    }
                }
                item { Box(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun ProviderRow(info: AppWidgetProviderInfo, span: Pair<Int, Int>, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val scheme = LocalXeScheme.current
    val density = ctx.resources.displayMetrics.densityDpi
    val preview by produceState<ImageBitmap?>(null, info) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                (info.loadPreviewImage(ctx, density) ?: info.loadIcon(ctx, density))?.let { d ->
                    val w = d.intrinsicWidth.coerceIn(1, 480); val h = d.intrinsicHeight.coerceIn(1, 480)
                    d.toBitmap(w, h).asImageBitmap()
                }
            }.getOrNull()
        }
    }
    val label = remember(info) { runCatching { info.loadLabel(ctx.packageManager) }.getOrDefault("") }
    Row(
        Modifier.fillMaxWidth().glassTile(RoundedCornerShape(20.dp), LocalGlassStyle.current, Color.White.copy(alpha = .07f))
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(96.dp).height(72.dp).clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            preview?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(label, style = XeType.Body, color = scheme.text, maxLines = 2)
            Text("${span.first} × ${span.second}", style = XeType.Caption, color = scheme.textSecondary)
        }
    }
}
