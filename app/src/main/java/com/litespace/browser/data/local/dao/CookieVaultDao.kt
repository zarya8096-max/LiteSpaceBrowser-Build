package com.litespace.browser.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.litespace.browser.data.local.entity.CookieVaultEntity

@Dao
interface CookieVaultDao {
    @Query("SELECT * FROM cookie_vault WHERE profileId = :profileId")
    suspend fun getVaultEntriesForProfile(profileId: String): List<CookieVaultEntity>

    @Query("SELECT * FROM cookie_vault WHERE vaultKey = :vaultKey LIMIT 1")
    suspend fun getVaultEntry(vaultKey: String): CookieVaultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVaultEntry(entry: CookieVaultEntity)

    @Query("DELETE FROM cookie_vault WHERE profileId = :profileId")
    suspend fun clearProfileVault(profileId: String)
}
