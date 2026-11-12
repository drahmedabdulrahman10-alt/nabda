package io.github.drahmedabdulrahman10_alt.nabda.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import io.github.drahmedabdulrahman10_alt.nabda.data.ai.ScheduleImportSummary
import io.github.drahmedabdulrahman10_alt.nabda.data.ai.ScheduledDayDto
import io.github.drahmedabdulrahman10_alt.nabda.data.local.NabdaDatabase
import io.github.drahmedabdulrahman10_alt.nabda.data.local.SettingsJson
import io.github.drahmedabdulrahman10_alt.nabda.data.model.Contact
import io.github.drahmedabdulrahman10_alt.nabda.data.model.DailyTask
import io.github.drahmedabdulrahman10_alt.nabda.data.model.Day
import io.github.drahmedabdulrahman10_alt.nabda.data.model.FixedTask
import io.github.drahmedabdulrahman10_alt.nabda.data.model.FixedTaskCompletion
import io.github.drahmedabdulrahman10_alt.nabda.data.model.Goal
import io.github.drahmedabdulrahman10_alt.nabda.data.model.GoalMilestone
import io.github.drahmedabdulrahman10_alt.nabda.data.model.GoalTimeframe
import io.github.drahmedabdulrahman10_alt.nabda.data.model.PinnedContactCompletion
import io.github.drahmedabdulrahman10_alt.nabda.data.model.Quote
import io.github.drahmedabdulrahman10_alt.nabda.data.model.TaskPriority
import io.github.drahmedabdulrahman10_alt.nabda.data.model.Timestamp
import io.github.drahmedabdulrahman10_alt.nabda.data.model.UserSettings
import io.github.drahmedabdulrahman10_alt.nabda.data.model.Week
import io.github.drahmedabdulrahman10_alt.nabda.util.ArabicCalendarUtils
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** The app stores everything on the device, so every record belongs to this single owner. */
const val LOCAL_USER_ID = "local"

interface NabdaRepository {
    fun observeWeeks(userId: String): Flow<List<Week>>
    fun observeDays(userId: String): Flow<List<Day>>
    fun observeTasks(userId: String): Flow<List<DailyTask>>
    fun observeFixedTasks(userId: String): Flow<List<FixedTask>>
    fun observeFixedTaskCompletions(userId: String): Flow<List<FixedTaskCompletion>>
    fun observeContacts(userId: String): Flow<List<Contact>>
    fun observePinnedContactCompletions(userId: String): Flow<List<PinnedContactCompletion>>
    fun observeQuotes(userId: String): Flow<List<Quote>>
    fun observeUserSettings(userId: String): Flow<UserSettings?>
    fun observeGoals(userId: String): Flow<List<Goal>>

    /** Recomputes the visible week window; call when the calendar day may have changed. */
    fun refreshToday()

    suspend fun updateWeekReflection(weekId: String, reflection: String)
    suspend fun importScheduleFromJson(scheduledDays: List<ScheduledDayDto>): Result<ScheduleImportSummary>
    suspend fun ensureDateInitialized(targetDate: Date): Result<String>

    suspend fun createTask(dayId: String, title: String, priority: TaskPriority, movedFromDayId: String? = null): Result<String>
    suspend fun restoreTask(task: DailyTask): Result<Unit>
    suspend fun toggleTaskCompleted(taskId: String, isCompleted: Boolean): Result<Unit>
    suspend fun updateTaskDetails(taskId: String, title: String, priority: TaskPriority): Result<Unit>
    suspend fun moveTaskToDay(taskId: String, targetDayId: String, movedFromDayId: String?): Result<Unit>
    suspend fun moveTasksToDay(taskIds: List<String>, targetDayId: String): Result<Unit>
    suspend fun moveTaskToDate(task: DailyTask, targetDate: Date): Result<String>
    suspend fun deleteTask(taskId: String): Result<Unit>

    suspend fun createFixedTask(
        title: String,
        order: Int,
        missedDaysThreshold: Int,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int
    ): Result<String>
    suspend fun updateFixedTask(fixedTask: FixedTask): Result<Unit>
    suspend fun deleteFixedTask(fixedTaskId: String): Result<Unit>
    suspend fun restoreFixedTask(fixedTask: FixedTask): Result<Unit>
    suspend fun toggleFixedTaskCompletion(fixedTaskId: String, dayId: String, currentlyCompleted: Boolean): Result<Unit>

    suspend fun createContact(name: String, phone: String, isPinned: Boolean): Result<String>
    suspend fun updateContact(contact: Contact): Result<Unit>
    suspend fun deleteContact(contactId: String): Result<Unit>
    suspend fun restoreContact(contact: Contact): Result<Unit>
    suspend fun markContactSuggestedOrContacted(contactId: String): Result<Unit>
    suspend fun togglePinnedContactCompletion(contactId: String, dayId: String, currentlyCompleted: Boolean): Result<Unit>

