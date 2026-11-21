package com.xenoorigin.xestudio.projects

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/** What a project makes. Wallpapers and widgets arrive in later versions. */
@Serializable enum class ProjectKind { ICONS }

@Serializable data class Project(
    val id: String,
    val kind: ProjectKind,
    val name: String,
    val recipeJson: String,
    val updatedAt: Long,
)

/**
 * Keeps the user's work on this phone: one folder per project with project.json and an images/ folder.
 * Writes are atomic, and a folder that can't be read is skipped so one bad file never hides the rest.
 */
class ProjectStore(private val dir: File, private val clock: () -> Long = System::currentTimeMillis) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val lock = Mutex()
    private val _projects = MutableStateFlow(load())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private fun load(): List<Project> =
        (dir.listFiles() ?: emptyArray()).filter { it.isDirectory }.mapNotNull { d ->
            runCatching { json.decodeFromString(Project.serializer(), File(d, FILE).readText()) }.getOrNull()?.takeIf { it.id == d.name }
        }.sortedByDescending { it.updatedAt }

    suspend fun create(kind: ProjectKind, name: String, recipeJson: String): Project =
        write(Project(UUID.randomUUID().toString(), kind, name, recipeJson, clock()))

    /** Saves [p] with a fresh updatedAt and returns what was saved. */
    suspend fun save(p: Project): Project = write(p.copy(updatedAt = clock()))

    suspend fun delete(id: String) = lock.withLock {
        withContext(Dispatchers.IO) { File(dir, id).deleteRecursively() }
        _projects.value = _projects.value.filterNot { it.id == id }
    }

    fun get(id: String): Project? = _projects.value.firstOrNull { it.id == id }

    /** Where pictures used by project [id] live. */
    fun imageDir(id: String): File = File(File(dir, id), "images")

    private suspend fun write(p: Project): Project = lock.withLock {
        withContext(Dispatchers.IO) {
            val folder = File(dir, p.id).apply { mkdirs() }
            val tmp = File(folder, "$FILE.tmp")
            tmp.writeText(json.encodeToString(Project.serializer(), p))
            if (!tmp.renameTo(File(folder, FILE))) { tmp.copyTo(File(folder, FILE), overwrite = true); tmp.delete() }
        }
        _projects.value = (_projects.value.filterNot { it.id == p.id } + p).sortedByDescending { it.updatedAt }
        p
    }

    private companion object { const val FILE = "project.json" }
}
