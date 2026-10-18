package com.lhy.deepseekmobile.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lhy.deepseekmobile.data.Attachment
import com.lhy.deepseekmobile.data.AttachmentKind
import com.lhy.deepseekmobile.data.Mode
import com.lhy.deepseekmobile.data.ModelSpec
import com.lhy.deepseekmobile.ui.ChatSession
import com.lhy.deepseekmobile.ui.components.EmojiTile
import com.lhy.deepseekmobile.ui.theme.LocalDsPalette
import com.lhy.deepseekmobile.util.Tokens
import java.io.File

@Composable
fun Composer(
    session: ChatSession,
    mode: Mode,
    model: ModelSpec,
    models: List<ModelSpec>,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onPickImages: () -> Unit,
    onPickFiles: () -> Unit,
    onPickFolder: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onPickMode: () -> Unit,
    onPickModel: () -> Unit,
) {
    val palette = LocalDsPalette.current
    val scheme = MaterialTheme.colorScheme
    var attachSheet by remember { mutableStateOf(false) }

    val hasContent = session.draft.isNotBlank() || session.pending.isNotEmpty()
    val canSend = hasContent && !session.streaming

    Column(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            .padding(horizontal = 10.dp)
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ModeChip(mode) { onPickMode() }
            Spacer(Modifier.width(6.dp))
            ModelChip(model, models.size > 1) { onPickModel() }
            Spacer(Modifier.weight(1f))
            if (session.contextTokens > 0) {
                Text(
                    text = "${Tokens.format(session.contextTokens)} ctx",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.55f),
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(26.dp),
            color = scheme.surface.copy(alpha = if (palette.dark) 0.86f else 0.96f),
            border = BorderStroke(1.dp, palette.hairline),
            tonalElevation = 0.dp,
        ) {
            Column(Modifier.padding(6.dp)) {
                if (session.pending.isNotEmpty()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        session.pending.forEach { attachment ->
                            ComposerAttachment(attachment, onRemoveAttachment)
                        }
                    }
                }

                if (session.phase != null) {
                    Row(
                        Modifier.padding(start = 12.dp, top = 6.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.6.dp,
                            color = scheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            session.phase,
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    IconButton(
                        onClick = { attachSheet = true },
                        modifier = Modifier.size(42.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "添加附件",
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                            .padding(vertical = 9.dp)
                    ) {
                        if (session.draft.isBlank()) {
                            Text(
                                text = "发消息，或粘贴长文…",
                                style = MaterialTheme.typography.bodyLarge,
                                color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        BasicTextField(
                            value = session.draft,
                            onValueChange = onDraft,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                            cursorBrush = SolidColor(scheme.primary),
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Box(
                        Modifier
                            .padding(start = 6.dp)
                            .size(42.dp)
                            .clip(CircleShape)
                            .then(
                                if (session.streaming) {
                                    Modifier
                                        .background(scheme.surfaceContainerHigh)
                                        .clickable { onStop() }
                                } else if (canSend) {
                                    Modifier
                                        .background(
                                            Brush.linearGradient(listOf(palette.bubbleTop, palette.bubbleBottom))
                                        )
                                        .clickable { onSend() }
                                } else {
                                    Modifier
                                        .background(scheme.surfaceContainerHigh)
                                        .alpha(0.7f)
                                }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (session.streaming) Icons.Rounded.Stop else Icons.Rounded.ArrowUpward,
                            contentDescription = if (session.streaming) "停止" else "发送",
                            tint = when {
                                session.streaming -> scheme.onSurface
                                canSend -> Color.White
                                else -> scheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }

    if (attachSheet) {
        AttachmentSheet(
            onDismiss = { attachSheet = false },
            onImages = { attachSheet = false; onPickImages() },
            onFiles = { attachSheet = false; onPickFiles() },
            onFolder = { attachSheet = false; onPickFolder() },
        )
    }
}

@Composable
private fun ModeChip(mode: Mode, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .background(scheme.surfaceContainerLow.copy(alpha = 0.7f))
            .padding(start = 8.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(mode.emoji, fontSize = 12.sp)
        Spacer(Modifier.width(5.dp))
        Text(
            mode.name,
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurface,
            maxLines = 1,
        )
        Spacer(Modifier.width(2.dp))
        Icon(
            Icons.Rounded.ChevronRight,
            null,
            tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(12.dp),
        )
    }
}

@Composable
private fun ModelChip(model: ModelSpec, clickable: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (model.reasoning) scheme.primary else scheme.tertiary)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            model.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
        )
        if (clickable) {
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun ComposerAttachment(attachment: Attachment, onRemove: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box {
        when (attachment.kind) {
            AttachmentKind.IMAGE -> {
                val path = attachment.localPath
                Box(
                    Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(scheme.surfaceContainerHigh)
                ) {
                    if (path != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(File(path)).crossfade(true).build(),
                            contentDescription = attachment.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                }
            }
            AttachmentKind.FILE, AttachmentKind.FOLDER -> {
                Row(
                    Modifier
                        .height(58.dp)
                        .widthIn(max = 210.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(scheme.surfaceContainerHigh)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (attachment.kind == AttachmentKind.FOLDER) Icons.Rounded.FolderOpen else Icons.Rounded.InsertDriveFile,
                        null,
                        tint = scheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            attachment.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            if (attachment.kind == AttachmentKind.FOLDER) "${attachment.fileCount} 个文件" else formatSize(attachment.sizeBytes),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onRemove(attachment.id) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(11.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentSheet(
    onDismiss: () -> Unit,
    onImages: () -> Unit,
    onFiles: () -> Unit,
    onFolder: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 28.dp)) {
            Text(
                "添加内容",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            SheetOption(
                icon = { Icon(Icons.Rounded.Image, null, tint = MaterialTheme.colorScheme.primary) },
                title = "图片",
                subtitle = "截图、照片；模型不支持视觉时自动 OCR",
                onClick = onImages,
            )
            SheetOption(
                icon = { Icon(Icons.Rounded.InsertDriveFile, null, tint = MaterialTheme.colorScheme.primary) },
                title = "文件",
                subtitle = "代码、Markdown、CSV、JSON 等文本文件",
                onClick = onFiles,
            )
            SheetOption(
                icon = { Icon(Icons.Rounded.FolderOpen, null, tint = MaterialTheme.colorScheme.primary) },
                title = "文件夹",
                subtitle = "扫描整个目录，按需读取文本内容",
                onClick = onFolder,
            )
        }
    }
}

@Composable
private fun SheetOption(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/* ------------------------------------------------------------ picker sheets */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModePickerSheet(
    modes: List<Mode>,
    selectedId: String?,
    onPick: (Mode) -> Unit,
    onManage: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "选择模式",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "管理模式",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onManage)
                        .padding(6.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                modes.forEach { mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPick(mode) }
                            .background(
                                if (mode.id == selectedId) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else Color.Transparent
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EmojiTile(mode.emoji, size = 40.dp, gradient = mode.id == selectedId)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                mode.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                mode.tagline.ifBlank { mode.systemPrompt.take(36) },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (mode.id == selectedId) {
                            Icon(
                                Icons.Rounded.Check,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPickerSheet(
    models: List<ModelSpec>,
    selectedId: String?,
    baseUrl: String,
    onPick: (ModelSpec) -> Unit,
    onManage: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "选择模型",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "管理模型",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onManage)
                        .padding(6.dp),
                )
            }
            Text(
                baseUrl,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
            )
            models.forEach { model ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onPick(model) }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            model.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            model.id,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (model.vision) TinyBadge("视觉")
                    if (model.reasoning) TinyBadge("推理")
                    if (model.id == selectedId) {
                        Icon(
                            Icons.Rounded.Check,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TinyBadge(text: String) {
    Box(
        Modifier
            .padding(end = 6.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}
