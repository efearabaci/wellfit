package com.denge.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.denge.app.domain.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable internal fun ActivityCalendar(state: AppState, period: StatsPeriod, openDay: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz").forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, color = ProgressBlueInk, style = MaterialTheme.typography.labelSmall)
            }
        }
        val offset = period.start.dayOfWeek.value - 1
        val cells: List<LocalDate?> = List(offset) { null } + period.dates
        cells.chunked(7).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { index ->
                    val date = row.getOrNull(index)
                    if (date == null) Spacer(Modifier.weight(1f).height(48.dp)) else {
                        val mark = activityMark(state.days[date.toString()], date, period.today)
                        val training = mark == ActivityMark.TRAINING
                        val rest = mark == ActivityMark.REST
                        val future = mark == ActivityMark.FUTURE
                        val label = when (mark) { ActivityMark.TRAINING -> "Antrenman"; ActivityMark.REST -> "Dinlenme"; ActivityMark.UNKNOWN -> "Kayıt yok"; ActivityMark.FUTURE -> "Gelecek gün" }
                        Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(10.dp))
                            .background(if (training) ProgressBlueInk else if (rest) Color.White else Color.White.copy(alpha = if (future) .12f else .35f))
                            .then(if (date == period.today || rest) Modifier.border(if (date == period.today) 1.5.dp else 1.dp, ProgressBlueInk.copy(alpha = if (date == period.today) 1f else .35f), RoundedCornerShape(10.dp)) else Modifier)
                            .clickable(enabled = !future) { openDay(date.toString()) }
                            .semantics { contentDescription = "${date.format(DateTimeFormatter.ofPattern("d MMMM EEEE", Turkish))}, $label" }, contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(date.dayOfMonth.toString(), color = if (training) Color.White else ProgressBlueInk.copy(alpha = if (future) .35f else 1f), style = MaterialTheme.typography.labelLarge)
                                if (training) Box(Modifier.width(12.dp).height(3.dp).background(Color.White.copy(alpha = .6f), CircleShape))
                                else if (rest) Box(Modifier.size(4.dp).border(1.dp, ProgressBlueInk, CircleShape))
                            }
                        }
                    }
                }
            }
        }
        FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CalendarKey("Antrenman", ProgressBlueInk)
            CalendarKey("Dinlenme", Color.White, outlined = true)
            CalendarKey("Kayıt yok", Color.White.copy(alpha = .4f))
        }
    }
}

@Composable private fun CalendarKey(label: String, color: Color, outlined: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)).then(if (outlined) Modifier.border(1.dp, ProgressBlueInk.copy(alpha = .4f), RoundedCornerShape(2.dp)) else Modifier))
        Text(label, style = MaterialTheme.typography.labelSmall, color = ProgressBlueInk)
    }
}

