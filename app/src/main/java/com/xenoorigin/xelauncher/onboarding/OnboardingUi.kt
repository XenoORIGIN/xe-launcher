package com.xenoorigin.xelauncher.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.design.GlassPillButton
import com.xenoorigin.xelauncher.design.GlassSurface
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.settings.AppLanguage
import com.xenoorigin.xelauncher.settings.OnboardingStage
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Length of the opening animation. */
const val INTRO_MS = 2600
const val TUTORIAL_SLIDES = 4

private fun ease(p: Float) = 1f - (1f - p).let { it * it * it * it }
private fun part(ms: Float, from: Int, len: Int) = ((ms - from) / len).coerceIn(0f, 1f)

private class Shard(val angle: Float, val dist: Float, val spin: Float, val delay: Int, val hue: Float, val seed: Int)

/** A slightly broken square, so the pieces read as shards of glass. */
private fun shardShape(seed: Int) = GenericShape { size, _ ->
    val r = Random(seed); fun j(max: Float) = r.nextFloat() * max
    moveTo(j(size.width * .1f), j(size.height * .1f))
    lineTo(size.width - j(size.width * .1f), j(size.height * .12f))
    lineTo(size.width - j(size.width * .12f), size.height - j(size.height * .1f))
    lineTo(j(size.width * .12f), size.height - j(size.height * .1f))
    close()
}

/** Glass shards fly in and gather into the Xe icon, light runs across it, then the name appears. Tap to skip. */
@Composable
fun IntroAnimation(onDone: () -> Unit, modifier: Modifier = Modifier) {
    val done by rememberUpdatedState(onDone)
    var finished by remember { mutableStateOf(false) }
    fun finish() { if (!finished) { finished = true; done() } }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(INTRO_MS.toFloat(), tween(INTRO_MS, easing = LinearEasing)); finish() }
    val shards = remember {
        val r = Random(42)
        List(16) { Shard(r.nextFloat() * 2f * PI.toFloat(), 700f + r.nextFloat() * 900f, (r.nextFloat() - .5f) * 520f, r.nextInt(0, 380), 180f + r.nextFloat() * 140f, r.nextInt()) }
    }
    Box(
        modifier.fillMaxSize().background(Color.Black).testTag("intro")
            .clickable(remember { MutableInteractionSource() }, null) { finish() },
        contentAlignment = Alignment.Center,
    ) {
        val ms = t.value
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(128.dp)) {
                val cell = 32.dp
                shards.forEachIndexed { i, s ->
                    val p = ease(part(ms, s.delay, 1050))
                    val shape = remember(s.seed) { shardShape(s.seed) }
                    val hue = Color.hsv(s.hue, .6f, 1f)
                    Box(
                        Modifier.offset(cell * (i % 4), cell * (i / 4)).size(cell + 1.dp)
                            .graphicsLayer {
                                translationX = cos(s.angle) * s.dist * (1f - p); translationY = sin(s.angle) * s.dist * (1f - p)
                                rotationZ = s.spin * (1f - p); scaleX = 1f + .6f * (1f - p); scaleY = scaleX
                                alpha = if (ms < 1350) (part(ms, s.delay, 250)) else 1f - part(ms, 1350, 350)
                            }
                            .clip(shape)
                            .background(Brush.linearGradient(listOf(Color.White.copy(alpha = .42f), hue.copy(alpha = .14f), Color.White.copy(alpha = .06f))))
                            .border(1.dp, Color.White.copy(alpha = .55f), shape),
                    )
                }
                val lp = part(ms, 1300, 450)
                val pop = if (lp < 1f) .88f + .16f * sin(lp * PI.toFloat() * .75f) else 1f
                Box(
                    Modifier.fillMaxSize()
                        .graphicsLayer { alpha = lp; scaleX = pop; scaleY = pop }
                        .clip(RoundedCornerShape(30.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF3A3A40), Color(0xFF0E0E10))))
                        .border(1.dp, Color.White.copy(alpha = .35f), RoundedCornerShape(30.dp))
                        .drawWithContent {
                            drawContent()
                            val sp = part(ms, 1550, 800)
                            if (sp > 0f && sp < 1f) {
                                val x = -size.width * .6f + size.width * 2f * sp
                                rotate(18f) {
                                    drawRect(
                                        Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = .7f), Color.Transparent), startX = x, endX = x + size.width * .45f),
                                        Offset(x, -size.height * .4f), Size(size.width * .45f, size.height * 1.8f),
                                    )
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) { Text("Xe", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp) }
            }
            Spacer(Modifier.height(22.dp))
            val np = part(ms, 1750, 450)
            Text("Xe Launcher", color = Color.White, style = XeType.Title, modifier = Modifier.graphicsLayer { alpha = np; translationY = (1f - np) * 24f })
        }
    }
}

