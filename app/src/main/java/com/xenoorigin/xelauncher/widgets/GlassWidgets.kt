package com.xenoorigin.xelauncher.widgets

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenoorigin.xelauncher.R
import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.design.GlassSurface
import com.xenoorigin.xelauncher.design.LocalGlassStyle
import com.xenoorigin.xelauncher.design.XeType
import com.xenoorigin.xelauncher.design.glassTile
import com.xenoorigin.xelauncher.icons.IconShape
import com.xenoorigin.xelauncher.icons.IconStyle
import com.xenoorigin.xelauncher.icons.IconTile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Xe's own glass widgets. Stored on Home by [id]; they need no permission and no other app. */
enum class GlassWidgetKind(val id: String, val spanX: Int, val spanY: Int, val title: Int) {
    CLOCK("xe_clock", 4, 2, R.string.widget_clock),
    ANALOG("xe_analog", 2, 2, R.string.widget_analog),
    DATE("xe_date", 2, 2, R.string.widget_date),
    MONTH("xe_month", 4, 3, R.string.widget_month),
    SEARCH("xe_search", 4, 1, R.string.widget_search),
    SUGGESTED("xe_suggested", 4, 1, R.string.widget_suggested),
    PHOTO("xe_photo", 2, 2, R.string.widget_photo),
    NOTE("xe_note", 2, 2, R.string.widget_note),
    BATTERY("xe_battery", 2, 2, R.string.widget_battery),
    STORAGE("xe_storage", 2, 2, R.string.widget_storage),
    ;
    companion object { fun of(id: String?) = entries.firstOrNull { it.id == id } }
}

/** What Xe widgets can do in the app around them. Placeholders outside the launcher (previews, tests). */
class WidgetActions(
    val openSearch: () -> Unit = {},
    val launch: (AppEntry) -> Unit = {},
    val suggested: () -> List<AppEntry> = { emptyList() },
    val style: (AppEntry) -> IconStyle = { IconStyle.GLASS_DARK },
    val shape: IconShape = IconShape.SQUIRCLE,
    val photo: (Long) -> File? = { null },
    val pickPhoto: (Long) -> Unit = {},
    val note: (Long) -> String = { "" },
    val editNote: (Long) -> Unit = {},
)

val LocalWidgetActions = staticCompositionLocalOf { WidgetActions() }

private val Accent = Color(0xFFFF6B6B)

/** Current time, refreshed every [stepMs] on the boundary. */
@Composable
private fun rememberNow(stepMs: Long = 60_000): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(stepMs) {
        while (true) { delay(stepMs - System.currentTimeMillis() % stepMs + 30); now = System.currentTimeMillis() }
    }
    return now
}

/** [itemId] identifies this widget on Home (photo and note widgets keep their own content). */
@Composable
fun GlassWidget(kind: GlassWidgetKind, itemId: Long = -1, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(if (kind.spanY == 1) 32.dp else 28.dp)
    if (kind == GlassWidgetKind.PHOTO) { PhotoWidget(itemId, shape, modifier); return }
    GlassSurface(shape, modifier.fillMaxSize()) {
        // A soft light from the top-left gives every widget the same depth.
        Box(Modifier.fillMaxSize().clip(shape).background(Brush.radialGradient(listOf(Color.White.copy(alpha = .10f), Color.Transparent), Offset.Zero, 700f)))
        when (kind) {
            GlassWidgetKind.CLOCK -> ClockWidget()
            GlassWidgetKind.ANALOG -> AnalogWidget()
            GlassWidgetKind.DATE -> DateWidget()
            GlassWidgetKind.MONTH -> MonthWidget()
            GlassWidgetKind.SEARCH -> SearchWidget()
            GlassWidgetKind.SUGGESTED -> SuggestedWidget()
            GlassWidgetKind.NOTE -> NoteWidget(itemId)
            GlassWidgetKind.BATTERY -> BatteryWidget()
            GlassWidgetKind.STORAGE -> StorageWidget()
            GlassWidgetKind.PHOTO -> Unit
        }
    }
}

private fun Modifier.background(b: Brush) = this.then(Modifier.drawBehindBrush(b))
private fun Modifier.drawBehindBrush(b: Brush) = this.drawBehind { drawRect(b) }

