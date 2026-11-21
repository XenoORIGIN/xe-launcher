package com.xenoorigin.xelauncher.drawer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.apps.AppKey
import com.xenoorigin.xelauncher.categories.BuiltInCategory
import com.xenoorigin.xelauncher.categories.CategoryWithApps
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.icons.IconShape
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xelauncher.icons.LocalIconRepository
import com.xenoorigin.xelauncher.icons.sampleRepo
import com.xenoorigin.xelauncher.settings.DrawerLayout
import com.xenoorigin.xelauncher.testutil.SHOT_DIR
import com.xenoorigin.xelauncher.testutil.TestBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class BentoDrawerTest {
    @get:Rule val rule = createComposeRule()

    private val names = listOf("Discord", "Gmail", "LINE", "Meet", "Messages", "Messenger", "Outlook", "Telegram", "WhatsApp", "ข้อความ", "โทรศัพท์", "รายชื่อ")
    private val chat = names.mapIndexed { i, n -> AppEntry(AppKey("pkg.${i % 4}", "pkg.${i % 4}.Main", 800L + i), n, -1, 0, false) }
    private val uses = mapOf("LINE" to 40, "Messenger" to 22)

    private fun cats(): List<CategoryWithApps> = sampleCategories().map { if (it.builtIn == BuiltInCategory.COMMUNICATION) it.copy(apps = chat) else it }

    private fun show(state: DrawerState, play: DrawerPlay = DrawerPlay(), launched: MutableList<String> = mutableListOf(), menus: MutableList<String> = mutableListOf()) {
        val repo = sampleRepo(); val cats = cats()
        rule.setContent {
            XeTheme {
                CompositionLocalProvider(LocalIconRepository provides repo) {
                    TestBackdrop(black = true) {
                        AppDrawer(
                            cats, state,
                            DrawerLook({ IconStyle.GLASS_DARK }, IconShape.SQUIRCLE, 60, true, true, DrawerLayout.BENTO, { uses[it.label] ?: 0 }),
                            { a, _ -> launched += a.label }, { a, _ -> menus += a.label }, {}, {}, {}, play = play,
                        )
                    }
                }
            }
        }
        rule.waitForIdle(); Thread.sleep(300); rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
    }

    private val chatIndex = BuiltInCategory.entries.indexOf(BuiltInCategory.COMMUNICATION)

    @Test fun mostUsedApp_isTheBigCard_andTheNextOneIsWide() {
        show(DrawerState(initial = chatIndex, pageCount = { cats().size }), DrawerPlay(glow = true))
        rule.onRoot().captureRoboImage("$SHOT_DIR/drawer_bento.png")
        val hero = rule.onAllNodesWithTag("bentoHero").onFirst().getBoundsInRoot()
        val wide = rule.onAllNodesWithTag("bentoWide").onFirst().getBoundsInRoot()
        assertThat((hero.right - hero.left).value).isGreaterThan(150f)
        assertThat((hero.bottom - hero.top).value).isGreaterThan(150f)
        assertThat((wide.right - wide.left).value).isGreaterThan(150f)
        assertThat((wide.bottom - wide.top).value).isLessThan(110f)
    }

    @Test fun theBigCard_opensItsApp_andLongPressOpensTheMenu() {
        val launched = mutableListOf<String>(); val menus = mutableListOf<String>()
        show(DrawerState(initial = chatIndex, pageCount = { cats().size }), launched = launched, menus = menus)
        rule.onAllNodesWithTag("bentoHero").onFirst().performClick()
        rule.waitForIdle()
        assertThat(launched).containsExactly("LINE")
        rule.onAllNodesWithTag("bentoWide").onFirst().performTouchInput { longClick() }
        rule.waitForIdle()
        assertThat(menus).containsExactly("Messenger")
    }

    @Test fun dustOnTheBigCard_rubsOff_andSwipesStillTurnPages() {
        val state = DrawerState(initial = chatIndex, pageCount = { cats().size })
        val wiped = mutableListOf<String>(); val launched = mutableListOf<String>()
        show(state, DrawerPlay(pond = true, dust = { if (it.label == "LINE") 1f else 0f }, onWiped = { wiped += it.label }), launched)
        rule.onAllNodesWithTag("bentoHero").onFirst().performTouchInput {
            down(center); advanceEventTime(220)
            repeat(16) { k -> moveBy(Offset(if (k % 2 == 0) 90f else -90f, 6f)); advanceEventTime(30) }
            up()
        }
        rule.mainClock.advanceTimeBy(800); rule.waitForIdle()
        assertThat(wiped).containsExactly("LINE")
        assertThat(launched).isEmpty()
        rule.onRoot().performTouchInput {
            down(Offset(width * .8f, height * .75f))
            repeat(8) { moveBy(Offset(-70f, 0f)); advanceEventTime(10) }
            up()
        }
        rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
        assertThat(state.pager.targetPage).isEqualTo(chatIndex + 1)
    }
}
