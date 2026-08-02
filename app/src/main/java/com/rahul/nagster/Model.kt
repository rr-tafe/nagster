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

const val GIVEUP_NEVER = "NEVER"
const val GIVEUP_DURATION = "DURATION"
const val GIVEUP_TIME = "TIME"

const val THEME_SYSTEM = "SYSTEM"
const val THEME_LIGHT = "LIGHT"
const val THEME_DARK = "DARK"

/**
 * Swatches a nag can be tagged with. Mid-tone and saturated so the badge reads
 * clearly against both light and dark shades, and so full-colour emoji drawn on
 * top stay legible.
 */
val NAG_COLORS: List<Int> = listOf(
    0xFFE53935.toInt(), 0xFFD81B60.toInt(), 0xFF8E24AA.toInt(), 0xFF5E35B1.toInt(),
    0xFF3949AB.toInt(), 0xFF1E88E5.toInt(), 0xFF039BE5.toInt(), 0xFF00ACC1.toInt(),
    0xFF00897B.toInt(), 0xFF43A047.toInt(), 0xFF7CB342.toInt(), 0xFFC0CA33.toInt(),
    0xFFFDD835.toInt(), 0xFFFFB300.toInt(), 0xFFFB8C00.toInt(), 0xFFF4511E.toInt(),
    0xFF6D4C41.toInt(), 0xFF757575.toInt(), 0xFF546E7A.toInt(), 0xFF37474F.toInt(),
)

/** Fallback badge background when a nag has an emoji but no colour. */
const val NAG_BADGE_NEUTRAL = 0xFF757575.toInt()

/** Clock time in the user's chosen format. */
fun formatClock(hour: Int, minute: Int, use24Hour: Boolean): String =
    if (use24Hour) {
        "%02d:%02d".format(hour, minute)
    } else {
        java.time.LocalTime.of(hour, minute)
            .format(DateTimeFormatter.ofPattern("h:mm a"))
            .uppercase()
    }

