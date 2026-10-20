package com.denge.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denge.app.domain.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal val ProgressBlue = Color(0xFFE5EBF0)
internal val ProgressBlueInk = Color(0xFF3E586F)
internal val ProgressGreen = Color(0xFFE5ECE6)
internal val ProgressSand = Color(0xFFF1EBE1)
internal val ProgressSandInk = Color(0xFF85603E)
internal val ProgressViolet = Color(0xFFEBE7F0)
internal val ProgressVioletInk = Color(0xFF72608F)
internal fun progressDate(date: String) = LocalDate.parse(date).format(DateTimeFormatter.ofPattern("d MMM", Turkish))
private fun progressValue(value: Double, unit: String) = if (unit == "kg") java.text.NumberFormat.getNumberInstance(Turkish).apply { maximumFractionDigits = 2 }.format(value) else number(value)
internal fun signedNumber(value: Double, unit: String) = (if (value > 0) "+" else if (value < 0) "−" else "") + progressValue(kotlin.math.abs(value), unit)
internal fun periodTitle(period: StatsPeriod): String = when {
    period.mode == PeriodMode.MONTH -> period.start.format(DateTimeFormatter.ofPattern("MMMM yyyy", Turkish))
    period.start.year != period.end.year -> "${period.start.format(DateTimeFormatter.ofPattern("d MMM yy", Turkish))} – ${period.end.format(DateTimeFormatter.ofPattern("d MMM yy", Turkish))}"
    period.start.month == period.end.month -> "${period.start.dayOfMonth} – ${period.end.format(DateTimeFormatter.ofPattern(if (period.start.year == period.today.year) "d MMMM" else "d MMM yy", Turkish))}"
    else -> "${progressDate(period.start.toString())} – ${period.end.format(DateTimeFormatter.ofPattern(if (period.start.year == period.today.year) "d MMM" else "d MMM yy", Turkish))}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ProgressScreen(state: AppState, openDay: (String) -> Unit) {
    var modeName by rememberSaveable { mutableStateOf(PeriodMode.WEEK.name) }
    var anchor by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var periodPicker by rememberSaveable { mutableStateOf(false) }
    val today = LocalDate.now()
    val period = remember(modeName, anchor, today) { StatsPeriod(PeriodMode.valueOf(modeName), LocalDate.parse(anchor), today) }
    val stats = remember(state, period) { PeriodStatistics(state, period) }
    val previous = remember(state, period) { PeriodStatistics(state, period.shift(-1)) }
    val comparisons = buildList {
        if (stats.stepsAverage != null && previous.stepsAverage != null) add(ProgressComparison("Günlük adım", "adım", stats.stepsAverage, previous.stepsAverage, stats.stepDays.size, previous.stepDays.size))
        if (stats.caloriesAverage != null && previous.caloriesAverage != null) add(ProgressComparison("Günlük kalori", "kcal", stats.caloriesAverage, previous.caloriesAverage, stats.closedDays.size, previous.closedDays.size))
        if (stats.averageWeight != null && previous.averageWeight != null) add(ProgressComparison("Ortalama kilo", "kg", stats.averageWeight, previous.averageWeight, stats.weightDays.size, previous.weightDays.size, "ölçüm"))
    }
    LazyColumn(Modifier.fillMaxSize().testTag("statistics-list"), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item(key = "period") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Gidişat", Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.headlineLarge)
                    Row(Modifier.clip(RoundedCornerShape(12.dp)).background(ProgressGreen).clickable { periodPicker = true }.testTag("period-picker").heightIn(min = 48.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(WellfitIcons.Period, null, Modifier.size(19.dp), tint = Forest)
                        Text(if (period.mode == PeriodMode.WEEK) "Hafta" else "Ay", style = MaterialTheme.typography.labelLarge, color = Forest)
                        Icon(WellfitIcons.Down, "Dönem seç", Modifier.size(16.dp), tint = Forest)
                    }
                }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ anchor = period.shift(-1).start.toString() }) { Icon(WellfitIcons.Left, "Önceki dönem") }
                    Text(periodTitle(period), Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                    IconButton({ anchor = period.shift(1).start.toString() }, enabled = period.canAdvance) { Icon(WellfitIcons.Right, "Sonraki dönem") }
                }
                if (today !in period.start..period.end) TextButton({ anchor = today.toString() }, contentPadding = PaddingValues(horizontal = 0.dp)) {
                    Icon(WellfitIcons.Today, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(if (period.mode == PeriodMode.WEEK) "Bu haftaya dön" else "Bu aya dön")
                }
            }
        }
        if (stats.recordedDays.isEmpty()) {
            item(key = "empty") {
                ProgressSection(ProgressGreen, Modifier.testTag("progress-empty")) {
                    Icon(WellfitIcons.Period, null, Modifier.size(32.dp), tint = Forest)
                    Text(if (today in period.start..period.end) "Bu ${if (period.mode == PeriodMode.WEEK) "hafta" else "ay"} kayıt yok" else "Seçili ${if (period.mode == PeriodMode.WEEK) "haftada" else "ayda"} kayıt yok", style = MaterialTheme.typography.titleLarge)
                    OutlinedButton({ openDay(today.toString()) }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Forest), border = BorderStroke(1.dp, Forest.copy(alpha = .35f))) { Text("Bugünün kaydını aç"); Spacer(Modifier.width(8.dp)); Icon(WellfitIcons.Arrow, null, Modifier.size(18.dp)) }
                }
            }
        } else {
            if (stats.knownActivityDays > 0) item(key = "training") {
                ProgressSection(ProgressBlue, Modifier.testTag("progress-training")) {
                    ProgressHeading("Antrenman", WellfitIcons.Activity, ProgressBlueInk)
                    if (stats.trainingDays.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            ProgressNumber(number(stats.trainingDays.size), "Gün", Modifier.weight(1f))
                            if (stats.workoutCount > 0) {
                                VerticalDivider(color = ProgressBlueInk.copy(alpha = .16f))
                                ProgressNumber(number(stats.workoutCount), "Seans", Modifier.weight(1f))
                                VerticalDivider(color = ProgressBlueInk.copy(alpha = .16f))
                                ProgressNumber(number(stats.trainingMinutes), "Dakika", Modifier.weight(1f))
                            }
                        }
                    }
                    ActivityCalendar(state, period, openDay)
                    if (stats.workouts.isNotEmpty()) ProgressDisclosure("Seanslar", "${stats.workoutCount}", key = "sessions-${period.start}-${period.mode}") {
                        stats.trainingDays.sortedByDescending { it.date }.forEach { day ->
                            day.workouts.forEach { workout ->
                                ProgressRecord(workout.title, "${progressDate(day.date)} · ${workout.kind}", "${workout.minutes} dk", ProgressBlueInk) { openDay(day.date) }
                            }
                        }
                    }
                }
            }
            if (stats.stepDays.isNotEmpty()) item(key = "steps") {
                ProgressSection(ProgressGreen, Modifier.testTag("progress-steps")) {
                    ProgressHeading("Adımlar", WellfitIcons.Steps, Forest, "${stats.stepDays.size} gün")
                    ProgressNumber(number(stats.stepsAverage!!), "adım / gün", large = true)
                    DailyMetricChart(state, period, "adım", { it.steps?.toDouble() }, openDay, Forest)
                    TargetResult("Adım hedefinde", stats.stepTargetDays, Forest)
                    if (stats.stepDays.size > 1) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Toplam", style = MaterialTheme.typography.bodyMedium, color = Forest)
                        Text("${number(stats.stepsTotal)} adım", style = MaterialTheme.typography.labelLarge, color = Forest)
                    }
                }
            }
            if (stats.meals.isNotEmpty() || stats.closedDays.isNotEmpty()) item(key = "nutrition") {
                ProgressSection(ProgressSand, Modifier.testTag("progress-nutrition")) {
                    ProgressHeading("Beslenme", WellfitIcons.Nutrition, ProgressSandInk)
                    if (stats.caloriesAverage != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ProgressNumber(number(stats.caloriesAverage), "kcal / gün", Modifier.weight(1f), large = true)
                            ScopeCounter("${stats.closedDays.size} gün", ProgressSandInk)
                        }
                    }
                    DailyMetricChart(state, period, "kcal", { if (it.meals.isNotEmpty() || it.closed) it.calories.toDouble() else null }, openDay, ProgressSandInk)
                    if (stats.proteinAverage != null) {
                        HorizontalDivider(color = ProgressSandInk.copy(alpha = .15f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MacroAverage("Protein", stats.proteinAverage, null, Forest, Modifier.weight(1f))
                            stats.carbsAverage?.let { MacroAverage("Karb.", it, stats.knownCarbDays.size.takeIf { n -> n != stats.closedDays.size }, ProgressSandInk, Modifier.weight(1f)) }
                            stats.fatAverage?.let { MacroAverage("Yağ", it, stats.knownFatDays.size.takeIf { n -> n != stats.closedDays.size }, ProgressVioletInk, Modifier.weight(1f)) }
                        }
                        TargetResult("Protein hedefine ulaşan", stats.proteinTargetDays, Forest)
                        TargetResult("Kalori hedefinde (±%10)", stats.calorieTargetDays, ProgressSandInk)
                    }
                    if (stats.meals.isNotEmpty()) ProgressDisclosure("Öğünler", "${stats.meals.size}", key = "meals-${period.start}-${period.mode}") {
                        stats.mealCategories.filterValues { it > 0 }.forEach { (category, count) ->
                            CategoryDistribution(category, count, stats.meals.size, ProgressSandInk)
                        }
                    }
                }
            }
            if (stats.weightDays.isNotEmpty()) item(key = "weight") {
                ProgressSection(ProgressViolet, Modifier.testTag("progress-weight")) {
                    ProgressHeading("Kilo", WellfitIcons.WeightCurve, ProgressVioletInk, "${stats.weightDays.size} ölçüm")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProgressNumber(number(stats.latestWeight!!), "Son ölçüm · kg", Modifier.weight(1f), large = true)
                        stats.weightChange?.let { ChangeBadge(it, "kg", ProgressVioletInk) }
                    }
                    WeightTrend(stats, openDay)
                    state.profile.targetWeight?.let { target ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(WellfitIcons.Flag, null, Modifier.size(18.dp), tint = ProgressVioletInk)
                            Text("Hedef ${number(target)} kg", style = MaterialTheme.typography.labelLarge, color = ProgressVioletInk)
                        }
                    }
                    ProgressDisclosure("Ölçümler", "${stats.weightDays.size}", key = "weights-${period.start}-${period.mode}") {
                        stats.weightDays.asReversed().forEach { day ->
                            ProgressRecord(progressDate(day.date), null, "${number(day.weight!!)} kg", ProgressVioletInk) { openDay(day.date) }
                        }
                        if (period.mode == PeriodMode.MONTH && stats.weeklyWeights.size > 1) {
                            Text("Haftalık ortalama", style = MaterialTheme.typography.labelLarge, color = ProgressVioletInk)
                            stats.weeklyWeights.forEach { week ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${progressDate(week.start.toString())} · ${week.count} ölçüm", style = MaterialTheme.typography.bodySmall, color = Muted)
                                    Text("${number(week.kg)} kg", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
            if (comparisons.isNotEmpty()) item(key = "comparison") {
                ProgressSection(Color.White, Modifier.testTag("progress-comparison")) {
                    ProgressHeading("Karşılaştırma", WellfitIcons.Compare, Forest)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (period.mode == PeriodMode.WEEK) "Önceki hafta" else "Önceki ay", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = Muted)
                        Spacer(Modifier.width(18.dp))
                        Text(if (period.mode == PeriodMode.WEEK) "Seçili hafta" else "Seçili ay", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = Forest)
                    }
                    comparisons.forEach { comparison ->
                        ComparisonCard(comparison)
                    }
                }
            }
            item(key = "journal") {
                ProgressSection(Color.White) {
                    ProgressDisclosure("Günlük kayıtlar", "${stats.recordedDays.size}", key = "journal-${period.start}-${period.mode}") {
                        stats.recordedDays.sortedByDescending { it.date }.forEach { day ->
                            val values = buildList {
                                if (day.meals.isNotEmpty() || day.closed) add("${number(day.calories)} kcal")
                                day.steps?.let { add("${number(it)} adım") }
                                day.weight?.let { add("${number(it)} kg") }
                                if (day.workouts.isNotEmpty()) add("${day.workouts.size} seans")
                                else if (day.workoutStatus == "rest") add("Dinlenme")
                            }
                            ProgressRecord(LocalDate.parse(day.date).format(DateTimeFormatter.ofPattern("d MMMM EEEE", Turkish)), values.takeIf { it.isNotEmpty() }?.joinToString(" · "), null, Forest) { openDay(day.date) }
                        }
                    }
                }
            }
        }
    }
    if (periodPicker) ModalBottomSheet(onDismissRequest = { periodPicker = false }, containerColor = CanvasColor) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Görünüm", style = MaterialTheme.typography.titleLarge)
            listOf(PeriodMode.WEEK to "Hafta görünümü", PeriodMode.MONTH to "Ay görünümü").forEach { (mode, label) ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (period.mode == mode) ProgressGreen else Color.White).selectable(selected = period.mode == mode, role = androidx.compose.ui.semantics.Role.RadioButton, onClick = { modeName = mode.name; periodPicker = false }).padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(if (mode == PeriodMode.WEEK) WellfitIcons.Period else WellfitIcons.Calendar, null, tint = Forest)
                    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    if (period.mode == mode) Icon(WellfitIcons.Check, null, tint = Forest)
                }
            }
        }
    }
}

