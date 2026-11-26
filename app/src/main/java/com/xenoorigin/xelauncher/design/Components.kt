package com.xenoorigin.xelauncher.design

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border

/** Round glass button holding one icon. */
@Composable
fun GlassIconButton(icon: Int, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 42.dp, tint: Color = LocalXeScheme.current.text) {
    val src = remember { MutableInteractionSource() }
    GlassSurface(
        CircleShape,
        modifier.size(size).pressSquish(src)
            .clickable(src, indication = null, role = Role.Button, onClickLabel = contentDescription) { com.xenoorigin.xelauncher.sound.XeSound.tap(); onClick() },
    ) { Icon(painterResource(icon), contentDescription, Modifier.size(size * .48f).align(Alignment.Center), tint = tint) }
}

/** Pill-shaped glass button with text. */
@Composable
fun GlassPillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: Int? = null) {
    val src = remember { MutableInteractionSource() }
    val scheme = LocalXeScheme.current
    GlassSurface(
        RoundedCornerShape(50),
        modifier.heightIn(min = 44.dp).pressSquish(src).clickable(src, indication = null, role = Role.Button) { com.xenoorigin.xelauncher.sound.XeSound.tap(); onClick() },
    ) {
        Row(Modifier.align(Alignment.Center).padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            icon?.let { Icon(painterResource(it), null, Modifier.size(18.dp), tint = scheme.text) }
            Text(text, style = XeType.Headline, color = scheme.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Glass bottom sheet over a dimmed scrim. Slides up with a spring; back or a scrim tap closes it.
 */
@Composable
fun GlassSheet(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val speed = LocalAnimationSpeed.current
    if (visible) BackHandler(onBack = onDismiss)
    val firstSheet = remember { booleanArrayOf(true) }
    androidx.compose.runtime.LaunchedEffect(visible) {
        if (firstSheet[0]) { firstSheet[0] = false; if (!visible) return@LaunchedEffect }
        com.xenoorigin.xelauncher.sound.XeSound.play(if (visible) com.xenoorigin.xelauncher.sound.XeSound.Kind.OPEN else com.xenoorigin.xelauncher.sound.XeSound.Kind.CLOSE)
    }
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(XeMotion.durationMs(180, speed))), exit = fadeOut(tween(XeMotion.durationMs(160, speed)))) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .35f)).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss))
        }
        AnimatedVisibility(
            visible, Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(XeMotion.settle(speed)) { it } + fadeIn(),
            exit = slideOutVertically(XeMotion.settle(speed)) { it } + fadeOut(tween(XeMotion.durationMs(180, speed))),
        ) {
            GlassSurface(
                RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                Modifier.fillMaxWidth().padding(top = 80.dp),
                LocalGlassStyle.current.let { it.copy(tint = if (it.dark) Color(0xFF121216).copy(alpha = .72f) else Color.White.copy(alpha = .7f)) },
            ) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Box(Modifier.align(Alignment.CenterHorizontally).width(38.dp).padding(bottom = 10.dp).size(38.dp, 5.dp).background(LocalXeScheme.current.textSecondary.copy(alpha = .5f), CircleShape))
                    content()
                }
            }
        }
    }
}

/** A settings-style row: leading icon, title, optional subtitle and trailing content. */
@Composable
fun XeRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: Int? = null,
    iconTint: Color = LocalXeScheme.current.text,
    subtitle: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val scheme = LocalXeScheme.current
    val src = remember { MutableInteractionSource() }
    var m = modifier.fillMaxWidth().heightIn(min = 56.dp)
    if (onClick != null && enabled) m = m.clickable(src, indication = null) { com.xenoorigin.xelauncher.sound.XeSound.tap(); onClick() }.pressSquish(src, haptics = false)
    Row(m.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            RowIcon(icon, if (enabled) iconTint else scheme.textSecondary)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = XeType.Headline, color = if (enabled) scheme.text else scheme.textSecondary)
            subtitle?.let { Text(it, style = XeType.Caption, color = scheme.textSecondary) }
        }
        trailing()
    }
}

/**
 * A row's icon. Liquid Glass: a white symbol on a small glossy tile of the row's colour.
 * Nothing: the bare symbol in the text colour, no colour at all.
 */
@Composable
fun RowIcon(icon: Int, tint: Color) {
    val scheme = LocalXeScheme.current
    if (LocalUiStyle.current == UiStyle.NOTHING || tint == scheme.text || tint == scheme.textSecondary) {
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { Icon(painterResource(icon), null, Modifier.size(24.dp), tint = if (tint == scheme.textSecondary) tint else scheme.text) }
        return
    }
    Box(
        Modifier.size(32.dp).clip(RoundedCornerShape(9.dp))
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.lerp(tint, Color.White, .18f), tint, androidx.compose.ui.graphics.lerp(tint, Color.Black, .12f))))
            .border(.8.dp, Color.White.copy(alpha = .28f), RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center,
    ) { Icon(painterResource(icon), null, Modifier.size(19.dp), tint = Color.White) }
}

/** Liquid glass toggle switch. */
@Composable
fun GlassSwitch(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val speed = LocalAnimationSpeed.current
    val scheme = LocalXeScheme.current
    val x = androidx.compose.animation.core.animateDpAsState(if (checked) 22.dp else 2.dp, XeMotion.spring(speed, dampingRatio = .6f), label = "knob")
    val track = androidx.compose.animation.animateColorAsState(
        if (checked) (if (scheme.accent == XeColors.White) Color(0xFF34C759) else scheme.accent) else Color.White.copy(alpha = .16f), label = "track",
    )
    val src = remember { MutableInteractionSource() }
    // The knob stretches like jelly while it slides, then wobbles back to round.
    val jelly = remember { androidx.compose.animation.core.Animatable(0f) }
    val first = remember { booleanArrayOf(true) }
    androidx.compose.runtime.LaunchedEffect(checked) {
        if (first[0]) { first[0] = false; return@LaunchedEffect }
        if (speed == AnimationSpeed.OFF) return@LaunchedEffect
        jelly.animateTo(1f, androidx.compose.animation.core.tween(110))
        jelly.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = .32f, stiffness = 420f))
    }
    Box(
        modifier.size(52.dp, 32.dp).background(track.value, CircleShape)
            .clickable(src, null, role = Role.Switch) {
                com.xenoorigin.xelauncher.sound.XeSound.play(if (checked) com.xenoorigin.xelauncher.sound.XeSound.Kind.TOGGLE_OFF else com.xenoorigin.xelauncher.sound.XeSound.Kind.TOGGLE_ON)
                onChange(!checked)
            },
    ) {
        GlassSurface(CircleShape, Modifier.padding(start = x.value).align(Alignment.CenterStart).size(28.dp)
            .graphicsLayer { val j = jelly.value; scaleX = 1f + .32f * j; scaleY = 1f - .14f * j; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (checked) 1f else 0f, .5f) }
            .pressSquish(src),
            LocalGlassStyle.current.copy(tint = Color.White.copy(alpha = .92f)))
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier.padding(top = 22.dp, bottom = 6.dp), style = XeType.Label, color = LocalXeScheme.current.textSecondary)
}

@Composable
fun BoxScope.CenterText(text: String) {
    Text(text, Modifier.align(Alignment.Center).padding(32.dp), style = XeType.Body, color = LocalXeScheme.current.textSecondary)
}
