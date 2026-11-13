package io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.drahmedabdulrahman10_alt.nabda.data.ai.AiTaskAssistantService
import io.github.drahmedabdulrahman10_alt.nabda.data.ai.BulkScheduleJsonImporter
import io.github.drahmedabdulrahman10_alt.nabda.data.model.Contact
import io.github.drahmedabdulrahman10_alt.nabda.data.model.DailyTask
import io.github.drahmedabdulrahman10_alt.nabda.data.model.FixedTask
import io.github.drahmedabdulrahman10_alt.nabda.data.model.FixedTaskCompletion
import io.github.drahmedabdulrahman10_alt.nabda.data.model.NabdaDefaults
import io.github.drahmedabdulrahman10_alt.nabda.data.model.PinnedContactCompletion
import io.github.drahmedabdulrahman10_alt.nabda.data.model.Quote
import io.github.drahmedabdulrahman10_alt.nabda.data.model.TaskPriority
import io.github.drahmedabdulrahman10_alt.nabda.data.model.UserSettings
import io.github.drahmedabdulrahman10_alt.nabda.data.repository.NabdaRepository
import io.github.drahmedabdulrahman10_alt.nabda.util.ArabicCalendarUtils
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FixedTaskStatusUi(
    val fixedTask: FixedTask,
    val isCompletedOnSelectedDay: Boolean,
    val currentStreakDays: Int,
    val consecutiveMissedDays: Int,
    val isWarningActive: Boolean
)

data class PinnedContactStatusUi(
    val contact: Contact,
    val isCompletedOnSelectedDay: Boolean
)

/** One cell of the week strip at the top of the day screen. */
data class DayChipUi(
    val dayId: String,
    val letter: String,
    val dayOfMonth: Int,
    val isToday: Boolean,
    val isSelected: Boolean,
    val hasOpenTasks: Boolean
)

data class DailyScreenData(
    val selectedDayId: String = "",
    val selectedDate: Date = Date(),
    val weekDays: List<DayChipUi> = emptyList(),
    val isToday: Boolean = true,
    val gregorianHeader: String = "",
    val hijriHeader: String = "",
    val dailyQuote: Quote = Quote(
        text = NabdaDefaults.defaultQuotes.first().first,
        author = NabdaDefaults.defaultQuotes.first().second
    ),
    val allQuotes: List<Quote> = emptyList(),
    val rotatingSuggestedContact: Contact? = null,
    val pinnedContacts: List<PinnedContactStatusUi> = emptyList(),
    val allContacts: List<Contact> = emptyList(),
    val fixedTasks: List<FixedTaskStatusUi> = emptyList(),
    val allFixedTasks: List<FixedTask> = emptyList(),
    val tasks: List<DailyTask> = emptyList(),
    val overdueTasks: List<DailyTask> = emptyList(),
    val overallProgressFraction: Float = 0f,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val userSettings: UserSettings = UserSettings()
)

