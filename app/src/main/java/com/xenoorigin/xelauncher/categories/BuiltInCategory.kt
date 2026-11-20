package com.xenoorigin.xelauncher.categories

import android.content.pm.ApplicationInfo

/** Built-in categories, in their default bar order. Names are stored in the database: never rename. */
enum class BuiltInCategory(val iconName: String) {
    COMMUNICATION("message-circle"),
    SOCIAL("users"),
    MEDIA("player-play"),
    GAMES("device-gamepad-2"),
    SHOPPING("shopping-bag"),
    FINANCE("wallet"),
    TRAVEL("map-pin"),
    WORK("briefcase"),
    TOOLS("tool"),
    OTHER("apps"),
    ;

    companion object {
        /** Icons used by the first release; rows still carrying them get the new icon on upgrade. */
        val legacyIcons = mapOf(SOCIAL to "affiliate", MEDIA to "photo", TOOLS to "settings", OTHER to "category")
    }
}

object Categorizer {
    private val byPackage: Map<String, BuiltInCategory> = buildMap {
        fun put(c: BuiltInCategory, vararg pkgs: String) = pkgs.forEach { put(it, c) }
        put(
            BuiltInCategory.COMMUNICATION,
            "com.discord", "jp.naver.line.android", "org.telegram.messenger", "com.facebook.orca", "com.whatsapp",
            "com.samsung.android.messaging", "com.google.android.apps.messaging", "com.samsung.android.dialer",
            "com.google.android.dialer", "com.samsung.android.app.contacts", "com.google.android.contacts",
            "com.google.android.gm", "com.samsung.android.email.provider", "com.microsoft.office.outlook",
            "us.zoom.videomeetings", "com.google.android.apps.meetings", "com.google.android.apps.tachyon",
            "com.microsoft.teams", "com.Slack", "com.skype.raider", "com.viber.voip", "org.thoughtcrime.securesms",
            "com.facebook.mlite", "com.tencent.mm", "com.kakao.talk",
        )
        put(
            BuiltInCategory.SOCIAL,
            "com.facebook.katana", "com.facebook.lite", "com.instagram.android", "com.instagram.barcelona",
            "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.twitter.android", "com.linkedin.android",
            "com.bd.nproject", "com.reddit.frontpage", "com.pinterest", "com.snapchat.android", "com.tumblr",
            "com.padlet.mobile", "jp.naver.line.android.today", "xyz.blueskyweb.app", "com.bluesky.social", "com.lemon.lvoverseas",
        )
        put(
            BuiltInCategory.GAMES,
            "com.mojang.minecraftpe", "com.roblox.client", "com.tencent.ig", "com.garena.game.kgth", "com.garena.game.codm",
            "com.supercell.clashofclans", "com.supercell.brawlstars", "com.supercell.clashroyale", "com.mobile.legends",
            "com.miHoYo.GenshinImpact", "com.HoYoverse.hkrpgoversea", "com.dts.freefireth", "com.levelinfinite.sgameGlobal",
            // Minecraft Java launchers
            "net.kdt.pojavlaunch", "net.kdt.pojavlaunch.debug", "git.artdeell.mojo", "org.angelauncher.mojo",
            "com.movtery.zalithlauncher", "com.tungsten.fcl", "com.mio.launcher",
            "com.valvesoftware.steamlink", "com.discord.games",
        )
        put(
            BuiltInCategory.MEDIA,
            "com.sec.android.app.camera", "com.sec.android.gallery3d", "com.google.android.apps.photos",
            "com.google.android.youtube", "com.google.android.apps.youtube.music", "com.spotify.music",
            "com.netflix.mediaclient", "com.sec.android.app.music", "com.samsung.android.video",
            "com.google.android.GoogleCamera", "com.joox.mobile", "tv.twitch.android.app", "com.disney.disneyplus",
            "com.sec.android.app.voicenote", "com.amazon.avod.thirdpartyclient", "com.hbo.hbonow", "com.viu.phone",
            "com.wetv.th", "com.iqiyi.i18n", "com.trueid.mobile", "com.google.android.apps.youtube.kids",
            "com.lemon.lvoverseas.cap", "com.vblast.flipaclip", "com.adobe.lrmobile", "com.picsart.studio",
        )
        put(
            BuiltInCategory.SHOPPING,
            "com.shopee.th", "com.shopee.my", "com.shopee.id", "com.lazada.android", "com.alibaba.aliexpresshd",
            "com.einnovation.temu", "com.amazon.mShop.android.shopping", "com.zzkko", "com.ebay.mobile",
            "com.android.vending", "com.sec.android.app.samsungapps", "com.central.app", "com.lotuss.mobile",
            "th.co.makro.mobile", "com.nike.omega",
        )
        put(
            BuiltInCategory.FINANCE,
            "com.kasikorn.retail.mbanking.wap", "com.scb.phone", "ktbcs.netbank", "com.krungthai.next",
            "com.bbl.mobilebanking", "com.krungsri.kma", "com.tmbbank.oneapp", "com.ttbbank.oneapp", "com.gsb.mymo",
            "com.baac.ababaac", "th.co.truemoney.wallet", "com.paotang.app", "com.ktb.paotang", "com.samsung.android.spay",
            "com.google.android.apps.walletnfcrel", "com.paypal.android.p2pmobile", "com.binance.dev",
            "co.bitkub.mobile", "com.dime.th", "com.settrade.streaming.streamingplus", "th.co.kbank.kplusshop",
        )
        put(
            BuiltInCategory.TRAVEL,
            "com.grabtaxi.passenger", "com.linecorp.linemanth", "com.lineman.app", "com.robinhood.app",
            "com.foodpanda.android", "com.ubercab", "ee.mtakso.client", "com.google.android.apps.maps",
            "com.waze", "com.agoda.mobile.consumer", "com.booking", "com.airbnb.android", "com.traveloka.android",
            "com.airasia.mobile", "com.thaiairways.mobile", "com.bts.rabbitlinepay", "th.co.srt.dsrt",
            "com.here.app.maps", "com.citymapper.app.release",
        )
        put(
            BuiltInCategory.WORK,
            "com.notion.id", "com.google.android.apps.docs", "com.google.android.apps.docs.editors.docs",
            "com.google.android.apps.docs.editors.sheets", "com.google.android.apps.docs.editors.slides",
            "com.google.android.keep", "com.microsoft.office.officehubrow", "com.microsoft.office.word",
            "com.microsoft.office.excel", "com.microsoft.office.powerpoint", "com.microsoft.office.onenote",
            "com.anthropic.claude", "com.openai.chatgpt", "com.perplexity.app.android", "com.google.android.apps.bard",
            "com.google.android.apps.classroom", "com.canva.editor", "com.github.android", "com.trello",
            "com.todoist", "com.samsung.android.app.notes", "com.google.android.apps.tasks", "com.dropbox.android",
            "com.google.android.apps.docs.editors.kix", "com.figma.mirror", "com.termux",
        )
        put(
            BuiltInCategory.TOOLS,
            "com.android.settings", "com.sec.android.app.myfiles", "com.google.android.apps.nbu.files",
            "com.sec.android.app.popupcalculator", "com.google.android.calculator", "com.sec.android.app.clockpackage",
            "com.google.android.deskclock", "com.samsung.android.calendar", "com.google.android.calendar",
            "com.android.chrome", "com.sec.android.app.sbrowser", "com.samsung.android.lool",
            "com.samsung.android.app.smartcapture", "com.google.android.googlequicksearchbox",
            "com.samsung.android.app.watchmanager", "com.samsung.android.oneconnect",
            "com.google.android.apps.authenticator2", "com.samsung.android.app.tips", "com.samsung.android.voc",
            "com.sec.android.app.shealth", "com.samsung.android.forest", "com.samsung.android.themestore",
            "com.samsung.android.app.galaxyfinder", "com.samsung.android.bixby.agent", "com.google.android.apps.translate",
            "com.google.android.apps.googleassistant", "com.google.android.apps.wellbeing", "org.mozilla.firefox",
            "com.opera.browser", "com.microsoft.emmx", "com.brave.browser", "com.samsung.android.app.reminder",
        )
    }

