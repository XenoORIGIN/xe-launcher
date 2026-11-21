package com.xenoorigin.xelauncher.widgets

import android.os.Looper
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class WidgetLongPressTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun ev(action: Int, screenX: Float, localX: Float) =
        MotionEvent.obtain(0, 0, action, screenX, 10f, 0).apply { offsetLocation(localX - screenX, 0f) }

    @Test fun holdingStill_opensMenu() {
        val v = XeWidgetHostView(ctx); var fired = false; v.onLongPress = { fired = true }
        v.onInterceptTouchEvent(ev(MotionEvent.ACTION_DOWN, 50f, 50f))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ViewConfiguration.getLongPressTimeout() + 100L))
        assertThat(fired).isTrue()
    }

    @Test fun swipingPages_withFingerOnWidget_doesNotOpenMenu() {
        // While the home pager scrolls, the widget moves with the finger: same spot inside the widget,
        // but the finger has travelled across the screen.
        val v = XeWidgetHostView(ctx); var fired = false; v.onLongPress = { fired = true }
        v.onInterceptTouchEvent(ev(MotionEvent.ACTION_DOWN, 50f, 50f))
        v.onInterceptTouchEvent(ev(MotionEvent.ACTION_MOVE, 250f, 50f))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ViewConfiguration.getLongPressTimeout() + 100L))
        assertThat(fired).isFalse()
    }
}
