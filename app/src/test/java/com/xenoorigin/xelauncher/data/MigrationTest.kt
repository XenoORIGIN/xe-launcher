package com.xenoorigin.xelauncher.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/** The first release shipped database version 1; updating the app must keep everyone's home screen. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MigrationTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test fun v1Database_opensWithWidgetColumns_andKeepsItems() = runBlocking<Unit> {
        val name = "mig-${System.nanoTime()}.db"
        val file = ctx.getDatabasePath(name).apply { parentFile?.mkdirs() }
        val schema = JSONObject(File("schemas/com.xenoorigin.xelauncher.data.XeDatabase/1.json").readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val e = entities.getJSONObject(i)
                db.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", e.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.execSQL("INSERT INTO home_items (id, type, appKey, folderId, container, page, x, y, position) VALUES (5, 'APP', 'a/b#0', NULL, 'PAGE', 0, 1, 2, 0)")
            db.version = 1
        }
        val room = Room.databaseBuilder(ctx, XeDatabase::class.java, name).addMigrations(*XeDatabase.MIGRATIONS).allowMainThreadQueries().build()
        val item = room.home().all().single()
        assertThat(item.id).isEqualTo(5)
        assertThat(item.spanX to item.spanY).isEqualTo(1 to 1)
        assertThat(item.widgetId).isEqualTo(0)
        room.close()
    }
}
