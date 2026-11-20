package com.xenoorigin.xelauncher.categories

import android.content.pm.ApplicationInfo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.apps.AppKey
import com.xenoorigin.xelauncher.data.XeDatabase
import com.xenoorigin.xelauncher.settings.DrawerSort
import com.xenoorigin.xelauncher.settings.XeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private fun app(pkg: String, label: String, cat: Int = ApplicationInfo.CATEGORY_UNDEFINED, installed: Long = 0) =
    AppEntry(AppKey(pkg, "$pkg.Main", 0), label, cat, installed, false)

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CategoryRepositoryTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val db = Room.inMemoryDatabaseBuilder(ctx, XeDatabase::class.java).allowMainThreadQueries().build()
    private val apps = MutableStateFlow(listOf<AppEntry>())
    private val settings = MutableStateFlow(XeSettings())
    private val repo = CategoryRepository(db, apps, settings) { it.name }

    @After fun close() = db.close()

    @Test fun ensureDefaults_twice_keepsSix() = runBlocking<Unit> {
        repo.ensureDefaults(); repo.ensureDefaults()
        assertThat(db.categories().all()).hasSize(6)
    }

    @Test fun moveApp_showsInNewCategoryOnly() = runBlocking<Unit> {
        repo.ensureDefaults()
        val discord = app("com.discord", "Discord")
        apps.value = listOf(discord)
        val games = db.categories().all().first { it.builtIn == "GAMES" }.id
        repo.moveApp(discord.key, games)
        val cats = repo.categories.first()
        assertThat(cats.single { it.id == games }.apps.map { it.label }).containsExactly("Discord")
        assertThat(cats.none { it.id != games && it.apps.any { a -> a.label == "Discord" } }).isTrue()
    }

    @Test fun deleteUserCategory_returnsAppsToAuto() = runBlocking<Unit> {
        repo.ensureDefaults()
        val discord = app("com.discord", "Discord")
        apps.value = listOf(discord)
        val mine = repo.add("งาน", "briefcase")
        repo.moveApp(discord.key, mine)
        repo.delete(mine)
        val cats = repo.categories.first()
        assertThat(cats.single { it.apps.any { a -> a.label == "Discord" } }.title).isEqualTo("COMMUNICATION")
        assertThat(cats.none { it.id == mine }).isTrue()
    }

    @Test fun emptyBuiltIn_hidden_emptyUser_kept() = runBlocking<Unit> {
        repo.ensureDefaults()
        apps.value = listOf(app("com.discord", "Discord"))
        val mine = repo.add("ว่าง", "star")
        val titles = repo.categories.first().map { it.title }
        assertThat(titles).containsExactly("COMMUNICATION", "ว่าง").inOrder()
    }

    @Test fun reorder_persists() = runBlocking<Unit> {
        repo.ensureDefaults()
        val ids = db.categories().all().sortedBy { it.position }.map { it.id }
        repo.reorder(ids.reversed())
        assertThat(db.categories().all().sortedBy { it.position }.map { it.id }).isEqualTo(ids.reversed())
    }

    @Test fun alphabetical_latinBeforeThai_thaiCollated() = runBlocking<Unit> {
        repo.ensureDefaults()
        apps.value = listOf("Zoom", "กล้อง", "Apple", "ไลน์").map { app("p.$it", it) }
        val other = repo.categories.first().single { it.title == "OTHER" }
        assertThat(other.apps.map { it.label }).containsExactly("Apple", "Zoom", "กล้อง", "ไลน์").inOrder()
    }

    @Test fun mostUsed_ordersByLaunchCount() = runBlocking<Unit> {
        repo.ensureDefaults()
        val a = app("p.a", "A"); val b = app("p.b", "B")
        apps.value = listOf(a, b)
        settings.value = XeSettings(drawerSort = DrawerSort.MOST_USED)
        repeat(3) { repo.recordLaunch(b.key) }
        repo.recordLaunch(a.key)
        assertThat(repo.categories.first().single { it.title == "OTHER" }.apps.map { it.label }).containsExactly("B", "A").inOrder()
    }

    @Test fun hiddenApps_excluded() = runBlocking<Unit> {
        repo.ensureDefaults()
        val a = app("p.a", "A")
        apps.value = listOf(a, app("p.b", "B"))
        settings.value = XeSettings(hiddenApps = setOf(a.key.encode()))
        assertThat(repo.categories.first().flatMap { it.apps }.map { it.label }).containsExactly("B")
    }

    @Test fun otherCategory_cannotBeDeleted() = runBlocking<Unit> {
        repo.ensureDefaults()
        val other = db.categories().all().single { it.builtIn == "OTHER" }.id
        repo.delete(other)
        assertThat(db.categories().all().any { it.id == other }).isTrue()
    }
}
