package com.rahul.nagster

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NagViewModel(app: Application) : AndroidViewModel(app) {

    val data = NagStore.data

    private val ctx: Context get() = getApplication()

    fun save(nag: Nag, onSaved: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            // Saving ends any in-progress nagging session rather than carrying it
            // through unchanged. Without this, editing a nag's schedule while it
            // was actively firing (e.g. moving the date to next week) left the
            // old session running on its interval alone, ignoring the new
            // schedule entirely. rescheduleAfterEdit below re-evaluates from
            // scratch and restarts immediately only if the new schedule still
            // calls for it.
            val saved = NagStore.upsert(nag.copy(activeSince = null))
            Notifications.cancel(ctx, saved.id)
            Scheduler.rescheduleAfterEdit(ctx, saved)
            withContext(Dispatchers.Main) { onSaved() }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            NagStore.delete(id)
            Scheduler.cancel(ctx, id)
            Notifications.cancel(ctx, id)
        }
    }

    fun setEnabled(nag: Nag, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = NagStore.upsert(
                nag.copy(enabled = enabled, activeSince = null)
            )
            if (!enabled) Notifications.cancel(ctx, nag.id)
            Scheduler.reschedule(ctx, updated)
        }
    }

    fun markDone(nag: Nag) {
        viewModelScope.launch(Dispatchers.IO) {
            finishSession(ctx, nag, logDone = true)
        }
    }

    fun setUse24Hour(use24Hour: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            NagStore.setUse24Hour(use24Hour)
        }
    }

    fun setThemeMode(themeMode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            NagStore.setThemeMode(themeMode)
        }
    }

    /** Re-arm every nag, e.g. once exact-alarm permission is finally granted. */
    fun rescheduleAll() {
        viewModelScope.launch(Dispatchers.IO) {
            Scheduler.rescheduleAll(ctx)
        }
    }

    fun setSoundUri(soundUri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            NagStore.setSoundUri(soundUri)
            Notifications.ensureChannel(ctx)
        }
    }

    fun deleteEvents(timestamps: Set<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            NagStore.deleteEvents(timestamps)
        }
    }

    fun clearEvents() {
        viewModelScope.launch(Dispatchers.IO) {
            NagStore.clearEvents()
        }
    }
}
