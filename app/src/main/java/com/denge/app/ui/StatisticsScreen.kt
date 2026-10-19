package com.denge.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denge.app.domain.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private fun Double?.display(suffix: String = "") = this?.let { number(it) + suffix } ?: "—"
private fun Double.signed(suffix: String) = (if (this > 0) "+" else "") + number(this) + suffix
private fun shortDate(date: String) = LocalDate.parse(date).format(DateTimeFormatter.ofPattern("d MMM", Turkish))

@Composable fun ProgressScreen(state: AppState, openDay: (String) -> Unit) {
    var modeName by rememberSaveable { mutableStateOf(PeriodMode.WEEK.name) }
    var anchor by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var section by rememberSaveable { mutableStateOf("Genel") }
    val today = LocalDate.now()
    val period = remember(modeName, anchor, today) { StatsPeriod(PeriodMode.valueOf(modeName), LocalDate.parse(anchor), today) }
    val stats = remember(state, period) { PeriodStatistics(state, period) }
    val previous = remember(state, period) { PeriodStatistics(state, period.shift(-1)) }
    LazyColumn(Modifier.fillMaxSize().testTag("statistics-list"), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Text("Emeğin görünür olsun.", style = MaterialTheme.typography.headlineLarge)
            Text("Rakamlar, alışkanlıkların ve büyük resim.", color = Muted)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(PeriodMode.WEEK to "Haftalık", PeriodMode.MONTH to "Aylık").forEach { (mode, label) ->
                    FilterChip(modeName == mode.name, { modeName = mode.name }, label = { Text(label) }, leadingIcon = { Icon(if (mode == PeriodMode.WEEK) Icons.Outlined.ViewWeek else Icons.Outlined.CalendarMonth, null, Modifier.size(17.dp)) })
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ anchor = period.shift(-1).start.toString() }) { Icon(Icons.Outlined.ChevronLeft, "Önceki dönem") }
                Text(if (period.mode == PeriodMode.MONTH) period.start.format(DateTimeFormatter.ofPattern("MMMM yyyy", Turkish))
                    else "${period.start.format(DateTimeFormatter.ofPattern("d MMM", Turkish))} – ${period.end.format(DateTimeFormatter.ofPattern("d MMM yyyy", Turkish))}",
                    Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                IconButton({ anchor = period.shift(1).start.toString() }, enabled = period.canAdvance) { Icon(Icons.Outlined.ChevronRight, "Sonraki dönem") }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${stats.recordedDays.size} kayıtlı · ${stats.closedDays.size} kapalı gün", color = Muted, style = MaterialTheme.typography.bodySmall)
                if (today !in period.start..period.end) TextButton({ anchor = today.toString() }) { Text("Bu döneme dön") }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Genel", "Beslenme", "Hareket", "Kilo").forEach { label -> FilterChip(section == label, { section = label }, label = { Text(label) }) }
            }
        }
        when (section) {
            "Genel" -> {
                item { StatGrid(listOf(
                    StatValue("Spor yaptığın gün", stats.trainingDays.size.toString(), "${period.elapsedDates.size} takvim günü içinde", Icons.Outlined.FitnessCenter),
                    StatValue("Toplam antrenman", stats.workoutCount.toString(), "${number(stats.trainingMinutes)} dakika", Icons.Outlined.Timer),
                    StatValue("Günlük adım", stats.stepsAverage.display(), "${stats.stepDays.size} ölçülen günün ortalaması", Icons.Outlined.DirectionsWalk),
                    StatValue("Günlük enerji", stats.caloriesAverage.display(" kcal"), "${stats.closedDays.size} kapalı günün ortalaması", Icons.Outlined.Restaurant)
                )) }
                item { ActivityCalendar(state, period, openDay) }
                item { Panel {
                    Section("Dönemin ritmi", "Kaydettiklerin üzerinden, tahmin eklemeden.")
                    GoalLine("Günü tamamlama", GoalScore(stats.closedDays.size, period.elapsedDates.size), Forest)
                    GoalLine("Adım hedefine ulaşma", stats.stepTargetDays, Orange)
                    GoalLine("Protein hedefine ulaşma", stats.proteinTargetDays, Lilac)
                    Text("Adım ve protein oranları yalnızca ölçüm bulunan / kapalı günlerden hesaplanır. Kayıtsız günler sıfır sayılmaz.", style = MaterialTheme.typography.bodySmall, color = Muted)
                } }
                item { Panel(color = Color(0xFFEEF2E7)) {
                    Section("Önceki dönemle birlikte")
                    ComparisonRow("Günlük enerji", stats.caloriesAverage, previous.caloriesAverage, " kcal")
                    ComparisonRow("Günlük adım", stats.stepsAverage, previous.stepsAverage, " adım")
                    Text("Bu dönem: ${stats.closedDays.size} kapalı / ${stats.stepDays.size} adım kayıtlı gün. Önceki dönem: ${previous.closedDays.size} kapalı / ${previous.stepDays.size} adım kayıtlı gün. Ortalama farkı gösterilir; dönemler farklı sayıda kayıt içerebilir.", color = Muted, style = MaterialTheme.typography.bodySmall)
                } }
            }
            "Beslenme" -> {
                item { StatGrid(listOf(
                    StatValue("Ortalama enerji", stats.caloriesAverage.display(" kcal"), "${stats.closedDays.size} kapalı gün", Icons.Outlined.Restaurant),
                    StatValue("Toplam enerji", if (stats.closedDays.isEmpty()) "—" else number(stats.caloriesTotal), "kcal · yalnızca kapalı günler", Icons.Outlined.LocalFireDepartment),
                    StatValue("Kaydedilen öğün", stats.meals.size.toString(), "Açık günlerdeki öğünler dahil", Icons.Outlined.MenuBook),
                    StatValue("Öğün / gün", stats.averageMealsOnClosedDays.display(), "Kapalı günlerin ortalaması", Icons.Outlined.Schedule)
                )) }
                item { DailyMetricChart(state, period, "Günlük enerji", "kcal", { d -> if (d.meals.isEmpty() && !d.closed) null else d.calories.toDouble() }, openDay) }
                item { Panel {
                    Section("Makroların", "Kapalı gün başına ortalama · gram")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SmallStat("Protein", stats.proteinAverage.display(" g"), Modifier.weight(1f))
                        SmallStat("Karbonhidrat", stats.carbsAverage.display(" g"), Modifier.weight(1f))
                        SmallStat("Yağ", stats.fatAverage.display(" g"), Modifier.weight(1f))
                    }
                    Text("Karbonhidrat: ${stats.knownCarbDays.size}, yağ: ${stats.knownFatDays.size} tam makro kayıtlı kapalı gün. Eksik makrolar ortalamaya katılmaz.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    GoalLine("Protein hedefi", stats.proteinTargetDays, Forest)
                    GoalLine("Kalori hedefine yakın günler", stats.calorieTargetDays, Orange)
                    Text("Kaloride o günkü hedefin ±%10 aralığı; proteinde o günkü hedefe ulaşma sayılır. Protein hedefi 0 olan günler protein oranına katılmaz.", color = Muted, style = MaterialTheme.typography.bodySmall)
                } }
                item { Panel {
                    Section("Öğün alışkanlıkların", "Açık günler dahil, girdiğin tüm öğünler.")
                    stats.mealCategories.forEach { (category, count) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(category); Text("$count öğün", fontWeight = FontWeight.SemiBold) }
                    }
                    if (stats.meals.isEmpty()) Text("Öğün ekledikçe bu bölüm dolacak.", color = Muted)
                } }
            }
            "Hareket" -> {
                item { StatGrid(listOf(
                    StatValue("Antrenman günü", stats.trainingDays.size.toString(), "Aynı günün seansları bir gün sayılır", Icons.Outlined.FitnessCenter),
                    StatValue("Toplam süre", "${number(stats.trainingMinutes)} dk", "${stats.workoutCount} antrenman kaydı", Icons.Outlined.Timer),
                    StatValue("Ortalama seans", stats.averageSessionMinutes.display(" dk"), "Kayıtlı antrenmanlar üzerinden", Icons.Outlined.AvTimer),
                    StatValue("Dinlenme günü", stats.restDays.size.toString(), "${stats.unknownActivityDays} günün durumu bilinmiyor", Icons.Outlined.Spa)
                )) }
                item { ActivityCalendar(state, period, openDay) }
                item { WeekdayChart(stats.weekdayActivity) }
                item { StatGrid(listOf(
                    StatValue("Toplam adım", if (stats.stepDays.isEmpty()) "—" else number(stats.stepsTotal), "${stats.stepDays.size} ölçülen gün", Icons.Outlined.DirectionsWalk),
                    StatValue("En hareketli gün", stats.bestStepDay?.steps?.let { number(it) } ?: "—", stats.bestStepDay?.date?.let(::shortDate) ?: "Adım kaydı yok", Icons.Outlined.EmojiEvents)
                )) }
                item { DailyMetricChart(state, period, "Günlük adımlar", "adım", { it.steps?.toDouble() }, openDay, Orange) }
                item { Panel {
                    Section("Spor günlerin", "Hareket / kas dağılımı yerine devamlılığın.")
                    if (stats.trainingDays.isEmpty()) EmptyState("Bu dönemde antrenman kaydı yok", "Hevy geçmişini Ayarlar'dan topluca içe aktarabilirsin.", Icons.Outlined.FitnessCenter)
                    stats.trainingDays.sortedByDescending { it.date }.forEach { day ->
                        Row(Modifier.fillMaxWidth().clickable { openDay(day.date) }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(shortDate(day.date), style = MaterialTheme.typography.titleMedium); Text(day.workouts.joinToString(" · ") { it.title }.ifBlank { "Antrenman yaptığın belirtilmiş" }, style = MaterialTheme.typography.bodySmall, color = Muted) }
                            Text("${day.workouts.sumOf { it.minutes }} dk", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                } }
            }
            "Kilo" -> {
                item { StatGrid(listOf(
                    StatValue("Son kilo", stats.latestWeight.display(" kg"), stats.weightDays.lastOrNull()?.date?.let(::shortDate) ?: "Bu dönemde kayıt yok", Icons.Outlined.MonitorWeight),
                    StatValue("Dönem değişimi", stats.weightChange?.signed(" kg") ?: "—", "İlk ve son ölçüm arasındaki fark", Icons.Outlined.Timeline),
                    StatValue("Ortalama kilo", stats.averageWeight.display(" kg"), "${stats.weightDays.size} tartı kaydı", Icons.Outlined.Insights),
                    StatValue("Kişisel hedefin", state.profile.targetWeight.display(" kg"), "Ayarlar'dan değiştirebilirsin", Icons.Outlined.Flag)
                )) }
                item { WeightTrend(stats) }
                item { Panel {
                    Section("Haftalık tartı ortalamaları", "Yalnızca bu dönemdeki ölçümler.")
                    if (stats.weeklyWeights.isEmpty()) EmptyState("Henüz tartı kaydı yok", "Bugün ekranındaki kilo kartından ilk kaydını ekle.", Icons.Outlined.MonitorWeight)
                    stats.weeklyWeights.forEach { week ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("${shortDate(week.start.toString())} haftası"); Text("${week.count} ölçüm", style = MaterialTheme.typography.bodySmall, color = Muted) }; Text("${number(week.kg)} kg", fontWeight = FontWeight.SemiBold) }
                    }
                    if (stats.weightDays.isNotEmpty()) Text("Aralık: ${stats.minimumWeight.display()} – ${stats.maximumWeight.display()} kg. Tek bir ölçümden kilo değişimi çıkarılmaz.", color = Muted, style = MaterialTheme.typography.bodySmall)
                } }
            }
        }
        item { Panel(color = Warm) {
            Section("Kayıt devamlılığın")
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SmallStat("kapalı gün", stats.closedDays.size.toString(), Modifier.weight(1f))
                SmallStat("en uzun kayıt serisi", "${stats.longestClosedStreak} gün", Modifier.weight(1f))
            }
            Text("Seçili dönem içindeki ardışık kapalı günler. Antrenman serisi değildir; dinlenme günlerini de kapsar.", color = Muted, style = MaterialTheme.typography.bodySmall)
        } }
        item { Section("Dönemin günlüğü", "Bir güne dokunarak kayıtlarını aç.") }
        if (stats.recordedDays.isEmpty()) item { EmptyState("Bu dönemde henüz kayıt yok", "Kayıt ekleyebilir veya geçmiş bir döneme geçebilirsin.") }
        items(stats.recordedDays.sortedByDescending { it.date }, key = { it.date }) { day ->
            Panel(Modifier.clickable { openDay(day.date) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconTile(if (day.trained()) Icons.Outlined.FitnessCenter else if (day.closed) Icons.Outlined.TaskAlt else Icons.Outlined.Schedule)
                    Column(Modifier.weight(1f)) {
                        Text(LocalDate.parse(day.date).format(DateTimeFormatter.ofPattern("d MMMM EEEE", Turkish)), style = MaterialTheme.typography.titleMedium)
                        Text("${if (day.meals.isNotEmpty() || day.closed) "${number(day.calories)} kcal" else "Beslenme kaydı yok"} · ${day.steps?.let { "${number(it)} adım" } ?: "Adım bilinmiyor"}", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                    Icon(Icons.Outlined.ChevronRight, "Günün kayıtlarını aç", tint = Muted)
                }
            }
        }
    }
}

