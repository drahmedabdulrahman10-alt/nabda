package io.github.drahmedabdulrahman10_alt.nabda.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.github.drahmedabdulrahman10_alt.nabda.data.model.FixedTask
import io.github.drahmedabdulrahman10_alt.nabda.data.model.NabdaDefaults
import io.github.drahmedabdulrahman10_alt.nabda.data.model.UserSettings
import io.github.drahmedabdulrahman10_alt.nabda.util.ArabicCalendarUtils
import java.util.Date
import org.json.JSONObject

/**
 * The on-device database. Column order in each table is the order the repository reads them in,
 * so new columns must be appended at the end.
 */
class NabdaDatabase(context: Context) : SQLiteOpenHelper(context, "nabda.db", null, 4) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.enableWriteAheadLogging()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE tasks (id TEXT PRIMARY KEY, day_id TEXT NOT NULL, title TEXT NOT NULL, " +
                "priority TEXT NOT NULL, done INTEGER NOT NULL, moved_from TEXT, " +
                "created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, $REMIND_COLUMN, $ORDER_COLUMN)"
        )
        db.execSQL(
            "CREATE TABLE fixed_tasks (id TEXT PRIMARY KEY, title TEXT NOT NULL, ord INTEGER NOT NULL, " +
                "missed_threshold INTEGER NOT NULL, active INTEGER NOT NULL, reminder_enabled INTEGER NOT NULL, " +
                "reminder_hour INTEGER NOT NULL, reminder_minute INTEGER NOT NULL, start_date INTEGER NOT NULL, " +
                "created_at INTEGER NOT NULL, $WEEK_DAYS_COLUMN)"
        )
        db.execSQL(
            "CREATE TABLE fixed_task_completions (fixed_task_id TEXT NOT NULL, day_id TEXT NOT NULL, " +
                "completed_at INTEGER NOT NULL, PRIMARY KEY (fixed_task_id, day_id))"
        )
        db.execSQL(
            "CREATE TABLE contacts (id TEXT PRIMARY KEY, name TEXT NOT NULL, phone TEXT NOT NULL, " +
                "active INTEGER NOT NULL, pinned INTEGER NOT NULL, last_suggested INTEGER, " +
                "created_at INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE contact_completions (contact_id TEXT NOT NULL, day_id TEXT NOT NULL, " +
                "completed_at INTEGER NOT NULL, PRIMARY KEY (contact_id, day_id))"
        )
        db.execSQL("CREATE TABLE quotes (id TEXT PRIMARY KEY, text TEXT NOT NULL, author TEXT, created_at INTEGER NOT NULL)")
        db.execSQL(
            "CREATE TABLE goals (id TEXT PRIMARY KEY, title TEXT NOT NULL, description TEXT, " +
                "timeframe TEXT NOT NULL, period_label TEXT NOT NULL, completed INTEGER NOT NULL, " +
                "progress INTEGER NOT NULL, milestones TEXT NOT NULL, created_at INTEGER NOT NULL)"
        )
        db.execSQL("CREATE TABLE week_reflections (week_id TEXT PRIMARY KEY, reflection TEXT NOT NULL)")
        db.execSQL("CREATE TABLE kv (k TEXT PRIMARY KEY, v TEXT NOT NULL)")
        seedDefaults(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v2: daily tasks can repeat on chosen weekdays; existing ones keep repeating every day.
        if (oldVersion < 2) db.execSQL("ALTER TABLE fixed_tasks ADD COLUMN $WEEK_DAYS_COLUMN")
        // v3: optional reminder time on a task.
        if (oldVersion < 3) db.execSQL("ALTER TABLE tasks ADD COLUMN $REMIND_COLUMN")
        // v4: tasks can be reordered by hand; existing ones keep the order they were added in.
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN $ORDER_COLUMN")
            db.execSQL("UPDATE tasks SET ord = created_at")
        }
    }

    private companion object {
        const val WEEK_DAYS_COLUMN = "week_days INTEGER NOT NULL DEFAULT ${FixedTask.EVERY_DAY}"
        const val REMIND_COLUMN = "remind_minute INTEGER"
        const val ORDER_COLUMN = "ord INTEGER NOT NULL DEFAULT 0"
    }

    private fun seedDefaults(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        val today = ArabicCalendarUtils.startOfDay(Date()).time
        NabdaDefaults.defaultHabits.forEachIndexed { index, (title, hour, minute) ->
            db.insert(
                "fixed_tasks",
                null,
                ContentValues().apply {
                    put("id", "ft_default_${index + 1}")
                    put("title", title)
                    put("ord", index + 1)
                    put("missed_threshold", 5)
                    put("active", 1)
                    put("reminder_enabled", 1)
                    put("reminder_hour", hour)
                    put("reminder_minute", minute)
                    put("start_date", today)
                    put("created_at", now)
                }
            )
        }
        NabdaDefaults.defaultQuotes.forEachIndexed { index, (text, author) ->
            db.insert(
                "quotes",
                null,
                ContentValues().apply {
                    put("id", "qt_default_${index + 1}")
                    put("text", text)
                    put("author", author)
                    put("created_at", now - index)
                }
            )
        }
    }
}

