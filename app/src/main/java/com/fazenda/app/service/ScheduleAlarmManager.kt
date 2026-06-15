package com.fazenda.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.fazenda.app.data.entity.ScheduleEntity

class ScheduleAlarmManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(schedule: ScheduleEntity) {
        if (schedule.isCompleted) {
            cancelAlarm(schedule.id)
            return
        }

        val now = System.currentTimeMillis()
        if (schedule.startDate < now) {
            return
        }

        val intent = Intent(context, ScheduleNotificationReceiver::class.java).apply {
            putExtra("scheduleId", schedule.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    schedule.startDate,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    schedule.startDate,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback to inexact alarm if the app does not have permission to schedule exact alarms
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                schedule.startDate,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(scheduleId: Long) {
        val intent = Intent(context, ScheduleNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
