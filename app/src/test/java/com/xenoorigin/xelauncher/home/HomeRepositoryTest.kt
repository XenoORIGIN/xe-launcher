package com.xenoorigin.xelauncher.home

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.apps.AppKey
import com.xenoorigin.xelauncher.data.XeDatabase
import com.xenoorigin.xelauncher.settings.XeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private fun e(n: String) = AppEntry(AppKey("p.$n", "p.$n.Main", 0), n, -1, 0, false)

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class HomeRepositoryTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val db = Room.inMemoryDatabaseBuilder(ctx, XeDatabase::class.java).allowMainThreadQueries().build()
    private val all = (1..30).map { e("A$it") }
    private val apps = MutableStateFlow(all)
    private val settings = MutableStateFlow(XeSettings(homeColumns = 4, homeRows = 2, dockSize = 2))
    private val installed = mutableSetOf<String>().apply { all.forEach { add(it.key.packageName) } }
    private val repo = HomeRepository(
        db, apps, settings,
        folderNameFor = { "Folder of ${it.label}" },
        defaults = { listOf(all[0].key, all[1].key) },
        isInstalled = { it in installed },
    )

    @After fun close() = db.close()
    private suspend fun layout() = repo.layout.first()
    private suspend fun idOf(label: String) = db.home().all().first { it.appKey == all.first { a -> a.label == label }.key.encode() }.id

    @Test fun addApp_fillsPageThenOpensNext() = runBlocking<Unit> {
        repeat(9) { repo.addApp(all[it].key) }
        val l = layout()
        assertThat(l.pages).hasSize(2)
        assertThat(l.pages[0]).hasSize(8)
        assertThat(l.pages[1].single().x to l.pages[1].single().y).isEqualTo(0 to 0)
    }

    @Test fun addApp_alreadyOnHome_returnsFalse() = runBlocking<Unit> {
        assertThat(repo.addApp(all[0].key)).isTrue()
        assertThat(repo.addApp(all[0].key)).isFalse()
    }

    @Test fun move_ontoOccupiedCell_swaps() = runBlocking<Unit> {
        repo.addApp(all[0].key); repo.addApp(all[1].key) // (0,0) and (1,0)
        repo.move(idOf("A1"), 0, 1, 0)
        val cells: Map<String, Pair<Int, Int>> = layout().pages[0].associate { (it as AppCell).app.label to (it.x to it.y) }
        assertThat(cells["A1"]).isEqualTo(1 to 0)
        assertThat(cells["A2"]).isEqualTo(0 to 0)
    }

    @Test fun moveToDock_respectsDockSize() = runBlocking<Unit> {
        (0..2).forEach { repo.addApp(all[it].key) }
        assertThat(repo.moveToDock(idOf("A1"), 0)).isTrue()
        assertThat(repo.moveToDock(idOf("A2"), 1)).isTrue()
        assertThat(repo.moveToDock(idOf("A3"), 2)).isFalse()
        assertThat(layout().dock.map { (it as AppCell).app.label }).containsExactly("A1", "A2").inOrder()
    }

    @Test fun dropAppOnApp_createsNamedFolderAtTarget() = runBlocking<Unit> {
        repo.addApp(all[0].key); repo.addApp(all[1].key)
        repo.dropOnto(idOf("A1"), idOf("A2"))
        val cell = layout().pages[0].single() as FolderCell
        assertThat(cell.name).isEqualTo("Folder of A2")
        assertThat(cell.apps.map { it.label }).containsExactly("A2", "A1").inOrder()
        assertThat(cell.x to cell.y).isEqualTo(1 to 0)
    }

    @Test fun dropAppOnFolder_addsToFolder() = runBlocking<Unit> {
        (0..2).forEach { repo.addApp(all[it].key) }
        repo.dropOnto(idOf("A1"), idOf("A2"))
        val folderItem = db.home().all().first { it.type == "FOLDER" }.id
        repo.dropOnto(idOf("A3"), folderItem)
        assertThat((layout().pages[0].single() as FolderCell).apps.map { it.label }).containsExactly("A2", "A1", "A3").inOrder()
    }

    @Test fun folderLeftWithOneApp_dissolves() = runBlocking<Unit> {
        repo.addApp(all[0].key); repo.addApp(all[1].key)
        repo.dropOnto(idOf("A1"), idOf("A2"))
        repo.remove(idOf("A1"))
        val cell = layout().pages[0].single() as AppCell
        assertThat(cell.app.label).isEqualTo("A2")
        assertThat(cell.x to cell.y).isEqualTo(1 to 0)
        assertThat(db.home().folders()).isEmpty()
    }

    @Test fun seedDefault_twice_noDuplicates() = runBlocking<Unit> {
        repo.seedDefaultIfEmpty(); repo.seedDefaultIfEmpty()
        assertThat(layout().dock.map { (it as AppCell).app.label }).containsExactly("A1", "A2").inOrder()
        assertThat(db.home().all()).hasSize(2)
    }

    @Test fun appsNotLoadedYet_hidesButNeverDeletes() = runBlocking<Unit> {
        repo.addApp(all[0].key)
        apps.value = emptyList()
        assertThat(layout().pages.flatten()).isEmpty()
        assertThat(db.home().all()).hasSize(1)
        apps.value = all
        assertThat(layout().pages.flatten()).hasSize(1)
    }

    @Test fun prune_removesOnlyUninstalledPackages() = runBlocking<Unit> {
        repo.addApp(all[0].key); repo.addApp(all[1].key)
        installed.remove(all[0].key.packageName)
        repo.pruneUninstalled()
        assertThat(layout().pages.flatten().map { (it as AppCell).app.label }).containsExactly("A2")
    }

    @Test fun removeEmptyPages_renumbers() = runBlocking<Unit> {
        repeat(9) { repo.addApp(all[it].key) } // page 0 full, A9 on page 1
        repo.addPage() // page 2 empty
        (0..7).forEach { repo.remove(idOf("A${it + 1}")) } // empty page 0
        repo.removeEmptyPages()
        val l = layout()
        assertThat(l.pages).hasSize(1)
        assertThat((l.pages[0].single() as AppCell).app.label).isEqualTo("A9")
    }

    // ---- widgets ----

    @Test fun addWidget_takesAFreeAreaAroundApps() = runBlocking<Unit> {
        repo.addApp(all[0].key) // (0,0)
        val id = repo.addWidget(page = 0, spanX = 2, spanY = 2, widgetId = 7, kind = null)
        val w = layout().pages[0].filterIsInstance<WidgetCell>().single()
        assertThat(w.id).isEqualTo(id)
        assertThat(w.x to w.y).isEqualTo(1 to 0)
        assertThat(w.spanX to w.spanY).isEqualTo(2 to 2)
    }

    @Test fun addWidget_tooBigForPage_goesToNewPage() = runBlocking<Unit> {
        repeat(3) { repo.addApp(all[it].key) } // (0,0)(1,0)(2,0)
        repo.addWidget(page = 0, spanX = 4, spanY = 2, widgetId = 1, kind = null)
        val l = layout()
        assertThat(l.pages).hasSize(2)
        assertThat(l.pages[1].filterIsInstance<WidgetCell>().single().x).isEqualTo(0)
    }

    @Test fun addApp_skipsCellsCoveredByWidget() = runBlocking<Unit> {
        repo.addWidget(page = 0, spanX = 2, spanY = 2, widgetId = 1, kind = null) // covers (0..1, 0..1)
        repo.addApp(all[0].key)
        val app = layout().pages[0].filterIsInstance<AppCell>().single()
        assertThat(app.x to app.y).isEqualTo(2 to 0)
    }

    @Test fun moveApp_ontoWidget_isRefused() = runBlocking<Unit> {
        repo.addWidget(page = 0, spanX = 2, spanY = 2, widgetId = 1, kind = null)
        repo.addApp(all[0].key)
        repo.move(idOf("A1"), 0, 1, 1)
        val app = layout().pages[0].filterIsInstance<AppCell>().single()
        assertThat(app.x to app.y).isEqualTo(2 to 0)
    }

    @Test fun resizeWidget_growsOnlyIntoFreeSpace() = runBlocking<Unit> {
        val id = repo.addWidget(page = 0, spanX = 1, spanY = 1, widgetId = 1, kind = "xe_clock")
        repo.addApp(all[0].key) // (1,0)
        assertThat(repo.resizeWidget(id, 2, 1)).isFalse()
        assertThat(repo.resizeWidget(id, 1, 2)).isTrue()
        assertThat(layout().pages[0].filterIsInstance<WidgetCell>().single().spanY).isEqualTo(2)
    }

    @Test fun removeWidget_returnsItsHostId() = runBlocking<Unit> {
        val id = repo.addWidget(page = 0, spanX = 2, spanY = 1, widgetId = 42, kind = null)
        assertThat(repo.removeWidget(id)).isEqualTo(42)
        assertThat(layout().pages[0]).isEmpty()
    }

    @Test fun placeWidget_movesAndResizesInOneStep() = runBlocking<Unit> {
        val id = repo.addWidget(page = 0, spanX = 2, spanY = 1, widgetId = 1, kind = "xe_clock")
        assertThat(repo.placeWidget(id, 1, 0, 3, 2)).isTrue()
        val w = layout().pages[0].filterIsInstance<WidgetCell>().single()
        assertThat(listOf(w.x, w.y, w.spanX, w.spanY)).containsExactly(1, 0, 3, 2).inOrder()
    }

    @Test fun placeWidget_failureKeepsOriginal() = runBlocking<Unit> {
        val id = repo.addWidget(page = 0, spanX = 2, spanY = 1, widgetId = 1, kind = "xe_clock") // (0,0)
        repo.addApp(all[0].key) // (2,0)
        assertThat(repo.placeWidget(id, 1, 0, 2, 1)).isFalse() // would cover the app
        assertThat(repo.placeWidget(id, 3, 0, 2, 1)).isFalse() // past the edge
        val w = layout().pages[0].filterIsInstance<WidgetCell>().single()
        assertThat(listOf(w.x, w.y, w.spanX, w.spanY)).containsExactly(0, 0, 2, 1).inOrder()
    }
}
