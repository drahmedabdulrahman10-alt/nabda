package io.github.drahmedabdulrahman10_alt.nabda.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.drahmedabdulrahman10_alt.nabda.MainActivity
import io.github.drahmedabdulrahman10_alt.nabda.R
import io.github.drahmedabdulrahman10_alt.nabda.data.model.FixedTask
import io.github.drahmedabdulrahman10_alt.nabda.data.model.NabdaDefaults
import io.github.drahmedabdulrahman10_alt.nabda.data.model.UserSettings
import io.github.drahmedabdulrahman10_alt.nabda.data.repository.LOCAL_USER_ID
import io.github.drahmedabdulrahman10_alt.nabda.di.RepositoryModule
import io.github.drahmedabdulrahman10_alt.nabda.util.ArabicCalendarUtils
import java.util.Calendar
import java.util.Date
import kotlin.random.Random
import kotlinx.coroutines.flow.first

/**
 * Local reminders. Each reminder is a one-shot alarm; when it fires, the receiver builds the
 * notification from the data as it is at that moment and then sets the next alarm. Alarms are
 * also set again at app start, after a reboot and whenever the settings or daily tasks change.
 */
object NabdaNotificationEngine {

    const val CHANNEL_QUOTES = "quotes"
    const val CHANNEL_CONTACTS = "contacts"
    const val CHANNEL_HABITS = "habits"
    const val CHANNEL_FIXED_TASK_REMINDERS = "fixed_task_reminders"

    private const val PREFIX = "io.github.drahmedabdulrahman10_alt.nabda"
    const val ACTION_DAILY_QUOTE = "$PREFIX.ACTION_DAILY_QUOTE"
    const val ACTION_DAILY_CONTACT = "$PREFIX.ACTION_DAILY_CONTACT"
    const val ACTION_HABIT_CHECK = "$PREFIX.ACTION_HABIT_CHECK"
    const val ACTION_FIXED_TASK_REMINDER = "$PREFIX.ACTION_FIXED_TASK_REMINDER"

    val reminderActions = setOf(
        ACTION_DAILY_QUOTE,
        ACTION_DAILY_CONTACT,
        ACTION_HABIT_CHECK,
        ACTION_FIXED_TASK_REMINDER
    )

    private const val EXTRA_TASK_ID = "extra_task_id"
    private const val EXTRA_SLOT = "extra_slot"

    private const val REQUEST_QUOTE = 1001
    private const val REQUEST_CONTACT = 1002
    private const val REQUEST_HABIT_CHECK = 1003
    private const val REQUEST_FIXED_BASE = 2000

    private const val PREFS = "nabda_reminders"
    private const val KEY_FIXED_CODES = "fixed_codes"

