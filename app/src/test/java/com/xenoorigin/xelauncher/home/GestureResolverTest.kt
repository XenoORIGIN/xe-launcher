package com.xenoorigin.xelauncher.home

import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.settings.GestureAction
import com.xenoorigin.xelauncher.settings.XeSettings
import org.junit.Test

class GestureResolverTest {
    private val s = XeSettings()
    private fun r(dx: Float, dy: Float, v: Float = 0f, pointers: Int = 1, settings: XeSettings = s) =
        GestureResolver.resolve(dx, dy, v, pointers, settings, thresholdPx = 100f, flingPx = 2000f)

    @Test fun swipeUp_opensDrawerByDefault() { assertThat(r(0f, -150f)).isEqualTo(GestureAction.OPEN_DRAWER) }
    @Test fun swipeDown_notificationsByDefault() { assertThat(r(10f, 160f)).isEqualTo(GestureAction.NOTIFICATIONS) }
    @Test fun twoFingerDown_usesItsOwnSetting() { assertThat(r(0f, 160f, pointers = 2)).isEqualTo(GestureAction.OPEN_SEARCH) }
    @Test fun shortMove_isNothing() { assertThat(r(0f, -40f)).isNull() }
    @Test fun fastFlick_countsEvenIfShort() { assertThat(r(0f, -40f, v = -3000f)).isEqualTo(GestureAction.OPEN_DRAWER) }
    @Test fun mostlyHorizontal_isLeftToPager() { assertThat(r(300f, 120f)).isNull() }
    @Test fun customised_swipeDownToSearch() {
        assertThat(r(0f, 200f, settings = s.copy(swipeDown = GestureAction.OPEN_SEARCH))).isEqualTo(GestureAction.OPEN_SEARCH)
    }
    @Test fun none_isNull() { assertThat(r(0f, 200f, settings = s.copy(swipeDown = GestureAction.NONE))).isNull() }
}
