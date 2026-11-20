package com.xenoorigin.xelauncher.settings.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.design.GlassPillButton
import com.xenoorigin.xelauncher.design.GlassSheet
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.di.AppGraph
import com.xenoorigin.xelauncher.wallpaper.WallpaperPreset
import com.xenoorigin.xelauncher.wallpaper.WallpaperRenderer
import com.xenoorigin.xelauncher.wallpaper.WallpaperTarget
import com.xenoorigin.xelauncher.wallpaper.XeCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WallpaperScreen(graph: AppGraph, back: () -> Unit) {
    val scope = rememberCoroutineScope()
    val current by graph.settings.settings.collectAsState(initial = null)
    var pending by remember { mutableStateOf<WallpaperPreset?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { pending = graph.wallpaper.importFromUri(uri) }
    }
    SettingsPage(stringResource(R.string.wallpaper), back) {
        Text(stringResource(R.string.wallpaper_note), style = XeType.Caption, color = LocalXeScheme.current.textSecondary, modifier = Modifier.padding(bottom = 12.dp))
        GlassPillButton(stringResource(R.string.from_gallery), { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            Modifier.fillMaxWidth(), icon = R.drawable.ti_photo)
        SettingsGroup(stringResource(R.string.xe_collection)) {
            XeCollection.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { p ->
                        Column(Modifier.weight(1f)) {
                            val thumb by produceState<ImageBitmap?>(null, p.id) { value = withContext(Dispatchers.Default) { WallpaperRenderer.render(p, 135, 292).asImageBitmap() } }
                            val sel = current?.wallpaperId == p.id
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(135f / 292f).clip(RoundedCornerShape(16.dp))
                                    .border(if (sel) 2.5.dp else 0.5.dp, if (sel) Color.White else Color.White.copy(alpha = .2f), RoundedCornerShape(16.dp))
                                    .clickable { pending = p },
                            ) { thumb?.let { Image(it, p.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth()) } }
                            Text(p.name, style = XeType.Label, color = LocalXeScheme.current.textSecondary, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
    GlassSheet(pending != null, { pending = null }) {
        Text(stringResource(R.string.apply_wallpaper_to), style = XeType.Title, color = LocalXeScheme.current.text, modifier = Modifier.padding(vertical = 8.dp))
        listOf(WallpaperTarget.HOME to R.string.apply_home, WallpaperTarget.LOCK to R.string.apply_lock, WallpaperTarget.BOTH to R.string.apply_both).forEach { (t, l) ->
            ChoiceRow(stringResource(l), false) {
                val p = pending ?: return@ChoiceRow
                pending = null
                scope.launch { graph.wallpaper.apply(p, t) }
            }
        }
    }
}
