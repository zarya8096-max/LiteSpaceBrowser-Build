package com.litespace.browser.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.litespace.browser.domain.model.ContentFilterRule

@Entity(
    tableName = "content_filter_rules",
    indices = [Index(value = ["pattern"], unique = true)]
)
data class ContentFilterRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val pattern: String,
    val isException: Boolean = false,
    val isEnabled: Boolean = true,
    val hitCount: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = ContentFilterRule(
        id = id,
        pattern = pattern,
        isException = isException,
        isEnabled = isEnabled,
        hitCount = hitCount,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(rule: ContentFilterRule) = ContentFilterRuleEntity(
            id = rule.id,
            pattern = rule.pattern,
            isException = rule.isException,
            isEnabled = rule.isEnabled,
            hitCount = rule.hitCount,
            createdAt = rule.createdAt
        )
    }
}
