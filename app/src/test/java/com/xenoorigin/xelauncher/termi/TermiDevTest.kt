package com.xenoorigin.xelauncher.termi

import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.settings.XeSettings
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Developer mode: set Termi's level, unlock every item, start over. */
class TermiDevTest {
    @get:Rule val tmp = TemporaryFolder()
    private fun store() = TermiStore(tmp.root.resolve("t${System.nanoTime()}.json"), defaultCreators = { emptySet() })

    @Test fun devModeIsOffByDefault() = assertThat(XeSettings().devMode).isFalse()

    @Test fun setLevelJumpsStraightThere() {
        val s = store()
        s.setLevel(15)
        assertThat(TermiRules.level(s.current().xp)).isEqualTo(15)
        assertThat(TermiRules.stage(s.current().xp)).isEqualTo(Stage.LEAD)
        s.setLevel(1)
        assertThat(TermiRules.level(s.current().xp)).isEqualTo(1)
        s.setLevel(99)
        assertThat(TermiRules.level(s.current().xp)).isEqualTo(15)
    }

    @Test fun unlockAllLetsAnyItemBeWornAtLevelOne() {
        val s = store()
        s.equip(Item.WIZARD, Item.WIZARD.slot)
        assertThat(s.current().equipped).isEmpty()
        s.setUnlockAll(true)
        assertThat(TermiRules.isUnlocked(s.current(), Item.WIZARD)).isTrue()
        s.equip(Item.WIZARD, Item.WIZARD.slot)
        assertThat(s.current().equipped[Item.WIZARD.slot]).isEqualTo(Item.WIZARD)
    }

    @Test fun resetStartsOverButKeepsTheOwnersChoices() {
        val s = store()
        s.setNight(60, 420); s.setCreator("com.termux", true)
        s.setLevel(10); s.feed()
        s.reset()
        val now = s.current()
        assertThat(now.xp).isEqualTo(0)
        assertThat(now.equipped).isEmpty()
        assertThat(now.nightStart).isEqualTo(60)
        assertThat(s.creatorApps()).contains("com.termux")
    }
}
