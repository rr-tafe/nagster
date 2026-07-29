package com.rahul.nagster

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.VibratorManager
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private fun BroadcastReceiver.async(block: suspend () -> Unit) {
    val result = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            block()
        } finally {
            result.finish()
        }
    }
}

/** A crisp rise-and-click buzz plus a confirmation toast when a nag is confirmed done. */
private fun doneFeedback(context: Context, nag: Nag) {
    runCatching {
        val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator
        val effect = if (vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
                VibrationEffect.Composition.PRIMITIVE_CLICK,
            )
        ) {
            VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.6f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 50)
                .compose()
        } else {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
        }
        vibrator.vibrate(effect)
    }
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(context, "Done: ${nag.text}", Toast.LENGTH_SHORT).show()
    }
}

/** Ends the current nagging session and arms the next scheduled occurrence. */
fun finishSession(context: Context, nag: Nag, logDone: Boolean) {
    if (logDone) {
        NagStore.logEvent(NagEvent(nag.id, nag.text, System.currentTimeMillis(), "DONE"))
        doneFeedback(context, nag)
    }
    val expired = when (nag.effectiveMode) {
        MODE_ONCE -> true
        MODE_DATES -> nag.nextStartMillis() == null
        else -> false
    }
    val finished = nag.copy(
        activeSince = null,
        snoozedUntil = null,
        enabled = if (expired) false else nag.enabled,
    )
    NagStore.upsert(finished)
    Notifications.cancel(context, nag.id)
    Scheduler.reschedule(context, finished)
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val nagId = intent.getLongExtra(Scheduler.EXTRA_NAG_ID, -1)
        if (nagId == -1L) return

        if (intent.action == Scheduler.ACTION_REVERT_CONFIRM) {
            // The confirm step timed out — put the nag back the way it was.
            async {
                NagStore.init(context)
                val nag = NagStore.nag(nagId) ?: return@async
                if (nag.enabled && nag.activeSince != null) {
                    Notifications.show(context, nag, alertAgain = false)
                }
            }
            return
        }

        async {
            NagStore.init(context)
            var nag = NagStore.nag(nagId) ?: return@async
            if (!nag.enabled) return@async
            val now = System.currentTimeMillis()

            if (nag.activeSince == null) {
                nag = NagStore.upsert(nag.copy(activeSince = now, snoozedUntil = null))
            }

            if ((nag.snoozedUntil ?: 0) > now) {
                Scheduler.reschedule(context, nag)
                return@async
            }

            if (nag.giveUpAfterMinutes > 0 &&
                now - (nag.activeSince ?: now) >= nag.giveUpAfterMinutes * 60_000L
            ) {
                finishSession(context, nag, logDone = false)
                return@async
            }

            Notifications.show(context, nag)
            Scheduler.reschedule(context, nag)
        }
    }
}

class NagActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_DONE = "com.rahul.nagster.action.DONE"
        const val ACTION_DONE_CONFIRM = "com.rahul.nagster.action.DONE_CONFIRM"
        const val ACTION_DONE_CANCEL = "com.rahul.nagster.action.DONE_CANCEL"
        const val ACTION_SNOOZE = "com.rahul.nagster.action.SNOOZE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val nagId = intent.getLongExtra(Scheduler.EXTRA_NAG_ID, -1)
        if (nagId == -1L) return
        val action = intent.action
        async {
            NagStore.init(context)
            val nag = NagStore.nag(nagId) ?: return@async
            when (action) {
                // First tap only arms the confirmation; nothing is completed yet.
                ACTION_DONE -> {
                    Notifications.showConfirm(context, nag)
                    Scheduler.scheduleConfirmRevert(context, nagId)
                }
                ACTION_DONE_CONFIRM -> {
                    Scheduler.cancelConfirmRevert(context, nagId)
                    finishSession(context, nag, logDone = true)
                }
                ACTION_DONE_CANCEL -> {
                    Scheduler.cancelConfirmRevert(context, nagId)
                    Notifications.show(context, nag, alertAgain = false)
                }
                ACTION_SNOOZE -> {
                    val updated = NagStore.upsert(
                        nag.copy(snoozedUntil = System.currentTimeMillis() + nag.snoozeMinutes * 60_000L)
                    )
                    Notifications.cancel(context, nag.id)
                    Scheduler.reschedule(context, updated)
                }
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val afterReboot = intent.action == Intent.ACTION_BOOT_COMPLETED
        async {
            NagStore.init(context)
            Scheduler.rescheduleAll(context, afterReboot = afterReboot)
        }
    }
}
