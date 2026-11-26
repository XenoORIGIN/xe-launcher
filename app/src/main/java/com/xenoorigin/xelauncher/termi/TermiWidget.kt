package com.xenoorigin.xelauncher.termi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.currentStateAsState
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.widgets.LocalWidgetActions
import com.xenoorigin.xelauncher.widgets.Span

enum class TermiLayout { SMALL, BUTTONS, FULL }

/** Which parts fit: everything at 4×4, buttons from 3 cells wide, otherwise Termi alone. */
fun termiLayout(span: Span): TermiLayout = when {
    span.x >= 4 && span.y >= 4 -> TermiLayout.FULL
    span.x >= 3 -> TermiLayout.BUTTONS
    else -> TermiLayout.SMALL
}

/** Termi's state plus the event stream, ready to draw. */
internal class TermiUi(val store: TermiStore) {
    var event by mutableStateOf<TermiEvent?>(null)
    var eventKey by mutableIntStateOf(0)
    val listener: (TermiEvent) -> Unit = { event = it; eventKey++ }
}

@Composable
internal fun rememberTermiUi(store: TermiStore): TermiUi {
    val ui = remember(store) { TermiUi(store) }
    DisposableEffect(store) { store.listen(ui.listener); onDispose { store.unlisten(ui.listener) } }
    return ui
}

@Composable
fun TermiWidget(span: Span, modifier: Modifier = Modifier) {
    val actions = LocalWidgetActions.current
    val store = actions.termi ?: return
    val ui = rememberTermiUi(store)
    val saved by store.state.collectAsState()
    val minute = rememberMinuteTick()
    val now = remember(saved, minute) { store.current() }
    val mood = remember(saved, minute) { store.mood() }
    val body = TermiSprite.bodyColor(actions.termiBody)
    val layout = termiLayout(span)
    @Composable fun view(m: Modifier) = TermiView(
        TermiRules.stage(now.xp), now.equipped, mood, ui.event, body, actions.asciiColor, actions.termiBody, actions.animate,
        m.testTag("termi_view").clickable { actions.openTermi() }, ui.eventKey,
    )
    val asleep = mood == Mood.ASLEEP
    when (layout) {
        TermiLayout.SMALL -> view(modifier.fillMaxSize())
        TermiLayout.BUTTONS -> Row(modifier.fillMaxSize()) {
            view(Modifier.weight(1f).fillMaxHeight())
            Column(Modifier.width(76.dp).fillMaxHeight().padding(6.dp), verticalArrangement = Arrangement.SpaceEvenly) {
                CareButtons(store, asleep)
            }
        }
        TermiLayout.FULL -> Column(modifier.fillMaxSize()) {
            view(Modifier.weight(1f).fillMaxWidth())
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp).testTag("termi_bars")) {
                Text(
                    stringResource(R.string.termi_level, TermiRules.level(now.xp)) + " · " + stageName(TermiRules.stage(now.xp)),
                    color = Color.White.copy(alpha = .8f), fontSize = 12.sp,
                )
                NeedBar(stringResource(R.string.termi_food), now.food, Color(0xFFFFD166))
                NeedBar(stringResource(R.string.termi_fun), now.`fun`, Color(0xFFFF8CB4))
                NeedBar(stringResource(R.string.termi_energy), now.energy, Color(0xFF7FE7FF))
            }
            Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CareButtons(store, asleep, Modifier.weight(1f))
            }
        }
    }
}

/** Changes once a minute while Xe is visible, so needs and sleep follow the clock without any saved change. */
@Composable
internal fun rememberMinuteTick(): Int {
    var tick by remember { mutableIntStateOf(0) }
    val life by androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val visible = life.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
    androidx.compose.runtime.LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        tick++ // catch up on return
        while (true) { kotlinx.coroutines.delay(60_000); tick++ }
    }
    return tick
}

fun stageName(s: Stage) = when (s) { Stage.JUNIOR -> "Junior"; Stage.SENIOR -> "Senior"; Stage.LEAD -> "Lead" }

@Composable
internal fun NeedBar(label: String, value: Float, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White.copy(alpha = .85f), fontSize = 12.sp, modifier = Modifier.width(72.dp))
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = .15f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth((value / 100f).coerceIn(0f, 1f)).background(color))
        }
    }
}

@Composable
internal fun CareButtons(store: TermiStore, asleep: Boolean, each: Modifier = Modifier) {
    CareButton(stringResource(R.string.termi_feed), "termi_feed", each) { store.feed() }
    CareButton(stringResource(R.string.termi_play), "termi_play", each) { store.play() }
    CareButton(stringResource(if (asleep) R.string.termi_wake else R.string.termi_sleep), "termi_sleep", each) { store.toggleSleep() }
}

@Composable
private fun CareButton(label: String, tag: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.testTag(tag).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = .16f)).clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 1) }
}
