@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.rahul.nagster

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun NagsterTheme(content: @Composable () -> Unit) {
    val data by NagStore.data.collectAsState()
    val dark = when (data.themeMode) {
        THEME_LIGHT -> false
        THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val scheme = if (dark) {
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
    val zone = ZoneId.systemDefault()
    val zdt = Instant.ofEpochMilli(millis).atZone(zone)
    val pattern = if (zdt.toLocalDate() < LocalDate.now().plusDays(7)) "EEE HH:mm" else "d MMM HH:mm"
    return zdt.format(DateTimeFormatter.ofPattern(pattern))
}

// ---------------------------------------------------------------------------
// List screen
// ---------------------------------------------------------------------------

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
                    IconButton(onClick = {
                        vm.setThemeMode(
                            when (data.themeMode) {
                                THEME_SYSTEM -> THEME_LIGHT
                                THEME_LIGHT -> THEME_DARK
                                else -> THEME_SYSTEM
                            }
                        )
                    }) {
                        Icon(
                            when (data.themeMode) {
                                THEME_LIGHT -> Icons.Filled.LightMode
                                THEME_DARK -> Icons.Filled.DarkMode
                                else -> Icons.Filled.BrightnessAuto
                            },
                            contentDescription = "Theme: ${data.themeMode.lowercase()}",
                        )
                    }
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
            contentPadding = PaddingValues(16.dp),
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
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.buymeacoffee.com/fanciful.unicorn"),
                            )
                        )
                    }) {
                        Text(
                            "☕ Enjoying Nagster? Buy me a coffee",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
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
                        TextButton(onClick = onDone) { Text("Mark done") }
                    }
                    else -> {
                        val next = nag.nextStartMillis()
                        Text(
                            if (next != null) "Next: ${formatMillis(next)}" else "No upcoming time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Switch(checked = nag.enabled, onCheckedChange = onToggle)
        }
    }
}

// ---------------------------------------------------------------------------
// Wheel roller
// ---------------------------------------------------------------------------

