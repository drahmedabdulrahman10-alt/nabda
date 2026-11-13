package io.github.drahmedabdulrahman10_alt.nabda.ui.screens

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.drahmedabdulrahman10_alt.nabda.data.model.Contact
import io.github.drahmedabdulrahman10_alt.nabda.data.model.DailyTask
import io.github.drahmedabdulrahman10_alt.nabda.data.model.Quote
import io.github.drahmedabdulrahman10_alt.nabda.data.model.TaskPriority
import io.github.drahmedabdulrahman10_alt.nabda.ui.components.LocalNotifier
import io.github.drahmedabdulrahman10_alt.nabda.ui.components.launchPhoneDialer
import io.github.drahmedabdulrahman10_alt.nabda.ui.components.launchWhatsAppChat
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.AmiriFontFamily
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.PriorityHighAccent
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.PriorityMediumAccent
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.DailyScreenData
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.DailyTasksViewModel
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.DayChipUi
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.FixedTaskStatusUi
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.PinnedContactStatusUi
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.UiState
import java.util.Calendar
import java.util.Locale

@Composable
private fun priorityColor(priority: TaskPriority): Color = when (priority) {
    TaskPriority.HIGH -> PriorityHighAccent
    TaskPriority.MEDIUM -> PriorityMediumAccent
    TaskPriority.LOW -> MaterialTheme.colorScheme.outline
}

