package com.xenoorigin.xelauncher.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import kotlin.math.abs

/** Hosts other apps' widgets on Xe's home screen. One instance for the whole app. */
class XeWidgetHost(context: Context) : AppWidgetHost(context.applicationContext, HOST_ID) {
    val manager: AppWidgetManager = AppWidgetManager.getInstance(context.applicationContext)
    private val views = HashMap<Int, XeWidgetHostView>()

    override fun onCreateView(context: Context, appWidgetId: Int, appWidget: AppWidgetProviderInfo?): AppWidgetHostView =
        XeWidgetHostView(context)

    /** One view per widget id, reused across recompositions so the widget isn't rebuilt while scrolling. */
    fun viewFor(context: Context, id: Int): XeWidgetHostView? {
        views[id]?.let { v -> (v.parent as? android.view.ViewGroup)?.removeView(v); return v }
        val info = manager.getAppWidgetInfo(id) ?: return null
        return (createView(widgetContext(context), id, info) as XeWidgetHostView).also { views[id] = it }
    }

    fun release(id: Int) {
        views.remove(id)
        if (id > 0) runCatching { deleteAppWidgetId(id) }
    }

    fun providers(): List<AppWidgetProviderInfo> = runCatching { manager.installedProviders }.getOrDefault(emptyList())

    companion object {
        const val HOST_ID = 0x58E1

        /** Cells a widget wants, from its minimum size. [cellW]/[cellH] are one cell in dp. */
        fun spanFor(info: AppWidgetProviderInfo, density: Float, cellW: Float, cellH: Float, cols: Int, rows: Int): Pair<Int, Int> {
            if (Build.VERSION.SDK_INT >= 31 && info.targetCellWidth > 0 && info.targetCellHeight > 0) {
                return info.targetCellWidth.coerceIn(1, cols) to info.targetCellHeight.coerceIn(1, rows)
            }
            return spanForSize(info.minWidth / density, info.minHeight / density, cellW, cellH, cols, rows)
        }

        fun spanForSize(wDp: Float, hDp: Float, cellW: Float, cellH: Float, cols: Int, rows: Int): Pair<Int, Int> =
            kotlin.math.ceil(wDp / cellW).toInt().coerceIn(1, cols) to kotlin.math.ceil(hDp / cellH).toInt().coerceIn(1, rows)
    }
}

/**
 * Another app's widget on Home. Long press is handled by Home's own gesture (as for icons), never here:
 * a view only hears the rest of a gesture when it took the first touch, so a timer started here can't
 * tell a hold from a swipe and used to open the menu in the middle of swipes.
 */
class XeWidgetHostView(context: Context) : AppWidgetHostView(context) {
    /** Tells the widget how big it is drawn (in dp) so it can pick its layout. */
    private var lastSize = 0f to 0f

    fun setSizeDp(w: Float, h: Float) {
        if (abs(lastSize.first - w) < 1f && abs(lastSize.second - h) < 1f) return
        lastSize = w to h
        if (Build.VERSION.SDK_INT >= 31) {
            updateAppWidgetSize(Bundle(), listOf(SizeF(w, h)))
        } else {
            @Suppress("DEPRECATION")
            updateAppWidgetSize(null, w.toInt(), h.toInt(), w.toInt(), h.toInt())
        }
    }
}

/**
 * Context for other apps' widget views: the activity underneath (so taps can open apps), but with a
 * plain LayoutInflater. AppCompat's inflater swaps TextView for AppCompatTextView, which RemoteViews
 * refuses, so with it every widget fails with "Couldn't add widget".
 */
fun widgetContext(base: Context): Context = PlainInflaterContext(base)

private class PlainInflaterContext(base: Context) : android.content.ContextWrapper(base) {
    private val inflater by lazy { android.view.LayoutInflater.from(base.applicationContext).cloneInContext(this) }
    override fun getSystemService(name: String): Any? =
        if (name == Context.LAYOUT_INFLATER_SERVICE) inflater else super.getSystemService(name)
}