/** Two big glass cards, one per language, labelled in both languages so anyone can read them. */
@Composable
fun LanguageStep(onPick: (AppLanguage) -> Unit, modifier: Modifier = Modifier) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(700)) }
    Column(
        modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("เลือกภาษา\nChoose your language", style = XeType.LargeTitle.copy(fontSize = 30.sp, lineHeight = 38.sp), color = Color.White, textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { alpha = appear.value; translationY = (1f - appear.value) * 40f })
        Spacer(Modifier.height(34.dp))
        listOf(Triple(AppLanguage.TH, "ไทย", "Thai"), Triple(AppLanguage.EN, "English", "อังกฤษ")).forEachIndexed { i, (lang, name, other) ->
            val p = ((appear.value - .2f * (i + 1)) / .6f).coerceIn(0f, 1f)
            GlassSurface(
                RoundedCornerShape(28.dp),
                Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    .graphicsLayer { alpha = p; translationY = (1f - p) * 80f; rotationX = (1f - p) * 30f }
                    .clip(RoundedCornerShape(28.dp)).clickable { onPick(lang) },
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (lang == AppLanguage.TH) "ก" else "A", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.width(52.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, style = XeType.Title, color = Color.White)
                        Text(other, style = XeType.Body, color = Color.White.copy(alpha = .6f))
                    }
                }
            }
        }
    }
}

private enum class Demo { SWIPE_UP, HOLD_ICON, SWIPE_SIDE, HOLD_EMPTY }

/** Four short slides, each with a finger showing the gesture on a little phone. */
@Composable
fun TutorialSlides(onPractice: () -> Unit, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    val pager = rememberPagerState { TUTORIAL_SLIDES }
    val scope = rememberCoroutineScope()
    val slides = listOf(
        Triple(Demo.SWIPE_UP, R.string.slide1_title, R.string.slide1_body),
        Triple(Demo.HOLD_ICON, R.string.slide2_title, R.string.slide2_body),
        Triple(Demo.SWIPE_SIDE, R.string.slide3_title, R.string.slide3_body),
        Triple(Demo.HOLD_EMPTY, R.string.slide4_title, R.string.slide4_body),
    )
    Column(modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
            Text(stringResource(R.string.onb_skip), style = XeType.Headline, color = Color.White.copy(alpha = .7f),
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onSkip() }.padding(horizontal = 14.dp, vertical = 8.dp).testTag("slides_skip"))
        }
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            val (demo, title, body) = slides[page]
            Column(Modifier.fillMaxSize().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                GestureDemo(demo, Modifier.size(170.dp, 320.dp))
                Spacer(Modifier.height(30.dp))
                Text(stringResource(title), style = XeType.Title.copy(fontSize = 26.sp), color = Color.White, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(body), style = XeType.Body.copy(fontSize = 16.sp, lineHeight = 23.sp), color = Color.White.copy(alpha = .7f), textAlign = TextAlign.Center)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(TUTORIAL_SLIDES) { i ->
                    val on = pager.currentPage == i
                    Box(Modifier.size(width = if (on) 22.dp else 7.dp, height = 7.dp).clip(CircleShape).background(Color.White.copy(alpha = if (on) 1f else .35f)))
                }
            }
            val last = pager.currentPage == TUTORIAL_SLIDES - 1
            GlassPillButton(
                stringResource(if (last) R.string.onb_try else R.string.onb_next),
                { if (last) onPractice() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                Modifier.testTag("slides_next"),
                icon = if (last) R.drawable.ti_check else null,
            )
        }
    }
}

