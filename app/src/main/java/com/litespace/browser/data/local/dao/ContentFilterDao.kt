package com.litespace.browser.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.litespace.browser.data.local.entity.ContentFilterRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentFilterDao {
    @Query("SELECT * FROM content_filter_rules ORDER BY hitCount DESC, pattern ASC")
    fun observeAllRules(): Flow<List<ContentFilterRuleEntity>>

    @Query("SELECT * FROM content_filter_rules WHERE isEnabled = 1")
    suspend fun getEnabledRulesOnce(): List<ContentFilterRuleEntity>

    @Query("SELECT * FROM content_filter_rules")
    suspend fun getAllRulesOnce(): List<ContentFilterRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: ContentFilterRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<ContentFilterRuleEntity>)

    @Query("UPDATE content_filter_rules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setRuleEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE content_filter_rules SET hitCount = hitCount + 1 WHERE pattern = :pattern")
    suspend fun incrementHitCount(pattern: String)

    @Query("DELETE FROM content_filter_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)
}
