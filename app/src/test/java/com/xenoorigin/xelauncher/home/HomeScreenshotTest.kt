package com.xenoorigin.xelauncher.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.icons.IconShape
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xelauncher.icons.LocalIconRepository
import com.xenoorigin.xelauncher.icons.sampleEntry
import com.xenoorigin.xelauncher.icons.sampleRepo
import com.xenoorigin.xelauncher.testutil.SHOT_DIR
import com.xenoorigin.xelauncher.testutil.TestBackdrop
import com.xenoorigin.xelauncher.wallpaper.byId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class HomeScreenshotTest {
    @get:Rule val rule = createComposeRule()
    private val look = HomeLook(4, 6, 4, 60, true, IconShape.SQUIRCLE, { IconStyle.GLASS_DARK })
    private val layout = HomeLayout(
        pages = listOf(listOf(
            AppCell(1, 0, 0, sampleEntry(0)), AppCell(2, 1, 0, sampleEntry(1)), AppCell(3, 2, 0, sampleEntry(2)),
            FolderCell(4, 3, 0, 9, "สื่อสาร", listOf(sampleEntry(0), sampleEntry(1), sampleEntry(2), sampleEntry(3))),
            AppCell(5, 0, 1, sampleEntry(3)),
        ), emptyList()),
        dock = listOf(AppCell(6, 0, 0, sampleEntry(0)), AppCell(7, 0, 0, sampleEntry(1)), AppCell(8, 0, 0, sampleEntry(2)), AppCell(10, 0, 0, sampleEntry(3))),
    )
    private val noop = HomeCallbacks({ _, _ -> }, { _, _, _ -> }, { _, _ -> }, {}, {}, {}, {}, {})

    private fun shot(name: String, content: @Composable () -> Unit) {
        val repo = sampleRepo()
        rule.setContent { XeTheme { CompositionLocalProvider(LocalIconRepository provides repo) { TestBackdrop(black = true) { content() } } } }
        rule.waitForIdle(); Thread.sleep(300); rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
        rule.onRoot().captureRoboImage("$SHOT_DIR/home_$name.png")
    }

    private val liquid = look.copy(style = { IconStyle.GLASS_CLEAR })

    private fun liquidShot(name: String, wall: android.graphics.Bitmap, content: @Composable () -> Unit) {
        val repo = sampleRepo()
        rule.setContent { XeTheme { CompositionLocalProvider(LocalIconRepository provides repo) { com.xenoorigin.xelauncher.testutil.LiquidBackdrop(wall) { content() } } } }
        rule.waitForIdle(); Thread.sleep(300); rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
        rule.onRoot().captureRoboImage("$SHOT_DIR/home_$name.png")
    }

    @Test fun homeLiquid() = liquidShot("liquid", com.xenoorigin.xelauncher.testutil.xeWall("ocean")) { HomeScreen(layout, liquid, rememberPagerState { 2 }, false, noop) }

    @Test fun homeLiquidWhite() = liquidShot("liquid_white", android.graphics.Bitmap.createBitmap(1170, 2532, android.graphics.Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.rgb(246, 244, 240)) }) {
        HomeScreen(layout, liquid, rememberPagerState { 2 }, false, noop)
    }

    @Test fun folderLiquid() = liquidShot("folder_liquid", com.xenoorigin.xelauncher.testutil.xeWall("sunset")) {
        Box {
            HomeScreen(layout, liquid, rememberPagerState { 2 }, false, noop)
            FolderPopup(layout.pages[0][3] as FolderCell, Rect(800f, 300f, 980f, 480f), liquid, {}, { _, _ -> }, { _, _ -> }, {})
        }
    }

    @Test fun home() = shot("main") { HomeScreen(layout, look, rememberPagerState { 2 }, false, noop) }

    @Test fun editMode() = shot("edit") {
        Box { HomeScreen(layout, look, rememberPagerState { 3 }, true, noop); EditBar({}, {}, {}, {}, {}) }
    }

    @Test fun iconMenu() = shot("menu") {
        Box {
            HomeScreen(layout, look, rememberPagerState { 2 }, false, noop)
            IconMenu(Rect(60f, 330f, 240f, 520f), emptyList(), { null }, {}, listOf(
                MenuAction(R.drawable.ti_x, "เอาออกจากหน้าหลัก") {}, MenuAction(R.drawable.ti_palette, "แก้ไอคอน") {},
                MenuAction(R.drawable.ti_category, "ย้ายหมวด") {}, MenuAction(R.drawable.ti_eye_off, "ซ่อน") {},
                MenuAction(R.drawable.ti_info_circle, "ข้อมูลแอป") {}, MenuAction(R.drawable.ti_trash, "ถอนการติดตั้ง") {},
            ), {})
        }
    }

    @Test fun folder() = shot("folder") {
        Box {
            HomeScreen(layout, look, rememberPagerState { 2 }, false, noop)
            FolderPopup(layout.pages[0][3] as FolderCell, Rect(800f, 300f, 980f, 480f), look, {}, { _, _ -> }, { _, _ -> }, {})
        }
    }
}