/** Daily values retain real zeros and gaps. Bars select a date; the selection opens its journal. */
@Composable internal fun DailyMetricChart(state: AppState, period: StatsPeriod, unit: String, value: (Day) -> Double?, openDay: (String) -> Unit, color: Color) {
    val entries = period.dates.map { date -> date to if (date <= period.today) state.days[date.toString()]?.let(value) else null }
    val known = entries.filter { it.second != null }
    if (known.isEmpty()) return
    if (known.size == 1) {
        val entry = known.single()
        ProgressRecord(entry.first.format(DateTimeFormatter.ofPattern("d MMM EEE", Turkish)), null, "${number(entry.second!!)} $unit", color) { openDay(entry.first.toString()) }
        return
    }
    var selectedKey by rememberSaveable(period.start.toString(), period.mode, unit) { mutableStateOf(known.last().first.toString()) }
    val selected = entries.firstOrNull { it.first.toString() == selectedKey } ?: known.last()
    val selectedIndex = entries.indexOf(selected)
    val axisWidth = 40.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
    val maxValue = maxOf(1.0, known.maxOf { it.second!! })
    val maxY = when {
        maxValue >= 10000 -> kotlin.math.ceil(maxValue / 5000) * 5000
        maxValue >= 1000 -> kotlin.math.ceil(maxValue / 1000) * 1000
        else -> kotlin.math.ceil(maxValue / 100) * 100
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().height(144.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChartAxis(maxY, color, width = axisWidth)
            Canvas(Modifier.weight(1f).fillMaxHeight()
                .pointerInput(period, entries, unit) {
                    detectTapGestures { tap ->
                        val index = (tap.x / size.width * entries.size).toInt().coerceIn(0, entries.lastIndex)
                        if (entries[index].first <= period.today) selectedKey = entries[index].first.toString()
                    }
                }
                .semantics {
                    contentDescription = "Günlük $unit grafiği. " + entries.filter { it.first <= period.today }.joinToString("; ") { "${progressDate(it.first.toString())}: ${it.second?.let { n -> "${number(n)} $unit" } ?: "Kayıt yok"}" }
                    customActions = buildList {
                        if (selectedIndex > 0) add(CustomAccessibilityAction("Önceki gün") { selectedKey = entries[selectedIndex - 1].first.toString(); true })
                        if (selectedIndex < entries.lastIndex && entries[selectedIndex + 1].first <= period.today) add(CustomAccessibilityAction("Sonraki gün") { selectedKey = entries[selectedIndex + 1].first.toString(); true })
                    }
                }) {
                val chartHeight = size.height - 8.dp.toPx()
                val cell = size.width / entries.size
                listOf(0f, .5f, 1f).forEach { fraction ->
                    val y = 4.dp.toPx() + chartHeight * fraction
                    drawLine(color.copy(alpha = .12f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
                entries.forEachIndexed { index, (date, current) ->
                    val x = cell * (index + .5f)
                    val bottom = size.height - 4.dp.toPx()
                    val width = (cell * .65f).coerceAtMost(26.dp.toPx())
                    if (index == selectedIndex) drawRoundRect(color.copy(alpha = .08f), Offset(cell * index + 1.dp.toPx(), 0f), Size((cell - 2.dp.toPx()).coerceAtLeast(1f), size.height), CornerRadius(4.dp.toPx()))
                    when {
                        current == null && date <= period.today -> drawCircle(color.copy(alpha = .35f), 2.dp.toPx(), Offset(x, bottom), style = Stroke(1.dp.toPx()))
                        current == 0.0 -> drawLine(color, Offset(x - width / 2, bottom), Offset(x + width / 2, bottom), 2.dp.toPx(), StrokeCap.Round)
                        current != null -> {
                            val height = (current / maxY * chartHeight).toFloat().coerceAtLeast(2.dp.toPx())
                            drawRoundRect(if (index == selectedIndex) color else color.copy(alpha = .4f), Offset(x - width / 2, bottom - height), Size(width, height), CornerRadius(3.dp.toPx()))
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = axisWidth + 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            if (period.mode == PeriodMode.WEEK) period.dates.forEach { date -> Text(date.dayOfMonth.toString(), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = color.copy(alpha = if (date <= period.today) 1f else .4f)) }
            else {
                Text(progressDate(period.start.toString()), color = color, style = MaterialTheme.typography.labelSmall)
                Text(progressDate(period.end.toString()), color = color, style = MaterialTheme.typography.labelSmall)
            }
        }
        ProgressRecord(selected.first.format(DateTimeFormatter.ofPattern("d MMM EEE", Turkish)), null, selected.second?.let { "${number(it)} $unit" } ?: "Kayıt yok", color) { openDay(selected.first.toString()) }
    }
}

@Composable private fun ChartAxis(maxY: Double, color: Color, minY: Double = 0.0, width: Dp) {
    Column(Modifier.width(width).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
        listOf(maxY, (maxY + minY) / 2, minY).forEach { Text(number(it), style = MaterialTheme.typography.labelSmall, color = color) }
    }
}

/** Points are placed by elapsed calendar time; missing weigh-ins never become fabricated values. */
@Composable internal fun WeightTrend(stats: PeriodStatistics, openDay: (String) -> Unit) {
    val weights = stats.weightDays
    if (weights.isEmpty()) return
    if (weights.size == 1) {
        val day = weights.single()
        ProgressRecord(LocalDate.parse(day.date).format(DateTimeFormatter.ofPattern("d MMM EEE", Turkish)), null, null, ProgressVioletInk) { openDay(day.date) }
        return
    }
    var selectedDate by rememberSaveable(stats.period.start.toString(), stats.period.mode) { mutableStateOf(weights.last().date) }
    val selected = weights.firstOrNull { it.date == selectedDate } ?: weights.last()
    val selectedIndex = weights.indexOf(selected)
    val axisWidth = 40.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
    val min = kotlin.math.floor((weights.minOf { it.weight!! } - .3) * 2) / 2
    val max = min + kotlin.math.ceil(weights.maxOf { it.weight!! } + .3 - min)
    val span = maxOf(1L, ChronoUnit.DAYS.between(stats.period.start, minOf(stats.period.end, stats.period.today)))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().height(144.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChartAxis(max, ProgressVioletInk, min, axisWidth)
            Canvas(Modifier.weight(1f).fillMaxHeight()
                .pointerInput(weights, stats.period) {
                    detectTapGestures { tap ->
                        selectedDate = weights.minBy { day ->
                            val x = ChronoUnit.DAYS.between(stats.period.start, LocalDate.parse(day.date)).toFloat() / span * (size.width - 12.dp.toPx()) + 6.dp.toPx()
                            kotlin.math.abs(x - tap.x)
                        }.date
                    }
                }
                .semantics {
                    contentDescription = "Kilo grafiği. " + weights.joinToString("; ") { "${progressDate(it.date)}: ${number(it.weight!!)} kg" }
                    customActions = buildList {
                        if (selectedIndex > 0) add(CustomAccessibilityAction("Önceki ölçüm") { selectedDate = weights[selectedIndex - 1].date; true })
                        if (selectedIndex < weights.lastIndex) add(CustomAccessibilityAction("Sonraki ölçüm") { selectedDate = weights[selectedIndex + 1].date; true })
                    }
                }) {
                val height = size.height - 12.dp.toPx()
                listOf(0f, .5f, 1f).forEach { fraction ->
                    val y = 6.dp.toPx() + height * fraction
                    drawLine(ProgressVioletInk.copy(alpha = .12f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
                val points = weights.map { day -> Offset(
                    ChronoUnit.DAYS.between(stats.period.start, LocalDate.parse(day.date)).toFloat() / span * (size.width - 12.dp.toPx()) + 6.dp.toPx(),
                    (height * (1 - (day.weight!! - min) / (max - min))).toFloat() + 6.dp.toPx()) }
                if (points.size > 1) {
                    val area = Path().apply {
                        moveTo(points.first().x, size.height)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, size.height); close()
                    }
                    drawPath(area, ProgressVioletInk.copy(alpha = .08f))
                    val line = Path().apply { points.forEachIndexed { index, point -> if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y) } }
                    drawPath(line, ProgressVioletInk, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
                }
                points.forEachIndexed { index, point ->
                    if (index == selectedIndex) drawCircle(ProgressVioletInk.copy(alpha = .15f), 8.dp.toPx(), point)
                    drawCircle(Color.White, 4.dp.toPx(), point)
                    drawCircle(ProgressVioletInk, 3.dp.toPx(), point)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = axisWidth + 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(progressDate(stats.period.start.toString()), color = ProgressVioletInk, style = MaterialTheme.typography.labelSmall)
            Text(progressDate(minOf(stats.period.end, stats.period.today).toString()), color = ProgressVioletInk, style = MaterialTheme.typography.labelSmall)
        }
        ProgressRecord(progressDate(selected.date), null, "${number(selected.weight!!)} kg", ProgressVioletInk) { openDay(selected.date) }
    }
}
