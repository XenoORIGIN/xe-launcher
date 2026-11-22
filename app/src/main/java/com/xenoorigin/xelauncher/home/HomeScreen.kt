package com.xenoorigin.xelauncher.home

import com.xenoorigin.xelauncher.design.appear

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.xenoorigin.xelauncher.widgets.minX
import com.xenoorigin.xelauncher.widgets.minY
import com.xenoorigin.xelauncher.widgets.presetSpans
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.design.GlassSurface
import com.xenoorigin.xelauncher.design.glassTile
import com.xenoorigin.xelauncher.design.LocalGlassStyle
import com.xenoorigin.xelauncher.design.toShape
import com.xenoorigin.xelauncher.design.LocalAnimationSpeed
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.SquircleShape
import com.xenoorigin.xelauncher.design.XeMotion
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.design.AnimationSpeed
import com.xenoorigin.xelauncher.icons.AppIcon
import com.xenoorigin.xelauncher.icons.IconShape
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xelauncher.icons.IconTile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

sealed interface DropRequest {
    data class Move(val itemId: Long, val page: Int, val x: Int, val y: Int) : DropRequest
    data class Onto(val itemId: Long, val targetId: Long) : DropRequest
    data class ToDock(val itemId: Long, val position: Int) : DropRequest
}

/** Draws a widget cell; provided by the app (needs the widget host), a placeholder in previews/tests. */
val LocalWidgetContent = androidx.compose.runtime.staticCompositionLocalOf<@Composable (WidgetCell) -> Unit> {
    { Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = .08f), RoundedCornerShape(24.dp))) }
}

data class HomeLook(
    val columns: Int, val rows: Int, val dockSize: Int,
    val iconSize: Int, val labels: Boolean, val shape: IconShape, val style: (AppEntry) -> IconStyle,
)

class HomeCallbacks(
    val onLaunch: (AppEntry, Rect) -> Unit,
    val onItemMenu: (HomeCell, AppEntry?, Rect) -> Unit,
    val onFolderOpen: (FolderCell, Rect) -> Unit,
    val onEmptyLongPress: () -> Unit,
    val onEmptyTap: () -> Unit,
    val onDoubleTap: () -> Unit,
    val onDrop: (DropRequest) -> Unit,
    val onDragStart: () -> Unit,
    val onWidgetEdit: (WidgetCell) -> Unit = {},
)

private data class Drag(val cell: HomeCell, val pos: Offset, val grab: Offset)

/** A widget being moved/resized in place, and what to do with the result. */
class WidgetEditing(
    val id: Long,
    val place: (Placement) -> Unit,
    val remove: () -> Unit,
    val done: () -> Unit,
    val content: @Composable (WidgetCell, com.xenoorigin.xelauncher.widgets.Span) -> Unit,
)

