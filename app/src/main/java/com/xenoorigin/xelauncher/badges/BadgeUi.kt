package com.xenoorigin.xelauncher.badges

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.design.XeFont
import com.xenoorigin.xelauncher.settings.BadgeStyle

/** Unread counts by [badgeKey]; empty when Xe has no notification access. */
val LocalBadgeCounts = compositionLocalOf<Map<String, Int>> { emptyMap() }
val LocalBadgeStyle = compositionLocalOf { BadgeStyle.OFF }

@Composable
fun badgeCountOf(app: AppEntry): Int = LocalBadgeCounts.current[badgeKey(app.key.packageName, app.key.userSerial)] ?: 0

@Composable
fun badgeCountOf(apps: List<AppEntry>): Int {
    val counts = LocalBadgeCounts.current
    return apps.distinctBy { it.key.packageName to it.key.userSerial }.sumOf { counts[badgeKey(it.key.packageName, it.key.userSerial)] ?: 0 }
}

/**
 * The unread badge for an icon of [iconSize], in the chosen style. Pops in with a little bounce the moment
 * an app gets its first unread message. Draws nothing when the count is 0 or badges are off.
 */
@Composable
fun UnreadBadge(count: Int, iconSize: Dp, modifier: Modifier = Modifier) {
    val style = LocalBadgeStyle.current
    if (count <= 0 || style == BadgeStyle.OFF) return
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = .45f, stiffness = 520f)) }
    val ring = Color.Black.copy(alpha = .28f)
    val m = modifier.testTag("badge").graphicsLayer { scaleX = pop.value; scaleY = pop.value }
    if (style == BadgeStyle.DOT) {
        val d = (iconSize * .2f).coerceIn(11.dp, 16.dp)
        Box(m.size(d).background(Color.White, CircleShape).border(1.5.dp, ring, CircleShape))
        return
    }
    val h = (iconSize * .34f).coerceIn(17.dp, 26.dp)
    val (bg, fg) = if (style == BadgeStyle.RED) Color(0xFFFF3B30) to Color.White else Color.White to Color(0xFF111114)
    val fontSize = with(LocalDensity.current) { (h * .62f).toSp() }
    Box(
        m.height(h).defaultMinSize(minWidth = h)
            .background(bg, RoundedCornerShape(h / 2)).border(1.5.dp, ring, RoundedCornerShape(h / 2))
            .padding(horizontal = h * .26f),
        contentAlignment = Alignment.Center,
    ) {
        Text(badgeLabel(count), style = TextStyle(fontFamily = XeFont, fontWeight = FontWeight.Bold, fontSize = fontSize, color = fg))
    }
}

/** Whether Xe may read notifications; checked again whenever the screen comes back (e.g. from system settings). */
@Composable
fun rememberBadgeAccess(): Boolean {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val state = remember { androidx.compose.runtime.mutableStateOf(BadgeListener.hasAccess(ctx)) }
    androidx.compose.runtime.DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) state.value = BadgeListener.hasAccess(ctx)
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return state.value
}
