package com.xenoorigin.xelauncher.termi

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class TermiAnimationsTest {
    private val white = 0xFFE8E8EE.toInt()

    @Test fun everyAnimationBuildsAtEveryMoment() {
        for (a in TermiAnim.entries) for (t in listOf(0f, .25f, .5f, .75f, .99f)) {
            val f = TermiAnimations.frame(a, t, (t * 60).toInt())
            assertWithMessage("$a $t").that(f.pose.face in TermiSprite.FACES).isTrue()
            val px = TermiSprite.build(f.pose, Stage.JUNIOR, emptyMap(), white)
            assertWithMessage("$a $t").that(px).isNotEmpty()
            assertWithMessage("$a $t props").that(f.props.all { it.x >= 0 && it.y >= 0 && it.x + it.w <= TermiSprite.BOX_W && it.y + it.h <= TermiSprite.BOX_H }).isTrue()
        }
    }

    @Test fun sceneHints() {
        assertThat(TermiAnimations.frame(TermiAnim.BUILD_FAIL, .9f, 80).scene.rain).isGreaterThan(0f)
        assertThat(TermiAnimations.frame(TermiAnim.BUILD_PASS, .8f, 60).scene.dawn).isGreaterThan(0f)
        assertThat(TermiAnimations.frame(TermiAnim.SLEEP, .9f, 80).scene.dim).isGreaterThan(0f)
    }

    @Test fun propAnimationsBusyTheHands() {
        assertThat(TermiAnimations.frame(TermiAnim.CODING, .3f, 20).pose.handFree).isFalse()
        assertThat(TermiAnimations.frame(TermiAnim.COFFEE, .5f, 30).pose.handFree).isFalse()
        assertThat(TermiAnimations.frame(TermiAnim.PROMPT_AI, .3f, 20).pose.handFree).isFalse()
        assertThat(TermiAnimations.frame(TermiAnim.IDLE, .3f, 20).pose.handFree).isTrue()
        assertThat(TermiAnimations.frame(TermiAnim.CODING, .3f, 20).props).isNotEmpty()
    }

    @Test fun animationsMove() {
        val a = TermiAnimations.frame(TermiAnim.HELLO, .4f, 30)
        val b = TermiAnimations.frame(TermiAnim.HELLO, .45f, 33)
        assertThat(a).isNotEqualTo(b)
    }

    @Test fun directorFollowsEventsAndMood() {
        val d = AnimDirector(java.util.Random(1))
        assertThat(d.next(Mood.HAPPY, TermiEvent.FED, 0)).isEqualTo(TermiAnim.HUNGRY)
        assertThat(d.next(Mood.HAPPY, TermiEvent.PLAYED, 0)).isIn(listOf(TermiAnim.MUSIC, TermiAnim.BUG_SQUASH, TermiAnim.HELLO))
        assertThat(d.next(Mood.HAPPY, TermiEvent.CODED, 0)).isEqualTo(TermiAnim.CODING)
        assertThat(d.next(Mood.HAPPY, null, 0)).isEqualTo(TermiAnim.BUILD_PASS)
        assertThat(d.next(Mood.HAPPY, TermiEvent.LEVEL_UP, 0)).isEqualTo(TermiAnim.BUILD_PASS)
        assertThat(d.next(Mood.ASLEEP, null, 0)).isEqualTo(TermiAnim.NAP)
        assertThat(d.next(Mood.HUNGRY, null, 0)).isEqualTo(TermiAnim.HUNGRY_WAIT)
    }

    @Test fun idlePicksAVibeAnimationOnlyAfterAWhile() {
        val d = AnimDirector(java.util.Random(2))
        // 2.1.2: a routine every 6–14 s (owner wants Termi always on the move).
        assertThat(d.next(Mood.HAPPY, null, 1_000)).isEqualTo(TermiAnim.IDLE)
        assertThat(d.next(Mood.HAPPY, null, 5_000)).isEqualTo(TermiAnim.IDLE)
        assertThat(d.next(Mood.HAPPY, null, 15_000)).isIn(AnimDirector.VIBES)
        assertThat(d.next(Mood.HAPPY, null, 16_000)).isEqualTo(TermiAnim.IDLE)
    }
}