internal data class StatValue(val label: String, val value: String, val detail: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
@Composable internal fun StatGrid(values: List<StatValue>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        values.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { value -> Panel(Modifier.weight(1f).fillMaxHeight(), color = Color(0xFFEEF2E7)) {
                    Icon(value.icon, null, tint = Forest, modifier = Modifier.size(23.dp))
                    Text(value.value, style = MaterialTheme.typography.headlineMedium)
                    Text(value.label, style = MaterialTheme.typography.labelLarge)
                    Text(value.detail, color = Muted, style = MaterialTheme.typography.bodySmall)
                } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
@Composable internal fun GoalLine(label: String, score: GoalScore, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium); Text(if (score.eligible == 0) "Ölçüm yok" else "${score.reached} / ${score.eligible} gün", style = MaterialTheme.typography.labelMedium, color = Muted) }
        LinearProgressIndicator(progress = { score.ratio }, Modifier.fillMaxWidth().height(6.dp), color = color, trackColor = color.copy(alpha = .12f), drawStopIndicator = {})
    }
}
@Composable private fun ComparisonRow(label: String, current: Double?, previous: Double?, unit: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f))
        Text(if (current == null || previous == null) "Karşılaştırma için kayıt yok" else (current - previous).signed(unit), style = MaterialTheme.typography.labelMedium, color = Forest)
    }
}
