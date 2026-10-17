package com.litespace.browser

import android.app.Application
import com.litespace.browser.core.webview.ContentBlockerEngine
import com.litespace.browser.core.webview.ProfileIsolationManager
import com.litespace.browser.data.local.LiteSpaceDatabase
import com.litespace.browser.data.preferences.GlobalPreferencesRepository
import com.litespace.browser.data.repository.LiteAppRepository
import com.litespace.browser.worker.FeedNotificationWorker

class LiteSpaceApplication : Application() {

    lateinit var database: LiteSpaceDatabase
        private set

    lateinit var preferencesRepository: GlobalPreferencesRepository
        private set

    lateinit var repository: LiteAppRepository
        private set

    lateinit var contentBlockerEngine: ContentBlockerEngine
        private set

    lateinit var profileIsolationManager: ProfileIsolationManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = LiteSpaceDatabase.getInstance(this)
        preferencesRepository = GlobalPreferencesRepository(this)
        repository = LiteAppRepository(database, preferencesRepository)
        contentBlockerEngine = ContentBlockerEngine(this)
        profileIsolationManager = ProfileIsolationManager(
            cookieVaultDao = database.cookieVaultDao(),
            preferencesRepository = preferencesRepository
        )
        FeedNotificationWorker.schedulePeriodicWork(this)
    }
}
