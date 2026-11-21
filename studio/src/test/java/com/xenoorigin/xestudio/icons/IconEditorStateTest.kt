package com.xenoorigin.xestudio.icons

import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xestudio.core.IconOverride
import com.xenoorigin.xestudio.core.IconRecipe
import com.xenoorigin.xestudio.core.TileShape
import com.xenoorigin.xestudio.projects.ProjectKind
import com.xenoorigin.xestudio.projects.ProjectStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class IconEditorStateTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun updateIsSavedAfterDebounce() = runBlocking<Unit> {
        val store = ProjectStore(tmp.root)
        val p = store.create(ProjectKind.ICONS, "A", IconRecipe.encode(IconRecipe()))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val st = IconEditorState(p, store, scope)
        st.update { it.copy(shape = TileShape.CIRCLE) }
        assertThat(st.recipe.value.shape).isEqualTo(TileShape.CIRCLE)
        delay(150)
        assertThat(IconRecipe.decode(store.get(p.id)!!.recipeJson)!!.shape).isEqualTo(TileShape.SQUIRCLE)
        delay(600)
        assertThat(IconRecipe.decode(store.get(p.id)!!.recipeJson)!!.shape).isEqualTo(TileShape.CIRCLE)
        scope.cancel()
    }

    @Test fun overrideSetAndCleared() = runBlocking<Unit> {
        val store = ProjectStore(tmp.root)
        val p = store.create(ProjectKind.ICONS, "A", IconRecipe.encode(IconRecipe()))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val st = IconEditorState(p, store, scope)
        st.setOverride("a/b#0", IconOverride.Letter("Q", 0, com.xenoorigin.xestudio.core.Fill(listOf(0))))
        assertThat(st.recipe.value.overrides).containsKey("a/b#0")
        st.setOverride("a/b#0", null)
        st.flush()
        assertThat(IconRecipe.decode(store.get(p.id)!!.recipeJson)!!.overrides).isEmpty()
        scope.cancel()
    }

    @Test fun replacingAnImageOverrideDeletesTheOldPicture() = runBlocking<Unit> {
        val store = ProjectStore(tmp.root)
        val p = store.create(ProjectKind.ICONS, "A", IconRecipe.encode(IconRecipe()))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val st = IconEditorState(p, store, scope)
        val old = st.saveImage(byteArrayOf(1, 2, 3))
        st.setOverride("a/b#0", IconOverride.Image(old))
        st.setOverride("a/b#0", null)
        assertThat(java.io.File(store.imageDir(p.id), old).exists()).isFalse()
        scope.cancel()
    }
}