class DailyTasksViewModel(
    private val repository: NabdaRepository,
    private val currentUserId: String,
    private val aiTaskAssistantService: AiTaskAssistantService = AiTaskAssistantService()
) : ViewModel() {

    private fun dayIdOf(date: Date) = ArabicCalendarUtils.buildDayId(currentUserId, date)

    private val todayId = MutableStateFlow(dayIdOf(Date()))
    private val _selectedDayId = MutableStateFlow(todayId.value)
    val selectedDayId: StateFlow<String> = _selectedDayId.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _isAiParsing = MutableStateFlow(false)
    val isAiParsing: StateFlow<Boolean> = _isAiParsing.asStateFlow()

    private val _aiDialogError = MutableStateFlow<String?>(null)
    val aiDialogError: StateFlow<String?> = _aiDialogError.asStateFlow()

    private val latestSettings = MutableStateFlow(UserSettings(id = currentUserId, userId = currentUserId))

    private data class TaskStreams(
        val tasks: List<DailyTask>,
        val fixedTasks: List<FixedTask>,
        val fixedCompletions: List<FixedTaskCompletion>
    )

    private data class PeopleStreams(
        val contacts: List<Contact>,
        val pinnedCompletions: List<PinnedContactCompletion>,
        val quotes: List<Quote>
    )

    val uiState: StateFlow<UiState<DailyScreenData>> = combine(
        combine(
            repository.observeTasks(currentUserId),
            repository.observeFixedTasks(currentUserId),
            repository.observeFixedTaskCompletions(currentUserId),
            ::TaskStreams
        ),
        combine(
            repository.observeContacts(currentUserId),
            repository.observePinnedContactCompletions(currentUserId),
            repository.observeQuotes(currentUserId),
            ::PeopleStreams
        ),
        repository.observeUserSettings(currentUserId),
        _selectedDayId,
        todayId
    ) { taskStreams, people, settings, dayId, today ->
        val effectiveSettings = settings ?: UserSettings(id = currentUserId, userId = currentUserId)
        latestSettings.value = effectiveSettings
        UiState.Success(buildDailyScreenData(taskStreams, people, dayId, today, effectiveSettings))
            as UiState<DailyScreenData>
    }
        .flowOn(Dispatchers.Default)
        .catch { e ->
            Log.w(TAG, "Error observing daily screen data", e)
            emit(UiState.Error(e.localizedMessage ?: "تعذّر تحميل بيانات اليوم"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    /** Call when the screen comes back to the foreground so "today" follows the real date. */
    fun onResume() {
        val newToday = dayIdOf(Date())
        val previousToday = todayId.value
        if (newToday != previousToday) {
            if (_selectedDayId.value == previousToday) _selectedDayId.value = newToday
            todayId.value = newToday
            repository.refreshToday()
        }
    }

    fun selectDay(dayId: String) {
        if (dayId.isNotBlank()) _selectedDayId.value = dayId
    }

    fun selectToday() {
        onResume()
        _selectedDayId.value = todayId.value
    }

    /** Moves the selection by whole days; used for week-strip swipes. */
    fun shiftSelectedDay(days: Int) {
        val current = ArabicCalendarUtils.dateFromDayId(_selectedDayId.value) ?: Date()
        _selectedDayId.value = dayIdOf(ArabicCalendarUtils.addDays(current, days))
    }

    fun clearBannerMessage() {
        _statusBannerMessage.value = null
    }

    fun clearAiDialogError() {
        _aiDialogError.value = null
    }

    // --- AI-assisted creation and bulk import ---

    fun importBulkScheduleJson(rawJson: String, onSuccessCloseDialog: () -> Unit) {
        val parsedDays = BulkScheduleJsonImporter.parseScheduleJson(rawJson).getOrElse { err ->
            _aiDialogError.value = err.localizedMessage ?: "صيغة كود JSON غير صالحة."
            return
        }
        viewModelScope.launch {
            _isAiParsing.value = true
            _aiDialogError.value = null
            try {
                repository.importScheduleFromJson(parsedDays).fold(
                    onSuccess = { summary ->
                        summary.firstImportedDayId?.takeIf { it.isNotBlank() }?.let { _selectedDayId.value = it }
                        _statusBannerMessage.value =
                            "تم استيراد ${summary.tasksCreated} مهمة على ${summary.daysProcessed} أيام"
                        onSuccessCloseDialog()
                    },
                    onFailure = { err ->
                        _aiDialogError.value = err.localizedMessage ?: "تعذّر حفظ الجدول."
                    }
                )
            } finally {
                _isAiParsing.value = false
            }
        }
    }

    fun addTasksViaAi(userInput: String, onSuccessCloseDialog: (Int) -> Unit) {
        if (userInput.isBlank() || _isAiParsing.value) return
        val targetDayId = _selectedDayId.value
        val currentSettings = latestSettings.value
        viewModelScope.launch {
            _isAiParsing.value = true
            _aiDialogError.value = null
            try {
                aiTaskAssistantService.parseNaturalLanguageTasks(userInput, currentSettings).fold(
                    onSuccess = { parsedList ->
                        var addedCount = 0
                        for (item in parsedList) {
                            if (repository.createTask(targetDayId, item.title, item.priority).isSuccess) addedCount++
                        }
                        if (addedCount > 0) {
                            _statusBannerMessage.value = "تمت إضافة $addedCount مهام"
                            onSuccessCloseDialog(addedCount)
                        } else {
                            _aiDialogError.value = "تعذّر حفظ المهام المستخرجة."
                        }
                    },
                    onFailure = { err ->
                        _aiDialogError.value = err.localizedMessage ?: "تعذّر تحليل النص."
                    }
                )
            } finally {
                _isAiParsing.value = false
            }
        }
    }

    // --- Tasks ---

    fun addTask(title: String, priority: TaskPriority) {
        if (title.isBlank()) return
        val dayId = _selectedDayId.value
        viewModelScope.launch {
            if (repository.createTask(dayId, title.trim(), priority).isFailure) {
                _statusBannerMessage.value = "تعذّر إضافة المهمة"
            }
        }
    }

    fun toggleTask(task: DailyTask) {
        viewModelScope.launch { repository.toggleTaskCompleted(task.id, !task.isCompleted) }
    }

    fun updateTask(taskId: String, title: String, priority: TaskPriority) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.updateTaskDetails(taskId, title, priority) }
    }

    fun deleteTask(task: DailyTask) {
        viewModelScope.launch { repository.deleteTask(task.id) }
    }

    /** Puts a task back exactly as it was; used to undo a delete or a move. */
    fun restoreTask(task: DailyTask) {
        viewModelScope.launch { repository.restoreTask(task) }
    }

    fun moveTaskToNextDay(task: DailyTask) {
        val base = ArabicCalendarUtils.dateFromDayId(task.dayId) ?: Date()
        val nextDayId = dayIdOf(ArabicCalendarUtils.addDays(base, 1))
        viewModelScope.launch { repository.moveTaskToDay(task.id, nextDayId, task.dayId) }
    }

    fun moveTasksToToday(tasks: List<DailyTask>) {
        val target = todayId.value
        viewModelScope.launch { repository.moveTasksToDay(tasks.map { it.id }, target) }
    }

    fun restoreTasks(tasks: List<DailyTask>) {
        viewModelScope.launch { tasks.forEach { repository.restoreTask(it) } }
    }

    // --- Fixed daily tasks ---

    fun toggleFixedTask(fixedTask: FixedTask, isCurrentlyCompleted: Boolean) {
        val dayId = _selectedDayId.value
        viewModelScope.launch {
            repository.toggleFixedTaskCompletion(fixedTask.id, dayId, isCurrentlyCompleted)
        }
    }

    fun addFixedTask(
        title: String,
        missedDaysThreshold: Int,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
        nextOrder: Int,
        weekDays: Int = FixedTask.EVERY_DAY
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.createFixedTask(
                title = title.trim(),
                order = nextOrder,
                missedDaysThreshold = missedDaysThreshold,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                weekDays = weekDays
            )
        }
    }

    fun updateFixedTask(fixedTask: FixedTask) {
        viewModelScope.launch { repository.updateFixedTask(fixedTask) }
    }

    fun deleteFixedTask(fixedTaskId: String) {
        viewModelScope.launch { repository.deleteFixedTask(fixedTaskId) }
    }

    fun restoreFixedTask(fixedTask: FixedTask) {
        viewModelScope.launch { repository.restoreFixedTask(fixedTask) }
    }

    // --- Contacts ---

    fun addContact(name: String, phone: String, isPinned: Boolean) {
        if (name.isBlank() || phone.isBlank()) return
        viewModelScope.launch { repository.createContact(name.trim(), phone.trim(), isPinned) }
    }

    fun updateContact(contact: Contact) {
        viewModelScope.launch { repository.updateContact(contact) }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch { repository.deleteContact(contactId) }
    }

    fun restoreContact(contact: Contact) {
        viewModelScope.launch { repository.restoreContact(contact) }
    }

    fun markRotatingContactDone(contact: Contact) {
        viewModelScope.launch { repository.markContactSuggestedOrContacted(contact.id) }
    }

    fun togglePinnedContact(contact: Contact, isCurrentlyCompleted: Boolean) {
        val dayId = _selectedDayId.value
        viewModelScope.launch {
            repository.togglePinnedContactCompletion(contact.id, dayId, isCurrentlyCompleted)
        }
    }

    // --- Quotes ---

    fun addQuote(text: String, author: String?) {
        if (text.isBlank()) return
        viewModelScope.launch { repository.createQuote(text.trim(), author?.trim()) }
    }

    fun updateQuote(quoteId: String, text: String, author: String?) {
        if (text.isBlank()) return
        viewModelScope.launch { repository.updateQuote(quoteId, text.trim(), author?.trim()) }
    }

    fun deleteQuote(quoteId: String) {
        viewModelScope.launch { repository.deleteQuote(quoteId) }
    }

    fun restoreQuote(quote: Quote) {
        viewModelScope.launch { repository.restoreQuote(quote) }
    }

    private fun buildDailyScreenData(
        streams: TaskStreams,
        people: PeopleStreams,
        selectedDayId: String,
        todayDayId: String,
        userSettings: UserSettings
    ): DailyScreenData {
        val now = Date()
        val dayDate = ArabicCalendarUtils.dateFromDayId(selectedDayId) ?: ArabicCalendarUtils.startOfDay(now)
        val prefix = selectedDayId.substringBeforeLast('_') + "_"

        // Quote of the day: stable for a given day, different between days.
        val allQuotes = people.quotes.ifEmpty {
            NabdaDefaults.defaultQuotes.mapIndexed { idx, (text, author) ->
                Quote(id = "default_$idx", text = text, author = author, userId = currentUserId)
            }
        }
        val dailyQuote = allQuotes[(selectedDayId.hashCode() and Int.MAX_VALUE) % allQuotes.size]

        val activeContacts = people.contacts.filter { it.isActive }
        val rotatingSuggested = activeContacts.filter { !it.isPinned }
            .minByOrNull { it.lastSuggested?.millis ?: 0L }
        val pinnedDoneIds = people.pinnedCompletions
            .filter { it.dayId == selectedDayId }
            .mapTo(HashSet()) { it.contactId }
        val pinnedStatus = activeContacts.filter { it.isPinned }.map {
            PinnedContactStatusUi(contact = it, isCompletedOnSelectedDay = it.id in pinnedDoneIds)
        }

        // Fixed daily tasks with streak and missed-day counts.
        val completedDaysByTask = HashMap<String, HashSet<String>>()
        for (completion in streams.fixedCompletions) {
            completedDaysByTask.getOrPut(completion.fixedTaskId) { HashSet() }.add(completion.dayId)
        }
        val cursor = Calendar.getInstance()
        // Only the task's own weekdays count: days it does not repeat on neither break a streak
        // nor add to the missed-days warning.
        val fixedStatus = streams.fixedTasks.filter { it.isActive && it.isScheduledOn(dayDate) }.map { habit ->
            val doneDays: Set<String> = completedDaysByTask[habit.id] ?: emptySet()
            val doneOnSelected = selectedDayId in doneDays

            var streak = 0
            cursor.time = dayDate
            if (!doneOnSelected) cursor.add(Calendar.DAY_OF_YEAR, -1)
            for (step in 0 until 400) {
                if (habit.isScheduledOn(cursor.time)) {
                    if ((prefix + ArabicCalendarUtils.dateKey(cursor)) !in doneDays) break
                    streak++
                }
                cursor.add(Calendar.DAY_OF_YEAR, -1)
            }

            var missed = 0
            val habitStart = habit.startDate?.let { ArabicCalendarUtils.startOfDay(it.toDate()).time } ?: 0L
            cursor.time = now
            for (step in 0 until 400) {
                if (missed >= 60 || cursor.timeInMillis < habitStart) break
                if (habit.isScheduledOn(cursor.time)) {
                    if ((prefix + ArabicCalendarUtils.dateKey(cursor)) in doneDays) break
                    missed++
                }
                cursor.add(Calendar.DAY_OF_YEAR, -1)
            }

            FixedTaskStatusUi(
                fixedTask = habit,
                isCompletedOnSelectedDay = doneOnSelected,
                currentStreakDays = streak,
                consecutiveMissedDays = missed,
                isWarningActive = missed >= habit.missedDaysThreshold
            )
        }

        val dayTasks = streams.tasks.filter { it.dayId == selectedDayId }

        // Unfinished tasks from the last week, offered for carry-over when viewing today.
        val overdue = if (selectedDayId == todayDayId) {
            cursor.time = ArabicCalendarUtils.dateFromDayId(todayDayId) ?: now
            val recentDayIds = HashSet<String>()
            repeat(7) {
                cursor.add(Calendar.DAY_OF_YEAR, -1)
                recentDayIds += prefix + ArabicCalendarUtils.dateKey(cursor)
            }
            streams.tasks.filter { !it.isCompleted && it.dayId in recentDayIds }
        } else {
            emptyList()
        }

        val openTaskDays = streams.tasks.filter { !it.isCompleted }.mapTo(HashSet()) { it.dayId }
        val weekStart = ArabicCalendarUtils.getSaturdayOfWeek(dayDate)
        cursor.time = weekStart
        val weekDays = List(7) {
            val date = cursor.time
            val id = prefix + ArabicCalendarUtils.dateKey(cursor)
            val chip = DayChipUi(
                dayId = id,
                letter = ArabicCalendarUtils.getArabicDayLetter(date),
                dayOfMonth = cursor.get(Calendar.DAY_OF_MONTH),
                isToday = id == todayDayId,
                isSelected = id == selectedDayId,
                hasOpenTasks = id in openTaskDays
            )
            cursor.add(Calendar.DAY_OF_YEAR, 1)
            chip
        }

        val total = dayTasks.size + fixedStatus.size + pinnedStatus.size
        val completed = dayTasks.count { it.isCompleted } +
            fixedStatus.count { it.isCompletedOnSelectedDay } +
            pinnedStatus.count { it.isCompletedOnSelectedDay }

        return DailyScreenData(
            selectedDayId = selectedDayId,
            selectedDate = dayDate,
            weekDays = weekDays,
            isToday = selectedDayId == todayDayId,
            gregorianHeader = ArabicCalendarUtils.formatGregorianArabic(dayDate),
            hijriHeader = ArabicCalendarUtils.formatHijriArabic(dayDate),
            dailyQuote = dailyQuote,
            allQuotes = allQuotes,
            rotatingSuggestedContact = rotatingSuggested,
            pinnedContacts = pinnedStatus,
            allContacts = people.contacts,
            fixedTasks = fixedStatus,
            allFixedTasks = streams.fixedTasks,
            tasks = dayTasks,
            overdueTasks = overdue,
            overallProgressFraction = if (total > 0) completed.toFloat() / total else 0f,
            completedCount = completed,
            totalCount = total,
            userSettings = userSettings
        )
    }

    private companion object {
        const val TAG = "DailyTasksVM"
    }
}
