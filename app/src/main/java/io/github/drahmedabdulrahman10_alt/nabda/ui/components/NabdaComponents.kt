package io.github.drahmedabdulrahman10_alt.nabda.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.NextPlan
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.drahmedabdulrahman10_alt.nabda.R
import io.github.drahmedabdulrahman10_alt.nabda.data.model.Contact
import io.github.drahmedabdulrahman10_alt.nabda.data.model.DailyTask
import io.github.drahmedabdulrahman10_alt.nabda.data.model.Quote
import io.github.drahmedabdulrahman10_alt.nabda.data.model.TaskPriority
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.AmiriFontFamily
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.PriorityHighAccent
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.PriorityHighContainerDark
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.PriorityHighContainerLight
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.PriorityLowAccent
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.PriorityLowContainerDark
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.PriorityLowContainerLight
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.PriorityMediumAccent
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.PriorityMediumContainerDark
import io.github.drahmedabdulrahman10_alt.nabda.ui.theme.PriorityMediumContainerLight
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.FixedTaskStatusUi
import io.github.drahmedabdulrahman10_alt.nabda.ui.viewmodel.PinnedContactStatusUi

/**
 * Circular progress ring displaying completion percentage.
 */
@Composable
fun ProgressRing(
    progress: Float,
    percentText: String,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    strokeWidth: Dp = 6.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = activeColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Text(
            text = percentText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Spiritual Daily Quote Card with decorative banner art and Amiri calligraphy typography.
 */
@Composable
fun QuoteCard(
    quote: Quote,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_quote_card"),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_spiritual_banner),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize(),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = Color(0xFFE9C36B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إشراقة اليوم",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFE9C36B),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = quote.text,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = AmiriFontFamily
                    ),
                    color = Color(0xFFFAF7F0)
                )
                if (!quote.author.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "— ${quote.author}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFD9E6E0)
                    )
                }
            }
        }
    }
}

/**
 * Daily rotating Kinship Contact Card (صلة الرحم) with direct Phone Dialer, WhatsApp, and "تم التواصل".
 */
@Composable
fun ContactCard(
    contact: Contact,
    onMarkContacted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_contact_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
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
                    Text(
                        text = "اقتراح صلة الرحم اليوم ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = contact.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                    )
                }
                Button(
                    onClick = onMarkContacted,
                    modifier = Modifier.testTag("contact_done_button_${contact.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تم التواصل")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { launchPhoneDialer(context, contact.phone) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("call_contact_button_${contact.id}"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "اتصال هاتفي",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اتصال")
                }

                FilledTonalButton(
                    onClick = { launchWhatsAppChat(context, contact.phone, contact.name) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("whatsapp_contact_button_${contact.id}"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "واتساب",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("واتساب")
                }
            }
        }
    }
}

/**
 * Pinned Daily Contact row (e.g., الوالدين) for daily check-in.
 */
@Composable
fun PinnedContactItem(
    status: PinnedContactStatusUi,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contact = status.contact

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("pinned_contact_item_${contact.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (status.isCompletedOnSelectedDay) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = status.isCompletedOnSelectedDay,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = "مثبّت يومياً",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (status.isCompletedOnSelectedDay) {
                        TextDecoration.LineThrough
                    } else {
                        TextDecoration.None
                    },
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = contact.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { launchPhoneDialer(context, contact.phone) }) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "اتصال بـ ${contact.name}",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = { launchWhatsAppChat(context, contact.phone, contact.name) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "مراسلة ${contact.name} عبر واتساب",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

/**
 * Fixed Task / Habit Item with streak counter and missed-days warning badge.
 */
@Composable
fun FixedTaskItem(
    status: FixedTaskStatusUi,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habit = status.fixedTask

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("fixed_task_item_${habit.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                status.isCompletedOnSelectedDay -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                status.isWarningActive -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (status.isWarningActive && !status.isCompletedOnSelectedDay) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = status.isCompletedOnSelectedDay,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("fixed_task_checkbox_${habit.id}")
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (status.isCompletedOnSelectedDay) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        },
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (habit.reminderEnabled) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%02d:%02d", habit.reminderHour, habit.reminderMinute),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Streak Pill
                if (status.currentStreakDays > 0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = PriorityMediumAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${status.currentStreakDays} يوم",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            if (status.isWarningActive && !status.isCompletedOnSelectedDay) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "لم تُنجز منذ ${status.consecutiveMissedDays} أيام — عُد إليها اليوم ولو بالقليل ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

/**
 * Daily Task Item card styled by Priority (HIGH red, MEDIUM amber, LOW neutral)
 * with checkbox strike-through, move to tomorrow ("ترحيل إلى الغد"), edit, and delete.
 */
@Composable
fun TaskItem(
    task: DailyTask,
    onToggleComplete: () -> Unit,
    onMoveToNextDay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val priority = task.priorityEnum

    val accentColor = when (priority) {
        TaskPriority.HIGH -> PriorityHighAccent
        TaskPriority.MEDIUM -> PriorityMediumAccent
        TaskPriority.LOW -> PriorityLowAccent
    }

    val containerBg by animateColorAsState(
        targetValue = if (task.isCompleted) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        } else {
            when (priority) {
                TaskPriority.HIGH -> if (isDark) PriorityHighContainerDark else PriorityHighContainerLight
                TaskPriority.MEDIUM -> if (isDark) PriorityMediumContainerDark else PriorityMediumContainerLight
                TaskPriority.LOW -> if (isDark) PriorityLowContainerDark else PriorityLowContainerLight
            }
        },
        label = "taskBgAnimation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_item_card_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = BorderStroke(
            width = 1.dp,
            color = if (task.isCompleted) {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            } else {
                accentColor.copy(alpha = 0.4f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vertical priority accent bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (task.isCompleted) accentColor.copy(alpha = 0.35f) else accentColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() },
                modifier = Modifier.testTag("task_checkbox_${task.id}")
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggleComplete() }
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isCompleted) {
                        TextDecoration.LineThrough
                    } else {
                        TextDecoration.None
                    },
                    color = if (task.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "الأولوية: ${priority.labelAr}",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!task.movedFromDayId.isNullOrBlank()) {
                        Text(
                            text = "• مرحّلة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            if (!task.isCompleted) {
                IconButton(
                    onClick = onMoveToNextDay,
                    modifier = Modifier.testTag("move_task_tomorrow_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NextPlan,
                        contentDescription = "ترحيل إلى الغد",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(
                onClick = onEdit,
                modifier = Modifier.testTag("edit_task_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "تعديل المهمة",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_task_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "حذف المهمة",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

fun launchPhoneDialer(context: Context, phone: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.trim()}")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (ignored: Exception) {
    }
}

fun launchWhatsAppChat(context: Context, phone: String, name: String) {
    // wa.me needs the full international number in digits only, so local numbers are completed
    // with the country of the SIM (or of the device language when there is no SIM).
    val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
    val country = (telephony?.simCountryIso?.takeIf { it.isNotBlank() }
        ?: telephony?.networkCountryIso?.takeIf { it.isNotBlank() }
        ?: java.util.Locale.getDefault().country).uppercase()
    val e164 = android.telephony.PhoneNumberUtils.formatNumberToE164(phone, country) ?: phone
    val cleanPhone = e164.replace(Regex("[^0-9]"), "").removePrefix("00")
    val message = Uri.encode("السلام عليكم ورحمة الله وبركاته $name، كيف حالك؟ أحببت الاطمئنان عليك")
    try {
        val uri = Uri.parse("https://wa.me/$cleanPhone?text=$message")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (ignored: Exception) {
    }
}
