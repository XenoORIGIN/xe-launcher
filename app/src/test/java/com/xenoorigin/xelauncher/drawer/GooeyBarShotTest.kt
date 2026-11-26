package com.xenoorigin.xelauncher.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.xenoorigin.xelauncher.design.GlassBackdrop
import com.xenoorigin.xelauncher.design.LocalGlassBackdrop
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.testutil.SHOT_DIR
import com.xenoorigin.xelauncher.wallpaper.WallpaperPreset
import com.xenoorigin.xelauncher.wallpaper.WallpaperRenderer
import com.xenoorigin.xelauncher.wallpaper.XeCollection
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class GooeyBarShotTest {
    @get:Rule val rule = createComposeRule()

    private fun show(wallId: String, name: String) {
        com.xenoorigin.xelauncher.design.lensDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
        val wall = WallpaperRenderer.render(XeCollection.first { it.id == wallId }, 1170, 2532)
        val backdrop = WallpaperRenderer.glassBackdrop(wall).asImageBitmap()
        rule.setContent {
            XeTheme {
                val gb = GlassBackdrop({ backdrop }, { Offset.Zero }, { Size(1170f, 2532f) })
                CompositionLocalProvider(LocalGlassBackdrop provides gb) {
                    Box(Modifier.fillMaxSize()) {
                        Image(wall.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceEvenly) {
                            listOf(0f, .45f, 1f).forEach { l -> GooeyBar({}, lift = l) { m -> Box(m) } }
                        }
                    }
                }
            }
        }
        rule.waitForIdle(); rule.mainClock.advanceTimeBy(100); rule.waitForIdle()
        rule.onRoot().captureRoboImage("$SHOT_DIR/$name")
    }

    @Test fun garden() = show("bokeh_garden", "gooey_garden.png")
    @Test fun blossom() = show("bokeh_blossom", "gooey_blossom.png")

    @Test fun bubbleOpensSearch() {
        var opened = 0
        rule.setContent { XeTheme { GooeyBar({ opened++ }) { m -> Box(m) } } }
        rule.onNodeWithTag("gooeySearch").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        com.google.common.truth.Truth.assertThat(opened).isEqualTo(1)
    }
}
