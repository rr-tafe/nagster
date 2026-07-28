package com.rahul.nagster

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat

object Notifications {
    const val CHANNEL_ID = "nags"

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Nags", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Persistent nagging reminders"
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
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

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
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
