package com.denge.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

fun number(n: Number): String = NumberFormat.getNumberInstance(Locale.forLanguageTag("tr-TR")).apply { maximumFractionDigits = 1 }.format(n)
@Composable fun Panel(modifier: Modifier = Modifier, color: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(color).border(1.dp, Line.copy(alpha = .65f), RoundedCornerShape(26.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
}
@Composable fun Section(title: String, subtitle: String? = null, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleLarge); subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Muted) } }
        action?.let { TextButton(onClick = onAction) { Text(it) } }
    }
}
@Composable fun Tag(text: String, color: Color = Forest, bg: Color = Sage) {
    Text(text, Modifier.clip(RoundedCornerShape(20.dp)).background(bg).padding(horizontal = 11.dp, vertical = 5.dp), color = color, style = MaterialTheme.typography.labelMedium)
}
@Composable fun IconTile(icon: ImageVector, color: Color = Forest, bg: Color = Sage, size: Int = 44) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(15.dp)).background(bg), contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(22.dp)) }
}
@Composable fun EmptyState(title: String, detail: String, icon: ImageVector = Icons.Outlined.Spa) {
    Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, Modifier.size(30.dp), tint = Forest.copy(alpha = .65f))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(detail, color = Muted, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
@Composable fun PrimaryButton(text: String, icon: ImageVector? = null, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().heightIn(min = 54.dp), enabled = enabled, shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)) {
        icon?.let { Icon(it, null, Modifier.size(19.dp)); Spacer(Modifier.width(9.dp)) }; Text(text)
    }
}
@Composable fun Macro(label: String, current: Double, goal: Int, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(6.dp).background(color, CircleShape)); Spacer(Modifier.width(5.dp)); Text(label, style = MaterialTheme.typography.bodySmall, color = Muted) }
        Text("${number(current)} g", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        LinearProgressIndicator(progress = { if (goal > 0) (current / goal).toFloat().coerceIn(0f, 1f) else 0f }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape), color = color, trackColor = color.copy(alpha = .15f), drawStopIndicator = {})
        Text("/ $goal g", style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}
@Composable fun CalorieRing(current: Int, goal: Int) {
    val fraction by androidx.compose.animation.core.animateFloatAsState((current.toFloat() / goal).coerceIn(0f, 1f), label = "energy")
    Box(Modifier.size(168.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            drawArc(Sage, -90f, 360f, false, style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
            if (fraction > 0) drawArc(Forest, -90f, 360f * fraction, false, style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(number(current), fontSize = 34.sp, letterSpacing = (-1.4).sp, fontWeight = FontWeight.Bold)
            Text("kcal alındı", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
@Composable fun SmallStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(value, style = MaterialTheme.typography.titleLarge); Text(label, color = Muted, style = MaterialTheme.typography.bodySmall) }
}
