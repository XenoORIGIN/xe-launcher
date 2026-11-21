package com.xenoorigin.xelauncher.studio

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Opens Xe Studio, or its download when it isn't installed yet. */
object StudioLink {
    const val DOWNLOAD = "https://github.com/XenoORIGIN/xe-launcher/releases/latest/download/xe-studio.apk"

    fun isInstalled(ctx: Context): Boolean =
        runCatching { ctx.packageManager.getPackageInfo(StudioThemeStore.STUDIO_PACKAGE, 0) }.isSuccess

    fun open(ctx: Context) {
        val launch = ctx.packageManager.getLaunchIntentForPackage(StudioThemeStore.STUDIO_PACKAGE)
        val intent = launch ?: Intent(Intent.ACTION_VIEW, Uri.parse(DOWNLOAD))
        runCatching { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
