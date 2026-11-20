package com.xenoorigin.xelauncher.categories

import android.content.pm.ApplicationInfo

enum class BuiltInCategory(val iconName: String) {
    COMMUNICATION("message-circle"),
    SOCIAL("affiliate"),
    GAMES("device-gamepad-2"),
    MEDIA("photo"),
    TOOLS("settings"),
    OTHER("category"),
}

object Categorizer {
    private val byPackage: Map<String, BuiltInCategory> = buildMap {
        listOf(
            "com.discord", "jp.naver.line.android", "org.telegram.messenger", "com.facebook.orca", "com.whatsapp",
            "com.samsung.android.messaging", "com.google.android.apps.messaging", "com.samsung.android.dialer",
            "com.google.android.dialer", "com.samsung.android.app.contacts", "com.google.android.contacts",
            "com.google.android.gm", "com.samsung.android.email.provider", "com.microsoft.office.outlook",
            "us.zoom.videomeetings", "com.google.android.apps.meetings", "com.google.android.apps.tachyon",
            "com.microsoft.teams", "com.Slack", "com.skype.raider", "com.viber.voip", "org.thoughtcrime.securesms",
        ).forEach { put(it, BuiltInCategory.COMMUNICATION) }
        listOf(
            "com.facebook.katana", "com.facebook.lite", "com.instagram.android", "com.instagram.barcelona",
            "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.twitter.android", "com.linkedin.android",
            "com.bd.nproject", "com.reddit.frontpage", "com.pinterest", "com.snapchat.android", "com.tumblr",
            "com.padlet.mobile", "jp.naver.line.android.today", "com.bluesky.social",
        ).forEach { put(it, BuiltInCategory.SOCIAL) }
        listOf(
            "com.mojang.minecraftpe", "com.roblox.client", "com.tencent.ig", "com.garena.game.kgth",
            "com.supercell.clashofclans", "com.supercell.brawlstars", "com.mobile.legends", "com.miHoYo.GenshinImpact",
        ).forEach { put(it, BuiltInCategory.GAMES) }
        listOf(
            "com.sec.android.app.camera", "com.sec.android.gallery3d", "com.google.android.apps.photos",
            "com.google.android.youtube", "com.google.android.apps.youtube.music", "com.spotify.music",
            "com.netflix.mediaclient", "com.sec.android.app.music", "com.samsung.android.video",
            "com.google.android.GoogleCamera", "com.joox.mobile", "tv.twitch.android.app", "com.disney.disneyplus",
            "com.sec.android.app.voicenote", "com.samsung.android.app.notes",
        ).forEach { put(it, BuiltInCategory.MEDIA) }
        listOf(
            "com.android.settings", "com.sec.android.app.myfiles", "com.google.android.apps.nbu.files",
            "com.sec.android.app.popupcalculator", "com.google.android.calculator", "com.sec.android.app.clockpackage",
            "com.google.android.deskclock", "com.samsung.android.calendar", "com.google.android.calendar",
            "com.android.chrome", "com.sec.android.app.sbrowser", "com.android.vending", "com.sec.android.app.samsungapps",
            "com.google.android.apps.maps", "com.samsung.android.lool", "com.samsung.android.app.smartcapture",
            "com.google.android.googlequicksearchbox", "com.notion.id", "com.google.android.apps.docs",
            "com.google.android.keep", "com.microsoft.office.officehubrow", "com.anthropic.claude", "com.openai.chatgpt",
            "com.perplexity.app.android", "com.samsung.android.app.watchmanager", "com.samsung.android.oneconnect",
            "com.google.android.apps.authenticator2", "com.samsung.android.spay",
        ).forEach { put(it, BuiltInCategory.TOOLS) }
    }

    fun categorize(packageName: String, androidCategory: Int): BuiltInCategory {
        byPackage[packageName]?.let { return it }
        return when (androidCategory) {
            ApplicationInfo.CATEGORY_GAME -> BuiltInCategory.GAMES
            ApplicationInfo.CATEGORY_SOCIAL, ApplicationInfo.CATEGORY_NEWS -> BuiltInCategory.SOCIAL
            ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_IMAGE -> BuiltInCategory.MEDIA
            ApplicationInfo.CATEGORY_PRODUCTIVITY, ApplicationInfo.CATEGORY_MAPS, ApplicationInfo.CATEGORY_ACCESSIBILITY -> BuiltInCategory.TOOLS
            else -> BuiltInCategory.OTHER
        }
    }
}
