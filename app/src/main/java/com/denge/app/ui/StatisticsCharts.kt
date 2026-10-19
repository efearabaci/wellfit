package com.denge.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denge.app.domain.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable internal fun ActivityCalendar(state: AppState, period: StatsPeriod, openDay: (String) -> Unit) {
    Panel {
        Section("Antrenman takvimin", "Bir güne dokun, o günün kaydını aç.")
        Row(Modifier.fillMaxWidth()) {
            listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz").forEach { Text(it, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Muted, style = MaterialTheme.typography.bodySmall) }
        }
        val offset = period.start.dayOfWeek.value - 1
        val cells: List<LocalDate?> = List(offset) { null } + period.dates
        cells.chunked(7).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(7) { index ->
                    val date = row.getOrNull(index)
                    if (date == null) Box(Modifier.weight(1f).height(49.dp)) else {
                        val mark = activityMark(state.days[date.toString()], date, period.today)
                        val label = when (mark) { ActivityMark.TRAINING -> "Antrenman"; ActivityMark.REST -> "Dinlenme"; ActivityMark.UNKNOWN -> "Durum bilinmiyor"; ActivityMark.FUTURE -> "Gelecek gün" }
                        val color = when (mark) { ActivityMark.TRAINING -> Forest; ActivityMark.REST -> Warm; ActivityMark.UNKNOWN -> Color(0xFFF1F3ED); ActivityMark.FUTURE -> Color.Transparent }
                        val textColor = when (mark) { ActivityMark.TRAINING -> Color.White; ActivityMark.FUTURE -> Muted.copy(alpha = .45f); else -> Ink }
                        Box(Modifier.weight(1f).height(49.dp).clip(RoundedCornerShape(12.dp)).background(color)
                            .then(if (date == period.today) Modifier.border(1.5.dp, Orange, RoundedCornerShape(12.dp)) else Modifier)
                            .clickable(enabled = mark != ActivityMark.FUTURE) { openDay(date.toString()) }
                            .semantics { contentDescription = "${date.format(DateTimeFormatter.ofPattern("d MMMM EEEE", Turkish))}, $label" }, contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(date.dayOfMonth.toString(), color = textColor, style = MaterialTheme.typography.labelLarge)
                                if (mark == ActivityMark.TRAINING || mark == ActivityMark.REST) Box(Modifier.size(4.dp).background(if (mark == ActivityMark.TRAINING) Sage else Orange, CircleShape))
                            }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag("Antrenman", Color.White, Forest)
            Tag("Dinlenme", Ink, Warm)
            Tag("Bilinmiyor", Muted, Color(0xFFF1F3ED))
        }
        Text("Dinlenme yalnızca sen işaretlediğinde görünür. Hevy'de antrenman bulunmaması, dinlenme yaptığın anlamına gelmez.", style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}

@Composable internal fun DailyMetricChart(state: AppState, period: StatsPeriod, title: String, unit: String, value: (Day) -> Double?, openDay: (String) -> Unit, color: Color = Forest) {
    var expanded by remember(period.start, title) { mutableStateOf(false) }
    val entries = period.elapsedDates.map { date -> date to state.days[date.toString()]?.let(value) }
    val known = entries.filter { it.second != null }
    Panel {
        Section(title, "$unit · ${known.size} gün için kayıt var")
        if (known.isEmpty()) EmptyState("Bu dönemde ölçüm yok", "Kayıt geldikçe grafik dolacak.", Icons.Outlined.BarChart)
        else {
            val maxValue = maxOf(1.0, known.maxOf { it.second!! })
            Row(Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.spacedBy(if (entries.size > 10) 3.dp else 9.dp), verticalAlignment = Alignment.Bottom) {
                entries.forEach { (date, current) ->
                    val closed = state.days[date.toString()]?.closed == true
                    val description = "${date.dayOfMonth} ${date.month.getDisplayName(java.time.format.TextStyle.FULL, Turkish)}: ${current?.let { "${number(it)} $unit" } ?: "Kayıt yok"}"
                    Box(Modifier.weight(1f).fillMaxHeight().clickable { openDay(date.toString()) }.semantics { contentDescription = description }, contentAlignment = Alignment.BottomCenter) {
                        if (current == null) Box(Modifier.size(3.dp).background(Line, CircleShape))
                        else Box(Modifier.fillMaxWidth().height(maxOf(3f, (current / maxValue * 130).toFloat()).dp).clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)).background(if (closed) color else color.copy(alpha = .45f)))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(period.start.format(DateTimeFormatter.ofPattern("d MMM", Turkish)), color = Muted, style = MaterialTheme.typography.bodySmall)
                Text(entries.last().first.format(DateTimeFormatter.ofPattern("d MMM", Turkish)), color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Text("Soluk çubuklar açık günlerdir; değerler değişebilir. Noktalar eksik kayıttır, sıfır değildir.", color = Muted, style = MaterialTheme.typography.bodySmall)
            TextButton({ expanded = !expanded }) { Text(if (expanded) "Günlük değerleri gizle" else "Gün gün değerleri göster") }
            if (expanded) entries.forEach { (date, current) ->
                Row(Modifier.fillMaxWidth().clickable { openDay(date.toString()) }.padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(date.format(DateTimeFormatter.ofPattern("d MMM EEE", Turkish)), style = MaterialTheme.typography.bodyMedium)
                    Text(current?.let { "${number(it)} $unit" } ?: "Kayıt yok", color = if (current == null) Muted else Forest, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable internal fun WeekdayChart(entries: List<WeekdayActivity>) {
    Panel {
        Section("Hangi günler spor yaptın?", "Takvim günleri içinde antrenman yapılan gün sayısı.")
        entries.forEach { entry ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(entry.weekday.getDisplayName(java.time.format.TextStyle.SHORT, Turkish), Modifier.width(32.dp), style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(progress = { if (entry.elapsedDays == 0) 0f else entry.trainingDays.toFloat() / entry.elapsedDays }, Modifier.weight(1f).height(8.dp), color = Forest, trackColor = Sage.copy(alpha = .5f), drawStopIndicator = {})
                Text("${entry.trainingDays} / ${entry.elapsedDays}", Modifier.width(40.dp), style = MaterialTheme.typography.labelMedium, textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }
        Text("Gelecek tarihler paydaya dahil değildir. Durumu bilinmeyen geçmiş tarihler için antrenman varsayılmaz.", color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable internal fun WeightTrend(stats: PeriodStatistics) {
    Panel {
        Section("Kilo eğrin", "Ölçülen günler arasında çizilir; ara günler tahmin edilmez.")
        val weights = stats.weightDays
        if (weights.isEmpty()) EmptyState("İlk tartı kaydınla başla", "Her gün tartılmak zorunda değilsin; girdiğin ölçümler burada kalır.", Icons.Outlined.MonitorWeight)
        else {
            val min = weights.minOf { it.weight!! } - .5
            val max = weights.maxOf { it.weight!! } + .5
            Canvas(Modifier.fillMaxWidth().height(160.dp).padding(8.dp).semantics { contentDescription = "${weights.size} tartı kaydı. İlk ölçüm ${number(weights.first().weight!!)} kg, son ölçüm ${number(weights.last().weight!!)} kg." }) {
                val span = maxOf(1L, ChronoUnit.DAYS.between(stats.period.start, minOf(stats.period.end, stats.period.today)))
                val points = weights.map { day -> Offset(
                    ChronoUnit.DAYS.between(stats.period.start, LocalDate.parse(day.date)).toFloat() / span * size.width,
                    (size.height * (1 - (day.weight!! - min) / (max - min))).toFloat()) }
                val path = Path().apply { points.forEachIndexed { index, point -> if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y) } }
                drawPath(path, Forest, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                points.forEach { drawCircle(Forest, 4.dp.toPx(), it) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(LocalDate.parse(weights.first().date).format(DateTimeFormatter.ofPattern("d MMM", Turkish)), color = Muted, style = MaterialTheme.typography.bodySmall)
                Text(LocalDate.parse(weights.last().date).format(DateTimeFormatter.ofPattern("d MMM", Turkish)), color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
