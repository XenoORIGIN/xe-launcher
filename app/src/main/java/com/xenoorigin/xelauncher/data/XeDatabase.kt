package com.xenoorigin.xelauncher.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CategoryEntity::class, AppCategoryOverride::class, LaunchStat::class, HomeItemEntity::class, FolderEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class XeDatabase : RoomDatabase() {
    abstract fun categories(): CategoryDao
    abstract fun overrides(): OverrideDao
    abstract fun launches(): LaunchDao
    abstract fun home(): HomeDao

    companion object {
        /** v1 -> v2: widgets on the home screen. */
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE home_items ADD COLUMN spanX INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE home_items ADD COLUMN spanY INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE home_items ADD COLUMN widgetId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE home_items ADD COLUMN widgetKind TEXT")
            }
        }
        val MIGRATIONS = arrayOf(MIGRATION_1_2)

        fun open(context: Context): XeDatabase =
            Room.databaseBuilder(context, XeDatabase::class.java, "xe.db").addMigrations(*MIGRATIONS).build()
    }
}
