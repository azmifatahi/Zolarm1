package com.zolarm.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zolarm.app.challenge.CameraChallenge
import com.zolarm.app.challenge.StepCounterManager
import com.zolarm.app.ui.theme.NeonCyan
import com.zolarm.app.ui.theme.NeonLime

@Composable
fun ZolarmOverlayScreen(
    label: String,
    challenge: String,
    stepTarget: Int,
    onDismissed: () -> Unit
) {
    val context = LocalContext.current
    var unlocked by remember { mutableStateOf(false) }
    var steps by remember { mutableIntStateOf(0) }
    var sensorMissing by remember { mutableStateOf(false) }

    DisposableEffect(challenge, stepTarget) {
        var mgr: StepCounterManager? = null
        if (challenge == "STEPS") {
            mgr = StepCounterManager(
                context = context,
                target = stepTarget,
                onProgress = { c, _ -> steps = c },
                onCompleted = { unlocked = true },
                onSensorMissing = { sensorMissing = true }
            )
            mgr.start()
        }
        onDispose { mgr?.stop() }
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text("زولارم", style = MaterialTheme.typography.headlineLarge, color = NeonCyan, letterSpacing = 4.sp)
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(28.dp))

        when (challenge) {
            "CAMERA" -> CameraChallenge(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                onCaptured = { unlocked = true; onDismissed() }
            )
            else -> {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    val progress = if (stepTarget == 0) 1f else steps.toFloat() / stepTarget
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(190.dp),
                            strokeWidth = 12.dp,
                            color = NeonCyan,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$steps", style = MaterialTheme.typography.displayLarge, color = NeonCyan)
                            Text("/ $stepTarget خطوة", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("انهض وامشِ.", style = MaterialTheme.typography.titleLarge)
                    if (sensorMissing) {
                        Spacer(Modifier.height(12.dp))
                        Text("لا يوجد مستشعر خطوات", color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { unlocked = true }) { Text("فتح يدوي") }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { if (unlocked) onDismissed() },
            enabled = unlocked,
            modifier = Modifier.fillMaxWidth().height(62.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonLime,
                contentColor = MaterialTheme.colorScheme.background,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                if (unlocked) "إغلاق المنبّه" else "أكمل التحدي أولاً",
                fontWeight = FontWeight.Black
            )
        }
    }
}
