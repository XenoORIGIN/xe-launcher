package com.xenoorigin.xestudio

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xenoorigin.xestudio.design.Ink
import com.xenoorigin.xestudio.design.Motion
import com.xenoorigin.xestudio.design.StudioTheme
import com.xenoorigin.xestudio.design.StudioType
import com.xenoorigin.xestudio.design.glassPanel
import com.xenoorigin.xestudio.design.pressSquish
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

enum class Tab(val label: Int, val tag: String) {
    CREATE(R.string.tab_create, "tab_create"),
    STORE(R.string.tab_store, "tab_store"),
    MINE(R.string.tab_mine, "tab_mine"),
    PROFILE(R.string.tab_profile, "tab_profile"),
}

/** Everything the screens need, made once per process. Later phases add stores here. */
class StudioGraph(context: Context) {
    val context: Context = context.applicationContext
    val projects = com.xenoorigin.xestudio.projects.ProjectStore(java.io.File(this.context.filesDir, "projects"))
    /** The project open in an editor, if any. */
    val openProject = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    fun open(id: String?) { openProject.value = id }
}

@Composable
fun StudioApp(graph: StudioGraph) {
    val haze = remember { HazeState() }
    var tab by rememberSaveable { mutableStateOf(Tab.CREATE) }
    StudioTheme(haze) {
        Box(Modifier.fillMaxSize().background(Ink.Black)) {
            Box(Modifier.fillMaxSize().hazeSource(haze)) {
                AnimatedContent(
                    tab,
                    transitionSpec = { (fadeIn(Motion.settle()) + scaleIn(Motion.settle(), initialScale = .97f)) togetherWith fadeOut(Motion.snappy()) },
                    label = "tab",
                ) { t ->
                    when (t) {
                        Tab.CREATE -> CreateScreen()
                        Tab.STORE -> SoonScreen(R.string.tab_store, R.string.store_soon)
                        Tab.MINE -> MineScreen(graph)
                        Tab.PROFILE -> SoonScreen(R.string.tab_profile, R.string.profile_soon)
                    }
                }
            }
            TabBar(tab, { tab = it }, haze, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun TabBar(current: Tab, onPick: (Tab) -> Unit, haze: HazeState, modifier: Modifier) {
    Box(
        modifier
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .fillMaxWidth()
            .height(60.dp)
            .glassPanel(haze, RoundedCornerShape(30.dp)),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(6.dp)) {
            val slot = maxWidth / Tab.entries.size
            // A white pill slides to the chosen tab on a spring.
            val x by animateDpAsState(slot * current.ordinal, Motion.settle(), label = "pill")
            Box(Modifier.offset(x = x).width(slot).fillMaxHeight().clip(RoundedCornerShape(24.dp)).background(Ink.White))
            Row(Modifier.fillMaxSize()) {
                for (t in Tab.entries) {
                    val on = t == current
                    Box(
                        Modifier.width(slot).fillMaxHeight().testTag(t.tag).pressSquish()
                            .clickable(remember { MutableInteractionSource() }, null) { onPick(t) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(t.label), style = StudioType.Label.copy(color = if (on) Ink.Black else Ink.Soft))
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateScreen() {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 22.dp, vertical = 18.dp)) {
        Text(stringResource(R.string.create_title), style = StudioType.Display)
        Spacer(Modifier.height(22.dp))
        CreateCard(stringResource(R.string.new_icon_theme), stringResource(R.string.new_icon_theme_desc), big = true) {}
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CreateCard(stringResource(R.string.new_wallpaper), stringResource(R.string.coming_soon), Modifier.weight(1f), enabled = false) {}
            CreateCard(stringResource(R.string.new_widget), stringResource(R.string.coming_soon), Modifier.weight(1f), enabled = false) {}
        }
    }
}

@Composable
internal fun CreateCard(title: String, desc: String, modifier: Modifier = Modifier, big: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .height(if (big) 168.dp else 112.dp)
            .pressSquish()
            .clip(RoundedCornerShape(26.dp))
            .background(if (big) Ink.White else Ink.Card)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(18.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        val fg = if (big) Ink.Black else if (enabled) Ink.White else Ink.Mute
        Text(title, style = StudioType.Title.copy(color = fg))
        Spacer(Modifier.height(4.dp))
        Text(desc, style = StudioType.Body.copy(color = if (big) Ink.Black.copy(alpha = .62f) else Ink.Mute))
    }
}

@Composable
private fun MineScreen(graph: StudioGraph) {
    val projects by graph.projects.projects.collectAsState()
    if (projects.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.mine_empty), style = StudioType.Body, textAlign = TextAlign.Center)
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text(stringResource(R.string.tab_mine), style = StudioType.Display, modifier = Modifier.padding(bottom = 12.dp)) }
        items(projects, key = { it.id }) { p ->
            Row(
                Modifier.fillMaxWidth().animateItem().pressSquish().clip(RoundedCornerShape(22.dp)).background(Ink.Card)
                    .clickable { graph.open(p.id) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Ink.White))
                Spacer(Modifier.width(14.dp))
                Text(p.name, style = StudioType.Title)
            }
        }
    }
}

@Composable
private fun SoonScreen(title: Int, desc: Int) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(Ink.Card))
        Spacer(Modifier.height(18.dp))
        Text(stringResource(title), style = StudioType.Title)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(desc), style = StudioType.Body, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.coming_soon), style = StudioType.Label)
    }
}
