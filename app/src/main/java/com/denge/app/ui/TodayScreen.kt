package com.denge.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val SummarySurface = Color(0xFFE5ECE6)
private val MealSurface = Color(0xFFF1EBE1)
private val TrainingSurface = Color(0xFFE5EBF0)
private val TrainingInk = Color(0xFF3E586F)
private val OnForestMuted = Color(0xFFCFDED6)

@Composable fun TodayScreen(state: AppState, day: Day, date: LocalDate, busy: Boolean, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit,
    addMeal: () -> Unit, editMeal: (Meal) -> Unit, metrics: () -> Unit, sync: () -> Unit, note: () -> Unit, workout: () -> Unit, editWorkout: (Workout) -> Unit,
    hevy: () -> Unit, rest: () -> Unit, close: () -> Unit, reopen: () -> Unit, memory: () -> Unit, fabClearance: Boolean = false) {
    val editable = !day.closed && !busy
    LazyColumn(Modifier.fillMaxSize().testTag("today-list"), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = if (fabClearance) 104.dp else 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item(key = "summary") {
            TodaySection(SummarySurface, Modifier.testTag("today-summary")) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (date == LocalDate.now()) "Bugün" else "Günlük", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
                        Text(date.format(DateTimeFormatter.ofPattern("d MMMM EEEE", Turkish)), color = Forest, style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onPrevious, enabled = !busy) { Icon(WellfitIcons.Left, "Önceki gün", tint = Forest) }
                    IconButton(onNext, enabled = date < LocalDate.now() && !busy) { Icon(WellfitIcons.Right, "Sonraki gün") }
                }
                if (date != LocalDate.now()) TextButton(onToday, enabled = !busy, contentPadding = PaddingValues(horizontal = 0.dp)) { Icon(WellfitIcons.Today, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Bugüne dön") }
                CalorieSummary(day)
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TodayMacro("Protein", day.protein, day.goals.protein, Forest, Modifier.weight(1f))
                    TodayMacro("Karb.", day.carbs, day.goals.carbs, Color(0xFF976336), Modifier.weight(1f))
                    TodayMacro("Yağ", day.fat, day.goals.fat, Color(0xFF72608F), Modifier.weight(1f))
                }
                if (day.carbs == null || day.fat == null) Text("Eksik makro içeren öğünler var; ilgili toplam gösterilmiyor.", color = Forest, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(color = Forest.copy(alpha = .14f))
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Adım", day.steps?.let(::number) ?: "—", "Hedef ${number(day.goals.steps)}", if (day.steps == null) "Girilmedi" else day.stepSource,
                        WellfitIcons.Steps, editable, metrics, Modifier.weight(1f).fillMaxHeight())
                    MetricCard("Kilo", day.weight?.let(::number) ?: "—", "kg", state.profile.targetWeight?.let { "Hedef ${number(it)} kg" } ?: if (day.weight == null) "Girilmedi" else "Kaydedildi",
                        WellfitIcons.Weight, editable, metrics, Modifier.weight(1f).fillMaxHeight())
                }
                if (state.profile.healthEnabled) {
                    day.stepSyncedAt?.let { stamp ->
                        val time = runCatching { Instant.parse(stamp).atZone(ZoneId.of(day.zone)).format(DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull()
                        if (time != null) Text("Son adım eşitleme · $time", style = MaterialTheme.typography.bodySmall, color = Forest)
                    }
                    if (editable) TextButton(sync, contentPadding = PaddingValues(horizontal = 0.dp)) { Icon(WellfitIcons.Sync, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Health Connect'ten güncelle", style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
        item(key = "meals") {
            TodaySection(MealSurface, Modifier.testTag("today-meals")) {
                TodaySectionHeader("Öğünler", "${day.meals.size} kayıt · ${number(day.calories)} kcal", WellfitIcons.Meals, Color(0xFF85603E), if (editable) addMeal else null)
                if (day.meals.isEmpty()) {
                    Text("Henüz öğün kaydı yok", style = MaterialTheme.typography.titleMedium)
                    Text("Kalori ve makroları gir veya yediklerini AI ile kayda dönüştür.", color = Muted, style = MaterialTheme.typography.bodyMedium)
                    if (editable) OutlinedButton(addMeal, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFFB7A58D))) {
                        Icon(WellfitIcons.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Öğün ekle")
                    }
                } else day.meals.sortedBy { it.time }.forEach { meal ->
                    MealRecord(meal, editable) { editMeal(meal) }
                }
            }
        }
        item(key = "training") {
            TodaySection(TrainingSurface, Modifier.testTag("today-training")) {
                TodaySectionHeader("Antrenmanlar", if (day.workouts.isEmpty()) "Günlük hareket kaydı" else "${day.workouts.size} seans · ${day.workouts.sumOf { it.minutes }} dk", WellfitIcons.Workout, TrainingInk, if (editable) workout else null)
                if (day.workouts.isEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (day.workoutStatus == "rest") WellfitIcons.Rest else WellfitIcons.Workout, null, Modifier.size(30.dp), tint = TrainingInk)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (day.workoutStatus == "rest") "Dinlenme günü" else "Antrenman kaydı yok", style = MaterialTheme.typography.titleMedium)
                            Text(if (day.workoutStatus == "rest") "Dinlenme olarak kaydedildi." else "Antrenman durumu belirtilmedi.", color = TrainingInk, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else day.workouts.forEach { w ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .8f)).clickable(enabled = editable) { editWorkout(w) }.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(WellfitIcons.Workout, null, Modifier.size(24.dp), tint = TrainingInk)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(w.title, style = MaterialTheme.typography.titleMedium); Text("${w.minutes} dk · ${w.kind} · ${w.source}", style = MaterialTheme.typography.bodySmall, color = TrainingInk) }
                        if (editable) Icon(WellfitIcons.Right, "Antrenman detayları", Modifier.size(18.dp), tint = TrainingInk)
                    }
                }
                if (editable) {
                    // FlowRow keeps complete labels and 48 dp targets at larger font scales.
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(hevy, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, TrainingInk.copy(alpha = .35f)), contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.heightIn(min = 48.dp)) { Icon(WellfitIcons.Sync, null, Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text("Hevy'den al") }
                        OutlinedButton(workout, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, TrainingInk.copy(alpha = .35f)), contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.heightIn(min = 48.dp)) { Icon(WellfitIcons.Add, null, Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text("Kendim ekle") }
                    }
                    if (day.workouts.isEmpty() && day.workoutStatus != "rest") TextButton(rest, contentPadding = PaddingValues(horizontal = 0.dp)) { Icon(WellfitIcons.Rest, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Bugün dinleniyorum", color = TrainingInk) }
                }
            }
        }
        item(key = "note") {
            TodaySection(Color.White, Modifier.clickable(enabled = editable, onClick = note)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(WellfitIcons.Note, null, tint = Forest)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("Gün notu", style = MaterialTheme.typography.titleMedium); Text(day.note.ifBlank { "Henüz not eklenmedi." }, style = MaterialTheme.typography.bodyMedium, color = Muted) }
                    if (editable) Icon(WellfitIcons.Edit, "Gün notunu düzenle", Modifier.size(20.dp), tint = Forest)
                }
            }
        }
        item(key = "close") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (day.closed) {
                    Text("Gün kapalı · Düzenlemek için yeniden aç.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    PrimaryButton("Günün özetini gör", WellfitIcons.Memory, onClick = memory)
                    TextButton(reopen, Modifier.fillMaxWidth(), enabled = !busy) { Text("Günü yeniden aç") }
                } else PrimaryButton("Günü kapat", WellfitIcons.Complete, enabled = !busy, onClick = close)
            }
        }
    }
}

