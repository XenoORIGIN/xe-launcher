package com.xenoorigin.xestudio.icons

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenoorigin.xestudio.R
import com.xenoorigin.xestudio.core.Fill
import com.xenoorigin.xestudio.core.IconOverride
import com.xenoorigin.xestudio.core.IconRenderer
import com.xenoorigin.xestudio.design.Ink
import com.xenoorigin.xestudio.design.LocalStudioHaze
import com.xenoorigin.xestudio.design.Motion
import com.xenoorigin.xestudio.design.StudioType
import com.xenoorigin.xestudio.design.glassPanel
import com.xenoorigin.xestudio.design.pressSquish
import com.xenoorigin.xestudio.images.ImageImport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Mode { MENU, LETTER, DRAW }

/** Changes one app's icon: a picture, a letter or symbol, a drawing, or back to the theme's style. */
@Composable
fun OverrideSheet(app: InstalledApp, state: IconEditorState, onClose: () -> Unit) {
    var mode by remember { mutableStateOf(Mode.MENU) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val recipe by state.recipe.collectAsState()
    BackHandler { if (mode == Mode.MENU) onClose() else mode = Mode.MENU }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val bytes = withContext(Dispatchers.IO) { ImageImport.decode(ctx, uri)?.let(ImageImport::normalize) }
            if (bytes != null) { state.setOverride(app.key, IconOverride.Image(state.saveImage(bytes))); onClose() }
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(10.dp)
                .glassPanel(LocalStudioHaze.current, RoundedCornerShape(30.dp))
                .clickable(remember { MutableInteractionSource() }, null) {}
                .padding(20.dp).testTag("override_sheet"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val px = with(LocalDensity.current) { 84.dp.roundToPx() }
            val preview by produceState<android.graphics.Bitmap?>(null, recipe, app.key) {
                value = withContext(Dispatchers.Default) { IconRenderer.render(recipe, app.key, app.label, app.icon, px, state::loadImage) }
            }
            Box(Modifier.size(84.dp)) { preview?.let { Image(it.asImageBitmap(), app.label, Modifier.fillMaxSize()) } }
            Spacer(Modifier.height(8.dp))
            Text(app.label, style = StudioType.Title)
            Spacer(Modifier.height(16.dp))
            AnimatedContent(mode, transitionSpec = { fadeIn(Motion.settle()) togetherWith fadeOut(Motion.snappy()) }, label = "mode") { m ->
                when (m) {
                    Mode.MENU -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SheetButton(stringResource(R.string.ov_image), "ov_image") { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        SheetButton(stringResource(R.string.ov_letter), "ov_letter") { mode = Mode.LETTER }
                        SheetButton(stringResource(R.string.ov_draw), "ov_draw") { mode = Mode.DRAW }
                        if (recipe.overrides.containsKey(app.key)) {
                            SheetButton(stringResource(R.string.ov_reset), "ov_reset", quiet = true) { state.setOverride(app.key, null); onClose() }
                        }
                    }
                    Mode.LETTER -> LetterEditor(app) { o -> state.setOverride(app.key, o); onClose() }
                    Mode.DRAW -> DrawEditor(recipe.background, recipe.tint) { bmp ->
                        scope.launch {
                            val bytes = withContext(Dispatchers.Default) { ImageImport.normalize(bmp) }
                            state.setOverride(app.key, IconOverride.Image(state.saveImage(bytes))); onClose()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetButton(text: String, tag: String, quiet: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(52.dp).pressSquish().clip(RoundedCornerShape(26.dp))
            .background(if (quiet) Color.Transparent else Ink.White).border(1.dp, if (quiet) Ink.Line else Color.Transparent, RoundedCornerShape(26.dp))
            .clickable(onClick = onClick).testTag(tag),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = StudioType.Title.copy(color = if (quiet) Ink.White else Ink.Black, fontSize = 15.sp)) }
}

@Composable
private fun LetterEditor(app: InstalledApp, onDone: (IconOverride.Letter) -> Unit) {
    var text by remember { mutableStateOf(app.label.take(1).uppercase()) }
    var color by remember { mutableIntStateOf(Swatches.tints[0]) }
    var bg by remember { mutableIntStateOf(0) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicTextField(
            text, { text = it.take(2) }, singleLine = true,
            textStyle = StudioType.Display.copy(textAlign = TextAlign.Center, color = Ink.White),
            cursorBrush = SolidColor(Ink.White),
            modifier = Modifier.size(120.dp, 64.dp).clip(RoundedCornerShape(18.dp)).background(Ink.Card).padding(10.dp).testTag("letter_text"),
        )
        SwatchRow(Swatches.tints.map { Brush.linearGradient(listOf(Color(it), Color(it))) }, Swatches.tints.indexOf(color)) { color = Swatches.tints[it] }
        SwatchRow(Swatches.backgrounds.map { it.brush() }, bg) { bg = it }
        SheetButton(stringResource(R.string.ov_save), "letter_save") {
            if (text.isNotBlank()) onDone(IconOverride.Letter(text.trim(), color, Swatches.backgrounds[bg]))
        }
    }
}

@Composable
private fun SwatchRow(brushes: List<Brush>, selected: Int, onPick: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        brushes.forEachIndexed { i, b ->
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(b)
                    .border(if (i == selected) 3.dp else 1.dp, if (i == selected) Ink.White else Ink.Line, CircleShape)
                    .clickable { onPick(i) },
            )
        }
    }
}

@Composable
private fun DrawEditor(background: Fill, ink: Int, onDone: (android.graphics.Bitmap) -> Unit) {
    val sketch = remember { Sketch() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DrawPad(sketch, background.brush(), Color(ink), Modifier.padding(horizontal = 24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { SheetButton(stringResource(R.string.ov_clear), "draw_clear", quiet = true) { sketch.clear() } }
            Box(Modifier.weight(1f)) {
                SheetButton(stringResource(R.string.ov_save), "draw_save") {
                    if (!sketch.isEmpty) onDone(sketch.render(ImageImport.MAX_SIDE, background.colors.firstOrNull() ?: 0xFF000000.toInt(), ink))
                }
            }
        }
    }
}
