package com.xenoorigin.xelauncher.settings.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.design.GlassIconButton
import com.xenoorigin.xelauncher.design.GlassPillButton
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.di.AppGraph
import com.xenoorigin.xelauncher.wallpaper.WallpaperAdjust
import com.xenoorigin.xelauncher.wallpaper.WallpaperPreset
import com.xenoorigin.xelauncher.wallpaper.WallpaperRenderer
import com.xenoorigin.xelauncher.wallpaper.WallpaperTarget
import com.xenoorigin.xelauncher.wallpaper.XeCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val PW = 270
private const val PH = 585

@Composable
fun WallpaperScreen(graph: AppGraph, back: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val current by graph.settings.settings.collectAsState(initial = null)
    var editing by remember { mutableStateOf<WallpaperPreset?>(null) }
    var offerLiveSky by remember { mutableStateOf(false) }
    var importsVersion by remember { mutableIntStateOf(0) }
    var importing by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            importing = true
            runCatching { graph.wallpaper.importFromUri(uri) }
                .onSuccess { editing = it; importsVersion++ }
                .onFailure { Toast.makeText(ctx, ctx.getString(R.string.import_failed), Toast.LENGTH_LONG).show() }
            importing = false
        }
    }
    val imports = remember(importsVersion) { graph.wallpaper.imports() }

    SettingsPage(stringResource(R.string.wallpaper), back) {
        Text(stringResource(R.string.wallpaper_note), style = XeType.Caption, color = LocalXeScheme.current.textSecondary, modifier = Modifier.padding(bottom = 12.dp))
        GlassPillButton(
            stringResource(if (importing) R.string.loading else R.string.from_gallery),
            { if (!importing) picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            Modifier.fillMaxWidth(), icon = R.drawable.ti_photo,
        )
        if (imports.isNotEmpty()) SettingsGroup(stringResource(R.string.your_photos)) {
            ThumbGrid(imports.map { WallpaperPreset.Image(it) }, current?.wallpaperId) { editing = it }
        }
        SettingsGroup(stringResource(R.string.xe_collection)) {
            ThumbGrid(XeCollection, current?.wallpaperId) { editing = it }
        }
    }
    com.xenoorigin.xelauncher.design.GlassSheet(offerLiveSky, { offerLiveSky = false }) {
        Text(stringResource(R.string.live_sky_offer_title), style = XeType.Title, color = LocalXeScheme.current.text, modifier = Modifier.padding(vertical = 8.dp))
        Text(stringResource(R.string.live_sky_offer_body), style = XeType.Body, color = LocalXeScheme.current.textSecondary)
        GlassPillButton(stringResource(R.string.live_sky_offer_yes), {
            offerLiveSky = false
            runCatching { ctx.startActivity(com.xenoorigin.xelauncher.wallpaper.LiveSkyWallpaper.pickerIntent(ctx)) }
        }, Modifier.fillMaxWidth().padding(top = 14.dp).testTag("live_sky_yes"), icon = R.drawable.ti_check)
        GlassPillButton(stringResource(R.string.not_now), { offerLiveSky = false }, Modifier.fillMaxWidth().padding(vertical = 10.dp))
    }
    editing?.let { p ->
        WallpaperEditor(
            p,
            onApply = { preset, adjust, target ->
                editing = null
                scope.launch {
                    if (preset is WallpaperPreset.Image) graph.wallpaper.applyImage(preset.file, adjust, target)
                    else graph.wallpaper.apply(preset, target, adjust)
                }
                // Offer the always-on-time system version of the live sky.
                if (preset is WallpaperPreset.Sky && !com.xenoorigin.xelauncher.wallpaper.LiveSkyWallpaper.isActive(ctx)) offerLiveSky = true
            },
            onDelete = if (p is WallpaperPreset.Image) ({ graph.wallpaper.deleteImport(p.file); importsVersion++; editing = null }) else null,
            onClose = { editing = null },
        )
    }
}

@Composable
private fun ThumbGrid(items: List<WallpaperPreset>, selectedId: String?, onPick: (WallpaperPreset) -> Unit) {
    items.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { p ->
                Column(Modifier.weight(1f)) {
                    val thumb by produceState<ImageBitmap?>(null, p.id) {
                        value = withContext(Dispatchers.Default) { runCatching { WallpaperRenderer.render(p, 135, 292).asImageBitmap() }.getOrNull() }
                    }
                    val sel = selectedId == p.id
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(135f / 292f).clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = .06f))
                            .border(if (sel) 2.5.dp else 0.5.dp, if (sel) Color.White else Color.White.copy(alpha = .2f), RoundedCornerShape(16.dp))
                            .clickable { onPick(p) },
                    ) { thumb?.let { Image(it, p.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) } }
                    if (p !is WallpaperPreset.Image) Text(p.name, style = XeType.Label, color = LocalXeScheme.current.textSecondary, modifier = Modifier.padding(top = 4.dp))
                }
            }
            repeat(3 - row.size) { Box(Modifier.weight(1f)) }
        }
    }
}

