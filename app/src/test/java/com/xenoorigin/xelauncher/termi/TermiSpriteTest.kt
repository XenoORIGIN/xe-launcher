package com.xenoorigin.xelauncher.termi

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class TermiSpriteTest {
    private val white = 0xFFE8E8EE.toInt()
    private fun inBox(px: List<Px>) = px.all { it.x >= 0 && it.y >= 0 && it.x + it.w <= TermiSprite.BOX_W && it.y + it.h <= TermiSprite.BOX_H }

    @Test fun everyFaceBuildsInsideTheBox() {
        assertThat(TermiSprite.FACES.size).isAtLeast(24)
        for (f in TermiSprite.FACES) {
            val px = TermiSprite.build(TermiPose(face = f), Stage.JUNIOR, emptyMap(), white)
            assertWithMessage(f).that(inBox(px)).isTrue()
            assertWithMessage(f).that(px.size).isGreaterThan(10)
        }
    }

    @Test fun stagesAddTheirItems() {
        val j = TermiSprite.build(TermiPose(), Stage.JUNIOR, emptyMap(), white)
        val s = TermiSprite.build(TermiPose(), Stage.SENIOR, emptyMap(), white)
        val l = TermiSprite.build(TermiPose(), Stage.LEAD, emptyMap(), white)
        assertThat(j.count { it.argb == TermiSprite.GLASS_FRAME }).isGreaterThan(3)
        assertThat(s.count { it.argb == TermiSprite.HEADPHONE }).isGreaterThan(1)
        assertThat(s.count { it.argb == TermiSprite.HOOD }).isGreaterThan(1)
        assertThat(l.count { it.argb == TermiSprite.GOLD }).isGreaterThan(2)
        assertThat(l.count { it.argb == TermiSprite.MONITOR }).isAtLeast(3)
    }

    @Test fun wornHatReplacesTheCrown() {
        val l = TermiSprite.build(TermiPose(), Stage.LEAD, mapOf(Slot.HEAD to Item.CAP), white)
        assertThat(l.count { it.argb == TermiSprite.GOLD }).isEqualTo(0)
        assertThat(l.count { it.argb == TermiSprite.CAP_RED }).isGreaterThan(1)
    }

    @Test fun everyAccessoryDrawsInsideTheBox() {
        for (it in Item.entries) for (st in Stage.entries) {
            val px = TermiSprite.build(TermiPose(), st, mapOf(it.slot to it), white)
            assertWithMessage("$it $st").that(inBox(px)).isTrue()
        }
    }

    @Test fun heldItemHiddenWhenHandsAreBusy() {
        val free = TermiSprite.build(TermiPose(), Stage.JUNIOR, mapOf(Slot.HAND to Item.DUCK), white)
        val busy = TermiSprite.build(TermiPose(handFree = false), Stage.JUNIOR, mapOf(Slot.HAND to Item.DUCK), white)
        assertThat(free.count { it.argb == TermiSprite.DUCK_YELLOW }).isGreaterThan(2)
        assertThat(busy.count { it.argb == TermiSprite.DUCK_YELLOW }).isEqualTo(0)
    }

    @Test fun bodyColourIsUsedAndDarkAccentsLifted() {
        val blue = 0xFF2F6BFF.toInt()
        assertThat(TermiSprite.build(TermiPose(), Stage.JUNIOR, emptyMap(), blue).count { it.argb == blue }).isGreaterThan(3)
        val c = TermiSprite.bodyColor(0xFF101010)
        assertThat(maxOf((c shr 16) and 255, (c shr 8) and 255, c and 255)).isAtLeast(160)
    }

    @Test fun squashKeepsFeetOnTheGround() {
        val a = TermiSprite.build(TermiPose(), Stage.JUNIOR, emptyMap(), white)
        val b = TermiSprite.build(TermiPose(squash = 1), Stage.JUNIOR, emptyMap(), white)
        assertThat(a.filter { it.argb == white }.maxOf { it.y + it.h }).isEqualTo(b.filter { it.argb == white }.maxOf { it.y + it.h })
        assertThat(b.filter { it.argb == white }.minOf { it.y }).isGreaterThan(a.filter { it.argb == white }.minOf { it.y })
    }
}