/** A tiny phone with a finger acting out [demo], on a loop. */
@Composable
private fun GestureDemo(demo: Demo, modifier: Modifier) {
    val loop = rememberInfiniteTransition(label = "demo")
    val p by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "p")
    Canvas(modifier) {
        val w = size.width; val h = size.height; val r = w * .14f
        drawRoundRect(Color.White.copy(alpha = .06f), cornerRadius = CornerRadius(r))
        drawRoundRect(Color.White.copy(alpha = .35f), cornerRadius = CornerRadius(r), style = Stroke(1.5.dp.toPx()))
        val cols = 4; val icon = w * .15f; val gap = (w - cols * icon) / (cols + 1)
        fun iconAt(c: Int, row: Int, dx: Float = 0f) = Offset(gap + c * (icon + gap) + dx, h * .16f + row * (icon + gap * 1.6f))
        fun grid(dx: Float = 0f, wiggle: Float = 0f, lifted: Pair<Int, Int>? = null, rows: Int = 3) {
            for (row in 0 until rows) for (c in 0 until cols) {
                val o = iconAt(c, row, dx); val big = lifted == (c to row)
                val s = if (big) icon * 1.18f else icon
                withTransform({ rotate(if (wiggle != 0f) wiggle * (if ((c + row) % 2 == 0) 1 else -1) else 0f, o + Offset(icon / 2, icon / 2)) }) {
                    drawRoundRect(Color.White.copy(alpha = if (big) .55f else .22f), o - Offset((s - icon) / 2, (s - icon) / 2), Size(s, s), CornerRadius(s * .3f))
                }
            }
        }
        fun finger(at: Offset, press: Float = 0f) {
            drawCircle(Color.White.copy(alpha = .18f + .2f * press), w * .09f + press * w * .05f, at)
            drawCircle(Color.White.copy(alpha = .9f), w * .055f, at)
        }
        when (demo) {
            Demo.SWIPE_UP -> {
                val m = ease(part(p * 2600, 300, 1000))
                val out = part(p * 2600, 2100, 400)
                grid()
                val top = h - h * .72f * m
                drawRoundRect(Color.White.copy(alpha = .16f * (1f - out)), Offset(w * .04f, top), Size(w * .92f, h - top - h * .02f), CornerRadius(r * .8f))
                for (i in 0 until 8) if (m > .3f) {
                    val o = Offset(gap + (i % 4) * (icon + gap), top + h * .06f + (i / 4) * (icon + gap))
                    drawRoundRect(Color.White.copy(alpha = .35f * (1f - out) * ((m - .3f) / .7f)), o, Size(icon, icon), CornerRadius(icon * .3f))
                }
                if (p < .55f) finger(Offset(w / 2, h * .85f - h * .5f * m), .6f)
            }
            Demo.HOLD_ICON -> {
                val hold = part(p * 2600, 300, 800); val menu = ease(part(p * 2600, 1150, 400))
                grid(lifted = if (hold >= 1f) 1 to 1 else null)
                val ic = iconAt(1, 1) + Offset(icon / 2, icon / 2)
                if (menu > 0f) {
                    val top = ic.y + icon * .9f
                    drawRoundRect(Color.White.copy(alpha = .28f * menu), Offset(w * .18f, top), Size(w * .64f, h * .22f * menu), CornerRadius(14.dp.toPx()))
                    for (i in 0 until 3) drawRoundRect(Color.White.copy(alpha = .5f * menu), Offset(w * .26f, top + h * .04f + i * h * .055f * menu), Size(w * .4f, 3.dp.toPx()), CornerRadius(4f))
                }
                if (p < .8f) finger(ic + Offset(icon * .2f, icon * .3f), hold)
            }
            Demo.SWIPE_SIDE -> {
                val m = ease(part(p * 2600, 300, 900)); val back = ease(part(p * 2600, 1700, 700))
                val dx = -w * (m - back)
                clipRect { grid(dx); grid(dx + w, rows = 2) }
                for (i in 0..1) drawCircle(Color.White.copy(alpha = if ((m - back > .5f) == (i == 1)) 1f else .35f), 3.dp.toPx(), Offset(w / 2 + (i - .5f) * 12.dp.toPx(), h * .8f))
                if (p < .5f) finger(Offset(w * .8f - w * .6f * m, h * .55f), .6f)
            }
            Demo.HOLD_EMPTY -> {
                val hold = part(p * 2600, 300, 800); val edit = part(p * 2600, 1150, 300)
                val wig = if (edit > 0f) sin(p * 2600 / 70f) * 5f else 0f
                val sc = 1f - .1f * edit
                withTransform({ scale(sc, sc, Offset(w / 2, h)) }) { grid(wiggle = wig, rows = 2) }
                if (edit > 0f) drawRoundRect(Color.White.copy(alpha = .3f * edit), Offset(w * .2f, h * .04f), Size(w * .6f, h * .06f), CornerRadius(20f))
                if (p < .8f) finger(Offset(w * .5f, h * .62f), hold)
            }
        }
    }
}

