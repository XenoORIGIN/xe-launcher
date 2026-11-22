package com.xenoorigin.xelauncher.design

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.testutil.SHOT_DIR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

class GlassDimTest {
    @Test fun brightBackdropGetsDimmedDarkDoesNot() {
        assertThat(glassDim(.95f)).isGreaterThan(.15f)
        assertThat(glassDim(.95f)).isAtMost(.3f)
        assertThat(glassDim(.4f)).isEqualTo(0f)
    }
}

class BackdropSourceTest {
    private val bmp = IntSize(360, 780)
    private val screen = Size(1080f, 2340f)
    private val k = 3f * 1.12f

    @Test fun backdropSourceMatchesWallpaperBehindTile() {
        val r = backdropSource(Rect(490f, 1120f, 590f, 1220f), bmp, screen, Offset.Zero)
        assertThat(r.center.x).isWithin(1f).of(180f)
        assertThat(r.center.y).isWithin(1f).of(390f)
        assertThat(r.width).isWithin(.01f).of(100f / k)
    }

    @Test fun backdropSourceFollowsParallax() {
        val a = backdropSource(Rect(490f, 1120f, 590f, 1220f), bmp, screen, Offset.Zero)
        val b = backdropSource(Rect(490f, 1120f, 590f, 1220f), bmp, screen, Offset(33.6f, 0f))
        assertThat(a.left - b.left).isWithin(.5f).of(10f)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h640dp-xxhdpi")
class LiquidGlassTest {
    @get:Rule val rule = androidx.compose.ui.test.junit4.createAndroidComposeRule<androidx.activity.ComponentActivity>()

    /** The window as drawn, without waiting on frames (captureToImage stalls under Robolectric here). */
    private fun windowBitmap(): Bitmap { val v = rule.activity.window.decorView; return Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888).also { v.draw(android.graphics.Canvas(it)) } }

    private fun stripes(): Bitmap {
        val b = Bitmap.createBitmap(360, 640, Bitmap.Config.ARGB_8888)
        val cols = intArrayOf(0xFFE53935.toInt(), 0xFF1E88E5.toInt(), 0xFFFDD835.toInt(), 0xFF43A047.toInt(), 0xFF8E24AA.toInt())
        val c = android.graphics.Canvas(b); val p = android.graphics.Paint()
        for (i in 0 until 18) { p.color = cols[i % cols.size]; c.drawRect(i * 20f, 0f, i * 20f + 20f, 640f, p) }
        return b
    }

    @Test fun liquidGlassWithoutBackdropDrawsFilm() {
        rule.setContent { XeTheme { Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)) { Box(Modifier.size(60.dp).testTag("t").liquidGlass(CircleShape)) } } }
        rule.waitForIdle()
        val r = rule.onNodeWithTag("t").fetchSemanticsNode().boundsInRoot
        val c = windowBitmap().getPixel(r.center.x.toInt(), r.center.y.toInt())
        // Frosted film over black: visibly lighter than the black around it.
        assertThat(android.graphics.Color.red(c)).isGreaterThan(25)
    }

    @Test fun liquidTileShot() {
        val wall = stripes()
        val img = wall.asImageBitmap()
        rule.setContent {
            XeTheme {
                Box(Modifier.fillMaxSize()) {
                    Image(img, null, Modifier.fillMaxSize().scale(com.xenoorigin.xelauncher.wallpaper.WALL_SCALE), contentScale = ContentScale.Crop)
                    CompositionLocalProvider(LocalGlassBackdrop provides GlassBackdrop({ com.xenoorigin.xelauncher.wallpaper.WallpaperRenderer.glassBackdrop(wall).asImageBitmap() }, { Offset.Zero }, { Size(1080f, 1920f) })) {
                        Row(Modifier.padding(top = 120.dp, start = 20.dp)) {
                            listOf(RoundedCornerShape(18.dp), CircleShape, RoundedCornerShape(24.dp), RoundedCornerShape(12.dp)).forEach {
                                Box(Modifier.padding(10.dp).size(64.dp).liquidGlass(it))
                            }
                        }
                    }
                }
            }
        }
        rule.onRoot().captureRoboImage("$SHOT_DIR/liquid_tile.png")
    }

    @Test fun liquidSurfaceOffUsesGlassSurface() {
        var useLiquidSurface by androidx.compose.runtime.mutableStateOf(false)
        rule.setContent {
            XeTheme {
                Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF3355AA))) {
                    if (useLiquidSurface) LiquidSurface(RoundedCornerShape(24.dp), Modifier.size(120.dp)) {}
                    else GlassSurface(RoundedCornerShape(24.dp), Modifier.size(120.dp)) {}
                }
            }
        }
        rule.waitForIdle()
        val a = windowBitmap()
        useLiquidSurface = true
        rule.waitForIdle()
        val b = windowBitmap()
        assertThat(b.sameAs(a)).isTrue()
    }

    @Test fun liquidSurfaceOnIsClear() {
        rule.setContent {
            XeTheme {
                CompositionLocalProvider(LocalLiquidGlass provides true) {
                    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF3355AA))) {
                        LiquidSurface(RoundedCornerShape(24.dp), Modifier.size(120.dp).testTag("s")) {}
                    }
                }
            }
        }
        rule.waitForIdle()
        val r = rule.onNodeWithTag("s").fetchSemanticsNode().boundsInRoot
        val c = windowBitmap().getPixel(r.center.x.toInt(), r.center.y.toInt())
        // No backdrop yet: a frosted film over the blue, so still clearly blue (not the dark glass panel).
        assertThat(android.graphics.Color.blue(c)).isGreaterThan(android.graphics.Color.red(c) + 60)
    }
}
