package com.rahul.nagster

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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

    private fun actionIntent(context: Context, nag: Nag, action: String): PendingIntent {
        val intent = Intent(context, NagActionReceiver::class.java)
            .setAction(action)
            .setData(Uri.parse("nagster://nag/${nag.id}/$action"))
            .putExtra(Scheduler.EXTRA_NAG_ID, nag.id)
        return PendingIntent.getBroadcast(
            context, nag.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * The nag's emoji drawn on a filled circle in its colour, used as the
     * notification's large icon. Android reserves the small (status bar) icon
     * for a monochrome silhouette, so this badge is where colour and emoji can
     * actually show up prominently.
     */
    private fun badgeBitmap(nag: Nag): Bitmap {
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val half = size / 2f

        canvas.drawCircle(half, half, half, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = nag.colorArgb ?: NAG_BADGE_NEUTRAL
        })

        if (nag.emoji.isNotEmpty()) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size * 0.55f
                textAlign = Paint.Align.CENTER
            }
            val metrics = textPaint.fontMetrics
            canvas.drawText(
                nag.emoji, half, half - (metrics.ascent + metrics.descent) / 2f, textPaint
            )
        }
        return bitmap
    }

    private fun baseBuilder(context: Context, nag: Nag, alertAgain: Boolean) =
        NotificationCompat.Builder(context, channelId(NagStore.data.value.soundUri))
            .setSmallIcon(R.drawable.ic_stat_nag)
            .setContentTitle(nag.text)
            .apply {
                // Both are optional; an untagged nag looks exactly as it did before.
                if (nag.emoji.isNotEmpty() || nag.colorArgb != null) {
                    setLargeIcon(badgeBitmap(nag))
                }
                nag.colorArgb?.let { setColor(it) }
            }
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, 0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setAutoCancel(false)
            .setOnlyAlertOnce(!alertAgain)

    /** The normal nagging notification. DONE only arms the confirmation step. */
    fun show(context: Context, nag: Nag, alertAgain: Boolean = true) {
        val notification = baseBuilder(context, nag, alertAgain)
            .setContentText("Nagging every ${formatMinutes(nag.intervalMinutes)} until you confirm")
            .addAction(0, "DONE", actionIntent(context, nag, NagActionReceiver.ACTION_DONE))
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(nag.id.toInt(), notification)
    }

    /**
     * Second step of marking a nag done. "Not yet" deliberately takes the first
     * slot — the same position DONE just occupied — so a repeated tap in that
     * spot backs out instead of completing the nag by accident. Never re-alerts.
     */
    fun showConfirm(context: Context, nag: Nag) {
        val notification = baseBuilder(context, nag, alertAgain = false)
            .setContentText("Mark this done? This stops the nagging.")
            .addAction(
                0, "Not yet",
                actionIntent(context, nag, NagActionReceiver.ACTION_DONE_CANCEL),
            )
            .addAction(
                0, "Yes, done",
                actionIntent(context, nag, NagActionReceiver.ACTION_DONE_CONFIRM),
            )
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(nag.id.toInt(), notification)
    }

    fun cancel(context: Context, nagId: Long) {
        context.getSystemService(NotificationManager::class.java).cancel(nagId.toInt())
    }
}