@Composable private fun TodaySection(color: Color, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(color).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}

@Composable private fun CalorieSummary(day: Day) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Forest).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(WellfitIcons.Energy, null, Modifier.size(18.dp), tint = OnForestMuted)
            Text("Beslenme özeti", color = OnForestMuted, style = MaterialTheme.typography.labelLarge)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(number(day.calories), color = Color.White, fontSize = 44.sp, lineHeight = 48.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-2).sp, modifier = Modifier.weight(1f, fill = false))
            Text("kcal", color = OnForestMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 5.dp))
        }
        LinearProgressIndicator(progress = { if (day.goals.calories > 0) (day.calories.toFloat() / day.goals.calories).coerceIn(0f, 1f) else 0f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = Color(0xFFC8E6AB), trackColor = Color.White.copy(alpha = .16f), drawStopIndicator = {})
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Hedef ${number(day.goals.calories)}", color = OnForestMuted, style = MaterialTheme.typography.bodySmall)
            Text("${number(abs(day.goals.calories - day.calories))} kcal ${if (day.calories <= day.goals.calories) "kaldı" else "üzerinde"}", color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable private fun TodayMacro(label: String, value: Double?, goal: Int, color: Color, modifier: Modifier) {
    Column(modifier.fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .72f)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        Text("${macroNumber(value)} g", style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(progress = { if (goal > 0 && value != null) (value / goal).toFloat().coerceIn(0f, 1f) else 0f }, modifier = Modifier.fillMaxWidth().height(3.dp), color = color, trackColor = color.copy(alpha = .12f), drawStopIndicator = {})
        Text("/ $goal g", style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}

@Composable private fun MetricCard(label: String, value: String, target: String, source: String, icon: ImageVector, editable: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .8f)).clickable(enabled = editable, onClickLabel = "$label düzenle", onClick = onClick).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, Modifier.size(20.dp), tint = Forest)
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = Forest)
            if (editable) Icon(WellfitIcons.Edit, null, Modifier.size(14.dp), tint = Muted)
        }
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.8).sp)
        Text(target, style = MaterialTheme.typography.bodySmall, color = Muted)
        Text(source, style = MaterialTheme.typography.bodySmall, color = Forest)
    }
}

