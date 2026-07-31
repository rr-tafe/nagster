@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class,
)

package com.rahul.nagster

import android.app.Activity
import android.app.AlarmManager
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import kotlinx.coroutines.delay

// Dynamic (wallpaper-derived) colour needs API 31. Below that, fall back to a
// fixed scheme built from the same purple used for the launcher icon and
// feature graphic, so the app still looks intentionally themed rather than
// like a default Compose starter app.
private val FallbackDarkScheme = darkColorScheme(primary = Color(0xFFCFBCFF))
private val FallbackLightScheme = lightColorScheme(primary = Color(0xFF6750A4))

@Composable
fun NagsterTheme(content: @Composable () -> Unit) {
    val data by NagStore.data.collectAsState()
    val dark = when (data.themeMode) {
        THEME_LIGHT -> false
        THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val scheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (dark) FallbackDarkScheme else FallbackLightScheme
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun NagsterApp(startInQuickAdd: Boolean, vm: NagViewModel = viewModel()) {
    val nav = rememberNavController()
    // A short push/pop slide instead of the default cross-fade.
    val slide = tween<androidx.compose.ui.unit.IntOffset>(170)
    NavHost(
        nav,
        startDestination = "list",
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, slide)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, slide)
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, slide)
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, slide)
        },
    ) {
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

private fun formatMillis(millis: Long, use24Hour: Boolean): String {
    val zdt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    val dayPattern = if (zdt.toLocalDate() < LocalDate.now().plusDays(7)) "EEE" else "d MMM"
    val day = zdt.format(DateTimeFormatter.ofPattern(dayPattern, Locale.getDefault()))
    // Reuse formatClock so AM/PM casing matches the rest of the app.
    return "$day ${formatClock(zdt.hour, zdt.minute, use24Hour)}"
}

/** The stored preference, falling back to how the device itself shows the clock. */
@Composable
private fun rememberUse24Hour(stored: Boolean?): Boolean {
    val context = LocalContext.current
    return stored ?: android.text.format.DateFormat.is24HourFormat(context)
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
    val use24Hour = rememberUse24Hour(data.use24Hour)

    fun exactAlarmsAllowed() = Scheduler.canScheduleExactAlarms(context)

    fun batteryExempt() = context.getSystemService(PowerManager::class.java)
        .isIgnoringBatteryOptimizations(context.packageName)

    var exactAlarmsOk by remember { mutableStateOf(exactAlarmsAllowed()) }
    var batteryOk by remember { mutableStateOf(batteryExempt()) }

    // Granting these happens in system Settings, so the only reliable moment to
    // re-check is when we come back to the foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val exactNow = exactAlarmsAllowed()
                // Anything queued while the permission was missing was scheduled
                // inexactly; re-arm it now that we can be precise.
                if (exactNow && !exactAlarmsOk) vm.rescheduleAll()
                exactAlarmsOk = exactNow
                batteryOk = batteryExempt()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val soundPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // The typed two-arg overload needs API 33; below that, the plain
            // getParcelableExtra(name) is deprecated but the only option.
            val picked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data
                    ?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) as? Uri
            }
            vm.setSoundUri(picked?.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nagster") },
                actions = {
                    IconButton(onClick = {
                        soundPicker.launch(
                            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_TYPE,
                                    RingtoneManager.TYPE_NOTIFICATION,
                                )
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Nag sound")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                    data.soundUri?.let(Uri::parse)
                                        ?: Settings.System.DEFAULT_NOTIFICATION_URI,
                                )
                            }
                        )
                    }) {
                        Icon(Icons.Filled.MusicNote, contentDescription = "Nag sound")
                    }
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
                    // Compact label rather than an icon: nothing pictorial
                    // distinguishes 12- from 24-hour time.
                    TextButton(onClick = { vm.setUse24Hour(!use24Hour) }) {
                        Text(
                            if (use24Hour) "24h" else "12h",
                            style = MaterialTheme.typography.labelLarge,
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
                        text = "Battery optimisation may delay nags.",
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
                    use24Hour = use24Hour,
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
    use24Hour: Boolean,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDone: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (nag.emoji.isNotEmpty() || nag.colorArgb != null) {
                NagBadge(emoji = nag.emoji, colorArgb = nag.colorArgb, size = 44.dp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    nag.text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    nag.scheduleSummary(use24Hour),
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
                            "🔔 Nagging now (since ${formatMillis(nag.activeSince, use24Hour)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        // Two taps here too, so completing a nag is deliberate
                        // wherever you do it. Disarms itself after a few seconds.
                        var armed by remember(nag.id) { mutableStateOf(false) }
                        LaunchedEffect(armed) {
                            if (armed) {
                                delay(4_000)
                                armed = false
                            }
                        }
                        TextButton(
                            onClick = {
                                if (armed) {
                                    armed = false
                                    onDone()
                                } else {
                                    armed = true
                                }
                            }
                        ) {
                            Text(if (armed) "Tap again to confirm" else "Mark done")
                        }
                    }
                    else -> {
                        val next = nag.nextStartMillis()
                        if (next != null || !nag.willNeverFire()) {
                            Text(
                                if (next != null) "Next: ${formatMillis(next, use24Hour)}" else "Due now",
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

/**
 * Softens whichever edges still have content beyond them, so a row of pills
 * reads as scrollable rather than as a complete set.
 */
private fun Modifier.scrollHints(state: LazyListState, fadeWidth: Float): Modifier =
    this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            if (state.canScrollBackward) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startX = 0f,
                        endX = fadeWidth,
                    ),
                    topLeft = Offset.Zero,
                    size = Size(fadeWidth, size.height),
                    blendMode = BlendMode.DstIn,
                )
            }
            if (state.canScrollForward) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startX = size.width - fadeWidth,
                        endX = size.width,
                    ),
                    topLeft = Offset(size.width - fadeWidth, 0f),
                    size = Size(fadeWidth, size.height),
                    blendMode = BlendMode.DstIn,
                )
            }
        }

