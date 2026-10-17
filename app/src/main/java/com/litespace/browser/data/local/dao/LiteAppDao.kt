package com.litespace.browser.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.litespace.browser.data.local.entity.LiteAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LiteAppDao {
    @Query("SELECT * FROM lite_apps ORDER BY isFavorite DESC, lastLaunchedAt DESC, name ASC")
    fun observeAllLiteApps(): Flow<List<LiteAppEntity>>

    @Query("SELECT * FROM lite_apps ORDER BY name ASC")
    suspend fun getAllLiteAppsOnce(): List<LiteAppEntity>

    @Query("SELECT * FROM lite_apps WHERE id = :id LIMIT 1")
    suspend fun getLiteAppById(id: Long): LiteAppEntity?

    @Query("SELECT * FROM lite_apps WHERE id = :id LIMIT 1")
    fun observeLiteAppById(id: Long): Flow<LiteAppEntity?>

    @Query("SELECT * FROM lite_apps WHERE uuid = :uuid LIMIT 1")
    suspend fun getLiteAppByUuid(uuid: String): LiteAppEntity?

    @Query("SELECT * FROM lite_apps WHERE feedMonitorUrl != '' AND feedCheckIntervalMinutes > 0")
    suspend fun getAppsWithActiveFeedMonitor(): List<LiteAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(app: LiteAppEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<LiteAppEntity>)

    @Update
    suspend fun update(app: LiteAppEntity)

    @Delete
    suspend fun delete(app: LiteAppEntity)

    @Query("DELETE FROM lite_apps WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE lite_apps SET lastLaunchedAt = :timestamp, launchCount = launchCount + 1 WHERE id = :id")
    suspend fun recordLaunch(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE lite_apps SET lastSeenFeedItemHash = :hash WHERE id = :id")
    suspend fun updateLastSeenFeedHash(id: Long, hash: String)
}
