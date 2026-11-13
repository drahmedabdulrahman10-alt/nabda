package io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.drahmedabdulrahman10_alt.nabda.data.ai.BulkScheduleJsonImporter
import io.github.drahmedabdulrahman10_alt.nabda.data.ai.ScheduleImportSummary
import io.github.drahmedabdulrahman10_alt.nabda.data.model.DailyTask
import io.github.drahmedabdulrahman10_alt.nabda.data.model.Day
import io.github.drahmedabdulrahman10_alt.nabda.data.model.FixedTask
import io.github.drahmedabdulrahman10_alt.nabda.data.model.FixedTaskCompletion
import io.github.drahmedabdulrahman10_alt.nabda.data.model.Goal
import io.github.drahmedabdulrahman10_alt.nabda.data.model.GoalTimeframe
import io.github.drahmedabdulrahman10_alt.nabda.data.model.TaskPriority
import io.github.drahmedabdulrahman10_alt.nabda.data.model.Week
import io.github.drahmedabdulrahman10_alt.nabda.data.repository.NabdaRepository
import io.github.drahmedabdulrahman10_alt.nabda.util.ArabicCalendarUtils
import java.util.Date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WeekSummaryUi(
    val week: Week,
    val isCurrentWeek: Boolean,
    val days: List<DaySummaryUi>,
    val totalItems: Int,
    val completedItems: Int,
    val progressFraction: Float,
    val progressPercent: Int
)

data class DaySummaryUi(
    val day: Day,
    val isToday: Boolean,
    val dayNameAr: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val completedHabits: Int,
    val totalActiveHabits: Int,
    val progressFraction: Float,
    val progressPercent: Int
)

data class WeeksDashboardData(
    val weeks: List<WeekSummaryUi> = emptyList(),
    val tasksByDateKey: Map<String, List<DailyTask>> = emptyMap(),
    val activeGoals: List<Goal> = emptyList(),
    val todayDayId: String = "",
    val currentWeekId: String = "",
    val isRefreshing: Boolean = false
)

