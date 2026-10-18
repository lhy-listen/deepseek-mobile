package com.lhy.deepseekmobile.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lhy.deepseekmobile.data.Message
import com.lhy.deepseekmobile.data.Role
import com.lhy.deepseekmobile.ui.AppViewModel
import com.lhy.deepseekmobile.ui.components.DsBackground
import com.lhy.deepseekmobile.ui.components.DsSnackbarHost
import com.lhy.deepseekmobile.ui.drawer.HistoryDrawer
import com.lhy.deepseekmobile.ui.theme.LocalDsPalette
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    vm: AppViewModel,
    onOpenSettings: () -> Unit,
    onOpenModes: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val session by vm.session.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val modes by vm.modes.collectAsStateWithLifecycle()
    val summaries by vm.conversations.collectAsStateWithLifecycle()

    val mode = vm.activeMode
    val model = vm.activeModel
    val messages = session.conversation?.messages.orEmpty()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var showModePicker by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Message?>(null) }
    var renameTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var deleteConversation by remember { mutableStateOf(false) }
    var deletingMessage by remember { mutableStateOf<Message?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) vm.attachImages(uris)
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.attachFiles(uris)
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.attachFolder(uri)
    }

    LaunchedEffect(session.notice) {
        val notice = session.notice
        if (!notice.isNullOrBlank()) {
            snackbar.showSnackbar(notice)
            vm.clearNotice()
        }
    }

    val listState = rememberLazyListState()
    val atBottom by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 24 }
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && (listState.firstVisibleItemIndex <= 1)) {
            listState.animateScrollToItem(0)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        drawerContent = {
            HistoryDrawer(
                summaries = summaries,
                activeId = session.conversation?.id,
                onNewChat = {
                    scope.launch { drawerState.close() }
                    vm.newChat()
                },
                onOpen = { id ->
                    scope.launch { drawerState.close() }
                    vm.openChat(id)
                },
                onRename = { summary ->
                    scope.launch { drawerState.close() }
                    renameTarget = summary.id to summary.title
                },
                onPin = { summary, pinned ->
                    vm.pinChat(summary.id, pinned)
                },
                onDelete = { summary ->
                    vm.deleteChat(summary.id)
                },
                onOpenModes = {
                    scope.launch { drawerState.close() }
                    onOpenModes()
                },
                onOpenSettings = {
                    scope.launch { drawerState.close() }
                    onOpenSettings()
                },
                onOpenAbout = {
                    scope.launch { drawerState.close() }
                    onOpenAbout()
                },
            )
        },
    ) {
        DsBackground {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    ChatTopBar(
                        title = session.conversation?.title ?: "新对话",
                        subtitle = "${mode.emoji} ${mode.name} · ${model.displayName}",
                        configured = settings.apiKey.isNotBlank(),
                        modifier = Modifier.statusBarsPadding(),
                        onMenu = { scope.launch { drawerState.open() } },
                        onNewChat = { vm.newChat() },
                        onSettings = onOpenSettings,
                        onRename = { renameTarget = session.conversation?.let { it.id to it.title } },
                        onDelete = { deleteConversation = session.conversation != null },
                    )

                    Box(Modifier.weight(1f)) {
                        if (messages.isEmpty()) {
                            EmptyChatState(
                                mode = mode,
                                model = model,
                                configured = settings.apiKey.isNotBlank(),
                                onPrompt = { vm.setDraft(it) },
                                onOpenSettings = onOpenSettings,
                            )
                        } else {
                            val lastAssistantId = messages.lastOrNull { it.role == Role.ASSISTANT }?.id
                            LazyColumn(
                                state = listState,
                                reverseLayout = true,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 10.dp),
                            ) {
                                items(messages.asReversed(), key = { it.id }) { message ->
                                    MessageItem(
                                        message = message,
                                        modelLabel = if (message.role == Role.ASSISTANT) {
                                            settings.models.firstOrNull { it.id == message.model }?.displayName
                                                ?: message.model ?: model.displayName
                                        } else model.displayName,
                                        modeEmoji = mode.emoji,
                                        isLastAssistant = message.id == lastAssistantId,
                                        streaming = session.streaming,
                                        showUsage = settings.showUsage,
                                        onEdit = { editing = message },
                                        onDelete = { deletingMessage = message },
                                        onRegenerate = { vm.regenerate() },
                                        onRetry = { vm.regenerate() },
                                    )
                                }
                            }
                        }

                        if (!atBottom && messages.isNotEmpty()) {
                            Box(
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 6.dp)
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { scope.launch { listState.animateScrollToItem(0) } },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.ArrowDownward,
                                    contentDescription = "回到底部",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    Composer(
                        session = session,
                        mode = mode,
                        model = model,
                        models = settings.models,
                        onDraft = vm::setDraft,
                        onSend = { vm.send() },
                        onStop = { vm.stop() },
                        onPickImages = { imagePicker.launch("image/*") },
                        onPickFiles = { filePicker.launch(arrayOf("*/*")) },
                        onPickFolder = { folderPicker.launch(null) },
                        onRemoveAttachment = vm::removePending,
                        onPickMode = { showModePicker = true },
                        onPickModel = { showModelPicker = true },
                    )
                }

                DsSnackbarHost(
                    hostState = snackbar,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 104.dp),
                )
            }
        }
    }

    if (showModePicker) {
        ModePickerSheet(
            modes = modes,
            selectedId = mode.id,
            onPick = {
                vm.selectMode(it.id)
                showModePicker = false
            },
            onManage = {
                showModePicker = false
                onOpenModes()
            },
            onDismiss = { showModePicker = false },
        )
    }

    if (showModelPicker) {
        ModelPickerSheet(
            models = settings.models,
            selectedId = model.id,
            baseUrl = settings.baseUrl,
            onPick = {
                vm.selectModel(it.id)
                showModelPicker = false
            },
            onManage = {
                showModelPicker = false
                onOpenSettings()
            },
            onDismiss = { showModelPicker = false },
        )
    }

    editing?.let { target ->
        EditMessageDialog(
            initial = target.text,
            onDismiss = { editing = null },
            onConfirm = { newText ->
                editing = null
                vm.editMessage(target.id, newText)
            },
        )
    }

    renameTarget?.let { (targetId, title) ->
        RenameDialog(
            initial = title,
            onDismiss = { renameTarget = null },
            onConfirm = { newTitle ->
                renameTarget = null
                vm.renameChat(targetId, newTitle)
            },
        )
    }

    if (deleteConversation) {
        AlertDialog(
            onDismissRequest = { deleteConversation = false },
            title = { Text("删除对话") },
            text = { Text("这条对话会被永久删除，无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    deleteConversation = false
                    session.conversation?.id?.let { vm.deleteChat(it) }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConversation = false }) { Text("取消") }
            },
        )
    }

    deletingMessage?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingMessage = null },
            title = { Text("删除消息") },
            text = { Text("只删除这一条消息，不会影响其他内容。") },
            confirmButton = {
                TextButton(onClick = {
                    deletingMessage = null
                    vm.deleteMessage(target.id)
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingMessage = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun ChatTopBar(
    title: String,
    subtitle: String,
    configured: Boolean,
    modifier: Modifier = Modifier,
    onMenu: () -> Unit,
    onNewChat: () -> Unit,
    onSettings: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMenu) {
            Icon(Icons.Rounded.Menu, contentDescription = "对话列表", tint = scheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!configured) {
                    Box(
                        Modifier
                            .padding(end = 6.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(scheme.error)
                    )
                }
                Text(
                    text = if (configured) subtitle else "未配置 API Key · 点右上角设置",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (configured) scheme.onSurfaceVariant else scheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onNewChat) {
            Icon(Icons.Rounded.AddComment, contentDescription = "新对话", tint = scheme.onSurfaceVariant, modifier = Modifier.size(21.dp))
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "更多", tint = scheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("重命名对话") },
                    leadingIcon = { Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(18.dp)) },
                    onClick = { menu = false; onRename() },
                )
                DropdownMenuItem(
                    text = { Text("删除对话") },
                    leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(18.dp)) },
                    onClick = { menu = false; onDelete() },
                )
                DropdownMenuItem(
                    text = { Text("模式库") },
                    leadingIcon = { Icon(Icons.Rounded.Tune, null, modifier = Modifier.size(18.dp)) },
                    onClick = { menu = false; onSettings() },
                )
            }
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "设置", tint = scheme.onSurfaceVariant, modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
private fun EditMessageDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑并重新发送") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "发送后，这条消息之后的内容会被替换。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
            ) { Text("发送") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun RenameDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名对话") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank(),
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
