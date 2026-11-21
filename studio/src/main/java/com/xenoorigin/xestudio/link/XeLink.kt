package com.xenoorigin.xestudio.link

import android.content.Context
import android.content.Intent
import com.xenoorigin.xestudio.core.IconOverride
import com.xenoorigin.xestudio.core.IconRecipe
import com.xenoorigin.xestudio.projects.Project
import com.xenoorigin.xestudio.projects.ProjectStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class ApplyResult { APPLIED, XE_MISSING, CANT_OPEN }

/**
 * Hands themes to Xe Launcher. The applied theme is copied to filesDir/applied/, served by [ThemeProvider],
 * and Xe is told with a broadcast that only apps signed with Xe's key can receive.
 */
object XeLink {
    const val XE_PACKAGE = "com.xenoorigin.xelauncher"
    const val AUTHORITY = "com.xenoorigin.xestudio.themes"
    const val PERMISSION = "com.xenoorigin.xestudio.permission.READ_THEME"
    const val ACTION_THEME_APPLIED = "com.xenoorigin.xestudio.action.THEME_APPLIED"
    const val XE_WEBSITE = "https://xenoorigin.github.io/xe-launcher/"

    fun appliedDir(ctx: Context) = File(ctx.filesDir, "applied")

    fun isXeInstalled(ctx: Context): Boolean =
        runCatching { ctx.packageManager.getPackageInfo(XE_PACKAGE, 0) }.isSuccess

    suspend fun applyIcons(ctx: Context, project: Project, store: ProjectStore): ApplyResult {
        if (!isXeInstalled(ctx)) return ApplyResult.XE_MISSING
        val recipe = IconRecipe.decode(project.recipeJson) ?: return ApplyResult.CANT_OPEN
        withContext(Dispatchers.IO) {
            val dir = appliedDir(ctx).apply { mkdirs() }
            val images = File(dir, "images")
            images.deleteRecursively(); images.mkdirs()
            for (o in recipe.overrides.values) if (o is IconOverride.Image) {
                File(store.imageDir(project.id), o.file).takeIf { it.isFile }?.copyTo(File(images, o.file), overwrite = true)
            }
            atomicWrite(File(dir, "icons.json"), IconRecipe.encode(recipe))
            atomicWrite(File(dir, "version"), System.currentTimeMillis().toString())
        }
        ctx.sendBroadcast(Intent(ACTION_THEME_APPLIED).setPackage(XE_PACKAGE), PERMISSION)
        return ApplyResult.APPLIED
    }

    private fun atomicWrite(f: File, text: String) {
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(f)) { tmp.copyTo(f, overwrite = true); tmp.delete() }
    }
}
