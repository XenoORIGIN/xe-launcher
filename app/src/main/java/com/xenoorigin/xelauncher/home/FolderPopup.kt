package com.xenoorigin.xelauncher.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.design.GlassSurface
import com.xenoorigin.xelauncher.design.LocalAnimationSpeed
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.XeMotion
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.icons.AppIcon

/** Folder opened from its icon into a centred glass panel; name is editable in place. */
@Composable
fun FolderPopup(
    folder: FolderCell,
    from: Rect,
    look: HomeLook,
    onRename: (String) -> Unit,
    onLaunch: (AppEntry, Rect) -> Unit,
    onLongPress: (AppEntry, Rect) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = LocalXeScheme.current
    val speed = LocalAnimationSpeed.current
    val density = LocalDensity.current
    val conf = LocalConfiguration.current
    var name by remember(folder.folderId) { mutableStateOf(folder.name) }
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, XeMotion.spring(speed, dampingRatio = .72f, stiffness = 420f)) }
    val close = { if (name != folder.name) onRename(name); onDismiss() }
    BackHandler(onBack = close)
    val sw = with(density) { conf.screenWidthDp.dp.toPx() }; val sh = with(density) { conf.screenHeightDp.dp.toPx() }
    val origin = TransformOrigin((from.center.x / sw).coerceIn(0f, 1f), (from.center.y / sh).coerceIn(0f, 1f))
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = .3f * anim.value.coerceIn(0f, 1f)))
            .clickable(remember { MutableInteractionSource() }, null, onClick = close),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxSize().graphicsLayer {
                val v = anim.value; scaleX = .2f + .8f * v; scaleY = .2f + .8f * v; alpha = v.coerceIn(0f, 1f); transformOrigin = origin
            },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            BasicTextField(
                name, { name = it.take(30) }, Modifier.width(280.dp).padding(bottom = 18.dp),
                textStyle = XeType.Title.copy(color = scheme.text, textAlign = TextAlign.Center), singleLine = true,
                cursorBrush = SolidColor(scheme.text), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onRename(name) }),
            )
            GlassSurface(RoundedCornerShape(36.dp), Modifier.width(320.dp).clickable(enabled = false) {}) {
                Column(Modifier.padding(vertical = 22.dp, horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    folder.apps.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { app ->
                                var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                                Box(Modifier.weight(1f).onGloballyPositioned { coords = it }, contentAlignment = Alignment.TopCenter) {
                                    AppIcon(app, (look.iconSize - 4).dp, look.style(app), look.shape, look.labels,
                                        Modifier.homeItemGestures(app.key, { false }) {
                                            ItemGestureCallbacks(
                                                toRoot = { it }, onTap = { onLaunch(app, coords?.boundsInWindow() ?: Rect.Zero) },
                                                onLongPress = { onLongPress(app, coords?.boundsInWindow() ?: Rect.Zero) },
                                                onDragStart = {}, onDrag = {}, onDragEnd = { _: Offset -> }, onDragCancel = {},
                                            )
                                        })
                                }
                            }
                            repeat(4 - row.size) { Box(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}