private inline fun DrawScope.clipRect(block: DrawScope.() -> Unit) =
    drawContext.canvas.let { c -> c.save(); c.clipRect(0f, 0f, size.width, size.height); block(); c.restore() }

/** The card that tells you what to try next on the real Home screen. */
@Composable
fun PracticeOverlay(step: PracticeStep?, index: Int, total: Int, finished: Boolean, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = LocalXeScheme.current
    val bob = rememberInfiniteTransition(label = "bob").animateFloat(0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "b")
    GlassSurface(RoundedCornerShape(26.dp), modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        AnimatedContent(
            finished to step, label = "practice",
            transitionSpec = { (fadeIn(tween(260)) + scaleIn(spring(.7f, 500f), .9f)) togetherWith (fadeOut(tween(160)) + scaleOut(targetScale = 1.05f)) },
        ) { (done, s) ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (done || s == null) {
                    Text(stringResource(R.string.practice_finished), style = XeType.Headline, color = scheme.text, modifier = Modifier.weight(1f))
                } else {
                    val arrow = when (s) { PracticeStep.OPEN_DRAWER -> "↑"; PracticeStep.CLOSE_DRAWER -> "↓"; else -> "●" }
                    Text(arrow, fontSize = 22.sp, color = scheme.text, modifier = Modifier.width(30.dp).graphicsLayer {
                        val v = bob.value
                        when (s) { PracticeStep.OPEN_DRAWER -> translationY = -10f * v; PracticeStep.CLOSE_DRAWER -> translationY = 10f * v; else -> { scaleX = .8f + .4f * v; scaleY = scaleX } }
                    })
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.practice_title), style = XeType.Caption, color = scheme.textSecondary)
                        Text(stringResource(s.text), style = XeType.Headline, color = scheme.text)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${index + 1}/$total", style = XeType.Caption, color = scheme.textSecondary)
                        Text(stringResource(R.string.onb_skip), style = XeType.Body, color = scheme.text,
                            modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(50)).clickable { onSkip() }.padding(horizontal = 10.dp, vertical = 4.dp).testTag("practice_skip"))
                    }
                }
            }
        }
    }
}

/**
 * The full-screen part of the welcome (intro, language, default Home, slides). Each stage swaps in with a
 * soft zoom; the Home screen shows faintly behind everything but the intro.
 */
@Composable
fun OnboardingHost(
    stage: OnboardingStage,
    onIntroDone: () -> Unit,
    onLanguage: (AppLanguage) -> Unit,
    onSetDefault: () -> Unit,
    onDefaultLater: () -> Unit,
    onPractice: () -> Unit,
    onSkipTutorial: () -> Unit,
) {
    AnimatedContent(
        stage, label = "onboarding",
        transitionSpec = { (fadeIn(tween(450)) + scaleIn(tween(500), initialScale = 1.08f)) togetherWith (fadeOut(tween(350)) + scaleOut(tween(400), targetScale = .94f)) },
    ) { s ->
        Box(
            Modifier.fillMaxSize().background(if (s == OnboardingStage.INTRO) Color.Black else Color.Black.copy(alpha = .78f))
                .clickable(remember { MutableInteractionSource() }, null) {},
            contentAlignment = Alignment.Center,
        ) {
            when (s) {
                OnboardingStage.INTRO -> IntroAnimation(onIntroDone)
                OnboardingStage.LANGUAGE -> LanguageStep(onLanguage)
                OnboardingStage.DEFAULT_HOME -> SetDefaultHomeCard(onSetDefault, onDefaultLater)
                OnboardingStage.SLIDES -> TutorialSlides(onPractice, onSkipTutorial)
                else -> Unit
            }
        }
    }
}
