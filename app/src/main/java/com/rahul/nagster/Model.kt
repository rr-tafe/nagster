package com.rahul.nagster

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Serializable
data class Nag(
    val id: Long = 0,
    val text: String = "",
    val hour: Int = 9,
    val minute: Int = 0,
    /** ISO day-of-week values (1 = Monday .. 7 = Sunday). Empty set = one-off nag. */
    val daysOfWeek: Set<Int> = (1..7).toSet(),
    val intervalMinutes: Int = 10,
    /** 0 = never give up. */
    val giveUpAfterMinutes: Int = 0,
    val snoozeMinutes: Int = 10,
    val enabled: Boolean = true,
    /** Epoch millis when the current nagging session started; null = idle. */
    val activeSince: Long? = null,
    val snoozedUntil: Long? = null,
) {
    val isOneOff: Boolean get() = daysOfWeek.isEmpty()

    fun nextStartMillis(now: Long = System.currentTimeMillis()): Long {
        val zone = ZoneId.systemDefault()
        var candidate = Instant.ofEpochMilli(now).atZone(zone)
            .withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (candidate.toInstant().toEpochMilli() <= now) candidate = candidate.plusDays(1)
        if (daysOfWeek.isNotEmpty()) {
            var guard = 0
            while (candidate.dayOfWeek.value !in daysOfWeek && guard++ < 7) {
                candidate = candidate.plusDays(1)
            }
        }
        return candidate.toInstant().toEpochMilli()
    }

    fun scheduleSummary(): String {
        val time = "%02d:%02d".format(hour, minute)
        val days = when {
            daysOfWeek.isEmpty() -> "Once"
            daysOfWeek.size == 7 -> "Every day"
            else -> daysOfWeek.sorted().joinToString(" ") {
                DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault())
            }
        }
        return "$days at $time · every $intervalMinutes min"
    }
}

@Serializable
data class NagEvent(
    val nagId: Long,
    val text: String,
    val timestamp: Long,
    val action: String,
)

@Serializable
data class StoreData(
    val nags: List<Nag> = emptyList(),
    val events: List<NagEvent> = emptyList(),
    val nextId: Long = 1,
)
