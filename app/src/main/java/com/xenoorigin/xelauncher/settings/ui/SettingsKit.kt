package com.xenoorigin.xelauncher.settings.ui

import com.xenoorigin.xelauncher.design.appear
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.design.GlassIconButton
import com.xenoorigin.xelauncher.design.GlassSurface
import com.xenoorigin.xelauncher.design.LocalXeScheme
import com.xenoorigin.xelauncher.design.SectionTitle
import com.xenoorigin.xelauncher.design.XeRow
import com.xenoorigin.xelauncher.design.XeType

/** Colours for the outline icons on the settings list (as in the owner's reference). */
object RowColors {
    val Green = Color(0xFF30D158); val Orange = Color(0xFFFF9F0A); val Blue = Color(0xFF0A84FF); val Purple = Color(0xFFBF5AF2)
    val Red = Color(0xFFFF453A); val Cyan = Color(0xFF40C8E0); val Pink = Color(0xFFFF375F); val Indigo = Color(0xFF5E5CE6); val Grey = Color(0xFF8E8E93)
}

@Composable
fun SettingsPage(title: String, onBack: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    val scheme = LocalXeScheme.current
    val stagger = remember { com.xenoorigin.xelauncher.design.Stagger() }
    androidx.compose.runtime.CompositionLocalProvider(com.xenoorigin.xelauncher.design.LocalStagger provides stagger) {
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 18.dp).then(Modifier.appear(0, distance = 30f)), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) GlassIconButton(R.drawable.ti_arrow_left, stringResource(R.string.cancel), onBack, Modifier.padding(end = 14.dp))
            PageTitle(title)
        }
        content()
        Spacer(Modifier.height(40.dp))
    }
    }
}

/**
 * A page's big title. Nothing style draws Latin titles in dot-matrix type and ends Thai ones with the red dot.
 */
@Composable
fun PageTitle(title: String) {
    val scheme = LocalXeScheme.current
    val nothing = com.xenoorigin.xelauncher.design.LocalUiStyle.current == com.xenoorigin.xelauncher.design.UiStyle.NOTHING
    when {
        nothing && com.xenoorigin.xelauncher.design.DotFont.canDraw(title) && title.length <= 12 ->
            com.xenoorigin.xelauncher.design.DotText(title.uppercase(), 4.dp, scheme.text)
        nothing -> Row(verticalAlignment = Alignment.Bottom) {
            Text(title, style = XeType.LargeTitle, color = scheme.text)
            Box(Modifier.padding(start = 6.dp, bottom = 10.dp).size(8.dp).background(com.xenoorigin.xelauncher.design.NothingRed, CircleShape))
        }
        else -> Text(title, style = XeType.LargeTitle, color = scheme.text)
    }
}

/** iOS-style grouped glass card. */
@Composable
fun SettingsGroup(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val stagger = com.xenoorigin.xelauncher.design.LocalStagger.current
    val index = remember { stagger?.let { ++it.next } ?: 0 }
    if (title != null) SectionTitle(title, Modifier.padding(start = 6.dp).appear(index))
    GlassSurface(RoundedCornerShape(24.dp), Modifier.fillMaxWidth().padding(bottom = 6.dp).appear(index)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), content = content)
    }
}

/** One choice in a single-select list. */
@Composable
fun ChoiceRow(title: String, selected: Boolean, subtitle: String? = null, onClick: () -> Unit) {
    XeRow(title, subtitle = subtitle, onClick = onClick) {
        Box(Modifier.size(24.dp).background(if (selected) LocalXeScheme.current.text else Color.Transparent, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Icon(painterResource(R.drawable.ti_check), null, Modifier.size(16.dp), tint = LocalXeScheme.current.background)
        }
    }
}

@Composable
fun SliderRow(title: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int = 0, display: (Float) -> String, onChange: (Float) -> Unit) {
    val scheme = LocalXeScheme.current
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row { Text(title, Modifier.weight(1f), style = XeType.Headline, color = scheme.text); Text(display(value), style = XeType.Body, color = scheme.textSecondary) }
        Slider(value, onChange, valueRange = range, steps = steps,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = scheme.text, inactiveTrackColor = scheme.textSecondary.copy(alpha = .3f)))
    }
}

@Composable
fun NavRow(title: String, icon: Int, tint: Color, subtitle: String? = null, enabled: Boolean = true, onClick: () -> Unit) {
    XeRow(title, icon = icon, iconTint = tint, subtitle = subtitle, enabled = enabled, onClick = onClick) {
        Icon(painterResource(R.drawable.ti_chevron_right), null, Modifier.size(20.dp), tint = LocalXeScheme.current.textSecondary)
    }
}

@Composable
fun SettingsColumn(content: @Composable ColumnScope.() -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(0.dp), content = content)
