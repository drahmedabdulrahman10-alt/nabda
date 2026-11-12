package io.github.drahmedabdulrahman10_alt.nabda.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives the reminder alarms and the system events (boot, app update, clock change) after
 * which alarms must be set again.
 */
class NabdaNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                if (action in NabdaNotificationEngine.reminderActions) {
                    NabdaNotificationEngine.deliver(appContext, action, intent)
                }
                NabdaNotificationEngine.rescheduleAll(appContext)
            } catch (e: Exception) {
                android.util.Log.w("NabdaReminders", "Failed handling $action", e)
            } finally {
                pending.finish()
            }
        }
    }
}
