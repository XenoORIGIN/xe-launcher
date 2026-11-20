package com.xenoorigin.xelauncher.icons

import android.graphics.Color as AColor
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.apps.AppKey
import com.xenoorigin.xelauncher.design.XeTheme
import com.xenoorigin.xelauncher.testutil.SHOT_DIR
import com.xenoorigin.xelauncher.testutil.TestBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Fake adaptive icons: coloured background + white or black glyph. */
fun fakeIcon(glyph: Int, bg: Int, glyphColor: Int = AColor.WHITE): Drawable {
    val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    val fg = ContextCompat.getDrawable(ctx, glyph)!!.mutate().apply { setTint(glyphColor) }
    val inset = android.graphics.drawable.InsetDrawable(fg, 0.3f)
    return AdaptiveIconDrawable(ColorDrawable(bg), inset)
}

val SAMPLE_APPS = listOf(
    Triple("โทรศัพท์", R.drawable.ti_phone, AColor.rgb(52, 199, 89)),
    Triple("Discord", R.drawable.ti_message_circle, AColor.rgb(88, 101, 242)),
    Triple("กล้อง", R.drawable.ti_camera, AColor.rgb(40, 40, 44)),
    Triple("Notion", R.drawable.ti_notes, AColor.WHITE),
)

fun sampleEntry(i: Int, clone: Boolean = false) =
    AppEntry(AppKey("pkg.$i", "pkg.$i.Main", if (clone) 10 else 0), SAMPLE_APPS[i].first, -1, 0, clone)

fun sampleRepo(): IconRepository {
    val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    return IconRepository({ k ->
        val i = k.packageName.removePrefix("pkg.").toInt()
        val (_, g, bg) = SAMPLE_APPS[i]
        fakeIcon(g, bg, if (bg == AColor.WHITE) AColor.BLACK else AColor.WHITE)
    }, File(ctx.cacheDir, "icons-shot-${System.nanoTime()}"))
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h500dp-xxhdpi")
class AppIconScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private fun shot(name: String, black: Boolean) {
        val repo = sampleRepo()
        rule.setContent {
            XeTheme {
                CompositionLocalProvider(LocalIconRepository provides repo) {
                    TestBackdrop(black) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            IconStyle.entries.forEach { st ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    (0..3).forEach { AppIcon(sampleEntry(it, clone = it == 1 && st == IconStyle.GLASS_DARK), 60.dp, st, IconShape.SQUIRCLE, label = true) }
                                }
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle(); Thread.sleep(300); rule.waitForIdle()
        rule.onRoot().captureRoboImage("$SHOT_DIR/icons_$name.png")
    }

    @Test fun onBlack() = shot("black", true)
    @Test fun onBlue() = shot("blue", false)
}
