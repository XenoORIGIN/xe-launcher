package com.xenoorigin.xelauncher.search

import android.content.pm.ShortcutInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.design.GlassIconButton
import com.xenoorigin.xelauncher.design.GlassSurface
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.design.pressSquish
import com.xenoorigin.xelauncher.icons.AppIcon
import com.xenoorigin.xelauncher.icons.IconShape
import com.xenoorigin.xelauncher.icons.IconStyle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchPage(
    apps: List<AppEntry>,
    shortcuts: () -> List<ShortcutInfo>,
    iconStyle: (AppEntry) -> IconStyle,
    iconShape: IconShape,
    calculator: Boolean,
    web: Boolean,
    autoKeyboard: Boolean,
    onLaunch: (AppEntry, Rect) -> Unit,
    onLongPress: (AppEntry, Rect) -> Unit,
    onShortcut: (ShortcutInfo) -> Unit,
    onWeb: (String) -> Unit,
    onClose: () -> Unit,
) {
    val scheme = LocalXeScheme.current
    var query by remember { mutableStateOf("") }
    val allShortcuts = remember { runCatching(shortcuts).getOrDefault(emptyList()) }
    val results = remember(query, apps) { SearchEngine.search(query, apps, allShortcuts, calculator, web) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (autoKeyboard) runCatching { focus.requestFocus() } }
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassSurface(RoundedCornerShape(26.dp), Modifier.weight(1f).height(52.dp)) {
                Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ti_search), null, Modifier.size(20.dp), tint = scheme.textSecondary)
                    Box(Modifier.weight(1f).padding(start = 10.dp)) {
                        if (query.isEmpty()) Text(stringResource(R.string.search_hint), style = XeType.Body, color = scheme.textSecondary)
                        BasicTextField(
                            query, { query = it }, Modifier.fillMaxWidth().focusRequester(focus),
                            textStyle = XeType.Body.copy(color = scheme.text), singleLine = true, cursorBrush = SolidColor(scheme.text),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                when (val first = results.firstOrNull()) {
                                    is SearchResult.App -> onLaunch(first.entry, Rect.Zero)
                                    is SearchResult.Web -> onWeb(first.query)
                                    else -> Unit
                                }
                            }),
                        )
                    }
                    if (query.isNotEmpty()) Icon(painterResource(R.drawable.ti_x), stringResource(R.string.cancel),
                        Modifier.size(20.dp).clickable { query = "" }, tint = scheme.textSecondary)
                }
            }
            GlassIconButton(R.drawable.ti_x, stringResource(R.string.cancel), onClose, Modifier.padding(start = 10.dp), size = 46.dp)
        }
        val appResults = results.filterIsInstance<SearchResult.App>()
        LazyVerticalGrid(GridCells.Fixed(4), Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            results.filterIsInstance<SearchResult.Calc>().forEach { c ->
                item(span = { GridItemSpan(4) }) {
                    GlassSurface(RoundedCornerShape(22.dp), Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
                        Column(Modifier.padding(18.dp)) {
                            Text(stringResource(R.string.search_calc), style = XeType.Label, color = scheme.textSecondary)
                            Text("${c.expr} =", style = XeType.Body, color = scheme.textSecondary)
                            Text(c.value, style = XeType.LargeTitle, color = scheme.text)
                        }
                    }
                }
            }
            items(appResults, key = { "a" + it.entry.key.encode() }) { r ->
                var bounds = remember { Rect.Zero }
                val src = remember { MutableInteractionSource() }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    AppIcon(r.entry, 56.dp, iconStyle(r.entry), iconShape, true,
                        Modifier.onGloballyPositioned { bounds = it.boundsInWindow() }.pressSquish(src)
                            .combinedClickable(src, null, onLongClick = { onLongPress(r.entry, bounds) }) { onLaunch(r.entry, bounds) })
                }
            }
            if (query.isNotBlank() && appResults.isEmpty()) item(span = { GridItemSpan(4) }) {
                Text(stringResource(R.string.no_results, query.trim()), Modifier.padding(16.dp), style = XeType.Body, color = scheme.textSecondary)
            }
            results.filterIsInstance<SearchResult.Shortcut>().forEach { s ->
                item(span = { GridItemSpan(4) }) {
                    ResultRow(R.drawable.ti_bolt, s.label) { onShortcut(s.info) }
                }
            }
            results.filterIsInstance<SearchResult.Web>().forEach { w ->
                item(span = { GridItemSpan(4) }) { ResultRow(R.drawable.ti_world, stringResource(R.string.search_web, w.query)) { onWeb(w.query) } }
            }
        }
    }
}

@Composable
private fun ResultRow(icon: Int, text: String, onClick: () -> Unit) {
    val scheme = LocalXeScheme.current
    val src = remember { MutableInteractionSource() }
    GlassSurface(RoundedCornerShape(20.dp), Modifier.fillMaxWidth().padding(horizontal = 6.dp).height(56.dp).pressSquish(src).clickable(src, null, onClick = onClick)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), null, Modifier.size(22.dp), tint = scheme.text)
            Text(text, Modifier.padding(start = 14.dp), style = XeType.Headline, color = scheme.text, maxLines = 1)
        }
    }
}
