package com.xenoorigin.xelauncher.settings.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xenoorigin.xelauncher.BuildConfig
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.bugs.BugReport
import com.xenoorigin.xelauncher.bugs.DeviceInfo
import com.xenoorigin.xelauncher.design.GlassPillButton
import com.xenoorigin.xelauncher.design.GlassSurface
import com.xenoorigin.xelauncher.design.GlassSwitch
import com.xenoorigin.xelauncher.design.LocalGlassStyle
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.XeRow
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.design.glassTile
import com.xenoorigin.xelauncher.design.toShape
import com.xenoorigin.xelauncher.di.AppGraph
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xelauncher.settings.XeSettings
import com.xenoorigin.xelauncher.theme.ThemePreset
import com.xenoorigin.xelauncher.theme.applyTo
import com.xenoorigin.xelauncher.theme.currentTheme
import com.xenoorigin.xelauncher.theme.themesFor
import com.xenoorigin.xelauncher.wallpaper.WallpaperRenderer
import com.xenoorigin.xelauncher.wallpaper.WallpaperTarget
import com.xenoorigin.xelauncher.wallpaper.allWallpapers
import com.xenoorigin.xelauncher.wallpaper.byId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val PreviewIcons = listOf(
    R.drawable.ti_photo to Color(0xFFFF9F0A), R.drawable.ti_mail to Color(0xFF0A84FF), R.drawable.ti_world_www to Color(0xFF32ADE6),
    R.drawable.ti_brush to Color(0xFFFF375F), R.drawable.ti_home to Color(0xFF34C759), R.drawable.ti_search to Color(0xFFAF52DE),
)

