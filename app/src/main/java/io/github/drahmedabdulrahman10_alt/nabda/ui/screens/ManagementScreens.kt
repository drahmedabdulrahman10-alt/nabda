package io.github.drahmedabdulrahman10_alt.nabda.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.drahmedabdulrahman10_alt.nabda.data.ai.AiTaskAssistantService
import io.github.drahmedabdulrahman10_alt.nabda.data.model.Contact
import io.github.drahmedabdulrahman10_alt.nabda.data.model.FixedTask
import io.github.drahmedabdulrahman10_alt.nabda.data.model.Quote
import io.github.drahmedabdulrahman10_alt.nabda.data.model.UserSettings
import io.github.drahmedabdulrahman10_alt.nabda.notifications.NabdaNotificationEngine
import io.github.drahmedabdulrahman10_alt.nabda.ui.components.LocalNotifier
import io.github.drahmedabdulrahman10_alt.nabda.ui.components.launchPhoneDialer
import io.github.drahmedabdulrahman10_alt.nabda.ui.components.launchWhatsAppChat
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.AmiriFontFamily
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.DailyScreenData
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.DailyTasksViewModel
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.SettingsViewModel
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.UiState

/**
 * 4. Fixed Tasks Management (`FixedTasksScreen`)
 */
@Composable
fun FixedTasksScreen(
    uiState: UiState<DailyScreenData>,
    viewModel: DailyTasksViewModel,
    onBackToToday: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val notifier = LocalNotifier.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<FixedTask?>(null) }

    val habits = (uiState as? UiState.Success)?.data?.allFixedTasks ?: emptyList()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_fixed_task_fab"),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("إضافة مهمة يومية") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("fixed_tasks_management_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // The screen title is already in the top bar; keep only a one-line hint here.
                Text(
                    text = "حدّد ما تكرره كل يوم، وقت التذكير، وعدد أيام الانقطاع للتنبيه الذكي.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(habits, key = { it.id }) { habit ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manage_habit_card_${habit.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = habit.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "تنبيه الانقطاع بعد: ${habit.missedDaysThreshold} أيام متتالية",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (habit.reminderEnabled) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "تذكير يومي الساعة ${String.format("%02d:%02d", habit.reminderHour, habit.reminderMinute)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Switch(
                            checked = habit.isActive,
                            onCheckedChange = { active ->
                                viewModel.updateFixedTask(habit.copy(isActive = active))
                            }
                        )
                        IconButton(onClick = { editingHabit = habit }) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل المهمة")
                        }
                        IconButton(onClick = {
                            viewModel.deleteFixedTask(habit.id)
                            notifier.show("تم حذف «${habit.title}»") { viewModel.restoreFixedTask(habit) }
                        }) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "حذف المهمة",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog || editingHabit != null) {
        val target = editingHabit
        var title by remember(target) { mutableStateOf(target?.title.orEmpty()) }
        var thresholdStr by remember(target) { mutableStateOf((target?.missedDaysThreshold ?: 5).toString()) }
        var reminderEnabled by remember(target) { mutableStateOf(target?.reminderEnabled ?: true) }
        var hourStr by remember(target) { mutableStateOf((target?.reminderHour ?: 7).toString()) }
        var minuteStr by remember(target) { mutableStateOf((target?.reminderMinute ?: 0).toString()) }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingHabit = null
            },
            title = { Text(if (target == null) "إضافة مهمة يومية" else "تعديل المهمة اليومية") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("اسم المهمة اليومية") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("habit_title_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = thresholdStr,
                        onValueChange = { thresholdStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("تنبيه الانقطاع بعد (أيام)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تفعيل التذكير اليومي")
                        Checkbox(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it }
                        )
                    }
                    if (reminderEnabled) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = hourStr,
                                onValueChange = { hourStr = it.filter { ch -> ch.isDigit() } },
                                label = { Text("الساعة (0-23)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = minuteStr,
                                onValueChange = { minuteStr = it.filter { ch -> ch.isDigit() } },
                                label = { Text("الدقيقة (0-59)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val threshold = (thresholdStr.toIntOrNull() ?: 5).coerceIn(1, 365)
                        val hour = (hourStr.toIntOrNull() ?: 7).coerceIn(0, 23)
                        val minute = (minuteStr.toIntOrNull() ?: 0).coerceIn(0, 59)
                        if (target == null) {
                            viewModel.addFixedTask(
                                title = title,
                                missedDaysThreshold = threshold,
                                reminderEnabled = reminderEnabled,
                                reminderHour = hour,
                                reminderMinute = minute,
                                nextOrder = habits.size + 1
                            )
                        } else {
                            viewModel.updateFixedTask(
                                target.copy(
                                    title = title.trim(),
                                    missedDaysThreshold = threshold,
                                    reminderEnabled = reminderEnabled,
                                    reminderHour = hour,
                                    reminderMinute = minute
                                )
                            )
                        }
                        showAddDialog = false
                        editingHabit = null
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.testTag("save_habit_button")
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        editingHabit = null
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * 5. Kinship & Contacts Management (`ContactsScreen`)
 */
@Composable
fun ContactsScreen(
    uiState: UiState<DailyScreenData>,
    viewModel: DailyTasksViewModel,
    onBackToToday: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val notifier = LocalNotifier.current
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<Contact?>(null) }

    val contacts = (uiState as? UiState.Success)?.data?.allContacts ?: emptyList()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_contact_fab"),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("إضافة قريب") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("contacts_management_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "سجل صلة الرحم والأقارب ",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "الأقارب المثبّتون (كالوالدين) يظهرون يومياً في قائمة المتابعة، بينما يتم التناوب تلقائياً على بقية الأقارب كل يوم.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            items(contacts, key = { it.id }) { contact ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manage_contact_card_${contact.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = contact.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (contact.isPinned) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = "تثبيت يومي",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "مثبّت يومياً",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                                Text(
                                    text = contact.phone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(onClick = { launchPhoneDialer(context, contact.phone) }) {
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = "اتصال",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { launchWhatsAppChat(context, contact.phone, contact.name) }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = "واتساب",
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            IconButton(onClick = { editingContact = contact }) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل")
                            }
                            IconButton(onClick = {
                                viewModel.deleteContact(contact.id)
                                notifier.show("تم حذف «${contact.name}»") { viewModel.restoreContact(contact) }
                            }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "حذف",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog || editingContact != null) {
        val target = editingContact
        var name by remember(target) { mutableStateOf(target?.name.orEmpty()) }
        var phone by remember(target) { mutableStateOf(target?.phone.orEmpty()) }
        var isPinned by remember(target) { mutableStateOf(target?.isPinned ?: false) }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingContact = null
            },
            title = { Text(if (target == null) "إضافة قريب (صلة رحم)" else "تعديل بيانات القريب") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("صلة القرابة أو الاسم (مثال: الوالدة، العم أحمد)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_name_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف (مع رمز الدولة لواتساب)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_phone_input"),
                        singleLine = true
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تثبيت يومي (يظهر كل يوم كالوالدين)")
                        Checkbox(
                            checked = isPinned,
                            onCheckedChange = { isPinned = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (target == null) {
                            viewModel.addContact(name, phone, isPinned)
                        } else {
                            viewModel.updateContact(
                                target.copy(
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    isPinned = isPinned
                                )
                            )
                        }
                        showAddDialog = false
                        editingContact = null
                    },
                    enabled = name.isNotBlank() && phone.isNotBlank(),
                    modifier = Modifier.testTag("save_contact_button")
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        editingContact = null
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * 6. Quotes Library (`QuotesScreen`)
 */
@Composable
fun QuotesScreen(
    uiState: UiState<DailyScreenData>,
    viewModel: DailyTasksViewModel,
    onBackToToday: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val notifier = LocalNotifier.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editingQuote by remember { mutableStateOf<Quote?>(null) }

    val quotes = (uiState as? UiState.Success)?.data?.allQuotes ?: emptyList()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_quote_fab"),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("إضافة حكمة أو آية") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("quotes_library_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "مكتبة الحِكم والاقتباسات الإيمانية ",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "تظهر هذه الآيات والحِكم بالتناوب في بطاقة إشراقة اليوم وفي الإشعارات الصباحية.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            items(quotes, key = { it.id }) { quote ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quote_item_card_${quote.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = quote.text,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = AmiriFontFamily
                                    )
                                )
                                if (!quote.author.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "— ${quote.author}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            if (!quote.id.startsWith("default_")) {
                                IconButton(onClick = { editingQuote = quote }) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل الاقتباس")
                                }
                                IconButton(onClick = {
                                    viewModel.deleteQuote(quote.id)
                                    notifier.show("تم حذف الاقتباس") { viewModel.restoreQuote(quote) }
                                }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "حذف الاقتباس",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog || editingQuote != null) {
        val target = editingQuote
        var text by remember(target) { mutableStateOf(target?.text.orEmpty()) }
        var author by remember(target) { mutableStateOf(target?.author.orEmpty()) }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingQuote = null
            },
            title = { Text(if (target == null) "إضافة آية أو حكمة جديدة" else "تعديل الاقتباس") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("نص الآية أو الحكمة") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("quote_text_input"),
                        maxLines = 4
                    )
                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it },
                        label = { Text("المصدر أو القائل (اختياري)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quote_author_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (target == null) {
                            viewModel.addQuote(text, author.takeIf { it.isNotBlank() })
                        } else {
                            viewModel.updateQuote(target.id, text, author.takeIf { it.isNotBlank() })
                        }
                        showAddDialog = false
                        editingQuote = null
                    },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.testTag("save_quote_button")
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        editingQuote = null
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * 7. User Settings, AI Provider Configuration & Smart Notification Engine (`SettingsScreen`)
 */
@Composable
fun SettingsScreen(
    settingsState: UiState<UserSettings>,
    saveMessage: String?,
    onClearMessage: () -> Unit,
    viewModel: SettingsViewModel,
    onBackToToday: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isTestingAi by viewModel.isTestingAiConnection.collectAsStateWithLifecycle()

    var hasNotifPermission by remember {
        mutableStateOf(NabdaNotificationEngine.hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
    }

    when (settingsState) {
        is UiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(settingsState.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is UiState.Success -> {
            val current = settingsState.data
            var draft by remember(current) { mutableStateOf(current) }
            var showApiKey by remember { mutableStateOf(false) }

            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .testTag("settings_screen_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!saveMessage.isNullOrBlank()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_status_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = saveMessage,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onClearMessage) {
                                    Text("حسناً")
                                }
                            }
                        }
                    }
                }

                // Android 13+ Runtime Notification Permission Card
                if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                                    Text(
                                        text = "تفعيل إذن الإشعارات الذكية",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "اسمح لتطبيق نَبضة بإرسال إشراقة الصباح وتذكيرات صلة الرحم والعادات.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    modifier = Modifier.testTag("grant_notifications_permission_button")
                                ) {
                                    Text("السماح")
                                }
                            }
                        }
                    }
                }

                // 1. Appearance Card ("المظهر والعرض")
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "المظهر والعرض",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("الوضع الليلي الداكن (Dark Mode)")
                                Switch(
                                    checked = draft.themeMode == "DARK",
                                    onCheckedChange = {
                                        draft = draft.copy(themeMode = if (it) "DARK" else "LIGHT")
                                        viewModel.updateSettings(context, draft)
                                    },
                                    modifier = Modifier.testTag("dark_mode_switch")
                                )
                            }
                        }
                    }
                }

                // 2. AI Assistant Configuration Card ("إعدادات الذكاء الاصطناعي (AI Assistant) ")
                item {
                    val isOpenRouter = draft.normalizedProvider() == AiTaskAssistantService.PROVIDER_OPENROUTER
                    val activeApiKey = if (isOpenRouter) draft.openRouterApiKey else draft.geminiApiKey

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_settings_card"),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "إعدادات الذكاء الاصطناعي (AI Assistant) ",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "اختر مزود الخدمة وأدخل مفتاح الـ API لإضافة المهام بالصوت أو النص الذكي وترتيب أولوياتها تلقائياً",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Provider Selector (FilterChip Row)
                            Text(
                                text = "مزوّد خدمة الذكاء الاصطناعي:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilterChip(
                                    selected = !isOpenRouter,
                                    onClick = {
                                        val updatedModel = if (
                                            draft.aiModelName.isBlank() ||
                                            draft.aiModelName == AiTaskAssistantService.DEFAULT_OPENROUTER_MODEL ||
                                            draft.aiModelName == "openai/gpt-4o-mini"
                                        ) {
                                            AiTaskAssistantService.DEFAULT_GEMINI_MODEL
                                        } else {
                                            draft.aiModelName
                                        }
                                        draft = draft.copy(
                                            aiProvider = AiTaskAssistantService.PROVIDER_GEMINI,
                                            aiModelName = updatedModel
                                        )
                                    },
                                    label = { Text("Google AI Studio (Gemini)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("provider_chip_gemini")
                                )

                                FilterChip(
                                    selected = isOpenRouter,
                                    onClick = {
                                        val updatedModel = if (
                                            draft.aiModelName.isBlank() ||
                                            draft.aiModelName == AiTaskAssistantService.DEFAULT_GEMINI_MODEL
                                        ) {
                                            AiTaskAssistantService.DEFAULT_OPENROUTER_MODEL
                                        } else {
                                            draft.aiModelName
                                        }
                                        draft = draft.copy(
                                            aiProvider = AiTaskAssistantService.PROVIDER_OPENROUTER,
                                            aiModelName = updatedModel
                                        )
                                    },
                                    label = { Text("OpenRouter") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("provider_chip_openrouter")
                                )
                            }

                            // API Key Input with Visibility Toggle and Paste Button
                            OutlinedTextField(
                                value = activeApiKey,
                                onValueChange = { newValue ->
                                    draft = if (isOpenRouter) {
                                        draft.copy(openRouterApiKey = newValue)
                                    } else {
                                        draft.copy(geminiApiKey = newValue)
                                    }
                                },
                                label = {
                                    Text(
                                        if (isOpenRouter) {
                                            "مفتاح OpenRouter API Key"
                                        } else {
                                            "مفتاح Google Gemini API Key"
                                        }
                                    )
                                },
                                placeholder = {
                                    Text(if (isOpenRouter) "sk-or-v1-..." else "AIzaSy...")
                                },
                                visualTransformation = if (showApiKey) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { showApiKey = !showApiKey },
                                            modifier = Modifier.testTag("toggle_api_key_visibility_btn")
                                        ) {
                                            Icon(
                                                imageVector = if (showApiKey) {
                                                    Icons.Default.VisibilityOff
                                                } else {
                                                    Icons.Default.Visibility
                                                },
                                                contentDescription = "إظهار أو إخفاء مفتاح API"
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                val clipText = clipboardManager.getText()?.text?.trim().orEmpty()
                                                if (clipText.isNotEmpty()) {
                                                    draft = if (isOpenRouter) {
                                                        draft.copy(openRouterApiKey = clipText)
                                                    } else {
                                                        draft.copy(geminiApiKey = clipText)
                                                    }
                                                }
                                            },
                                            modifier = Modifier.testTag("paste_api_key_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentPaste,
                                                contentDescription = "لصق المفتاح"
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_api_key_input"),
                                singleLine = true
                            )

                            // Model Name Input
                            OutlinedTextField(
                                value = draft.aiModelName,
                                onValueChange = { draft = draft.copy(aiModelName = it) },
                                label = { Text("اسم النموذج (Model Name)") },
                                supportingText = {
                                    Text(
                                        if (isOpenRouter) {
                                            "الافتراضي لـ OpenRouter: google/gemini-2.5-flash أو openai/gpt-4o-mini"
                                        } else {
                                            "الافتراضي لـ Gemini: gemini-2.5-flash"
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_model_name_input"),
                                singleLine = true
                            )

                            // Test Connection Button
                            FilledTonalButton(
                                onClick = { viewModel.testAiConnection(draft) },
                                enabled = !isTestingAi,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("test_ai_connection_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isTestingAi) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("جارٍ اختبار الاتصال...")
                                } else {
                                    Icon(Icons.Default.Bolt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("اختبار الاتصال بالذكاء الاصطناعي")
                                }
                            }
                        }
                    }
                }

                // 3. Daily Quote Notifications Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "إشعارات إشراقة وحكمة اليوم الصباحية",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "القناة: quotes • افتراضياً الساعة 08:00 صباحاً",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = draft.quoteNotificationsEnabled,
                                    onCheckedChange = { draft = draft.copy(quoteNotificationsEnabled = it) }
                                )
                            }

                            if (draft.quoteNotificationsEnabled) {
                                HorizontalDivider()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("استخدام نافذة وقت عشوائية في الصباح")
                                    Switch(
                                        checked = draft.useRandomTimeForQuotes,
                                        onCheckedChange = { draft = draft.copy(useRandomTimeForQuotes = it) }
                                    )
                                }

                                if (draft.useRandomTimeForQuotes) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = draft.quoteRandomStartHour.toString(),
                                            onValueChange = { v ->
                                                val h = (v.toIntOrNull() ?: 7).coerceIn(0, 23)
                                                draft = draft.copy(quoteRandomStartHour = h)
                                            },
                                            label = { Text("من الساعة") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = draft.quoteRandomEndHour.toString(),
                                            onValueChange = { v ->
                                                val h = (v.toIntOrNull() ?: 9).coerceIn(0, 23)
                                                draft = draft.copy(quoteRandomEndHour = h)
                                            },
                                            label = { Text("إلى الساعة") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = draft.quoteNotificationHour.toString(),
                                            onValueChange = { v ->
                                                val h = (v.toIntOrNull() ?: 8).coerceIn(0, 23)
                                                draft = draft.copy(quoteNotificationHour = h)
                                            },
                                            label = { Text("الساعة (0-23)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = draft.quoteNotificationMinute.toString(),
                                            onValueChange = { v ->
                                                val m = (v.toIntOrNull() ?: 0).coerceIn(0, 59)
                                                draft = draft.copy(quoteNotificationMinute = m)
                                            },
                                            label = { Text("الدقيقة (0-59)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                }

                                FilledTonalButton(
                                    onClick = { viewModel.sendTestNotification(context, "quote") },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تجربة إشعار حكمة اليوم الآن")
                                }
                            }
                        }
                    }
                }

                // 4. Kinship Notifications Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "إشعارات تذكير صلة الرحم المسائية",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "القناة: contacts • افتراضياً الساعة 19:00 مساءً",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = draft.contactNotificationsEnabled,
                                    onCheckedChange = { draft = draft.copy(contactNotificationsEnabled = it) }
                                )
                            }

                            if (draft.contactNotificationsEnabled) {
                                HorizontalDivider()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("استخدام نافذة وقت عشوائية في المساء")
                                    Switch(
                                        checked = draft.useRandomTimeForContacts,
                                        onCheckedChange = { draft = draft.copy(useRandomTimeForContacts = it) }
                                    )
                                }

                                if (draft.useRandomTimeForContacts) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = draft.contactRandomStartHour.toString(),
                                            onValueChange = { v ->
                                                val h = (v.toIntOrNull() ?: 19).coerceIn(0, 23)
                                                draft = draft.copy(contactRandomStartHour = h)
                                            },
                                            label = { Text("من الساعة") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = draft.contactRandomEndHour.toString(),
                                            onValueChange = { v ->
                                                val h = (v.toIntOrNull() ?: 20).coerceIn(0, 23)
                                                draft = draft.copy(contactRandomEndHour = h)
                                            },
                                            label = { Text("إلى الساعة") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = draft.contactNotificationHour.toString(),
                                            onValueChange = { v ->
                                                val h = (v.toIntOrNull() ?: 19).coerceIn(0, 23)
                                                draft = draft.copy(contactNotificationHour = h)
                                            },
                                            label = { Text("الساعة (0-23)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = draft.contactNotificationMinute.toString(),
                                            onValueChange = { v ->
                                                val m = (v.toIntOrNull() ?: 0).coerceIn(0, 59)
                                                draft = draft.copy(contactNotificationMinute = m)
                                            },
                                            label = { Text("الدقيقة (0-59)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                }

                                FilledTonalButton(
                                    onClick = { viewModel.sendTestNotification(context, "contact") },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تجربة إشعار صلة الرحم الآن")
                                }
                            }
                        }
                    }
                }

                // 5. Missed Habit Alerts Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "تنبيهات الانقطاع عن المهام اليومية",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "القناة: habits • تنبيه تشجيعي عند تجاوز حد الانقطاع",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = draft.habitAlertsEnabled,
                                    onCheckedChange = { draft = draft.copy(habitAlertsEnabled = it) }
                                )
                            }
                            if (draft.habitAlertsEnabled) {
                                FilledTonalButton(
                                    onClick = { viewModel.sendTestNotification(context, "habit") },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تجربة تنبيه المهام اليومية الآن")
                                }
                            }
                        }
                    }
                }

                // Save Button
                item {
                    Button(
                        onClick = { viewModel.updateSettings(context, draft) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_settings_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("حفظ وجدولة جميع الإعدادات", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
