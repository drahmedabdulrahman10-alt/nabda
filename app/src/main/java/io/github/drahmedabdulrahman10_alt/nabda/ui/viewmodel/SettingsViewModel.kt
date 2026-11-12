package io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.drahmedabdulrahman10_alt.nabda.data.ai.AiTaskAssistantService
import io.github.drahmedabdulrahman10_alt.nabda.data.model.UserSettings
import io.github.drahmedabdulrahman10_alt.nabda.data.repository.NabdaRepository
import io.github.drahmedabdulrahman10_alt.nabda.notifications.NabdaNotificationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: NabdaRepository,
    private val currentUserId: String,
    private val aiService: AiTaskAssistantService = AiTaskAssistantService()
) : ViewModel() {

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    private val _isTestingAiConnection = MutableStateFlow(false)
    val isTestingAiConnection: StateFlow<Boolean> = _isTestingAiConnection.asStateFlow()

    val settingsState: StateFlow<UiState<UserSettings>> = repository.observeUserSettings(currentUserId)
        .map<UserSettings?, UiState<UserSettings>> { settings ->
            UiState.Success(settings ?: UserSettings(id = currentUserId, userId = currentUserId))
        }
        .catch { e ->
            Log.w(TAG, "Error observing user settings", e)
            emit(UiState.Error(e.localizedMessage ?: "تعذّر تحميل الإعدادات"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    fun updateSettings(context: Context, updated: UserSettings) {
        viewModelScope.launch {
            val result = repository.saveUserSettings(updated)
            if (result.isSuccess) {
                _saveMessage.value = "تم حفظ الإعدادات"
            } else {
                _saveMessage.value = "تعذّر حفظ الإعدادات"
            }
        }
    }

    fun testAiConnection(draftSettings: UserSettings) {
        if (_isTestingAiConnection.value) return
        viewModelScope.launch {
            _isTestingAiConnection.value = true
            try {
                // Save current draft first so Firestore has the latest keys/provider
                repository.saveUserSettings(draftSettings)
                val result = aiService.testConnection(draftSettings)
                result.fold(
                    onSuccess = { msg ->
                        _saveMessage.value = msg
                    },
                    onFailure = { err ->
                        _saveMessage.value = "فشل اختبار الاتصال: ${err.localizedMessage ?: "تحقق من مفتاح الـ API واسم النموذج"}"
                    }
                )
            } finally {
                _isTestingAiConnection.value = false
            }
        }
    }

    fun sendTestNotification(context: Context, type: String) {
        when (type) {
            "quote" -> NabdaNotificationEngine.postNotification(
                context = context,
                channelId = NabdaNotificationEngine.CHANNEL_QUOTES,
                notificationId = 9001,
                title = "نَبضة الصباح ",
                body = "﴿ أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ ﴾ — يوم مبارك مليء بالسكينة والإنجاز."
            )
            "contact" -> NabdaNotificationEngine.postNotification(
                context = context,
                channelId = NabdaNotificationEngine.CHANNEL_CONTACTS,
                notificationId = 9002,
                title = "تذكير صلة الرحم ",
                body = "«مَنْ أَحَبَّ أَنْ يُبْسَطَ لَهُ فِي رِزْقِهِ فَلْيَصِلْ رَحِمَهُ» — بادر بالاتصال بأحد أقاربك اليوم."
            )
            "habit" -> NabdaNotificationEngine.postNotification(
                context = context,
                channelId = NabdaNotificationEngine.CHANNEL_HABITS,
                notificationId = 9003,
                title = "تنبيه المهام اليومية",
                body = "أحب الأعمال إلى الله أدومها وإن قل — لا تنسَ مهامك اليومية."
            )
        }
    }

    fun clearMessage() {
        _saveMessage.value = null
    }

    private companion object {
        const val TAG = "SettingsVM"
    }
}
