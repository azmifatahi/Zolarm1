package com.zolarm.app.ui.overlay

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.zolarm.app.service.AlarmScheduler
import com.zolarm.app.service.ZolarmForegroundService
import com.zolarm.app.ui.screens.ZolarmOverlayScreen
import com.zolarm.app.ui.theme.ZolarmTheme

class ZolarmOverlayActivity : ComponentActivity() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true); setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
                .requestDismissKeyguard(this, null)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* blocked */ }
        })
        startAlarmSound()
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_LABEL) ?: "زولارم"
        val challenge = intent.getStringExtra(AlarmScheduler.EXTRA_CHALLENGE) ?: "STEPS"
        val target = intent.getIntExtra(AlarmScheduler.EXTRA_STEP_TARGET, 10)
        setContent {
            ZolarmTheme {
                Surface(Modifier.fillMaxSize()) {
                    ZolarmOverlayScreen(
                        label = label,
                        challenge = challenge,
                        stepTarget = target,
                        onDismissed = { finishAlarm() }
                    )
                }
            }
        }
    }

    private fun startAlarmSound() {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@ZolarmOverlayActivity, uri)
                isLooping = true
                prepare()
                start()
            }
        }
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 400, 800), 0))
    }

    private fun finishAlarm() {
        runCatching { player?.stop(); player?.release() }
        player = null
        vibrator?.cancel()
        stopService(android.content.Intent(this, ZolarmForegroundService::class.java))
        finishAndRemoveTask()
    }

    override fun onDestroy() {
        runCatching { player?.stop(); player?.release() }
        vibrator?.cancel()
        super.onDestroy()
    }
}
