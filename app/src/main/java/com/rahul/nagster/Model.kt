package com.rahul.nagster

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

const val MODE_ROUTINE = "ROUTINE"
const val MODE_DATES = "DATES"
const val MODE_ONCE = "ONCE"

const val THEME_SYSTEM = "SYSTEM"
const val THEME_LIGHT = "LIGHT"
const val THEME_DARK = "DARK"

fun formatMinutes(totalMinutes: Int): String {
    val d = totalMinutes / (24 * 60)
    val h = (totalMinutes % (24 * 60)) / 60
    val m = totalMinutes % 60
    return buildList {
        if (d > 0) add("$d d")
        if (h > 0) add("$h h")
        if (m > 0 || (d == 0 && h == 0)) add("$m min")
    }.joinToString(" ")
}

@Serializable
data class Nag(
    val id: Long = 0,
    val text: String = "",
    val hour: Int = 9,
    val minute: Int = 0,
    /** ISO day-of-week values (1 = Monday .. 7 = Sunday); used in MODE_ROUTINE. */
    val daysOfWeek: Set<Int> = (1..7).toSet(),
    val intervalMinutes: Int = 10,
    /** 0 = never give up. */
    val giveUpAfterMinutes: Int = 0,
    val snoozeMinutes: Int = 10,
    val enabled: Boolean = true,
    /** Epoch millis when the current nagging session started; null = idle. */
    val activeSince: Long? = null,
    val snoozedUntil: Long? = null,
    /** MODE_ROUTINE, MODE_DATES or MODE_ONCE. Blank (data written by v1.0) = derived. */
    val mode: String = "",
    /** ISO local dates ("2026-08-03") this nag fires on; used in MODE_DATES. */
    val dates: List<String> = emptyList(),
    /** Optional ISO local date bounds for MODE_ROUTINE. */
    val startDate: String? = null,
    val endDate: String? = null,
) {
    val effectiveMode: String
        get() = when {
            mode.isNotBlank() -> mode
            daysOfWeek.isEmpty() -> MODE_ONCE
            else -> MODE_ROUTINE
        }

    /** Next moment this nag's schedule begins, or null if it never will again. */
    fun nextStartMillis(now: Long = System.currentTimeMillis()): Long? {
        val zone = ZoneId.systemDefault()
        fun LocalDate.fireMillis() = atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return when (effectiveMode) {
            MODE_DATES -> dates
                .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
                .map { it.fireMillis() }
                .filter { it > now }
                .minOrNull()
            MODE_ONCE -> {
                val t = today.fireMillis()
                if (t > now) t else today.plusDays(1).fireMillis()
            }
            else -> {
                val start = startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                val end = endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                var day = if (start != null && start.isAfter(today)) start else today
                repeat(400) {
                    if (end != null && day.isAfter(end)) return null
                    val allowedDay = daysOfWeek.isEmpty() || day.dayOfWeek.value in daysOfWeek
                    if (allowedDay && day.fireMillis() > now) return day.fireMillis()
                    day = day.plusDays(1)
                }
                null
            }
        }
    }

    fun scheduleSummary(): String {
        val time = "%02d:%02d".format(hour, minute)
        val df = DateTimeFormatter.ofPattern("d MMM")
        val head = when (effectiveMode) {
            MODE_ONCE -> "Once"
            MODE_DATES -> {
                val parsed = dates
                    .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
                    .sorted()
                when {
                    parsed.isEmpty() -> "No dates"
                    parsed.size <= 2 -> parsed.joinToString(", ") { it.format(df) }
                    else -> "${parsed.size} dates"
                }
            }
            else -> {
                val days = if (daysOfWeek.size == 7) "Every day"
                else daysOfWeek.sorted().joinToString(" ") {
                    DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                }
                val from = startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?.takeIf { it.isAfter(LocalDate.now()) }
                    ?.let { " · from ${it.format(df)}" } ?: ""
                val until = endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?.let { " · until ${it.format(df)}" } ?: ""
                "$days$from$until"
            }
        }
        return "$head at $time · every ${formatMinutes(intervalMinutes)}"
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
    val themeMode: String = THEME_SYSTEM,
)
