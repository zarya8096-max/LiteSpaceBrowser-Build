package com.litespace.browser.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.litespace.browser.data.local.dao.BookmarkDao
import com.litespace.browser.data.local.dao.ContentFilterDao
import com.litespace.browser.data.local.dao.CookieVaultDao
import com.litespace.browser.data.local.dao.LiteAppDao
import com.litespace.browser.data.local.entity.BookmarkEntity
import com.litespace.browser.data.local.entity.ContentFilterRuleEntity
import com.litespace.browser.data.local.entity.CookieVaultEntity
import com.litespace.browser.data.local.entity.LiteAppEntity
import com.litespace.browser.domain.model.CatalogTemplates
import com.litespace.browser.domain.model.LiteAppConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        LiteAppEntity::class,
        ContentFilterRuleEntity::class,
        CookieVaultEntity::class,
        BookmarkEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LiteSpaceDatabase : RoomDatabase() {

    abstract fun liteAppDao(): LiteAppDao
    abstract fun contentFilterDao(): ContentFilterDao
    abstract fun cookieVaultDao(): CookieVaultDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: LiteSpaceDatabase? = null

        fun getInstance(context: Context): LiteSpaceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): LiteSpaceDatabase {
            val dbScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            return Room.databaseBuilder(
                context,
                LiteSpaceDatabase::class.java,
                "litespace_browser.db"
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        dbScope.launch {
                            val instance = getInstance(context)
                            seedInitialData(instance)
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
        }

        private suspend fun seedInitialData(database: LiteSpaceDatabase) {
            val existing = database.liteAppDao().getAllLiteAppsOnce()
            if (existing.isEmpty()) {
                val starterApps = CatalogTemplates.builtInTemplates.take(4).mapIndexed { index, tpl ->
                    LiteAppEntity.fromDomain(
                        LiteAppConfig(
                            uuid = UUID.randomUUID().toString(),
                            name = tpl.name,
                            startUrl = tpl.startUrl,
                            category = tpl.category,
                            iconText = tpl.iconText,
                            iconColorHex = tpl.iconColorHex,
                            themeColorHex = tpl.iconColorHex,
                            profileId = "profile_${index + 1}",
                            feedMonitorUrl = tpl.feedUrl,
                            feedCheckIntervalMinutes = if (tpl.feedUrl.isNotBlank()) 60 else 0,
                            isFavorite = index < 2
                        )
                    )
                }
                database.liteAppDao().insertAll(starterApps)
            }

            val existingRules = database.contentFilterDao().getAllRulesOnce()
            if (existingRules.isEmpty()) {
                val starterRules = listOf(
                    ContentFilterRuleEntity(pattern = "doubleclick.net", isException = false, isEnabled = true),
                    ContentFilterRuleEntity(pattern = "google-analytics.com", isException = false, isEnabled = true),
                    ContentFilterRuleEntity(pattern = "googlesyndication.com", isException = false, isEnabled = true),
                    ContentFilterRuleEntity(pattern = "connect.facebook.net", isException = false, isEnabled = true),
                    ContentFilterRuleEntity(pattern = "taboola.com", isException = false, isEnabled = true),
                    ContentFilterRuleEntity(pattern = "outbrain.com", isException = false, isEnabled = true),
                    ContentFilterRuleEntity(pattern = "criteo.com", isException = false, isEnabled = true)
                )
                database.contentFilterDao().insertAll(starterRules)
            }
        }
    }
}
