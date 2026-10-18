package com.lhy.deepseekmobile.ui.modes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lhy.deepseekmobile.data.Mode
import com.lhy.deepseekmobile.ui.AppViewModel
import com.lhy.deepseekmobile.ui.components.DsBackground
import com.lhy.deepseekmobile.ui.components.DsSnackbarHost
import com.lhy.deepseekmobile.ui.components.EmojiTile
import com.lhy.deepseekmobile.ui.components.GlassCard
import com.lhy.deepseekmobile.ui.components.TinyTag
import com.lhy.deepseekmobile.ui.theme.LocalDsPalette
import kotlinx.coroutines.launch

@Composable
fun ModesScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onEdit: (String?) -> Unit,
) {
    val modes by vm.modes.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    var menu by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Mode?>(null) }
    var resetConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                val result = vm.exportModes(uri)
                snackbar.showSnackbar(result.getOrElse { it.message ?: "导出失败" })
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val raw = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                }.getOrNull()
                if (raw == null) {
                    snackbar.showSnackbar("无法读取文件")
                } else {
                    val count = vm.importModes(raw)
                    snackbar.showSnackbar(if (count > 0) "已导入 $count 个模式" else "没有可导入的模式")
                }
            }
        }
    }

    DsBackground {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, "返回", tint = scheme.onSurfaceVariant)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("模式库", style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
                        Text(
                            "${modes.size} 个模式 · 每个模式 = 提示词 + 模型 + 参数",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onEdit(null) }) {
                        Icon(Icons.Rounded.Add, "新建模式", tint = scheme.primary)
                    }
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Rounded.MoreVert, "更多", tint = scheme.onSurfaceVariant)
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("导入模式 JSON") },
                                onClick = { menu = false; importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                            )
                            DropdownMenuItem(
                                text = { Text("导出全部模式") },
                                onClick = {
                                    menu = false
                                    exportLauncher.launch("deepseek-modes.json")
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("恢复内置模式") },
                                leadingIcon = { Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(18.dp)) },
                                onClick = { menu = false; resetConfirm = true },
                            )
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(modes, key = { it.id }) { mode ->
                        ModeCard(
                            mode = mode,
                            onClick = { onEdit(mode.id) },
                            onDuplicate = {
                                vm.duplicateMode(mode.id)?.let { copy ->
                                    scope.launch { snackbar.showSnackbar("已复制为「${copy.name}」") }
                                }
                            },
                            onDelete = { deleteTarget = mode },
                        )
                    }
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(scheme.surface.copy(alpha = 0.4f))
                                .clickable { onEdit(null) }
                                .padding(14.dp),
                        ) {
                            Column(
                                Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(Icons.Rounded.Add, null, tint = scheme.primary, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "新建模式",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = scheme.primary,
                                )
                                Text(
                                    "从零定义一套玩法",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            DsSnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 20.dp),
            )
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除模式") },
            text = { Text("删除「${target.name}」？已使用该模式的对话不会被删除。") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    vm.deleteMode(target.id)
                }) { Text("删除", color = scheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } },
        )
    }

    if (resetConfirm) {
        AlertDialog(
            onDismissRequest = { resetConfirm = false },
            title = { Text("恢复内置模式") },
            text = { Text("内置模式会恢复为初始内容，你自己创建的模式不受影响。") },
            confirmButton = {
                TextButton(onClick = {
                    resetConfirm = false
                    vm.resetModes()
                }) { Text("恢复") }
            },
            dismissButton = { TextButton(onClick = { resetConfirm = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun ModeCard(
    mode: Mode,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val palette = LocalDsPalette.current
    var menu by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), onClick = onClick) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                EmojiTile(mode.emoji, size = 40.dp)
                Spacer(Modifier.weight(1f))
                Box {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { menu = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            "更多",
                            tint = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("复制一份") },
                            leadingIcon = { Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(18.dp)) },
                            onClick = { menu = false; onDuplicate() },
                        )
                        if (!mode.builtIn) {
                            DropdownMenuItem(
                                text = { Text("删除") },
                                leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(18.dp)) },
                                onClick = { menu = false; onDelete() },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                mode.name,
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                mode.tagline.ifBlank { mode.systemPrompt.take(30) },
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp, bottom = 10.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TinyTag(
                    if (mode.builtIn) "内置" else "自定义",
                    color = if (mode.builtIn) palette.accent.primary else scheme.tertiary,
                )
                Spacer(Modifier.weight(1f))
                if (mode.temperature != null) {
                    Text(
                        "T ${String.format("%.1f", mode.temperature)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}
