package io.github.hongjeonghwan.tankdoctor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class WaterScheduleTest {
    private val weekly = WaterSchedule(intervalDays = 7, notify = true, minuteOfDay = 9 * 60)
    private val oct1 = LocalDate.of(2026, 10, 1)

    @Test
    fun offWhenNoInterval() {
        assertNull(WaterSchedule().dueDate(oct1))
        assertNull(WaterSchedule(notify = true).nextAlarm(oct1))
    }

    @Test
    fun noAlarmWhenNotifyOff() {
        assertNull(weekly.copy(notify = false).nextAlarm(oct1, LocalDateTime.of(2026, 10, 2, 8, 0)))
    }

    @Test
    fun dueIntervalDaysAfterLastChange() {
        assertEquals(LocalDate.of(2026, 10, 8), weekly.dueDate(oct1))
    }

    @Test
    fun noRecordIsDueToday() {
        assertEquals(oct1, weekly.dueDate(null, oct1))
    }

    @Test
    fun ringsAtReminderTimeOnDueDay() {
        val now = LocalDateTime.of(2026, 10, 3, 12, 0)
        assertEquals(LocalDateTime.of(2026, 10, 8, 9, 0), weekly.nextAlarm(oct1, now))
    }

    @Test
    fun overdueRingsTodayIfTimeNotPassed() {
        val now = LocalDateTime.of(2026, 10, 10, 8, 0)
        assertEquals(LocalDateTime.of(2026, 10, 10, 9, 0), weekly.nextAlarm(oct1, now))
    }

    @Test
    fun overdueRingsTomorrowOnceTimePassed() {
        val now = LocalDateTime.of(2026, 10, 8, 9, 1)
        assertEquals(LocalDateTime.of(2026, 10, 9, 9, 0), weekly.nextAlarm(oct1, now))
    }

    @Test
    fun timeLabel() {
        assertEquals("오전 9:00", weekly.timeLabel)
        assertEquals("오후 12:05", weekly.copy(minuteOfDay = 12 * 60 + 5).timeLabel)
        assertEquals("오전 12:30", weekly.copy(minuteOfDay = 30).timeLabel)
        assertEquals("오후 8:30", weekly.copy(minuteOfDay = 20 * 60 + 30).timeLabel)
    }

    @Test
    fun dueLabels() {
        assertEquals("3일 남음", dueLabel(oct1.plusDays(3), oct1))
        assertEquals("오늘", dueLabel(oct1, oct1))
        assertEquals("2일 지남", dueLabel(oct1.minusDays(2), oct1))
    }

    @Test
    fun lastWaterIsLatestWaterEntry() {
        val entries = listOf(
            LogEntry(1, oct1, LogCategory.WATER, ""),
            LogEntry(2, oct1.plusDays(2), LogCategory.PLANT, ""),
            LogEntry(3, oct1.minusDays(5), LogCategory.WATER, ""),
        )
        assertEquals(oct1, lastWaterChange(entries))
        assertNull(lastWaterChange(emptyList()))
    }
}
