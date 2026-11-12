package io.github.drahmedabdulrahman10_alt.nabda.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.drahmedabdulrahman10_alt.nabda.data.ai.BulkScheduleJsonImporter
import io.github.drahmedabdulrahman10_alt.nabda.data.model.DailyTask
import io.github.drahmedabdulrahman10_alt.nabda.data.model.Goal
import io.github.drahmedabdulrahman10_alt.nabda.data.model.TaskPriority
import io.github.drahmedabdulrahman10_alt.nabda.ui.components.BulkScheduleImportDialog
import io.github.drahmedabdulrahman10_alt.nabda.ui.components.InteractiveCalendarView
import io.github.drahmedabdulrahman10_alt.nabda.ui.components.ProgressRing
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.DaySummaryUi
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.UiState
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.WeekSummaryUi
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.WeeksDashboardData
import io.github.drahmedabdulrahman10_alt.nabda.util.ArabicCalendarUtils
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeksDashboardScreen(
    uiState: UiState<WeeksDashboardData>,
    onRefresh: () -> Unit = {},
    onSelectWeek: (WeekSummaryUi) -> Unit,
    onJumpToToday: () -> Unit,
    onSaveReflection: (String, String) -> Unit,
    isImportingSchedule: Boolean = false,
    importError: String? = null,
    statusBannerMessage: String? = null,
    onClearImportError: () -> Unit = {},
    onClearStatusBanner: () -> Unit = {},
    onShowStatusBanner: (String) -> Unit = {},
    onImportScheduleJson: (String, onSuccess: () -> Unit) -> Unit = { _, _ -> },
    showCalendarInitially: Boolean = false,
    onCalendarModeChanged: (Boolean) -> Unit = {},
    onMoveTaskToDate: (DailyTask, Date) -> Unit = { _, _ -> },
    onToggleTaskFromCalendar: (DailyTask) -> Unit = {},
    onAddTaskForDate: (Date, String, TaskPriority) -> Unit = { _, _, _ -> },
    onNavigateToDate: (Date) -> Unit = {},
    onOpenGoals: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var editingReflectionWeek by remember { mutableStateOf<WeekSummaryUi?>(null) }
    var showBulkImportDialog by remember { mutableStateOf(false) }
    var isCalendarTabSelected by remember(showCalendarInitially) {
        mutableStateOf(showCalendarInitially)
    }

    when (uiState) {
        is UiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onRefresh) {
                        Text("إعادة المحاولة")
                    }
                }
            }
        }
        is UiState.Success -> {
            val data = uiState.data

            Column(modifier = modifier.fillMaxSize()) {
                // Top Segmented Button Row: 1. سجل الأسابيع | 2. التقويم الشامل
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("weeks_calendar_segmented_row")
                ) {
                    SegmentedButton(
                        selected = !isCalendarTabSelected,
                        onClick = {
                            isCalendarTabSelected = false
                            onCalendarModeChanged(false)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("tab_weeks_list_btn")
                    ) {
                        Text(
                            text = "الأسابيع",
                            fontWeight = if (!isCalendarTabSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    SegmentedButton(
                        selected = isCalendarTabSelected,
                        onClick = {
                            isCalendarTabSelected = true
                            onCalendarModeChanged(true)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("tab_full_calendar_btn")
                    ) {
                        Text(
                            text = "الشهر",
                            fontWeight = if (isCalendarTabSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // Shared Status Banner (e.g. "تم نقل المهمة إلى [تاريخ اليوم الجديد]")
                if (!statusBannerMessage.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("weeks_status_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = statusBannerMessage,
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

                if (isCalendarTabSelected) {
                    InteractiveCalendarView(
                        tasksByDateKey = data.tasksByDateKey,
                        onMoveTaskToDate = onMoveTaskToDate,
                        onToggleTaskCompleted = onToggleTaskFromCalendar,
                        onAddTaskForDate = onAddTaskForDate,
                        onNavigateToDate = onNavigateToDate,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("weeks_dashboard_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Collapsible Strategic Goals Compass Card ("بوصلة الأهداف الحالية")
                        item {
                            CurrentGoalsCompassCard(
                                activeGoals = data.activeGoals,
                                onOpenGoals = onOpenGoals
                            )
                        }

                        // Header Summary Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "سجل الأسابيع والتخطيط",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "يبدأ الأسبوع يوم السبت وينتهي يوم الجمعة • خطّط لأسبوعك أو شهرك واستورد الجداول بالذكاء الاصطناعي",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = onJumpToToday,
                                            modifier = Modifier.testTag("dashboard_jump_today_button"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Today,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("انتقل إلى اليوم")
                                        }
                                    }

                                    // Bulk AI Schedule JSON Importer & One-Click Prompt Generator Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = {
                                                onClearImportError()
                                                showBulkImportDialog = true
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("open_bulk_json_import_button"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("استيراد جدول JSON")
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val prompt = BulkScheduleJsonImporter.buildExternalAiPrompt()
                                                clipboardManager.setText(AnnotatedString(prompt))
                                                onShowStatusBanner("تم نسخ الـ Prompt الذكي للحافظة! الصقه في ChatGPT أو Gemini أو Claude ثم استورد الـ JSON ")
                                            },
                                            modifier = Modifier.testTag("quick_copy_ai_prompt_button"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "نسخ أمر التخطيط الذكي",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("نسخ الـ Prompt")
                                        }
                                    }
                                }
                            }
                        }

                        items(data.weeks, key = { it.week.id }) { weekSummary ->
                            WeekCardItem(
                                summary = weekSummary,
                                onClick = { onSelectWeek(weekSummary) },
                                onEditReflection = { editingReflectionWeek = weekSummary }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBulkImportDialog) {
        BulkScheduleImportDialog(
            isImporting = isImportingSchedule,
            errorMessage = importError,
            onDismiss = {
                onClearImportError()
                showBulkImportDialog = false
            },
            onClearError = onClearImportError,
            onImportJson = { rawJson ->
                onImportScheduleJson(rawJson) {
                    showBulkImportDialog = false
                }
            }
        )
    }

    if (editingReflectionWeek != null) {
        val target = editingReflectionWeek!!
        var reflectionText by remember(target.week.id) {
            mutableStateOf(target.week.reflection.orEmpty())
        }
        AlertDialog(
            onDismissRequest = { editingReflectionWeek = null },
            title = {
                Text("تأملات ومراجعة: ${target.week.name}")
            },
            text = {
                OutlinedTextField(
                    value = reflectionText,
                    onValueChange = { reflectionText = it },
                    label = { Text("ما أبرز إنجازاتك ودروسك في هذا الأسبوع؟") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .testTag("week_reflection_input"),
                    maxLines = 5
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveReflection(target.week.id, reflectionText)
                        editingReflectionWeek = null
                    },
                    modifier = Modifier.testTag("save_week_reflection_button")
                ) {
                    Text("حفظ التأملات")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingReflectionWeek = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun WeekCardItem(
    summary: WeekSummaryUi,
    onClick: () -> Unit,
    onEditReflection: () -> Unit
) {
    val startDate = summary.week.startDate?.toDate() ?: Date()
    val endDate = summary.week.endDate?.toDate() ?: Date()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("week_card_${summary.week.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (summary.isCurrentWeek) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            }
        ),
        border = BorderStroke(
            width = if (summary.isCurrentWeek) 2.dp else 1.dp,
            color = if (summary.isCurrentWeek) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (summary.isCurrentWeek) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = summary.week.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (summary.isCurrentWeek) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "الحالي",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${ArabicCalendarUtils.formatGregorianArabic(startDate)}  ←  ${ArabicCalendarUtils.formatGregorianArabic(endDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = ArabicCalendarUtils.formatHijriArabic(startDate),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                ProgressRing(
                    progress = summary.progressFraction,
                    percentText = "${summary.progressPercent}%",
                    size = 58.dp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { summary.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تم إنجاز ${summary.completedItems} من ${summary.totalItems} بنداً",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onEditReflection,
                        modifier = Modifier.testTag("reflection_btn_${summary.week.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (summary.week.reflection.isNullOrBlank()) "إضافة تأملات" else "تعديل التأملات")
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "عرض أيام الأسبوع",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!summary.week.reflection.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    Text(
                        text = "${summary.week.reflection}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

/**
 * 2. Week Days View (`WeekDaysScreen`) displaying all 7 days (Saturday to Friday)
 * with Gregorian + Hijri dates and completion status chips.
 */
@Composable
fun WeekDaysScreen(
    weekSummary: WeekSummaryUi,
    onBack: () -> Unit = {},
    onSelectDay: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("week_days_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        FilledTonalButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("back_to_weeks_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "العودة للأسابيع",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("جميع الأسابيع")
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "أيام ${weekSummary.week.name}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "اضغط على أي يوم من السبت إلى الجمعة لإدارة مهامه ومهامه اليومية",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    ProgressRing(
                        progress = weekSummary.progressFraction,
                        percentText = "${weekSummary.progressPercent}%",
                        size = 64.dp
                    )
                }
            }
        }

        items(weekSummary.days, key = { it.day.id }) { daySummary ->
            DayCardItem(
                daySummary = daySummary,
                onClick = { onSelectDay(daySummary.day.id) }
            )
        }
    }
}

@Composable
private fun DayCardItem(
    daySummary: DaySummaryUi,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("day_card_${daySummary.day.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (daySummary.isToday) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (daySummary.isToday) 2.dp else 1.dp,
            color = if (daySummary.isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = daySummary.day.gregorianDateStr,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (daySummary.isToday) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "اليوم",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "التاريخ الهجري: ${daySummary.day.hijriDateStr}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                ProgressRing(
                    progress = daySummary.progressFraction,
                    percentText = "${daySummary.progressPercent}%",
                    size = 50.dp,
                    strokeWidth = 5.dp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onClick,
                    label = {
                        Text("المهام: ${daySummary.completedTasks}/${daySummary.totalTasks}")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        leadingIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )

                AssistChip(
                    onClick = onClick,
                    label = {
                        Text("المهام اليومية: ${daySummary.completedHabits}/${daySummary.totalActiveHabits}")
                    }
                )
            }
        }
    }
}

@Composable
private fun CurrentGoalsCompassCard(
    activeGoals: List<Goal>,
    onOpenGoals: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weeks_goals_compass_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "بوصلة الأهداف الحالية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    if (activeGoals.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "${activeGoals.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onOpenGoals,
                        modifier = Modifier.testTag("manage_goals_from_weeks_btn")
                    ) {
                        Text(
                            text = "إدارة الأهداف ←",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "طي البوصلة" else "توسيع البوصلة",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                if (activeGoals.isEmpty()) {
                    Text(
                        text = "لم تضف أهدافاً شهرية أو مرحلية بعد. اضغط على «إدارة الأهداف ←» لتحديد أهدافك الكبرى وربط تخطيطك الأسبوعي بها.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeGoals.take(3).forEach { goal ->
                            val prog = goal.effectiveProgress()
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenGoals() }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = goal.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "${goal.periodLabel} • $prog%",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    LinearProgressIndicator(
                                        progress = { (prog / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(50)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        strokeCap = StrokeCap.Round
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

