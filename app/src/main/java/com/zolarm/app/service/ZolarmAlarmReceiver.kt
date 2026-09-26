package com.zolarm.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.zolarm.app.ui.overlay.ZolarmOverlayActivity

class ZolarmAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ID, 0)
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_LABEL) ?: "زولارم"
        val challenge = intent.getStringExtra(AlarmScheduler.EXTRA_CHALLENGE) ?: "STEPS"
        val target = intent.getIntExtra(AlarmScheduler.EXTRA_STEP_TARGET, 10)

        val svc = Intent(context, ZolarmForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(svc)
        else context.startService(svc)

        context.startActivity(
            Intent(context, ZolarmOverlayActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION
                )
                putExtra(AlarmScheduler.EXTRA_ID, id)
                putExtra(AlarmScheduler.EXTRA_LABEL, label)
                putExtra(AlarmScheduler.EXTRA_CHALLENGE, challenge)
                putExtra(AlarmScheduler.EXTRA_STEP_TARGET, target)
            }
        )
    }
}
