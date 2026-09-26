package com.zolarm.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zolarm.app.core.UpdateChecker
import com.zolarm.app.data.Alarm
import com.zolarm.app.data.AlarmRepository
import com.zolarm.app.service.AlarmScheduler
import com.zolarm.app.service.ZolarmForegroundService
import com.zolarm.app.ui.screens.AddEditAlarmScreen
import com.zolarm.app.ui.screens.SplashScreen
import com.zolarm.app.ui.screens.ZolarmDashboardScreen
import com.zolarm.app.ui.theme.ZolarmTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var repo: AlarmRepository
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = AlarmRepository(this)
        requestRuntimePermissions()
        requestOverlayPermission()
        requestExactAlarmPermission()
        requestBatteryOptimizationExemption()
        startEngine()
        autoUpdate()

        setContent {
            ZolarmTheme {
                val nav = rememberNavController()
                val alarms = remember { mutableStateListOf<Alarm>().apply { addAll(repo.load()) } }
                fun persist() {
                    repo.save(alarms)
                    AlarmScheduler.rescheduleAll(this@MainActivity, alarms)
                }
                Surface(Modifier.fillMaxSize()) {
                    NavHost(navController = nav, startDestination = "splash") {
                        composable("splash") {
                            SplashScreen {
                                nav.navigate("dashboard") { popUpTo("splash") { inclusive = true } }
                            }
                        }
                        composable("dashboard") {
                            ZolarmDashboardScreen(
                                alarms = alarms,
                                onToggle = { alarm, enabled ->
                                    val i = alarms.indexOfFirst { it.id == alarm.id }
                                    if (i >= 0) { alarms[i] = alarm.copy(enabled = enabled); persist() }
                                },
                                onDelete = { alarm ->
                                    AlarmScheduler.cancel(this@MainActivity, alarm)
                                    alarms.removeAll { it.id == alarm.id }
                                    persist()
                                },
                                onAddClick = { nav.navigate("add") }
                            )
                        }
                        composable("add") {
                            AddEditAlarmScreen(
                                onSave = { h, m, label, challenge, target ->
                                    alarms.add(Alarm(repo.nextId(), h, m, label, challenge, target, true))
                                    persist()
                                    nav.popBackStack()
                                },
                                onBack = { nav.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun autoUpdate() {
        lifecycleScope.launch {
            runCatching {
                val r = UpdateChecker.checkAndDownload(applicationContext)
                if (!r.isLatest && r.apkFile != null) UpdateChecker.install(this@MainActivity, r.apkFile)
            }
        }
    }

    private fun requestRuntimePermissions() {
        val perms = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) perms.add(Manifest.permission.ACTIVITY_RECOGNITION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        permissionLauncher.launch(perms.toTypedArray())
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            runCatching {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        }
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(android.app.AlarmManager::class.java)
            if (!am.canScheduleExactAlarms()) {
                runCatching { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) }
            }
        }
    }

    private fun requestBatteryOptimizationExemption() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            runCatching {
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
            }
        }
    }

    private fun startEngine() {
        val i = Intent(this, ZolarmForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i) else startService(i)
    }
}
