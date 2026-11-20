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

    @Test fun androidSocial_isSocial_newsIsMedia() {
        assertThat(Categorizer.categorize("x", ApplicationInfo.CATEGORY_SOCIAL)).isEqualTo(BuiltInCategory.SOCIAL)
        assertThat(Categorizer.categorize("x", ApplicationInfo.CATEGORY_NEWS)).isEqualTo(BuiltInCategory.MEDIA)
    }

    @Test fun packageMap_winsOverAndroidCategory() =
        // Instagram declares IMAGE, but owners expect it under Social.
        assertThat(Categorizer.categorize("com.instagram.android", ApplicationInfo.CATEGORY_IMAGE)).isEqualTo(BuiltInCategory.SOCIAL)

    @Test fun thaiBanksAndWallets_areFinance() {
        listOf("com.kasikorn.retail.mbanking.wap", "com.scb.phone", "ktbcs.netbank", "th.co.truemoney.wallet", "com.bbl.mobilebanking")
            .forEach { assertThat(Categorizer.categorize(it, ApplicationInfo.CATEGORY_UNDEFINED)).isEqualTo(BuiltInCategory.FINANCE) }
    }

    @Test fun shoppingApps_areShopping() {
        listOf("com.shopee.th", "com.lazada.android", "com.alibaba.aliexpresshd")
            .forEach { assertThat(Categorizer.categorize(it, ApplicationInfo.CATEGORY_UNDEFINED)).isEqualTo(BuiltInCategory.SHOPPING) }
    }

    @Test fun rideAndFoodApps_areTravel() {
        listOf("com.grabtaxi.passenger", "com.linecorp.linemanth", "com.google.android.apps.maps")
            .forEach { assertThat(Categorizer.categorize(it, ApplicationInfo.CATEGORY_UNDEFINED)).isEqualTo(BuiltInCategory.TRAVEL) }
    }

    @Test fun javaMinecraftLaunchers_areGames() {
        listOf("net.kdt.pojavlaunch", "org.angelauncher.mojo", "com.tungsten.fcl")
            .forEach { assertThat(Categorizer.categorize(it, ApplicationInfo.CATEGORY_UNDEFINED)).isEqualTo(BuiltInCategory.GAMES) }
    }

    @Test fun productivityApps_areWork() {
        assertThat(Categorizer.categorize("com.notion.id", ApplicationInfo.CATEGORY_UNDEFINED)).isEqualTo(BuiltInCategory.WORK)
        assertThat(Categorizer.categorize("x.y", ApplicationInfo.CATEGORY_PRODUCTIVITY)).isEqualTo(BuiltInCategory.WORK)
    }

    @Test fun packageNameKeywords_guessUnknownApps() {
        assertThat(Categorizer.categorize("com.foo.bankapp", ApplicationInfo.CATEGORY_UNDEFINED)).isEqualTo(BuiltInCategory.FINANCE)
        assertThat(Categorizer.categorize("com.foo.musicplayer", ApplicationInfo.CATEGORY_UNDEFINED)).isEqualTo(BuiltInCategory.MEDIA)
        assertThat(Categorizer.categorize("com.foo.chat", ApplicationInfo.CATEGORY_UNDEFINED)).isEqualTo(BuiltInCategory.COMMUNICATION)
    }

    @Test fun systemUtilities_areTools() {
        assertThat(Categorizer.categorize("com.android.settings", ApplicationInfo.CATEGORY_UNDEFINED)).isEqualTo(BuiltInCategory.TOOLS)
        assertThat(Categorizer.categorize("com.sec.android.app.myfiles", ApplicationInfo.CATEGORY_UNDEFINED)).isEqualTo(BuiltInCategory.TOOLS)
    }
}
