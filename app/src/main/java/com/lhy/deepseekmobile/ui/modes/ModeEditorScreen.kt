package com.lhy.deepseekmobile.ui.modes

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lhy.deepseekmobile.data.Mode
import com.lhy.deepseekmobile.data.newId
import com.lhy.deepseekmobile.ui.AppViewModel
import com.lhy.deepseekmobile.ui.components.DsBackground
import com.lhy.deepseekmobile.ui.components.EmojiTile
import com.lhy.deepseekmobile.ui.components.GlassCard
import com.lhy.deepseekmobile.ui.components.SectionLabel
import com.lhy.deepseekmobile.ui.theme.LocalDsPalette
import kotlin.math.roundToInt
private val EMOJI_CHOICES = listOf(
    "✨", "🧠", "💻", "✍️", "🌍", "📚", "🖼️", "🎲",
    "🔬", "🧩", "🎯", "⚡️", "🪄", "🧭", "📝", "🗂️",
    "🧪", "🎨", "📊", "🕵️", "🧘", "🚀", "🫧", "🔮",
)

@Composable
fun ModeEditorScreen(
    vm: AppViewModel,
    modeId: String?,
    onBack: () -> Unit,
) {
    val modes by vm.modes.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val existing = remember(modeId, modes) { modes.firstOrNull { it.id == modeId } }
    val scheme = MaterialTheme.colorScheme
    val palette = LocalDsPalette.current

    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var emoji by remember(existing?.id) { mutableStateOf(existing?.emoji ?: "✨") }
    var tagline by remember(existing?.id) { mutableStateOf(existing?.tagline ?: "") }
    var prompt by remember(existing?.id) { mutableStateOf(existing?.systemPrompt ?: "") }
    var modelOverride by remember(existing?.id) { mutableStateOf(existing?.modelId) }
    var temperature by remember(existing?.id) { mutableStateOf((existing?.temperature ?: 0.7).toFloat()) }
    var maxTokens by remember(existing?.id) { mutableStateOf(existing?.maxTokens) }
    var showReasoning by remember(existing?.id) { mutableStateOf(existing?.showReasoning ?: true) }
    var quickPrompts by remember(existing?.id) { mutableStateOf(existing?.quickPrompts ?: emptyList()) }
    var newPrompt by remember { mutableStateOf("") }
    var deleteConfirm by remember { mutableStateOf(false) }

    fun save() {
        if (name.isBlank()) return
        val draft = Mode(
            id = existing?.id ?: newId(),
            name = name.trim(),
            emoji = emoji,
            tagline = tagline.trim(),
            systemPrompt = prompt.trim(),
            modelId = modelOverride,
            temperature = temperature.toDouble(),
            topP = existing?.topP,
            maxTokens = maxTokens,
            showReasoning = showReasoning,
            quickPrompts = quickPrompts,
            builtIn = existing?.builtIn ?: false,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        )
        vm.saveMode(draft)
        onBack()
    }

    DsBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, "返回", tint = scheme.onSurfaceVariant)
                }
                Text(
                    if (existing == null) "新建模式" else "编辑模式",
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (existing != null && !existing.builtIn) {
                    IconButton(onClick = { deleteConfirm = true }) {
                        Icon(Icons.Rounded.DeleteOutline, "删除", tint = scheme.onSurfaceVariant)
                    }
                }
                TextButton(onClick = { save() }, enabled = name.isNotBlank()) {
                    Text("保存", color = if (name.isNotBlank()) scheme.primary else scheme.onSurfaceVariant)
                }
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 40.dp)
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        EmojiTile(emoji, size = 46.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                name.ifBlank { "未命名模式" },
                                style = MaterialTheme.typography.titleMedium,
                                color = scheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                tagline.ifBlank { "一句话说明这个模式的用途" },
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                SectionLabel("身份")
                EditorField(
                    label = "名称",
                    value = name,
                    onChange = { name = it },
                    placeholder = "例如：产品经理、论文审稿人",
                )
                Spacer(Modifier.height(10.dp))
                EditorField(
                    label = "一句话说明",
                    value = tagline,
                    onChange = { tagline = it },
                    placeholder = "显示在模式卡片上",
                )

                Spacer(Modifier.height(12.dp))
                Text("图标", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    EMOJI_CHOICES.forEach { choice ->
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (choice == emoji) scheme.primary.copy(alpha = 0.18f)
                                    else scheme.surfaceContainerHigh.copy(alpha = 0.6f)
                                )
                                .clickable { emoji = choice },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(choice, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                SectionLabel("系统提示词")
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    placeholder = { Text("你希望它扮演什么角色、遵守什么规则、如何输出……") },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = scheme.primary.copy(alpha = 0.6f),
                        unfocusedBorderColor = scheme.outlineVariant,
                    ),
                )
                Text(
                    "提示词会作为 system 消息原样发送，不做任何过滤或改写。",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )

                Spacer(Modifier.height(18.dp))
                SectionLabel("模型与参数")
                Text("模型", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = modelOverride == null,
                        onClick = { modelOverride = null },
                        label = { Text("跟随全局") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = scheme.primary.copy(alpha = 0.16f),
                        ),
                    )
                    settings.models.take(2).forEach { model ->
                        FilterChip(
                            selected = modelOverride == model.id,
                            onClick = { modelOverride = model.id },
                            label = { Text(model.displayName, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = scheme.primary.copy(alpha = 0.16f),
                            ),
                        )
                    }
                }
                Text(
                    "可选模型在「设置 → 模型」里维护。",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )

                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("温度", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Text(
                        String.format("%.2f", temperature),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.primary,
                    )
                }
                Slider(
                    value = temperature,
                    onValueChange = { temperature = it },
                    valueRange = 0f..1.5f,
                    steps = 29,
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = scheme.primary,
                        activeTrackColor = scheme.primary.copy(alpha = 0.7f),
                    ),
                )
                Text(
                    when {
                        temperature <= 0.35f -> "更稳定、更保守"
                        temperature >= 1.0f -> "更发散、更意外"
                        else -> "平衡"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(14.dp))
                Text("最大输出", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf<Int?>(null, 4096, 8192, 16384).forEach { tokens ->
                        FilterChip(
                            selected = maxTokens == tokens,
                            onClick = { maxTokens = tokens },
                            label = { Text(if (tokens == null) "默认" else "${tokens / 1024}k") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = scheme.primary.copy(alpha = 0.16f),
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("显示思考过程", style = MaterialTheme.typography.labelMedium, color = scheme.onSurface)
                        Text(
                            "推理模型返回的 reasoning 内容会折叠展示",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = showReasoning, onCheckedChange = { showReasoning = it })
                }

                Spacer(Modifier.height(18.dp))
                SectionLabel("快捷提示")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    quickPrompts.forEachIndexed { index, item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(scheme.surfaceContainerHigh.copy(alpha = 0.55f))
                                .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                item,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { quickPrompts = quickPrompts.filterIndexed { i, _ -> i != index } }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Rounded.Close, "删除", tint = scheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newPrompt,
                        onValueChange = { newPrompt = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("添加一条开场提示") },
                        textStyle = MaterialTheme.typography.bodySmall,
                        singleLine = true,
                    )
                    IconButton(
                        onClick = {
                            if (newPrompt.isNotBlank()) {
                                quickPrompts = quickPrompts + newPrompt.trim()
                                newPrompt = ""
                            }
                        },
                        enabled = newPrompt.isNotBlank(),
                    ) {
                        Icon(Icons.Rounded.Add, "添加", tint = if (newPrompt.isNotBlank()) scheme.primary else scheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Check, null, tint = palette.accent.primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "保存后可在对话底部随时切换",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (deleteConfirm) {
        AlertDialog(
            onDismissRequest = { deleteConfirm = false },
            title = { Text("删除模式") },
            text = { Text("「${existing?.name}」将被删除，历史对话不受影响。") },
            confirmButton = {
                TextButton(onClick = {
                    deleteConfirm = false
                    existing?.let { vm.deleteMode(it.id) }
                    onBack()
                }) { Text("删除", color = scheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun EditorField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    singleLine: Boolean = true,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = singleLine,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
    }
}
