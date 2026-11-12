package io.github.drahmedabdulrahman10_alt.nabda

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.drahmedabdulrahman10_alt.nabda.data.repository.LOCAL_USER_ID
import io.github.drahmedabdulrahman10_alt.nabda.di.RepositoryModule
import io.github.drahmedabdulrahman10_alt.nabda.ui.components.LocalNotifier
import io.github.drahmedabdulrahman10_alt.nabda.ui.components.Notifier
import io.github.drahmedabdulrahman10_alt.nabda.ui.screens.ContactsScreen
import io.github.drahmedabdulrahman10_alt.nabda.ui.screens.FixedTasksScreen
import io.github.drahmedabdulrahman10_alt.nabda.ui.screens.GoalsScreen
import io.github.drahmedabdulrahman10_alt.nabda.ui.screens.QuotesScreen
import io.github.drahmedabdulrahman10_alt.nabda.ui.screens.SettingsScreen
import io.github.drahmedabdulrahman10_alt.nabda.ui.screens.TodayScreen
import io.github.drahmedabdulrahman10_alt.nabda.ui.screens.WeekDaysScreen
import io.github.drahmedabdulrahman10_alt.nabda.ui.screens.WeeksDashboardScreen
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.NabdaTheme
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.DailyTasksViewModel
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.GoalsViewModel
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.SettingsViewModel
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.UiState
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.WeeksDashboardViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Screens of the app. The first four are the bottom tabs; the rest open from "المزيد". */
enum class NabdaDestination(val titleAr: String, val icon: ImageVector, val parent: NabdaDestination? = null) {
    TODAY("اليوم", Icons.Default.Today),
    CALENDAR("التقويم", Icons.Default.CalendarMonth),
    DAILY("اليومية", Icons.Default.Repeat),
    MORE("المزيد", Icons.Default.MoreHoriz),
    KINSHIP("صلة الرحم", Icons.Default.Favorite, MORE),
    QUOTES("الحِكم والاقتباسات", Icons.Default.FormatQuote, MORE),
    GOALS("الأهداف", Icons.Default.Flag, MORE),
    SETTINGS("الإعدادات", Icons.Default.Settings, MORE)
}

private val bottomTabs = listOf(
    NabdaDestination.TODAY,
    NabdaDestination.CALENDAR,
    NabdaDestination.DAILY,
    NabdaDestination.MORE
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NabdaApp() }
    }
}

