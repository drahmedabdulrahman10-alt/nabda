package io.github.drahmedabdulrahman10_alt.nabda.data.model

import java.util.Date

enum class TaskPriority(val labelAr: String, val sortOrder: Int) {
    HIGH("مرتفعة", 0),
    MEDIUM("متوسطة", 1),
    LOW("منخفضة", 2);

    companion object {
        fun fromString(value: String?): TaskPriority {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}

data class Week(
    val id: String = "",
    val name: String = "",
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val reflection: String? = null,
    val userId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class Day(
    val id: String = "",
    val weekId: String = "",
    val date: Timestamp? = null,
    val gregorianDateStr: String = "",
    val hijriDateStr: String = "",
    val userId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class DailyTask(
    val id: String = "",
    val dayId: String = "",
    val title: String = "",
    val priority: String = TaskPriority.MEDIUM.name,
    val isCompleted: Boolean = false,
    val movedFromDayId: String? = null,
    val userId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val priorityEnum: TaskPriority
        get() = TaskPriority.fromString(priority)
}

data class FixedTask(
    val id: String = "",
    val title: String = "",
    val order: Int = 0,
    val missedDaysThreshold: Int = 5,
    val isActive: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val userId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    /** Days of the week the task repeats on: bit 0 = Saturday … bit 6 = Friday. */
    val weekDays: Int = EVERY_DAY
) {
    val isEveryDay: Boolean get() = weekDays and EVERY_DAY == EVERY_DAY

    fun isScheduledOn(date: Date): Boolean =
        weekDays and (1 shl weekDayIndex(date)) != 0

    companion object {
        const val EVERY_DAY = 0b1111111

        /** Index of [date] in the app's Saturday-first week (Saturday = 0 … Friday = 6). */
        fun weekDayIndex(date: Date): Int {
            val cal = java.util.Calendar.getInstance().apply { time = date }
            return cal.get(java.util.Calendar.DAY_OF_WEEK) % 7
        }
    }
}

data class FixedTaskCompletion(
    val id: String = "",
    val fixedTaskId: String = "",
    val dayId: String = "",
    val completedAt: Timestamp? = null,
    val userId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class Contact(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val isActive: Boolean = true,
    val isPinned: Boolean = false,
    val lastSuggested: Timestamp? = null,
    val userId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class PinnedContactCompletion(
    val id: String = "",
    val contactId: String = "",
    val dayId: String = "",
    val completedAt: Timestamp? = null,
    val userId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class Quote(
    val id: String = "",
    val text: String = "",
    val author: String? = null,
    val userId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class UserSettings(
    val id: String = "",
    val userId: String = "",
    /** "SYSTEM", "LIGHT" or "DARK". */
    val themeMode: String = "SYSTEM",
    val quoteNotificationsEnabled: Boolean = true,
    val useRandomTimeForQuotes: Boolean = false,
    val quoteNotificationHour: Int = 8,
    val quoteNotificationMinute: Int = 0,
    val quoteRandomStartHour: Int = 7,
    val quoteRandomEndHour: Int = 9,
    val contactNotificationsEnabled: Boolean = true,
    val useRandomTimeForContacts: Boolean = false,
    val contactNotificationHour: Int = 19,
    val contactNotificationMinute: Int = 0,
    val contactRandomStartHour: Int = 19,
    val contactRandomEndHour: Int = 20,
    val habitAlertsEnabled: Boolean = true,
    val aiProvider: String = "GEMINI",
    val geminiApiKey: String = "",
    val openRouterApiKey: String = "",
    val aiModelName: String = "gemini-2.5-flash",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun normalizedProvider(): String {
        return if (aiProvider.equals("OPENROUTER", ignoreCase = true)) "OPENROUTER" else "GEMINI"
    }
}

enum class GoalTimeframe(
    val labelAr: String,
    val tabLabelAr: String,
    val sortOrder: Int
) {
    YEARLY("أهداف سنوية", "أهداف السنة", 0),
    QUARTERLY("أهداف مرحلية / موديول", "أهداف المرحلة/الموديول", 1),
    MONTHLY("أهداف شهرية", "أهداف الشهر", 2);

    companion object {
        fun fromString(value: String?): GoalTimeframe {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MONTHLY
        }
    }
}

data class GoalMilestone(
    val id: String = "",
    val title: String = "",
    val isDone: Boolean = false
)

data class Goal(
    val id: String = "",
    val title: String = "",
    val description: String? = null,
    val timeframe: String = GoalTimeframe.MONTHLY.name,
    val periodLabel: String = "",
    val isCompleted: Boolean = false,
    val progress: Int = 0,
    val milestones: List<GoalMilestone> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val userId: String = ""
) {
    val timeframeEnum: GoalTimeframe
        get() = GoalTimeframe.fromString(timeframe)

    fun effectiveProgress(): Int {
        return if (milestones.isNotEmpty()) {
            val doneCount = milestones.count { it.isDone }
            ((doneCount.toFloat() / milestones.size.toFloat()) * 100f).toInt().coerceIn(0, 100)
        } else if (isCompleted) {
            100
        } else {
            progress.coerceIn(0, 100)
        }
    }

    fun effectiveIsCompleted(): Boolean {
        return if (milestones.isNotEmpty()) {
            milestones.all { it.isDone }
        } else {
            isCompleted || progress >= 100
        }
    }
}

object NabdaDefaults {
    val defaultQuotes: List<Pair<String, String>> = listOf(
        "﴿ أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ ﴾" to "سورة الرعد - آية ٢٨",
        "«أَحَبُّ الأَعْمَالِ إِلَى اللَّهِ أَدْوَمُهَا وَإِنْ قَلَّ»" to "حديث شريف - متفق عليه",
        "«مَنْ أَحَبَّ أَنْ يُبْسَطَ لَهُ فِي رِزْقِهِ، وَيُنْسَأَ لَهُ فِي أَثَرِهِ، فَلْيَصِلْ رَحِمَهُ»" to "حديث شريف - متفق عليه",
        "﴿ وَأَن لَّيْسَ لِلْإِنسَانِ إِلَّا مَا سَعَىٰ * وَأَنَّ سَعْيَهُ سَوْفَ يُرَىٰ ﴾" to "سورة النجم",
        "ما قُرِنَ شيءٌ إلى شيءٍ أحسن من حِلمٍ إلى عِلم، ومن عفوٍ إلى قُدرة." to "عمر بن عبد العزيز",
        "الوقت أنفس ما عنيت بحفظه، وأراه أسهل ما عليك يضيع." to "ابن هبيرة",
        "﴿ فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ ﴾" to "سورة البقرة - آية ١٥٢"
    )

    val defaultHabits: List<Triple<String, Int, Int>> = listOf(
        Triple("أذكار الصباح", 7, 0),
        Triple("ورد القرآن الكريم اليومي", 6, 30),
        Triple("صلاة الضحى", 9, 0),
        Triple("أذكار المساء", 17, 30),
        Triple("الوتر وقراءة الملك قبل النوم", 22, 0)
    )
}
