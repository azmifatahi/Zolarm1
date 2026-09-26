package com.zolarm.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.zolarm.app.data.Alarm
import java.util.Calendar

object AlarmScheduler {
    const val EXTRA_ID = "zolarm.extra.ID"
    const val EXTRA_LABEL = "zolarm.extra.LABEL"
    const val EXTRA_CHALLENGE = "zolarm.extra.CHALLENGE"
    const val EXTRA_STEP_TARGET = "zolarm.extra.STEP_TARGET"

    private fun pendingIntent(context: Context, alarm: Alarm): PendingIntent {
        val intent = Intent(context, ZolarmAlarmReceiver::class.java).apply {
            action = "com.zolarm.app.FIRE"
            putExtra(EXTRA_ID, alarm.id)
            putExtra(EXTRA_LABEL, alarm.label)
            putExtra(EXTRA_CHALLENGE, alarm.challenge.name)
            putExtra(EXTRA_STEP_TARGET, alarm.stepTarget)
        }
        return PendingIntent.getBroadcast(
            context, alarm.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun schedule(context: Context, alarm: Alarm) {
        val am = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) return
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val showIntent = PendingIntent.getActivity(
            context, alarm.id,
            Intent(context, com.zolarm.app.MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(cal.timeInMillis, showIntent), pendingIntent(context, alarm))
        } catch (_: SecurityException) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent(context, alarm))
        }
    }

    fun cancel(context: Context, alarm: Alarm) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context, alarm))
    }

    fun rescheduleAll(context: Context, alarms: List<Alarm>) {
        alarms.forEach { if (it.enabled) schedule(context, it) else cancel(context, it) }
    }
}
