package com.fazenda.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.fazenda.app.FazendaApplication
import com.fazenda.app.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra("scheduleId", -1L)
        if (scheduleId == -1L) return

        val pendingResult = goAsync()
        val application = context.applicationContext as FazendaApplication
        val scheduleRepository = application.scheduleRepository
        val categoryRepository = application.categoryRepository

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val schedule = scheduleRepository.getScheduleById(scheduleId)
                if (schedule != null && !schedule.isCompleted) {
                    val category = schedule.categoryId?.let { categoryRepository.getCategoryById(it) }
                    val categoryName = category?.name ?: "Всі рослини"
                    
                    showNotification(
                        context = context,
                        id = scheduleId.toInt(),
                        title = "Заплановано догляд: $categoryName",
                        message = "${schedule.phaseTime}: ${schedule.recipe}"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, id: Int, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "schedule_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Нагадування про догляд",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Нагадування про планові обробки рослин"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notification)
    }
}
