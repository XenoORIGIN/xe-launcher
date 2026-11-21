package com.xenoorigin.xestudio.icons

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xenoorigin.xestudio.R
import com.xenoorigin.xestudio.core.Fill
import com.xenoorigin.xestudio.core.IconRecipe
import com.xenoorigin.xestudio.core.IconRenderer
import com.xenoorigin.xestudio.core.TileShape
import com.xenoorigin.xestudio.core.Treatment
import com.xenoorigin.xestudio.core.tilePath
import com.xenoorigin.xestudio.design.Ink
import com.xenoorigin.xestudio.design.LocalStudioHaze
import com.xenoorigin.xestudio.design.Motion
import com.xenoorigin.xestudio.design.StudioType
import com.xenoorigin.xestudio.design.glassPanel
import com.xenoorigin.xestudio.design.pressSquish
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Colour presets. Studio's own chrome stays black and white; these are for the user's theme. */
internal object Swatches {
    val backgrounds = listOf(
        Fill(listOf(0xFF111111.toInt())), Fill(listOf(0xFFFFFFFF.toInt())), Fill(listOf(0xFF2B2B2B.toInt())),
        Fill(listOf(0xFF14213D.toInt())), Fill(listOf(0xFF1F3A2E.toInt())), Fill(listOf(0xFF4A1C2C.toInt())), Fill(listOf(0xFFE9DFCF.toInt())),
        Fill(listOf(0xFFFF7E5F.toInt(), 0xFFFEB47B.toInt())), Fill(listOf(0xFF2193B0.toInt(), 0xFF6DD5ED.toInt())),
        Fill(listOf(0xFF7F00FF.toInt(), 0xFFE100FF.toInt())), Fill(listOf(0xFF0F2027.toInt(), 0xFF2C5364.toInt())),
    )
    val tints = listOf(
        0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF9BE7C4.toInt(), 0xFFFF8A80.toInt(),
        0xFF8AB4F8.toInt(), 0xFFFFD166.toInt(), 0xFFC3A6FF.toInt(), 0xFF888888.toInt(),
    )
}

internal fun Fill.brush(): Brush = if (colors.size == 1) Brush.linearGradient(listOf(Color(colors[0]), Color(colors[0])))
else Brush.linearGradient(colors.map { Color(it) })

@Composable
fun IconEditorScreen(state: IconEditorState, apps: AppSource, onBack: () -> Unit, onApply: () -> Unit) {
    val recipe by state.recipe.collectAsState()
    val imageVersion by state.imageVersion.collectAsState()
    val list by produceState<List<InstalledApp>?>(null, apps) { value = apps.apps() }
    var editing by remember { mutableStateOf<InstalledApp?>(null) }
    val scope = rememberCoroutineScope()
    val haze = LocalStudioHaze.current
    BackHandler(editing == null) { scope.launch { state.flush() }; onBack() }

    Box(Modifier.fillMaxSize().background(Ink.Black)) {
        Column(Modifier.fillMaxSize().let { if (haze != null) it.hazeSource(haze) else it }) {
            TopBar(state.name, onBack = { scope.launch { state.flush() }; onBack() }, onApply = { scope.launch { state.flush(); onApply() } })
            // Live preview: the phone's real apps in the theme being made.
            PreviewGrid(list.orEmpty(), recipe, imageVersion, state, Modifier.fillMaxWidth().weight(1f)) { editing = it }
            Controls(recipe, state::update, Modifier.fillMaxWidth().heightIn(max = 360.dp))
        }
        AnimatedVisibility(
            editing != null,
            enter = fadeIn(Motion.settle()) + slideInVertically(Motion.settle()) { it / 3 },
            exit = fadeOut(Motion.snappy()) + slideOutVertically(Motion.snappy()) { it / 3 },
        ) {
            editing?.let { app -> OverrideSheet(app, state, onClose = { editing = null }) }
        }
    }
}

@Composable
private fun TopBar(name: String, onBack: () -> Unit, onApply: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(42.dp).pressSquish().clip(CircleShape).background(Ink.Card).clickable(onClick = onBack).testTag("editor_back"),
            contentAlignment = Alignment.Center,
        ) { Text("‹", style = StudioType.Title) }
        Spacer(Modifier.width(12.dp))
        Text(name, style = StudioType.Title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(
            Modifier.height(42.dp).pressSquish().clip(RoundedCornerShape(21.dp)).background(Ink.White)
                .clickable(onClick = onApply).padding(horizontal = 18.dp).testTag("apply_xe"),
            contentAlignment = Alignment.Center,
        ) { Text(stringResource(R.string.use_in_xe), style = StudioType.Label.copy(color = Ink.Black)) }
    }
}