    suspend fun createQuote(text: String, author: String?): Result<String>
    suspend fun updateQuote(quoteId: String, text: String, author: String?): Result<Unit>
    suspend fun deleteQuote(quoteId: String): Result<Unit>
    suspend fun restoreQuote(quote: Quote): Result<Unit>

    suspend fun saveUserSettings(settings: UserSettings): Result<Unit>

    suspend fun createGoal(
        title: String,
        description: String?,
        timeframe: GoalTimeframe,
        periodLabel: String,
        progress: Int = 0,
        milestones: List<GoalMilestone> = emptyList()
    ): Result<String>
    suspend fun updateGoal(goal: Goal): Result<Unit>
    suspend fun toggleGoalCompleted(goal: Goal, isCompleted: Boolean): Result<Unit>
    suspend fun toggleGoalMilestone(goal: Goal, milestoneId: String): Result<Unit>
    suspend fun updateGoalProgress(goal: Goal, newProgress: Int): Result<Unit>
    suspend fun deleteGoal(goalId: String): Result<Unit>
}

/**
 * On-device storage. Every table is read once in the background at start-up and then served
 * from memory; writes go to SQLite on a single background thread and then update the in-memory
 * copy, so the UI never waits on the disk or the network.
 */
class LocalNabdaRepository(context: Context) : NabdaRepository {

