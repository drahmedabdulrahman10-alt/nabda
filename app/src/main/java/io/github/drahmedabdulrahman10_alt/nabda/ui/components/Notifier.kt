package io.github.drahmedabdulrahman10_alt.nabda.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Shows short messages at the bottom of the app, optionally with a "تراجع" action so that
 * deletes and moves can be undone instead of confirmed up front.
 */
class Notifier(
    private val hostState: SnackbarHostState,
    private val scope: CoroutineScope
) {
    fun show(message: String, onUndo: (() -> Unit)? = null) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            val result = hostState.showSnackbar(
                message = message,
                actionLabel = if (onUndo != null) "تراجع" else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onUndo?.invoke()
        }
    }
}

val LocalNotifier = staticCompositionLocalOf<Notifier> { error("Notifier is not provided") }