@Composable
fun HomeScreen(
    layout: HomeLayout,
    look: HomeLook,
    pager: PagerState,
    editMode: Boolean,
    cb: HomeCallbacks,
    modifier: Modifier = Modifier,
    widgetEdit: WidgetEditing? = null,
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val touch = remember { HomeTouch() }
    val scope = rememberCoroutineScope()
    var drag by remember { mutableStateOf<Drag?>(null) }
    var gridBounds by remember { mutableStateOf(Rect.Zero) }
    var dockBounds by remember { mutableStateOf(Rect.Zero) }
    val pageCount = layout.pages.size + if (editMode) 1 else 0
    var edgeSince by remember { mutableStateOf(0L) }

    fun dropFor(p: Offset, cell: HomeCell): DropRequest? {
        if (dockBounds.contains(p) && cell !is WidgetCell) {
            val slot = ((p.x - dockBounds.left) / (dockBounds.width / look.dockSize)).toInt().coerceIn(0, look.dockSize - 1)
            return DropRequest.ToDock(cell.id, slot)
        }
        if (!gridBounds.contains(p)) return null
        val cw = gridBounds.width / look.columns; val ch = gridBounds.height / look.rows
        val x = floor((p.x - gridBounds.left) / cw).toInt().coerceIn(0, look.columns - 1)
        val y = floor((p.y - gridBounds.top) / ch).toInt().coerceIn(0, look.rows - 1)
        val page = pager.currentPage
        if (cell is WidgetCell) {
            // Drop so the widget's top-left follows where it was grabbed.
            val gx = floor((p.x - drag!!.grab.x + cw / 2 - gridBounds.left) / cw).toInt().coerceIn(0, look.columns - cell.spanX)
            val gy = floor((p.y - drag!!.grab.y + ch / 2 - gridBounds.top) / ch).toInt().coerceIn(0, look.rows - cell.spanY)
            return DropRequest.Move(cell.id, page, gx, gy)
        }
        val occupant = layout.pages.getOrNull(page)?.firstOrNull { it.x == x && it.y == y && it.id != cell.id }
        if (occupant != null && cell is AppCell) {
            val cx = gridBounds.left + (x + .5f) * cw; val cy = gridBounds.top + (y + .45f) * ch
            if ((Offset(cx, cy) - p).getDistance() < cw * .32f) return DropRequest.Onto(cell.id, occupant.id)
        }
        return DropRequest.Move(cell.id, page, x, y)
    }

    fun callbacksFor(cell: HomeCell, coords: () -> LayoutCoordinates?, app: AppEntry?): ItemGestureCallbacks = ItemGestureCallbacks(
        toRoot = { coords()?.localToRoot(it) ?: it },
        onTap = {
            val b = coords()?.boundsInWindow() ?: Rect.Zero
            when {
                editMode -> Unit
                cell is FolderCell -> cb.onFolderOpen(cell, b)
                app != null -> cb.onLaunch(app, b)
            }
        },
        onLongPress = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            // Widgets lift straight into in-place editing; apps and folders show their menu.
            if (cell is WidgetCell && !editMode) cb.onWidgetEdit(cell) else cb.onItemMenu(cell, app, coords()?.boundsInWindow() ?: Rect.Zero)
        },
        onDragStart = { p ->
            val topLeft = coords()?.boundsInRoot()?.topLeft ?: p
            drag = Drag(cell, p, p - topLeft); cb.onDragStart()
        },
        onDrag = { p ->
            drag = drag?.copy(pos = p)
            val w = gridBounds.width
            val nearLeft = p.x < gridBounds.left + w * .06f; val nearRight = p.x > gridBounds.right - w * .06f
            if (nearLeft || nearRight) {
                val now = System.currentTimeMillis()
                if (edgeSince == 0L) edgeSince = now
                if (now - edgeSince > 650) {
                    edgeSince = now
                    val target = (pager.currentPage + if (nearRight) 1 else -1).coerceIn(0, pageCount - 1)
                    scope.launch { pager.animateScrollToPage(target) }
                }
            } else edgeSince = 0L
        },
        onDragEnd = { p -> drag?.let { d -> dropFor(p, d.cell)?.let(cb.onDrop) }; drag = null; edgeSince = 0L },
        onDragCancel = { drag = null; edgeSince = 0L },
        onDown = { touch.itemDown = true },
    )

    val lens = LocalLens.current
    Box(
        modifier.fillMaxSize().pointerInput(editMode, lens) {
            detectTapGestures(
                onLongPress = { p ->
                    // With the lens on, Home's long-press action waits until the finger lifts without dragging.
                    if (lens != null && !editMode && !touch.onItem) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); lens.begin(p) } else cb.onEmptyLongPress()
                },
                onDoubleTap = { cb.onDoubleTap() }, onTap = { cb.onEmptyTap() },
            )
        }.then(if (lens != null) Modifier.lensDrag(lens, touch) { cb.onEmptyLongPress() } else Modifier),
    ) {
        // Edit mode shrinks the whole page (keeping every cell the same shape, labels included) to make room for the bar.
        val editScale by androidx.compose.animation.core.animateFloatAsState(
            if (editMode) .86f else 1f, com.xenoorigin.xelauncher.design.XeMotion.spring(com.xenoorigin.xelauncher.design.LocalAnimationSpeed.current, dampingRatio = .78f, stiffness = 380f), label = "editScale",
        )
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            HorizontalPager(
                pager,
                Modifier.weight(1f).fillMaxWidth().graphicsLayer {
                    scaleX = editScale; scaleY = editScale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(.5f, 1f)
                },
                beyondViewportPageCount = 1, userScrollEnabled = drag == null && lens?.active != true,
            ) { page ->
                val depthOn = com.xenoorigin.xelauncher.design.LocalAnimationSpeed.current != com.xenoorigin.xelauncher.design.AnimationSpeed.OFF
                Box(Modifier.fillMaxSize()
                    .graphicsLayer {
                        // Pages lean back and dim a little as they slide away, like turning a glass pane.
                        if (depthOn && drag == null) {
                            val d = com.xenoorigin.xelauncher.design.pageDepth((pager.currentPage - page) + pager.currentPageOffsetFraction)
                            scaleX = d.scale; scaleY = d.scale; rotationY = d.rotationY; alpha = d.alpha
                            cameraDistance = 14f * this.density
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 18.dp).onGloballyPositioned { if (page == pager.currentPage) gridBounds = it.boundsInRoot() }) {
                    PageGrid(layout.pages.getOrNull(page).orEmpty(), look, editMode, drag?.cell?.id ?: widgetEdit?.id, ::callbacksFor)
                }
            }
            PageDots(pageCount, pager)
            Dock(layout.dock, look, editMode, drag?.cell?.id, Modifier.onGloballyPositioned { dockBounds = it.boundsInRoot() }, ::callbacksFor)
        }
        // The icon being dragged floats above everything.
        drag?.let { d ->
            Box(Modifier.offset { IntOffset((d.pos.x - d.grab.x).roundToInt(), (d.pos.y - d.grab.y).roundToInt()) }.graphicsLayer { scaleX = 1.12f; scaleY = 1.12f; alpha = .92f }) {
                val wc = d.cell as? WidgetCell
                if (wc != null) {
                    // A light stand-in while moving (the real widget view stays in its place).
                    val cwDp = with(density) { (gridBounds.width / look.columns).toDp() }
                    val chDp = with(density) { (gridBounds.height / look.rows).toDp() }
                    Box(Modifier.size(cwDp * wc.spanX - 12.dp, chDp * wc.spanY - 12.dp).glassTile(RoundedCornerShape(26.dp), LocalGlassStyle.current, Color.White.copy(alpha = .22f)))
                } else CellContent(d.cell, look, label = look.labels)
            }
        }
        lens?.let { GlassLens(it) }
        widgetEdit?.let { we ->
            val pageCells = layout.pages.getOrNull(pager.currentPage).orEmpty()
            val wc = pageCells.filterIsInstance<WidgetCell>().firstOrNull { it.id == we.id }
            if (wc == null) { androidx.compose.runtime.LaunchedEffect(we.id) { we.done() }; return@let }
            val kind = com.xenoorigin.xelauncher.widgets.GlassWidgetKind.of(wc.kind)
            val taken = pageCells.filter { it.id != wc.id }.flatMap { c ->
                val (w, h) = (c as? WidgetCell)?.let { it.spanX to it.spanY } ?: (1 to 1)
                (0 until w).flatMap { dx -> (0 until h).map { dy -> (c.x + dx) to (c.y + dy) } }
            }.toSet()
            val min = kind?.let { com.xenoorigin.xelauncher.widgets.Span(it.minX, it.minY) } ?: com.xenoorigin.xelauncher.widgets.Span(1, 1)
            val max = com.xenoorigin.xelauncher.widgets.Span(look.columns, look.rows)
            val presets = kind?.presetSpans(look.columns, look.rows)
                ?: listOf(com.xenoorigin.xelauncher.widgets.Span(2, 1), com.xenoorigin.xelauncher.widgets.Span(2, 2), com.xenoorigin.xelauncher.widgets.Span(4, 2), com.xenoorigin.xelauncher.widgets.Span(4, 4))
                    .map { com.xenoorigin.xelauncher.widgets.Span(it.x.coerceAtMost(look.columns), it.y.coerceAtMost(look.rows)) }.distinct()
            androidx.activity.compose.BackHandler { we.done() }
            WidgetEditOverlay(wc, gridBounds, look.columns, look.rows, taken, min, max, presets, { s -> we.content(wc, s) }, we.place, we.remove, we.done)
        }
    }
}