    private val database = NabdaDatabase(context.applicationContext)
    private val io = Dispatchers.IO.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + io)

    private val tasks = MutableStateFlow<List<DailyTask>?>(null)
    private val fixedTasks = MutableStateFlow<List<FixedTask>?>(null)
    private val fixedCompletions = MutableStateFlow<List<FixedTaskCompletion>?>(null)
    private val contacts = MutableStateFlow<List<Contact>?>(null)
    private val contactCompletions = MutableStateFlow<List<PinnedContactCompletion>?>(null)
    private val quotes = MutableStateFlow<List<Quote>?>(null)
    private val goals = MutableStateFlow<List<Goal>?>(null)
    private val reflections = MutableStateFlow<Map<String, String>?>(null)
    private val settings = MutableStateFlow<UserSettings?>(null)

    private val extraWeekStarts = MutableStateFlow<Set<Long>>(emptySet())
    private val todayKey = MutableStateFlow(ArabicCalendarUtils.formatDateKey(Date()))

    private val weekStartByDayId = ConcurrentHashMap<String, Long>()
    private val dayCache = ConcurrentHashMap<Long, Day>()

    private val loadJob = scope.launch { loadAll() }

    private fun loadAll() {
        val db = database.readableDatabase
        tasks.value = db.queryAll("tasks") { c ->
            DailyTask(
                id = c.getString(0),
                dayId = c.getString(1),
                title = c.getString(2),
                priority = c.getString(3),
                isCompleted = c.getInt(4) != 0,
                movedFromDayId = c.getStringOrNull(5),
                userId = LOCAL_USER_ID,
                createdAt = Timestamp(c.getLong(6)),
                updatedAt = Timestamp(c.getLong(7))
            )
        }
        fixedTasks.value = db.queryAll("fixed_tasks") { c ->
            FixedTask(
                id = c.getString(0),
                title = c.getString(1),
                order = c.getInt(2),
                missedDaysThreshold = c.getInt(3),
                isActive = c.getInt(4) != 0,
                reminderEnabled = c.getInt(5) != 0,
                reminderHour = c.getInt(6),
                reminderMinute = c.getInt(7),
                startDate = Timestamp(c.getLong(8)),
                userId = LOCAL_USER_ID,
                createdAt = Timestamp(c.getLong(9))
            )
        }
        fixedCompletions.value = db.queryAll("fixed_task_completions") { c ->
            FixedTaskCompletion(
                id = "ftc_${c.getString(0)}_${c.getString(1)}",
                fixedTaskId = c.getString(0),
                dayId = c.getString(1),
                completedAt = Timestamp(c.getLong(2)),
                userId = LOCAL_USER_ID
            )
        }
        contacts.value = db.queryAll("contacts") { c ->
            Contact(
                id = c.getString(0),
                name = c.getString(1),
                phone = c.getString(2),
                isActive = c.getInt(3) != 0,
                isPinned = c.getInt(4) != 0,
                lastSuggested = if (c.isNull(5)) null else Timestamp(c.getLong(5)),
                userId = LOCAL_USER_ID,
                createdAt = Timestamp(c.getLong(6))
            )
        }
        contactCompletions.value = db.queryAll("contact_completions") { c ->
            PinnedContactCompletion(
                id = "pcc_${c.getString(0)}_${c.getString(1)}",
                contactId = c.getString(0),
                dayId = c.getString(1),
                completedAt = Timestamp(c.getLong(2)),
                userId = LOCAL_USER_ID
            )
        }
        quotes.value = db.queryAll("quotes") { c ->
            Quote(
                id = c.getString(0),
                text = c.getString(1),
                author = c.getStringOrNull(2),
                userId = LOCAL_USER_ID,
                createdAt = Timestamp(c.getLong(3))
            )
        }
        goals.value = db.queryAll("goals") { c ->
            Goal(
                id = c.getString(0),
                title = c.getString(1),
                description = c.getStringOrNull(2),
                timeframe = c.getString(3),
                periodLabel = c.getString(4),
                isCompleted = c.getInt(5) != 0,
                progress = c.getInt(6),
                milestones = milestonesFromJson(c.getString(7)),
                createdAt = Timestamp(c.getLong(8)),
                userId = LOCAL_USER_ID
            )
        }
        reflections.value = db.queryAll("week_reflections") { c -> c.getString(0) to c.getString(1) }.toMap()
        settings.value = db.queryAll("kv") { c -> c.getString(0) to c.getString(1) }
            .firstOrNull { it.first == KEY_SETTINGS }
            ?.let { SettingsJson.fromJson(it.second) }
            ?: UserSettings(id = LOCAL_USER_ID, userId = LOCAL_USER_ID)
    }

    /** Runs [block] on the database thread once the initial load has finished. */
    private suspend fun <T> write(block: (SQLiteDatabase) -> T): Result<T> = withContext(io) {
        loadJob.join()
        try {
            Result.success(block(database.writableDatabase))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun newId(prefix: String) = "${prefix}_${UUID.randomUUID().toString().replace("-", "")}"

    // --- Observation ---

    private val weekStarts: Flow<List<Long>> =
        combine(tasks.filterNotNull(), extraWeekStarts, todayKey) { allTasks, extra, _ ->
            val currentSaturday = ArabicCalendarUtils.getSaturdayOfWeek(Date())
            val starts = sortedSetOf<Long>()
            for (offset in -4..2) {
                starts += ArabicCalendarUtils.addDays(currentSaturday, offset * 7).time
            }
            starts += extra
            val dayIds = HashSet<String>()
            for (task in allTasks) dayIds += task.dayId
            for (dayId in dayIds) weekStartOf(dayId)?.let { starts += it }
            starts.toList()
        }.distinctUntilChanged().flowOn(Dispatchers.Default)

    private fun weekStartOf(dayId: String): Long? {
        weekStartByDayId[dayId]?.let { return it }
        val date = ArabicCalendarUtils.dateFromDayId(dayId) ?: return null
        val start = ArabicCalendarUtils.getSaturdayOfWeek(date).time
        weekStartByDayId[dayId] = start
        return start
    }

    override fun observeWeeks(userId: String): Flow<List<Week>> =
        combine(weekStarts, reflections.filterNotNull(), todayKey) { starts, notes, _ ->
            val currentSaturday = ArabicCalendarUtils.getSaturdayOfWeek(Date())
            starts.sortedDescending().map { startMillis ->
                val start = Date(startMillis)
                val end = ArabicCalendarUtils.getFridayOfWeek(start)
                val offset = Math.round((startMillis - currentSaturday.time) / WEEK_MILLIS.toDouble()).toInt()
                val weekId = ArabicCalendarUtils.buildWeekId(LOCAL_USER_ID, start)
                Week(
                    id = weekId,
                    name = ArabicCalendarUtils.getWeekLabel(offset, start, end),
                    startDate = Timestamp(start),
                    endDate = Timestamp(end),
                    reflection = notes[weekId],
                    userId = LOCAL_USER_ID
                )
            }
        }.flowOn(Dispatchers.Default)

    override fun observeDays(userId: String): Flow<List<Day>> =
        weekStarts.map { starts ->
            val days = ArrayList<Day>(starts.size * 7)
            for (startMillis in starts) {
                val start = Date(startMillis)
                val weekId = ArabicCalendarUtils.buildWeekId(LOCAL_USER_ID, start)
                for (index in 0..6) {
                    val date = ArabicCalendarUtils.addDays(start, index)
                    days += dayCache.getOrPut(date.time) {
                        Day(
                            id = ArabicCalendarUtils.buildDayId(LOCAL_USER_ID, date),
                            weekId = weekId,
                            date = Timestamp(date),
                            gregorianDateStr = ArabicCalendarUtils.formatGregorianArabic(date),
                            hijriDateStr = ArabicCalendarUtils.formatHijriArabic(date),
                            userId = LOCAL_USER_ID
                        )
                    }
                }
            }
            days as List<Day>
        }.flowOn(Dispatchers.Default)

    override fun observeTasks(userId: String): Flow<List<DailyTask>> =
        tasks.filterNotNull().map { list ->
            list.sortedWith(
                compareBy<DailyTask> { it.isCompleted }
                    .thenBy { it.priorityEnum.sortOrder }
                    .thenByDescending { it.createdAt?.millis ?: 0L }
            )
        }.flowOn(Dispatchers.Default)

    override fun observeFixedTasks(userId: String): Flow<List<FixedTask>> =
        fixedTasks.filterNotNull().map { list -> list.sortedBy { it.order } }

    // Completions outlive a deleted task so that undoing the delete brings its history back;
    // they are hidden here while the task is gone.
    override fun observeFixedTaskCompletions(userId: String): Flow<List<FixedTaskCompletion>> =
        combine(fixedCompletions.filterNotNull(), fixedTasks.filterNotNull()) { completions, existing ->
            val ids = existing.mapTo(HashSet()) { it.id }
            completions.filter { it.fixedTaskId in ids }
        }

    override fun observeContacts(userId: String): Flow<List<Contact>> =
        contacts.filterNotNull().map { list ->
            list.sortedWith(compareByDescending<Contact> { it.isPinned }.thenBy { it.name })
        }

    override fun observePinnedContactCompletions(userId: String): Flow<List<PinnedContactCompletion>> =
        combine(contactCompletions.filterNotNull(), contacts.filterNotNull()) { completions, existing ->
            val ids = existing.mapTo(HashSet()) { it.id }
            completions.filter { it.contactId in ids }
        }

    override fun observeQuotes(userId: String): Flow<List<Quote>> =
        quotes.filterNotNull().map { list -> list.sortedByDescending { it.createdAt?.millis ?: 0L } }

    override fun observeUserSettings(userId: String): Flow<UserSettings?> = settings.filterNotNull()

    override fun observeGoals(userId: String): Flow<List<Goal>> =
        goals.filterNotNull().map { list ->
            list.sortedWith(
                compareBy<Goal> { it.effectiveIsCompleted() }
                    .thenBy { it.timeframeEnum.sortOrder }
                    .thenByDescending { it.createdAt?.millis ?: 0L }
            )
        }

    override fun refreshToday() {
        todayKey.value = ArabicCalendarUtils.formatDateKey(Date())
    }

    // --- Weeks ---

    override suspend fun updateWeekReflection(weekId: String, reflection: String) {
        val text = reflection.trim().take(1000)
        write { db ->
            db.insertWithOnConflict(
                "week_reflections",
                null,
                ContentValues().apply {
                    put("week_id", weekId)
                    put("reflection", text)
                },
                SQLiteDatabase.CONFLICT_REPLACE
            )
        }.onSuccess { reflections.update { it.orEmpty() + (weekId to text) } }
    }

    override suspend fun ensureDateInitialized(targetDate: Date): Result<String> {
        val day = ArabicCalendarUtils.startOfDay(targetDate)
        val start = ArabicCalendarUtils.getSaturdayOfWeek(day).time
        extraWeekStarts.update { it + start }
        return Result.success(ArabicCalendarUtils.buildDayId(LOCAL_USER_ID, day))
    }

    override suspend fun importScheduleFromJson(
        scheduledDays: List<ScheduledDayDto>
    ): Result<ScheduleImportSummary> {
        if (scheduledDays.isEmpty()) {
            return Result.failure(IllegalArgumentException("قائمة الأيام في الجدول فارغة."))
        }
        val now = System.currentTimeMillis()
        val created = ArrayList<DailyTask>()
        val weekIds = HashSet<Long>()
        val dayIds = LinkedHashSet<String>()
        for (scheduledDay in scheduledDays) {
            val date = ArabicCalendarUtils.startOfDay(scheduledDay.date)
            weekIds += ArabicCalendarUtils.getSaturdayOfWeek(date).time
            val dayId = ArabicCalendarUtils.buildDayId(LOCAL_USER_ID, date)
            dayIds += dayId
            for (dto in scheduledDay.tasks) {
                created += DailyTask(
                    id = newId("tsk"),
                    dayId = dayId,
                    title = dto.title.trim().take(300),
                    priority = dto.priority.name,
                    userId = LOCAL_USER_ID,
                    createdAt = Timestamp(now - created.size),
                    updatedAt = Timestamp(now)
                )
            }
        }
        return write { db ->
            db.beginTransaction()
            try {
                created.forEach { db.insertOrThrow("tasks", null, it.toValues()) }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }.map {
            tasks.update { it.orEmpty() + created }
            extraWeekStarts.update { it + weekIds }
            ScheduleImportSummary(
                weeksProcessed = weekIds.size,
                daysProcessed = dayIds.size,
                tasksCreated = created.size,
                firstImportedDayId = dayIds.firstOrNull()
            )
        }
    }

    // --- Daily tasks ---

    private fun DailyTask.toValues() = ContentValues().apply {
        put("id", id)
        put("day_id", dayId)
        put("title", title)
        put("priority", priority)
        put("done", if (isCompleted) 1 else 0)
        put("moved_from", movedFromDayId)
        put("created_at", createdAt?.millis ?: System.currentTimeMillis())
        put("updated_at", updatedAt?.millis ?: System.currentTimeMillis())
    }

    override suspend fun createTask(
        dayId: String,
        title: String,
        priority: TaskPriority,
        movedFromDayId: String?
    ): Result<String> {
        val now = Timestamp.now()
        val task = DailyTask(
            id = newId("tsk"),
            dayId = dayId,
            title = title.trim().take(300),
            priority = priority.name,
            movedFromDayId = movedFromDayId,
            userId = LOCAL_USER_ID,
            createdAt = now,
            updatedAt = now
        )
        return write { db -> db.insertOrThrow("tasks", null, task.toValues()) }.map {
            tasks.update { it.orEmpty() + task }
            task.id
        }
    }

    override suspend fun restoreTask(task: DailyTask): Result<Unit> =
        write { db ->
            db.insertWithOnConflict("tasks", null, task.toValues(), SQLiteDatabase.CONFLICT_REPLACE)
        }.map {
            tasks.update { list -> list.orEmpty().filterNot { it.id == task.id } + task }
        }

    private suspend fun updateTask(
        taskId: String,
        values: ContentValues,
        transform: (DailyTask) -> DailyTask
    ): Result<Unit> {
        val now = System.currentTimeMillis()
        values.put("updated_at", now)
        return write { db -> db.update("tasks", values, "id = ?", arrayOf(taskId)) }.map {
            tasks.update { list ->
                list.orEmpty().map { if (it.id == taskId) transform(it).copy(updatedAt = Timestamp(now)) else it }
            }
        }
    }

    override suspend fun toggleTaskCompleted(taskId: String, isCompleted: Boolean): Result<Unit> =
        updateTask(taskId, ContentValues().apply { put("done", if (isCompleted) 1 else 0) }) {
            it.copy(isCompleted = isCompleted)
        }

    override suspend fun updateTaskDetails(taskId: String, title: String, priority: TaskPriority): Result<Unit> {
        val cleanTitle = title.trim().take(300)
        return updateTask(
            taskId,
            ContentValues().apply {
                put("title", cleanTitle)
                put("priority", priority.name)
            }
        ) { it.copy(title = cleanTitle, priority = priority.name) }
    }

    override suspend fun moveTaskToDay(taskId: String, targetDayId: String, movedFromDayId: String?): Result<Unit> =
        updateTask(
            taskId,
            ContentValues().apply {
                put("day_id", targetDayId)
                put("moved_from", movedFromDayId)
            }
        ) { it.copy(dayId = targetDayId, movedFromDayId = movedFromDayId) }

    override suspend fun moveTasksToDay(taskIds: List<String>, targetDayId: String): Result<Unit> {
        if (taskIds.isEmpty()) return Result.success(Unit)
        val ids = taskIds.toSet()
        val now = System.currentTimeMillis()
        return write { db ->
            db.beginTransaction()
            try {
                for (id in ids) {
                    db.execSQL(
                        "UPDATE tasks SET moved_from = day_id, day_id = ?, updated_at = ? WHERE id = ?",
                        arrayOf<Any>(targetDayId, now, id)
                    )
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }.map {
            tasks.update { list ->
                list.orEmpty().map {
                    if (it.id in ids) {
                        it.copy(dayId = targetDayId, movedFromDayId = it.dayId, updatedAt = Timestamp(now))
                    } else {
                        it
                    }
                }
            }
        }
    }

    override suspend fun moveTaskToDate(task: DailyTask, targetDate: Date): Result<String> {
        val targetDayId = ensureDateInitialized(targetDate).getOrThrow()
        if (targetDayId == task.dayId) return Result.success(targetDayId)
        return moveTaskToDay(task.id, targetDayId, task.dayId).map { targetDayId }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> =
        write { db -> db.delete("tasks", "id = ?", arrayOf(taskId)) }.map {
            tasks.update { list -> list.orEmpty().filterNot { it.id == taskId } }
        }

    // --- Fixed daily tasks ---

    private fun FixedTask.toValues() = ContentValues().apply {
        put("id", id)
        put("title", title.trim().take(200))
        put("ord", order.coerceIn(0, 10000))
        put("missed_threshold", missedDaysThreshold.coerceIn(1, 365))
        put("active", if (isActive) 1 else 0)
        put("reminder_enabled", if (reminderEnabled) 1 else 0)
        put("reminder_hour", reminderHour.coerceIn(0, 23))
        put("reminder_minute", reminderMinute.coerceIn(0, 59))
        put("start_date", startDate?.millis ?: System.currentTimeMillis())
        put("created_at", createdAt?.millis ?: System.currentTimeMillis())
    }

    override suspend fun createFixedTask(
        title: String,
        order: Int,
        missedDaysThreshold: Int,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int
    ): Result<String> {
        val now = Timestamp.now()
        val fixedTask = FixedTask(
            id = newId("ft"),
            title = title.trim().take(200),
            order = order,
            missedDaysThreshold = missedDaysThreshold,
            isActive = true,
            reminderEnabled = reminderEnabled,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
            startDate = now,
            userId = LOCAL_USER_ID,
            createdAt = now
        )
        return write { db -> db.insertOrThrow("fixed_tasks", null, fixedTask.toValues()) }.map {
            fixedTasks.update { it.orEmpty() + fixedTask }
            fixedTask.id
        }
    }

    override suspend fun updateFixedTask(fixedTask: FixedTask): Result<Unit> =
        write { db ->
            db.update("fixed_tasks", fixedTask.toValues(), "id = ?", arrayOf(fixedTask.id))
        }.map {
            fixedTasks.update { list -> list.orEmpty().map { if (it.id == fixedTask.id) fixedTask else it } }
        }

    override suspend fun deleteFixedTask(fixedTaskId: String): Result<Unit> =
        write { db -> db.delete("fixed_tasks", "id = ?", arrayOf(fixedTaskId)) }.map {
            fixedTasks.update { list -> list.orEmpty().filterNot { it.id == fixedTaskId } }
        }

    override suspend fun restoreFixedTask(fixedTask: FixedTask): Result<Unit> =
        write { db ->
            db.insertWithOnConflict("fixed_tasks", null, fixedTask.toValues(), SQLiteDatabase.CONFLICT_REPLACE)
        }.map {
            fixedTasks.update { list -> list.orEmpty().filterNot { it.id == fixedTask.id } + fixedTask }
        }

    override suspend fun toggleFixedTaskCompletion(
        fixedTaskId: String,
        dayId: String,
        currentlyCompleted: Boolean
    ): Result<Unit> {
        val now = Timestamp.now()
        return write { db ->
            if (currentlyCompleted) {
                db.delete(
                    "fixed_task_completions",
                    "fixed_task_id = ? AND day_id = ?",
                    arrayOf(fixedTaskId, dayId)
                )
            } else {
                db.insertWithOnConflict(
                    "fixed_task_completions",
                    null,
                    ContentValues().apply {
                        put("fixed_task_id", fixedTaskId)
                        put("day_id", dayId)
                        put("completed_at", now.millis)
                    },
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
        }.map {
            fixedCompletions.update { list ->
                val without = list.orEmpty().filterNot { it.fixedTaskId == fixedTaskId && it.dayId == dayId }
                if (currentlyCompleted) {
                    without
                } else {
                    without + FixedTaskCompletion(
                        id = "ftc_${fixedTaskId}_$dayId",
                        fixedTaskId = fixedTaskId,
                        dayId = dayId,
                        completedAt = now,
                        userId = LOCAL_USER_ID
                    )
                }
            }
        }
    }

    // --- Contacts ---

    private fun Contact.toValues() = ContentValues().apply {
        put("id", id)
        put("name", name.trim().take(150))
        put("phone", phone.trim().take(40))
        put("active", if (isActive) 1 else 0)
        put("pinned", if (isPinned) 1 else 0)
        put("last_suggested", lastSuggested?.millis)
        put("created_at", createdAt?.millis ?: System.currentTimeMillis())
    }

    override suspend fun createContact(name: String, phone: String, isPinned: Boolean): Result<String> {
        val contact = Contact(
            id = newId("cnt"),
            name = name.trim().take(150),
            phone = phone.trim().take(40),
            isActive = true,
            isPinned = isPinned,
            userId = LOCAL_USER_ID,
            createdAt = Timestamp.now()
        )
        return write { db -> db.insertOrThrow("contacts", null, contact.toValues()) }.map {
            contacts.update { it.orEmpty() + contact }
            contact.id
        }
    }

    override suspend fun updateContact(contact: Contact): Result<Unit> =
        write { db -> db.update("contacts", contact.toValues(), "id = ?", arrayOf(contact.id)) }.map {
            contacts.update { list -> list.orEmpty().map { if (it.id == contact.id) contact else it } }
        }

    override suspend fun deleteContact(contactId: String): Result<Unit> =
        write { db -> db.delete("contacts", "id = ?", arrayOf(contactId)) }.map {
            contacts.update { list -> list.orEmpty().filterNot { it.id == contactId } }
        }

    override suspend fun restoreContact(contact: Contact): Result<Unit> =
        write { db ->
            db.insertWithOnConflict("contacts", null, contact.toValues(), SQLiteDatabase.CONFLICT_REPLACE)
        }.map {
            contacts.update { list -> list.orEmpty().filterNot { it.id == contact.id } + contact }
        }

    override suspend fun markContactSuggestedOrContacted(contactId: String): Result<Unit> {
        val now = Timestamp.now()
        return write { db ->
            db.update(
                "contacts",
                ContentValues().apply { put("last_suggested", now.millis) },
                "id = ?",
                arrayOf(contactId)
            )
        }.map {
            contacts.update { list ->
                list.orEmpty().map { if (it.id == contactId) it.copy(lastSuggested = now) else it }
            }
        }
    }

    override suspend fun togglePinnedContactCompletion(
        contactId: String,
        dayId: String,
        currentlyCompleted: Boolean
    ): Result<Unit> {
        val now = Timestamp.now()
        return write { db ->
            if (currentlyCompleted) {
                db.delete("contact_completions", "contact_id = ? AND day_id = ?", arrayOf(contactId, dayId))
            } else {
                db.insertWithOnConflict(
                    "contact_completions",
                    null,
                    ContentValues().apply {
                        put("contact_id", contactId)
                        put("day_id", dayId)
                        put("completed_at", now.millis)
                    },
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
        }.map {
            contactCompletions.update { list ->
                val without = list.orEmpty().filterNot { it.contactId == contactId && it.dayId == dayId }
                if (currentlyCompleted) {
                    without
                } else {
                    without + PinnedContactCompletion(
                        id = "pcc_${contactId}_$dayId",
                        contactId = contactId,
                        dayId = dayId,
                        completedAt = now,
                        userId = LOCAL_USER_ID
                    )
                }
            }
        }
    }

    // --- Quotes ---

    override suspend fun createQuote(text: String, author: String?): Result<String> {
        val quote = Quote(
            id = newId("qt"),
            text = text.trim().take(1000),
            author = author?.trim()?.take(150)?.takeIf { it.isNotBlank() },
            userId = LOCAL_USER_ID,
            createdAt = Timestamp.now()
        )
        return write { db ->
            db.insertOrThrow(
                "quotes",
                null,
                ContentValues().apply {
                    put("id", quote.id)
                    put("text", quote.text)
                    put("author", quote.author)
                    put("created_at", quote.createdAt?.millis)
                }
            )
        }.map {
            quotes.update { it.orEmpty() + quote }
            quote.id
        }
    }

    override suspend fun updateQuote(quoteId: String, text: String, author: String?): Result<Unit> {
        val cleanText = text.trim().take(1000)
        val cleanAuthor = author?.trim()?.take(150)?.takeIf { it.isNotBlank() }
        return write { db ->
            db.update(
                "quotes",
                ContentValues().apply {
                    put("text", cleanText)
                    put("author", cleanAuthor)
                },
                "id = ?",
                arrayOf(quoteId)
            )
        }.map {
            quotes.update { list ->
                list.orEmpty().map { if (it.id == quoteId) it.copy(text = cleanText, author = cleanAuthor) else it }
            }
        }
    }

    override suspend fun deleteQuote(quoteId: String): Result<Unit> =
        write { db -> db.delete("quotes", "id = ?", arrayOf(quoteId)) }.map {
            quotes.update { list -> list.orEmpty().filterNot { it.id == quoteId } }
        }

    override suspend fun restoreQuote(quote: Quote): Result<Unit> =
        write { db ->
            db.insertWithOnConflict(
                "quotes",
                null,
                ContentValues().apply {
                    put("id", quote.id)
                    put("text", quote.text)
                    put("author", quote.author)
                    put("created_at", quote.createdAt?.millis ?: System.currentTimeMillis())
                },
                SQLiteDatabase.CONFLICT_REPLACE
            )
        }.map {
            quotes.update { list -> list.orEmpty().filterNot { it.id == quote.id } + quote }
        }

    // --- Settings ---

    override suspend fun saveUserSettings(settings: UserSettings): Result<Unit> {
        val clean = settings.copy(id = LOCAL_USER_ID, userId = LOCAL_USER_ID)
        return write { db ->
            db.insertWithOnConflict(
                "kv",
                null,
                ContentValues().apply {
                    put("k", KEY_SETTINGS)
                    put("v", SettingsJson.toJson(clean))
                },
                SQLiteDatabase.CONFLICT_REPLACE
            )
        }.map { this.settings.value = clean }
    }

    // --- Goals ---

    private fun Goal.toValues() = ContentValues().apply {
        put("id", id)
        put("title", title.trim().take(300))
        put("description", description?.trim()?.take(2000)?.takeIf { it.isNotBlank() })
        put("timeframe", timeframeEnum.name)
        put("period_label", periodLabel.trim().ifBlank { "المرحلة الحالية" }.take(120))
        put("completed", if (effectiveIsCompleted()) 1 else 0)
        put("progress", effectiveProgress())
        put("milestones", milestonesToJson(milestones))
        put("created_at", createdAt?.millis ?: System.currentTimeMillis())
    }

    private fun normalizeMilestones(milestones: List<GoalMilestone>): List<GoalMilestone> =
        milestones.mapIndexed { index, ms ->
            ms.copy(id = ms.id.ifBlank { "ms_${index + 1}" }, title = ms.title.trim().take(200))
        }.filter { it.title.isNotBlank() }.take(50)

    private suspend fun saveGoal(goal: Goal): Result<Unit> {
        val stored = goal.copy(
            isCompleted = goal.effectiveIsCompleted(),
            progress = goal.effectiveProgress()
        )
        return write { db ->
            db.insertWithOnConflict("goals", null, stored.toValues(), SQLiteDatabase.CONFLICT_REPLACE)
        }.map {
            goals.update { list -> list.orEmpty().filterNot { it.id == stored.id } + stored }
        }
    }

    override suspend fun createGoal(
        title: String,
        description: String?,
        timeframe: GoalTimeframe,
        periodLabel: String,
        progress: Int,
        milestones: List<GoalMilestone>
    ): Result<String> {
        val goal = Goal(
            id = newId("gl"),
            title = title.trim(),
            description = description?.trim()?.takeIf { it.isNotBlank() },
            timeframe = timeframe.name,
            periodLabel = periodLabel.trim().ifBlank { "المرحلة الحالية" },
            isCompleted = progress >= 100,
            progress = progress.coerceIn(0, 100),
            milestones = normalizeMilestones(milestones),
            createdAt = Timestamp.now(),
            userId = LOCAL_USER_ID
        )
        return saveGoal(goal).map { goal.id }
    }

    override suspend fun updateGoal(goal: Goal): Result<Unit> =
        saveGoal(goal.copy(milestones = normalizeMilestones(goal.milestones)))

    override suspend fun toggleGoalCompleted(goal: Goal, isCompleted: Boolean): Result<Unit> {
        val progress = when {
            isCompleted -> 100
            goal.milestones.isNotEmpty() -> 0
            else -> goal.progress.takeIf { it < 100 } ?: 0
        }
        return saveGoal(
            goal.copy(
                isCompleted = isCompleted,
                progress = progress,
                milestones = goal.milestones.map { it.copy(isDone = isCompleted) }
            )
        )
    }

    override suspend fun toggleGoalMilestone(goal: Goal, milestoneId: String): Result<Unit> =
        saveGoal(
            goal.copy(
                milestones = goal.milestones.map { if (it.id == milestoneId) it.copy(isDone = !it.isDone) else it }
            )
        )

    override suspend fun updateGoalProgress(goal: Goal, newProgress: Int): Result<Unit> {
        val clamped = newProgress.coerceIn(0, 100)
        return saveGoal(goal.copy(progress = clamped, isCompleted = clamped >= 100))
    }

    override suspend fun deleteGoal(goalId: String): Result<Unit> =
        write { db -> db.delete("goals", "id = ?", arrayOf(goalId)) }.map {
            goals.update { list -> list.orEmpty().filterNot { it.id == goalId } }
        }

    private companion object {
        const val KEY_SETTINGS = "settings"
        const val WEEK_MILLIS = 7L * 24L * 3600L * 1000L

        fun <T> SQLiteDatabase.queryAll(table: String, read: (Cursor) -> T): List<T> =
            rawQuery("SELECT * FROM $table", null).use { cursor ->
                val out = ArrayList<T>(cursor.count)
                while (cursor.moveToNext()) out += read(cursor)
                out
            }

        fun Cursor.getStringOrNull(index: Int): String? = if (isNull(index)) null else getString(index)

        fun milestonesToJson(milestones: List<GoalMilestone>): String {
            val array = JSONArray()
            for (ms in milestones) {
                array.put(
                    JSONObject().apply {
                        put("id", ms.id)
                        put("title", ms.title)
                        put("isDone", ms.isDone)
                    }
                )
            }
            return array.toString()
        }

        fun milestonesFromJson(raw: String?): List<GoalMilestone> {
            if (raw.isNullOrBlank()) return emptyList()
            return try {
                val array = JSONArray(raw)
                List(array.length()) { index ->
                    val obj = array.getJSONObject(index)
                    GoalMilestone(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        isDone = obj.optBoolean("isDone")
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
