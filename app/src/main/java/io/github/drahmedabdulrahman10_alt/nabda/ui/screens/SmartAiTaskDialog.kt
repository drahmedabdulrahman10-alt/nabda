package io.github.drahmedabdulrahman10_alt.nabda.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.drahmedabdulrahman10_alt.nabda.data.model.DailyTask
import io.github.drahmedabdulrahman10_alt.nabda.data.model.TaskPriority
import io.github.drahmedabdulrahman10_alt.nabda.ui.components.BulkScheduleImportDialog
import io.github.drahmedabdulrahman10_alt.nabda.ui.components.ContactCard
import io.github.drahmedabdulrahman10_alt.nabda.ui.components.FixedTaskItem
import io.github.drahmedabdulrahman10_alt.nabda.ui.components.PinnedContactItem
import io.github.drahmedabdulrahman10_alt.nabda.ui.components.ProgressRing
import io.github.drahmedabdulrahman10_alt.nabda.ui.components.QuoteCard
import io.github.drahmedabdulrahman10_alt.nabda.ui.components.TaskItem
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.PriorityHighAccent
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.PriorityLowAccent
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.PriorityMediumAccent
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.DailyScreenData
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.DailyTasksViewModel
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.UiState
import io.github.drahmedabdulrahman10_alt.nabda.util.ArabicCalendarUtils
import java.util.Date

/**
 * Smart Voice & Natural Language Task Creation Dialog (`SmartAiTaskDialog`).
 * Supports Android native Speech-to-Text (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`)
 * in Arabic (`ar-SA` / `ar-EG`) and AI priority extraction via `AiTaskAssistantService`.
 */
@Composable
fun SmartAiTaskDialog(
    isParsing: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onOpenAiSettings: () -> Unit,
    onAnalyzeAndAdd: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var speechStatusHint by remember { mutableStateOf<String?>(null) }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = matches?.firstOrNull()?.trim().orEmpty()
            if (recognizedText.isNotEmpty()) {
                inputText = if (inputText.isBlank()) {
                    recognizedText
                } else {
                    "${inputText.trim()} ، $recognizedText"
                }
                speechStatusHint = "تم التقاط الصوت وإضافته للنص بنجاح "
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("إضافة مهام ذكية (صوت / نص)")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "تحدث بصوتك أو اكتب مهامك بحرية، وسيقوم المساعد الذكي باستخراج كل مهمة وتحديد أولويتها تلقائياً.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    enabled = !isParsing,
                    label = { Text("اكتب أو أملِ مهامك اليومية") },
                    placeholder = {
                        Text(
                            "تحدث أو اكتب مهامك بحرية.. مثال: النهارده لازم أذاكر محاضرة الجراحة ضروري، وأشتري طلبات للبيت، وبالليل أقرأ فصل من كتاب..."
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .testTag("smart_ai_task_input"),
                    maxLines = 6
                )

                // Prominent Microphone Button (التحدث بالصوت)
                FilledTonalButton(
                    onClick = {
                        speechStatusHint = null
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-EG")
                            putExtra(
                                RecognizerIntent.EXTRA_PROMPT,
                                "تحدث بمهامك اليومية باللغة العربية..."
                            )
                        }
                        try {
                            speechRecognizerLauncher.launch(intent)
                        } catch (e: ActivityNotFoundException) {
                            speechStatusHint =
                                "خدمة التعرف على الصوت غير متوفرة على هذا الجهاز حالياً؛ يمكنك كتابة مهامك مباشرة في الحقل أعلاه."
                        }
                    },
                    enabled = !isParsing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_speech_input_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "التحدث بالصوت",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "التحدث بالصوت (إملاء المهام بالعربية)",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!speechStatusHint.isNullOrBlank()) {
                    Text(
                        text = speechStatusHint!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            TextButton(
                                onClick = onOpenAiSettings,
                                modifier = Modifier.testTag("open_ai_settings_from_dialog_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فتح إعدادات الذكاء الاصطناعي ومفتاح الـ API")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAnalyzeAndAdd(inputText) },
                enabled = inputText.isNotBlank() && !isParsing,
                modifier = Modifier.testTag("analyze_and_add_ai_tasks_button")
            ) {
                if (isParsing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جارٍ التحليل...")
                } else {
                    Text("تحليل وإضافة المهام ")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isParsing
            ) {
                Text("إلغاء")
            }
        }
    )
}

