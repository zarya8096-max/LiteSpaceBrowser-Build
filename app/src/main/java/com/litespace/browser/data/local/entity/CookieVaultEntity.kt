package com.litespace.browser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cookie_vault")
data class CookieVaultEntity(
    @PrimaryKey
    val vaultKey: String,
    val profileId: String,
    val host: String,
    val serializedCookiesJson: String,
    val localStorageJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)
