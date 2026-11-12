package io.github.drahmedabdulrahman10_alt.nabda.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.drahmedabdulrahman10_alt.nabda.data.model.Goal
import io.github.drahmedabdulrahman10_alt.nabda.data.model.GoalMilestone
import io.github.drahmedabdulrahman10_alt.nabda.data.model.GoalTimeframe
import io.github.drahmedabdulrahman10_alt.nabda.ui.components.ProgressRing
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.GoalsScreenData
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.GoalsViewModel
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.UiState
import java.util.UUID

private enum class GoalFilterTab(val labelAr: String, val timeframe: GoalTimeframe?) {
    ALL("الكل", null),
    MONTHLY("أهداف الشهر", GoalTimeframe.MONTHLY),
    QUARTERLY("أهداف المرحلة/الموديول", GoalTimeframe.QUARTERLY),
    YEARLY("أهداف السنة", GoalTimeframe.YEARLY)
}

@Composable
fun GoalsScreen(
    uiState: UiState<GoalsScreenData>,
    statusBanner: String?,
    onClearStatusBanner: () -> Unit,
    viewModel: GoalsViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val notifier = io.github.drahmedabdulrahman10_alt.nabda.ui.components.LocalNotifier.current

    var selectedTab by remember { mutableStateOf(GoalFilterTab.ALL) }
    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<Goal?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingGoal = null
                    showAddOrEditDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("+ إضافة هدف جديد", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_goal_fab")
            )
        }
    ) { innerPadding ->
        when (uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            is UiState.Success -> {
                val data = uiState.data
                val filteredGoals = remember(data.goals, selectedTab) {
                    val tf = selectedTab.timeframe
                    if (tf == null) {
                        data.goals
                    } else {
                        data.goals.filter { it.timeframeEnum == tf }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .testTag("goals_screen_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Status Banner
                    if (!statusBanner.isNullOrBlank()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("goals_status_banner")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = statusBanner,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = onClearStatusBanner) {
                                        Text("إغلاق")
                                    }
                                }
                            }
                        }
                    }

                    // Strategic Goals Hero Overview Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "الأهداف الكبرى والبوصلة",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "اربط مهامك اليومية والأسبوعية بأهدافك الشهرية والمرحلية (الموديولات) والسنوية.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "المنجز: ${data.completedCount} من ${data.totalCount} هدف • متوسط التقدم ${data.averageProgress}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                ProgressRing(
                                    progress = data.averageProgress / 100f,
                                    percentText = "${data.averageProgress}%",
                                    size = 64.dp,
                                    strokeWidth = 6.dp
                                )
                            }
                        }
                    }

                    // Top Filter Tabs: الكل | أهداف الشهر | أهداف المرحلة/الموديول | أهداف السنة
                    item {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("goals_filter_tabs_row"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(GoalFilterTab.entries) { tab ->
                                val isSelected = selectedTab == tab
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedTab = tab },
                                    label = {
                                        Text(
                                            text = tab.labelAr,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("goal_filter_tab_${tab.name}")
                                )
                            }
                        }
                    }

                    // Empty State or Goal Cards
                    if (filteredGoals.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrackChanges,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "لا توجد أهداف في هذا التصنيف حالياً",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "اضغط على «+ إضافة هدف جديد» بالأسفل لتحديد أهدافك السنوية أو المرحلية (الموديول) أو الشهرية وتقسيمها لخطوات عملية.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredGoals, key = { it.id }) { goal ->
                            GoalCardItem(
                                goal = goal,
                                onToggleGoalCompleted = { viewModel.toggleGoalCompleted(goal) },
                                onToggleMilestone = { milestoneId ->
                                    viewModel.toggleGoalMilestone(goal, milestoneId)
                                },
                                onAdjustProgress = { nextProgress ->
                                    viewModel.updateGoalProgress(goal, nextProgress)
                                },
                                onEdit = {
                                    editingGoal = goal
                                    showAddOrEditDialog = true
                                },
                                onDelete = {
                                    viewModel.deleteGoal(goal.id)
                                    notifier.show("تم حذف الهدف") { viewModel.updateGoal(goal) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddOrEditDialog) {
        AddOrEditGoalDialog(
            initialGoal = editingGoal,
            defaultTimeframe = selectedTab.timeframe ?: GoalTimeframe.MONTHLY,
            onDismiss = {
                showAddOrEditDialog = false
                editingGoal = null
            },
            onSave = { title, description, timeframe, periodLabel, progress, milestones ->
                val existing = editingGoal
                if (existing == null) {
                    viewModel.createGoal(
                        title = title,
                        description = description,
                        timeframe = timeframe,
                        periodLabel = periodLabel,
                        progress = progress,
                        milestones = milestones
                    )
                } else {
                    viewModel.updateGoal(
                        existing.copy(
                            title = title,
                            description = description,
                            timeframe = timeframe.name,
                            periodLabel = periodLabel,
                            progress = progress,
                            milestones = milestones
                        )
                    )
                }
                showAddOrEditDialog = false
                editingGoal = null
            }
        )
    }
}

@Composable
private fun GoalCardItem(
    goal: Goal,
    onToggleGoalCompleted: () -> Unit,
    onToggleMilestone: (String) -> Unit,
    onAdjustProgress: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val effectiveProgress = goal.effectiveProgress()
    val isDone = goal.effectiveIsCompleted()
    val animatedProgress by animateFloatAsState(
        targetValue = (effectiveProgress / 100f).coerceIn(0f, 1f),
        label = "goalProgressAnim"
    )
    var isMilestonesExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("goal_card_${goal.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDone) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Badges + Edit & Delete Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Timeframe Badge
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = goal.timeframeEnum.labelAr,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Period Label Badge (e.g. موديول الباطنة / شهر نوفمبر 2026)
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = goal.periodLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("edit_goal_${goal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل الهدف",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("delete_goal_${goal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف الهدف",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Title & Completion Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onToggleGoalCompleted,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("toggle_goal_complete_${goal.id}")
                ) {
                    Icon(
                        imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "تبديل إتمام الهدف",
                        tint = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!goal.description.isNullOrBlank()) {
                        Text(
                            text = goal.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Progress Bar Row
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (goal.milestones.isNotEmpty()) {
                            val doneMs = goal.milestones.count { it.isDone }
                            "تقدم الخطوات الفرعية: $doneMs من ${goal.milestones.size}"
                        } else {
                            "نسبة الإنجاز"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$effectiveProgress%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }

            // Manual progress controls if goal has NO milestones
            if (goal.milestones.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onAdjustProgress((effectiveProgress - 10).coerceAtLeast(0)) },
                        enabled = effectiveProgress > 0,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("-10%")
                    }
                    OutlinedButton(
                        onClick = { onAdjustProgress((effectiveProgress + 10).coerceAtMost(100)) },
                        enabled = effectiveProgress < 100,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+10%")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    FilledTonalButton(
                        onClick = onToggleGoalCompleted,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isDone) "إعادة فتح الهدف" else "إتمام ١٠٠٪")
                    }
                }
            } else {
                // Expandable Sub-Milestones Checklist
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isMilestonesExpanded = !isMilestonesExpanded }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الخطوات العملية (${goal.milestones.count { it.isDone }}/${goal.milestones.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (isMilestonesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(visible = isMilestonesExpanded) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        goal.milestones.forEach { milestone ->
                            Surface(
                                color = if (milestone.isDone) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleMilestone(milestone.id) }
                                    .testTag("goal_milestone_${goal.id}_${milestone.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = milestone.isDone,
                                        onCheckedChange = { onToggleMilestone(milestone.id) },
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = milestone.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textDecoration = if (milestone.isDone) {
                                            TextDecoration.LineThrough
                                        } else {
                                            TextDecoration.None
                                        },
                                        color = if (milestone.isDone) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddOrEditGoalDialog(
    initialGoal: Goal?,
    defaultTimeframe: GoalTimeframe,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String?,
        timeframe: GoalTimeframe,
        periodLabel: String,
        progress: Int,
        milestones: List<GoalMilestone>
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialGoal?.title ?: "") }
    var description by remember { mutableStateOf(initialGoal?.description ?: "") }
    var timeframe by remember {
        mutableStateOf(initialGoal?.timeframeEnum ?: defaultTimeframe)
    }
    var periodLabel by remember {
        mutableStateOf(
            initialGoal?.periodLabel ?: when (defaultTimeframe) {
                GoalTimeframe.MONTHLY -> "شهر نوفمبر 2026"
                GoalTimeframe.QUARTERLY -> "موديول الباطنة"
                GoalTimeframe.YEARLY -> "عام 2026"
            }
        )
    }
    var manualProgress by remember { mutableIntStateOf(initialGoal?.progress ?: 0) }
    val milestones = remember {
        mutableStateListOf<GoalMilestone>().apply {
            if (initialGoal != null) {
                addAll(initialGoal.milestones)
            }
        }
    }
    var newMilestoneTitle by remember { mutableStateOf("") }

    val periodSuggestions = remember(timeframe) {
        when (timeframe) {
            GoalTimeframe.MONTHLY -> listOf("شهر نوفمبر 2026", "شهر ديسمبر 2026", "شهر رمضان المبارك")
            GoalTimeframe.QUARTERLY -> listOf("موديول الباطنة", "موديول الجراحة", "الربع الرابع 2026")
            GoalTimeframe.YEARLY -> listOf("عام 2026", "عام 1448 هـ", "السنة الدراسية الحالية")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (initialGoal == null) "إضافة هدف استراتيجي جديد" else "تعديل الهدف",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الهدف *") },
                    placeholder = { Text("مثال: إتقان موديول الباطنة أو ختم القرآن تلاوةً وتدبراً") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_dialog_title_input"),
                    singleLine = true
                )

                // Timeframe Selector
                Text(
                    text = "النطاق الزمني للهدف:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GoalTimeframe.entries.forEach { tf ->
                        FilterChip(
                            selected = timeframe == tf,
                            onClick = {
                                timeframe = tf
                                if (initialGoal == null) {
                                    periodLabel = when (tf) {
                                        GoalTimeframe.MONTHLY -> "شهر نوفمبر 2026"
                                        GoalTimeframe.QUARTERLY -> "موديول الباطنة"
                                        GoalTimeframe.YEARLY -> "عام 2026"
                                    }
                                }
                            },
                            label = { Text(tf.labelAr) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("goal_dialog_timeframe_${tf.name}")
                        )
                    }
                }

                // Period Label Input + Suggestions
                OutlinedTextField(
                    value = periodLabel,
                    onValueChange = { periodLabel = it },
                    label = { Text("اسم الفترة / الموديول *") },
                    placeholder = { Text("مثال: عام 2026، موديول الباطنة، شهر نوفمبر 2026") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_dialog_period_input"),
                    singleLine = true
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    periodSuggestions.forEach { suggestion ->
                        AssistChip(
                            onClick = { periodLabel = suggestion },
                            label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف أو نية الهدف (اختياري)") },
                    placeholder = { Text("لماذا هذا الهدف مهم لك في هذه المرحلة؟") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_dialog_description_input"),
                    minLines = 2,
                    maxLines = 4
                )

                HorizontalDivider()

                // Sub-milestones builder
                Text(
                    text = "الخطوات العملية لتحقيق الهدف (Milestones):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "إضافة خطوات فرعية يجعل نسبة الإنجاز تُحسب تلقائياً عند إتمام كل خطوة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newMilestoneTitle,
                        onValueChange = { newMilestoneTitle = it },
                        label = { Text("خطوة فرعية جديدة") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("goal_dialog_milestone_input"),
                        singleLine = true
                    )
                    FilledTonalButton(
                        onClick = {
                            val clean = newMilestoneTitle.trim()
                            if (clean.isNotEmpty()) {
                                val msId = "ms_${UUID.randomUUID().toString().take(8)}"
                                milestones.add(GoalMilestone(id = msId, title = clean, isDone = false))
                                newMilestoneTitle = ""
                            }
                        },
                        enabled = newMilestoneTitle.isNotBlank(),
                        modifier = Modifier.testTag("goal_dialog_add_milestone_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "إضافة خطوة")
                    }
                }

                if (milestones.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        milestones.forEachIndexed { idx, ms ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = ms.isDone,
                                        onCheckedChange = { checked ->
                                            milestones[idx] = ms.copy(isDone = checked)
                                        }
                                    )
                                    Text(
                                        text = ms.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { milestones.removeAt(idx) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "حذف الخطوة",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Manual progress slider when no milestones are added
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "نسبة التقدم المبدئية: $manualProgress%",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(
                            value = manualProgress.toFloat(),
                            onValueChange = { manualProgress = it.toInt() },
                            valueRange = 0f..100f,
                            steps = 9
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pendingClean = newMilestoneTitle.trim()
                    val finalMilestones = milestones.toMutableList()
                    if (pendingClean.isNotEmpty()) {
                        val msId = "ms_${UUID.randomUUID().toString().take(8)}"
                        finalMilestones.add(GoalMilestone(id = msId, title = pendingClean, isDone = false))
                    }
                    onSave(
                        title.trim(),
                        description.trim().takeIf { it.isNotEmpty() },
                        timeframe,
                        periodLabel.trim().ifBlank { "المرحلة الحالية" },
                        manualProgress,
                        finalMilestones
                    )
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("goal_dialog_save_btn")
            ) {
                Text("حفظ الهدف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
