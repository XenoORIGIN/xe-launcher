package com.xenoorigin.xelauncher.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** BuiltInCategory name for built-ins, null for user categories. */
    val builtIn: String? = null,
    /** User-chosen name; null means "use the built-in's localized label". */
    val name: String? = null,
    val iconName: String,
    val position: Int,
)

@Serializable
@Entity(tableName = "app_category_overrides")
data class AppCategoryOverride(@PrimaryKey val appKey: String, val categoryId: Long)

@Serializable
@Entity(tableName = "launch_stats")
data class LaunchStat(@PrimaryKey val appKey: String, val count: Int, val lastLaunch: Long)

@Serializable
@Entity(tableName = "home_items")
data class HomeItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** APP, FOLDER or WIDGET */
    val type: String,
    val appKey: String? = null,
    val folderId: Long? = null,
    /** PAGE, DOCK or FOLDER */
    val container: String,
    val page: Int = 0,
    val x: Int = 0,
    val y: Int = 0,
    /** Order inside the dock or a folder. */
    val position: Int = 0,
    /** Widgets cover spanX × spanY cells starting at (x, y). */
    @ColumnInfo(defaultValue = "1") val spanX: Int = 1,
    @ColumnInfo(defaultValue = "1") val spanY: Int = 1,
    /** Android AppWidget id (0 for Xe's own widgets). */
    @ColumnInfo(defaultValue = "0") val widgetId: Int = 0,
    /** Xe glass widget kind ("xe_clock", …) or null for Android widgets. */
    val widgetKind: String? = null,
)

@Serializable
@Entity(tableName = "folders")
data class FolderEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String)
