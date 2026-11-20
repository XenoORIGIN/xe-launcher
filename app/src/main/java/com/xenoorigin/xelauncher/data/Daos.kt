package com.xenoorigin.xelauncher.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY position") fun observe(): Flow<List<CategoryEntity>>
    @Query("SELECT * FROM categories ORDER BY position") suspend fun all(): List<CategoryEntity>
    @Query("SELECT COUNT(*) FROM categories") suspend fun count(): Int
    @Insert suspend fun insert(c: CategoryEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(c: List<CategoryEntity>)
    @Update suspend fun update(c: CategoryEntity)
    @Query("DELETE FROM categories WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM categories") suspend fun clear()
}

@Dao
interface OverrideDao {
    @Query("SELECT * FROM app_category_overrides") fun observe(): Flow<List<AppCategoryOverride>>
    @Query("SELECT * FROM app_category_overrides") suspend fun all(): List<AppCategoryOverride>
    @Upsert suspend fun upsert(o: AppCategoryOverride)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(o: List<AppCategoryOverride>)
    @Query("DELETE FROM app_category_overrides WHERE categoryId = :categoryId") suspend fun deleteForCategory(categoryId: Long)
    @Query("DELETE FROM app_category_overrides") suspend fun clear()
}

@Dao
interface LaunchDao {
    @Query("SELECT * FROM launch_stats") fun observe(): Flow<List<LaunchStat>>
    @Query("SELECT * FROM launch_stats") suspend fun all(): List<LaunchStat>
    @Query("INSERT OR IGNORE INTO launch_stats(appKey, count, lastLaunch) VALUES(:k, 0, 0)") suspend fun ensure(k: String)
    @Query("UPDATE launch_stats SET count = count + 1, lastLaunch = :t WHERE appKey = :k") suspend fun bump(k: String, t: Long)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(s: List<LaunchStat>)
    @Query("DELETE FROM launch_stats") suspend fun clear()
}

@Dao
interface HomeDao {
    @Query("SELECT * FROM home_items") fun observe(): Flow<List<HomeItemEntity>>
    @Query("SELECT * FROM home_items") suspend fun all(): List<HomeItemEntity>
    @Query("SELECT * FROM home_items WHERE id = :id") suspend fun get(id: Long): HomeItemEntity?
    @Insert suspend fun insert(i: HomeItemEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(i: List<HomeItemEntity>)
    @Update suspend fun update(i: HomeItemEntity)
    @Update suspend fun updateAll(i: List<HomeItemEntity>)
    @Query("DELETE FROM home_items WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM home_items WHERE id IN (:ids)") suspend fun deleteAll(ids: List<Long>)
    @Query("DELETE FROM home_items") suspend fun clear()

    @Query("SELECT * FROM folders") fun observeFolders(): Flow<List<FolderEntity>>
    @Query("SELECT * FROM folders") suspend fun folders(): List<FolderEntity>
    @Insert suspend fun insertFolder(f: FolderEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertFolders(f: List<FolderEntity>)
    @Update suspend fun updateFolder(f: FolderEntity)
    @Query("DELETE FROM folders WHERE id = :id") suspend fun deleteFolder(id: Long)
    @Query("DELETE FROM folders") suspend fun clearFolders()
}
