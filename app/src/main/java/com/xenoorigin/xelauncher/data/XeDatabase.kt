package com.xenoorigin.xelauncher.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CategoryEntity::class, AppCategoryOverride::class, LaunchStat::class, HomeItemEntity::class, FolderEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class XeDatabase : RoomDatabase() {
    abstract fun categories(): CategoryDao
    abstract fun overrides(): OverrideDao
    abstract fun launches(): LaunchDao
    abstract fun home(): HomeDao

    companion object {
        fun open(context: Context): XeDatabase =
            Room.databaseBuilder(context, XeDatabase::class.java, "xe.db").build()
    }
}
