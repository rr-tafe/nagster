package com.rahul.nagster

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri

object Scheduler {
    const val ACTION_FIRE = "com.rahul.nagster.action.FIRE"
    const val ACTION_REVERT_CONFIRM = "com.rahul.nagster.action.REVERT_CONFIRM"
    const val EXTRA_NAG_ID = "nag_id"

    /** How long the "confirm done?" step waits before reverting to the nag. */
    private const val CONFIRM_TIMEOUT_MS = 15_000L

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

    private fun revertPendingIntent(context: Context, nagId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(ACTION_REVERT_CONFIRM)
            .setData(Uri.parse("nagster://revert/$nagId"))
            .putExtra(EXTRA_NAG_ID, nagId)
        return PendingIntent.getBroadcast(
            context, (nagId + 1_000_000L).toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Back out of the confirm step if the user walks away mid-decision. */
    fun scheduleConfirmRevert(context: Context, nagId: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = revertPendingIntent(context, nagId)
        am.cancel(pi)
        am.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + CONFIRM_TIMEOUT_MS, pi
        )
    }

    fun cancelConfirmRevert(context: Context, nagId: Long) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(revertPendingIntent(context, nagId))
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

    /**
     * Reschedule after the user creates or edits a nag. When today's occurrence
     * has already passed but its window is still open, start nagging right away
     * instead of waiting for the next scheduled day — unless it was already
     * confirmed done since that occurrence.
     */
    fun rescheduleAfterEdit(context: Context, nag: Nag) {
        val now = System.currentTimeMillis()
        val missed = nag.liveMissedStart(now)
        val alreadyDone = missed != null && NagStore.data.value.events.any {
            it.nagId == nag.id && it.action == "DONE" && it.timestamp >= missed
        }
        if (missed != null && nag.activeSince == null && !alreadyDone) {
            scheduleAt(context, nag, now + 1_000)
        } else {
            reschedule(context, nag)
        }
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