/** Stores [UserSettings] as one JSON value so adding a setting needs no schema change. */
object SettingsJson {

    fun toJson(s: UserSettings): String = JSONObject().apply {
        put("themeMode", s.themeMode)
        put("colorTheme", s.colorTheme)
        put("quoteNotificationsEnabled", s.quoteNotificationsEnabled)
        put("useRandomTimeForQuotes", s.useRandomTimeForQuotes)
        put("quoteNotificationHour", s.quoteNotificationHour.coerceIn(0, 23))
        put("quoteNotificationMinute", s.quoteNotificationMinute.coerceIn(0, 59))
        put("quoteRandomStartHour", s.quoteRandomStartHour.coerceIn(0, 23))
        put("quoteRandomEndHour", s.quoteRandomEndHour.coerceIn(0, 23))
        put("contactNotificationsEnabled", s.contactNotificationsEnabled)
        put("useRandomTimeForContacts", s.useRandomTimeForContacts)
        put("contactNotificationHour", s.contactNotificationHour.coerceIn(0, 23))
        put("contactNotificationMinute", s.contactNotificationMinute.coerceIn(0, 59))
        put("contactRandomStartHour", s.contactRandomStartHour.coerceIn(0, 23))
        put("contactRandomEndHour", s.contactRandomEndHour.coerceIn(0, 23))
        put("habitAlertsEnabled", s.habitAlertsEnabled)
        put("aiProvider", s.normalizedProvider())
        put("geminiApiKey", SecretBox.encrypt(s.geminiApiKey.trim().take(500)))
        put("openRouterApiKey", SecretBox.encrypt(s.openRouterApiKey.trim().take(500)))
        put("aiModelName", s.aiModelName.trim().ifBlank { "gemini-2.5-flash" }.take(150))
    }.toString()

    /** True when [raw] still holds an API key written before keys were encrypted. */
    fun hasPlainSecrets(raw: String): Boolean {
        val o = try {
            JSONObject(raw)
        } catch (e: Exception) {
            return false
        }
        return listOf("geminiApiKey", "openRouterApiKey").any { name ->
            val value = o.optString(name, "")
            value.isNotEmpty() && !SecretBox.isEncrypted(value)
        }
    }

    fun fromJson(raw: String): UserSettings {
        val o = try {
            JSONObject(raw)
        } catch (e: Exception) {
            JSONObject()
        }
        val d = UserSettings()
        return UserSettings(
            id = "local",
            userId = "local",
            themeMode = o.optString("themeMode", d.themeMode),
            colorTheme = o.optString("colorTheme", d.colorTheme),
            quoteNotificationsEnabled = o.optBoolean("quoteNotificationsEnabled", d.quoteNotificationsEnabled),
            useRandomTimeForQuotes = o.optBoolean("useRandomTimeForQuotes", d.useRandomTimeForQuotes),
            quoteNotificationHour = o.optInt("quoteNotificationHour", d.quoteNotificationHour),
            quoteNotificationMinute = o.optInt("quoteNotificationMinute", d.quoteNotificationMinute),
            quoteRandomStartHour = o.optInt("quoteRandomStartHour", d.quoteRandomStartHour),
            quoteRandomEndHour = o.optInt("quoteRandomEndHour", d.quoteRandomEndHour),
            contactNotificationsEnabled = o.optBoolean("contactNotificationsEnabled", d.contactNotificationsEnabled),
            useRandomTimeForContacts = o.optBoolean("useRandomTimeForContacts", d.useRandomTimeForContacts),
            contactNotificationHour = o.optInt("contactNotificationHour", d.contactNotificationHour),
            contactNotificationMinute = o.optInt("contactNotificationMinute", d.contactNotificationMinute),
            contactRandomStartHour = o.optInt("contactRandomStartHour", d.contactRandomStartHour),
            contactRandomEndHour = o.optInt("contactRandomEndHour", d.contactRandomEndHour),
            habitAlertsEnabled = o.optBoolean("habitAlertsEnabled", d.habitAlertsEnabled),
            aiProvider = o.optString("aiProvider", d.aiProvider),
            geminiApiKey = SecretBox.decrypt(o.optString("geminiApiKey", "")),
            openRouterApiKey = SecretBox.decrypt(o.optString("openRouterApiKey", "")),
            aiModelName = o.optString("aiModelName", d.aiModelName)
        )
    }
}
