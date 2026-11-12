package io.github.drahmedabdulrahman10_alt.nabda.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.drahmedabdulrahman10_alt.nabda.data.ai.BulkScheduleJsonImporter

/**
 * Dialog enabling users to:
 * 1. Generate and copy a ready-made AI prompt with one click for ChatGPT, Gemini, or Claude.
 * 2. Paste the resulting multi-day JSON schedule (`{ "schedule": [ ... ] }`) and import all
 *    weeks, days, and prioritized tasks into "نَبضة" (Nabda) automatically.
 */
@Composable
fun BulkScheduleImportDialog(
    isImporting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onClearError: () -> Unit,
    onImportJson: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var userGoalHint by remember { mutableStateOf("") }
    var jsonInput by remember { mutableStateOf("") }
    var copyFeedback by remember { mutableStateOf<String?>(null) }
    var showPromptPreview by remember { mutableStateOf(false) }

    val liveParsePreview = remember(jsonInput) {
        if (jsonInput.isBlank()) null else BulkScheduleJsonImporter.parseScheduleJson(jsonInput)
    }

    AlertDialog(
        onDismissRequest = {
            if (!isImporting) onDismiss()
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "مُستورد الجداول الذكي (JSON + AI) ",
                    style = MaterialTheme.typography.titleLarge,
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
                Text(
                    text = "خطّط لأسبوعك أو شهرك الكامل عبر أي ذكاء اصطناعي خارجي (Gemini أو ChatGPT أو Claude)، ثم الصق كود JSON هنا لتوزيع المهام تلقائياً على الأسابيع والأيام.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Step 1 Card: One-Click AI Prompt Generator
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "١. مولّد أمر التخطيط الذكي (Prompt) بضغطة واحدة ",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        OutlinedTextField(
                            value = userGoalHint,
                            onValueChange = {
                                userGoalHint = it
                                copyFeedback = null
                            },
                            label = { Text("ما خطتك أو موادك؟ (اختياري لتخصيص الـ Prompt)") },
                            placeholder = {
                                Text("مثال: مذاكرة ٣ محاضرات باطنة وجراحة هذا الأسبوع مع مراجعة يوم الخميس...")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_prompt_goal_hint_input"),
                            maxLines = 3
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    val promptText = BulkScheduleJsonImporter.buildExternalAiPrompt(userGoalHint)
                                    clipboardManager.setText(AnnotatedString(promptText))
                                    copyFeedback = "تم نسخ الـ Prompt الذكي للحافظة! الصقه الآن في ChatGPT أو Gemini أو Claude "
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("copy_ai_schedule_prompt_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("نسخ الـ Prompt الجاهز")
                            }

                            TextButton(
                                onClick = { showPromptPreview = !showPromptPreview },
                                modifier = Modifier.testTag("toggle_prompt_preview_button")
                            ) {
                                Text(if (showPromptPreview) "إخفاء" else "معاينة")
                            }
                        }

                        if (!copyFeedback.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = copyFeedback!!,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        if (showPromptPreview) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = BulkScheduleJsonImporter.buildExternalAiPrompt(userGoalHint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Step 2: Paste & Import JSON Schedule
                Text(
                    text = "٢. الصق كود JSON الناتج واستورد الجدول تلقائياً ",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val clip = clipboardManager.getText()?.text?.trim().orEmpty()
                            if (clip.isNotEmpty()) {
                                jsonInput = clip
                                onClearError()
                            }
                        },
                        enabled = !isImporting,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("paste_bulk_json_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("لصق من الحافظة")
                    }

                    OutlinedButton(
                        onClick = {
                            jsonInput = BulkScheduleJsonImporter.buildSampleScheduleJson()
                            onClearError()
                        },
                        enabled = !isImporting,
                        modifier = Modifier.testTag("fill_sample_bulk_json_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مثال تجريبي")
                    }
                }

                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = {
                        jsonInput = it
                        if (errorMessage != null) onClearError()
                    },
                    label = { Text("كود جدول JSON (حقل schedule)") },
                    placeholder = {
                        Text("{\n  \"schedule\": [\n    { \"date\": \"2026-11-02\", \"tasks\": [ ... ] }\n  ]\n}")
                    },
                    enabled = !isImporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .testTag("bulk_json_input_field"),
                    maxLines = 10
                )

                // Live JSON Validation Preview Banner
                if (liveParsePreview != null) {
                    liveParsePreview.fold(
                        onSuccess = { days ->
                            val totalTasks = days.sumOf { it.tasks.size }
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bulk_json_valid_preview_banner")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "كود JSON صالح وجاهز: ${days.size} أيام مجدولة • إجمالي $totalTasks مهمة (${days.first().dateIso} ← ${days.last().dateIso})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        },
                        onFailure = { parseErr ->
                            if (errorMessage == null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = parseErr.localizedMessage ?: "صيغة JSON غير مكتملة بعد",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bulk_json_error_banner")
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (isImporting) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "جارٍ إنشاء الأسابيع والأيام وتوزيع المهام...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onImportJson(jsonInput) },
                enabled = !isImporting && jsonInput.isNotBlank(),
                modifier = Modifier.testTag("confirm_bulk_json_import_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("استيراد وتوزيع المهام ")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isImporting
            ) {
                Text("إلغاء")
            }
        }
    )
}
