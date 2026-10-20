package com.denge.app.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Forest = Color(0xFF264F3E)
val Sage = Color(0xFFDCE8D0)
val CanvasColor = Color(0xFFF5F6F3)
val Ink = Color(0xFF202D26)
val Muted = Color(0xFF626E66)
val Orange = Color(0xFFCB8653)
val Lilac = Color(0xFFAAA0C4)
val Line = Color(0xFFE5E9DF)
val Warm = Color(0xFFF1E9DC)

@Composable fun DengeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Forest, onPrimary = Color.White, primaryContainer = Sage, onPrimaryContainer = Forest,
        background = CanvasColor, onBackground = Ink, surface = Color.White, onSurface = Ink, surfaceVariant = Color(0xFFEEF1E8),
        onSurfaceVariant = Muted, outline = Color(0xFFB8C1B2), outlineVariant = Line, secondary = Orange, tertiary = Lilac),
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.3).sp),
            headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp),
            titleLarge = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
            titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
            bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
            labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
        ), content = content)
}
