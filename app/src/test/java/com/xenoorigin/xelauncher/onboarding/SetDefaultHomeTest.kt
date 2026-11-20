package com.xenoorigin.xelauncher.onboarding

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SetDefaultHomeTest {
    @Test fun notDefault_firstRun_showsCard() { assertThat(shouldOfferDefaultHome(isDefault = false, firstRunDone = false)).isTrue() }
    @Test fun alreadyDefault_hidesCard() { assertThat(shouldOfferDefaultHome(isDefault = true, firstRunDone = false)).isFalse() }
    @Test fun dismissedBefore_hidesCard() { assertThat(shouldOfferDefaultHome(isDefault = false, firstRunDone = true)).isFalse() }
}
