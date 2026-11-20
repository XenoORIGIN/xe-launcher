package com.xenoorigin.xelauncher.categories

import android.content.pm.ApplicationInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CategorizerTest {
    @Test fun androidGameCategory_isGames() =
        assertThat(Categorizer.categorize("com.some.game", ApplicationInfo.CATEGORY_GAME)).isEqualTo(BuiltInCategory.GAMES)

    @Test fun knownPackage_withoutCategory_usesMap() =
        assertThat(Categorizer.categorize("com.discord", ApplicationInfo.CATEGORY_UNDEFINED)).isEqualTo(BuiltInCategory.COMMUNICATION)

    @Test fun unknownPackage_isOther() =
        assertThat(Categorizer.categorize("org.unknown.thing", ApplicationInfo.CATEGORY_UNDEFINED)).isEqualTo(BuiltInCategory.OTHER)

    @Test fun androidSocialAndNews_areSocial() {
        assertThat(Categorizer.categorize("x", ApplicationInfo.CATEGORY_SOCIAL)).isEqualTo(BuiltInCategory.SOCIAL)
        assertThat(Categorizer.categorize("x", ApplicationInfo.CATEGORY_NEWS)).isEqualTo(BuiltInCategory.SOCIAL)
    }

    @Test fun packageMap_winsOverAndroidCategory() =
        // Instagram declares IMAGE, but owners expect it under Social.
        assertThat(Categorizer.categorize("com.instagram.android", ApplicationInfo.CATEGORY_IMAGE)).isEqualTo(BuiltInCategory.SOCIAL)
}
