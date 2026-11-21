package com.xenoorigin.xelauncher.bugs

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CrashStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun nothingPending_atFirst() {
        assertThat(CrashStore(tmp.root).pending()).isNull()
    }

    @Test fun recordedCrash_isPendingWithItsDetails() {
        val store = CrashStore(tmp.root)
        store.record(IllegalStateException("boom"), "1.0.5", now = 0L)
        val p = store.pending()!!
        assertThat(p).contains("IllegalStateException")
        assertThat(p).contains("boom")
        assertThat(p).contains("1.0.5")
        // A fresh store (next launch) still sees it.
        assertThat(CrashStore(tmp.root).pending()).isEqualTo(p)
    }

    @Test fun clear_removesIt() {
        val store = CrashStore(tmp.root)
        store.record(RuntimeException("x"), "1.0.5", now = 0L)
        store.clear()
        assertThat(store.pending()).isNull()
    }

    @Test fun hugeTraces_areCappedSoTheEmailStillOpens() {
        val store = CrashStore(tmp.root)
        var t: Throwable = RuntimeException("root")
        repeat(400) { t = RuntimeException("wrap $it", t) }
        store.record(t, "1.0.5", now = 0L)
        assertThat(store.pending()!!.length).isAtMost(CrashStore.MAX_CHARS)
    }
}