/**
 * Moves [lens] with the finger once a long press on empty space started it. Lifting without dragging
 * runs [onPlainLongPress] (Home's normal long-press action).
 */
/** Whether the current touch started on an item; items report their downs before Home sees them. */
private class HomeTouch {
    var itemDown = false
    var onItem = false
}

private fun Modifier.lensDrag(lens: LensState, touch: HomeTouch, onPlainLongPress: () -> Unit): Modifier = pointerInput(lens) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        touch.onItem = touch.itemDown
        touch.itemDown = false
        var last = down.position
        var travelled = 0f
        while (true) {
            val ev = awaitPointerEvent()
            val c = ev.changes.firstOrNull { it.id == down.id } ?: break
            if (lens.active) {
                travelled += (c.position - last).getDistance()
                lens.moveTo(c.position)
                c.consume()
            }
            last = c.position
            if (!c.pressed) break
        }
        if (lens.active) {
            lens.end()
            if (travelled < viewConfiguration.touchSlop) onPlainLongPress()
        }
    }
}

@Composable
private fun PageGrid(
    cells: List<HomeCell>, look: HomeLook, editMode: Boolean, draggingId: Long?,
    callbacksFor: (HomeCell, () -> LayoutCoordinates?, AppEntry?) -> ItemGestureCallbacks,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cw = maxWidth / look.columns; val ch = maxHeight / look.rows
        cells.forEach { cell ->
            val w = cell as? WidgetCell
            Box(
                Modifier.offset(cw * cell.x, ch * cell.y).size(cw * (w?.spanX ?: 1), ch * (w?.spanY ?: 1))
                    .then(if (w != null) Modifier.padding(6.dp) else Modifier),
                contentAlignment = Alignment.TopCenter,
            ) { HomeItem(cell, look, editMode, draggingId == cell.id, label = look.labels, callbacksFor) }
        }
    }
}

