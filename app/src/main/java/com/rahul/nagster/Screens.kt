@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class,
)

package com.rahul.nagster

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
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
import kotlin.math.abs

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

private fun formatTime12(hour: Int, minute: Int): String =
    LocalTime.of(hour, minute)
        .format(DateTimeFormatter.ofPattern("hh:mm a"))
        .uppercase()

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
                        if (next != null || !nag.willNeverFire()) {
                            Text(
                                if (next != null) "Next: ${formatMillis(next)}" else "Due now",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.errorContainer,
                            ) {
                                Text(
                                    "⚠ Won't fire — tap to fix the time or end date",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(
                                        horizontal = 8.dp, vertical = 4.dp
                                    ),
                                )
                            }
                        }
                    }
                }
            }
            Switch(checked = nag.enabled, onCheckedChange = onToggle)
        }
    }
}

// ---------------------------------------------------------------------------
// iOS-style wheel roller
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
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    val initialIndex = remember { values.indexOf(value).coerceAtLeast(0) }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    val view = LocalView.current

    // Tick haptic as values roll past the center, like a real dial.
    LaunchedEffect(state) {
        var last = state.firstVisibleItemIndex
        snapshotFlow { state.firstVisibleItemIndex }.collect { idx ->
            if (idx != last) {
                last = idx
                if (state.isScrollInProgress) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
            }
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) {
                onValueChange(values[state.firstVisibleItemIndex.coerceIn(0, values.lastIndex)])
            }
        }
    }

    Box(
        modifier = Modifier
            .height(itemHeight * 5)
            .width(64.dp)
            .alpha(if (enabled) 1f else 0.35f),
    ) {
        Column(modifier = Modifier.align(Alignment.Center).fillMaxWidth()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(itemHeight))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        LazyColumn(
            state = state,
            flingBehavior = fling,
            userScrollEnabled = enabled,
            contentPadding = PaddingValues(vertical = itemHeight * 2),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(values) { i, v ->
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .graphicsLayer {
                            val center = state.firstVisibleItemIndex +
                                state.firstVisibleItemScrollOffset / itemHeightPx
                            val dist = (i - center).coerceIn(-2.5f, 2.5f)
                            rotationX = -dist * 22f
                            val scale = 1f - 0.07f * abs(dist)
                            scaleX = scale
                            scaleY = scale
                            alpha = (1f - 0.3f * abs(dist)).coerceAtLeast(0.15f)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$v", style = MaterialTheme.typography.titleMedium)
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
// Preset carousel
// ---------------------------------------------------------------------------

@Composable
private fun PresetCarousel(
    presets: List<Pair<Int, String>>,
    selected: Int?, // null = Custom
    onSelect: (Int) -> Unit,
    onCustom: () -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(presets) { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) },
            )
        }
        item {
            FilterChip(
                selected = selected == null,
                onClick = onCustom,
                label = { Text("Custom…") },
            )
        }
    }
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
    var endHour by remember { mutableStateOf(existing?.endHour) }
    var endMinute by remember { mutableStateOf(existing?.endMinute) }

    val minutePresets = remember { listOf(1, 5, 10, 15, 20, 30, 60) }
    var intervalH by remember { mutableStateOf((existing?.intervalMinutes ?: 5) / 60) }
    var intervalM by remember { mutableStateOf((existing?.intervalMinutes ?: 5) % 60) }
    var intervalCustom by remember {
        mutableStateOf((existing?.intervalMinutes ?: 5) !in minutePresets)
    }

    val existingGiveUp = existing?.giveUpAfterMinutes ?: 0
    val giveUpPresetValues = remember { listOf(0) + minutePresets }
    var giveUpCustom by remember { mutableStateOf(existingGiveUp !in giveUpPresetValues) }
    var giveUpPreset by remember {
        mutableStateOf(if (existingGiveUp in giveUpPresetValues) existingGiveUp else 0)
    }
    var giveUpD by remember { mutableStateOf(existingGiveUp / (24 * 60)) }
    var giveUpH by remember { mutableStateOf((existingGiveUp % (24 * 60)) / 60) }
    var giveUpM by remember { mutableStateOf(if (existingGiveUp == 0) 0 else existingGiveUp % 60) }

    var snooze by remember { mutableStateOf(existing?.snoozeMinutes ?: 10) }
    var timePickerTarget by remember { mutableStateOf<String?>(null) }
    var datePickerTarget by remember { mutableStateOf<String?>(null) }

    val intervalTotal = (intervalH * 60 + intervalM).coerceAtLeast(1)
    val giveUpTotal = (giveUpD * 24 * 60 + giveUpH * 60 + giveUpM).coerceAtLeast(5)
    val effectiveGiveUp = if (giveUpCustom) giveUpTotal else giveUpPreset
    val valid = text.isNotBlank() &&
        (mode != MODE_ROUTINE || days.isNotEmpty()) &&
        (mode != MODE_DATES || dates.isNotEmpty())

    var wontFireWarning by remember { mutableStateOf(false) }

    fun buildNag(): Nag = (existing ?: Nag()).copy(
        text = text.trim(),
        hour = hour,
        minute = minute,
        mode = mode,
        daysOfWeek = days,
        dates = dates.sorted(),
        startDate = startDate,
        endDate = endDate,
        endHour = if (endDate != null) endHour else null,
        endMinute = if (endDate != null) endMinute else null,
        intervalMinutes = intervalTotal,
        giveUpAfterMinutes = effectiveGiveUp,
        snoozeMinutes = snooze,
        enabled = true,
    )

    fun persist() = vm.save(buildNag()) { onClose() }

    if (wontFireWarning) {
        AlertDialog(
            onDismissRequest = { wontFireWarning = false },
            title = { Text("This nag will never fire") },
            text = {
                Text(
                    "Its next scheduled time falls outside the dates you set, so no " +
                        "notification will ever be sent. Check the nag time and the " +
                        "Ends date before saving."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    wontFireWarning = false
                }) { Text("Go back and fix") }
            },
            dismissButton = {
                TextButton(onClick = {
                    wontFireWarning = false
                    persist()
                }) { Text("Save anyway") }
            },
        )
    }

    if (timePickerTarget != null) {
        val isEnd = timePickerTarget == "END"
        val state = rememberTimePickerState(
            initialHour = if (isEnd) endHour ?: 23 else hour,
            initialMinute = if (isEnd) endMinute ?: 59 else minute,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { timePickerTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    if (isEnd) {
                        endHour = state.hour
                        endMinute = state.minute
                    } else {
                        hour = state.hour
                        minute = state.minute
                    }
                    timePickerTarget = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { timePickerTarget = null }) { Text("Cancel") }
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
                            "END" -> {
                                endDate = iso
                                if (endHour == null) {
                                    endHour = 23
                                    endMinute = 59
                                }
                            }
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!valid) return@FloatingActionButton
                    if (buildNag().willNeverFire()) wontFireWarning = true else persist()
                },
                containerColor = if (valid) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (valid) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Save & activate nag")
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
            SectionCard("Nag about what?") {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("e.g. Take medication after dinner") },
                    modifier = Modifier.fillMaxWidth(),
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
                        DateTimeRow(
                            label = "Starts",
                            dateValue = startDate?.let {
                                runCatching { LocalDate.parse(it).format(dateFmt) }.getOrNull()
                            } ?: "Today",
                            timeValue = formatTime12(hour, minute),
                            onDateClick = { datePickerTarget = "START" },
                            onTimeClick = { timePickerTarget = "MAIN" },
                            onClear = if (startDate != null) ({ startDate = null }) else null,
                        )
                        DateTimeRow(
                            label = "Ends",
                            dateValue = endDate?.let {
                                runCatching { LocalDate.parse(it).format(dateFmt) }.getOrNull()
                            } ?: "Never",
                            timeValue = if (endDate != null) {
                                formatTime12(endHour ?: 23, endMinute ?: 59)
                            } else null,
                            onDateClick = { datePickerTarget = "END" },
                            onTimeClick = { timePickerTarget = "END" },
                            onClear = if (endDate != null) ({
                                endDate = null
                                endHour = null
                                endMinute = null
                            }) else null,
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
                        TimeRow(
                            label = "At",
                            timeValue = formatTime12(hour, minute),
                            onClick = { timePickerTarget = "MAIN" },
                        )
                    }
                    else -> {
                        TimeRow(
                            label = "At",
                            timeValue = formatTime12(hour, minute),
                            onClick = { timePickerTarget = "MAIN" },
                        )
                        Text(
                            "Fires at the next occurrence of this time, then turns itself off.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            SectionCard("Persistence Rules") {
                Text(
                    "Nag interval (repeats until Done)",
                    style = MaterialTheme.typography.bodyMedium,
                )
                PresetCarousel(
                    presets = minutePresets.map { it to "${it}m" },
                    selected = if (intervalCustom) null else intervalTotal,
                    onSelect = {
                        intervalCustom = false
                        intervalH = 0
                        intervalM = it
                    },
                    onCustom = { intervalCustom = true },
                )
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
                PresetCarousel(
                    presets = listOf(0 to "Never") + minutePresets.map { it to "${it}m" },
                    selected = if (giveUpCustom) null else giveUpPreset,
                    onSelect = {
                        giveUpCustom = false
                        giveUpPreset = it
                    },
                    onCustom = { giveUpCustom = true },
                )
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

            Spacer(Modifier.height(56.dp))
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
private fun TimeChip(value: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun DateTimeRow(
    label: String,
    dateValue: String,
    timeValue: String?,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    onClear: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            dateValue,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable(onClick = onDateClick),
        )
        if (timeValue != null) {
            TimeChip(timeValue, onTimeClick)
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
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable(onClick = onDateClick),
        )
    }
}

@Composable
private fun TimeRow(label: String, timeValue: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        TimeChip(timeValue, onClick)
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

    var selecting by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf(setOf<Long>()) }
    var confirmDelete by remember { mutableStateOf<String?>(null) }

    fun exitSelection() {
        selecting = false
        selection = emptySet()
    }

    if (confirmDelete != null) {
        val deleteAll = confirmDelete == "ALL"
        val count = if (deleteAll) events.size else selection.size
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(if (deleteAll) "Delete all history?" else "Delete selected?") },
            text = {
                Text(
                    "This will permanently delete $count " +
                        (if (count == 1) "entry." else "entries.")
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (deleteAll) vm.clearEvents() else vm.deleteEvents(selection)
                    exitSelection()
                    confirmDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (selecting) "${selection.size} selected" else "History")
                },
                navigationIcon = {
                    IconButton(onClick = { if (selecting) exitSelection() else onClose() }) {
                        if (selecting) {
                            Icon(Icons.Filled.Close, contentDescription = "Exit selection")
                        } else {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (selecting) {
                        IconButton(onClick = {
                            selection = events.map { it.timestamp }.toSet()
                        }) {
                            Icon(Icons.Filled.SelectAll, contentDescription = "Select all")
                        }
                        IconButton(
                            onClick = {
                                if (selection.isNotEmpty()) confirmDelete = "SELECTED"
                            },
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete selected")
                        }
                    } else if (events.isNotEmpty()) {
                        IconButton(onClick = { selecting = true }) {
                            Icon(Icons.Filled.Checklist, contentDescription = "Select entries")
                        }
                        IconButton(onClick = { confirmDelete = "ALL" }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Delete all")
                        }
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
            items(events, key = { it.timestamp }) { event ->
                val isSelected = event.timestamp in selection
                fun toggle() {
                    selection = if (isSelected) selection - event.timestamp
                    else selection + event.timestamp
                }
                Card(
                    colors = if (isSelected) {
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        )
                    } else {
                        CardDefaults.cardColors()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { if (selecting) toggle() },
                            onLongClick = {
                                selecting = true
                                selection = selection + event.timestamp
                            },
                        ),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(event.text, style = MaterialTheme.typography.titleSmall)
                            Text(
                                Instant.ofEpochMilli(event.timestamp)
                                    .atZone(ZoneId.systemDefault())
                                    .format(formatter),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (selecting) {
                            Checkbox(checked = isSelected, onCheckedChange = { toggle() })
                        }
                    }
                }
            }
        }
    }
}
