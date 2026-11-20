package com.xenoorigin.xelauncher.backup

import androidx.room.withTransaction
import com.xenoorigin.xelauncher.data.AppCategoryOverride
import com.xenoorigin.xelauncher.data.CategoryEntity
import com.xenoorigin.xelauncher.data.FolderEntity
import com.xenoorigin.xelauncher.data.HomeItemEntity
import com.xenoorigin.xelauncher.data.LaunchStat
import com.xenoorigin.xelauncher.data.XeDatabase
import com.xenoorigin.xelauncher.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class DbSnapshot(
    val version: Int = 1,
    val categories: List<CategoryEntity>,
    val overrides: List<AppCategoryOverride>,
    val home: List<HomeItemEntity>,
    val folders: List<FolderEntity>,
    val launches: List<LaunchStat>,
)

/** `.xebackup` = zip of settings.json, db.json and wallpaper.png. Import is all-or-nothing. */
class BackupManager(
    private val db: XeDatabase,
    private val settings: SettingsStore,
    private val wallpaperFile: () -> File?,
    private val restoreWallpaper: suspend (File) -> Unit,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun export(out: OutputStream) = withContext(Dispatchers.IO) {
        val snap = DbSnapshot(
            categories = db.categories().all(), overrides = db.overrides().all(), home = db.home().all(),
            folders = db.home().folders(), launches = db.launches().all(),
        )
        ZipOutputStream(out).use { z ->
            z.putNextEntry(ZipEntry("settings.json")); z.write(settings.exportJson().toByteArray()); z.closeEntry()
            z.putNextEntry(ZipEntry("db.json")); z.write(json.encodeToString(DbSnapshot.serializer(), snap).toByteArray()); z.closeEntry()
            wallpaperFile()?.takeIf { it.exists() }?.let { f ->
                z.putNextEntry(ZipEntry("wallpaper.png")); f.inputStream().use { it.copyTo(z) }; z.closeEntry()
            }
        }
    }

    suspend fun import(input: InputStream): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val entries = HashMap<String, ByteArray>()
            ZipInputStream(input).use { z ->
                while (true) {
                    val e = z.nextEntry ?: break
                    if (e.name in setOf("settings.json", "db.json", "wallpaper.png")) entries[e.name] = z.readBytes()
                }
            }
            val settingsText = entries["settings.json"]?.decodeToString() ?: error("missing settings")
            val snap = json.decodeFromString(DbSnapshot.serializer(), entries["db.json"]?.decodeToString() ?: error("missing db"))
            require(snap.version == 1)
            db.withTransaction {
                db.categories().clear(); db.overrides().clear(); db.home().clear(); db.home().clearFolders(); db.launches().clear()
                db.categories().insertAll(snap.categories); db.overrides().insertAll(snap.overrides)
                db.home().insertFolders(snap.folders); db.home().insertAll(snap.home); db.launches().insertAll(snap.launches)
            }
            settings.importJson(settingsText)
            entries["wallpaper.png"]?.let { bytes ->
                val tmp = File.createTempFile("restore", ".png"); tmp.writeBytes(bytes); restoreWallpaper(tmp); tmp.delete()
            }
            Unit
        }
    }
}
