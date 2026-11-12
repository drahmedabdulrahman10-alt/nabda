package io.github.drahmedabdulrahman10_alt.nabda.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.drahmedabdulrahman10_alt.nabda.data.model.DailyTask
import io.github.drahmedabdulrahman10_alt.nabda.data.model.TaskPriority
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.PriorityHighAccent
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.PriorityHighContainerLight
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.PriorityLowAccent
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.PriorityLowContainerLight
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.PriorityMediumAccent
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.PriorityMediumContainerLight
import io.github.drahmedabdulrahman10_alt.nabda.util.ArabicCalendarUtils
import io.github.drahmedabdulrahman10_alt.nabda.util.CalendarGridCellDate
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToInt

enum class CalendarGridMode {
    MONTH,
    WEEK
}

/**
 * Interactive Full Calendar View (Month & Week Grid) inspired by Google Calendar,
 * featuring Long-Press Drag-and-Drop task rescheduling across dates.
 */
@Composable
fun InteractiveCalendarView(
    tasksByDateKey: Map<String, List<DailyTask>>,
    onMoveTaskToDate: (DailyTask, Date) -> Unit,
    onToggleTaskCompleted: (DailyTask) -> Unit,
    onAddTaskForDate: (Date, String, TaskPriority) -> Unit,
    onNavigateToDate: (Date) -> Unit,
    weekFooter: (@Composable (weekStart: Date) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    val initialCal = remember { Calendar.getInstance() }

    var selectedYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) }
    var selectedWeekAnchorDate by remember { mutableStateOf(ArabicCalendarUtils.startOfDay(Date())) }
    var gridMode by rememberSaveable { mutableStateOf(CalendarGridMode.WEEK) }

    // Selected date for quick inspection / task creation dialog
    var inspectedCellDate by remember { mutableStateOf<CalendarGridCellDate?>(null) }

    // Long-press drag-and-drop state
    val dayCellBounds = remember { mutableStateMapOf<String, Rect>() }
    val dateByKey = remember { mutableStateMapOf<String, Date>() }
    var rootBoxPositionInRoot by remember { mutableStateOf(Offset.Zero) }
    var draggedTask by remember { mutableStateOf<DailyTask?>(null) }
    var dragSourceDateKey by remember { mutableStateOf<String?>(null) }
    var dropTargetDateKey by remember { mutableStateOf<String?>(null) }
    var dragPointerRootOffset by remember { mutableStateOf(Offset.Zero) }

    val monthNames = remember { ArabicCalendarUtils.getGregorianMonthNames() }
    val weekHeaders = remember { ArabicCalendarUtils.getWeekDayColumnHeaders() }

    val hijriSubtitle = remember(selectedYear, selectedMonth) {
        ArabicCalendarUtils.getHijriMonthYearSubtitle(selectedYear, selectedMonth)
    }

    val gridCells = remember(selectedYear, selectedMonth, selectedWeekAnchorDate, gridMode) {
        when (gridMode) {
            CalendarGridMode.MONTH -> ArabicCalendarUtils.buildMonthGridDates(selectedYear, selectedMonth)
            CalendarGridMode.WEEK -> ArabicCalendarUtils.buildWeekGridDates(selectedWeekAnchorDate)
        }
    }

    // Keep dateByKey synchronized with visible grid cells
    gridCells.forEach { cell ->
        dateByKey[cell.dateKey] = cell.date
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                rootBoxPositionInRoot = coords.positionInRoot()
            }
            .testTag("interactive_calendar_root")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Week / month switch and one navigation row, so the grid starts near the top.
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = gridMode == CalendarGridMode.WEEK,
                    onClick = { gridMode = CalendarGridMode.WEEK },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("calendar_mode_week")
                ) { Text("أسبوع") }
                SegmentedButton(
                    selected = gridMode == CalendarGridMode.MONTH,
                    onClick = { gridMode = CalendarGridMode.MONTH },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("calendar_mode_month")
                ) { Text("شهر") }
            }

            fun step(forward: Boolean) {
                if (gridMode == CalendarGridMode.WEEK) {
                    selectedWeekAnchorDate = ArabicCalendarUtils.addDays(selectedWeekAnchorDate, if (forward) 7 else -7)
                    val cal = Calendar.getInstance().apply { time = selectedWeekAnchorDate }
                    selectedYear = cal.get(Calendar.YEAR)
                    selectedMonth = cal.get(Calendar.MONTH)
                } else {
                    val cal = Calendar.getInstance().apply {
                        clear()
                        set(selectedYear, selectedMonth, 1)
                        add(Calendar.MONTH, if (forward) 1 else -1)
                    }
                    selectedYear = cal.get(Calendar.YEAR)
                    selectedMonth = cal.get(Calendar.MONTH)
                    selectedWeekAnchorDate = cal.time
                }
            }

            val title = if (gridMode == CalendarGridMode.WEEK && gridCells.isNotEmpty()) {
                val first = Calendar.getInstance().apply { time = gridCells.first().date }
                val last = Calendar.getInstance().apply { time = gridCells.last().date }
                val firstMonth = monthNames[first.get(Calendar.MONTH)]
                val lastMonth = monthNames[last.get(Calendar.MONTH)]
                if (firstMonth == lastMonth) {
                    "‏${first.get(Calendar.DAY_OF_MONTH)}‏ – ‏${last.get(Calendar.DAY_OF_MONTH)} $lastMonth"
                } else {
                    "‏${first.get(Calendar.DAY_OF_MONTH)} $firstMonth‏ – ‏${last.get(Calendar.DAY_OF_MONTH)} $lastMonth"
                }
            } else {
                "${monthNames[selectedMonth]} $selectedYear"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { step(forward = false) }, modifier = Modifier.testTag("calendar_prev")) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "السابق")
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = hijriSubtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { step(forward = true) }, modifier = Modifier.testTag("calendar_next")) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "التالي")
                }
                TextButton(
                    onClick = {
                        val nowCal = Calendar.getInstance()
                        selectedYear = nowCal.get(Calendar.YEAR)
                        selectedMonth = nowCal.get(Calendar.MONTH)
                        selectedWeekAnchorDate = ArabicCalendarUtils.startOfDay(Date())
                    },
                    modifier = Modifier.testTag("calendar_jump_today_button")
                ) { Text("اليوم") }
            }

            // Shown only while a task is being dragged, to say where it will land.
            val dragging = draggedTask
            if (dragging != null) {
                val targetDate = dropTargetDateKey?.let { dateByKey[it] }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (targetDate != null) {
                            "أفلت «${dragging.title}» لنقلها إلى ${ArabicCalendarUtils.formatGregorianArabic(targetDate)}"
                        } else {
                            "اسحب «${dragging.title}» فوق اليوم الجديد"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // 2. Calendar 7-Column Grid (Saturday / السبت to Friday / الجمعة)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calendar_grid_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Weekday Header Row (السبت -> الجمعة)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                            .padding(vertical = 8.dp)
                    ) {
                        weekHeaders.forEach { dayTitle ->
                            Text(
                                text = dayTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Rows of 7 Day Cells
                    val rows = gridCells.chunked(7)
                    val cellMinHeight: Dp = if (gridMode == CalendarGridMode.WEEK) 185.dp else 104.dp

                    rows.forEachIndexed { rowIndex, weekRowCells ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            weekRowCells.forEach { cell ->
                                val dayTasks = tasksByDateKey[cell.dateKey] ?: emptyList()
                                val isDropTarget = draggedTask != null && dropTargetDateKey == cell.dateKey

                                CalendarDayCell(
                                    cell = cell,
                                    tasks = dayTasks,
                                    minHeight = cellMinHeight,
                                    isDropTarget = isDropTarget,
                                    draggedTaskId = draggedTask?.id,
                                    onCellBoundsChanged = { rect ->
                                        dayCellBounds[cell.dateKey] = rect
                                    },
                                    onCellClick = {
                                        inspectedCellDate = cell
                                    },
                                    onTaskLongPressDragStart = { task, startRootOffset ->
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        draggedTask = task
                                        dragSourceDateKey = cell.dateKey
                                        dragPointerRootOffset = startRootOffset
                                        dropTargetDateKey = dayCellBounds.entries
                                            .firstOrNull { (_, rect) -> rect.contains(startRootOffset) }
                                            ?.key ?: cell.dateKey
                                    },
                                    onTaskDrag = { dragDelta ->
                                        dragPointerRootOffset += dragDelta
                                        val hoveredKey = dayCellBounds.entries
                                            .firstOrNull { (_, rect) -> rect.contains(dragPointerRootOffset) }
                                            ?.key
                                        dropTargetDateKey = hoveredKey
                                    },
                                    onTaskDragEnd = {
                                        val taskToMove = draggedTask
                                        val sourceKey = dragSourceDateKey
                                        val targetKey = dropTargetDateKey
                                        val targetDate = targetKey?.let { dateByKey[it] }

                                        draggedTask = null
                                        dragSourceDateKey = null
                                        dropTargetDateKey = null

                                        if (taskToMove != null &&
                                            targetKey != null &&
                                            targetKey != sourceKey &&
                                            targetDate != null
                                        ) {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onMoveTaskToDate(taskToMove, targetDate)
                                        }
                                    },
                                    onTaskDragCancel = {
                                        draggedTask = null
                                        dragSourceDateKey = null
                                        dropTargetDateKey = null
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        if (rowIndex < rows.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }

            if (gridMode == CalendarGridMode.WEEK && weekFooter != null && gridCells.isNotEmpty()) {
                weekFooter(gridCells.first().date)
            }
        }

        // 3. Floating Drag Preview Chip following the user's finger
        val activeDraggedTask = draggedTask
        if (activeDraggedTask != null) {
            val density = LocalDensity.current
            val localOffset = dragPointerRootOffset - rootBoxPositionInRoot
            val xPx = (localOffset.x - with(density) { 60.dp.toPx() }).roundToInt()
            val yPx = (localOffset.y - with(density) { 28.dp.toPx() }).roundToInt()

            val (chipBg, chipAccent) = when (activeDraggedTask.priorityEnum) {
                TaskPriority.HIGH -> PriorityHighContainerLight to PriorityHighAccent
                TaskPriority.MEDIUM -> PriorityMediumContainerLight to PriorityMediumAccent
                TaskPriority.LOW -> PriorityLowContainerLight to PriorityLowAccent
            }

            Surface(
                modifier = Modifier
                    .offset { IntOffset(xPx, yPx) }
                    .zIndex(20f)
                    .scale(1.06f)
                    .shadow(12.dp, RoundedCornerShape(10.dp))
                    .widthIn(min = 110.dp, max = 200.dp),
                shape = RoundedCornerShape(10.dp),
                color = chipBg,
                border = BorderStroke(2.dp, chipAccent)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = null,
                        tint = chipAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = activeDraggedTask.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = chipAccent,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    // 4. Quick Day Tasks & Add Task Sheet/Dialog when tapping a Day Cell
    if (inspectedCellDate != null) {
        val targetCell = inspectedCellDate!!
        val cellTasks = tasksByDateKey[targetCell.dateKey] ?: emptyList()

        CalendarDayQuickDialog(
            cell = targetCell,
            tasks = cellTasks,
            onDismiss = { inspectedCellDate = null },
            onToggleTask = onToggleTaskCompleted,
            onAddTask = { title, priority ->
                onAddTaskForDate(targetCell.date, title, priority)
            },
            onOpenFullDayScreen = {
                val dateToOpen = targetCell.date
                inspectedCellDate = null
                onNavigateToDate(dateToOpen)
            }
        )
    }
}

@Composable
private fun CalendarDayCell(
    cell: CalendarGridCellDate,
    tasks: List<DailyTask>,
    minHeight: Dp,
    isDropTarget: Boolean,
    draggedTaskId: String?,
    onCellBoundsChanged: (Rect) -> Unit,
    onCellClick: () -> Unit,
    onTaskLongPressDragStart: (DailyTask, Offset) -> Unit,
    onTaskDrag: (Offset) -> Unit,
    onTaskDragEnd: () -> Unit,
    onTaskDragCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = when {
            isDropTarget -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
            cell.isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
            !cell.isCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "cellBgColor"
    )

    val borderColor = when {
        isDropTarget -> MaterialTheme.colorScheme.primary
        cell.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    }

    val borderWidth = if (isDropTarget) 2.5.dp else 0.5.dp

    Column(
        modifier = modifier
            .heightIn(min = minHeight)
            .background(bgColor)
            .border(borderWidth, borderColor)
            .onGloballyPositioned { coords ->
                onCellBoundsChanged(coords.boundsInRoot())
            }
            .clickable { onCellClick() }
            .padding(horizontal = 3.dp, vertical = 4.dp)
            .testTag("calendar_day_cell_${cell.dateKey}"),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // Day Header: Gregorian Day Number (Primary circle if Today) + Subtle Hijri Day Number
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (cell.isToday) MaterialTheme.colorScheme.primary else Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cell.dayOfMonth.toString(),
                    fontSize = 11.sp,
                    fontWeight = if (cell.isToday || cell.isCurrentMonth) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        cell.isToday -> MaterialTheme.colorScheme.onPrimary
                        cell.isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
            }

            Text(
                text = cell.hijriDay,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.secondary.copy(
                    alpha = if (cell.isCurrentMonth) 0.85f else 0.45f
                )
            )
        }

        // Task Chips inside Day Cell
        tasks.forEach { task ->
            CalendarTaskChip(
                task = task,
                isBeingDragged = draggedTaskId == task.id,
                onClick = onCellClick,
                onLongPressDragStart = { startRootPos ->
                    onTaskLongPressDragStart(task, startRootPos)
                },
                onDrag = onTaskDrag,
                onDragEnd = onTaskDragEnd,
                onDragCancel = onTaskDragCancel
            )
        }
    }
}

@Composable
private fun CalendarTaskChip(
    task: DailyTask,
    isBeingDragged: Boolean,
    onClick: () -> Unit,
    onLongPressDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    var chipRootPosition by remember { mutableStateOf(Offset.Zero) }

    val (chipBg, chipTextColor) = when (task.priorityEnum) {
        TaskPriority.HIGH -> PriorityHighContainerLight to PriorityHighAccent
        TaskPriority.MEDIUM -> PriorityMediumContainerLight to PriorityMediumAccent
        TaskPriority.LOW -> PriorityLowContainerLight to PriorityLowAccent
    }

    val effectiveAlpha = when {
        isBeingDragged -> 0.35f
        task.isCompleted -> 0.6f
        else -> 1f
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = effectiveAlpha }
            .onGloballyPositioned { coords ->
                chipRootPosition = coords.positionInRoot()
            }
            .pointerInput(task.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { startOffset ->
                        onLongPressDragStart(chipRootPosition + startOffset)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    },
                    onDragEnd = {
                        onDragEnd()
                    },
                    onDragCancel = {
                        onDragCancel()
                    }
                )
            }
            .clickable { onClick() }
            .testTag("calendar_task_chip_${task.id}"),
        shape = RoundedCornerShape(5.dp),
        color = chipBg,
        border = BorderStroke(0.5.dp, chipTextColor.copy(alpha = 0.4f))
    ) {
        Text(
            text = task.title,
            fontSize = 9.5.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = chipTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun CalendarDayQuickDialog(
    cell: CalendarGridCellDate,
    tasks: List<DailyTask>,
    onDismiss: () -> Unit,
    onToggleTask: (DailyTask) -> Unit,
    onAddTask: (String, TaskPriority) -> Unit,
    onOpenFullDayScreen: () -> Unit
) {
    var newTaskTitle by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(TaskPriority.HIGH) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = ArabicCalendarUtils.formatGregorianArabic(cell.date),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "الموافق: ${ArabicCalendarUtils.formatHijriArabic(cell.date)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (tasks.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "لا توجد مهام مسجلة في هذا اليوم حتى الآن.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    Text(
                        text = "مهام هذا اليوم (${tasks.count { it.isCompleted }}/${tasks.size}):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    tasks.forEach { task ->
                        val (chipBg, chipAccent) = when (task.priorityEnum) {
                            TaskPriority.HIGH -> PriorityHighContainerLight to PriorityHighAccent
                            TaskPriority.MEDIUM -> PriorityMediumContainerLight to PriorityMediumAccent
                            TaskPriority.LOW -> PriorityLowContainerLight to PriorityLowAccent
                        }
                        Surface(
                            color = chipBg.copy(alpha = if (task.isCompleted) 0.5f else 0.9f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, chipAccent.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleTask(task) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (task.isCompleted) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Default.RadioButtonUnchecked
                                    },
                                    contentDescription = null,
                                    tint = chipAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = chipAccent,
                                    fontWeight = FontWeight.SemiBold,
                                    textDecoration = if (task.isCompleted) {
                                        TextDecoration.LineThrough
                                    } else {
                                        TextDecoration.None
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = task.priorityEnum.labelAr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = chipAccent
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Quick Add Task Section
                Text(
                    text = "إضافة مهمة سريعة لهذا التاريخ:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = newTaskTitle,
                    onValueChange = { newTaskTitle = it },
                    label = { Text("عنوان المهمة") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calendar_quick_add_task_input"),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TaskPriority.entries.forEach { prio ->
                        FilterChip(
                            selected = selectedPriority == prio,
                            onClick = { selectedPriority = prio },
                            label = { Text(prio.labelAr) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                FilledTonalButton(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            onAddTask(newTaskTitle, selectedPriority)
                            newTaskTitle = ""
                        }
                    },
                    enabled = newTaskTitle.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calendar_quick_add_task_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة المهمة لهذا اليوم")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenFullDayScreen,
                modifier = Modifier.testTag("calendar_open_full_day_btn"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("فتح صفحة اليوم الكاملة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
