package com.lhy.deepseekmobile.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lhy.deepseekmobile.data.Attachment
import com.lhy.deepseekmobile.data.AttachmentKind
import com.lhy.deepseekmobile.data.ImageRoute
import com.lhy.deepseekmobile.data.Message
import com.lhy.deepseekmobile.data.Role
import com.lhy.deepseekmobile.data.Usage
import com.lhy.deepseekmobile.ui.components.GradientOrb
import com.lhy.deepseekmobile.ui.components.MarkdownText
import com.lhy.deepseekmobile.ui.components.TinyTag
import com.lhy.deepseekmobile.ui.theme.LocalDsPalette
import java.io.File
import kotlin.math.sin

@Composable
fun MessageItem(
    message: Message,
    modelLabel: String,
    modeEmoji: String,
    isLastAssistant: Boolean,
    streaming: Boolean,
    showUsage: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRegenerate: () -> Unit,
    onRetry: () -> Unit,
) {
    when (message.role) {
        Role.USER -> UserMessage(message, onEdit, onDelete)
        Role.ASSISTANT -> AssistantMessage(
            message = message,
            modelLabel = modelLabel,
            modeEmoji = modeEmoji,
            isLastAssistant = isLastAssistant,
            streaming = streaming,
            showUsage = showUsage,
            onDelete = onDelete,
            onRegenerate = onRegenerate,
            onRetry = onRetry,
        )
        Role.SYSTEM -> Unit
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserMessage(message: Message, onEdit: () -> Unit, onDelete: () -> Unit) {
    val palette = LocalDsPalette.current
    val clipboard = LocalClipboardManager.current
    var menu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Column(modifier = Modifier.widthIn(max = 330.dp), horizontalAlignment = Alignment.End) {
            if (message.attachments.isNotEmpty()) {
                AttachmentStrip(
                    attachments = message.attachments,
                    modifier = Modifier.padding(bottom = 6.dp),
                    alignEnd = true,
                )
            }
            if (message.text.isNotBlank()) {
                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp))
                            .background(Brush.linearGradient(listOf(palette.bubbleTop, palette.bubbleBottom)))
                            .combinedClickable(
                                onClick = { },
                                onLongClick = { menu = true },
                            )
                            .padding(horizontal = 15.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                        )
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("复制") },
                            leadingIcon = { Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                clipboard.setText(AnnotatedString(message.text))
                                menu = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("编辑并重发") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(18.dp)) },
                            onClick = { menu = false; onEdit() },
                        )
                        DropdownMenuItem(
                            text = { Text("删除") },
                            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(18.dp)) },
                            onClick = { menu = false; onDelete() },
                        )
                    }
                }
            }
            Text(
                text = if (message.stopped) "已停止" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = if (message.stopped) Modifier.padding(top = 3.dp, end = 4.dp) else Modifier,
            )
        }
    }
}

