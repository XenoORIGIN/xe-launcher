package com.xenoorigin.xelauncher.widgets

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

/**
 * Other apps' widgets are built from RemoteViews, which only allow plain framework views. AppCompat's
 * inflater swaps TextView for AppCompatTextView, so widgets hosted with the activity's inflater fail
 * with "Couldn't add widget". Widget views must get a context whose inflater has no AppCompat factory.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class WidgetContextTest {
    @Test fun activityInflater_hasAppCompatFactory_butWidgetContextDoesNot() {
        val activity = Robolectric.buildActivity(TestActivity::class.java).setup().get()
        assertThat(LayoutInflater.from(activity).factory2).isNotNull()
        val ctx = widgetContext(activity)
        assertThat(LayoutInflater.from(ctx).factory2).isNull()
        assertThat(LayoutInflater.from(ctx).factory).isNull()
    }

    @Test fun widgetContext_canStillStartActivities() {
        val activity = Robolectric.buildActivity(TestActivity::class.java).setup().get()
        // Clicks inside widgets start PendingIntents from the view's context: keep the activity underneath.
        assertThat((widgetContext(activity) as android.content.ContextWrapper).baseContext).isSameInstanceAs(activity)
    }
}

class TestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        setTheme(androidx.appcompat.R.style.Theme_AppCompat)
        super.onCreate(savedInstanceState)
    }
}