    fun ensureChannelsCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_QUOTES, "حكمة اليوم", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_CONTACTS, "تذكير صلة الرحم", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_HABITS, "متابعة المهام اليومية", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(
                    CHANNEL_FIXED_TASK_REMINDERS,
                    "مواعيد المهام اليومية",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        )
    }

    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun postNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String
    ) {
        ensureChannelsCreated(context)
        if (!hasNotificationPermission(context)) return

        val openApp = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (ignored: SecurityException) {
        }
    }

    /** Builds and shows the notification for an alarm that has just fired. */
    suspend fun deliver(context: Context, action: String, intent: Intent) {
        val repository = RepositoryModule.provideNabdaRepository(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val todayKey = ArabicCalendarUtils.formatDateKey(Date())
        val todayId = ArabicCalendarUtils.buildDayId(LOCAL_USER_ID, Date())
        val slot = intent.getStringExtra(EXTRA_SLOT)
        if (slot != null) prefs.edit().putString("fired_$slot", todayKey).remove("next_$slot").apply()

        when (action) {
            ACTION_DAILY_QUOTE -> {
                val quotes = repository.observeQuotes(LOCAL_USER_ID).first()
                val (text, author) = quotes.randomOrNull()?.let { it.text to it.author }
                    ?: NabdaDefaults.defaultQuotes.random()
                postNotification(
                    context,
                    CHANNEL_QUOTES,
                    REQUEST_QUOTE,
                    "حكمة اليوم",
                    if (author.isNullOrBlank()) text else "$text — $author"
                )
            }
            ACTION_DAILY_CONTACT -> {
                val next = repository.observeContacts(LOCAL_USER_ID).first()
                    .filter { it.isActive && !it.isPinned }
                    .minByOrNull { it.lastSuggested?.millis ?: 0L }
                    ?: return
                postNotification(
                    context,
                    CHANNEL_CONTACTS,
                    REQUEST_CONTACT,
                    "صلة الرحم",
                    "اليوم فرصة طيبة للاتصال بـ «${next.name}» والاطمئنان عليه."
                )
            }
            ACTION_HABIT_CHECK -> {
                val done = repository.observeFixedTaskCompletions(LOCAL_USER_ID).first()
                    .filter { it.dayId == todayId }
                    .mapTo(HashSet()) { it.fixedTaskId }
                val pending = repository.observeFixedTasks(LOCAL_USER_ID).first()
                    .filter { it.isActive && it.isScheduledOn(Date()) && it.id !in done }
                if (pending.isEmpty()) return
                postNotification(
                    context,
                    CHANNEL_HABITS,
                    REQUEST_HABIT_CHECK,
                    "باقٍ لك اليوم ${pending.size} من مهامك اليومية",
                    pending.joinToString("، ") { it.title }
                )
            }
            ACTION_FIXED_TASK_REMINDER -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
                val task = repository.observeFixedTasks(LOCAL_USER_ID).first()
                    .firstOrNull { it.id == taskId && it.isActive && it.reminderEnabled && it.isScheduledOn(Date()) }
                    ?: return
                val alreadyDone = repository.observeFixedTaskCompletions(LOCAL_USER_ID).first()
                    .any { it.fixedTaskId == taskId && it.dayId == todayId }
                if (alreadyDone) return
                postNotification(
                    context,
                    CHANNEL_FIXED_TASK_REMINDERS,
                    requestCodeFor(task),
                    task.title,
                    "حان وقت «${task.title}»."
                )
            }
        }
    }

    /** Sets (or cancels) every reminder alarm to match the current settings and daily tasks. */
    suspend fun rescheduleAll(context: Context) {
        ensureChannelsCreated(context)
        val repository = RepositoryModule.provideNabdaRepository(context)
        val settings = repository.observeUserSettings(LOCAL_USER_ID).first() ?: UserSettings()
        val fixedTasks = repository.observeFixedTasks(LOCAL_USER_ID).first()

        scheduleSlot(
            context = context,
            slot = "quote",
            enabled = settings.quoteNotificationsEnabled,
            requestCode = REQUEST_QUOTE,
            action = ACTION_DAILY_QUOTE,
            random = settings.useRandomTimeForQuotes,
            hour = settings.quoteNotificationHour,
            minute = settings.quoteNotificationMinute,
            randomStart = settings.quoteRandomStartHour,
            randomEnd = settings.quoteRandomEndHour
        )
        scheduleSlot(
            context = context,
            slot = "contact",
            enabled = settings.contactNotificationsEnabled,
            requestCode = REQUEST_CONTACT,
            action = ACTION_DAILY_CONTACT,
            random = settings.useRandomTimeForContacts,
            hour = settings.contactNotificationHour,
            minute = settings.contactNotificationMinute,
            randomStart = settings.contactRandomStartHour,
            randomEnd = settings.contactRandomEndHour
        )
        scheduleSlot(
            context = context,
            slot = "habit_check",
            enabled = settings.habitAlertsEnabled && fixedTasks.any { it.isActive },
            requestCode = REQUEST_HABIT_CHECK,
            action = ACTION_HABIT_CHECK,
            random = false,
            hour = 20,
            minute = 30,
            randomStart = 0,
            randomEnd = 0
        )

        // Per-task reminders: remember which alarms exist so removed tasks get theirs cancelled.
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previousCodes = prefs.getStringSet(KEY_FIXED_CODES, emptySet()).orEmpty()
        val currentCodes = HashSet<String>()
        for (task in fixedTasks) {
            if (!task.isActive || !task.reminderEnabled) continue
            val code = requestCodeFor(task)
            currentCodes += code.toString()
            scheduleSlot(
                context = context,
                slot = "fixed_$code",
                enabled = true,
                requestCode = code,
                action = ACTION_FIXED_TASK_REMINDER,
                random = false,
                hour = task.reminderHour,
                minute = task.reminderMinute,
                randomStart = 0,
                randomEnd = 0,
                taskId = task.id
            )
        }
        for (stale in previousCodes - currentCodes) {
            val code = stale.toIntOrNull() ?: continue
            scheduleSlot(context, "fixed_$code", false, code, ACTION_FIXED_TASK_REMINDER, false, 0, 0, 0, 0)
        }
        prefs.edit().putStringSet(KEY_FIXED_CODES, currentCodes).apply()
    }

    private fun requestCodeFor(task: FixedTask): Int = REQUEST_FIXED_BASE + (task.id.hashCode() and 0xFFFFF)

    private fun scheduleSlot(
        context: Context,
        slot: String,
        enabled: Boolean,
        requestCode: Int,
        action: String,
        random: Boolean,
        hour: Int,
        minute: Int,
        randomStart: Int,
        randomEnd: Int,
        taskId: String? = null
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, NabdaNotificationReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_SLOT, slot)
                if (taskId != null) putExtra(EXTRA_TASK_ID, taskId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (!enabled) {
            alarmManager.cancel(pendingIntent)
            prefs.edit().remove("next_$slot").remove("sig_$slot").apply()
            return
        }

        // Reuse the time already chosen for this slot while its settings are unchanged, so a
        // random-window reminder is not rolled again (and possibly fired twice) on every start.
        val signature = "$random:$hour:$minute:$randomStart:$randomEnd"
        val now = System.currentTimeMillis()
        val stored = prefs.getLong("next_$slot", 0L)
        val triggerAt = if (stored > now && prefs.getString("sig_$slot", null) == signature) {
            stored
        } else {
            val firedToday = prefs.getString("fired_$slot", null) == ArabicCalendarUtils.formatDateKey(Date())
            val cal = Calendar.getInstance()
            if (random) {
                val start = minOf(randomStart, randomEnd).coerceIn(0, 23)
                val end = maxOf(randomStart, randomEnd).coerceIn(0, 23)
                val minutes = Random.nextInt(start * 60, maxOf(end * 60, start * 60 + 1))
                cal.set(Calendar.HOUR_OF_DAY, minutes / 60)
                cal.set(Calendar.MINUTE, minutes % 60)
            } else {
                cal.set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
                cal.set(Calendar.MINUTE, minute.coerceIn(0, 59))
            }
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            if (firedToday || cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
            prefs.edit().putLong("next_$slot", cal.timeInMillis).putString("sig_$slot", signature).apply()
            cal.timeInMillis
        }

        // Exact when the system allows it (USE_EXACT_ALARM on 13+, SCHEDULE_EXACT_ALARM on 12),
        // otherwise inexact, which Android may delay by up to an hour.
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (ignored: SecurityException) {
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } catch (alsoIgnored: SecurityException) {
            }
        }
    }
}