@Composable
private fun NumberWheel(
    range: IntRange,
    value: Int,
    onValueChange: (Int) -> Unit,
    enabled: Boolean = true,
) {
    val values = remember(range) { range.toList() }
    val itemHeight = 36.dp
    val initialIndex = remember { values.indexOf(value).coerceAtLeast(0) }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    val centered by remember { derivedStateOf { state.firstVisibleItemIndex } }

    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) {
                onValueChange(values[state.firstVisibleItemIndex.coerceIn(0, values.lastIndex)])
            }
        }
    }

    Box(
        modifier = Modifier
            .height(itemHeight * 3)
            .width(56.dp)
            .alpha(if (enabled) 1f else 0.35f),
    ) {
        LazyColumn(
            state = state,
            flingBehavior = fling,
            userScrollEnabled = enabled,
            contentPadding = PaddingValues(vertical = itemHeight),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(values, key = { it }) { v ->
                Box(
                    modifier = Modifier.height(itemHeight).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    val isCentered = values.getOrNull(centered) == v
                    Text(
                        "$v",
                        style = if (isCentered) MaterialTheme.typography.titleLarge
                        else MaterialTheme.typography.bodyLarge,
                        color = if (isCentered) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun WheelUnit(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ---------------------------------------------------------------------------
// Edit screen
// ---------------------------------------------------------------------------

@Composable
fun EditScreen(vm: NagViewModel, nagId: Long, onClose: () -> Unit) {
    val data by vm.data.collectAsState()
    val existing = remember(nagId) { data.nags.find { it.id == nagId } }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE d MMM") }

    var text by remember { mutableStateOf(existing?.text ?: "") }
    var hour by remember { mutableStateOf(existing?.hour ?: 9) }
    var minute by remember { mutableStateOf(existing?.minute ?: 0) }
    var mode by remember { mutableStateOf(existing?.effectiveMode ?: MODE_ROUTINE) }
    var days by remember { mutableStateOf(existing?.daysOfWeek?.ifEmpty { (1..7).toSet() } ?: (1..7).toSet()) }
    var dates by remember { mutableStateOf(existing?.dates?.toSet() ?: emptySet()) }
    var startDate by remember { mutableStateOf(existing?.startDate) }
    var endDate by remember { mutableStateOf(existing?.endDate) }
    var intervalH by remember { mutableStateOf((existing?.intervalMinutes ?: 5) / 60) }
    var intervalM by remember { mutableStateOf((existing?.intervalMinutes ?: 5) % 60) }
    val intervalPresets = remember { listOf(1, 5, 15, 30) }
    var intervalCustom by remember {
        mutableStateOf((existing?.intervalMinutes ?: 5) !in intervalPresets)
    }
    val existingGiveUp = existing?.giveUpAfterMinutes ?: 0
    var giveUpNever by remember { mutableStateOf(existingGiveUp == 0) }
    var giveUpD by remember { mutableStateOf(existingGiveUp / (24 * 60)) }
    var giveUpH by remember { mutableStateOf((existingGiveUp % (24 * 60)) / 60) }
    var giveUpM by remember { mutableStateOf(if (existingGiveUp == 0) 0 else existingGiveUp % 60) }
    var snooze by remember { mutableStateOf(existing?.snoozeMinutes ?: 10) }
    var showTimePicker by remember { mutableStateOf(false) }
    var datePickerTarget by remember { mutableStateOf<String?>(null) }

    val intervalTotal = (intervalH * 60 + intervalM).coerceAtLeast(1)
    val giveUpTotal = (giveUpD * 24 * 60 + giveUpH * 60 + giveUpM).coerceAtLeast(5)
    val valid = text.isNotBlank() &&
        (mode != MODE_ROUTINE || days.isNotEmpty()) &&
        (mode != MODE_DATES || dates.isNotEmpty())

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

    if (datePickerTarget != null) {
        val initial = when (datePickerTarget) {
            "START" -> startDate
            "END" -> endDate
            else -> null
        }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val dpState = rememberDatePickerState(
            initialSelectedDateMillis = (initial ?: LocalDate.now())
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { datePickerTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    val ms = dpState.selectedDateMillis
                    if (ms != null) {
                        val iso = Instant.ofEpochMilli(ms)
                            .atZone(ZoneOffset.UTC).toLocalDate().toString()
                        when (datePickerTarget) {
                            "ADD" -> dates = dates + iso
                            "START" -> startDate = iso
                            "END" -> endDate = iso
                        }
                    }
                    datePickerTarget = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { datePickerTarget = null }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = dpState)
        }
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
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        val base = existing ?: Nag()
                        vm.save(
                            base.copy(
                                text = text.trim(),
                                hour = hour,
                                minute = minute,
                                mode = mode,
                                daysOfWeek = days,
                                dates = dates.sorted(),
                                startDate = startDate,
                                endDate = endDate,
                                intervalMinutes = intervalTotal,
                                giveUpAfterMinutes = if (giveUpNever) 0 else giveUpTotal,
                                snoozeMinutes = snooze,
                                enabled = true,
                            )
                        ) { onClose() }
                    },
                    enabled = valid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(if (nagId == 0L) "Save & activate nag" else "Save changes")
                }
            }
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

            val previewNag = Nag(
                text = text,
                hour = hour,
                minute = minute,
                mode = mode,
                daysOfWeek = days,
                dates = dates.sorted(),
                startDate = startDate,
                endDate = endDate,
                intervalMinutes = intervalTotal,
            )
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "🔔 ${text.ifBlank { "Your nag" }}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        buildString {
                            append(previewNag.scheduleSummary())
                            append(" — keeps nagging until you press DONE. ")
                            append(
                                if (giveUpNever) "Never gives up."
                                else "Gives up after ${formatMinutes(giveUpTotal)}."
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary,
                            ) {
                                Text(
                                    "DONE",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp, vertical = 4.dp
                                    ),
                                )
                            }
                            Text(
                                "Completes it — quiet until the next scheduled time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surface,
                            ) {
                                Text(
                                    "SNOOZE ${snooze}m",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp, vertical = 4.dp
                                    ),
                                )
                            }
                            Text(
                                "Pauses it for $snooze min — still not done",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            SectionLabel("Nag time")
            Card(modifier = Modifier.clickable { showTimePicker = true }) {
                Text(
                    "%02d:%02d".format(hour, minute),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }

            SectionLabel("Repeat on")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(
                    MODE_ROUTINE to "Routine",
                    MODE_DATES to "Pick dates",
                    MODE_ONCE to "Just once",
                ).forEachIndexed { i, (m, label) ->
                    SegmentedButton(
                        selected = mode == m,
                        onClick = { mode = m },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = 3),
                    ) { Text(label) }
                }
            }

            when (mode) {
                MODE_ROUTINE -> {
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
                            "Select at least one day.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DateField(
                            label = "Starts on",
                            value = startDate?.let {
                                runCatching { LocalDate.parse(it).format(dateFmt) }.getOrNull()
                            } ?: "Today",
                            onClick = { datePickerTarget = "START" },
                            onClear = if (startDate != null) ({ startDate = null }) else null,
                        )
                        DateField(
                            label = "Ends on",
                            value = endDate?.let {
                                runCatching { LocalDate.parse(it).format(dateFmt) }.getOrNull()
                            } ?: "Never",
                            onClick = { datePickerTarget = "END" },
                            onClear = if (endDate != null) ({ endDate = null }) else null,
                        )
                    }
                }
                MODE_DATES -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        dates.sorted().forEach { d ->
                            InputChip(
                                selected = false,
                                onClick = { dates = dates - d },
                                label = {
                                    Text(
                                        runCatching {
                                            LocalDate.parse(d).format(dateFmt)
                                        }.getOrDefault(d)
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                            )
                        }
                        AssistChip(
                            onClick = { datePickerTarget = "ADD" },
                            label = { Text("+ Add date") },
                        )
                    }
                    if (dates.isEmpty()) {
                        Text(
                            "Add at least one date.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                else -> {
                    Text(
                        "Fires at the next occurrence of the nag time, then turns itself off.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SectionLabel("Nag every — ${formatMinutes(intervalTotal)}")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                intervalPresets.forEach { m ->
                    FilterChip(
                        selected = !intervalCustom && intervalTotal == m,
                        onClick = {
                            intervalCustom = false
                            intervalH = 0
                            intervalM = m
                        },
                        label = { Text("$m min") },
                    )
                }
                FilterChip(
                    selected = intervalCustom,
                    onClick = { intervalCustom = true },
                    label = { Text("Custom") },
                )
            }
            if (intervalCustom) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    NumberWheel(0..23, intervalH, { intervalH = it })
                    WheelUnit("h")
                    NumberWheel(0..59, intervalM, { intervalM = it })
                    WheelUnit("min")
                }
                if (intervalH == 0 && intervalM == 0) {
                    Text(
                        "Minimum interval is 1 minute — saving will use 1 min.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SectionLabel(
                if (giveUpNever) "Give up after — never"
                else "Give up after — ${formatMinutes(giveUpTotal)}"
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                NumberWheel(0..7, giveUpD, { giveUpD = it }, enabled = !giveUpNever)
                WheelUnit("d")
                NumberWheel(0..23, giveUpH, { giveUpH = it }, enabled = !giveUpNever)
                WheelUnit("h")
                NumberWheel(0..59, giveUpM, { giveUpM = it }, enabled = !giveUpNever)
                WheelUnit("min")
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Switch(checked = giveUpNever, onCheckedChange = { giveUpNever = it })
                    Text(
                        "Never",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

        }
    }
}

@Composable
private fun DateField(
    label: String,
    value: String,
    onClick: () -> Unit,
    onClear: (() -> Unit)?,
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Card(modifier = Modifier.clickable(onClick = onClick)) {
                Text(
                    value,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            if (onClear != null) {
                IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Clear $label",
                        modifier = Modifier.size(16.dp),
                    )
                }
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

// ---------------------------------------------------------------------------
// History screen
// ---------------------------------------------------------------------------

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
            contentPadding = PaddingValues(16.dp),
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
