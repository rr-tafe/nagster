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
            val saved = NagStore.upsert(nag)
            Scheduler.reschedule(ctx, saved)
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
                nag.copy(enabled = enabled, activeSince = null, snoozedUntil = null)
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

    fun setThemeMode(themeMode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            NagStore.setThemeMode(themeMode)
        }
    }
}