@Composable
private fun HomeItem(
    cell: HomeCell, look: HomeLook, editMode: Boolean, isDragging: Boolean, label: Boolean,
    callbacksFor: (HomeCell, () -> LayoutCoordinates?, AppEntry?) -> ItemGestureCallbacks,
) {
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val app = (cell as? AppCell)?.app
    val wiggle = wiggleAngle(editMode, cell.id)
    val cbs = callbacksFor(cell, { coords }, app)
    val latestCbs by androidx.compose.runtime.rememberUpdatedState(cbs)
    val latestEdit by androidx.compose.runtime.rememberUpdatedState(editMode)
    val isWidget = cell is WidgetCell
    Box(
        Modifier.then(if (isWidget) Modifier.fillMaxSize() else Modifier)
            .appear(cell.y * look.columns + cell.x, distance = 46f, stepMs = 22, scaleFrom = .8f)
            .onGloballyPositioned { coords = it }
            .homeItemGestures(cell.id, { latestEdit }, { latestCbs })
            .graphicsLayer { rotationZ = if (isWidget) wiggle() * .3f else wiggle(); alpha = if (isDragging) .25f else 1f },
    ) {
        CellContent(cell, look, label)
        // While editing, a cover over the widget takes the touches so it can be dragged instead of used.
        if (isWidget && editMode) Box(
            Modifier.matchParentSize().border(1.dp, Color.White.copy(alpha = .5f), RoundedCornerShape(26.dp))
                .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
        )
    }
}

@Composable
fun CellContent(cell: HomeCell, look: HomeLook, label: Boolean) {
    when (cell) {
        is AppCell -> AppIcon(cell.app, look.iconSize.dp, look.style(cell.app), look.shape, label)
        is FolderCell -> FolderIcon(cell, look, label)
        is WidgetCell -> LocalWidgetContent.current(cell)
    }
}

@Composable
fun FolderIcon(cell: FolderCell, look: HomeLook, label: Boolean) {
    val size = look.iconSize.dp
    Column(Modifier.width(size + 22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val fs = look.shape.toShape()
        Box {
            Box(Modifier.size(size).glassTile(fs, LocalGlassStyle.current, Color.White.copy(alpha = .16f))) {
                val mini = size * .36f
                Column(Modifier.align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(size * .06f)) {
                    cell.apps.take(4).chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(size * .06f)) {
                            row.forEach { IconTile(it, mini, look.style(it), look.shape) }
                        }
                    }
                }
            }
            // A folder shows everything unread inside it.
            com.xenoorigin.xelauncher.badges.UnreadBadge(
                com.xenoorigin.xelauncher.badges.badgeCountOf(cell.apps), size,
                Modifier.align(Alignment.TopEnd).offset(x = 9.dp, y = (-7).dp),
            )
        }
        if (label) Text(cell.name, style = XeType.Label, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Dock(
    cells: List<HomeCell>, look: HomeLook, editMode: Boolean, draggingId: Long?, modifier: Modifier,
    callbacksFor: (HomeCell, () -> LayoutCoordinates?, AppEntry?) -> ItemGestureCallbacks,
) {
    GlassSurface(RoundedCornerShape(36.dp), modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 10.dp).height(look.iconSize.dp + 30.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            (0 until look.dockSize).forEach { slot ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    cells.getOrNull(slot)?.let { HomeItem(it, look, editMode, draggingId == it.id, label = false, callbacksFor) }
                }
            }
        }
    }
}

@Composable
private fun PageDots(count: Int, pager: PagerState) {
    if (count <= 1) { Spacer(Modifier.height(14.dp)); return }
    Row(Modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val active = pager.currentPage == i
            Box(Modifier.padding(horizontal = 4.dp).size(if (active) 7.dp else 6.dp).alpha(if (active) 1f else .4f).background(Color.White, CircleShape))
        }
    }
}

/** Gentle ±2° wiggle while editing, each icon slightly out of phase. */
@Composable
private fun wiggleAngle(editMode: Boolean, seed: Long): () -> Float {
    if (!editMode || LocalAnimationSpeed.current == AnimationSpeed.OFF) return { 0f }
    val t = rememberInfiniteTransition(label = "wiggle")
    val phase by t.animateFloat(0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(280, easing = LinearEasing), RepeatMode.Restart), label = "p")
    val offset = (seed % 7) / 7f * 6.28f
    return { (sin(phase + offset) * 2f) }
}
