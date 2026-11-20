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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Round glass button holding one icon. */
@Composable
fun GlassIconButton(icon: Int, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 42.dp, tint: Color = LocalXeScheme.current.text) {
    val src = remember { MutableInteractionSource() }
    GlassSurface(
        CircleShape,
        modifier.size(size).pressSquish(src)
            .clickable(src, indication = null, role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
    ) { Icon(painterResource(icon), contentDescription, Modifier.size(size * .48f).align(Alignment.Center), tint = tint) }
}

/** Pill-shaped glass button with text. */
@Composable
fun GlassPillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: Int? = null) {
    val src = remember { MutableInteractionSource() }
    val scheme = LocalXeScheme.current
    GlassSurface(
        RoundedCornerShape(50),
        modifier.heightIn(min = 44.dp).pressSquish(src).clickable(src, indication = null, role = Role.Button, onClick = onClick),
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
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(XeMotion.durationMs(180, speed))), exit = fadeOut(tween(XeMotion.durationMs(160, speed)))) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .35f)).clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss))
        }
        AnimatedVisibility(
            visible, Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(XeMotion.spring(speed, dampingRatio = .82f, stiffness = 420f)) { it } + fadeIn(),
            exit = slideOutVertically(tween(XeMotion.durationMs(200, speed))) { it } + fadeOut(),
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
    if (onClick != null && enabled) m = m.clickable(src, indication = null, onClick = onClick).pressSquish(src, haptics = false)
    Row(m.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(painterResource(icon), null, Modifier.size(26.dp), tint = if (enabled) iconTint else scheme.textSecondary)
            Spacer(Modifier.width(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = XeType.Headline, color = if (enabled) scheme.text else scheme.textSecondary)
            subtitle?.let { Text(it, style = XeType.Caption, color = scheme.textSecondary) }
        }
        trailing()
    }
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
    Box(
        modifier.size(52.dp, 32.dp).background(track.value, CircleShape)
            .clickable(src, null, role = Role.Switch) { onChange(!checked) },
    ) {
        GlassSurface(CircleShape, Modifier.padding(start = x.value).align(Alignment.CenterStart).size(28.dp).pressSquish(src),
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
