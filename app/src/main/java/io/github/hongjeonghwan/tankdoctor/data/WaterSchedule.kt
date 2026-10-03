package io.github.hongjeonghwan.tankdoctor.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/** How often the water should be changed and when to be reminded; [intervalDays] 0 means off. */
data class WaterSchedule(
    val intervalDays: Int = 0,
    val notify: Boolean = false,
    val minuteOfDay: Int = DEFAULT_MINUTE,
) {
    val isSet: Boolean get() = intervalDays > 0
    val time: LocalTime get() = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)

    /** e.g. "오후 8:30" */
    val timeLabel: String
        get() {
            val h = minuteOfDay / 60
            val h12 = if (h % 12 == 0) 12 else h % 12
            return "${if (h < 12) "오전" else "오후"} $h12:${"%02d".format(minuteOfDay % 60)}"
        }

    /** The day the next change is due; with no change logged yet, it is due today. */
    fun dueDate(lastWater: LocalDate?, today: LocalDate = LocalDate.now()): LocalDate? =
        if (!isSet) null else lastWater?.plusDays(intervalDays.toLong()) ?: today

    /**
     * When to ring next: the reminder time on the due day, or once that has passed, the next
     * reminder time, so an overdue change is nagged about once a day until it is logged.
     */
    fun nextAlarm(lastWater: LocalDate?, now: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        if (!isSet || !notify) return null
        val due = dueDate(lastWater, now.toLocalDate())!!.atTime(time)
        if (due.isAfter(now)) return due
        val today = now.toLocalDate().atTime(time)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    companion object {
        const val DEFAULT_MINUTE = 9 * 60
    }
}

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
