package com.xenoorigin.xelauncher.categories

import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.apps.AppKey
import com.xenoorigin.xelauncher.data.AppCategoryOverride
import com.xenoorigin.xelauncher.data.CategoryEntity
import com.xenoorigin.xelauncher.data.XeDatabase
import com.xenoorigin.xelauncher.settings.XeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction

data class CategoryWithApps(
    val id: Long,
    val title: String,
    val iconName: String,
    val builtIn: BuiltInCategory?,
    val apps: List<AppEntry>,
)

class CategoryRepository(
    private val db: XeDatabase,
    apps: Flow<List<AppEntry>>,
    settings: Flow<XeSettings>,
    private val labelFor: (BuiltInCategory) -> String,
) {
    /** Visible categories in order, each with its (sorted, non-hidden) apps. */
    val categories: Flow<List<CategoryWithApps>> = combine(
        db.categories().observe(), db.overrides().observe(), db.launches().observe(), apps, settings,
    ) { cats, overrides, stats, all, s ->
        val overrideMap = overrides.associate { it.appKey to it.categoryId }
        val statMap = stats.associateBy { it.appKey }
        val ids = cats.map { it.id }.toSet()
        val builtInIds = cats.mapNotNull { c -> c.builtIn?.let { n -> runCatching { BuiltInCategory.valueOf(n) }.getOrNull() }?.let { it to c.id } }.toMap()
        val otherId = builtInIds[BuiltInCategory.OTHER]
        val grouped = all.asSequence()
            .filter { it.key.encode() !in s.hiddenApps }
            .groupBy { a ->
                overrideMap[a.key.encode()]?.takeIf { it in ids }
                    ?: builtInIds[Categorizer.categorize(a.key.packageName, a.androidCategory)]
                    ?: otherId
            }
        cats.mapNotNull { c ->
            val builtIn = c.builtIn?.let { runCatching { BuiltInCategory.valueOf(it) }.getOrNull() }
            val members = AppSorting.sort(grouped[c.id].orEmpty(), s.drawerSort, statMap, s.appOrder)
            if (builtIn != null && members.isEmpty()) return@mapNotNull null
            CategoryWithApps(c.id, c.name ?: labelFor(builtIn ?: BuiltInCategory.OTHER), c.iconName, builtIn, members)
        }
    }

    /** Every category (including empty ones) with its display title, for the editor. */
    val editable: Flow<List<Pair<CategoryEntity, String>>> = db.categories().observe().map { list ->
        list.map { it to (it.name ?: labelFor(it.builtIn?.let(BuiltInCategory::valueOf) ?: BuiltInCategory.OTHER)) }
    }

    suspend fun ensureDefaults() = db.withTransaction {
        if (db.categories().count() > 0) return@withTransaction
        BuiltInCategory.entries.forEachIndexed { i, b ->
            db.categories().insert(CategoryEntity(builtIn = b.name, iconName = b.iconName, position = i))
        }
    }

    /**
     * Brings an older category list up to date: adds built-ins it doesn't have yet (just before "Other",
     * which stays last) and swaps first-release icons for the new ones. User categories and order are kept.
     */
    suspend fun upgradeBuiltIns() = db.withTransaction {
        val all = db.categories().all()
        val present = all.mapNotNull { it.builtIn }.toSet()
        val missing = BuiltInCategory.entries.filter { it.name !in present }
        all.forEach { c ->
            val b = c.builtIn?.let { runCatching { BuiltInCategory.valueOf(it) }.getOrNull() } ?: return@forEach
            if (BuiltInCategory.legacyIcons[b] == c.iconName) db.categories().update(c.copy(iconName = b.iconName))
        }
        if (missing.isEmpty()) return@withTransaction
        val ordered = db.categories().all().toMutableList()
        val other = ordered.firstOrNull { it.builtIn == BuiltInCategory.OTHER.name }
        if (other != null) ordered.remove(other)
        var pos = 0
        ordered.forEach { db.categories().update(it.copy(position = pos++)) }
        missing.filter { it != BuiltInCategory.OTHER }.forEach { b ->
            db.categories().insert(CategoryEntity(builtIn = b.name, iconName = b.iconName, position = pos++))
        }
        if (other != null) db.categories().update(other.copy(position = pos))
        else db.categories().insert(CategoryEntity(builtIn = BuiltInCategory.OTHER.name, iconName = BuiltInCategory.OTHER.iconName, position = pos))
    }

    suspend fun add(name: String, iconName: String): Long {
        val pos = db.categories().all().maxOfOrNull { it.position }?.plus(1) ?: 0
        return db.categories().insert(CategoryEntity(name = name, iconName = iconName, position = pos))
    }

    suspend fun rename(id: Long, name: String) {
        db.categories().all().firstOrNull { it.id == id }?.let { db.categories().update(it.copy(name = name.trim().ifEmpty { null })) }
    }

    suspend fun setIcon(id: Long, iconName: String) {
        db.categories().all().firstOrNull { it.id == id }?.let { db.categories().update(it.copy(iconName = iconName)) }
    }

    /** Deletes a category; its manually moved apps go back to automatic placement. "Other" can't be deleted. */
    suspend fun delete(id: Long) = db.withTransaction {
        val c = db.categories().all().firstOrNull { it.id == id } ?: return@withTransaction
        if (c.builtIn == BuiltInCategory.OTHER.name) return@withTransaction
        db.overrides().deleteForCategory(id)
        db.categories().delete(id)
    }

    suspend fun reorder(ids: List<Long>) = db.withTransaction {
        val byId = db.categories().all().associateBy { it.id }
        ids.forEachIndexed { i, id -> byId[id]?.let { db.categories().update(it.copy(position = i)) } }
    }

    suspend fun moveApp(key: AppKey, categoryId: Long) = db.overrides().upsert(AppCategoryOverride(key.encode(), categoryId))

    suspend fun recordLaunch(key: AppKey) {
        db.launches().ensure(key.encode()); db.launches().bump(key.encode(), System.currentTimeMillis())
    }
}
