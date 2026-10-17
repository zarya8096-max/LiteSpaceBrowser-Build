package com.litespace.browser.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Xml
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.litespace.browser.LiteAppActivity
import com.litespace.browser.R
import com.litespace.browser.data.local.LiteSpaceDatabase
import com.litespace.browser.data.preferences.GlobalPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class FeedNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val db = LiteSpaceDatabase.getInstance(applicationContext)
        val dao = db.liteAppDao()
        val monitoredApps = dao.getAppsWithActiveFeedMonitor()
        if (monitoredApps.isEmpty()) {
            return@withContext Result.success()
        }

        ensureNotificationChannel(applicationContext)

        for (appEntity in monitoredApps) {
            val feedUrl = appEntity.feedMonitorUrl.trim()
            if (feedUrl.isBlank()) continue

            runCatching {
                val latestEntry = fetchLatestFeedEntry(feedUrl)
                if (latestEntry != null) {
                    val itemHash = GlobalPreferencesRepository.sha256("${latestEntry.title}|${latestEntry.link}")
                    if (appEntity.lastSeenFeedItemHash.isNotBlank() &&
                        appEntity.lastSeenFeedItemHash != itemHash
                    ) {
                        postFeedNotification(
                            context = applicationContext,
                            liteAppId = appEntity.id,
                            liteAppName = appEntity.name,
                            entryTitle = latestEntry.title,
                            entryLink = latestEntry.link.ifBlank { appEntity.startUrl }
                        )
                    }
                    dao.updateLastSeenFeedHash(appEntity.id, itemHash)
                }
            }
        }
        Result.success()
    }

    private data class FeedEntry(val title: String, val link: String)

    private fun fetchLatestFeedEntry(feedUrl: String): FeedEntry? {
        val connection = (URL(feedUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "LiteSpaceBrowser-FeedMonitor/2.0")
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.use { stream ->
                parseFirstEntry(stream)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseFirstEntry(inputStream: InputStream): FeedEntry? {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        var insideItemOrEntry = false
        var title = ""
        var link = ""
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.lowercase()
                    if (tag == "item" || tag == "entry") {
                        insideItemOrEntry = true
                    } else if (insideItemOrEntry && tag == "title") {
                        title = parser.nextText().trim()
                    } else if (insideItemOrEntry && tag == "link") {
                        val hrefAttr = parser.getAttributeValue(null, "href")
                        link = if (!hrefAttr.isNullOrBlank()) {
                            hrefAttr.trim()
                        } else {
                            runCatching { parser.nextText().trim() }.getOrDefault("")
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tag = parser.name.lowercase()
                    if ((tag == "item" || tag == "entry") && insideItemOrEntry) {
                        if (title.isNotBlank()) {
                            return FeedEntry(title = title, link = link)
                        }
                        insideItemOrEntry = false
                    }
                }
            }
            eventType = parser.next()
        }
        return null
    }

    companion object {
        const val CHANNEL_ID = "litespace_web_notifications"
        private const val WORK_NAME = "litespace_periodic_feed_monitor"

        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Lite App Notifications & Feeds",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications from active Lite Apps and RSS/Atom feed monitors"
                }
                manager.createNotificationChannel(channel)
            }
        }

        fun postFeedNotification(
            context: Context,
            liteAppId: Long,
            liteAppName: String,
            entryTitle: String,
            entryLink: String
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }

            ensureNotificationChannel(context)

            val launchIntent = Intent(context, LiteAppActivity::class.java).apply {
                action = LiteAppActivity.ACTION_LAUNCH_LITE_APP
                putExtra(LiteAppActivity.EXTRA_LITE_APP_ID, liteAppId)
                putExtra(LiteAppActivity.EXTRA_OVERRIDE_URL, entryLink)
                addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                liteAppId.toInt() + entryTitle.hashCode(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(liteAppName)
                .setContentText(entryTitle)
                .setSubText("LiteSpace Feed Monitor")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            NotificationManagerCompat.from(context)
                .notify((liteAppId * 1000L + System.currentTimeMillis() % 1000).toInt(), notification)
        }

        fun schedulePeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<FeedNotificationWorker>(
                1, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
