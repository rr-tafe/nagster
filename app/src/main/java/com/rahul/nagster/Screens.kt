@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.rahul.nagster

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun NagsterTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scheme = if (isSystemInDarkTheme()) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun NagsterApp(startInQuickAdd: Boolean, vm: NagViewModel = viewModel()) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "list") {
        composable("list") {
            ListScreen(
                vm = vm,
                onAdd = { nav.navigate("edit/0") },
                onEdit = { nav.navigate("edit/$it") },
                onHistory = { nav.navigate("history") },
            )
        }
        composable(
            "edit/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            EditScreen(
                vm = vm,
                nagId = entry.arguments?.getLong("id") ?: 0L,
                onClose = { nav.popBackStack() },
            )
        }
        composable("history") {
            HistoryScreen(vm = vm, onClose = { nav.popBackStack() })
        }
    }
    LaunchedEffect(startInQuickAdd) {
        if (startInQuickAdd) nav.navigate("edit/0")
    }
}

private fun formatMillis(millis: Long): String {
    val zdt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    return zdt.format(DateTimeFormatter.ofPattern("EEE HH:mm"))
}

@Composable
fun ListScreen(
    vm: NagViewModel,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onHistory: () -> Unit,
) {
    val data by vm.data.collectAsState()
    val context = LocalContext.current

    var exactAlarmsOk by remember { mutableStateOf(true) }
    var batteryOk by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        exactAlarmsOk = context.getSystemService(AlarmManager::class.java)
            .canScheduleExactAlarms()
        batteryOk = context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nagster") },
                actions = {
                    IconButton(onClick = onHistory) {
                        Icon(Icons.Filled.History, contentDescription = "History")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Add nag")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!exactAlarmsOk) {
                item {
                    WarningCard(
                        text = "Exact alarms are not allowed — nags may fire late.",
                        buttonText = "Allow",
                    ) {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:${context.packageName}"),
                            )
                        )
                    }
                }
            }
            if (!batteryOk) {
                item {
                    WarningCard(
                        text = "Battery optimization may delay nags.",
                        buttonText = "Exempt",
                    ) {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}"),
                            )
                        )
                    }
                }
            }
            if (data.nags.isEmpty()) {
                item {
                    Text(
                        "No nags yet. Tap + to create one.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 32.dp),
                    )
                }
            }
            items(data.nags, key = { it.id }) { nag ->
                NagCard(
                    nag = nag,
                    onClick = { onEdit(nag.id) },
                    onToggle = { vm.setEnabled(nag, it) },
                    onDone = { vm.markDone(nag) },
                )
            }
        }
    }
}

@Composable
private fun WarningCard(text: String, buttonText: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClick) { Text(buttonText) }
        }
    }
}

@Composable
private fun NagCard(
    nag: Nag,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDone: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    nag.text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    nag.scheduleSummary(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                when {
                    !nag.enabled -> Text(
                        "Off",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    nag.activeSince != null -> {
                        Text(
                            "🔔 Nagging now (since ${formatMillis(nag.activeSince)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        TextButton(onClick = onDone) { Text("Mark done ✓") }
                    }
                    else -> Text(
                        "Next: ${formatMillis(nag.nextStartMillis())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(checked = nag.enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
fun EditScreen(vm: NagViewModel, nagId: Long, onClose: () -> Unit) {
    val data by vm.data.collectAsState()
    val existing = remember(nagId) { data.nags.find { it.id == nagId } }

    var text by remember { mutableStateOf(existing?.text ?: "") }
    var hour by remember { mutableStateOf(existing?.hour ?: 9) }
    var minute by remember { mutableStateOf(existing?.minute ?: 0) }
    var days by remember { mutableStateOf(existing?.daysOfWeek ?: (1..7).toSet()) }
    var interval by remember { mutableStateOf(existing?.intervalMinutes ?: 10) }
    var giveUp by remember { mutableStateOf(existing?.giveUpAfterMinutes ?: 0) }
    var snooze by remember { mutableStateOf(existing?.snoozeMinutes ?: 10) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = hour, initialMinute = minute, is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    hour = state.hour
                    minute = state.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = state) },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (nagId == 0L) "New nag" else "Edit nag") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (nagId != 0L) {
                        IconButton(onClick = {
                            vm.delete(nagId)
                            onClose()
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("What should I nag you about?") },
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("Start time")
            Card(
                modifier = Modifier.clickable { showTimePicker = true },
            ) {
                Text(
                    "%02d:%02d".format(hour, minute),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }

            SectionLabel("Repeat on")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..7).forEach { day ->
                    FilterChip(
                        selected = day in days,
                        onClick = {
                            days = if (day in days) days - day else days + day
                        },
                        label = {
                            Text(
                                DayOfWeek.of(day)
                                    .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            )
                        },
                    )
                }
            }
            if (days.isEmpty()) {
                Text(
                    "No days selected — this nag fires once at the next occurrence of the start time, then turns itself off.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionLabel("Nag every")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1, 2, 5, 10, 15, 30, 60).forEach { m ->
                    FilterChip(
                        selected = interval == m,
                        onClick = { interval = m },
                        label = { Text("$m min") },
                    )
                }
            }

            SectionLabel("Give up after")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0 to "Never", 60 to "1 h", 180 to "3 h", 360 to "6 h", 720 to "12 h")
                    .forEach { (m, label) ->
                        FilterChip(
                            selected = giveUp == m,
                            onClick = { giveUp = m },
                            label = { Text(label) },
                        )
                    }
            }

            SectionLabel("Snooze length")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(5, 10, 15, 30).forEach { m ->
                    FilterChip(
                        selected = snooze == m,
                        onClick = { snooze = m },
                        label = { Text("$m min") },
                    )
                }
            }

            androidx.compose.material3.Button(
                onClick = {
                    val base = existing ?: Nag()
                    vm.save(
                        base.copy(
                            text = text.trim(),
                            hour = hour,
                            minute = minute,
                            daysOfWeek = days,
                            intervalMinutes = interval,
                            giveUpAfterMinutes = giveUp,
                            snoozeMinutes = snooze,
                            enabled = true,
                        )
                    ) { onClose() }
                },
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun HistoryScreen(vm: NagViewModel, onClose: () -> Unit) {
    val data by vm.data.collectAsState()
    val events = remember(data) { data.events.sortedByDescending { it.timestamp } }
    val formatter = remember {
        DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", Locale.getDefault())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (events.isEmpty()) {
                item { Text("Nothing confirmed yet.") }
            }
            items(events) { event ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(event.text, style = MaterialTheme.typography.titleSmall)
                        Text(
                            Instant.ofEpochMilli(event.timestamp)
                                .atZone(ZoneId.systemDefault())
                                .format(formatter),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
