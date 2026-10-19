package com.denge.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denge.app.domain.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable fun ProgressScreen(state: AppState, openDay: (String) -> Unit) {
    var range by rememberSaveable { mutableIntStateOf(7) }
    val today = LocalDate.now()
    val start = today.minusDays(range.toLong() - 1)
    val days = state.days.values.filter { LocalDate.parse(it.date) in start..today && it.hasData }.sortedBy { it.date }
    val closed = days.filter { it.closed }
    val weights = days.filter { it.weight != null }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item { Text("Büyük resme bak.", style = MaterialTheme.typography.headlineLarge); Text("Tek bir günden fazlasısın.", color = Muted) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { listOf(7, 30).forEach { FilterChip(range == it, { range = it }, label = { Text("Son $it gün") }, leadingIcon = if (range == it) ({ Icon(Icons.Outlined.Check, null, Modifier.size(16.dp)) }) else null) } } }
        item { Panel {
            Section("İstikrarın", "$range günlük dönemde ${closed.size} tamamlanmış gün")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallStat("ortalama kcal¹", if (closed.isEmpty()) "—" else number(closed.map { it.calories }.average().toInt()), Modifier.weight(1f))
                SmallStat("ortalama adım²", days.mapNotNull { it.steps }.takeIf { it.isNotEmpty() }?.let { number(it.average().toInt()) } ?: "—", Modifier.weight(1f))
                SmallStat("antrenman", number(days.sumOf { it.workouts.size }), Modifier.weight(1f))
            }
            Text("¹ Yalnızca kapalı günler. ² Adımı girilmiş ${days.count { it.steps != null }} gün. Eksik günler ortalamaya katılmaz.", style = MaterialTheme.typography.bodySmall, color = Muted)
        } }
        item { Panel {
            Section("Günlük enerji", "Tüketilen kalori · kcal")
            if (days.isEmpty()) EmptyState("Grafiğin burada büyüyecek", "İlk öğününü eklediğinde ilerlemeni görmeye başlarsın.", Icons.Outlined.BarChart)
            else {
                val chartDates = (0 until range).map { start.plusDays(it.toLong()) }
                val max = maxOf(state.profile.goals.calories, days.maxOfOrNull { it.calories } ?: 0).toFloat() * 1.15f
                Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                    val slot = size.width / range
                    chartDates.forEachIndexed { i, date ->
                        val day = state.days[date.toString()]
                        val x = i * slot + slot / 2
                        val base = size.height - 3.dp.toPx()
                        if (day != null && day.hasData) {
                            val h = (day.calories / max * (size.height - 10.dp.toPx())).coerceAtLeast(2.dp.toPx())
                            drawLine(if (day.closed) Forest else Forest.copy(alpha = .3f), Offset(x, base), Offset(x, base - h), strokeWidth = slot * .52f, cap = StrokeCap.Round)
                        } else drawCircle(Line, 2.dp.toPx(), Offset(x, base))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(start.format(DateTimeFormatter.ofPattern("d MMM", Turkish)), color = Muted, style = MaterialTheme.typography.bodySmall); Text("Bugün", color = Muted, style = MaterialTheme.typography.bodySmall) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Tag("Kapalı gün", Forest, Sage); Tag("Açık gün", Muted, Color(0xFFEEF0EB)) }
            }
        } }
        item { Panel {
            Section("Kilo yolculuğun", "Günlük dalgalanmalar doğaldır.")
            if (weights.isEmpty()) EmptyState("Henüz tartı kaydı yok", "İstersen Bugün ekranından kilonu ekleyebilirsin.", Icons.Outlined.MonitorWeight)
            else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallStat("son kayıt · kg", number(weights.last().weight!!), Modifier.weight(1f))
                    SmallStat("dönem değişimi", if (weights.size < 2) "—" else { val diff = weights.last().weight!! - weights.first().weight!!; "${if (diff > 0) "+" else ""}${number(diff)} kg" }, Modifier.weight(1f))
                }
                if (weights.size >= 2) {
                    Canvas(Modifier.fillMaxWidth().height(110.dp).padding(8.dp)) {
                        val min = weights.minOf { it.weight!! } - .5
                        val max = weights.maxOf { it.weight!! } + .5
                        val points = weights.map { d ->
                            val dayOffset = java.time.temporal.ChronoUnit.DAYS.between(start, LocalDate.parse(d.date)).toFloat()
                            Offset(dayOffset / (range - 1) * size.width, (size.height * (1 - (d.weight!! - min) / (max - min))).toFloat())
                        }
                        val path = Path().apply { points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
                        drawPath(path, Forest, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                        points.forEach { drawCircle(Forest, 4.dp.toPx(), it) }
                    }
                }
                Text("${weights.size} tartı kaydı · ${weights.first().date} — ${weights.last().date}", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        } }
        item { Section("Günlük arşivin", "Açmak için bir güne dokun.") }
        if (state.days.values.none { it.hasData }) item { EmptyState("Birlikte biriktireceğiz", "Kayıtların burada tarih sırasıyla yer alacak.") }
        items(state.days.values.filter { it.hasData }.sortedByDescending { it.date }, key = { it.date }) { d ->
            Panel(Modifier.clickable { openDay(d.date) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconTile(if (d.closed) Icons.Outlined.TaskAlt else Icons.Outlined.Schedule)
                    Column(Modifier.weight(1f)) { Text(LocalDate.parse(d.date).format(DateTimeFormatter.ofPattern("d MMMM EEEE", Turkish)), style = MaterialTheme.typography.titleMedium); Text("${number(d.calories)} kcal · ${d.meals.size} öğün · ${d.steps?.let { "${number(it)} adım" } ?: "adım yok"}", style = MaterialTheme.typography.bodySmall, color = Muted) }
                    Icon(Icons.Outlined.ChevronRight, "Günü aç", tint = Muted)
                }
            }
        }
    }
}
@Composable fun MemoryScreen(state: AppState, date: LocalDate, busy: Boolean, retry: () -> Unit, prompt: () -> Unit, export: () -> Unit, selectDate: (String) -> Unit) {
    val day = state.day(date)
    val recap = day.recap
    val recapDays = state.days.values.filter { it.closed }.sortedByDescending { it.date }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item { Text("Günün geride\nbıraktıkları.", style = MaterialTheme.typography.headlineLarge); Text("Seni hatırlayan, seninle gelişen bir günlük.", color = Muted) }
        item { Panel(color = Color(0xFFEAF0DF)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { IconTile(Icons.Outlined.AutoAwesome, bg = Color.White.copy(alpha = .7f)); Column(Modifier.weight(1f)) { Text("Denge hafızası", style = MaterialTheme.typography.titleLarge); Text("${state.days.values.count { it.hasData }} gün · ${state.days.values.sumOf { it.meals.size }} öğün · ${state.days.values.sumOf { it.workouts.size }} antrenman", style = MaterialTheme.typography.bodySmall, color = Forest) } }
            Text("Günlük kayıtların, haftalık ve aylık durumun, antrenman detayların ve kalıcı notların her değerlendirmede bir araya gelir.", color = Forest, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(export, enabled = !busy, shape = RoundedCornerShape(14.dp)) { Icon(Icons.Outlined.FileDownload, null, Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text("Hafızayı Markdown olarak al") }
        } }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionDate(date)
                if (day.closed) Tag("GÜN KAPALI")
            }
        }
        if (recapDays.isNotEmpty()) item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { recapDays.forEach { d -> FilterChip(d.date == date.toString(), { selectDate(d.date) }, label = { Text(LocalDate.parse(d.date).format(DateTimeFormatter.ofPattern("d MMM", Turkish))) }) } }
        }
        item { Panel {
            when {
                !day.closed -> EmptyState("Günün henüz devam ediyor", "Son öğününden sonra Bugün ekranında 'Günü kapat'a dokun. Özetin burada seni bekleyecek.", Icons.Outlined.Nightlight)
                recap?.status == "ready" -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Outlined.AutoAwesome, null, tint = Forest, modifier = Modifier.size(20.dp)); Text("Günün değerlendirmesi", style = MaterialTheme.typography.titleMedium) }
                    SelectionContainer { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { recap.text.split("\n").filter { it.isNotBlank() }.forEach { line ->
                        val heading = line.startsWith("#") || (line.startsWith("**") && line.endsWith("**"))
                        Text(line.trimStart('#', ' ').replace("**", ""), style = if (heading) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
                    } } }
                    HorizontalDivider(color = Line)
                    Text("${recap.model} · Kayıtlarına göre AI tarafından hazırlandı.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                else -> {
                    val consent = state.profile.aiConsent
                    EmptyState(if (!consent) "Günün güvenle kaydedildi" else if (recap?.status == "failed") "Özet tamamlanamadı" else "Özet için her şey kayıtlı",
                        if (!consent) "AI özeti istersen Ayarlar'dan DeepSeek bağlantısını ve veri paylaşımını aç."
                        else recap?.error?.takeIf { it.isNotBlank() } ?: "Bağlantı ve API anahtarı hazır olduğunda özetin oluşturulacak. Uygulamayı kapatabilirsin.", Icons.Outlined.CloudQueue)
                    if (recap?.status == "running") Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator(Modifier.size(24.dp), color = Forest) }
                    PrimaryButton(if (recap?.status == "failed") "Yeniden dene" else "Özeti oluştur / tekrar dene", Icons.Outlined.AutoAwesome, !busy && recap?.status != "running", retry)
                }
            }
            if (day.closed) TextButton(prompt, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.DataObject, null, Modifier.size(16.dp)); Spacer(Modifier.width(7.dp)); Text("AI'ya gönderilen veriyi incele") }
        } }
        if (day.closed) item { Panel {
            Section("Kaydının özeti")
            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) { SmallStat("kcal", number(day.calories), Modifier.weight(1f)); SmallStat("adım", day.steps?.let { number(it) } ?: "—", Modifier.weight(1f)); SmallStat("antrenman", day.workouts.size.toString(), Modifier.weight(1f)) }
            Text("${day.meals.size} öğün · P ${number(day.protein)} / K ${number(day.carbs)} / Y ${number(day.fat)} g", color = Muted, style = MaterialTheme.typography.bodySmall)
            if (day.note.isNotBlank()) Text(day.note, style = MaterialTheme.typography.bodyMedium)
        } }
        if (day.archivedRecaps.any { it.text.isNotBlank() }) item { Panel {
            Section("Önceki değerlendirmeler", "Günü yeniden açmadan önceki kayıtlar.")
            day.archivedRecaps.filter { it.text.isNotBlank() }.forEach { old ->
                var expanded by remember { mutableStateOf(false) }
                TextButton({ expanded = !expanded }) { Text("${old.createdAt.take(10)} · ${if (expanded) "Gizle" else "Göster"}") }
                if (expanded) Text(old.text, style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
        } }
        if (state.profile.memory.isNotBlank()) item { Panel(color = Warm) { Section("Senin için aklımda"); Text(state.profile.memory, style = MaterialTheme.typography.bodyMedium) } }
    }
}
@Composable private fun SectionDate(date: LocalDate) { Column { Text(date.format(DateTimeFormatter.ofPattern("d MMMM", Turkish)), style = MaterialTheme.typography.titleLarge); Text(date.format(DateTimeFormatter.ofPattern("EEEE", Turkish)), style = MaterialTheme.typography.bodySmall, color = Muted) } }
