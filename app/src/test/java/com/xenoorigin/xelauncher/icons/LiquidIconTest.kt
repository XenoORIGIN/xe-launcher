package com.xenoorigin.xelauncher.icons

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.design.XeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h500dp-xxhdpi")
class LiquidIconTest {
    @get:Rule val rule = androidx.compose.ui.test.junit4.createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun windowBitmap(): Bitmap { val v = rule.activity.window.decorView; return Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888).also { v.draw(android.graphics.Canvas(it)) } }

    @Test fun liquidTileUsesWhiteSymbol() {
        // Notion's symbol is black on white: Liquid Glass must still show it as a white symbol.
        val repo = sampleRepo()
        rule.setContent {
            XeTheme {
                CompositionLocalProvider(LocalIconRepository provides repo) {
                    Box(Modifier.fillMaxSize().background(Color.Black)) {
                        Box(Modifier.testTag("i")) { AppIcon(sampleEntry(3), 60.dp, IconStyle.GLASS_CLEAR, IconShape.SQUIRCLE, label = false) }
                    }
                }
            }
        }
        rule.waitUntil(5_000) { rule.waitForIdle(); true }
        Thread.sleep(300); rule.waitForIdle()
        val r = rule.onNodeWithTag("i").fetchSemanticsNode().boundsInRoot
        val b = windowBitmap()
        var bright = 0; var n = 0
        val x0 = (r.left + r.width * .25f).toInt(); val x1 = (r.right - r.width * .25f).toInt()
        val y0 = (r.top + r.height * .25f).toInt(); val y1 = (r.bottom - r.height * .25f).toInt()
        for (x in x0 until x1) for (y in y0 until y1) { n++; if (android.graphics.Color.red(b.getPixel(x, y)) > 200) bright++ }
        assertThat(bright.toFloat() / n).isGreaterThan(.03f)
    }
}
