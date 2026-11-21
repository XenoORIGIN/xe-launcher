package com.xenoorigin.xestudio.projects

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProjectStoreTest {
    @get:Rule val tmp = TemporaryFolder()
    private var now = 1_000L
    private fun store() = ProjectStore(tmp.root, clock = { now })

    @Test fun createThenReloadFromDisk() = runBlocking<Unit> {
        val p = store().create(ProjectKind.ICONS, "Night", "{\"a\":1}")
        val again = store()
        assertThat(again.projects.value).containsExactly(p)
        assertThat(p.updatedAt).isEqualTo(1_000L)
    }

    @Test fun saveBumpsUpdatedAtAndSortsNewestFirst() = runBlocking<Unit> {
        val s = store()
        val a = s.create(ProjectKind.ICONS, "A", "{}")
        now = 2_000L
        val b = s.create(ProjectKind.ICONS, "B", "{}")
        assertThat(s.projects.value.map { it.name }).containsExactly("B", "A").inOrder()
        now = 3_000L
        val a2 = s.save(a.copy(recipeJson = "{\"x\":2}"))
        assertThat(a2.updatedAt).isEqualTo(3_000L)
        assertThat(s.projects.value.map { it.name }).containsExactly("A", "B").inOrder()
        assertThat(store().projects.value.first().recipeJson).isEqualTo("{\"x\":2}")
        assertThat(b.id).isNotEqualTo(a.id)
    }

    @Test fun deleteRemovesFolderAndImages() = runBlocking<Unit> {
        val s = store()
        val p = s.create(ProjectKind.ICONS, "A", "{}")
        java.io.File(s.imageDir(p.id).apply { mkdirs() }, "o.webp").writeText("x")
        s.delete(p.id)
        assertThat(s.projects.value).isEmpty()
        assertThat(java.io.File(tmp.root, p.id).exists()).isFalse()
    }

    @Test fun corruptProjectIsSkipped() = runBlocking<Unit> {
        val s = store()
        s.create(ProjectKind.ICONS, "Good", "{}")
        java.io.File(tmp.root, "broken").apply { mkdirs() }.resolve("project.json").writeText("{nope")
        assertThat(store().projects.value.map { it.name }).containsExactly("Good")
    }
}
