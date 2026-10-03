package io.github.hongjeonghwan.tankdoctor.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * How often the water should be changed and when to be reminded; [intervalDays] 0 means off.
 * Weekends and public holidays get their own reminder time.
 */
data class WaterSchedule(
    val intervalDays: Int = 0,
    val notify: Boolean = false,
    val weekdayMinute: Int = DEFAULT_WEEKDAY_MINUTE,
    val holidayMinute: Int = DEFAULT_HOLIDAY_MINUTE,
) {
    val isSet: Boolean get() = intervalDays > 0

    fun minuteOn(date: LocalDate): Int = if (isHoliday(date)) holidayMinute else weekdayMinute

    fun timeOn(date: LocalDate): LocalTime = minuteOn(date).let { LocalTime.of(it / 60, it % 60) }

    /** The day the next change is due; with no change logged yet, it is due today. */
    fun dueDate(lastWater: LocalDate?, today: LocalDate = LocalDate.now()): LocalDate? =
        if (!isSet) null else lastWater?.plusDays(intervalDays.toLong()) ?: today

    /**
     * When to ring next: the reminder time on the due day, or once that has passed, the next
     * reminder time, so an overdue change is nagged about once a day until it is logged.
     */
    fun nextAlarm(lastWater: LocalDate?, now: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        if (!isSet || !notify) return null
        val due = dueDate(lastWater, now.toLocalDate())!!
        val first = maxOf(due, now.toLocalDate())
        return generateSequence(first) { it.plusDays(1) }
            .map { it.atTime(timeOn(it)) }
            .first { it.isAfter(now) }
    }

    companion object {
        const val DEFAULT_WEEKDAY_MINUTE = 9 * 60
        const val DEFAULT_HOLIDAY_MINUTE = 10 * 60
    }
}

/** e.g. "오후 8:30" */
fun minuteLabel(minuteOfDay: Int): String {
    val h = minuteOfDay / 60
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "${if (h < 12) "오전" else "오후"} $h12:${"%02d".format(minuteOfDay % 60)}"
}

fun isHoliday(date: LocalDate): Boolean =
    date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY || date in KOREAN_HOLIDAYS

/**
 * Korean public holidays falling on weekdays, substitutes included. Lunar dates shift every
 * year, so this list needs a new year added before it runs out.
 */
private val KOREAN_HOLIDAYS: Set<LocalDate> = listOf(
    // 2026
    "2026-01-01", "2026-02-16", "2026-02-17", "2026-02-18", "2026-03-02", "2026-05-05",
    "2026-05-25", "2026-06-03", "2026-08-17", "2026-09-24", "2026-09-25", "2026-10-05",
    "2026-10-09", "2026-12-25",
    // 2027
    "2027-01-01", "2027-02-08", "2027-02-09", "2027-03-01", "2027-05-05", "2027-05-13",
    "2027-08-16", "2027-09-14", "2027-09-15", "2027-09-16", "2027-10-04", "2027-10-11",
    "2027-12-27",
).map(LocalDate::parse).toSet()

fun lastWaterChange(entries: List<LogEntry>): LocalDate? =
    entries.filter { it.category == LogCategory.WATER }.maxOfOrNull { it.date }

/** e.g. "3일 남음", "오늘", "2일 지남" */
fun dueLabel(due: LocalDate, today: LocalDate = LocalDate.now()): String {
    val days = ChronoUnit.DAYS.between(today, due)
    return when {
        days > 0 -> "${days}일 남음"
        days == 0L -> "오늘"
        else -> "${-days}일 지남"
    }
}