@Composable
fun TodayScreen(
    uiState: UiState<DailyScreenData>,
    viewModel: DailyTasksViewModel,
    onOpenDailyManagement: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notifier = LocalNotifier.current
    val statusMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val isAiParsing by viewModel.isAiParsing.collectAsStateWithLifecycle()
    val aiDialogError by viewModel.aiDialogError.collectAsStateWithLifecycle()

    var editingTask by remember { mutableStateOf<DailyTask?>(null) }
    var showAiDialog by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            notifier.show(it)
            viewModel.clearBannerMessage()
        }
    }

    val data = (uiState as? UiState.Success)?.data
    if (data == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (uiState is UiState.Error) {
                Text(uiState.message, color = MaterialTheme.colorScheme.error)
            }
        }
        return
    }

    val deleteWithUndo: (DailyTask) -> Unit = { task ->
        viewModel.deleteTask(task)
        notifier.show("تم حذف المهمة") { viewModel.restoreTask(task) }
    }
    val postponeWithUndo: (DailyTask) -> Unit = { task ->
        viewModel.moveTaskToNextDay(task)
        notifier.show("تم ترحيل المهمة لليوم التالي") { viewModel.restoreTask(task) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
        ) {
            item(key = "header") {
                DayHeader(data = data, onJumpToToday = viewModel::selectToday)
            }
            item(key = "strip") {
                WeekStrip(
                    days = data.weekDays,
                    onSelect = viewModel::selectDay,
                    onShiftWeek = { weeks -> viewModel.shiftSelectedDay(weeks * 7) }
                )
            }
            item(key = "reminders") {
                RemindersBlock(
                    quote = data.dailyQuote,
                    suggestedContact = data.rotatingSuggestedContact,
                    pinnedContacts = data.pinnedContacts,
                    onContactDone = { contact ->
                        viewModel.markRotatingContactDone(contact)
                        notifier.show("تقبّل الله صلتك لـ «${contact.name}»")
                    },
                    onTogglePinned = { viewModel.togglePinnedContact(it.contact, it.isCompletedOnSelectedDay) }
                )
            }
            if (data.overdueTasks.isNotEmpty()) {
                item(key = "overdue") {
                    OverdueBanner(
                        dayId = data.selectedDayId,
                        count = data.overdueTasks.size,
                        onCarryOver = {
                            val moved = data.overdueTasks
                            viewModel.moveTasksToToday(moved)
                            notifier.show("تم ترحيل ${moved.size} مهام إلى اليوم") { viewModel.restoreTasks(moved) }
                        }
                    )
                }
            }

            item(key = "tasks_title") {
                SectionTitle(
                    title = if (data.isToday) "مهام اليوم" else "مهام هذا اليوم",
                    trailing = if (data.tasks.isEmpty()) null else "${data.tasks.count { it.isCompleted }} من ${data.tasks.size}"
                )
            }
            if (data.tasks.isEmpty()) {
                item(key = "tasks_empty") {
                    Text(
                        text = "لا توجد مهام بعد. اكتب مهمتك في الشريط بالأسفل واضغط إدخال.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(data.tasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        onToggle = { viewModel.toggleTask(task) },
                        onOpen = { editingTask = task },
                        onDelete = { deleteWithUndo(task) },
                        onPostpone = { postponeWithUndo(task) },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            item(key = "daily_title") {
                SectionTitle(
                    title = "المهام اليومية",
                    trailing = if (data.fixedTasks.isEmpty()) null else
                        "${data.fixedTasks.count { it.isCompletedOnSelectedDay }} من ${data.fixedTasks.size}",
                    actionLabel = "إدارة",
                    onAction = onOpenDailyManagement
                )
            }
            item(key = "daily_chips") {
                DailyChips(
                    items = data.fixedTasks,
                    onToggle = { viewModel.toggleFixedTask(it.fixedTask, it.isCompletedOnSelectedDay) }
                )
            }
        }

        QuickAddBar(
            onAdd = viewModel::addTask,
            onOpenAi = {
                viewModel.clearAiDialogError()
                showAiDialog = true
            }
        )
    }

    editingTask?.let { task ->
        TaskEditSheet(
            task = task,
            onDismiss = { editingTask = null },
            onSave = { title, priority, reminder ->
                viewModel.updateTask(task.id, title, priority, reminder)
                editingTask = null
            },
            onPostpone = {
                editingTask = null
                postponeWithUndo(task)
            },
            onDelete = {
                editingTask = null
                deleteWithUndo(task)
            }
        )
    }

    if (showAiDialog) {
        SmartAiTaskDialog(
            isParsing = isAiParsing,
            errorMessage = aiDialogError,
            onDismiss = { showAiDialog = false },
            onOpenAiSettings = {
                showAiDialog = false
                onOpenSettings()
            },
            onAnalyzeAndAdd = { text -> viewModel.addTasksViaAi(text) { showAiDialog = false } }
        )
    }
}

@Composable
private fun DayHeader(data: DailyScreenData, onJumpToToday: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = data.gregorianHeader,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = data.hijriHeader,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!data.isToday) {
            TextButton(onClick = onJumpToToday) { Text("اليوم") }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = "أنجزت ${data.completedCount} من ${data.totalCount}" }
        ) {
            CircularProgressIndicator(
                progress = { data.overallProgressFraction },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 4.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = "${data.completedCount}/${data.totalCount}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun WeekStrip(
    days: List<DayChipUi>,
    onSelect: (String) -> Unit,
    onShiftWeek: (Int) -> Unit
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var dragTotal by remember { mutableFloatStateOf(0f) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp)
            .pointerInput(isRtl) {
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onDragEnd = {
                        if (kotlin.math.abs(dragTotal) > 64.dp.toPx()) {
                            val forward = if (isRtl) dragTotal > 0 else dragTotal < 0
                            onShiftWeek(if (forward) 1 else -1)
                        }
                    }
                ) { _, amount -> dragTotal += amount }
            }
    ) {
        for (day in days) {
            val container = when {
                day.isSelected -> MaterialTheme.colorScheme.primary
                else -> Color.Transparent
            }
            val content = when {
                day.isSelected -> MaterialTheme.colorScheme.onPrimary
                day.isToday -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(day.dayId) }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = day.letter,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .background(container, CircleShape)
                        .then(
                            if (day.isToday && !day.isSelected) {
                                Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            } else {
                                Modifier
                            }
                        )
                ) {
                    Text(
                        text = day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (day.isSelected || day.isToday) FontWeight.Bold else FontWeight.Normal,
                        color = content
                    )
                }
                Spacer(Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(
                            if (day.hasOpenTasks) MaterialTheme.colorScheme.secondary else Color.Transparent,
                            CircleShape
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RemindersBlock(
    quote: Quote,
    suggestedContact: Contact?,
    pinnedContacts: List<PinnedContactStatusUi>,
    onContactDone: (Contact) -> Unit,
    onTogglePinned: (PinnedContactStatusUi) -> Unit
) {
    val context = LocalContext.current
    var quoteExpanded by rememberSaveable(quote.id) { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { quoteExpanded = !quoteExpanded }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(
                    Icons.Default.FormatQuote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quote.text,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = AmiriFontFamily),
                        maxLines = if (quoteExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (quoteExpanded && !quote.author.isNullOrBlank()) {
                        Text(
                            text = quote.author,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (suggestedContact != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "صِل اليوم: ${suggestedContact.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { launchPhoneDialer(context, suggestedContact.phone) }) {
                        Icon(Icons.Default.Call, contentDescription = "اتصال")
                    }
                    IconButton(
                        onClick = { launchWhatsAppChat(context, suggestedContact.phone, suggestedContact.name) }
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "رسالة واتساب")
                    }
                    IconButton(onClick = { onContactDone(suggestedContact) }) {
                        Icon(Icons.Default.Check, contentDescription = "تم التواصل")
                    }
                }
            }
            if (pinnedContacts.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    for (item in pinnedContacts) {
                        FilterChip(
                            selected = item.isCompletedOnSelectedDay,
                            onClick = { onTogglePinned(item) },
                            label = { Text(item.contact.name) },
                            leadingIcon = {
                                Icon(
                                    if (item.isCompletedOnSelectedDay) Icons.Default.Check else Icons.Default.Favorite,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverdueBanner(dayId: String, count: Int, onCarryOver: () -> Unit) {
    var dismissed by rememberSaveable(dayId) { mutableStateOf(false) }
    if (dismissed) return
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp)
        ) {
            Text(
                text = "لديك $count مهام غير منجزة من الأيام السابقة",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onCarryOver) { Text("رحّلها لليوم") }
            IconButton(onClick = { dismissed = true }) {
                Icon(Icons.Default.Close, contentDescription = "إخفاء")
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    trailing: String?,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(top = 8.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(
    task: DailyTask,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onPostpone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> onPostpone()
                SwipeToDismissBoxValue.EndToStart -> onDelete()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            value != SwipeToDismissBoxValue.Settled
        }
    )
    SwipeToDismissBox(
        state = swipeState,
        modifier = modifier.padding(vertical = 3.dp),
        backgroundContent = {
            val toEnd = swipeState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val toStart = swipeState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (toStart) Arrangement.End else Arrangement.Start,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        when {
                            toEnd -> MaterialTheme.colorScheme.tertiaryContainer
                            toStart -> MaterialTheme.colorScheme.errorContainer
                            else -> Color.Transparent
                        },
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 16.dp)
            ) {
                if (toEnd) {
                    Icon(Icons.Default.Redo, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("لليوم التالي", style = MaterialTheme.typography.labelLarge)
                } else if (toStart) {
                    Text("حذف", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.DeleteOutline, contentDescription = null)
                }
            }
        }
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = onOpen, onLongClick = onOpen)
                    .heightIn(min = 52.dp)
                    .padding(end = 14.dp)
            ) {
                IconButton(onClick = onToggle) {
                    if (task.isCompleted) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "إلغاء الإنجاز",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Icon(
                            Icons.Outlined.Circle,
                            contentDescription = "تمييز كمنجزة",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (task.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                )
                val reminder = task.reminderMinuteOfDay
                if (!task.isCompleted && reminder != null) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = formatMinuteOfDay(reminder),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (!task.isCompleted && task.priorityEnum != TaskPriority.MEDIUM) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (task.priorityEnum == TaskPriority.HIGH) "مهمة" else "لاحقًا",
                        style = MaterialTheme.typography.labelSmall,
                        color = priorityColor(task.priorityEnum)
                    )
                }
                if (!task.isCompleted) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(priorityColor(task.priorityEnum), CircleShape)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyChips(items: List<FixedTaskStatusUi>, onToggle: (FixedTaskStatusUi) -> Unit) {
    if (items.isEmpty()) {
        Text(
            text = "أضف ما تكرره كل يوم (تمرين، قراءة، وِرد) من «إدارة».",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (item in items) {
            key(item.fixedTask.id) {
                FilterChip(
                    selected = item.isCompletedOnSelectedDay,
                    onClick = { onToggle(item) },
                    label = { Text(item.fixedTask.title) },
                    leadingIcon = if (item.isCompletedOnSelectedDay) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    },
                    trailingIcon = if (item.currentStreakDays > 1) {
                        {
                            Text(
                                text = item.currentStreakDays.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        null
                    },
                    border = if (item.isWarningActive && !item.isCompletedOnSelectedDay) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    } else {
                        FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = item.isCompletedOnSelectedDay
                        )
                    }
                )
            }
        }
    }
}

/** "3:30 م" style time for a minute of the day. */
private fun formatMinuteOfDay(minuteOfDay: Int): String {
    val hour = minuteOfDay / 60
    val minute = minuteOfDay % 60
    val hour12 = if (hour % 12 == 0) 12 else hour % 12
    return "%d:%02d %s".format(Locale.US, hour12, minute, if (hour < 12) "ص" else "م")
}

private fun TaskPriority.next(): TaskPriority = when (this) {
    TaskPriority.MEDIUM -> TaskPriority.HIGH
    TaskPriority.HIGH -> TaskPriority.LOW
    TaskPriority.LOW -> TaskPriority.MEDIUM
}

private fun TaskPriority.shortLabel(): String = when (this) {
    TaskPriority.HIGH -> "مهمة"
    TaskPriority.MEDIUM -> "عادية"
    TaskPriority.LOW -> "لاحقًا"
}

@Composable
private fun QuickAddBar(
    onAdd: (String, TaskPriority) -> Unit,
    onOpenAi: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf(TaskPriority.MEDIUM) }
    val submit = {
        if (text.isNotBlank()) {
            onAdd(text, priority)
            text = ""
            priority = TaskPriority.MEDIUM
        }
    }
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
        ) {
            TextField(
                value = text,
                onValueChange = { text = it.take(300) },
                placeholder = { Text("أضف مهمة…") },
                singleLine = true,
                shape = RoundedCornerShape(26.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                // Keep the keyboard open after adding so several tasks can be typed in a row.
                keyboardActions = KeyboardActions(onDone = { submit() }),
                leadingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { priority = priority.next() }
                            .heightIn(min = 40.dp)
                            .padding(horizontal = 8.dp)
                            .semantics { contentDescription = "الأولوية: ${priority.shortLabel()}" }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(priorityColor(priority), CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(priority.shortLabel(), style = MaterialTheme.typography.labelMedium)
                    }
                },
                trailingIcon = {
                    IconButton(onClick = onOpenAi) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "إضافة ذكية بالصوت أو النص")
                    }
                },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = submit, enabled = text.isNotBlank(), modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إضافة المهمة")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditSheet(
    task: DailyTask,
    onDismiss: () -> Unit,
    onSave: (String, TaskPriority, Int?) -> Unit,
    onPostpone: () -> Unit,
    onDelete: () -> Unit
) {
    var title by remember(task.id) { mutableStateOf(task.title) }
    var priority by remember(task.id) { mutableStateOf(task.priorityEnum) }
    var reminder by remember(task.id) { mutableStateOf(task.reminderMinuteOfDay) }
    val context = LocalContext.current
    val pickTime = {
        val start = reminder ?: (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + 1).coerceAtMost(23) * 60
        TimePickerDialog(
            context,
            { _, hour, minute -> reminder = hour * 60 + minute },
            start / 60,
            start % 60,
            DateFormat.is24HourFormat(context)
        ).show()
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(300) },
                label = { Text("المهمة") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (title.isNotBlank()) onSave(title, priority, reminder) }),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            // Optional reminder at a time on the task's own day.
            FilterChip(
                selected = reminder != null,
                onClick = pickTime,
                label = { Text(reminder?.let { "تذكير ${formatMinuteOfDay(it)}" } ?: "إضافة تذكير") },
                leadingIcon = {
                    Icon(
                        Icons.Default.NotificationsNone,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                },
                trailingIcon = if (reminder != null) {
                    {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "إلغاء التذكير",
                            modifier = Modifier
                                .size(FilterChipDefaults.IconSize)
                                .clickable { reminder = null }
                        )
                    }
                } else {
                    null
                }
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (option in listOf(TaskPriority.HIGH, TaskPriority.MEDIUM, TaskPriority.LOW)) {
                    FilterChip(
                        selected = priority == option,
                        onClick = { priority = option },
                        label = { Text(option.shortLabel()) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(priorityColor(option), CircleShape)
                            )
                        }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onPostpone) {
                    Icon(Icons.Default.Redo, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("لليوم التالي")
                }
                TextButton(onClick = onDelete) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.weight(1f))
                androidx.compose.material3.Button(
                    onClick = { onSave(title, priority, reminder) },
                    enabled = title.isNotBlank()
                ) { Text("حفظ") }
            }
        }
    }
}