@Composable
private fun PresetCarousel(
    presets: List<Pair<Int, String>>,
    selected: Int?, // null = Custom
    onSelect: (Int) -> Unit,
    onCustom: () -> Unit,
) {
    val state = rememberLazyListState()
    val fadeWidth = with(LocalDensity.current) { 28.dp.toPx() }
    val moreToTheRight by remember { derivedStateOf { state.canScrollForward } }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        LazyRow(
            state = state,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .scrollHints(state, fadeWidth),
        ) {
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
        if (moreToTheRight) {
            Text(
                "swipe for more ›",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val use24Hour = rememberUse24Hour(data.use24Hour)

    var text by remember { mutableStateOf(existing?.text ?: "") }
    var emoji by remember { mutableStateOf(existing?.emoji ?: "") }
    var colorArgb by remember { mutableStateOf(existing?.colorArgb) }
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

    var timePickerTarget by remember { mutableStateOf<String?>(null) }
    var datePickerTarget by remember { mutableStateOf<String?>(null) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    if (showEmojiPicker) {
        EmojiPickerSheet(
            onPicked = {
                emoji = it
                showEmojiPicker = false
            },
            onClear = {
                emoji = ""
                showEmojiPicker = false
            },
            onDismiss = { showEmojiPicker = false },
        )
    }

    val intervalTotal = (intervalH * 60 + intervalM).coerceAtLeast(1)
    val giveUpTotal = (giveUpD * 24 * 60 + giveUpH * 60 + giveUpM).coerceAtLeast(5)
    val effectiveGiveUp = if (giveUpCustom) giveUpTotal else giveUpPreset
    val valid = text.isNotBlank() &&
        (mode != MODE_ROUTINE || days.isNotEmpty()) &&
        (mode != MODE_DATES || dates.isNotEmpty())

    var wontFireWarning by remember { mutableStateOf(false) }

    fun buildNag(): Nag = (existing ?: Nag()).copy(
        text = text.trim(),
        emoji = emoji,
        colorArgb = colorArgb,
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
            is24Hour = use24Hour,
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EmojiCircle(
                        emoji = emoji,
                        colorArgb = colorArgb,
                        onClick = { showEmojiPicker = true },
                    )
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("e.g. Take medication after dinner") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }

                ExpandableRow(
                    label = "Colour",
                    summary = { ColorSwatch(color = colorArgb, selected = false, onClick = null) },
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ColorSwatch(
                            color = null,
                            selected = colorArgb == null,
                            onClick = { colorArgb = null },
                        )
                        NAG_COLORS.forEach { swatch ->
                            ColorSwatch(
                                color = swatch,
                                selected = colorArgb == swatch,
                                onClick = { colorArgb = swatch },
                            )
                        }
                    }
                }
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
                            timeValue = formatClock(hour, minute, use24Hour),
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
                                formatClock(endHour ?: 23, endMinute ?: 59, use24Hour)
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
                            timeValue = formatClock(hour, minute, use24Hour),
                            onClick = { timePickerTarget = "MAIN" },
                        )
                    }
                    else -> {
                        TimeRow(
                            label = "At",
                            timeValue = formatClock(hour, minute, use24Hour),
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

/**
 * Emoji glyphs carry their own font padding, which throws off naive centring —
 * disabling it and pinning line height to the glyph size keeps them dead centre.
 */
@Composable
private fun CenteredEmoji(emoji: String, sizeDp: Float) {
    Text(
        emoji,
        style = LocalTextStyle.current.copy(
            fontSize = sizeDp.sp,
            lineHeight = sizeDp.sp,
            textAlign = TextAlign.Center,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both,
            ),
        ),
    )
}

/** The nag's emoji on a coloured circle — the same badge the notification draws. */
@Composable
private fun NagBadge(emoji: String, colorArgb: Int?, size: androidx.compose.ui.unit.Dp) {
    Surface(
        shape = CircleShape,
        color = Color(colorArgb ?: NAG_BADGE_NEUTRAL),
        modifier = Modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (emoji.isNotEmpty()) CenteredEmoji(emoji, size.value * 0.5f)
        }
    }
}

/** Tappable circle standing in for the emoji field; opens the picker sheet. */
@Composable
private fun EmojiCircle(emoji: String, colorArgb: Int?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (emoji.isEmpty() && colorArgb == null) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            Color(colorArgb ?: NAG_BADGE_NEUTRAL)
        },
        modifier = Modifier.size(56.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (emoji.isNotEmpty()) {
                CenteredEmoji(emoji, 28f)
            } else {
                Icon(
                    Icons.Filled.EmojiEmotions,
                    contentDescription = "Choose an emoji",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The full AndroidX emoji catalogue, since the IME can't be forced into emoji mode.
 *
 * Deliberately a Dialog rather than a ModalBottomSheet: the sheet claims vertical
 * drags for its own drag-to-dismiss, which starves the picker's internal
 * RecyclerView and leaves the grid unscrollable while tab taps still work.
 */
@Composable
private fun EmojiPickerSheet(
    onPicked: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.94f),
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Choose an emoji",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onClear) { Text("Remove") }
                }
                AndroidView(
                    factory = { ctx ->
                        EmojiPickerView(ctx).apply {
                            emojiGridColumns = 9
                            setOnEmojiPickedListener { picked -> onPicked(picked.emoji) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(400.dp),
                )
            }
        }
    }
}

/** Collapsed-by-default section with a chevron that rotates when opened. */
@Composable
private fun ExpandableRow(
    label: String,
    summary: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, label = "chevron")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            summary()
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(rotation),
            )
        }
        AnimatedVisibility(expanded) { content() }
    }
}

@Composable
private fun ColorSwatch(color: Int?, selected: Boolean, onClick: (() -> Unit)?) {
    val ring = if (selected) 3.dp else 0.dp
    val shared: @Composable () -> Unit = {
        // The "no colour" swatch is the only one that needs a glyph.
        if (color == null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "No colour",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (onClick == null) {
        Surface(
            shape = CircleShape,
            color = color?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(28.dp),
        ) { shared() }
        return
    }
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = color?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) {
            BorderStroke(ring, MaterialTheme.colorScheme.onSurface)
        } else null,
        modifier = Modifier.size(40.dp),
    ) { shared() }
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
    val use24Hour = rememberUse24Hour(data.use24Hour)
    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()) }

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
                                    .let {
                                        "${it.format(dayFormatter)} · " +
                                            formatClock(it.hour, it.minute, use24Hour)
                                    },
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
