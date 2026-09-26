package com.zolarm.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zolarm.app.data.ChallengeType
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAlarmScreen(
    onSave: (hour: Int, minute: Int, label: String, challenge: ChallengeType, stepTarget: Int) -> Unit,
    onBack: () -> Unit
) {
    val now = remember { Calendar.getInstance() }
    val timeState = rememberTimePickerState(
        initialHour = now.get(Calendar.HOUR_OF_DAY),
        initialMinute = now.get(Calendar.MINUTE),
        is24Hour = true
    )
    var label by remember { mutableStateOf("") }
    var challenge by remember { mutableStateOf(ChallengeType.STEPS) }
    var stepTarget by remember { mutableFloatStateOf(10f) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("منبّه جديد", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            TimePicker(state = timeState)
            OutlinedTextField(
                value = label, onValueChange = { label = it },
                label = { Text("عنوان المهمة") },
                placeholder = { Text("مثال: استيقاظ، جيم") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Text("تحدي الإيقاف", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChallengeChip(challenge == ChallengeType.STEPS, "خطوات", Modifier.weight(1f)) { challenge = ChallengeType.STEPS }
                ChallengeChip(challenge == ChallengeType.CAMERA, "كاميرا", Modifier.weight(1f)) { challenge = ChallengeType.CAMERA }
            }
            if (challenge == ChallengeType.STEPS) {
                Column(Modifier.fillMaxWidth()) {
                    Text("الخطوات المطلوبة: ${stepTarget.toInt()}")
                    Slider(value = stepTarget, onValueChange = { stepTarget = it }, valueRange = 5f..100f, steps = 18)
                }
            }
            Button(
                onClick = {
                    onSave(timeState.hour, timeState.minute, label.ifBlank { "مهمة" }, challenge, stepTarget.toInt())
                },
                modifier = Modifier.fillMaxWidth().height(58.dp)
            ) { Text("تفعيل المنبّه", fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun ChallengeChip(selected: Boolean, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val icon = if (label == "خطوات") Icons.Default.DirectionsWalk else Icons.Default.PhotoCamera
    OutlinedCard(
        modifier = modifier, onClick = onClick,
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(label, fontWeight = FontWeight.Bold, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
        }
    }
}
