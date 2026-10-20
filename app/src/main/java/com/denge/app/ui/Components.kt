package com.denge.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

fun number(n: Number): String = NumberFormat.getNumberInstance(Locale.forLanguageTag("tr-TR")).apply { maximumFractionDigits = 1 }.format(n)
fun macroNumber(n: Double?): String = n?.let { number(it) } ?: "—"
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
@Composable fun EmptyState(title: String, detail: String, icon: ImageVector = WellfitIcons.Brand) {
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
/**
 * Öğün editöründeki eşit yükseklikte makro girişi. Boş değer "—" olarak görünür; böylece
 * girilmemiş makro sıfır gibi okunmaz.
 */
@Composable fun MacroInput(value: String, onChange: (String) -> Unit, label: String, accent: Color, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(accent.copy(alpha = .09f)).border(1.dp, accent.copy(alpha = .22f), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.size(6.dp).background(accent, CircleShape))
            Text(label, style = MaterialTheme.typography.labelMedium, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                if (value.isBlank() && !focused) Text("—", style = MaterialTheme.typography.titleMedium, color = Muted.copy(alpha = .55f))
                BasicTextField(
                    value = value,
                    onValueChange = { if (it.length <= 7) onChange(it) },
                    modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.semantics { contentDescription = "$label · g" },
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = Ink),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    cursorBrush = SolidColor(accent)
                )
            }
            Text("g", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}
@Composable fun SmallStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(value, style = MaterialTheme.typography.titleLarge); Text(label, color = Muted, style = MaterialTheme.typography.bodySmall) }
}
