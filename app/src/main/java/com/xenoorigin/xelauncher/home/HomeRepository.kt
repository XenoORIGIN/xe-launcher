package com.xenoorigin.xelauncher.home

import androidx.room.withTransaction
import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.apps.AppKey
import com.xenoorigin.xelauncher.data.FolderEntity
import com.xenoorigin.xelauncher.data.HomeItemEntity
import com.xenoorigin.xelauncher.data.XeDatabase
import com.xenoorigin.xelauncher.settings.XeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

private const val APP = "APP"
private const val FOLDER = "FOLDER"
private const val WIDGET = "WIDGET"
private const val PAGE_MARKER = "PAGE"
private const val C_PAGE = "PAGE"
private const val C_DOCK = "DOCK"
private const val C_FOLDER = "FOLDER"

class HomeRepository(
    private val db: XeDatabase,
    apps: Flow<List<AppEntry>>,
    private val settings: Flow<XeSettings>,
    private val folderNameFor: (AppEntry) -> String,
    private val defaults: suspend () -> List<AppKey>,
    private val isInstalled: (String) -> Boolean,
) {
    private val dao get() = db.home()
    private val appsFlow = apps

    /** Current layout. Items whose app isn't (yet) known are hidden, never deleted here. */
    val layout: Flow<HomeLayout> = combine(dao.observe(), dao.observeFolders(), apps) { items, folders, all ->
        val byKey = all.associateBy { it.key.encode() }
        val folderNames = folders.associate { it.id to it.name }
        fun cellOf(i: HomeItemEntity): HomeCell? = when (i.type) {
            APP -> byKey[i.appKey]?.let { AppCell(i.id, i.x, i.y, it) }
            FOLDER -> {
                val members = items.filter { it.container == C_FOLDER && it.folderId == i.folderId }.sortedBy { it.position }
                    .mapNotNull { byKey[it.appKey] }
                when {
                    members.isEmpty() -> null
                    else -> FolderCell(i.id, i.x, i.y, i.folderId!!, folderNames[i.folderId] ?: "", members)
                }
            }
            WIDGET -> WidgetCell(i.id, i.x, i.y, i.spanX, i.spanY, i.widgetId, i.widgetKind)
            else -> null
        }
        val pageCount = (items.filter { it.container == C_PAGE }.maxOfOrNull { it.page } ?: 0) + 1
        val pages = (0 until pageCount).map { p -> items.filter { it.container == C_PAGE && it.page == p }.mapNotNull(::cellOf) }
        val dock = items.filter { it.container == C_DOCK }.sortedBy { it.position }.mapNotNull(::cellOf)
        HomeLayout(pages, dock)
    }

    private suspend fun grid(): Pair<Int, Int> = settings.first().let { it.homeColumns to it.homeRows }

    private suspend fun pageCount(): Int = (dao.all().filter { it.container == C_PAGE }.maxOfOrNull { it.page } ?: 0) + 1

    /** Every cell covered on [page] (widgets cover their whole span), ignoring item [except]. */
    private suspend fun taken(page: Int, except: Long = -1): Set<Pair<Int, Int>> =
        dao.all().filter { it.container == C_PAGE && it.page == page && it.type != PAGE_MARKER && it.id != except }
            .flatMap { i -> (0 until i.spanX).flatMap { dx -> (0 until i.spanY).map { dy -> (i.x + dx) to (i.y + dy) } } }.toSet()

    private suspend fun fits(page: Int, x: Int, y: Int, w: Int, h: Int, except: Long = -1): Boolean {
        val (cols, rows) = grid()
        if (x < 0 || y < 0 || x + w > cols || y + h > rows) return false
        val t = taken(page, except)
        return (0 until w).all { dx -> (0 until h).all { dy -> (x + dx to y + dy) !in t } }
    }

    private suspend fun freeArea(page: Int, w: Int, h: Int): Pair<Int, Int>? {
        val (cols, rows) = grid()
        for (y in 0 until rows) for (x in 0 until cols) if (fits(page, x, y, w, h)) return x to y
        return null
    }

    private suspend fun freeCell(page: Int): Pair<Int, Int>? = freeArea(page, 1, 1)

    /** Places a widget on the first free area from [page] onwards (new pages as needed). Returns its item id. */
    suspend fun addWidget(page: Int, spanX: Int, spanY: Int, widgetId: Int, kind: String?): Long = db.withTransaction {
        val (cols, rows) = grid()
        val w = spanX.coerceIn(1, cols); val h = spanY.coerceIn(1, rows)
        var p = page
        while (true) {
            val at = freeArea(p, w, h)
            if (at != null) return@withTransaction dao.insert(
                HomeItemEntity(type = WIDGET, container = C_PAGE, page = p, x = at.first, y = at.second, spanX = w, spanY = h, widgetId = widgetId, widgetKind = kind),
            )
            p++
        }
        @Suppress("UNREACHABLE_CODE") -1L
    }

    /** Changes a widget's size if the new area is free; returns whether it changed. */
    suspend fun resizeWidget(itemId: Long, spanX: Int, spanY: Int): Boolean = db.withTransaction {
        val i = dao.get(itemId)?.takeIf { it.type == WIDGET } ?: return@withTransaction false
        if (!fits(i.page, i.x, i.y, spanX, spanY, except = i.id)) return@withTransaction false
        dao.update(i.copy(spanX = spanX, spanY = spanY)); true
    }

    /** Deletes a widget item and returns its Android widget id (0 for Xe widgets) so the host can free it. */
    suspend fun removeWidget(itemId: Long): Int = db.withTransaction {
        val i = dao.get(itemId)?.takeIf { it.type == WIDGET } ?: return@withTransaction 0
        dao.delete(i.id); i.widgetId
    }

    suspend fun seedDefaultIfEmpty() = db.withTransaction {
        if (dao.all().isNotEmpty()) return@withTransaction
        val dockSize = settings.first().dockSize
        defaults().distinct().take(dockSize).forEachIndexed { i, k ->
            dao.insert(HomeItemEntity(type = APP, appKey = k.encode(), container = C_DOCK, position = i))
        }
    }

    /** Adds [key] to the first free cell (from [page], or the first page), opening a new page if all are full. */
    suspend fun addApp(key: AppKey, page: Int? = null): Boolean = db.withTransaction {
        if (dao.all().any { it.appKey == key.encode() }) return@withTransaction false
        val start = page ?: 0
        var p = start
        while (true) {
            val cell = freeCell(p)
            if (cell != null) {
                dao.insert(HomeItemEntity(type = APP, appKey = key.encode(), container = C_PAGE, page = p, x = cell.first, y = cell.second))
                return@withTransaction true
            }
            p++
        }
        @Suppress("UNREACHABLE_CODE") false
    }

    suspend fun addPage() = db.withTransaction {
        dao.insert(HomeItemEntity(type = PAGE_MARKER, container = C_PAGE, page = pageCount()))
    }

    /** Moves an item to a page cell; if occupied, the occupant takes the mover's old place. */
    suspend fun move(itemId: Long, page: Int, x: Int, y: Int) = db.withTransaction {
        val item = dao.get(itemId) ?: return@withTransaction
        if (item.type == WIDGET) {
            if (fits(page, x, y, item.spanX, item.spanY, except = item.id)) dao.update(item.copy(page = page, x = x, y = y))
            return@withTransaction
        }
        // A cell under a widget is not free and can't be swapped into.
        val underWidget = dao.all().any { it.type == WIDGET && it.container == C_PAGE && it.page == page &&
            x in it.x until it.x + it.spanX && y in it.y until it.y + it.spanY }
        if (underWidget) return@withTransaction
        val occupant = dao.all().firstOrNull { it.container == C_PAGE && it.type != PAGE_MARKER && it.page == page && it.x == x && it.y == y && it.id != itemId }
        if (occupant != null) {
            if (item.container == C_PAGE) dao.update(occupant.copy(page = item.page, x = item.x, y = item.y))
            else dao.update(occupant.copy(container = item.container, position = item.position, folderId = item.folderId))
        }
        dao.update(item.copy(container = C_PAGE, page = page, x = x, y = y, folderId = if (item.type == FOLDER) item.folderId else null))
        if (item.container == C_FOLDER) dissolveIfSmall(item.folderId!!)
    }

    suspend fun moveToDock(itemId: Long, position: Int): Boolean = db.withTransaction {
        val item = dao.get(itemId) ?: return@withTransaction false
        if (item.type == WIDGET) return@withTransaction false
        val dock = dao.all().filter { it.container == C_DOCK && it.id != itemId }.sortedBy { it.position }
        if (dock.size >= settings.first().dockSize) return@withTransaction false
        val ordered = dock.toMutableList().apply { add(position.coerceIn(0, size), item) }
        ordered.forEachIndexed { i, it ->
            dao.update(it.copy(container = C_DOCK, position = i, folderId = if (it.type == FOLDER) it.folderId else null))
        }
        if (item.container == C_FOLDER) dissolveIfSmall(item.folderId!!)
        true
    }

    /** Drops [draggedId] onto [targetId]: app→app makes a folder, app→folder adds to it. Returns the folder id. */
    suspend fun dropOnto(draggedId: Long, targetId: Long): Long = db.withTransaction {
        val dragged = dao.get(draggedId) ?: return@withTransaction -1L
        val target = dao.get(targetId) ?: return@withTransaction -1L
        if (dragged.type != APP) return@withTransaction -1L
        val folderId = when (target.type) {
            FOLDER -> target.folderId!!
            APP -> {
                val targetApp = appsFlow.first().firstOrNull { it.key.encode() == target.appKey }
                val fid = dao.insertFolder(FolderEntity(name = targetApp?.let(folderNameFor) ?: ""))
                dao.insert(target.copy(id = 0, type = FOLDER, appKey = null, folderId = fid))
                dao.update(target.copy(container = C_FOLDER, folderId = fid, position = 0, page = 0, x = 0, y = 0))
                fid
            }
            else -> return@withTransaction -1L
        }
        val next = dao.all().filter { it.container == C_FOLDER && it.folderId == folderId }.maxOfOrNull { it.position }?.plus(1) ?: 0
        val oldFolder = dragged.folderId.takeIf { dragged.container == C_FOLDER }
        dao.update(dragged.copy(container = C_FOLDER, folderId = folderId, position = next, page = 0, x = 0, y = 0))
        oldFolder?.let { dissolveIfSmall(it) }
        folderId
    }

    suspend fun renameFolder(id: Long, name: String) {
        dao.folders().firstOrNull { it.id == id }?.let { dao.updateFolder(it.copy(name = name)) }
    }

    suspend fun remove(itemId: Long) = db.withTransaction {
        val item = dao.get(itemId) ?: return@withTransaction
        if (item.type == FOLDER) {
            val members = dao.all().filter { it.container == C_FOLDER && it.folderId == item.folderId }
            dao.deleteAll(members.map { it.id }); dao.deleteFolder(item.folderId!!)
        }
        dao.delete(itemId)
        if (item.container == C_FOLDER) dissolveIfSmall(item.folderId!!)
    }

    /** Removes the item showing [key] (used after uninstall or "remove from Home"). */
    suspend fun removeApp(key: AppKey) {
        dao.all().filter { it.appKey == key.encode() }.forEach { remove(it.id) }
    }

    /** A folder with one app left becomes that app, in the folder's place. */
    private suspend fun dissolveIfSmall(folderId: Long) {
        val members = dao.all().filter { it.container == C_FOLDER && it.folderId == folderId }
        if (members.size > 1) return
        val holder = dao.all().firstOrNull { it.type == FOLDER && it.folderId == folderId } ?: return
        members.singleOrNull()?.let { dao.update(it.copy(container = holder.container, page = holder.page, x = holder.x, y = holder.y, position = holder.position, folderId = null)) }
        dao.delete(holder.id); dao.deleteFolder(folderId)
    }

    /** Deletes items for packages that are no longer installed at all. */
    suspend fun pruneUninstalled() = db.withTransaction {
        dao.all().filter { it.type == APP }.forEach { i ->
            val pkg = AppKey.decode(i.appKey ?: return@forEach)?.packageName ?: return@forEach
            if (!isInstalled(pkg)) {
                dao.delete(i.id)
                if (i.container == C_FOLDER) dissolveIfSmall(i.folderId!!)
            }
        }
    }

    suspend fun removeEmptyPages() = db.withTransaction {
        val all = dao.all()
        val used = all.filter { it.container == C_PAGE && it.type != PAGE_MARKER }.map { it.page }.toSortedSet()
        all.filter { it.type == PAGE_MARKER }.forEach { dao.delete(it.id) }
        val remap = used.withIndex().associate { (i, p) -> p to i }
        all.filter { it.container == C_PAGE && it.type != PAGE_MARKER }.forEach { dao.update(it.copy(page = remap[it.page] ?: 0)) }
    }

    /** Snapshot for backup. */
    suspend fun export() = dao.all() to dao.folders()
}