@Composable
private fun ClockWidget() {
    val now = rememberNow()
    val locale = Locale.getDefault()
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 16.dp), contentAlignment = Alignment.CenterStart) {
        val big = (maxHeight.value * .5f).coerceIn(30f, 96f)
        Column {
            Text(
                SimpleDateFormat("HH:mm", locale).format(Date(now)), color = Color.White,
                style = XeType.LargeTitle.copy(fontSize = big.sp, lineHeight = (big * 1.02f).sp, fontWeight = FontWeight.Light, letterSpacing = (-1.5).sp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).drawBehindBrush(Brush.linearGradient(listOf(Accent, Accent))))
                Text(SimpleDateFormat("EEEE d MMMM", locale).format(Date(now)), color = Color.White.copy(alpha = .78f), style = XeType.Headline, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun AnalogWidget() {
    val now = rememberNow(1000)
    val c = remember(now) { Calendar.getInstance().apply { timeInMillis = now } }
    val h = c.get(Calendar.HOUR) + c.get(Calendar.MINUTE) / 60f
    val m = c.get(Calendar.MINUTE) + c.get(Calendar.SECOND) / 60f
    val s = c.get(Calendar.SECOND).toFloat()
    Canvas(Modifier.fillMaxSize().padding(14.dp)) {
        val r = size.minDimension / 2; val ctr = center
        for (i in 0 until 60) {
            val major = i % 5 == 0
            rotate(i * 6f, ctr) {
                drawLine(
                    Color.White.copy(alpha = if (major) .85f else .25f),
                    Offset(ctr.x, ctr.y - r), Offset(ctr.x, ctr.y - r + if (major) r * .14f else r * .06f),
                    strokeWidth = if (major) 2.6f * density else 1.2f * density, cap = StrokeCap.Round,
                )
            }
        }
        rotate(h * 30f, ctr) { drawLine(Color.White, ctr, Offset(ctr.x, ctr.y - r * .5f), 5.5f * density, StrokeCap.Round) }
        rotate(m * 6f, ctr) { drawLine(Color.White, ctr, Offset(ctr.x, ctr.y - r * .74f), 3.5f * density, StrokeCap.Round) }
        rotate(s * 6f, ctr) {
            drawLine(Accent, Offset(ctr.x, ctr.y + r * .14f), Offset(ctr.x, ctr.y - r * .82f), 1.6f * density, StrokeCap.Round)
        }
        drawCircle(Accent, 4.5f * density, ctr)
        drawCircle(Color.Black, 1.8f * density, ctr)
    }
}

@Composable
private fun DateWidget() {
    val now = rememberNow()
    val locale = Locale.getDefault()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(SimpleDateFormat("EEEE", locale).format(Date(now)), color = Accent, style = XeType.Headline)
        Text(SimpleDateFormat("d", locale).format(Date(now)), color = Color.White, style = XeType.LargeTitle.copy(fontSize = 58.sp, lineHeight = 62.sp, fontWeight = FontWeight.Light))
        Text(SimpleDateFormat("MMMM yyyy", locale).format(Date(now)), color = Color.White.copy(alpha = .7f), style = XeType.Label)
    }
}

@Composable
private fun MonthWidget() {
    val now = rememberNow()
    val locale = Locale.getDefault()
    val cal = remember(now) { Calendar.getInstance().apply { timeInMillis = now } }
    val today = cal.get(Calendar.DAY_OF_MONTH)
    val first = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val firstDow = cal.firstDayOfWeek
    val lead = (first.get(Calendar.DAY_OF_WEEK) - firstDow + 7) % 7
    val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val names = remember(locale, firstDow) {
        val short = java.text.DateFormatSymbols.getInstance(locale).shortWeekdays // index 1 = Sunday
        (0 until 7).map { i -> short[(firstDow - 1 + i) % 7 + 1].removeSuffix(".").take(3) }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(SimpleDateFormat("MMMM yyyy", locale).format(Date(now)), color = Accent, style = XeType.Headline)
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            names.forEach { Text(it, Modifier.weight(1f), color = Color.White.copy(alpha = .5f), style = XeType.Label, textAlign = TextAlign.Center) }
        }
        val cells = lead + days
        val rows = (cells + 6) / 7
        Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
            for (r in 0 until rows) Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val d = r * 7 + col - lead + 1
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (d in 1..days) {
                            val isToday = d == today
                            Box(
                                Modifier.size(24.dp).clip(CircleShape).then(if (isToday) Modifier.drawBehindBrush(Brush.linearGradient(listOf(Accent, Color(0xFFFF8E53)))) else Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("$d", color = Color.White.copy(alpha = if (isToday) 1f else .86f), style = XeType.Label.copy(fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchWidget() {
    val actions = LocalWidgetActions.current
    Row(
        Modifier.fillMaxSize().clickable { actions.openSearch() }.padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ti_search), null, Modifier.size(22.dp), tint = Color.White)
        Text(stringResource(R.string.search_hint), Modifier.padding(start = 12.dp).weight(1f), color = Color.White.copy(alpha = .6f), style = XeType.Headline, maxLines = 1)
        Icon(painterResource(R.drawable.ti_sparkles), null, Modifier.size(20.dp), tint = Color.White.copy(alpha = .6f))
    }
}

@Composable
private fun SuggestedWidget() {
    val actions = LocalWidgetActions.current
    val apps = remember(actions) { actions.suggested().take(4) }
    Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        if (apps.isEmpty()) Text(stringResource(R.string.suggested_empty), color = Color.White.copy(alpha = .6f), style = XeType.Label, textAlign = TextAlign.Center)
        apps.forEach { a ->
            Box(Modifier.clip(RoundedCornerShape(16.dp)).clickable { actions.launch(a) }.padding(4.dp)) {
                IconTile(a, 46.dp, actions.style(a), actions.shape)
            }
        }
    }
}

@Composable
private fun PhotoWidget(itemId: Long, shape: RoundedCornerShape, modifier: Modifier) {
    val actions = LocalWidgetActions.current
    val file = actions.photo(itemId)
    val version = file?.lastModified()
    val bmp by produceState<ImageBitmap?>(null, file?.path, version) {
        value = file?.let { f -> withContext(Dispatchers.IO) { runCatching { android.graphics.BitmapFactory.decodeFile(f.path)?.asImageBitmap() }.getOrNull() } }
    }
    val b = bmp
    if (b != null) {
        Box(modifier.fillMaxSize().clip(shape).clickable { actions.pickPhoto(itemId) }) {
            Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().glassTile(shape, LocalGlassStyle.current, Color.Transparent))
        }
    } else GlassSurface(shape, modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().clickable { actions.pickPhoto(itemId) }, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(R.drawable.ti_photo), null, Modifier.size(30.dp), tint = Color.White.copy(alpha = .8f))
            Text(stringResource(R.string.widget_photo_pick), color = Color.White.copy(alpha = .7f), style = XeType.Label, modifier = Modifier.padding(top = 6.dp), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun NoteWidget(itemId: Long) {
    val actions = LocalWidgetActions.current
    val text = actions.note(itemId)
    Column(Modifier.fillMaxSize().clickable { actions.editNote(itemId) }.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ti_pencil), null, Modifier.size(16.dp), tint = Color(0xFFFFD60A))
            Text(stringResource(R.string.widget_note), color = Color(0xFFFFD60A), style = XeType.Label, modifier = Modifier.padding(start = 6.dp))
        }
        Spacer(Modifier.size(6.dp))
        Text(
            text.ifBlank { stringResource(R.string.widget_note_empty) },
            color = if (text.isBlank()) Color.White.copy(alpha = .5f) else Color.White,
            style = XeType.Body, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Ring(fraction: Float, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val st = 7.dp.toPx()
        val tl = Offset(st / 2, st / 2); val sz = Size(size.width - st, size.height - st)
        drawArc(Color.White.copy(alpha = .13f), 0f, 360f, false, tl, sz, style = Stroke(st))
        drawArc(color, -90f, 360f * fraction.coerceIn(0f, 1f), false, tl, sz, style = Stroke(st, cap = StrokeCap.Round))
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
        Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
            val c = when { charging != 0 -> Color(0xFF30D158); level in 0..20 -> Color(0xFFFF453A); else -> Color.White }
            Ring(level.coerceAtLeast(0) / 100f, c, Modifier.fillMaxSize())
            Text(if (level < 0) "–" else "$level%", color = Color.White, style = XeType.Headline)
        }
        Text(stringResource(if (charging != 0) R.string.charging else R.string.widget_battery), color = Color.White.copy(alpha = .7f), style = XeType.Label, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun StorageWidget() {
    val now = rememberNow()
    val used by produceState(-1f to "", now) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val st = StatFs(Environment.getDataDirectory().path)
                val total = st.totalBytes.toFloat(); val free = st.availableBytes.toFloat()
                val gb = (total - free) / 1_000_000_000f
                ((total - free) / total) to "%.0f / %.0f GB".format(gb, total / 1_000_000_000f)
            }.getOrDefault(-1f to "")
        }
    }
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
            Ring(used.first.coerceAtLeast(0f), Color(0xFF64D2FF), Modifier.fillMaxSize())
            Text(if (used.first < 0) "–" else "${(used.first * 100).toInt()}%", color = Color.White, style = XeType.Headline)
        }
        Text(used.second.ifEmpty { stringResource(R.string.widget_storage) }, color = Color.White.copy(alpha = .7f), style = XeType.Label, modifier = Modifier.padding(top = 6.dp))
    }
}

