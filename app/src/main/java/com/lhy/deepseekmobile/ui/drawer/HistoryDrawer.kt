package com.lhy.deepseekmobile.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lhy.deepseekmobile.data.ConversationSummary
import com.lhy.deepseekmobile.ui.components.GradientOrb
import com.lhy.deepseekmobile.ui.components.SectionLabel
import com.lhy.deepseekmobile.ui.components.formatRelativeTime
import com.lhy.deepseekmobile.ui.theme.LocalDsPalette
import java.util.Calendar

@Composable
fun HistoryDrawer(
    summaries: List<ConversationSummary>,
    activeId: String?,
    onNewChat: () -> Unit,
    onOpen: (String) -> Unit,
    onRename: (ConversationSummary) -> Unit,
    onPin: (ConversationSummary, Boolean) -> Unit,
    onDelete: (ConversationSummary) -> Unit,
    onOpenModes: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val palette = LocalDsPalette.current
    var query by remember { mutableStateOf("") }

    val filtered = remember(summaries, query) {
        if (query.isBlank()) summaries
        else summaries.filter {
            it.title.contains(query, ignoreCase = true) || it.preview.contains(query, ignoreCase = true)
        }
    }

    ModalDrawerSheet(
        drawerContainerColor = scheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 26.dp, bottomEnd = 26.dp),
        modifier = Modifier.width(312.dp),
    ) {
        Column(Modifier.fillMaxHeight()) {
            Row(
                Modifier.padding(start = 18.dp, end = 14.dp, top = 22.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GradientOrb(size = 38.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "DeepSeek",
                        style = MaterialTheme.typography.titleLarge,
                        color = scheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "API 版 · 直连模型",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Brush.linearGradient(listOf(palette.bubbleTop, palette.bubbleBottom)))
                    .clickable(onClick = onNewChat)
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.AddComment, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "新对话",
                    style = MaterialTheme.typography.labelLarge,
                    color = androidx.compose.ui.graphics.Color.White,
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(scheme.surfaceContainerHigh.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            "搜索对话",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = scheme.onSurface),
                        cursorBrush = SolidColor(scheme.primary),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (filtered.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        if (query.isBlank()) "还没有对话" else "没有匹配的对话",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val pinned = filtered.filter { it.pinned }
                    val rest = filtered.filterNot { it.pinned }
                    val today = rest.filter { isToday(it.updatedAt) }
                    val earlier = rest.filterNot { isToday(it.updatedAt) }

                    if (pinned.isNotEmpty()) {
                        item { SectionLabel("置顶", Modifier.padding(start = 8.dp, top = 8.dp)) }
                        items(pinned, key = { "p" + it.id }) { summary ->
                            DrawerItem(summary, summary.id == activeId, onOpen, onRename, onPin, onDelete)
                        }
                    }
                    if (today.isNotEmpty()) {
                        item { SectionLabel("今天", Modifier.padding(start = 8.dp, top = 8.dp)) }
                        items(today, key = { "t" + it.id }) { summary ->
                            DrawerItem(summary, summary.id == activeId, onOpen, onRename, onPin, onDelete)
                        }
                    }
                    if (earlier.isNotEmpty()) {
                        item { SectionLabel("更早", Modifier.padding(start = 8.dp, top = 8.dp)) }
                        items(earlier, key = { "e" + it.id }) { summary ->
                            DrawerItem(summary, summary.id == activeId, onOpen, onRename, onPin, onDelete)
                        }
                    }
                }
            }

            HorizontalDivider(color = palette.hairline)
            DrawerFooterRow(Icons.Rounded.Tune, "模式库", onOpenModes)
            DrawerFooterRow(Icons.Rounded.Settings, "设置", onOpenSettings)
            DrawerFooterRow(Icons.Rounded.Info, "关于", onOpenAbout)
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun DrawerItem(
    summary: ConversationSummary,
    selected: Boolean,
    onOpen: (String) -> Unit,
    onRename: (ConversationSummary) -> Unit,
    onPin: (ConversationSummary, Boolean) -> Unit,
    onDelete: (ConversationSummary) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(if (selected) scheme.primary.copy(alpha = 0.1f) else androidx.compose.ui.graphics.Color.Transparent)
                .combinedClickable(
                    onClick = { onOpen(summary.id) },
                    onLongClick = { menu = true },
                )
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(summary.modeEmoji, fontSize = 14.sp)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    summary.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) scheme.primary else scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (summary.preview.isNotBlank()) {
                    Text(
                        summary.preview,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            Text(
                formatRelativeTime(summary.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("重命名") },
                leadingIcon = { Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(18.dp)) },
                onClick = { menu = false; onRename(summary) },
            )
            DropdownMenuItem(
                text = { Text(if (summary.pinned) "取消置顶" else "置顶") },
                leadingIcon = { Icon(Icons.Rounded.PushPin, null, modifier = Modifier.size(18.dp)) },
                onClick = { menu = false; onPin(summary, !summary.pinned) },
            )
            DropdownMenuItem(
                text = { Text("删除") },
                leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(18.dp)) },
                onClick = { menu = false; onDelete(summary) },
            )
        }
    }
}

@Composable
private fun DrawerFooterRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun isToday(timestamp: Long): Boolean {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    return now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
}