class WeeksDashboardViewModel(
    private val repository: NabdaRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isImportingSchedule = MutableStateFlow(false)
    val isImportingSchedule: StateFlow<Boolean> = _isImportingSchedule.asStateFlow()

    private val _importError = MutableStateFlow(null as String?)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow(null as String?)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    val uiState: StateFlow<UiState<WeeksDashboardData>> = combine(
        repository.observeWeeks(currentUserId),
        repository.observeDays(currentUserId),
        repository.observeTasks(currentUserId),
        repository.observeFixedTasks(currentUserId),
        repository.observeFixedTaskCompletions(currentUserId)
    ) { weeks, days, tasks, fixedTasks, fixedCompletions ->
        buildDashboardData(weeks, days, tasks, fixedTasks, fixedCompletions)
    }
        .combine(repository.observeGoals(currentUserId)) { data, goals ->
            // Prioritize active Monthly & Quarterly/Module goals (and Yearly if none)
            val monthlyOrQuarterly = goals.filter {
                !it.effectiveIsCompleted() &&
                    (it.timeframeEnum == GoalTimeframe.MONTHLY || it.timeframeEnum == GoalTimeframe.QUARTERLY)
            }
            val compassGoals = if (monthlyOrQuarterly.isNotEmpty()) {
                monthlyOrQuarterly
            } else {
                goals.filter { !it.effectiveIsCompleted() }
            }
            data.copy(activeGoals = compassGoals)
        }
        .combine(_isRefreshing) { data, refreshing ->
            UiState.Success(data.copy(isRefreshing = refreshing)) as UiState<WeeksDashboardData>
        }
        .catch { e ->
            Log.w(TAG, "Error observing weeks dashboard", e)
            emit(UiState.Error(e.localizedMessage ?: "تعذّر تحميل الأسابيع"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    fun clearImportError() {
        _importError.value = null
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    fun showStatusBanner(message: String) {
        _statusBannerMessage.value = message
    }

    fun refreshAndSeed() {
        repository.refreshToday()
    }

    fun saveWeekReflection(weekId: String, reflection: String) {
        viewModelScope.launch {
            try {
                repository.updateWeekReflection(weekId, reflection)
            } catch (e: Exception) {
                Log.w(TAG, "Failed saving reflection", e)
            }
        }
    }

    fun moveTaskToDate(task: DailyTask, targetDate: Date) {
        viewModelScope.launch {
            val formattedDate = ArabicCalendarUtils.formatGregorianArabic(targetDate)
            val result = repository.moveTaskToDate(task, targetDate)
            result.fold(
                onSuccess = {
                    _statusBannerMessage.value = "تم نقل المهمة إلى $formattedDate"
                },
                onFailure = { err ->
                    _statusBannerMessage.value =
                        err.localizedMessage ?: "تعذّر نقل المهمة إلى التاريخ المحدد"
                }
            )
        }
    }

    fun toggleTaskFromCalendar(task: DailyTask) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(task.id, !task.isCompleted)
        }
    }

    fun addTaskForDate(targetDate: Date, title: String, priority: TaskPriority) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val dayIdRes = repository.ensureDateInitialized(targetDate)
            val dayId = dayIdRes.getOrElse {
                _statusBannerMessage.value = "تعذّر تهيئة اليوم لإضافة المهمة"
                return@launch
            }
            val createRes = repository.createTask(dayId, title.trim(), priority)
            if (createRes.isSuccess) {
                _statusBannerMessage.value =
                    "تمت إضافة «${title.trim()}» إلى ${ArabicCalendarUtils.formatGregorianArabic(targetDate)} "
            }
        }
    }

    fun openOrCreateDayForDate(targetDate: Date, onReady: (String) -> Unit) {
        val targetDayId = ArabicCalendarUtils.buildDayId(
            currentUserId,
            ArabicCalendarUtils.startOfDay(targetDate)
        )
        viewModelScope.launch {
            repository.ensureDateInitialized(targetDate)
            onReady(targetDayId)
        }
    }

    fun importBulkScheduleJson(
        rawJson: String,
        onSuccess: (ScheduleImportSummary) -> Unit = {}
    ) {
        val parseResult = BulkScheduleJsonImporter.parseScheduleJson(rawJson)
        val parsedDays = parseResult.getOrElse { err ->
            _importError.value = err.localizedMessage ?: "صيغة كود JSON غير صالحة."
            return
        }

        viewModelScope.launch {
            _isImportingSchedule.value = true
            _importError.value = null
            try {
                val importResult = repository.importScheduleFromJson(parsedDays)
                importResult.fold(
                    onSuccess = { summary ->
                        _statusBannerMessage.value =
                            "تم استيراد ${summary.tasksCreated} مهمة بنجاح عبر ${summary.daysProcessed} أيام (${summary.weeksProcessed} أسابيع) "
                        onSuccess(summary)
                    },
                    onFailure = { err ->
                        _importError.value = err.localizedMessage ?: "تعذّر حفظ الجدول في قاعدة البيانات."
                    }
                )
            } finally {
                _isImportingSchedule.value = false
            }
        }
    }

    private fun buildDashboardData(
        weeks: List<Week>,
        days: List<Day>,
        tasks: List<DailyTask>,
        fixedTasks: List<FixedTask>,
        fixedCompletions: List<FixedTaskCompletion>
    ): WeeksDashboardData {
        val now = Date()
        val todayId = ArabicCalendarUtils.buildDayId(currentUserId, ArabicCalendarUtils.startOfDay(now))
        val currentSat = ArabicCalendarUtils.getSaturdayOfWeek(now)
        val currentWeekId = ArabicCalendarUtils.buildWeekId(currentUserId, currentSat)

        val daysByWeek = days.groupBy { it.weekId }
        val tasksByDay = tasks.groupBy { it.dayId }
        val completionsByDay = fixedCompletions.groupBy { it.dayId }

        // Map dayId -> yyyyMMdd key from Day documents + deterministic dayId suffix fallback
        val dateKeyByDayId = mutableMapOf<String, String>()
        for (day in days) {
            val d = day.date?.toDate()
            if (d != null) {
                dateKeyByDayId[day.id] = ArabicCalendarUtils.formatDateKey(d)
            }
        }

        val tasksByDateKey = mutableMapOf<String, MutableList<DailyTask>>()
        for (task in tasks) {
            val resolvedKey = dateKeyByDayId[task.dayId]
                ?: task.dayId.substringAfterLast('_', "").takeIf { it.length == 8 && it.all(Char::isDigit) }
            if (!resolvedKey.isNullOrBlank()) {
                tasksByDateKey.getOrPut(resolvedKey) { mutableListOf() }.add(task)
            }
        }

        val weekSummaries = weeks.map { week ->
            val weekDays = (daysByWeek[week.id] ?: emptyList()).sortedBy { it.date?.toDate()?.time ?: 0L }
            val daySummaries = weekDays.map { day ->
                val dayDate = day.date?.toDate() ?: now
                val dayTasks = tasksByDay[day.id] ?: emptyList()
                val activeHabitsCount = fixedTasks.count { it.isActive && it.isScheduledOn(dayDate) }
                val completedDayTasks = dayTasks.count { it.isCompleted }
                val completedHabits = (completionsByDay[day.id] ?: emptyList()).size.coerceAtMost(activeHabitsCount)

                val totalDayItems = dayTasks.size + activeHabitsCount
                val completedDayItems = completedDayTasks + completedHabits
                val fraction = if (totalDayItems > 0) {
                    (completedDayItems.toFloat() / totalDayItems.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }

                DaySummaryUi(
                    day = day,
                    isToday = day.id == todayId || ArabicCalendarUtils.isToday(dayDate),
                    dayNameAr = ArabicCalendarUtils.getArabicDayName(dayDate),
                    totalTasks = dayTasks.size,
                    completedTasks = completedDayTasks,
                    completedHabits = completedHabits,
                    totalActiveHabits = activeHabitsCount,
                    progressFraction = fraction,
                    progressPercent = (fraction * 100).toInt()
                )
            }

            val totalWeekTasks = daySummaries.sumOf { it.totalTasks }
            val completedWeekTasks = daySummaries.sumOf { it.completedTasks }
            val completedWeekHabits = daySummaries.sumOf { it.completedHabits }
            val totalWeekItems = totalWeekTasks + daySummaries.sumOf { it.totalActiveHabits }
            val completedWeekItems = completedWeekTasks + completedWeekHabits
            val weekFraction = if (totalWeekItems > 0) {
                (completedWeekItems.toFloat() / totalWeekItems.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

            WeekSummaryUi(
                week = week,
                isCurrentWeek = week.id == currentWeekId || ArabicCalendarUtils.isCurrentWeek(week.startDate, week.endDate, now),
                days = daySummaries,
                totalItems = totalWeekItems,
                completedItems = completedWeekItems,
                progressFraction = weekFraction,
                progressPercent = (weekFraction * 100).toInt()
            )
        }

        return WeeksDashboardData(
            weeks = weekSummaries,
            tasksByDateKey = tasksByDateKey,
            todayDayId = todayId,
            currentWeekId = currentWeekId,
            isRefreshing = _isRefreshing.value
        )
    }

    private companion object {
        const val TAG = "WeeksDashboardVM"
    }
}
