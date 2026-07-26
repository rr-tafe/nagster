package com.rahul.nagster

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri

object Scheduler {
    const val ACTION_FIRE = "com.rahul.nagster.action.FIRE"
    const val EXTRA_NAG_ID = "nag_id"

    private fun firePendingIntent(context: Context, nagId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(ACTION_FIRE)
            .setData(Uri.parse("nagster://nag/$nagId"))
            .putExtra(EXTRA_NAG_ID, nagId)
        return PendingIntent.getBroadcast(
            context, nagId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Next moment this nag should fire, or null if it never will. */
    fun nextTrigger(nag: Nag, now: Long = System.currentTimeMillis()): Long? {
        if (!nag.enabled) return null
        if (nag.activeSince != null) {
            val snoozed = nag.snoozedUntil ?: 0
            return if (snoozed > now) snoozed else now + nag.intervalMinutes * 60_000L
        }
        return nag.nextStartMillis(now)
    }

    fun reschedule(context: Context, nag: Nag) {
        scheduleAt(context, nag, nextTrigger(nag))
    }

    fun cancel(context: Context, nagId: Long) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(firePendingIntent(context, nagId))
    }

    fun rescheduleAll(context: Context, afterReboot: Boolean = false) {
        val now = System.currentTimeMillis()
        NagStore.data.value.nags.forEach { nag ->
            val trigger = if (afterReboot && nag.enabled && nag.activeSince != null) {
                // Resume an in-progress nagging session shortly after boot
                // instead of waiting a full interval.
                maxOf(nag.snoozedUntil ?: 0, now + 60_000)
            } else {
                nextTrigger(nag, now)
            }
            scheduleAt(context, nag, trigger)
        }
    }

    private fun scheduleAt(context: Context, nag: Nag, triggerAt: Long?) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = firePendingIntent(context, nag.id)
        am.cancel(pi)
        if (triggerAt == null) return
        if (am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }
}
