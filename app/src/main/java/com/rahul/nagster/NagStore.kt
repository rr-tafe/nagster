package com.rahul.nagster

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Single-file JSON persistence. All mutations are synchronized and written
 * through to disk immediately; the StateFlow feeds the Compose UI.
 */
object NagStore {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var file: File? = null
    private val lock = Any()

    private val _data = MutableStateFlow(StoreData())
    val data: StateFlow<StoreData> get() = _data

    fun init(context: Context) {
        synchronized(lock) {
            if (file != null) return
            val f = File(context.applicationContext.filesDir, "nagster.json")
            file = f
            if (f.exists()) {
                runCatching { _data.value = json.decodeFromString<StoreData>(f.readText()) }
            }
        }
    }

    fun nag(id: Long): Nag? = _data.value.nags.find { it.id == id }

    fun upsert(nag: Nag): Nag {
        synchronized(lock) {
            val d = _data.value
            val saved: Nag
            if (nag.id == 0L) {
                saved = nag.copy(id = d.nextId)
                _data.value = d.copy(nags = d.nags + saved, nextId = d.nextId + 1)
            } else {
                saved = nag
                _data.value = d.copy(nags = d.nags.map { if (it.id == nag.id) nag else it })
            }
            persist()
            return saved
        }
    }

    fun delete(id: Long) {
        synchronized(lock) {
            _data.value = _data.value.let { it.copy(nags = it.nags.filter { n -> n.id != id }) }
            persist()
        }
    }

    fun setSoundUri(soundUri: String?) {
        synchronized(lock) {
            _data.value = _data.value.copy(soundUri = soundUri)
            persist()
        }
    }

    fun setUse24Hour(use24Hour: Boolean) {
        synchronized(lock) {
            _data.value = _data.value.copy(use24Hour = use24Hour)
            persist()
        }
    }

    fun setThemeMode(themeMode: String) {
        synchronized(lock) {
            _data.value = _data.value.copy(themeMode = themeMode)
            persist()
        }
    }

    fun logEvent(event: NagEvent) {
        synchronized(lock) {
            _data.value = _data.value.let { it.copy(events = (it.events + event).takeLast(500)) }
            persist()
        }
    }

    fun deleteEvents(timestamps: Set<Long>) {
        synchronized(lock) {
            _data.value = _data.value.let {
                it.copy(events = it.events.filterNot { e -> e.timestamp in timestamps })
            }
            persist()
        }
    }

    fun clearEvents() {
        synchronized(lock) {
            _data.value = _data.value.copy(events = emptyList())
            persist()
        }
    }

    private fun persist() {
        val f = file ?: return
        runCatching { f.writeText(json.encodeToString(StoreData.serializer(), _data.value)) }
    }
}
