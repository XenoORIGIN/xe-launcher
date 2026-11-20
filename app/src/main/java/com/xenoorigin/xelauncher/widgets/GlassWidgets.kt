package com.xenoorigin.xelauncher.widgets

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.design.GlassSurface
import com.xenoorigin.xelauncher.design.XeType
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Xe's own glass widgets. Stored on Home by kind; they need no permission and no other app. */
enum class GlassWidgetKind(val id: String, val spanX: Int, val spanY: Int, val title: Int) {
    CLOCK("xe_clock", 4, 2, R.string.widget_clock),
    DATE("xe_date", 2, 2, R.string.widget_date),
    BATTERY("xe_battery", 2, 2, R.string.widget_battery),
    ;
    companion object { fun of(id: String?) = entries.firstOrNull { it.id == id } }
}

/** Current time, refreshed on each minute boundary. */
@Composable
private fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(60_000 - System.currentTimeMillis() % 60_000 + 50); now = System.currentTimeMillis() }
    }
    return now
}

@Composable
fun GlassWidget(kind: GlassWidgetKind, modifier: Modifier = Modifier) {
    GlassSurface(RoundedCornerShape(28.dp), modifier.fillMaxSize()) {
        when (kind) {
            GlassWidgetKind.CLOCK -> ClockWidget()
            GlassWidgetKind.DATE -> DateWidget()
            GlassWidgetKind.BATTERY -> BatteryWidget()
        }
    }
}

@Composable
private fun ClockWidget() {
    val now = rememberNow()
    val locale = Locale.getDefault()
    BoxWithConstraints(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.CenterStart) {
        val big = (maxHeight.value * .42f).coerceIn(28f, 84f)
        Column {
            Text(SimpleDateFormat("HH:mm", locale).format(Date(now)), color = Color.White, style = XeType.LargeTitle.copy(fontSize = big.sp, lineHeight = big.sp, fontWeight = FontWeight.Light))
            Text(SimpleDateFormat("EEEE d MMMM", locale).format(Date(now)), color = Color.White.copy(alpha = .75f), style = XeType.Headline)
        }
    }
}

@Composable
private fun DateWidget() {
    val now = rememberNow()
    val locale = Locale.getDefault()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(SimpleDateFormat("EEEE", locale).format(Date(now)), color = Color(0xFFFF6B6B), style = XeType.Headline)
        Text(SimpleDateFormat("d", locale).format(Date(now)), color = Color.White, style = XeType.LargeTitle.copy(fontSize = 52.sp, lineHeight = 56.sp, fontWeight = FontWeight.Light))
        Text(SimpleDateFormat("MMMM", locale).format(Date(now)), color = Color.White.copy(alpha = .7f), style = XeType.Label)
    }
}

@Composable
private fun BatteryWidget() {
    val ctx = LocalContext.current
    val now = rememberNow()
    var level by remember { mutableIntStateOf(-1) }
    var charging by remember { mutableIntStateOf(0) }
    LaunchedEffect(now) {
        val i: Intent? = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val l = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val s = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        level = if (l < 0) -1 else l * 100 / s.coerceAtLeast(1)
        charging = i?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
    }
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val st = 7.dp.toPx()
                val tl = Offset(st / 2, st / 2); val sz = Size(size.width - st, size.height - st)
                drawArc(Color.White.copy(alpha = .15f), 0f, 360f, false, tl, sz, style = Stroke(st))
                val c = when { level in 0..20 -> Color(0xFFFF453A); charging != 0 -> Color(0xFF30D158); else -> Color.White }
                drawArc(c, -90f, 360f * level.coerceAtLeast(0) / 100f, false, tl, sz, style = Stroke(st, cap = StrokeCap.Round))
            }
            Text(if (level < 0) "–" else "$level%", color = Color.White, style = XeType.Headline)
        }
        Row(Modifier.padding(top = 6.dp)) {
            Text(stringResource(if (charging != 0) R.string.charging else R.string.widget_battery), color = Color.White.copy(alpha = .7f), style = XeType.Label)
        }
    }
}
