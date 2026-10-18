package com.litespace.browser.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.litespace.browser.domain.model.BookmarkItem
import com.litespace.browser.domain.model.HistoryItem

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["liteAppId"])]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val liteAppId: Long,
    val title: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = BookmarkItem(
        id = id,
        liteAppId = liteAppId,
        title = title,
        url = url,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(item: BookmarkItem) = BookmarkEntity(
            id = item.id,
            liteAppId = item.liteAppId,
            title = item.title,
            url = item.url,
            createdAt = item.createdAt
        )
    }
}

@Entity(
    tableName = "browsing_history",
    indices = [Index(value = ["liteAppId"]), Index(value = ["visitedAt"])]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val liteAppId: Long,
    val title: String,
    val url: String,
    val visitedAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = HistoryItem(
        id = id,
        liteAppId = liteAppId,
        title = title,
        url = url,
        visitedAt = visitedAt
    )
}
