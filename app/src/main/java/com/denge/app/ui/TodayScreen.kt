package com.denge.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denge.app.domain.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable fun TodayScreen(state: AppState, day: Day, date: LocalDate, busy: Boolean, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit,
    addMeal: () -> Unit, editMeal: (Meal) -> Unit, metrics: () -> Unit, sync: () -> Unit, note: () -> Unit, workout: () -> Unit, editWorkout: (Workout) -> Unit,
    hevy: () -> Unit, rest: () -> Unit, close: () -> Unit, reopen: () -> Unit, memory: () -> Unit) {
    val editable = !day.closed && !busy
    LazyColumn(Modifier.fillMaxSize().testTag("today-list"), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 26.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(Icons.Outlined.Spa, null, tint = Forest, modifier = Modifier.size(29.dp))
                    Text("denge", fontSize = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
                }
                Tag(if (day.closed) "GÜN TAMAMLANDI" else "KENDİ RİTMİNDE", if (day.closed) Forest else Muted, if (day.closed) Sage else Color(0xFFEBEFE4))
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(if (date != LocalDate.now()) "Gününe bir bakış." else state.profile.name.takeIf { it.isNotBlank() }?.let { "Merhaba, $it." } ?: "Her gün, küçük bir adım.", style = MaterialTheme.typography.headlineLarge)
                Text(if (day.closed) "Bir gün daha kendine yatırım yaptın." else "Mükemmel olmak değil, devam etmek.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).border(1.dp, Line, RoundedCornerShape(18.dp)), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onPrevious, enabled = !busy) { Icon(Icons.Outlined.ChevronLeft, "Önceki gün") }
                Text(date.format(DateTimeFormatter.ofPattern("d MMMM EEEE", Turkish)), Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelLarge)
                if (date != LocalDate.now()) TextButton(onToday) { Text("Bugün") }
                IconButton(onNext, enabled = date < LocalDate.now() && !busy) { Icon(Icons.Outlined.ChevronRight, "Sonraki gün") }
            }
        }
        item {
            Panel {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Günün dengesi", style = MaterialTheme.typography.titleMedium)
                    Text("HEDEF ${number(day.goals.calories)}", style = MaterialTheme.typography.labelMedium, color = Muted)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
                    CalorieRing(day.calories, day.goals.calories)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (day.calories <= day.goals.calories) "Hedefe kalan" else "Hedefin üzerinde", style = MaterialTheme.typography.bodySmall, color = Muted)
                        Text(number(kotlin.math.abs(day.goals.calories - day.calories)), fontSize = 29.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1).sp)
                        Text("kcal", style = MaterialTheme.typography.bodySmall, color = Muted)
                        Tag("${day.meals.size} öğün", Forest, Color(0xFFF0F4EA))
                    }
                }
                HorizontalDivider(color = Line)
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Macro("Protein", day.protein, day.goals.protein, Forest, Modifier.weight(1f))
                    Macro("Karbonhidrat", day.carbs, day.goals.carbs, Orange, Modifier.weight(1f))
                    Macro("Yağ", day.fat, day.goals.fat, Lilac, Modifier.weight(1f))
                }
                if (day.carbs == null || day.fat == null) Text("Bazı öğünlerde karbonhidrat veya yağ girilmedi; günlük toplamı bilinmiyor.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Panel(Modifier.weight(1f).clickable(enabled = editable, onClick = metrics), color = Color(0xFFEAF0DF)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.DirectionsWalk, null, tint = Forest); if (editable) Icon(Icons.Outlined.Edit, "Adımları düzenle", Modifier.size(15.dp), tint = Muted) }
                    Column { Text(day.steps?.let { number(it) } ?: "—", fontSize = 26.sp, fontWeight = FontWeight.Bold); Text("/ ${number(day.goals.steps)} adım", style = MaterialTheme.typography.bodySmall, color = Muted) }
                    Text(if (day.steps == null) "Adım sayını ekle" else day.stepSource, style = MaterialTheme.typography.bodySmall, color = Forest)
                }
                Panel(Modifier.weight(1f).clickable(enabled = editable, onClick = metrics), color = Warm) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.MonitorWeight, null, tint = Color(0xFF856544)); if (editable) Icon(Icons.Outlined.Add, "Kilo ekle", Modifier.size(15.dp), tint = Muted) }
                    Column { Text(day.weight?.let { number(it) } ?: "—", fontSize = 26.sp, fontWeight = FontWeight.Bold); Text("kilogram", style = MaterialTheme.typography.bodySmall, color = Muted) }
                    Text("${state.profile.targetWeight?.let { "Hedef ${number(it)} kg" } ?: "Kendi hızında ilerle"}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF856544))
                }
            }
            if (state.profile.healthEnabled && editable) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(sync) { Icon(Icons.Outlined.Sync, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Health Connect'ten güncelle", style = MaterialTheme.typography.bodySmall) } }
        }
        item { Section("Öğünlerin", "Yedikçe ekle, günün sonunda tamamla.", if (editable) "+ Ekle" else null, addMeal) }
        item {
            Panel {
                if (day.meals.isEmpty()) {
                    EmptyState("İlk öğününle başla", "Değerlerini kendin gir veya\nyediklerini AI'a anlat.", Icons.Outlined.Restaurant)
                    if (editable) PrimaryButton("Öğün ekle", Icons.Outlined.Add, onClick = addMeal)
                } else {
                    day.meals.sortedBy { it.time }.forEachIndexed { index, meal ->
                        if (index > 0) HorizontalDivider(color = Line)
                        Row(Modifier.fillMaxWidth().clickable(enabled = editable) { editMeal(meal) }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconTile(when (meal.category) { "Kahvaltı" -> Icons.Outlined.WbSunny; "Ara öğün" -> Icons.Outlined.LocalCafe; "Akşam" -> Icons.Outlined.Nightlight; else -> Icons.Outlined.Restaurant }, bg = Color(0xFFF0F3EA))
                            Column(Modifier.weight(1f)) { Text(meal.name, style = MaterialTheme.typography.titleMedium); Text("${meal.category} · ${meal.time}", style = MaterialTheme.typography.bodySmall, color = Muted); if (meal.aiSourceText != null) Text("AI destekli · değerler tahmini olabilir", style = MaterialTheme.typography.bodySmall, color = Forest); Text("P ${number(meal.protein)} · K ${macroNumber(meal.carbs)} · Y ${macroNumber(meal.fat)} g", style = MaterialTheme.typography.bodySmall, color = Muted) }
                            Column(horizontalAlignment = Alignment.End) { Text(number(meal.calories), fontWeight = FontWeight.SemiBold); Text("kcal", style = MaterialTheme.typography.bodySmall, color = Muted) }
                        }
                    }
                }
            }
        }
        item { Section("Hareket alanın", "Antrenmanın da hikâyenin bir parçası.") }
        item {
            Panel {
                if (day.workouts.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconTile(Icons.Outlined.FitnessCenter, bg = Color(0xFFEEEAF4), color = Color(0xFF817297))
                        Column(Modifier.weight(1f)) { Text(if (day.workoutStatus == "rest") "Bugün dinlenme günü" else "Bugün antrenman yaptın mı?", style = MaterialTheme.typography.titleMedium); Text(if (day.workoutStatus == "rest") "Toparlanmak da ilerlemenin parçası." else "Hevy'den al veya kendin ekle.", color = Muted, style = MaterialTheme.typography.bodySmall) }
                    }
                } else day.workouts.forEach { w ->
                    Row(Modifier.fillMaxWidth().clickable(enabled = editable) { editWorkout(w) }, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Outlined.FitnessCenter, bg = Color(0xFFEEEAF4), color = Color(0xFF817297))
                        Column(Modifier.weight(1f)) { Text(w.title, style = MaterialTheme.typography.titleMedium); Text("${w.minutes} dk · ${w.kind} · ${w.source}", style = MaterialTheme.typography.bodySmall, color = Muted) }
                        Icon(Icons.Outlined.ChevronRight, "Antrenman detayları", tint = Muted)
                    }
                }
                if (editable) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(hevy, Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Outlined.Sync, null, Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("Hevy'den al") }
                        OutlinedButton(workout, Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Outlined.Add, null, Modifier.size(17.dp)); Text("Kendim ekle") }
                    }
                    if (day.workouts.isEmpty() && day.workoutStatus != "rest") TextButton(rest, Modifier.align(Alignment.CenterHorizontally)) { Text("Bugün dinleniyorum", color = Muted) }
                }
            }
        }
        item {
            Panel(Modifier.clickable(enabled = editable, onClick = note), color = Color(0xFFF0F2E9)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.EditNote, null, tint = Forest)
                    Column(Modifier.weight(1f)) { Text("Kendine bir not", style = MaterialTheme.typography.titleMedium); Text(day.note.ifBlank { "Enerjin, açlığın, uykun… Bugün nasıl geçti?" }, style = MaterialTheme.typography.bodyMedium, color = Muted) }
                    if (editable) Icon(Icons.Outlined.Add, "Gün notunu düzenle", tint = Forest)
                }
            }
        }
        item {
            if (day.closed) { PrimaryButton("Günün özetini gör", Icons.Outlined.AutoAwesome, onClick = memory); TextButton(reopen, Modifier.fillMaxWidth(), enabled = !busy) { Text("Günü yeniden aç") } }
            else {
                PrimaryButton("Günü kapat", Icons.Outlined.Nightlight, enabled = !busy, onClick = close)
                Text("Son öğününü ekledikten sonra. Karar sende.", Modifier.fillMaxWidth().padding(top = 10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
    }
}
