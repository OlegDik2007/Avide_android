package com.avidetravel.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.avidetravel.app.data.AvideApi
import com.avidetravel.app.data.InboxItem
import com.avidetravel.app.data.SessionStore
import java.util.concurrent.TimeUnit

class AvideApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        scheduleBackgroundChecks()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "AvideTravel updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "New travel deals, travel tips and AvideTravel updates"
            }
        )
    }

    private fun scheduleBackgroundChecks() {
        val request = PeriodicWorkRequestBuilder<AvideUpdateWorker>(30, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "avide-background-updates",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    companion object {
        const val CHANNEL_ID = "avide_updates"
    }
}

class AvideUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!SessionStore.notificationsEnabled(applicationContext)) return Result.success()

        return try {
            val services = AvideApi.getServices()
            val tips = AvideApi.getTravelTips()

            val newestService = services.maxByOrNull { it.id }
            val newestTip = tips.maxByOrNull { it.id }

            val conversationId = SessionStore.conversationId(applicationContext)
            if (!conversationId.isNullOrBlank()) {
                val messages = runCatching { AvideApi.getChatMessages(conversationId) }.getOrDefault(emptyList())
                val newestAgentMessage = messages.lastOrNull { it.sender == "admin" }
                if (newestAgentMessage != null) {
                    val previousChatId = SessionStore.lastChatMessageId(applicationContext)
                    if (previousChatId != null && newestAgentMessage.id != previousChatId) {
                        val title = "New message from AvideTravel"
                        val body = newestAgentMessage.text
                        pushNotification(300001, title, body)
                        SessionStore.addInboxItem(
                            applicationContext,
                            InboxItem(
                                id = "chat-${newestAgentMessage.id}",
                                title = title,
                                body = body,
                                type = "chat",
                                refId = conversationId
                            )
                        )
                    }
                    SessionStore.setLastChatMessageId(applicationContext, newestAgentMessage.id)
                }
            }

            val previousServiceId = SessionStore.lastServiceId(applicationContext)
            val previousTipId = SessionStore.lastTipId(applicationContext)

            if (newestService != null) {
                if (previousServiceId >= 0 && newestService.id > previousServiceId) {
                    val title = "New AvideTravel deal"
                    val body = "${newestService.title} — ${newestService.displayPrice()}"
                    pushNotification(
                        id = (100000 + newestService.id % 100000).toInt(),
                        title = title,
                        body = body
                    )
                    SessionStore.addInboxItem(
                        applicationContext,
                        InboxItem(
                            id = "deal-${newestService.id}",
                            title = title,
                            body = body,
                            type = "deal",
                            refId = newestService.id.toString()
                        )
                    )
                }
                SessionStore.setLastServiceId(applicationContext, newestService.id)
            }

            if (newestTip != null) {
                if (previousTipId >= 0 && newestTip.id > previousTipId) {
                    val title = "New AvideTravel guide"
                    val body = newestTip.title
                    pushNotification(
                        id = (200000 + newestTip.id % 100000).toInt(),
                        title = title,
                        body = body
                    )
                    SessionStore.addInboxItem(
                        applicationContext,
                        InboxItem(
                            id = "tip-${newestTip.id}",
                            title = title,
                            body = body,
                            type = "tip",
                            refId = newestTip.id.toString()
                        )
                    )
                }
                SessionStore.setLastTipId(applicationContext, newestTip.id)
            }

            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }

    private fun pushNotification(id: Int, title: String, body: String) {
        val intent = Intent(applicationContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, AvideApplication.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(id, notification)
    }
}
