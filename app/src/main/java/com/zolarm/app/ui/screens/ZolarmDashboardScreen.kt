package com.zolarm.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zolarm.app.BuildConfig
import com.zolarm.app.R
import com.zolarm.app.core.UpdateChecker
import com.zolarm.app.data.Alarm
import com.zolarm.app.data.ChallengeType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZolarmDashboardScreen(
    alarms: List<Alarm>,
    onToggle: (Alarm, Boolean) -> Unit,
    onDelete: (Alarm) -> Unit,
    onAddClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateChecker.Result?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.ic_zolarm_logo), null, Modifier.size(30.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("زولارم", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, null) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_about)) },
                            onClick = { menu = false; showAbout = true },
                            leadingIcon = { Icon(Icons.Filled.Info, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_update)) },
                            onClick = {
                                menu = false; checking = true
                                scope.launch {
                                    val r = UpdateChecker.checkAndDownload(context)
                                    checking = false
                                    updateResult = r
                                    if (!r.isLatest && r.apkFile != null) UpdateChecker.install(context, r.apkFile)
                                }
                            },
                            leadingIcon = { Icon(Icons.Filled.SystemUpdate, null) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("منبّه جديد", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        if (alarms.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("لا توجد منبّهات بعد.\nاضغط + للبدء.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(alarm, onToggle = { onToggle(alarm, it) }, onDelete = { onDelete(alarm) })
                }
            }
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text(stringResource(R.string.about_title)) },
            text = { Text(stringResource(R.string.about_body, BuildConfig.VERSION_NAME)) },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text(stringResource(R.string.ok)) } }
        )
    }
    if (checking) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.menu_update)) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.update_checking))
                }
            },
            confirmButton = {}
        )
    }
    updateResult?.let { r ->
        val body = when {
            r.error != null && r.latest == null -> stringResource(R.string.update_failed)
            r.isLatest -> stringResource(R.string.update_latest, r.current)
            else -> stringResource(R.string.update_available, r.latest ?: "?", r.current)
        }
        AlertDialog(
            onDismissRequest = { updateResult = null },
            title = { Text(stringResource(R.string.menu_update)) },
            text = { Text(body) },
            confirmButton = {
                TextButton(onClick = {
                    if (!r.isLatest && r.apkFile != null) UpdateChecker.install(context, r.apkFile)
                    updateResult = null
                }) { Text(if (!r.isLatest) stringResource(R.string.update_open) else stringResource(R.string.ok)) }
            },
            dismissButton = {
                if (!r.isLatest) TextButton(onClick = { updateResult = null }) { Text(stringResource(R.string.later)) }
            }
        )
    }
}

@Composable
private fun AlarmCard(alarm: Alarm, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    alarm.timeText,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 38.sp),
                    color = if (alarm.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(alarm.label, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (alarm.challenge == ChallengeType.STEPS) Icons.Default.DirectionsWalk else Icons.Default.PhotoCamera,
                        null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (alarm.challenge == ChallengeType.STEPS) "${alarm.stepTarget} خطوة للإغلاق" else "وجه للإغلاق",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(checked = alarm.enabled, onCheckedChange = onToggle)
                TextButton(onClick = onDelete) { Text("حذف", style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}