@Composable internal fun ProgressSection(color: Color, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(color).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp), content = content)
}

@Composable internal fun ProgressHeading(title: String, icon: ImageVector, color: Color, count: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, Modifier.size(24.dp), tint = color)
        Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge)
        count?.let { ScopeCounter(it, color) }
    }
}

@Composable private fun ScopeCounter(value: String, color: Color) {
    Text(value, Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = .7f)).padding(horizontal = 9.dp, vertical = 6.dp), color = color, style = MaterialTheme.typography.labelMedium)
}

@Composable private fun ProgressNumber(value: String, unit: String, modifier: Modifier = Modifier, large: Boolean = false) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(value, fontSize = if (large) 38.sp else 28.sp, lineHeight = if (large) 44.sp else 34.sp, letterSpacing = (-1).sp, fontWeight = FontWeight.SemiBold)
        Text(unit, style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}

@Composable private fun MacroAverage(label: String, value: Double?, count: Int?, color: Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        Text("${number(value!!)} g", style = MaterialTheme.typography.titleMedium)
        count?.let { Text("$it gün", style = MaterialTheme.typography.labelSmall, color = Muted) }
    }
}

@Composable internal fun TargetResult(label: String, score: GoalScore, color: Color) {
    if (score.eligible == 0) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = color)
            Text("${score.reached} / ${score.eligible} gün", style = MaterialTheme.typography.labelMedium, color = color)
        }
        LinearProgressIndicator(progress = { score.ratio }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = color, trackColor = color.copy(alpha = .12f), drawStopIndicator = {})
    }
}

