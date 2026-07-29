package com.rahul.nagster

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationCompat

object Notifications {
    private const val CHANNEL_PREFIX = "nags_"
    private const val LEGACY_CHANNEL_ID = "nags"

    /**
     * Android freezes a channel's sound once it is created, so changing the
     * sound means publishing a *new* channel. The id is derived from the chosen
     * ringtone; stale channels are deleted so the system list stays tidy.
     */
    private fun channelId(soundUri: String?): String =
        CHANNEL_PREFIX + (soundUri?.hashCode()?.toUInt()?.toString(16) ?: "default")

    fun ensureChannel(context: Context) {
        val soundUri = NagStore.data.value.soundUri
        val id = channelId(soundUri)
        val nm = context.getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            id, "Nags", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Persistent nagging reminders"
            enableVibration(true)
            setSound(
                soundUri?.let(Uri::parse) ?: Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        nm.createNotificationChannel(channel)

        nm.notificationChannels
            .filter { it.id != id && (it.id == LEGACY_CHANNEL_ID || it.id.startsWith(CHANNEL_PREFIX)) }
            .forEach { nm.deleteNotificationChannel(it.id) }
    }

    fun show(context: Context, nag: Nag) {
        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun actionIntent(action: String): PendingIntent {
            val intent = Intent(context, NagActionReceiver::class.java)
                .setAction(action)
                .setData(Uri.parse("nagster://nag/${nag.id}/$action"))
                .putExtra(Scheduler.EXTRA_NAG_ID, nag.id)
            return PendingIntent.getBroadcast(
                context, nag.id.toInt(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(
            context, channelId(NagStore.data.value.soundUri)
        )
            .setSmallIcon(R.drawable.ic_stat_nag)
            .setContentTitle(nag.text)
            .setContentText("Nagging every ${nag.intervalMinutes} min until you confirm")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            .setAutoCancel(false)
            .setOnlyAlertOnce(false)
            .addAction(0, "DONE", actionIntent(NagActionReceiver.ACTION_DONE))
            .addAction(0, "Snooze ${nag.snoozeMinutes}m", actionIntent(NagActionReceiver.ACTION_SNOOZE))
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(nag.id.toInt(), notification)
    }

    fun cancel(context: Context, nagId: Long) {
        context.getSystemService(NotificationManager::class.java).cancel(nagId.toInt())
    }
}