/** Ready-made looks: wallpaper + icon style + icon shape in one tap. */
@Composable
fun ThemesScreen(graph: AppGraph, s: XeSettings, back: () -> Unit) {
    val scope = rememberCoroutineScope()
    val themes = themesFor(s)
    var picked by remember { mutableStateOf(currentTheme(s, themes)?.id) }
    var busy by remember { mutableStateOf(false) }
    SettingsPage(stringResource(R.string.themes), back) {
        Text(stringResource(R.string.themes_desc), style = XeType.Body, color = LocalXeScheme.current.textSecondary, modifier = Modifier.padding(start = 6.dp, bottom = 14.dp))
        themes.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { t ->
                    ThemeCard(t, selected = picked == t.id, Modifier.weight(1f)) {
                        if (busy) return@ThemeCard
                        picked = t.id; busy = true
                        scope.launch {
                            allWallpapers().byId(t.wallpaperId)?.let { graph.wallpaper.apply(it, WallpaperTarget.HOME) }
                            graph.settings.update { t.applyTo(it) }
                            busy = false
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ThemeCard(t: ThemePreset, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val scheme = LocalXeScheme.current
    val src = remember { MutableInteractionSource() }
    val thumb by produceState<ImageBitmap?>(null, t.wallpaperId) {
        value = withContext(Dispatchers.Default) { allWallpapers().byId(t.wallpaperId)?.let { WallpaperRenderer.render(it, 270, 480).asImageBitmap() } }
    }
    val sel by animateFloatAsState(if (selected) 1f else 0f, label = "sel")
    Column(modifier.clickable(src, null, onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(9f / 16f).scale(1f - .03f * sel).clip(RoundedCornerShape(24.dp)).background(Color(0xFF111114))
                .border(if (selected) 2.5.dp else 1.dp, if (selected) Color.White else Color.White.copy(alpha = .14f), RoundedCornerShape(24.dp)),
        ) {
            thumb?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            Column(Modifier.align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PreviewIcons.chunked(3).forEach { r ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { r.forEach { (icon, c) -> PreviewTile(t, icon, c) } }
                }
            }
            if (selected) Box(
                Modifier.align(Alignment.TopEnd).padding(10.dp).size(26.dp).background(Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(R.drawable.ti_check), null, Modifier.size(16.dp), tint = Color.Black) }
        }
        Text(t.name, style = XeType.Headline, color = scheme.text, modifier = Modifier.padding(top = 8.dp))
        Text(styleName(t.style), style = XeType.Caption, color = scheme.textSecondary)
    }
}

@Composable
private fun PreviewTile(t: ThemePreset, icon: Int, c: Color) {
    val shape = t.shape.toShape()
    val style = LocalGlassStyle.current
    val (mod, tint) = when (t.style) {
        IconStyle.GLASS_DARK -> Modifier.glassTile(shape, style, Color(0xCC1C1C1E)) to Color.White
        IconStyle.MONO -> {
            val g = (c.red * .3f + c.green * .59f + c.blue * .11f)
            Modifier.clip(shape).background(Color(g, g, g)) to if (g > .7f) Color.Black else Color.White
        }
        IconStyle.GLASS_COLOR -> Modifier.clip(shape).background(Brush.linearGradient(listOf(c.copy(alpha = .75f).compositeOverWhite(), c))).glassTile(shape, style, Color.Transparent) to Color.White
        IconStyle.GLASS_CLEAR -> Modifier.glassTile(shape, style, c.copy(alpha = .28f)) to Color.White
        IconStyle.ORIGINAL, IconStyle.STUDIO -> Modifier.clip(shape).background(c) to Color.White
    }
    Box(Modifier.size(40.dp).then(mod), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), null, Modifier.size(21.dp), tint = tint)
    }
}

private fun Color.compositeOverWhite(): Color = Color(red * alpha + (1 - alpha), green * alpha + (1 - alpha), blue * alpha + (1 - alpha))

@Composable
private fun styleName(s: IconStyle): String = stringResource(
    when (s) {
        IconStyle.GLASS_DARK -> R.string.style_glass_dark
        IconStyle.MONO -> R.string.style_mono
        IconStyle.GLASS_COLOR -> R.string.style_glass_color
        IconStyle.GLASS_CLEAR -> R.string.style_glass_clear
        IconStyle.ORIGINAL -> R.string.style_original
        IconStyle.STUDIO -> R.string.style_studio
    },
)

/** Write what went wrong; Xe adds device details, the last crash and recent log, then opens an email. */
@Composable
fun BugReportScreen(graph: AppGraph, s: XeSettings, back: () -> Unit) {
    val ctx = LocalContext.current
    val scheme = LocalXeScheme.current
    val scope = rememberCoroutineScope()
    val crash = remember { graph.crashes.pending() }
    var text by remember { mutableStateOf("") }
    var withDetails by remember { mutableStateOf(true) }
    var withCrash by remember { mutableStateOf(crash != null) }
    val noEmail = stringResource(R.string.bug_no_email)
    SettingsPage(stringResource(R.string.bug_report), back) {
        Text(stringResource(R.string.bug_report_desc), style = XeType.Body, color = scheme.textSecondary, modifier = Modifier.padding(start = 6.dp, bottom = 12.dp))
        GlassSurface(RoundedCornerShape(22.dp), Modifier.fillMaxWidth().heightIn(min = 150.dp).padding(bottom = 10.dp)) {
            if (text.isEmpty()) Text(stringResource(R.string.bug_hint), style = XeType.Body, color = scheme.textSecondary.copy(alpha = .7f), modifier = Modifier.padding(16.dp))
            BasicTextField(
                text, { text = it.take(4000) }, Modifier.fillMaxWidth().heightIn(min = 150.dp).padding(16.dp),
                textStyle = XeType.Body.copy(color = scheme.text), cursorBrush = SolidColor(scheme.text),
            )
        }
        SettingsGroup {
            XeRow(stringResource(R.string.bug_attach_details), subtitle = stringResource(R.string.bug_attach_details_desc)) { GlassSwitch(withDetails, { withDetails = it }) }
            if (crash != null) XeRow(stringResource(R.string.bug_attach_crash)) { GlassSwitch(withCrash, { withCrash = it }) }
        }
        GlassPillButton(
            stringResource(R.string.bug_send), {
                scope.launch {
                    val log = if (withDetails) withContext(Dispatchers.IO) { BugReport.recentLog() } else null
                    val body = BugReport.body(
                        text, DeviceInfo.collect(ctx), if (withDetails) BugReport.settingsSummary(s) else null,
                        if (withCrash) crash else null, log,
                    )
                    val subject = BugReport.subject(BuildConfig.VERSION_NAME, crash = withCrash && crash != null)
                    val sent = try {
                        ctx.startActivity(BugReport.emailIntent(subject, body).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
                    } catch (e: ActivityNotFoundException) {
                        // No email app: share it any way the phone can.
                        runCatching {
                            ctx.startActivity(
                                Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, subject).putExtra(Intent.EXTRA_TEXT, "To: ${BugReport.EMAIL}\n\n$body"), null)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }.isSuccess.also { if (!it) Toast.makeText(ctx, noEmail, Toast.LENGTH_LONG).show() }
                    }
                    if (sent && withCrash) graph.crashes.clear()
                }
            },
            Modifier.fillMaxWidth().padding(top = 8.dp), icon = R.drawable.ti_mail,
        )
        Text(stringResource(R.string.bug_to, BugReport.EMAIL), style = XeType.Caption, color = scheme.textSecondary, modifier = Modifier.padding(top = 10.dp, start = 6.dp))
    }
}

/** Glass bubbles bursting out from [origin]; plays each time [trigger] changes (not at 0). */
@Composable
fun GlassBurst(trigger: Int, origin: Offset, modifier: Modifier = Modifier) {
    if (trigger == 0) return
    val t = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) { t.animateTo(1f, tween(1500, easing = FastOutSlowInEasing)) }
    val bubbles = remember(trigger) {
        val r = Random(trigger)
        List(26) { Triple(r.nextFloat() * 6.283f, 220f + r.nextFloat() * 520f, 14f + r.nextFloat() * 46f) to r.nextFloat() * 360f }
    }
    if (t.value >= 1f) return
    Canvas(modifier.fillMaxSize()) {
        val p = t.value
        bubbles.forEach { (b, hue) ->
            val (a, d, s) = b
            val c = origin + Offset(cos(a) * d * p, sin(a) * d * p)
            val size = s * (.3f + .7f * p)
            val alpha = (1f - p).coerceIn(0f, 1f)
            val tint = Color.hsv(hue, .7f, 1f)
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = .9f * alpha), tint.copy(alpha = .45f * alpha), tint.copy(alpha = .1f * alpha)), center = c - Offset(size * .3f, size * .3f), radius = size * 1.2f), size, c)
            drawCircle(Color.White.copy(alpha = .5f * alpha), size, c, style = Stroke(1.2f))
        }
    }
}
