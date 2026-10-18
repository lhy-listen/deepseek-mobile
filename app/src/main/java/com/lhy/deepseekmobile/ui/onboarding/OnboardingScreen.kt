package com.lhy.deepseekmobile.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lhy.deepseekmobile.ui.AppViewModel
import com.lhy.deepseekmobile.ui.components.DsBackground
import com.lhy.deepseekmobile.ui.components.EmojiTile
import com.lhy.deepseekmobile.ui.theme.LocalDsPalette

@Composable
fun OnboardingScreen(vm: AppViewModel, onDone: () -> Unit) {
    val palette = LocalDsPalette.current
    val scheme = MaterialTheme.colorScheme
    var apiKey by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("https://api.deepseek.com") }
    var advanced by remember { mutableStateOf(false) }
    var showKey by remember { mutableStateOf(false) }

    fun finish(withKey: Boolean) {
        vm.updateSettings { s ->
            s.copy(
                apiKey = if (withKey) apiKey.trim() else s.apiKey,
                baseUrl = baseUrl.trim().ifBlank { "https://api.deepseek.com" },
                onboardingDone = true,
            )
        }
        onDone()
    }

    DsBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(56.dp))
            Box(
                Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(palette.bubbleTop, palette.bubbleBottom))),
                contentAlignment = Alignment.Center,
            ) {
                Text("✦", color = Color.White, fontSize = 32.sp)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "DeepSeek",
                style = MaterialTheme.typography.displaySmall,
                color = scheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "API 版 · 不降智的聊天客户端",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(Modifier.height(26.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureRow(Icons.Rounded.Bolt, "直连模型 API", "没有中间层改写你的问题，也没有额外的安全提示词")
                FeatureRow(Icons.Rounded.FolderOpen, "图片、文件、文件夹", "截图自动 OCR；整个代码目录可以一次投喂")
                FeatureRow(Icons.Rounded.Layers, "可自定义的模式", "提示词、模型、温度、快捷语，存成你自己的模式")
            }

            Spacer(Modifier.height(30.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API Key") },
                placeholder = { Text("sk-…") },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            null,
                            tint = scheme.onSurfaceVariant,
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = scheme.primary.copy(alpha = 0.7f),
                    unfocusedBorderColor = scheme.outlineVariant,
                ),
            )
            Text(
                "Key 只保存在这台手机上，请求直接发往你填写的接口。",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { advanced = !advanced }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "高级：自定义接口地址",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(visible = advanced) {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("接口地址") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = scheme.primary.copy(alpha = 0.7f),
                        unfocusedBorderColor = scheme.outlineVariant,
                    ),
                )
            }

            Spacer(Modifier.height(26.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (apiKey.isNotBlank()) {
                            Brush.linearGradient(listOf(palette.bubbleTop, palette.bubbleBottom))
                        } else {
                            Brush.linearGradient(listOf(scheme.surfaceContainerHigh, scheme.surfaceContainerHigh))
                        }
                    )
                    .clickable(enabled = apiKey.isNotBlank()) { finish(true) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "开始使用",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (apiKey.isNotBlank()) Color.White else scheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "先看看",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { finish(false) }
                    .padding(8.dp),
            )

            Spacer(Modifier.height(24.dp))
            Text(
                "第三方客户端，与 DeepSeek 官方无关。\n你的对话记录只存在本机。",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