    /** Last-resort guesses from words inside the package name, checked in this order. */
    private val keywords: List<Pair<BuiltInCategory, List<String>>> = listOf(
        BuiltInCategory.FINANCE to listOf("bank", "wallet", "money", "pay", "finance", "invest", "crypto", "stock", "trade"),
        BuiltInCategory.SHOPPING to listOf("shop", "store", "mall", "market", "buy"),
        BuiltInCategory.GAMES to listOf("game", "games", "launcher.mc", "minecraft"),
        BuiltInCategory.COMMUNICATION to listOf("chat", "messag", "mail", "dialer", "contacts", "call"),
        BuiltInCategory.MEDIA to listOf("music", "video", "player", "camera", "gallery", "photo", "movie", "tv", "radio", "podcast", "anime", "manga"),
        BuiltInCategory.TRAVEL to listOf("taxi", "ride", "travel", "flight", "hotel", "food", "delivery", "maps", "transit"),
        BuiltInCategory.WORK to listOf("office", "docs", "note", "task", "todo", "edu", "learn", "school", "ai"),
        BuiltInCategory.TOOLS to listOf("tool", "util", "file", "clock", "calc", "browser", "vpn", "clean", "scanner", "keyboard"),
    )

    fun categorize(packageName: String, androidCategory: Int): BuiltInCategory {
        byPackage[packageName]?.let { return it }
        when (androidCategory) {
            ApplicationInfo.CATEGORY_GAME -> return BuiltInCategory.GAMES
            ApplicationInfo.CATEGORY_SOCIAL -> return BuiltInCategory.SOCIAL
            ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_IMAGE,
            ApplicationInfo.CATEGORY_NEWS -> return BuiltInCategory.MEDIA
            ApplicationInfo.CATEGORY_MAPS -> return BuiltInCategory.TRAVEL
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> return BuiltInCategory.WORK
            ApplicationInfo.CATEGORY_ACCESSIBILITY -> return BuiltInCategory.TOOLS
        }
        val parts = packageName.lowercase().split('.', '_')
        for ((cat, words) in keywords) {
            if (parts.any { part -> words.any { w -> if (w.length <= 3) part == w else part.contains(w) } }) return cat
        }
        return BuiltInCategory.OTHER
    }
}