@Composable
private fun PreviewGrid(
    apps: List<InstalledApp>, recipe: IconRecipe, imageVersion: Int, state: IconEditorState,
    modifier: Modifier, onTap: (InstalledApp) -> Unit,
) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.tap_icon_hint), style = StudioType.Label)
        apps.chunked(4).forEachIndexed { row, chunk ->
            Row(Modifier.fillMaxWidth()) {
                chunk.forEachIndexed { col, app ->
                    IconTile(app, recipe, imageVersion, state, row * 4 + col, Modifier.weight(1f)) { onTap(app) }
                }
                repeat(4 - chunk.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun IconTile(app: InstalledApp, recipe: IconRecipe, imageVersion: Int, state: IconEditorState, index: Int, modifier: Modifier, onTap: () -> Unit) {
    val px = with(LocalDensity.current) { 60.dp.roundToPx() }
    val bmp by produceState<android.graphics.Bitmap?>(null, app.key, recipe, imageVersion) {
        value = withContext(Dispatchers.Default) { IconRenderer.render(recipe, app.key, app.label, app.icon, px, state::loadImage) }
    }
    // Each change ripples across the grid: tiles pop in turn, top-left first.
    val pop = remember { Animatable(1f) }
    LaunchedEffect(recipe) {
        delay((index % 24) * 14L)
        pop.snapTo(.86f); pop.animateTo(1f, Motion.settle())
    }
    Column(
        modifier.pressSquish().clickable(onClick = onTap).testTag("icon_${app.key}"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(60.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
            bmp?.let { Image(it.asImageBitmap(), app.label, Modifier.fillMaxSize()) }
        }
        Spacer(Modifier.height(6.dp))
        Text(app.label, style = StudioType.Label.copy(color = Ink.Soft), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Controls(recipe: IconRecipe, update: ((IconRecipe) -> IconRecipe) -> Unit, modifier: Modifier) {
    Column(
        modifier.navigationBarsPadding().padding(10.dp).glassPanel(LocalStudioHaze.current, RoundedCornerShape(28.dp))
            .verticalScroll(rememberScrollState()).padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Section(R.string.ctl_shape) {
            for (s in TileShape.entries) {
                Chip(recipe.shape == s, "ctl_shape_${s.name}", { update { it.copy(shape = s) } }) { ShapeGlyph(s, if (recipe.shape == s) Ink.Black else Ink.White) }
            }
        }
        Section(R.string.ctl_background) {
            for ((i, f) in Swatches.backgrounds.withIndex()) {
                Swatch(f.brush(), recipe.background == f, "ctl_bg_$i") { update { it.copy(background = f) } }
            }
        }
        Section(R.string.ctl_treatment) {
            val names = mapOf(Treatment.ORIGINAL to R.string.tr_original, Treatment.MONO to R.string.tr_mono, Treatment.TINT to R.string.tr_tint, Treatment.DUOTONE to R.string.tr_duotone)
            for (t in Treatment.entries) {
                Chip(recipe.treatment == t, "ctl_treatment_${t.name}", { update { it.copy(treatment = t) } }) {
                    Text(stringResource(names.getValue(t)), style = StudioType.Label.copy(color = if (recipe.treatment == t) Ink.Black else Ink.White))
                }
            }
        }
        if (recipe.treatment == Treatment.TINT || recipe.treatment == Treatment.DUOTONE) {
            Section(R.string.ctl_tint) {
                for ((i, c) in Swatches.tints.withIndex()) Swatch(Brush.linearGradient(listOf(Color(c), Color(c))), recipe.tint == c, "ctl_tint_$i") { update { it.copy(tint = c) } }
            }
        }
        LabeledSlider(R.string.ctl_glyph, recipe.glyphScale, .35f..1f, "ctl_glyph") { v -> update { it.copy(glyphScale = v) } }
        LabeledSlider(R.string.ctl_outline, recipe.outlineWidth, 0f..4f, "ctl_outline") { v -> update { it.copy(outlineWidth = v) } }
        LabeledSlider(R.string.ctl_shadow, recipe.shadow, 0f..1f, "ctl_shadow") { v -> update { it.copy(shadow = v) } }
    }
}

@Composable
private fun Section(title: Int, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(stringResource(title), style = StudioType.Label, modifier = Modifier.padding(start = 18.dp, bottom = 6.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun Chip(on: Boolean, tag: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.height(40.dp).pressSquish().clip(RoundedCornerShape(20.dp)).background(if (on) Ink.White else Ink.Card)
            .clickable(onClick = onClick).padding(horizontal = 12.dp).testTag(tag),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Swatch(brush: Brush, on: Boolean, tag: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).pressSquish().clip(CircleShape).background(brush)
            .border(if (on) 3.dp else 1.dp, if (on) Ink.White else Ink.Line, CircleShape).clickable(onClick = onClick).testTag(tag),
    )
}

@Composable
private fun ShapeGlyph(shape: TileShape, color: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        drawPath(tilePath(shape, this.size.minDimension).asComposePath(), color, style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
private fun LabeledSlider(title: Int, value: Float, range: ClosedFloatingPointRange<Float>, tag: String, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
        Text(stringResource(title), style = StudioType.Label)
        Slider(
            value, onChange, valueRange = range, modifier = Modifier.testTag(tag),
            colors = SliderDefaults.colors(thumbColor = Ink.White, activeTrackColor = Ink.White, inactiveTrackColor = Ink.Line),
        )
    }
}
