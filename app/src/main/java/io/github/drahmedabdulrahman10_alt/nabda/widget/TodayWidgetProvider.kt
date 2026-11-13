package io.github.drahmedabdulrahman10_alt.nabda.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.view.View
import android.widget.RemoteViews
import io.github.drahmedabdulrahman10_alt.nabda.MainActivity
import io.github.drahmedabdulrahman10_alt.nabda.R
import io.github.drahmedabdulrahman10_alt.nabda.data.model.DailyTask
import io.github.drahmedabdulrahman10_alt.nabda.data.model.TaskPriority
import io.github.drahmedabdulrahman10_alt.nabda.data.repository.LOCAL_USER_ID
import io.github.drahmedabdulrahman10_alt.nabda.di.RepositoryModule
import io.github.drahmedabdulrahman10_alt.nabda.util.ArabicCalendarUtils
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Home-screen widget with today's tasks. Tapping a task marks it done (or not done again);
 * unfinished tasks come first, finished ones follow struck through. The + button opens the app on
 * the quick-add field and the header opens the app. Plain RemoteViews, so no extra library.
 */
class TodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        runAsync { update(context.applicationContext) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TOGGLE_TASK) {
            super.onReceive(context, intent)
            return
        }
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val appContext = context.applicationContext
        runAsync {
            val repository = RepositoryModule.provideNabdaRepository(appContext)
            val task = repository.observeTasks(LOCAL_USER_ID).first().firstOrNull { it.id == taskId }
            if (task != null) repository.toggleTaskCompleted(task.id, !task.isCompleted)
            update(appContext)
        }
    }

    private fun runAsync(block: suspend () -> Unit) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                block()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_QUICK_ADD = "io.github.drahmedabdulrahman10_alt.nabda.ACTION_QUICK_ADD"
        private const val ACTION_TOGGLE_TASK = "io.github.drahmedabdulrahman10_alt.nabda.ACTION_WIDGET_TOGGLE"
        private const val EXTRA_TASK_ID = "task_id"

        /** One widget line: the tappable row, the task text and the tag/time beside it. */
        private class Row(val row: Int, val text: Int, val tag: Int, val dot: Int)

        private val rows = listOf(
            Row(R.id.widget_row_1, R.id.widget_task_1, R.id.widget_tag_1, R.id.widget_dot_1),
            Row(R.id.widget_row_2, R.id.widget_task_2, R.id.widget_tag_2, R.id.widget_dot_2),
            Row(R.id.widget_row_3, R.id.widget_task_3, R.id.widget_tag_3, R.id.widget_dot_3),
            Row(R.id.widget_row_4, R.id.widget_task_4, R.id.widget_tag_4, R.id.widget_dot_4),
            Row(R.id.widget_row_5, R.id.widget_task_5, R.id.widget_tag_5, R.id.widget_dot_5),
            Row(R.id.widget_row_6, R.id.widget_task_6, R.id.widget_tag_6, R.id.widget_dot_6)
        )

        /** Redraws every placed widget from the current data. Cheap no-op when none is placed. */
        suspend fun update(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TodayWidgetProvider::class.java))
            if (ids.isEmpty()) return

            val now = Date()
            val todayId = ArabicCalendarUtils.buildDayId(LOCAL_USER_ID, now)
            val tasks = RepositoryModule.provideNabdaRepository(context)
                .observeTasks(LOCAL_USER_ID).first()
                .filter { it.dayId == todayId }
                .sortedBy { it.sortKey }
            val remaining = tasks.filterNot { it.isCompleted }
            val done = tasks.size - remaining.size
            // Unfinished first, then finished, so the rows that matter are never pushed out.
            val shown = remaining + tasks.filter { it.isCompleted }

            val views = RemoteViews(context.packageName, R.layout.widget_today)
            val date = ArabicCalendarUtils.formatGregorianArabic(now)
            views.setTextViewText(
                R.id.widget_subtitle,
                if (tasks.isEmpty()) date else "$date • $done من ${tasks.size}"
            )

            val textColor = context.getColor(R.color.widget_text)
            val secondary = context.getColor(R.color.widget_text_secondary)
            val hiddenUnfinished = remaining.size - rows.size
            rows.forEachIndexed { index, row ->
                val task = shown.getOrNull(index)
                when {
                    index == rows.lastIndex && hiddenUnfinished > 0 -> {
                        // More unfinished tasks than rows: the last row opens the app instead.
                        views.setTextViewText(row.text, "و${hiddenUnfinished + 1} مهام أخرى…")
                        views.setTextColor(row.text, secondary)
                        views.setViewVisibility(row.tag, View.GONE)
                        views.setViewVisibility(row.dot, View.GONE)
                        views.setViewVisibility(row.row, View.VISIBLE)
                        views.setOnClickPendingIntent(row.row, openApp(context, requestCode = 0, action = null))
                    }
                    task != null -> {
                        bindTask(context, views, row, task, textColor, secondary)
                        views.setViewVisibility(row.row, View.VISIBLE)
                        views.setOnClickPendingIntent(row.row, toggle(context, requestCode = 100 + index, taskId = task.id))
                    }
                    else -> views.setViewVisibility(row.row, View.GONE)
                }
            }
            views.setViewVisibility(R.id.widget_empty, if (tasks.isEmpty()) View.VISIBLE else View.GONE)

            views.setOnClickPendingIntent(R.id.widget_root, openApp(context, requestCode = 0, action = null))
            views.setOnClickPendingIntent(R.id.widget_add, openApp(context, requestCode = 1, action = ACTION_QUICK_ADD))
            manager.updateAppWidget(ids, views)
        }

        /**
         * "○ title" with the same cues the app shows: a priority dot (an image, because some launchers
         * force widget text to one colour), «مهمة» or «لاحقًا», and the reminder time.
         */
        private fun bindTask(context: Context, views: RemoteViews, row: Row, task: DailyTask, textColor: Int, secondary: Int) {
            if (task.isCompleted) {
                val line = SpannableStringBuilder("✓  ")
                val start = line.length
                line.append(task.title)
                line.setSpan(StrikethroughSpan(), start, line.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                views.setTextViewText(row.text, line)
                views.setTextColor(row.text, secondary)
                views.setViewVisibility(row.tag, View.GONE)
                views.setViewVisibility(row.dot, View.GONE)
                return
            }
            views.setTextViewText(row.text, "○  ${task.title}")
            views.setTextColor(row.text, textColor)
            views.setImageViewResource(
                row.dot,
                when (task.priorityEnum) {
                    TaskPriority.HIGH -> R.drawable.widget_dot_high
                    TaskPriority.MEDIUM -> R.drawable.widget_dot_medium
                    TaskPriority.LOW -> R.drawable.widget_dot_low
                }
            )
            views.setViewVisibility(row.dot, View.VISIBLE)

            val label = when (task.priorityEnum) {
                TaskPriority.HIGH -> "مهمة"
                TaskPriority.LOW -> "لاحقًا"
                TaskPriority.MEDIUM -> null
            }
            val time = task.reminderMinuteOfDay?.let { formatMinuteOfDay(it) }
            val tag = listOfNotNull(label, time).joinToString(" · ")
            if (tag.isEmpty()) {
                views.setViewVisibility(row.tag, View.GONE)
                return
            }
            views.setTextViewText(row.tag, tag)
            views.setTextColor(
                row.tag,
                when (task.priorityEnum) {
                    TaskPriority.HIGH -> context.getColor(R.color.widget_high)
                    TaskPriority.LOW -> secondary
                    TaskPriority.MEDIUM -> context.getColor(R.color.widget_accent)
                }
            )
            views.setViewVisibility(row.tag, View.VISIBLE)
        }

        private fun formatMinuteOfDay(minuteOfDay: Int): String {
            val hour = minuteOfDay / 60
            val hour12 = if (hour % 12 == 0) 12 else hour % 12
            return "%d:%02d %s".format(Locale.US, hour12, minuteOfDay % 60, if (hour < 12) "ص" else "م")
        }

        private fun toggle(context: Context, requestCode: Int, taskId: String): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, TodayWidgetProvider::class.java).apply {
                    action = ACTION_TOGGLE_TASK
                    putExtra(EXTRA_TASK_ID, taskId)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        private fun openApp(context: Context, requestCode: Int, action: String?): PendingIntent =
            PendingIntent.getActivity(
                context,
                requestCode,
                Intent(context, MainActivity::class.java).apply {
                    this.action = action
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
    }
}