@Composable private fun TodaySectionHeader(title: String, detail: String, icon: ImageVector, color: Color, add: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, Modifier.size(24.dp), tint = color)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text(detail, color = color, style = MaterialTheme.typography.bodySmall)
        }
        if (add != null) IconButton(add, Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .8f))) { Icon(WellfitIcons.Add, "$title ekle", tint = color) }
    }
}

@Composable private fun MealRecord(meal: Meal, editable: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .85f)).clickable(enabled = editable, onClick = onClick).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(when (meal.category) { "Kahvaltı" -> WellfitIcons.Sun; "Ara öğün" -> WellfitIcons.Coffee; "Akşam" -> WellfitIcons.Moon; else -> WellfitIcons.Meals }, null, Modifier.size(18.dp), tint = Color(0xFF85603E))
            Text("${meal.category} · ${meal.time}", style = MaterialTheme.typography.labelMedium, color = Color(0xFF85603E), modifier = Modifier.weight(1f))
            if (editable) Icon(WellfitIcons.Right, "Öğünü düzenle", Modifier.size(16.dp), tint = Muted)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Text(meal.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) { Text(number(meal.calories), fontSize = 20.sp, fontWeight = FontWeight.SemiBold); Text("kcal", style = MaterialTheme.typography.bodySmall, color = Muted) }
        }
        Text("P ${number(meal.protein)} g   ·   K ${macroNumber(meal.carbs)} g   ·   Y ${macroNumber(meal.fat)} g", style = MaterialTheme.typography.bodySmall, color = Muted)
        if (meal.aiSourceText != null) Text("AI tahmini", style = MaterialTheme.typography.labelMedium, color = Forest)
    }
}
