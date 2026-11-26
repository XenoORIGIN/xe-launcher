package com.xenoorigin.xelauncher.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.xenoorigin.xelauncher.R

/**
 * Xe's own short interface sounds (taps, toggles, opening, closing, launching). They are quiet, play on
 * the system-sounds stream, and stay silent when the phone is on silent or vibrate or the setting is off.
 */
object XeSound {
    enum class Kind(val raw: Int, val volume: Float) {
        TAP(R.raw.xe_tap, .55f), TOGGLE_ON(R.raw.xe_toggle_on, .6f), TOGGLE_OFF(R.raw.xe_toggle_off, .55f),
        OPEN(R.raw.xe_open, .5f), CLOSE(R.raw.xe_close, .45f), LAUNCH(R.raw.xe_launch, .5f), SECRET(R.raw.xe_secret, .6f),
    }

    /** Lets one sound through per [minGapMs], so a burst of taps or a fling doesn't turn into noise. */
    class Throttle(private val minGapMs: Long) {
        private var last = Long.MIN_VALUE / 2
        @Synchronized fun allow(now: Long): Boolean = (now - last >= minGapMs).also { if (it) last = now }
    }

    fun shouldPlay(enabled: Boolean, ringerMode: Int): Boolean = enabled && ringerMode == AudioManager.RINGER_MODE_NORMAL

    /** Follows the "Sounds" setting. */
    @Volatile var enabled = true

    private var pool: SoundPool? = null
    private var audio: AudioManager? = null
    private val ids = HashMap<Kind, Int>()
    private val throttle = Throttle(minGapMs = 45)

    fun init(context: Context) {
        if (pool != null) return
        val app = context.applicationContext
        audio = app.getSystemService(AudioManager::class.java)
        pool = runCatching {
            SoundPool.Builder().setMaxStreams(3).setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
            ).build().also { p -> Kind.entries.forEach { k -> ids[k] = p.load(app, k.raw, 1) } }
        }.getOrNull()
    }

    fun play(kind: Kind) {
        val p = pool ?: return
        val ringer = audio?.ringerMode ?: return
        if (!shouldPlay(enabled, ringer)) return
        if (kind == Kind.TAP && !throttle.allow(System.currentTimeMillis())) return
        val id = ids[kind] ?: return
        runCatching { p.play(id, kind.volume, kind.volume, 1, 0, 1f) }
    }

    fun tap() = play(Kind.TAP)
}