@Composable
private fun AssistantMessage(
    message: Message,
    modelLabel: String,
    modeEmoji: String,
    isLastAssistant: Boolean,
    streaming: Boolean,
    showUsage: Boolean,
    onDelete: () -> Unit,
    onRegenerate: () -> Unit,
    onRetry: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current
    var menu by remember { mutableStateOf(false) }
    var reasoningExpanded by remember(message.id, message.pending) { mutableStateOf(message.pending) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        GradientOrb(emoji = modeEmoji, size = 30.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = modelLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
                if (message.reasoning.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    TinyTag("思考")
                }
                if (message.stopped) {
                    Spacer(Modifier.width(6.dp))
                    TinyTag("已停止", color = scheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(26.dp)) {
                        Text("⋯", color = scheme.onSurfaceVariant, fontSize = 16.sp)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("复制回答") },
                            onClick = {
                                clipboard.setText(AnnotatedString(message.text))
                                menu = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (isLastAssistant) "重新生成" else "重新生成（需为最后一条）") },
                            enabled = isLastAssistant,
                            onClick = { menu = false; onRegenerate() },
                        )
                        DropdownMenuItem(
                            text = { Text("删除这条回答") },
                            onClick = { menu = false; onDelete() },
                        )
                    }
                }
            }

            if (message.reasoning.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(scheme.surfaceContainerLow)
                        .combinedClickableNoIndication { reasoningExpanded = !reasoningExpanded }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "思考过程",
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${message.reasoning.length} 字",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            if (reasoningExpanded) "收起" else "展开",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.primary,
                        )
                    }
                    AnimatedVisibility(visible = reasoningExpanded) {
                        Text(
                            text = message.reasoning,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }

            if (message.text.isBlank() && message.pending && message.error == null) {
                TypingIndicator()
            } else if (message.text.isNotBlank()) {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    MarkdownText(
                        text = message.text,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                if (streaming && message.pending) {
                    BlinkingCursor()
                }
            } else if (message.stopped && message.error == null) {
                Text(
                    text = "已停止生成，可以重新发送或换个说法。",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            if (message.error != null) {
                ErrorCard(message.error, onRetry)
            }

            if (!message.pending && message.text.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    ActionIcon(Icons.Rounded.ContentCopy, "复制") {
                        clipboard.setText(AnnotatedString(message.text))
                    }
                    if (isLastAssistant) {
                        ActionIcon(Icons.Rounded.Refresh, "重新生成") { onRegenerate() }
                    }
                    ActionIcon(Icons.Rounded.DeleteOutline, "删除") { onDelete() }
                    Spacer(Modifier.weight(1f))
                    if (showUsage) {
                        val usage = message.usage
                        val text = buildString {
                            if (usage != null && usage.totalTokens > 0) append("${usage.totalTokens} tokens")
                            if (message.durationMs > 0) {
                                if (isNotEmpty()) append(" · ")
                                append(String.format("%.1fs", message.durationMs / 1000.0))
                            }
                        }
                        if (text.isNotBlank()) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant.copy(alpha = 0.55f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(30.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        color = scheme.errorContainer.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("请求失败", style = MaterialTheme.typography.labelMedium, color = scheme.error)
                Spacer(Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .combinedClickableNoIndication(onClick = onRetry)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Rounded.RestartAlt, null, tint = scheme.error, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("重试", style = MaterialTheme.typography.labelSmall, color = scheme.error)
                }
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onErrorContainer,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(3) { index ->
            val phase by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, delayMillis = index * 120, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "dot$index",
            )
            val scale = 0.55f + 0.45f * ((sin((phase * 2 * Math.PI).toFloat()) + 1f) / 2f)
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(scheme.primary.copy(alpha = 0.35f + 0.45f * scale))
            )
        }
    }
}

@Composable
private fun BlinkingCursor() {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "cursorAlpha",
    )
    Box(
        Modifier
            .padding(top = 2.dp)
            .size(width = 7.dp, height = 14.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttachmentStrip(
    attachments: List<Attachment>,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
    onRemove: ((String) -> Unit)? = null,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (alignEnd) Arrangement.spacedBy(8.dp, Alignment.End) else Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        attachments.forEach { attachment ->
            when (attachment.kind) {
                AttachmentKind.IMAGE -> ImageAttachmentChip(attachment, onRemove)
                AttachmentKind.FILE -> FileAttachmentChip(attachment, onRemove)
                AttachmentKind.FOLDER -> FolderAttachmentChip(attachment, onRemove)
            }
        }
    }
}

@Composable
private fun ImageAttachmentChip(attachment: Attachment, onRemove: ((String) -> Unit)?) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(112.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.surfaceContainerHigh)
    ) {
        val path = attachment.localPath
        if (path != null && File(path).exists()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(File(path)).crossfade(true).build(),
                contentDescription = attachment.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Icon(
                Icons.Rounded.Description,
                null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center).size(24.dp),
            )
        }
        if (attachment.route == ImageRoute.OCR || attachment.route == ImageRoute.VISION_MODEL) {
            TinyTag(
                text = if (attachment.route == ImageRoute.OCR) "OCR" else "视觉",
                modifier = Modifier.align(Alignment.BottomStart).padding(5.dp),
                color = Color.White,
            )
        }
        if (onRemove != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .combinedClickableNoIndication { onRemove(attachment.id) },
                contentAlignment = Alignment.Center,
            ) {
                Text("×", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun FileAttachmentChip(attachment: Attachment, onRemove: ((String) -> Unit)?) {
    AttachmentChip(
        icon = Icons.Rounded.Description,
        title = attachment.name,
        subtitle = formatSize(attachment.sizeBytes) + " · 文本",
        onRemove = onRemove?.let { { it(attachment.id) } },
    )
}

@Composable
private fun FolderAttachmentChip(attachment: Attachment, onRemove: ((String) -> Unit)?) {
    AttachmentChip(
        icon = Icons.Rounded.Folder,
        title = attachment.name,
        subtitle = "${attachment.fileCount} 个文件",
        onRemove = onRemove?.let { { it(attachment.id) } },
    )
}

@Composable
private fun AttachmentChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onRemove: (() -> Unit)?,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .widthIn(max = 260.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.surfaceContainerHigh.copy(alpha = 0.9f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = scheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f, fill = false)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (onRemove != null) {
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .combinedClickableNoIndication(onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Text("×", color = scheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    }
}

fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000.0)
    bytes >= 1_000 -> String.format("%.1f KB", bytes / 1000.0)
    bytes > 0 -> "$bytes B"
    else -> "文本"
}

@Composable
fun UsageLine(usage: Usage) {
    Text(
        text = "输入 ${usage.promptTokens} · 输出 ${usage.completionTokens} tokens",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    )
}

@Composable
fun Modifier.combinedClickableNoIndication(onClick: () -> Unit): Modifier =
    this.combinedClickable(
        interactionSource = null,
        indication = null,
        onClick = onClick,
    )
