package com.xenoorigin.xelauncher.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
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
class GlassScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private fun shot(name: String, black: Boolean, quality: GlassQuality) {
        rule.setContent {
            XeTheme(dark = true, glassQuality = quality) {
                TestBackdrop(black) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                        Text("ข้อความภาษาไทย Liquid Glass", style = XeType.LargeTitle, color = Color.White)
                        GlassSurface(RoundedCornerShape(33.dp), Modifier.fillMaxWidth().height(66.dp)) {
                            Text("Category bar", Modifier.align(Alignment.Center), color = Color.White, style = XeType.Headline)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            repeat(3) { GlassSurface(CircleShape, Modifier.size(56.dp)) }
                            GlassSurface(RoundedCornerShape(19.dp), Modifier.size(62.dp))
                        }
                    }
                }
            }
        }
        rule.onRoot().captureRoboImage("$SHOT_DIR/glass_$name.png")
    }

    @Test fun blue_normal() = shot("blue_normal", false, GlassQuality.NORMAL)
    @Test fun black_high() = shot("black_high", true, GlassQuality.HIGH)
    @Test fun blue_saver() = shot("blue_saver", false, GlassQuality.SAVER)
}
