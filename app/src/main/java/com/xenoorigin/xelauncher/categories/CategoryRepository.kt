package com.xenoorigin.xelauncher.categories

import com.xenoorigin.xelauncher.apps.AppEntry
import com.xenoorigin.xelauncher.apps.AppKey
import com.xenoorigin.xelauncher.data.AppCategoryOverride
import com.xenoorigin.xelauncher.data.CategoryEntity
import com.xenoorigin.xelauncher.data.XeDatabase
import com.xenoorigin.xelauncher.settings.XeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
        val builtInIds = cats.filter { it.builtIn != null }.associate { BuiltInCategory.valueOf(it.builtIn!!) to it.id }
        val otherId = builtInIds[BuiltInCategory.OTHER]
        val grouped = all.asSequence()
            .filter { it.key.encode() !in s.hiddenApps }
            .groupBy { a ->
                overrideMap[a.key.encode()]?.takeIf { it in ids }
                    ?: builtInIds[Categorizer.categorize(a.key.packageName, a.androidCategory)]
                    ?: otherId
            }
        cats.mapNotNull { c ->
            val builtIn = c.builtIn?.let { BuiltInCategory.valueOf(it) }
            val members = AppSorting.sort(grouped[c.id].orEmpty(), s.drawerSort, statMap)
            if (builtIn != null && members.isEmpty()) return@mapNotNull null
            CategoryWithApps(c.id, c.name ?: labelFor(builtIn ?: BuiltInCategory.OTHER), c.iconName, builtIn, members)
        }
    }

    suspend fun ensureDefaults() = db.withTransaction {
        if (db.categories().count() > 0) return@withTransaction
        BuiltInCategory.entries.forEachIndexed { i, b ->
            db.categories().insert(CategoryEntity(builtIn = b.name, iconName = b.iconName, position = i))
        }
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
