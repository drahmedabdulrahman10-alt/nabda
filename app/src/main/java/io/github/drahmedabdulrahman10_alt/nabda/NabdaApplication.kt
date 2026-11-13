package io.github.drahmedabdulrahman10_alt.nabda

import android.app.Application
import io.github.drahmedabdulrahman10_alt.nabda.data.repository.LOCAL_USER_ID
import io.github.drahmedabdulrahman10_alt.nabda.di.RepositoryModule
import io.github.drahmedabdulrahman10_alt.nabda.notifications.NabdaNotificationEngine
import io.github.drahmedabdulrahman10_alt.nabda.widget.TodayWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class NabdaApplication : Application() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Keep reminder alarms and the home-screen widget in step with the data: whenever the settings,
        // the daily tasks or the tasks change (and once at every start), set the alarms again and
        // redraw the widget.
        scope.launch {
            val repository = RepositoryModule.provideNabdaRepository(this@NabdaApplication)
            combine(
                repository.observeUserSettings(LOCAL_USER_ID),
                repository.observeFixedTasks(LOCAL_USER_ID),
                repository.observeTasks(LOCAL_USER_ID)
            ) { _, _, _ -> }.collectLatest {
                delay(500)
                NabdaNotificationEngine.rescheduleAll(this@NabdaApplication)
                TodayWidgetProvider.update(this@NabdaApplication)
            }
        }
    }
}
