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
import com.xenoorigin.xelauncher.design.moved
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

    @Test fun ensureDefaults_twice_keepsOneOfEachBuiltIn() = runBlocking<Unit> {
        repo.ensureDefaults(); repo.ensureDefaults()
        assertThat(db.categories().all()).hasSize(BuiltInCategory.entries.size)
        assertThat(db.categories().all().last().builtIn).isEqualTo("OTHER")
    }

    @Test fun upgrade_addsNewBuiltInsBeforeOther_andRefreshesOldIcons() = runBlocking<Unit> {
        // A phone set up with the first version: six built-ins with the old icons, plus one user category.
        listOf("COMMUNICATION" to "message-circle", "SOCIAL" to "affiliate", "GAMES" to "device-gamepad-2", "MEDIA" to "photo",
            "TOOLS" to "settings", "OTHER" to "category").forEachIndexed { i, (b, icon) ->
            db.categories().insert(com.xenoorigin.xelauncher.data.CategoryEntity(builtIn = b, iconName = icon, position = i))
        }
        repo.add("School", "school")
        repo.upgradeBuiltIns()
        val all = db.categories().all()
        assertThat(all.map { it.builtIn }.filterNotNull()).containsAtLeast("FINANCE", "SHOPPING", "TRAVEL", "WORK")
        assertThat(all.last().builtIn).isEqualTo("OTHER")
        assertThat(all.first { it.builtIn == "SOCIAL" }.iconName).isEqualTo(BuiltInCategory.SOCIAL.iconName)
        assertThat(all.any { it.name == "School" }).isTrue()
        repo.upgradeBuiltIns()
        assertThat(db.categories().all()).hasSize(all.size)
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

    @Test fun customOrder_putsArrangedAppsFirst_restAlphabetical() = runBlocking<Unit> {
        repo.ensureDefaults()
        apps.value = listOf(app("com.discord", "Discord"), app("com.whatsapp", "WhatsApp"), app("org.telegram.messenger", "Telegram"), app("com.viber.voip", "Viber"))
        settings.value = XeSettings(
            drawerSort = com.xenoorigin.xelauncher.settings.DrawerSort.CUSTOM,
            appOrder = mapOf(app("com.whatsapp", "").key.encode() to 0, app("org.telegram.messenger", "").key.encode() to 1),
        )
        val labels = repo.categories.first().first().apps.map { it.label }
        assertThat(labels).containsExactly("WhatsApp", "Telegram", "Discord", "Viber").inOrder()
    }

    @Test fun moved_shiftsItemAndClamps() {
        assertThat(listOf("a", "b", "c").moved(0, 2)).containsExactly("b", "c", "a").inOrder()
        assertThat(listOf("a", "b", "c").moved(2, -9)).containsExactly("c", "a", "b").inOrder()
    }

    @Test fun hiddenCategory_leavesBar_andItsAppsGoToOther() = runBlocking<Unit> {
        repo.ensureDefaults()
        apps.value = listOf(app("com.discord", "Discord"), app("com.example.unknown", "Zed"))
        val comm = db.categories().all().first { it.builtIn == "COMMUNICATION" }
        settings.value = XeSettings(hiddenCategories = setOf(comm.id))
        val cats = repo.categories.first()
        assertThat(cats.map { it.builtIn }).doesNotContain(BuiltInCategory.COMMUNICATION)
        assertThat(cats.first { it.builtIn == BuiltInCategory.OTHER }.apps.map { it.label }).containsExactly("Discord", "Zed")
    }

    @Test fun resetAutomatic_undoesManualMoves() = runBlocking<Unit> {
        repo.ensureDefaults()
        apps.value = listOf(app("com.discord", "Discord"))
        val games = db.categories().all().first { it.builtIn == "GAMES" }
        repo.moveApp(app("com.discord", "").key, games.id)
        assertThat(repo.categories.first().first { it.builtIn == BuiltInCategory.GAMES }.apps).hasSize(1)
        repo.resetAutomatic()
        assertThat(repo.categories.first().first().builtIn).isEqualTo(BuiltInCategory.COMMUNICATION)
    }

    @Test fun resetToDefaults_restoresBuiltInsAndRemovesUserCategories() = runBlocking<Unit> {
        repo.ensureDefaults()
        repo.add("School", "school")
        db.categories().all().first { it.builtIn == "GAMES" }.let { repo.delete(it.id) }
        repo.resetToDefaults()
        val all = db.categories().all()
        assertThat(all.map { it.builtIn }).containsExactlyElementsIn(BuiltInCategory.entries.map { it.name }).inOrder()
    }
}
