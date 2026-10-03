package io.github.hongjeonghwan.tankdoctor.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.hongjeonghwan.tankdoctor.MainActivity
import io.github.hongjeonghwan.tankdoctor.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** One pending alarm for the next water change; every call replaces the previous one. */
object WaterReminder {
    private const val CHANNEL = "water_change"
    private const val NOTIFICATION_ID = 1

    fun schedule(
        context: Context,
        schedule: WaterSchedule,
        lastWater: LocalDate?,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pending = alarmIntent(context)
        alarms.cancel(pending)
        val at = schedule.nextAlarm(lastWater, now) ?: return
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Inexact is fine for a daily chore and needs no exact-alarm permission.
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
    }

    /** For when no ViewModel is around: reads what the app stored. */
    fun reschedule(context: Context, now: LocalDateTime = LocalDateTime.now()) =
        schedule(context, SettingsStore(context).waterSchedule, lastWaterChange(LogStore(context).load()), now)

    @SuppressLint("MissingPermission") // checked on the first line
    fun notify(context: Context, schedule: WaterSchedule, lastWater: LocalDate?) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "환수 알림", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val text = lastWater?.let { "마지막 환수 ${relativeDay(daysAgo(it))} · ${schedule.intervalDays}일 주기라 물을 갈아 줄 때예요." }
            ?: "아직 환수 기록이 없어요. 물을 갈아 주고 기록해 두세요."
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_water)
            .setContentTitle("💧 환수할 때가 됐어요")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, WaterReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/** Fires the reminder, and puts the alarm back after a reboot or app update clears it. */
class WaterReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val schedule = SettingsStore(context).waterSchedule
        val last = lastWaterChange(LogStore(context).load())
        if (intent.action == null) {
            // Only ring if still due: a change may have been logged since the alarm was set.
            val due = schedule.dueDate(last)
            if (schedule.notify && due != null && !due.isAfter(LocalDate.now())) {
                WaterReminder.notify(context, schedule, last)
            }
        }
        // A minute ahead so an alarm delivered a little early does not pick today's slot again.
        WaterReminder.schedule(context, schedule, last, LocalDateTime.now().plusMinutes(1))
    }
}
