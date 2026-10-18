package com.lhy.deepseekmobile.ui.theme

import androidx.compose.ui.graphics.Color

/* Brand + neutral tokens. Kept in one place so the whole app can be re-tinted. */

val DeepBlue = Color(0xFF5B7CFA)
val DeepViolet = Color(0xFF9B7BFF)
val DeepMint = Color(0xFF2FD6BE)

val InkBg = Color(0xFF0A0C12)
val InkSurface = Color(0xFF10131C)
val InkSurfaceHigh = Color(0xFF161B26)
val InkSurfaceVariant = Color(0xFF1C2230)
val InkOutline = Color(0xFF2B3345)
val InkText = Color(0xFFE9ECF4)
val InkTextDim = Color(0xFF98A2B6)

val LightBg = Color(0xFFEEF1F8)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE9EDF6)
val LightOutline = Color(0xFFCFD6E6)
val LightText = Color(0xFF0E1118)
val LightTextDim = Color(0xFF4C5568)

val DangerRed = Color(0xFFFF6470)
val SuccessGreen = Color(0xFF3DD68C)
val WarnAmber = Color(0xFFFFB454)

/**
 * Accent presets. `primary` is tuned for dark surfaces, `primaryLight` for light ones;
 * containers are derived at theme-build time so every accent stays coherent.
 */
enum class Accent(
    val id: String,
    val label: String,
    val primary: Color,
    val primaryLight: Color,
    val secondary: Color,
    val secondaryLight: Color,
    val tertiary: Color,
    val tertiaryLight: Color,
) {
    Blue("blue", "星蓝", Color(0xFF6B8AFF), Color(0xFF3D5AE0), Color(0xFFA98BFF), Color(0xFF6B54D8), Color(0xFF3FD9C4), Color(0xFF0E9E8E)),
    Violet("violet", "暮紫", Color(0xFFA98BFF), Color(0xFF6B4FD8), Color(0xFFFF8FC7), Color(0xFFC23E85), Color(0xFF64C8FF), Color(0xFF1F7FBF)),
    Mint("mint", "青柠", Color(0xFF43D8BE), Color(0xFF0F8F7C), Color(0xFF7FD0FF), Color(0xFF2F7BC4), Color(0xFFC0E96B), Color(0xFF6C8A19)),
    Amber("amber", "琥珀", Color(0xFFFFB454), Color(0xFFB26A00), Color(0xFFFF8A6B), Color(0xFFC2411F), Color(0xFF8FD8A8), Color(0xFF2E7D54)),
    Rose("rose", "绯樱", Color(0xFFFF8FB2), Color(0xFFC23E6B), Color(0xFFB9A6FF), Color(0xFF6A55D6), Color(0xFFFFC978), Color(0xFFB07A16));

    companion object {
        fun fromId(id: String?): Accent = entries.firstOrNull { it.id == id } ?: Blue
    }
}
