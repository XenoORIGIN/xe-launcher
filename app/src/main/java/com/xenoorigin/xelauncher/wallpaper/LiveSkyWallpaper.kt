package com.xenoorigin.xelauncher.wallpaper

import android.app.WallpaperManager
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.xenoorigin.xelauncher.xeGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Redraws once when shown and then every [TICK_MS] while visible; nothing at all while hidden. */
class SkyTicker(
    private val post: (Runnable, Long) -> Unit,
    private val remove: (Runnable) -> Unit,
    private val draw: () -> Unit,
) {
    private var visible = false
    private val tick = object : Runnable {
        override fun run() { if (!visible) return; draw(); post(this, TICK_MS) }
    }

    fun onVisibility(v: Boolean) {
        if (v == visible) return
        visible = v
        remove(tick)
        if (v) tick.run()
    }

    companion object { const val TICK_MS = 60_000L }
}

/** The lock screen copy is rewritten only when the sky has visibly moved on since the last write. */
fun lockNeedsUpdate(lastWrittenBucket: Int, minute: Int): Boolean = skyBucket(minute) != lastWrittenBucket

/** The background job keeps the phone's home wallpaper fresh only when our live wallpaper isn't doing it. */
fun shouldMirrorHome(liveSkyChosen: Boolean, liveWallpaperActive: Boolean): Boolean = liveSkyChosen && !liveWallpaperActive

object LiveSkyWallpaper {
    private const val JOB_ID = 4207

    fun component(ctx: Context) = ComponentName(ctx, LiveSkyWallpaperService::class.java)

    /** True when the phone's wallpaper is Xe's live sky service. */
    fun isActive(ctx: Context): Boolean =
        runCatching { WallpaperManager.getInstance(ctx).wallpaperInfo?.component == component(ctx) }.getOrDefault(false)

    /** Android's live-wallpaper preview, opened on "Xe Live Sky". */
    fun pickerIntent(ctx: Context): Intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component(ctx))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Starts or stops the 15-minute home mirror job to match the chosen wallpaper. */
    fun schedule(ctx: Context, liveSkyChosen: Boolean) {
        val js = ctx.getSystemService(JobScheduler::class.java) ?: return
        if (!liveSkyChosen) { js.cancel(JOB_ID); return }
        if (js.getPendingJob(JOB_ID) != null) return
        runCatching {
            js.schedule(
                JobInfo.Builder(JOB_ID, ComponentName(ctx, SkyMirrorJob::class.java))
                    .setPeriodic(15 * 60_000L)
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
        }
    }

    private var lastLockBucket = -1

    /** Rewrites the lock screen's sky when the screen turns on, if the user put the live sky there. */
    fun watchScreenOn(ctx: Context) {
        val app = ctx.applicationContext
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                val graph = app.xeGraph
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        val s = graph.settings.current()
                        val minute = minuteNow()
                        if (s.wallpaperId == LIVE_SKY_ID && s.liveSkyLock && lockNeedsUpdate(lastLockBucket, minute)) {
                            lastLockBucket = skyBucket(minute)
                            graph.wallpaper.refreshLockSky(minute)
                        }
                    } finally { pending.finish() }
                }
            }
        }
        runCatching { app.registerReceiver(r, IntentFilter(Intent.ACTION_SCREEN_ON)) }
    }
}

/** Refreshes the phone's home wallpaper with the current sky every 15 minutes (when needed). */
class SkyMirrorJob : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        val graph = applicationContext.xeGraph
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val s = graph.settings.current()
                if (shouldMirrorHome(s.wallpaperId == LIVE_SKY_ID, LiveSkyWallpaper.isActive(this@SkyMirrorJob))) graph.wallpaper.refreshLiveSky()
            } finally { jobFinished(params, false) }
        }
        return true
    }

    override fun onStopJob(params: JobParameters) = true
}

/** "Xe Live Sky" as a real Android live wallpaper: always shows the sky for the current minute. */
class LiveSkyWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = SkyEngine()

    private inner class SkyEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val ticker = SkyTicker({ r, d -> handler.postDelayed(r, d) }, handler::removeCallbacks, ::drawNow)
        private var w = 0
        private var h = 0

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            w = width; h = height
            drawNow()
        }

        override fun onVisibilityChanged(visible: Boolean) = ticker.onVisibility(visible)

        override fun onSurfaceDestroyed(holder: SurfaceHolder) { ticker.onVisibility(false) }

        private fun drawNow() {
            if (w <= 0 || h <= 0) return
            val bmp = WallpaperRenderer.sky(skyAt(minuteNow(), sunToday()), w, h)
            val c = runCatching { surfaceHolder.lockHardwareCanvas() }.getOrNull() ?: runCatching { surfaceHolder.lockCanvas() }.getOrNull() ?: return
            try { c.drawBitmap(bmp, 0f, 0f, null) } finally { runCatching { surfaceHolder.unlockCanvasAndPost(c) } }
            bmp.recycle()
        }
    }
}
