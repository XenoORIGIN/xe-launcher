package com.xenoorigin.xelauncher.home

import com.xenoorigin.xelauncher.apps.AppEntry

sealed interface HomeCell {
    val id: Long
    val x: Int
    val y: Int
}

data class AppCell(override val id: Long, override val x: Int, override val y: Int, val app: AppEntry) : HomeCell

data class FolderCell(
    override val id: Long, override val x: Int, override val y: Int,
    val folderId: Long, val name: String, val apps: List<AppEntry>,
) : HomeCell

/** A widget: an Android AppWidget ([widgetId] > 0) or one of Xe's glass widgets ([kind]). */
data class WidgetCell(
    override val id: Long, override val x: Int, override val y: Int,
    val spanX: Int, val spanY: Int, val widgetId: Int, val kind: String?,
) : HomeCell

data class HomeLayout(val pages: List<List<HomeCell>>, val dock: List<HomeCell>) {
    companion object { val Empty = HomeLayout(listOf(emptyList()), emptyList()) }
}