@Composable internal fun ProgressDisclosure(title: String, count: String, key: String, content: @Composable ColumnScope.() -> Unit) {
    var expanded by rememberSaveable(key) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { expanded = !expanded }.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            Text(count, style = MaterialTheme.typography.labelMedium, color = Muted)
            Icon(if (expanded) WellfitIcons.Up else WellfitIcons.Down, if (expanded) "$title gizle" else "$title göster", Modifier.size(18.dp), tint = Muted)
        }
        if (expanded) content()
    }
}

@Composable internal fun ProgressRecord(title: String, detail: String?, value: String?, color: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .65f)).clickable(onClick = onClick).heightIn(min = 52.dp).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Muted) }
        }
        value?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = color) }
        Icon(WellfitIcons.Right, "Günün kayıtlarını aç", Modifier.size(16.dp), tint = color)
    }
}

@Composable private fun CategoryDistribution(label: String, count: Int, total: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.width(76.dp), style = MaterialTheme.typography.bodySmall)
        LinearProgressIndicator(progress = { count.toFloat() / total }, Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)), color = color, trackColor = color.copy(alpha = .12f), drawStopIndicator = {})
        Text(count.toString(), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable internal fun ChangeBadge(value: Double, unit: String, color: Color) {
    Row(Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = .75f)).padding(horizontal = 9.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (value < 0) WellfitIcons.Falling else if (value > 0) WellfitIcons.Rising else WellfitIcons.Flat, null, Modifier.size(16.dp), tint = color)
        Text("${signedNumber(value, unit)} $unit", style = MaterialTheme.typography.labelMedium, color = color)
    }
}

private data class ProgressComparison(val title: String, val unit: String, val current: Double, val previous: Double, val count: Int, val previousCount: Int, val scope: String = "gün")
@Composable private fun ComparisonCard(value: ProgressComparison) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CanvasColor).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(value.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            ChangeBadge(value.current - value.previous, value.unit, Forest)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${progressValue(value.previous, value.unit)} ${value.unit}", style = MaterialTheme.typography.labelLarge)
                Text("${value.previousCount} ${value.scope}", style = MaterialTheme.typography.labelSmall, color = Muted)
            }
            Icon(WellfitIcons.Arrow, null, Modifier.size(18.dp), tint = Muted)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${progressValue(value.current, value.unit)} ${value.unit}", style = MaterialTheme.typography.labelLarge, color = Forest)
                Text("${value.count} ${value.scope}", style = MaterialTheme.typography.labelSmall, color = Muted)
            }
        }
    }
}
