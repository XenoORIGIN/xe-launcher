package com.xenoorigin.xelauncher.drawer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.design.AnimationSpeed
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.icons.IconShape
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xelauncher.icons.LocalIconRepository
import com.xenoorigin.xelauncher.icons.sampleRepo
import com.xenoorigin.xelauncher.testutil.TestBackdrop
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Picking a category slides its page in with the icons already there; they must not vanish and load a second time. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class DrawerSwitchTest {
    @get:Rule val rule = androidx.compose.ui.test.junit4.createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun brightness(): Double {
        // Draw the window as it is right now (no waiting for animations to finish).
        val v = rule.activity.window.decorView
        val bmp = android.graphics.Bitmap.createBitmap(v.width, v.height, android.graphics.Bitmap.Config.ARGB_8888)
        v.draw(android.graphics.Canvas(bmp))
        var sum = 0.0; var n = 0
        // The first two rows of icons.
        for (y in (bmp.height * .12).toInt() until (bmp.height * .32).toInt() step 3) for (x in 0 until bmp.width step 3) {
            val c = bmp.getPixel(x, y); sum += (android.graphics.Color.red(c) + android.graphics.Color.green(c) + android.graphics.Color.blue(c)) / 3.0; n++
        }
        return sum / n
    }

    @Test fun switchingCategory_iconsStayPut_afterThePageSettles() {
        val repo = sampleRepo()
        val cats = sampleCategories()
        val state = DrawerState(pageCount = { cats.size })
        lateinit var scope: kotlinx.coroutines.CoroutineScope
        rule.setContent {
            scope = androidx.compose.runtime.rememberCoroutineScope()
            XeTheme {
                CompositionLocalProvider(LocalIconRepository provides repo) {
                    TestBackdrop(black = true) {
                        AppDrawer(cats, state, DrawerLook({ IconStyle.GLASS_DARK }, IconShape.SQUIRCLE, 60, true, true), { _, _ -> }, { _, _ -> }, {}, {}, {})
                    }
                }
            }
        }
        rule.waitForIdle(); Thread.sleep(400); rule.mainClock.advanceTimeBy(2000); rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        scope.launch { state.animateTo(1, AnimationSpeed.NORMAL) }
        var guard = 0
        while (state.pager.settledPage != 1 && guard++ < 200) rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(48)
        val justSettled = brightness()
        rule.mainClock.advanceTimeBy(2500)
        val later = brightness()
        assertThat(justSettled).isAtLeast(later * .9)
    }
}
