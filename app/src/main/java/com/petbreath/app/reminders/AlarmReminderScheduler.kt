package com.petbreath.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.petbreath.app.data.db.ReminderEntity
import com.petbreath.app.data.repository.ReminderScheduler
import com.petbreath.app.domain.ReminderSchedule
import java.time.Instant
import java.time.ZoneId

/**
 * Schedules one inexact, doze-tolerant alarm per reminder. When an alarm fires,
 * [ReminderReceiver] posts the notification and schedules the next occurrence.
 *
 * Inexact alarms avoid the special "exact alarm" permission; the system may
 * deliver them a few minutes late, which is acceptable for these reminders.
 */
class AlarmReminderScheduler(context: Context) : ReminderScheduler {
    private val appContext = context.applicationContext
    private val alarmManager = checkNotNull(appContext.getSystemService(AlarmManager::class.java))

    override fun schedule(reminder: ReminderEntity) {
        val next = ReminderSchedule.nextTrigger(
            now = Instant.now(),
            zone = ZoneId.systemDefault(),
            hour = reminder.hour,
            minute = reminder.minute,
            daysMask = reminder.daysMask,
        )
        if (next == null || !reminder.enabled) {
            cancel(reminder.id)
            return
        }
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next.toInstant().toEpochMilli(),
            pendingIntent(reminder.id),
        )
    }

    override fun cancel(reminderId: Long) {
        alarmManager.cancel(pendingIntent(reminderId))
    }

    private fun pendingIntent(reminderId: Long): PendingIntent {
        val intent = Intent(appContext, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_FIRE)
            .putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
        return PendingIntent.getBroadcast(
            appContext,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
