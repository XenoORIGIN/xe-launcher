package com.xenoorigin.xelauncher.play

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.design.GlassIconButton
import com.xenoorigin.xelauncher.design.LocalGlassStyle
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.design.glassTile
import com.xenoorigin.xelauncher.design.pressSquish
import com.xenoorigin.xelauncher.icons.AppIcon
import com.xenoorigin.xelauncher.icons.IconShape
import com.xenoorigin.xelauncher.icons.IconStyle
import kotlinx.coroutines.delay

/** The apps whose name starts with [letter], most used first. */
fun appsForLetter(apps: List<AppEntry>, letter: Char, uses: Map<String, Int>): List<AppEntry> =
    apps.filter { firstLetterOf(it.label) == letter }
        .sortedWith(compareByDescending<AppEntry> { uses[it.key.encode()] ?: 0 }.thenBy { it.label.lowercase() })

/**
 * Draw the first letter of an app's name to open it. If one app matches it opens straight away;
 * otherwise the matches are offered, along with the next-best readings of the drawing.
 */
@Composable
fun DrawPad(
    apps: List<AppEntry>, uses: Map<String, Int>, style: (AppEntry) -> IconStyle, shape: IconShape,
    onLaunch: (AppEntry, Rect) -> Unit, onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val haptic = LocalHapticFeedback.current
    val recognizer = remember { LetterRecognizer() }
    val strokes = remember { mutableStateListOf<List<Pt>>() }
    var current by remember { mutableStateOf<List<Pt>>(emptyList()) }
    var penUp by remember { mutableIntStateOf(0) }
    var guesses by remember { mutableStateOf<List<Char>>(emptyList()) }
    var letter by remember { mutableStateOf<Char?>(null) }
    var missed by remember { mutableStateOf(false) }
    val ink = remember { Animatable(1f) }
    val readable = remember(apps) { apps.mapNotNull { firstLetterOf(it.label) }.toSet() }

    // Read the drawing once the finger has rested for a moment (time to add another stroke).
    LaunchedEffect(penUp) {
        if (penUp == 0 || strokes.isEmpty()) return@LaunchedEffect
        delay(560)
        val g = recognizer.recognize(strokes.toList()).filter { it in readable }
        guesses = g
        letter = g.firstOrNull()
        missed = g.isEmpty()
        haptic.performHapticFeedback(if (g.isEmpty()) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
        ink.animateTo(0f, tween(380)); strokes.clear(); ink.snapTo(1f)
    }
    val matches = remember(letter, apps, uses) { letter?.let { appsForLetter(apps, it, uses) }.orEmpty() }
    var only by remember { mutableStateOf<Rect?>(null) }
    LaunchedEffect(matches) {
        if (matches.size == 1) { delay(420); onLaunch(matches.first(), only ?: Rect.Zero) }
    }

    Box(Modifier.fillMaxSize().testTag("drawPad")) {
        Canvas(
            Modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (letter != null || missed) { letter = null; guesses = emptyList(); missed = false }
                    current = listOf(Pt(down.position.x, down.position.y))
                    while (true) {
                        val e = awaitPointerEvent()
                        val c = e.changes.firstOrNull { it.id == down.id } ?: break
                        if (!c.pressed) break
                        c.consume()
                        current = current + Pt(c.position.x, c.position.y)
                    }
                    if (current.size > 1) strokes.add(current)
                    current = emptyList()
                    penUp++
                }
            },
        ) {
            val a = ink.value
            fun path(s: List<Pt>) = Path().apply { s.firstOrNull()?.let { moveTo(it.x, it.y) }; s.drop(1).forEach { lineTo(it.x, it.y) } }
            (strokes + listOf(current)).filter { it.size > 1 }.forEach { s ->
                val p = path(s)
                drawPath(p, Color(0xFF9FD3FF).copy(alpha = .22f * a), style = Stroke(34f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(p, Color(0xFFBFE4FF).copy(alpha = .45f * a), style = Stroke(16f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(p, Color.White.copy(alpha = a), style = Stroke(7f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }

        Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 22.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.draw_title), style = XeType.Title, color = Color.White, modifier = Modifier.weight(1f))
                GlassIconButton(R.drawable.ti_x, stringResource(R.string.close), onClose)
            }
            Text(
                stringResource(if (missed) R.string.draw_missed else R.string.draw_hint),
                style = XeType.Body, color = Color.White.copy(alpha = .7f), modifier = Modifier.padding(top = 6.dp),
            )
        }

        // The letter that was read, big, then the apps it matches.
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(bottom = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(letter, transitionSpec = { (scaleIn(spring(dampingRatio = .5f, stiffness = 500f), initialScale = .4f) + fadeIn()) togetherWith fadeOut() }, label = "letter") { l ->
                if (l != null) Text(l.toString(), color = Color.White, fontSize = 92.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("drawnLetter"))
                else Spacer(Modifier.height(1.dp))
            }
            if (matches.isNotEmpty()) {
                Row(
                    Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    matches.take(12).forEachIndexed { i, app ->
                        var bounds by remember { mutableStateOf(Rect.Zero) }
                        val pop = remember(app.key) { Animatable(0f) }
                        LaunchedEffect(app.key) { delay(i * 35L); pop.animateTo(1f, spring(dampingRatio = .55f, stiffness = 420f)) }
                        val src = remember { MutableInteractionSource() }
                        AppIcon(
                            app, 58.dp, style(app), shape, true,
                            Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = pop.value.coerceIn(0f, 1f) }
                                .onGloballyPositioned { bounds = it.boundsInWindow(); if (i == 0) only = bounds }
                                .pressSquish(src).clickable(src, null) { onLaunch(app, bounds) },
                        )
                    }
                }
            }
            val others = guesses.drop(1).take(3)
            if (letter != null && others.isNotEmpty()) {
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.draw_or), style = XeType.Caption, color = Color.White.copy(alpha = .6f))
                    others.forEach { ch ->
                        Box(
                            Modifier.size(44.dp).glassTile(RoundedCornerShape(14.dp), LocalGlassStyle.current.copy(dark = true), Color.White.copy(alpha = .08f))
                                .clickable { letter = ch; guesses = listOf(ch) + guesses.filter { it != ch } }
                                .testTag("letterChoice"),
                            contentAlignment = Alignment.Center,
                        ) { Text(ch.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}