/** Live preview with framing (drag / pinch for photos), shuffle and glow for Xe designs, dim and blur for all. */
@Composable
private fun WallpaperEditor(
    start: WallpaperPreset,
    onApply: (WallpaperPreset, WallpaperAdjust, WallpaperTarget) -> Unit,
    onDelete: (() -> Unit)?,
    onClose: () -> Unit,
) {
    val scheme = LocalXeScheme.current
    var preset by remember { mutableStateOf(start) }
    var adjust by remember { mutableStateOf(WallpaperAdjust()) }
    var chooseTarget by remember { mutableStateOf(false) }
    BackHandler { if (chooseTarget) chooseTarget = false else onClose() }

    // Source picture for photos (decoded once, smaller than the screen) or the rendered design.
    val source by produceState<Bitmap?>(null, preset) {
        value = withContext(Dispatchers.Default) {
            runCatching {
                val p = preset
                if (p is WallpaperPreset.Image) BitmapFactory.decodeFile(p.file.path, BitmapFactory.Options().apply { inSampleSize = 2 })
                else WallpaperRenderer.render(p, PW * 2, PH * 2)
            }.getOrNull()
        }
    }
    val preview by produceState<ImageBitmap?>(null, source, adjust) {
        val src = source ?: return@produceState
        value = withContext(Dispatchers.Default) { WallpaperRenderer.compose(src, PW, PH, adjust).asImageBitmap() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black).clickable(enabled = false) {}) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(R.drawable.ti_arrow_left, stringResource(R.string.back), onClose)
                Text(stringResource(R.string.adjust_wallpaper), Modifier.weight(1f).padding(start = 14.dp), style = XeType.Title, color = scheme.text)
                if (onDelete != null) GlassIconButton(R.drawable.ti_trash, stringResource(R.string.delete), onDelete)
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.fillMaxHeight().aspectRatio(PW.toFloat() / PH).padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(28.dp)).border(1.dp, Color.White.copy(alpha = .25f), RoundedCornerShape(28.dp))
                        .pointerInput(preset) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val z = (adjust.zoom * zoom).coerceIn(1f, 4f)
                                // Dragging right reveals more of the left side.
                                adjust = adjust.copy(
                                    zoom = z,
                                    offsetX = (adjust.offsetX - pan.x / (size.width * .5f * z)).coerceIn(-1f, 1f),
                                    offsetY = (adjust.offsetY - pan.y / (size.height * .5f * z)).coerceIn(-1f, 1f),
                                )
                            }
                        },
                ) {
                    preview?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                }
            }
            Text(
                stringResource(if (preset is WallpaperPreset.Image) R.string.drag_to_frame else R.string.shuffle_hint),
                Modifier.fillMaxWidth().padding(top = 6.dp), style = XeType.Caption, color = scheme.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).verticalScroll(rememberScrollState())) {
                when (val p = preset) {
                    is WallpaperPreset.Mesh -> {
                        GlassPillButton(stringResource(R.string.shuffle), { preset = p.copy(seed = p.seed + 101) }, Modifier.fillMaxWidth().padding(top = 8.dp), icon = R.drawable.ti_refresh)
                        SliderRow(stringResource(R.string.glow), p.glow, .2f..1.4f, display = { "${(it * 100).toInt()}%" }) { preset = p.copy(glow = it) }
                    }
                    is WallpaperPreset.Abstract -> {
                        GlassPillButton(stringResource(R.string.shuffle), { preset = p.copy(seed = p.seed + 101) }, Modifier.fillMaxWidth().padding(top = 8.dp), icon = R.drawable.ti_refresh)
                        SliderRow(stringResource(R.string.glass_lenses), p.lenses.toFloat(), 0f..5f, steps = 4, display = { it.toInt().toString() }) { preset = p.copy(lenses = it.toInt()) }
                    }
                    is WallpaperPreset.Sky -> Text(stringResource(R.string.live_sky_desc), Modifier.padding(top = 10.dp), style = XeType.Caption, color = scheme.textSecondary)
                    is WallpaperPreset.Image -> SliderRow(stringResource(R.string.zoom), adjust.zoom, 1f..4f, display = { "%.1f×".format(it) }) { adjust = adjust.copy(zoom = it) }
                }
                SliderRow(stringResource(R.string.dim), adjust.dim, 0f..0.7f, display = { "${(it * 100).toInt()}%" }) { adjust = adjust.copy(dim = it) }
                SliderRow(stringResource(R.string.blur), adjust.blur, 0f..1f, display = { "${(it * 100).toInt()}%" }) { adjust = adjust.copy(blur = it) }
                GlassPillButton(stringResource(R.string.set_wallpaper), { chooseTarget = true }, Modifier.fillMaxWidth().padding(vertical = 10.dp), icon = R.drawable.ti_check)
            }
        }
    }
    com.xenoorigin.xelauncher.design.GlassSheet(chooseTarget, { chooseTarget = false }) {
        Text(stringResource(R.string.apply_wallpaper_to), style = XeType.Title, color = scheme.text, modifier = Modifier.padding(vertical = 8.dp))
        listOf(WallpaperTarget.HOME to R.string.apply_home, WallpaperTarget.LOCK to R.string.apply_lock, WallpaperTarget.BOTH to R.string.apply_both).forEach { (t, l) ->
            ChoiceRow(stringResource(l), false) { chooseTarget = false; onApply(preset, adjust, t) }
        }
    }
}
