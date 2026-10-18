package com.lhy.deepseekmobile.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lhy.deepseekmobile.data.ModelSpec
import com.lhy.deepseekmobile.ui.AppViewModel
import com.lhy.deepseekmobile.ui.components.DsBackground
import com.lhy.deepseekmobile.ui.components.DsSnackbarHost
import com.lhy.deepseekmobile.ui.components.GlassCard
import com.lhy.deepseekmobile.ui.components.SectionLabel
import com.lhy.deepseekmobile.ui.components.TinyTag
import com.lhy.deepseekmobile.ui.theme.Accent
import com.lhy.deepseekmobile.ui.theme.LocalDsPalette
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onAbout: () -> Unit,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val palette = LocalDsPalette.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    var apiKey by remember { mutableStateOf(settings.apiKey) }
    var baseUrl by remember { mutableStateOf(settings.baseUrl) }
    var globalPrompt by remember { mutableStateOf(settings.globalSystemPrompt) }
    var visionBaseUrl by remember { mutableStateOf(settings.visionBaseUrl) }
    var visionApiKey by remember { mutableStateOf(settings.visionApiKey) }
    var visionModel by remember { mutableStateOf(settings.visionModel) }
    var asrBaseUrl by remember { mutableStateOf(settings.asrBaseUrl) }
    var asrApiKey by remember { mutableStateOf(settings.asrApiKey) }
    var asrModel by remember { mutableStateOf(settings.asrModel) }
    var glossary by remember { mutableStateOf(settings.voiceGlossary) }

    var showKey by remember { mutableStateOf(false) }
    var showVisionKey by remember { mutableStateOf(false) }
    var showAsrKey by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var addModel by remember { mutableStateOf(false) }
    var clearConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                val result = vm.exportBackup(uri)
                snackbar.showSnackbar(result.getOrElse { it.message ?: "导出失败" })
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val result = vm.importBackup(uri)
                snackbar.showSnackbar(result.getOrElse { it.message ?: "导入失败" })
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
                    Text(
                        "设置",
                        style = MaterialTheme.typography.titleLarge,
                        color = scheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }

                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SectionLabel("连接")
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            SettingsTextField(
                                label = "API Key",
                                value = apiKey,
                                onChange = {
                                    apiKey = it
                                    vm.updateSettings { s -> s.copy(apiKey = it.trim()) }
                                },
                                placeholder = "sk-…",
                                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                trailing = {
                                    IconButton(onClick = { showKey = !showKey }) {
                                        Icon(
                                            if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                            null,
                                            tint = scheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                },
                            )
                            Spacer(Modifier.height(10.dp))
                            SettingsTextField(
                                label = "接口地址",
                                value = baseUrl,
                                onChange = {
                                    baseUrl = it
                                    vm.updateSettings { s -> s.copy(baseUrl = it.trim()) }
                                },
                                placeholder = "https://api.deepseek.com",
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "DeepSeek 官方接口填 https://api.deepseek.com；也支持任何 OpenAI 兼容网关。",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = {
                                        testing = true
                                        scope.launch {
                                            val result = vm.testConnection()
                                            testing = false
                                            snackbar.showSnackbar(result.getOrElse { it.message ?: "连接失败" })
                                        }
                                    },
                                    enabled = !testing && apiKey.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = scheme.primary),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    if (testing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 1.8.dp,
                                            color = scheme.onPrimary,
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(if (testing) "连接中…" else "测试连接")
                                }
                                Spacer(Modifier.width(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Key, null, tint = scheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "只保存在本机",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    SectionLabel("模型")
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            settings.models.forEach { model ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                model.displayName,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = scheme.onSurface,
                                            )
                                            if (model.id == settings.currentModelId) {
                                                Spacer(Modifier.width(6.dp))
                                                TinyTag("默认")
                                            }
                                        }
                                        Text(
                                            model.id,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = scheme.onSurfaceVariant,
                                        )
                                    }
                                    if (model.vision) TinyTag("视觉", color = scheme.secondary)
                                    if (model.reasoning) TinyTag("推理", color = scheme.tertiary)
                                    if (settings.models.size > 1) {
                                        IconButton(
                                            onClick = { vm.removeModel(model.id) },
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Icon(
                                                Icons.Rounded.DeleteOutline,
                                                "删除模型",
                                                tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { addModel = true }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.Add, null, tint = scheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("添加模型", style = MaterialTheme.typography.labelLarge, color = scheme.primary)
                            }
                        }
                    }

                    SectionLabel("图片理解")
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("使用独立视觉模型", style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                                    Text(
                                        "主模型不支持读图时，先把图片交给视觉模型描述，再喂给主模型。",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = settings.visionEnabled,
                                    onCheckedChange = { vm.updateSettings { s -> s.copy(visionEnabled = it) } },
                                )
                            }
                            if (settings.visionEnabled) {
                                Spacer(Modifier.height(10.dp))
                                SettingsTextField(
                                    label = "视觉接口地址（留空沿用主接口）",
                                    value = visionBaseUrl,
                                    onChange = {
                                        visionBaseUrl = it
                                        vm.updateSettings { s -> s.copy(visionBaseUrl = it.trim()) }
                                    },
                                    placeholder = "https://dashscope.aliyuncs.com/compatible-mode/v1",
                                )
                                Spacer(Modifier.height(10.dp))
                                SettingsTextField(
                                    label = "视觉模型 Key",
                                    value = visionApiKey,
                                    onChange = {
                                        visionApiKey = it
                                        vm.updateSettings { s -> s.copy(visionApiKey = it.trim()) }
                                    },
                                    placeholder = "sk-…",
                                    visualTransformation = if (showVisionKey) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailing = {
                                        IconButton(onClick = { showVisionKey = !showVisionKey }) {
                                            Icon(
                                                if (showVisionKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                null,
                                                tint = scheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    },
                                )
                                Spacer(Modifier.height(10.dp))
                                SettingsTextField(
                                    label = "视觉模型名",
                                    value = visionModel,
                                    onChange = {
                                        visionModel = it
                                        vm.updateSettings { s -> s.copy(visionModel = it.trim()) }
                                    },
                                    placeholder = "qwen-vl-max / gpt-4o-mini / glm-4v-plus",
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "未配置视觉模型时，图片会自动走本地 OCR（离线，中文可识别）。",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    SectionLabel("全局指令")
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            OutlinedTextField(
                                value = globalPrompt,
                                onValueChange = {
                                    globalPrompt = it
                                    vm.updateSettings { s -> s.copy(globalSystemPrompt = it) }
                                },
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                placeholder = { Text("会拼在每个模式的提示词最前面，比如：永远用简体中文；先给结论。") },
                                textStyle = MaterialTheme.typography.bodySmall,
                                colors = fieldColors(),
                            )
                        }
                    }

                    SectionLabel("外观")
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text("主题", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (value, label) ->
                                    FilterChip(
                                        selected = settings.themeMode == value,
                                        onClick = { vm.updateSettings { s -> s.copy(themeMode = value) } },
                                        label = { Text(label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = scheme.primary.copy(alpha = 0.16f),
                                        ),
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Text("强调色", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Accent.entries.forEach { accent ->
                                    val selected = accent.id == settings.accentId
                                    Box(
                                        Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Brush.linearGradient(listOf(accent.primary, accent.secondary)))
                                            .border(
                                                width = if (selected) 2.dp else 0.dp,
                                                color = if (selected) scheme.onSurface else Color.Transparent,
                                                shape = CircleShape,
                                            )
                                            .clickable { vm.updateSettings { s -> s.copy(accentId = accent.id) } },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (selected) {
                                            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            if (settings.themeMode == "dark" || settings.themeMode == "system") {
                                Spacer(Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("纯黑背景", style = MaterialTheme.typography.labelMedium, color = scheme.onSurface)
                                        Text(
                                            "OLED 省电，对比更强",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = scheme.onSurfaceVariant,
                                        )
                                    }
                                    Switch(
                                        checked = settings.amoled,
                                        onCheckedChange = { vm.updateSettings { s -> s.copy(amoled = it) } },
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("字号", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "${(settings.fontScale * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = scheme.primary,
                                )
                            }
                            Slider(
                                value = settings.fontScale,
                                onValueChange = { vm.updateSettings { s -> s.copy(fontScale = it) } },
                                valueRange = 0.85f..1.25f,
                                steps = 7,
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    thumbColor = scheme.primary,
                                    activeTrackColor = scheme.primary.copy(alpha = 0.7f),
                                ),
                            )
                        }
                    }

                    SectionLabel("对话")
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("显示 token 用量", style = MaterialTheme.typography.labelMedium, color = scheme.onSurface)
                                    Text(
                                        "在回答下方显示 token 数与耗时",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = settings.showUsage,
                                    onCheckedChange = { vm.updateSettings { s -> s.copy(showUsage = it) } },
                                )
                            }
                        }
                    }

                    SectionLabel("语音输入")
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                "识别方式",
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = settings.asrMode == "system",
                                    onClick = { vm.updateSettings { s -> s.copy(asrMode = "system") } },
                                    label = { Text("系统识别") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = scheme.primary.copy(alpha = 0.16f),
                                    ),
                                )
                                FilterChip(
                                    selected = settings.asrMode == "cloud",
                                    onClick = { vm.updateSettings { s -> s.copy(asrMode = "cloud") } },
                                    label = { Text("云端转写") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = scheme.primary.copy(alpha = 0.16f),
                                    ),
                                )
                            }
                            Text(
                                if (settings.asrMode == "system") {
                                    "调用手机自带的语音服务（Google / 厂商），无需额外 Key；不支持时请改用云端转写。"
                                } else {
                                    "上传 WAV 到兼容 /audio/transcriptions 的接口，中文识别通常比系统服务更稳。"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )

                            if (settings.asrMode == "cloud") {
                                Spacer(Modifier.height(10.dp))
                                SettingsTextField(
                                    label = "转写接口地址",
                                    value = asrBaseUrl,
                                    onChange = {
                                        asrBaseUrl = it
                                        vm.updateSettings { s -> s.copy(asrBaseUrl = it.trim()) }
                                    },
                                    placeholder = "https://api.openai.com/v1",
                                )
                                Spacer(Modifier.height(10.dp))
                                SettingsTextField(
                                    label = "转写 Key",
                                    value = asrApiKey,
                                    onChange = {
                                        asrApiKey = it
                                        vm.updateSettings { s -> s.copy(asrApiKey = it.trim()) }
                                    },
                                    placeholder = "sk-…",
                                    visualTransformation = if (showAsrKey) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailing = {
                                        IconButton(onClick = { showAsrKey = !showAsrKey }) {
                                            Icon(
                                                if (showAsrKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                null,
                                                tint = scheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    },
                                )
                                Spacer(Modifier.height(10.dp))
                                SettingsTextField(
                                    label = "转写模型",
                                    value = asrModel,
                                    onChange = {
                                        asrModel = it
                                        vm.updateSettings { s -> s.copy(asrModel = it.trim()) }
                                    },
                                    placeholder = "whisper-1 / FunAudioLLM/SenseVoiceSmall / whisper-large-v3",
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "兼容 OpenAI Whisper、SiliconFlow、Groq 等提供 /audio/transcriptions 的服务。",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant,
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "自动纠正转写",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = scheme.onSurface,
                                    )
                                    Text(
                                        "用当前对话模型修正同音字、断句与口头禅，再放进输入框",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = settings.voiceCorrect,
                                    onCheckedChange = { vm.updateSettings { s -> s.copy(voiceCorrect = it) } },
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(
                                "术语表（每行一个，纠正时优先保留）",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = glossary,
                                onValueChange = {
                                    glossary = it
                                    vm.updateSettings { s -> s.copy(voiceGlossary = it) }
                                },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                placeholder = { Text("例如：\nDeepSeek\n端侧推理\n张三") },
                                textStyle = MaterialTheme.typography.bodySmall,
                                colors = fieldColors(),
                            )
                        }
                    }

                    SectionLabel("数据")
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            ActionRow(
                                icon = { Icon(Icons.Rounded.Upload, null, tint = scheme.primary, modifier = Modifier.size(18.dp)) },
                                title = "导出备份",
                                subtitle = "对话、模式、设置打包成一个 JSON",
                                onClick = {
                                    val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())
                                    exportLauncher.launch("deepseek-backup-$stamp.json")
                                },
                            )
                            ActionRow(
                                icon = { Icon(Icons.Rounded.Download, null, tint = scheme.primary, modifier = Modifier.size(18.dp)) },
                                title = "导入备份",
                                subtitle = "合并恢复，不会覆盖现有对话",
                                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                            )
                            ActionRow(
                                icon = { Icon(Icons.Rounded.RestartAlt, null, tint = scheme.tertiary, modifier = Modifier.size(18.dp)) },
                                title = "恢复内置模式",
                                subtitle = "自定义模式保留",
                                onClick = { vm.resetModes(); scope.launch { snackbar.showSnackbar("已恢复内置模式") } },
                            )
                            ActionRow(
                                icon = { Icon(Icons.Rounded.DeleteOutline, null, tint = scheme.error, modifier = Modifier.size(18.dp)) },
                                title = "清空全部对话",
                                subtitle = "不可恢复，请先导出备份",
                                titleColor = scheme.error,
                                onClick = { clearConfirm = true },
                            )
                        }
                    }

                    SectionLabel("关于")
                    GlassCard(Modifier.fillMaxWidth(), onClick = onAbout) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("DeepSeek Mobile", style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                                Text(
                                    "v1.0.0 · 第三方 API 客户端",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                            Text("查看", style = MaterialTheme.typography.labelMedium, color = palette.accent.primary)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }

            DsSnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 12.dp),
            )
        }
    }

    if (addModel) {
        AddModelDialog(
            existingIds = settings.models.map { it.id }.toSet(),
            onDismiss = { addModel = false },
            onAdd = { spec ->
                addModel = false
                vm.addModel(spec)
                scope.launch { snackbar.showSnackbar("已添加 ${spec.displayName}") }
            },
        )
    }

    if (clearConfirm) {
        AlertDialog(
            onDismissRequest = { clearConfirm = false },
            title = { Text("清空全部对话") },
            text = { Text("所有对话记录会被永久删除。建议先导出备份。") },
            confirmButton = {
                TextButton(onClick = {
                    clearConfirm = false
                    vm.resetAll()
                    scope.launch { snackbar.showSnackbar("已清空对话") }
                }) { Text("清空", color = scheme.error) }
            },
            dismissButton = { TextButton(onClick = { clearConfirm = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
)

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
            textStyle = MaterialTheme.typography.bodySmall,
            singleLine = true,
            visualTransformation = visualTransformation,
            trailingIcon = trailing,
            colors = fieldColors(),
        )
    }
}

@Composable
private fun ActionRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    titleColor: Color = Color.Unspecified,
    onClick: () -> Unit,
) {
    val resolvedTitle = if (titleColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else titleColor
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = resolvedTitle)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AddModelDialog(
    existingIds: Set<String>,
    onDismiss: () -> Unit,
    onAdd: (ModelSpec) -> Unit,
) {
    var id by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var vision by remember { mutableStateOf(false) }
    var reasoning by remember { mutableStateOf(false) }
    var context by remember { mutableStateOf("128000") }
    val duplicate = existingIds.contains(id.trim())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加模型") },
        text = {
            Column {
                OutlinedTextField(
                    value = id,
                    onValueChange = { id = it },
                    label = { Text("模型 ID") },
                    placeholder = { Text("例如 deepseek-chat") },
                    singleLine = true,
                    isError = duplicate,
                    supportingText = if (duplicate) ({ Text("这个 ID 已存在") }) else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("显示名称（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = context,
                    onValueChange = { context = it.filter { c -> c.isDigit() } },
                    label = { Text("上下文长度") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("支持图片", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    Switch(checked = vision, onCheckedChange = { vision = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("推理模型", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    Switch(checked = reasoning, onCheckedChange = { reasoning = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAdd(
                        ModelSpec(
                            id = id.trim(),
                            label = label.trim(),
                            vision = vision,
                            reasoning = reasoning,
                            contextWindow = context.toIntOrNull() ?: 128_000,
                        )
                    )
                },
                enabled = id.isNotBlank() && !duplicate,
            ) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
