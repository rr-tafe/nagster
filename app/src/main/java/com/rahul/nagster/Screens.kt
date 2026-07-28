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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import java.time.LocalTime
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
    val intervalPresets = remember { listOf(1, 5, 15) }
    var intervalCustom by remember {
        mutableStateOf((existing?.intervalMinutes ?: 5) !in intervalPresets)
    }

    val existingGiveUp = existing?.giveUpAfterMinutes ?: 0
    val giveUpPresets = remember { listOf(0 to "Never", 60 to "1 Hour", 1440 to "1 Day") }
    var giveUpCustom by remember {
        mutableStateOf(existingGiveUp !in giveUpPresets.map { it.first })
    }
    var giveUpPreset by remember {
        mutableStateOf(if (existingGiveUp in giveUpPresets.map { it.first }) existingGiveUp else 0)
    }
    var giveUpD by remember { mutableStateOf(existingGiveUp / (24 * 60)) }
    var giveUpH by remember { mutableStateOf((existingGiveUp % (24 * 60)) / 60) }
    var giveUpM by remember { mutableStateOf(if (existingGiveUp == 0) 0 else existingGiveUp % 60) }

    var snooze by remember { mutableStateOf(existing?.snoozeMinutes ?: 10) }
    var showTimePicker by remember { mutableStateOf(false) }
    var datePickerTarget by remember { mutableStateOf<String?>(null) }

    val intervalTotal = (intervalH * 60 + intervalM).coerceAtLeast(1)
    val giveUpTotal = (giveUpD * 24 * 60 + giveUpH * 60 + giveUpM).coerceAtLeast(5)
    val effectiveGiveUp = if (giveUpCustom) giveUpTotal else giveUpPreset
    val valid = text.isNotBlank() &&
        (mode != MODE_ROUTINE || days.isNotEmpty()) &&
        (mode != MODE_DATES || dates.isNotEmpty())

    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = hour, initialMinute = minute, is24Hour = false
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
                title = { Text(if (nagId == 0L) "New Nag" else "Edit Nag") },
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
                                giveUpAfterMinutes = effectiveGiveUp,
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
                    Text(if (nagId == 0L) "Save & Activate Nag" else "Save Changes")
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
            SectionCard("What & When") {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("What should I nag you about?") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        onClick = { showTimePicker = true },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            LocalTime.of(hour, minute)
                                .format(DateTimeFormatter.ofPattern("hh:mm a"))
                                .uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                    Text(
                        "Tap to adjust start time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SectionCard("Persistence Rules") {
                Text(
                    "Nag interval (repeats until Done)",
                    style = MaterialTheme.typography.bodyMedium,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    intervalPresets.forEach { m ->
                        FilterChip(
                            selected = !intervalCustom && intervalTotal == m,
                            onClick = {
                                intervalCustom = false
                                intervalH = 0
                                intervalM = m
                            },
                            label = { Text("${m}m") },
                        )
                    }
                    FilterChip(
                        selected = intervalCustom,
                        onClick = { intervalCustom = true },
                        label = { Text("Custom…") },
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
                }

                Text("Give up after", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    giveUpPresets.forEach { (m, label) ->
                        FilterChip(
                            selected = !giveUpCustom && giveUpPreset == m,
                            onClick = {
                                giveUpCustom = false
                                giveUpPreset = m
                            },
                            label = { Text(label) },
                        )
                    }
                    FilterChip(
                        selected = giveUpCustom,
                        onClick = { giveUpCustom = true },
                        label = { Text("Custom…") },
                    )
                }
                if (giveUpCustom) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        NumberWheel(0..7, giveUpD, { giveUpD = it })
                        WheelUnit("d")
                        NumberWheel(0..23, giveUpH, { giveUpH = it })
                        WheelUnit("h")
                        NumberWheel(0..59, giveUpM, { giveUpM = it })
                        WheelUnit("min")
                    }
                }

                Text("Snooze length", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5, 10, 15, 30).forEach { m ->
                        FilterChip(
                            selected = snooze == m,
                            onClick = { snooze = m },
                            label = { Text("$m min") },
                        )
                    }
                }

                Text(
                    "↻ Repeats every ${formatMinutes(intervalTotal)} until completed · " +
                        if (effectiveGiveUp == 0) "never gives up"
                        else "gives up after ${formatMinutes(effectiveGiveUp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            SectionCard("Schedule") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        MODE_ROUTINE to "Routine",
                        MODE_DATES to "Pick Dates",
                        MODE_ONCE to "Just Once",
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (1..7).forEach { day ->
                                DayDot(
                                    day = day,
                                    selected = day in days,
                                    onClick = {
                                        days = if (day in days) days - day else days + day
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
                        DateRow(
                            label = "Starts",
                            value = startDate?.let {
                                runCatching { LocalDate.parse(it).format(dateFmt) }.getOrNull()
                            } ?: "Today",
                            onClick = { datePickerTarget = "START" },
                            onClear = if (startDate != null) ({ startDate = null }) else null,
                        )
                        DateRow(
                            label = "Ends",
                            value = endDate?.let {
                                runCatching { LocalDate.parse(it).format(dateFmt) }.getOrNull()
                            } ?: "Never",
                            onClick = { datePickerTarget = "END" },
                            onClear = if (endDate != null) ({ endDate = null }) else null,
                        )
                    }
                    MODE_DATES -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

@Composable
private fun DayDot(day: Int, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                DayOfWeek.of(day)
                    .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    .first().uppercase(),
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun DateRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    onClear: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onClear != null) {
            IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Clear $label",
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
