package com.xenoorigin.xestudio.icons

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.xenoorigin.xestudio.core.IconOverride
import com.xenoorigin.xestudio.core.IconRecipe
import com.xenoorigin.xestudio.projects.Project
import com.xenoorigin.xestudio.projects.ProjectStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * The icon theme being edited. Every change shows at once and is saved to the phone shortly after
 * the user stops fiddling ([SAVE_DELAY_MS]), so dragging a slider doesn't write a file per frame.
 */
class IconEditorState(project: Project, private val store: ProjectStore, private val scope: CoroutineScope) {
    private var project = project
    private val _recipe = MutableStateFlow(IconRecipe.decode(project.recipeJson) ?: IconRecipe())
    val recipe: StateFlow<IconRecipe> = _recipe.asStateFlow()
    val name: String get() = project.name
    val projectId: String get() = project.id
    private var pending: Job? = null
    /** Bumped whenever an override picture changes, so tiles reload it. */
    private val _imageVersion = MutableStateFlow(0)
    val imageVersion: StateFlow<Int> = _imageVersion.asStateFlow()

    fun update(f: (IconRecipe) -> IconRecipe) {
        _recipe.value = f(_recipe.value)
        scheduleSave()
    }

    /** Sets or (with null) removes one app's override. A replaced picture is deleted from the phone. */
    fun setOverride(key: String, o: IconOverride?) {
        val old = _recipe.value.overrides[key]
        if (old is IconOverride.Image && (o as? IconOverride.Image)?.file != old.file) File(store.imageDir(project.id), old.file).delete()
        update { r -> r.copy(overrides = if (o == null) r.overrides - key else r.overrides + (key to o)) }
        _imageVersion.value++
    }

    /** Stores picture bytes in the project and returns the file name to put in an override. */
    fun saveImage(bytes: ByteArray): String {
        val name = UUID.randomUUID().toString().take(12) + ".webp"
        File(store.imageDir(project.id).apply { mkdirs() }, name).writeBytes(bytes)
        return name
    }

    fun loadImage(file: String): Bitmap? =
        File(store.imageDir(project.id), file).takeIf { it.isFile && !file.contains('/') }?.let { BitmapFactory.decodeFile(it.path) }

    /** Saves right now (e.g. when leaving the editor). */
    suspend fun flush() {
        pending?.cancel()
        save()
    }

    private fun scheduleSave() {
        pending?.cancel()
        pending = scope.launch { delay(SAVE_DELAY_MS); save() }
    }

    private suspend fun save() = withContext(Dispatchers.IO) {
        project = store.save(project.copy(recipeJson = IconRecipe.encode(_recipe.value)))
    }

    companion object { const val SAVE_DELAY_MS = 400L }
}
