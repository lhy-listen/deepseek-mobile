package com.lhy.deepseekmobile.ui.about

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lhy.deepseekmobile.BuildConfig
import com.lhy.deepseekmobile.ui.components.DsBackground
import com.lhy.deepseekmobile.ui.components.GlassCard
import com.lhy.deepseekmobile.ui.theme.LocalDsPalette

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val palette = LocalDsPalette.current
    val scheme = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current

    DsBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, "返回", tint = scheme.onSurfaceVariant)
                }
                Text("关于", style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(palette.bubbleTop, palette.bubbleBottom))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✦", color = Color.White, fontSize = 30.sp)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "DeepSeek Mobile",
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onBackground,
                )
                Text(
                    "版本 ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )

                Spacer(Modifier.height(22.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "这是一个第三方 DeepSeek / OpenAI 兼容 API 客户端：不内置任何系统提示词过滤，" +
                                "你看到的就是模型原样的输出。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface,
                        )
                        Text(
                            "对话、模式与 API Key 都保存在应用私有目录；除了你配置的接口地址，应用不会把数据发往别处。",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                GlassCard(
                    Modifier.fillMaxWidth(),
                    onClick = {
                        runCatching { uriHandler.openUri("https://github.com/lhy-listen/deepseek-mobile") }
                    },
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("项目主页", style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                            Text(
                                "github.com/lhy-listen/deepseek-mobile",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        Icon(Icons.Rounded.OpenInNew, null, tint = scheme.primary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("技术栈", style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Kotlin · Jetpack Compose · Material 3 · OkHttp · kotlinx.serialization · ML Kit（离线 OCR）",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("免责声明", style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "本应用与 DeepSeek 官方无隶属关系；API 使用产生的费用与合规责任由使用者承担。",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}
