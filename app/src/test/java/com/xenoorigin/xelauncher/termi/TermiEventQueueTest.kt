package com.xenoorigin.xelauncher.termi

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** 2.1 bug hunt: feeding that also levels Termi up played only the level-up; now both play, one after the other. */
class TermiEventQueueTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun eventsThatArriveTogetherPlayInTurn() {
        val ui = TermiUi(TermiStore(tmp.root.resolve("t.json")))
        ui.listener(TermiEvent.FED); ui.listener(TermiEvent.LEVEL_UP)
        assertThat(ui.event).isEqualTo(TermiEvent.FED)
        val k = ui.eventKey
        ui.done()
        assertThat(ui.event).isEqualTo(TermiEvent.LEVEL_UP)
        assertThat(ui.eventKey).isGreaterThan(k)
        val k2 = ui.eventKey
        ui.done()
        assertThat(ui.eventKey).isEqualTo(k2) // nothing left: no replay
        ui.listener(TermiEvent.PLAYED)
        assertThat(ui.event).isEqualTo(TermiEvent.PLAYED)
    }
}