/** First grapheme cluster, so multi-codepoint emoji (ZWJ, skin tones) survive. */
fun firstGrapheme(text: String): String {
    if (text.isEmpty()) return ""
    val iterator = java.text.BreakIterator.getCharacterInstance()
    iterator.setText(text)
    val end = iterator.next()
    return if (end == java.text.BreakIterator.DONE) text else text.substring(0, end)
}

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
    /** Single emoji shown on the nag's badge; blank = no badge glyph. */
    val emoji: String = "",
    /** ARGB colour for the badge and notification accent; null = uncoloured. */
    val colorArgb: Int? = null,
    val hour: Int = 9,
    val minute: Int = 0,
    /** ISO day-of-week values (1 = Monday .. 7 = Sunday); used in MODE_ROUTINE. */
    val daysOfWeek: Set<Int> = (1..7).toSet(),
    val intervalMinutes: Int = 10,
    /** Used when effectiveGiveUpMode is GIVEUP_DURATION. 0 = never give up. */
    val giveUpAfterMinutes: Int = 0,
    /** GIVEUP_NEVER, GIVEUP_DURATION or GIVEUP_TIME. Blank = derived, for back-compat. */
    val giveUpMode: String = "",
    /** Wall-clock give-up time; used when effectiveGiveUpMode is GIVEUP_TIME. */
    val giveUpHour: Int? = null,
    val giveUpMinute: Int? = null,
    val enabled: Boolean = true,
    /** Epoch millis when the current nagging session started; null = idle. */
    val activeSince: Long? = null,
    /** MODE_ROUTINE, MODE_DATES or MODE_ONCE. Blank (data written by v1.0) = derived. */
    val mode: String = "",
    /** ISO local dates ("2026-08-03") this nag fires on; used in MODE_DATES. */
    val dates: List<String> = emptyList(),
    /** Optional ISO local date bounds for MODE_ROUTINE. */
    val startDate: String? = null,
    val endDate: String? = null,
    /** Optional end-of-window time on endDate; null = end of day. */
    val endHour: Int? = null,
    val endMinute: Int? = null,
) {
    val effectiveMode: String
        get() = when {
            mode.isNotBlank() -> mode
            daysOfWeek.isEmpty() -> MODE_ONCE
            else -> MODE_ROUTINE
        }

    val effectiveGiveUpMode: String
        get() = when {
            giveUpMode.isNotBlank() -> giveUpMode
            giveUpAfterMinutes > 0 -> GIVEUP_DURATION
            else -> GIVEUP_NEVER
        }

    /**
     * A GIVEUP_TIME give-up must land later in the day than the nag's own start
     * time. Rolling an earlier-or-equal time to "tomorrow" was considered and
     * rejected: it produces give-up windows pushing 24 hours long and can overlap
     * the next scheduled occurrence, tangling two sessions together. Same-day-only
     * is simpler and matches what "give up at 9pm" actually means to someone
     * reading it.
     */
    val giveUpAtOrBeforeStart: Boolean
        get() = effectiveGiveUpMode == GIVEUP_TIME && giveUpHour != null &&
            (giveUpHour < hour || (giveUpHour == hour && (giveUpMinute ?: 0) <= minute))

    /**
     * Absolute deadline for a nagging session that began at [occurrenceStart], or
     * null if this nag never gives up. GIVEUP_TIME is resolved against the
     * calendar date of [occurrenceStart] — always today for both liveMissedStart's
     * same-day check and a live AlarmReceiver session, since give-up is
     * constrained to be same-day-only (see giveUpAtOrBeforeStart).
     */
    fun giveUpBoundMillis(occurrenceStart: Long): Long? {
        val zone = ZoneId.systemDefault()
        return when (effectiveGiveUpMode) {
            GIVEUP_DURATION ->
                if (giveUpAfterMinutes > 0) occurrenceStart + giveUpAfterMinutes * 60_000L else null
            GIVEUP_TIME -> giveUpHour?.let { h ->
                Instant.ofEpochMilli(occurrenceStart).atZone(zone).toLocalDate()
                    .atTime(h, giveUpMinute ?: 0).atZone(zone).toInstant().toEpochMilli()
            }
            else -> null
        }
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
                val endMillis = end?.atTime(endHour ?: 23, endMinute ?: 59)
                    ?.atZone(zone)?.toInstant()?.toEpochMilli()
                var day = if (start != null && start.isAfter(today)) start else today
                repeat(400) {
                    val fire = day.fireMillis()
                    if (endMillis != null && fire > endMillis) return null
                    val allowedDay = daysOfWeek.isEmpty() || day.dayOfWeek.value in daysOfWeek
                    if (allowedDay && fire > now) return fire
                    day = day.plusDays(1)
                }
                null
            }
        }
    }

    /**
     * Today's occurrence when its time has already passed but its nagging window
     * is still open, so a freshly saved nag starts nagging now instead of waiting
     * for the next scheduled day. Only applies when a window bound actually
     * exists (an end date/time or a give-up duration) — an unbounded nag whose
     * time passed hours ago should wait rather than ambush the user.
     */
    fun liveMissedStart(now: Long = System.currentTimeMillis()): Long? {
        if (!enabled) return null
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val scheduledToday = when (effectiveMode) {
            MODE_ROUTINE -> {
                val start = startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                (start == null || !start.isAfter(today)) &&
                    (daysOfWeek.isEmpty() || today.dayOfWeek.value in daysOfWeek)
            }
            MODE_DATES -> today.toString() in dates
            else -> false
        }
        if (!scheduledToday) return null

        val fire = today.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
        if (fire > now) return null

        val endMillis = if (effectiveMode == MODE_ROUTINE) {
            endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?.atTime(endHour ?: 23, endMinute ?: 59)
                ?.atZone(zone)?.toInstant()?.toEpochMilli()
        } else null
        val giveUpMillis = giveUpBoundMillis(fire)
        val bound = when {
            endMillis != null && giveUpMillis != null -> minOf(endMillis, giveUpMillis)
            endMillis != null -> endMillis
            giveUpMillis != null -> giveUpMillis
            else -> return null
        }
        return if (now <= bound) fire else null
    }

    /** True when nothing this nag is configured for can ever fire again. */
    fun willNeverFire(now: Long = System.currentTimeMillis()): Boolean =
        nextStartMillis(now) == null && liveMissedStart(now) == null

    fun scheduleSummary(use24Hour: Boolean): String {
        val time = formatClock(hour, minute, use24Hour)
        val df = DateTimeFormatter.ofPattern("d MMM")
        val parts = mutableListOf<String>()
        when (effectiveMode) {
            MODE_ONCE -> parts += "Once at $time"
            MODE_DATES -> {
                val parsed = dates
                    .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
                    .sorted()
                val head = when {
                    parsed.isEmpty() -> "No dates"
                    parsed.size <= 2 -> parsed.joinToString(", ") { it.format(df) }
                    else -> "${parsed.size} dates"
                }
                parts += "$head at $time"
            }
            else -> {
                val days = if (daysOfWeek.size == 7) "Every day"
                else daysOfWeek.sorted().joinToString(" ") {
                    DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                }
                parts += "$days at $time"
                startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?.takeIf { it.isAfter(LocalDate.now()) }
                    ?.let { parts += "from ${it.format(df)}" }
                endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }?.let {
                    val t = if (endHour != null) {
                        " " + formatClock(endHour, endMinute ?: 0, use24Hour)
                    } else ""
                    parts += "until ${it.format(df)}$t"
                }
            }
        }
        parts += "every ${formatMinutes(intervalMinutes)}"
        return parts.joinToString(" · ")
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
    /** null = follow the device's clock setting. */
    val use24Hour: Boolean? = null,
    /** Ringtone URI for nag notifications; null = system default. */
    val soundUri: String? = null,
)
