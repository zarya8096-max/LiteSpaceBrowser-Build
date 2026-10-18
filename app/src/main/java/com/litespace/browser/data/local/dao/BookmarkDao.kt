package com.litespace.browser.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.litespace.browser.data.local.entity.BookmarkEntity
import com.litespace.browser.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE liteAppId = :liteAppId ORDER BY createdAt DESC")
    fun observeBookmarksForApp(liteAppId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    suspend fun getAllBookmarksOnce(): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bookmarks: List<BookmarkEntity>)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM bookmarks WHERE liteAppId = :liteAppId")
    suspend fun deleteBookmarksForApp(liteAppId: Long)

    @Query("SELECT * FROM browsing_history WHERE liteAppId = :liteAppId ORDER BY visitedAt DESC LIMIT 100")
    fun observeHistoryForApp(liteAppId: Long): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(entry: HistoryEntity): Long

    @Query("DELETE FROM browsing_history WHERE liteAppId = :liteAppId")
    suspend fun clearHistoryForApp(liteAppId: Long)
}
