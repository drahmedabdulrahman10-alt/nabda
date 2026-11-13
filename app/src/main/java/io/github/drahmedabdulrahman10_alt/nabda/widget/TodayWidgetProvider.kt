package io.github.drahmedabdulrahman10_alt.nabda.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import io.github.drahmedabdulrahman10_alt.nabda.MainActivity
import io.github.drahmedabdulrahman10_alt.nabda.R
import io.github.drahmedabdulrahman10_alt.nabda.data.repository.LOCAL_USER_ID
import io.github.drahmedabdulrahman10_alt.nabda.di.RepositoryModule
import io.github.drahmedabdulrahman10_alt.nabda.util.ArabicCalendarUtils
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Home-screen widget with today's unfinished tasks. Tapping it opens the app; the + button opens
 * it straight on the quick-add field. Plain RemoteViews, so no extra library is needed.
 */
class TodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                update(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_QUICK_ADD = "io.github.drahmedabdulrahman10_alt.nabda.ACTION_QUICK_ADD"

        private val taskRows = intArrayOf(
            R.id.widget_task_1,
            R.id.widget_task_2,
            R.id.widget_task_3,
            R.id.widget_task_4,
            R.id.widget_task_5,
            R.id.widget_task_6
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

            val views = RemoteViews(context.packageName, R.layout.widget_today)
            val date = ArabicCalendarUtils.formatGregorianArabic(now)
            views.setTextViewText(
                R.id.widget_subtitle,
                if (tasks.isEmpty()) date else "$date • $done من ${tasks.size}"
            )

            taskRows.forEachIndexed { index, rowId ->
                val task = remaining.getOrNull(index)
                val isLastRowWithMore = index == taskRows.lastIndex && remaining.size > taskRows.size
                when {
                    isLastRowWithMore -> {
                        views.setTextViewText(rowId, "و${remaining.size - index} مهام أخرى…")
                        views.setViewVisibility(rowId, View.VISIBLE)
                    }
                    task != null -> {
                        views.setTextViewText(rowId, "○  ${task.title}")
                        views.setViewVisibility(rowId, View.VISIBLE)
                    }
                    else -> views.setViewVisibility(rowId, View.GONE)
                }
            }
            views.setTextViewText(
                R.id.widget_empty,
                if (tasks.isEmpty()) "لا مهام اليوم بعد" else "أنجزت كل مهام اليوم"
            )
            views.setViewVisibility(R.id.widget_empty, if (remaining.isEmpty()) View.VISIBLE else View.GONE)

            views.setOnClickPendingIntent(R.id.widget_root, openApp(context, requestCode = 0, action = null))
            views.setOnClickPendingIntent(R.id.widget_add, openApp(context, requestCode = 1, action = ACTION_QUICK_ADD))
            manager.updateAppWidget(ids, views)
        }

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
