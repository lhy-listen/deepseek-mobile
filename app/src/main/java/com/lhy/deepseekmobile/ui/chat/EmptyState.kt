package com.lhy.deepseekmobile.ui.chat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lhy.deepseekmobile.data.Mode
import com.lhy.deepseekmobile.data.ModelSpec
import com.lhy.deepseekmobile.ui.components.GlassCard
import com.lhy.deepseekmobile.ui.components.TinyTag
import com.lhy.deepseekmobile.ui.components.greetingFor
import com.lhy.deepseekmobile.ui.theme.LocalDsPalette
import java.util.Calendar

@Composable
fun EmptyChatState(
    mode: Mode,
    model: ModelSpec,
    configured: Boolean,
    onPrompt: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val palette = LocalDsPalette.current
    val scheme = MaterialTheme.colorScheme
    var appeared by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(500),
        label = "emptyFade",
    )
    LaunchedEffect(Unit) { appeared = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 12.dp)
            .alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(38.dp))
        Box(
            Modifier
                .size(66.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(palette.bubbleTop, palette.bubbleBottom))),
            contentAlignment = Alignment.Center,
        ) {
            Text("✦", color = androidx.compose.ui.graphics.Color.White, fontSize = 28.sp)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = "${greetingFor(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))}，我在。",
            style = MaterialTheme.typography.headlineMedium,
            color = scheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "API 直连 · 不降智 · 图片 / 文件 / 文件夹都能读",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (!configured) {
            Spacer(Modifier.height(18.dp))
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                onClick = onOpenSettings,
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Key, null, tint = scheme.primary, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("先填入 API Key", style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                        Text(
                            "填入后即可选择模型开始对话",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.Rounded.ArrowForward, null, tint = scheme.primary, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TinyTag(mode.emoji + " " + mode.name)
            TinyTag(model.displayName, color = scheme.tertiary)
            if (model.vision) TinyTag("视觉", color = scheme.secondary)
        }

        Spacer(Modifier.height(22.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            mode.quickPrompts.take(3).forEach { prompt ->
                PromptCard(prompt) { onPrompt(prompt) }
            }
        }

        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CapabilityTile(Icons.Rounded.Image, "图片", "OCR / 视觉", Modifier.weight(1f))
            CapabilityTile(Icons.Rounded.FolderOpen, "文件夹", "整目录投喂", Modifier.weight(1f))
            CapabilityTile(Icons.Rounded.AutoAwesome, "模式", "8 种玩法", Modifier.weight(1f))
        }

        Spacer(Modifier.height(22.dp))
        Text(
            text = "可以只发一个文件夹，让我先读；也可以贴截图、代码或长文。",
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun CapabilityTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(
            Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.height(7.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PromptCard(prompt: String, onClick: () -> Unit) {
    GlassCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = prompt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Rounded.ArrowForward,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(15.dp),
            )
        }
    }
}