@Composable
private inline fun <reified VM : androidx.lifecycle.ViewModel> nabdaViewModel(
    crossinline create: (io.github.drahmedabdulrahman10_alt.nabda.data.repository.NabdaRepository) -> VM
): VM = viewModel(
    factory = viewModelFactory {
        initializer { create(RepositoryModule.provideNabdaRepository(checkNotNull(this[APPLICATION_KEY]))) }
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NabdaApp() {
    val context = LocalContext.current
    val repository = remember { RepositoryModule.provideNabdaRepository(context.applicationContext) }
    val themeMode by remember {
        repository.observeUserSettings(LOCAL_USER_ID).map { it?.themeMode ?: "SYSTEM" }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = "SYSTEM")
    val darkTheme = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    AskForNotificationPermissionOnce()

    var destination by rememberSaveable { mutableStateOf(NabdaDestination.TODAY) }
    var weekIdForDaysView by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notifier = remember { Notifier(snackbarHostState, scope) }

    val dailyViewModel = nabdaViewModel { DailyTasksViewModel(it, LOCAL_USER_ID) }

    fun navigate(target: NabdaDestination) {
        weekIdForDaysView = null
        destination = target
    }

    // Back steps up one level (sub-screen → المزيد → اليوم) and only leaves the app from "اليوم".
    BackHandler(enabled = destination != NabdaDestination.TODAY || weekIdForDaysView != null) {
        if (weekIdForDaysView != null) {
            weekIdForDaysView = null
        } else {
            destination = destination.parent ?: NabdaDestination.TODAY
        }
    }

    NabdaTheme(darkTheme = darkTheme) {
        CompositionLocalProvider(LocalNotifier provides notifier) {
            Scaffold(
                snackbarHost = {
                    // On "اليوم" lift the message above the quick-add bar so it never hides it.
                    SnackbarHost(
                        snackbarHostState,
                        modifier = Modifier.padding(bottom = if (destination == NabdaDestination.TODAY) 76.dp else 0.dp)
                    )
                },
                topBar = {
                    if (destination != NabdaDestination.TODAY) {
                        TopAppBar(
                            title = { Text(if (weekIdForDaysView != null) "أيام الأسبوع" else destination.titleFull()) },
                            navigationIcon = {
                                if (destination.parent != null || weekIdForDaysView != null) {
                                    IconButton(
                                        onClick = {
                                            if (weekIdForDaysView != null) {
                                                weekIdForDaysView = null
                                            } else {
                                                destination = destination.parent ?: NabdaDestination.TODAY
                                            }
                                        }
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                                    }
                                }
                            }
                        )
                    }
                },
                bottomBar = {
                    NavigationBar {
                        for (tab in bottomTabs) {
                            NavigationBarItem(
                                selected = destination == tab || destination.parent == tab,
                                onClick = {
                                    if (tab == NabdaDestination.TODAY && destination == NabdaDestination.TODAY) {
                                        dailyViewModel.selectToday()
                                    }
                                    navigate(tab)
                                },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.titleAr, maxLines = 1) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .imePadding()
                ) {
                    val wide = destination == NabdaDestination.CALENDAR
                    val contentModifier = if (wide) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .fillMaxSize()
                            .widthIn(max = 720.dp)
                    }
                    when (destination) {
                        NabdaDestination.TODAY -> {
                            val dailyState by dailyViewModel.uiState.collectAsStateWithLifecycle()
                            TodayScreen(
                                uiState = dailyState,
                                viewModel = dailyViewModel,
                                onOpenDailyManagement = { navigate(NabdaDestination.DAILY) },
                                onOpenSettings = { navigate(NabdaDestination.SETTINGS) },
                                modifier = contentModifier.statusBarsPadding()
                            )
                        }
                        NabdaDestination.CALENDAR -> CalendarDestination(
                            weekIdForDaysView = weekIdForDaysView,
                            onWeekSelected = { weekIdForDaysView = it },
                            onOpenDay = { dayId ->
                                dailyViewModel.selectDay(dayId)
                                navigate(NabdaDestination.TODAY)
                            },
                            onOpenToday = {
                                dailyViewModel.selectToday()
                                navigate(NabdaDestination.TODAY)
                            },
                            onOpenGoals = { navigate(NabdaDestination.GOALS) },
                            modifier = contentModifier
                        )
                        NabdaDestination.DAILY -> {
                            val dailyState by dailyViewModel.uiState.collectAsStateWithLifecycle()
                            FixedTasksScreen(
                                uiState = dailyState,
                                viewModel = dailyViewModel,
                                modifier = contentModifier
                            )
                        }
                        NabdaDestination.MORE -> MoreScreen(onOpen = ::navigate, modifier = contentModifier)
                        NabdaDestination.KINSHIP -> {
                            val dailyState by dailyViewModel.uiState.collectAsStateWithLifecycle()
                            ContactsScreen(uiState = dailyState, viewModel = dailyViewModel, modifier = contentModifier)
                        }
                        NabdaDestination.QUOTES -> {
                            val dailyState by dailyViewModel.uiState.collectAsStateWithLifecycle()
                            QuotesScreen(uiState = dailyState, viewModel = dailyViewModel, modifier = contentModifier)
                        }
                        NabdaDestination.GOALS -> {
                            val goalsViewModel = nabdaViewModel { GoalsViewModel(it, LOCAL_USER_ID) }
                            val goalsState by goalsViewModel.uiState.collectAsStateWithLifecycle()
                            val banner by goalsViewModel.statusBanner.collectAsStateWithLifecycle()
                            GoalsScreen(
                                uiState = goalsState,
                                statusBanner = banner,
                                onClearStatusBanner = goalsViewModel::clearStatusBanner,
                                viewModel = goalsViewModel,
                                modifier = contentModifier
                            )
                        }
                        NabdaDestination.SETTINGS -> {
                            val settingsViewModel = nabdaViewModel { SettingsViewModel(it, LOCAL_USER_ID) }
                            val settingsState by settingsViewModel.settingsState.collectAsStateWithLifecycle()
                            val message by settingsViewModel.saveMessage.collectAsStateWithLifecycle()
                            SettingsScreen(
                                settingsState = settingsState,
                                saveMessage = message,
                                onClearMessage = settingsViewModel::clearMessage,
                                viewModel = settingsViewModel,
                                modifier = contentModifier
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun NabdaDestination.titleFull(): String =
    if (this == NabdaDestination.DAILY) "المهام اليومية" else titleAr

@Composable
private fun CalendarDestination(
    weekIdForDaysView: String?,
    onWeekSelected: (String) -> Unit,
    onOpenDay: (String) -> Unit,
    onOpenToday: () -> Unit,
    onOpenGoals: () -> Unit,
    modifier: Modifier
) {
    val weeksViewModel = nabdaViewModel { WeeksDashboardViewModel(it, LOCAL_USER_ID) }
    val weeksState by weeksViewModel.uiState.collectAsStateWithLifecycle()
    val isImporting by weeksViewModel.isImportingSchedule.collectAsStateWithLifecycle()
    val importError by weeksViewModel.importError.collectAsStateWithLifecycle()
    val banner by weeksViewModel.statusBannerMessage.collectAsStateWithLifecycle()

    val activeWeek = (weeksState as? UiState.Success)?.data?.weeks?.firstOrNull { it.week.id == weekIdForDaysView }
    if (activeWeek != null) {
        WeekDaysScreen(weekSummary = activeWeek, onSelectDay = onOpenDay, modifier = modifier)
        return
    }
    WeeksDashboardScreen(
        uiState = weeksState,
        onSelectWeek = { onWeekSelected(it.week.id) },
        onJumpToToday = onOpenToday,
        onSaveReflection = weeksViewModel::saveWeekReflection,
        isImportingSchedule = isImporting,
        importError = importError,
        statusBannerMessage = banner,
        onClearImportError = weeksViewModel::clearImportError,
        onClearStatusBanner = weeksViewModel::clearStatusBanner,
        onShowStatusBanner = weeksViewModel::showStatusBanner,
        onImportScheduleJson = { rawJson, onSuccess -> weeksViewModel.importBulkScheduleJson(rawJson) { onSuccess() } },
        onMoveTaskToDate = weeksViewModel::moveTaskToDate,
        onToggleTaskFromCalendar = weeksViewModel::toggleTaskFromCalendar,
        onAddTaskForDate = weeksViewModel::addTaskForDate,
        onNavigateToDate = { date -> weeksViewModel.openOrCreateDayForDate(date, onOpenDay) },
        onOpenGoals = onOpenGoals,
        modifier = modifier
    )
}

@Composable
private fun MoreScreen(onOpen: (NabdaDestination) -> Unit, modifier: Modifier) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        val entries = NabdaDestination.entries.filter { it.parent == NabdaDestination.MORE }
        entries.forEachIndexed { index, entry ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(entry) }
                    .heightIn(min = 60.dp)
                    .padding(horizontal = 4.dp)
            ) {
                Icon(entry.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Text(entry.titleAr, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (index != entries.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/** Asks for the notification permission the first time the app opens (Android 13+). */
@Composable
private fun AskForNotificationPermissionOnce() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("nabda_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("asked_notifications", false)) {
            prefs.edit().putBoolean("asked_notifications", true).apply()
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
