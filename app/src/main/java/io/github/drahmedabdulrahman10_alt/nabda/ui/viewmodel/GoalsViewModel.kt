package io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.drahmedabdulrahman10_alt.nabda.data.model.Goal
import io.github.drahmedabdulrahman10_alt.nabda.data.model.GoalMilestone
import io.github.drahmedabdulrahman10_alt.nabda.data.model.GoalTimeframe
import io.github.drahmedabdulrahman10_alt.nabda.data.repository.NabdaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GoalsScreenData(
    val goals: List<Goal> = emptyList(),
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val averageProgress: Int = 0
)

class GoalsViewModel(
    private val repository: NabdaRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    val uiState: StateFlow<UiState<GoalsScreenData>> = repository.observeGoals(currentUserId)
        .map { goals ->
            val total = goals.size
            val completed = goals.count { it.effectiveIsCompleted() }
            val avg = if (total > 0) {
                goals.sumOf { it.effectiveProgress() } / total
            } else {
                0
            }
            UiState.Success(
                GoalsScreenData(
                    goals = goals,
                    totalCount = total,
                    completedCount = completed,
                    averageProgress = avg
                )
            ) as UiState<GoalsScreenData>
        }
        .catch { e ->
            Log.w(TAG, "Failed observing goals", e)
            emit(UiState.Error(e.localizedMessage ?: "تعذّر تحميل الأهداف الكبرى"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun createGoal(
        title: String,
        description: String?,
        timeframe: GoalTimeframe,
        periodLabel: String,
        progress: Int,
        milestones: List<GoalMilestone>
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val result = repository.createGoal(
                title = title.trim(),
                description = description?.trim()?.takeIf { it.isNotBlank() },
                timeframe = timeframe,
                periodLabel = periodLabel.trim(),
                progress = progress,
                milestones = milestones
            )
            result.fold(
                onSuccess = {
                    _statusBanner.value = "تمت إضافة الهدف «${title.trim()}» بنجاح "
                },
                onFailure = { err ->
                    _statusBanner.value = err.localizedMessage ?: "تعذّر حفظ الهدف الجديد"
                }
            )
        }
    }

    fun updateGoal(goal: Goal) {
        if (goal.title.isBlank()) return
        viewModelScope.launch {
            val result = repository.updateGoal(goal)
            result.fold(
                onSuccess = {
                    _statusBanner.value = "تم تحديث الهدف «${goal.title}» "
                },
                onFailure = { err ->
                    _statusBanner.value = err.localizedMessage ?: "تعذّر تحديث الهدف"
                }
            )
        }
    }

    fun toggleGoalCompleted(goal: Goal) {
        val nextCompleted = !goal.effectiveIsCompleted()
        viewModelScope.launch {
            repository.toggleGoalCompleted(goal, nextCompleted)
        }
    }

    fun toggleGoalMilestone(goal: Goal, milestoneId: String) {
        viewModelScope.launch {
            repository.toggleGoalMilestone(goal, milestoneId)
        }
    }

    fun updateGoalProgress(goal: Goal, newProgress: Int) {
        viewModelScope.launch {
            repository.updateGoalProgress(goal, newProgress.coerceIn(0, 100))
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            val result = repository.deleteGoal(goalId)
            if (result.isSuccess) {
                _statusBanner.value = "تم حذف الهدف"
            }
        }
    }

    companion object {
        private const val TAG = "GoalsViewModel"
    }
}
