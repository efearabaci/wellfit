package com.denge.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denge.app.domain.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val MemoryGreen = Color(0xFFE5ECE6)
private val MemoryBlue = Color(0xFFE5EBF0)
private val MemorySand = Color(0xFFF1EBE1)
private val MemoryViolet = Color(0xFFEBE7F0)
private val MemoryBlueInk = Color(0xFF3E586F)
private val MemoryVioletInk = Color(0xFF72608F)

private enum class MemoryStatus(val label: String) {
    OPEN("Tamamlanmadı"), OFF("AI kapalı"), KEY_REQUIRED("Bağlantı gerekli"), WAITING("Sırada"), RUNNING("Hazırlanıyor"), FAILED("Hazırlanamadı"), READY("Hazır"), MISSING("Değerlendirme yok")
}
private fun memoryStatus(day: Day, profile: Profile, hasAiKey: Boolean): MemoryStatus = when {
    !day.closed -> MemoryStatus.OPEN
    day.recap?.status == "ready" && day.recap.text.isNotBlank() -> MemoryStatus.READY
    day.recap == null -> MemoryStatus.MISSING
    !profile.aiConsent -> MemoryStatus.OFF
    !hasAiKey -> MemoryStatus.KEY_REQUIRED
    day.recap?.status == "running" -> MemoryStatus.RUNNING
    day.recap?.status == "pending" -> MemoryStatus.WAITING
    day.recap?.status == "failed" || day.recap?.status == "ready" -> MemoryStatus.FAILED
    else -> MemoryStatus.MISSING
}
private fun memoryTimestamp(stamp: String, zone: String): String = runCatching {
    Instant.parse(stamp).atZone(ZoneId.of(zone)).format(DateTimeFormatter.ofPattern("d MMM · HH:mm", Turkish))
}.getOrDefault(stamp.take(16).replace('T', ' '))

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MemoryScreen(state: AppState, date: LocalDate, busy: Boolean, retry: () -> Unit, prompt: () -> Unit, export: () -> Unit, selectDate: (String) -> Unit,
    hasAiKey: Boolean, openDay: () -> Unit, settings: () -> Unit) {
    val day = state.day(date)
    val recap = day.recap
    val today = LocalDate.now()
    val history = state.days.values.filter { LocalDate.parse(it.date) <= today && (it.closed || it.recap != null || it.archivedRecaps.isNotEmpty()) }.sortedByDescending { it.date }
    val dates = (history.map { it.date } + today.toString() + date.toString()).distinct().sorted()
    val index = dates.indexOf(date.toString())
    val status = memoryStatus(day, state.profile, hasAiKey)
    val articles = remember(recap?.id, recap?.text) { memoryArticles(recap?.text.orEmpty()) }
    val archived = day.archivedRecaps.filter { it.text.isNotBlank() }.sortedByDescending { it.createdAt }
    val lastReady = history.firstOrNull { it.date != day.date && it.recap?.status == "ready" && it.recap.text.isNotBlank() }
    var historyOpen by rememberSaveable { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().testTag("memory-list"), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item(key = "header") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Hafıza", Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.headlineLarge)
                IconButton(export, Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White), enabled = !busy) { Icon(WellfitIcons.Export, "Hafızayı dışa aktar", tint = Forest) }
            }
        }
        item(key = "date") {
            MemorySection(MemoryGreen, Modifier.testTag("memory-date")) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ selectDate(dates[index - 1]) }, enabled = index > 0 && !busy) { Icon(WellfitIcons.Left, "Önceki değerlendirme", tint = if (index > 0) Forest else Muted.copy(alpha = .4f)) }
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable(enabled = !busy) { historyOpen = true }.heightIn(min = 56.dp).padding(horizontal = 4.dp).testTag("memory-history-picker"), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "d MMMM" else "d MMM yyyy", Turkish)), Modifier.weight(1f, fill = false), style = MaterialTheme.typography.titleLarge)
                            Icon(WellfitIcons.Down, "Değerlendirme tarihi seç", Modifier.size(16.dp), tint = Forest)
                        }
                        Text(date.format(DateTimeFormatter.ofPattern("EEEE", Turkish)), style = MaterialTheme.typography.bodySmall, color = Forest)
                    }
                    IconButton({ selectDate(dates[index + 1]) }, enabled = index < dates.lastIndex && !busy) { Icon(WellfitIcons.Right, "Sonraki değerlendirme", tint = if (index < dates.lastIndex) Forest else Muted.copy(alpha = .4f)) }
                }
                if (status == MemoryStatus.READY) {
                    HorizontalDivider(color = Forest.copy(alpha = .12f))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { Icon(WellfitIcons.Ai, null, Modifier.size(16.dp), tint = Forest); Text("AI değerlendirmesi", style = MaterialTheme.typography.labelMedium, color = Forest) }
                        Text(recap!!.model, style = MaterialTheme.typography.labelMedium, color = Muted)
                        Text(memoryTimestamp(recap.createdAt, day.zone), style = MaterialTheme.typography.labelMedium, color = Muted)
                    }
                }
            }
        }
        if (status == MemoryStatus.READY) {
            items(articles.withIndex().toList(), key = { "article-${it.index}" }) { (_, article) ->
                MemoryArticleCard(article)
            }
        } else item(key = "status") {
            MemoryStatusCard(status, day, busy, lastReady, retry, openDay, settings, selectDate)
        }
        if (day.hasData) item(key = "records") {
            MemorySection(MemoryBlue, Modifier.testTag("memory-records")) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = openDay).heightIn(min = 48.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(WellfitIcons.Today, null, tint = MemoryBlueInk)
                    Text("Günün kayıtları", Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                    Icon(WellfitIcons.Arrow, "Günün kaydını aç", Modifier.size(20.dp), tint = MemoryBlueInk)
                }
                val facts = buildList {
                    if (day.meals.isNotEmpty()) add(MemoryFact(number(day.calories), "kcal", WellfitIcons.Nutrition))
                    day.steps?.let { add(MemoryFact(number(it), "adım", WellfitIcons.Steps)) }
                    day.weight?.let { add(MemoryFact(number(it), "kg", WellfitIcons.Weight)) }
                    if (day.meals.isNotEmpty()) add(MemoryFact(day.meals.size.toString(), "öğün", WellfitIcons.Meals))
                    if (day.workouts.isNotEmpty()) add(MemoryFact("${day.workouts.sumOf { it.minutes }} dk", "${day.workouts.size} seans", WellfitIcons.Activity))
                    else if (day.workoutStatus == "rest") add(MemoryFact("Dinlenme", "Antrenman", WellfitIcons.Rest))
                }
                if (facts.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    facts.forEach { fact ->
                        Column(Modifier.widthIn(min = 96.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(fact.icon, null, Modifier.size(19.dp), tint = MemoryBlueInk)
                            Text(fact.value, style = MaterialTheme.typography.titleLarge)
                            Text(fact.label, style = MaterialTheme.typography.labelMedium, color = MemoryBlueInk)
                        }
                    }
                }
                if (day.meals.isNotEmpty()) {
                    HorizontalDivider(color = MemoryBlueInk.copy(alpha = .12f))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Protein ${number(day.protein)} g", style = MaterialTheme.typography.bodySmall, color = MemoryBlueInk)
                        day.carbs?.let { Text("Karb. ${number(it)} g", style = MaterialTheme.typography.bodySmall, color = MemoryBlueInk) }
                        day.fat?.let { Text("Yağ ${number(it)} g", style = MaterialTheme.typography.bodySmall, color = MemoryBlueInk) }
                    }
                }
                if (day.note.isNotBlank()) MemoryDisclosure("Gün notu", WellfitIcons.Note, MemoryBlueInk, "day-note-${day.date}") { Text(day.note, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        if (archived.isNotEmpty()) item(key = "archive") {
            MemorySection(MemoryViolet, Modifier.testTag("memory-archive")) {
                MemoryHeading("Önceki değerlendirmeler", WellfitIcons.History, MemoryVioletInk)
                archived.forEach { old ->
                    MemoryDisclosure(memoryTimestamp(old.createdAt, day.zone), WellfitIcons.Review, MemoryVioletInk, "archive-${old.id}") {
                        Text("AI · ${old.model}", style = MaterialTheme.typography.labelMedium, color = MemoryVioletInk)
                        memoryArticles(old.text).forEach { article ->
                            Text(article.title, style = MaterialTheme.typography.titleMedium)
                            MemoryBlocks(article.blocks, MemoryVioletInk)
                        }
                    }
                }
            }
        }
        if (state.profile.memory.isNotBlank()) item(key = "profile-note") {
            MemorySection(MemorySand, Modifier.testTag("memory-profile-note")) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(WellfitIcons.Bookmark, null, tint = Color(0xFF85603E))
                    Text("AI için notların", Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                    IconButton(settings, enabled = !busy) { Icon(WellfitIcons.Edit, "AI notlarını düzenle", Modifier.size(19.dp), tint = Color(0xFF85603E)) }
                }
                var expanded by rememberSaveable { mutableStateOf(false) }
                var overflowing by remember(state.profile.memory) { mutableStateOf(false) }
                SelectionContainer { Text(state.profile.memory, maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, onTextLayout = { if (!expanded) overflowing = it.hasVisualOverflow }) }
                if (expanded || overflowing) TextButton({ expanded = !expanded }, contentPadding = PaddingValues(horizontal = 0.dp)) { Text(if (expanded) "Kısalt" else "Tümünü oku") }
            }
        }
        if (recap?.snapshotGzip?.isNotBlank() == true) item(key = "audit") {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).clickable(enabled = !busy, onClick = prompt).heightIn(min = 56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(WellfitIcons.Shield, null, Modifier.size(20.dp), tint = Forest)
                Text("AI'ya gönderilen veri", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                Icon(WellfitIcons.Right, "AI'ya gönderilen veriyi incele", Modifier.size(18.dp), tint = Forest)
            }
        }
    }
    if (historyOpen) ModalBottomSheet(onDismissRequest = { historyOpen = false }, containerColor = CanvasColor) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 540.dp).testTag("memory-history-list"), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("Değerlendirmeler", Modifier.padding(bottom = 6.dp), style = MaterialTheme.typography.titleLarge) }
            val choices = (history + state.day(today)).distinctBy { it.date }.sortedByDescending { it.date }
            items(choices, key = { it.date }) { choice ->
                val choiceStatus = memoryStatus(choice, state.profile, hasAiKey)
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (choice.date == day.date) MemoryGreen else Color.White).clickable { selectDate(choice.date); historyOpen = false }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(if (choiceStatus == MemoryStatus.READY) WellfitIcons.Review else WellfitIcons.Clock, null, Modifier.size(22.dp), tint = Forest)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(LocalDate.parse(choice.date).format(DateTimeFormatter.ofPattern("d MMMM yyyy", Turkish)), style = MaterialTheme.typography.titleMedium)
                        Text(choiceStatus.label, style = MaterialTheme.typography.labelMedium, color = Muted)
                    }
                    if (choice.date == day.date) Icon(WellfitIcons.Check, null, Modifier.size(19.dp), tint = Forest)
                }
            }
        }
    }
}

@Composable private fun MemorySection(color: Color, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(color).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}
@Composable private fun MemoryHeading(title: String, icon: ImageVector, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(23.dp), tint = color)
        Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge)
    }
}
@Composable private fun MemoryArticleCard(article: MemoryArticle) {
    val (surface, color, icon) = when (article.tone) {
        MemoryTone.DAY -> Triple(MemoryGreen, Forest, WellfitIcons.Review)
        MemoryTone.WEEK -> Triple(MemoryBlue, MemoryBlueInk, WellfitIcons.Week)
        MemoryTone.MONTH -> Triple(MemorySand, Color(0xFF85603E), WellfitIcons.Calendar)
        MemoryTone.NEXT -> Triple(MemoryViolet, MemoryVioletInk, WellfitIcons.Arrow)
    }
    MemorySection(surface, Modifier.testTag("memory-article-${article.tone.name.lowercase()}")) {
        MemoryHeading(article.title, icon, color)
        MemoryBlocks(article.blocks, color)
    }
}
@Composable private fun MemoryBlocks(blocks: List<MemoryBlock>, color: Color) {
    SelectionContainer {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            blocks.forEach { block ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    block.marker?.let { Text(it, Modifier.widthIn(min = 16.dp), color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold) }
                    Text(memoryInlineText(block.text), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
private fun memoryInlineText(text: String) = buildAnnotatedString {
    val matches = Regex("\\*\\*(.+?)\\*\\*|__(.+?)__|`([^`]+)`").findAll(text)
    var cursor = 0
    matches.forEach { match ->
        append(text.substring(cursor, match.range.first))
        val start = length
        append(match.groups[1]?.value ?: match.groups[2]?.value ?: match.groups[3]!!.value)
        addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), start, length)
        cursor = match.range.last + 1
    }
    append(text.substring(cursor))
}
@Composable private fun MemoryDisclosure(title: String, icon: ImageVector, color: Color, key: String, content: @Composable ColumnScope.() -> Unit) {
    var expanded by rememberSaveable(key) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .55f)).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.heightIn(min = 52.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(19.dp), tint = color)
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            Icon(if (expanded) WellfitIcons.Up else WellfitIcons.Down, if (expanded) "$title gizle" else "$title göster", Modifier.size(17.dp), tint = color)
        }
        if (expanded) { content(); Spacer(Modifier.height(4.dp)) }
    }
}
private data class MemoryFact(val value: String, val label: String, val icon: ImageVector)

@Composable private fun MemoryStatusCard(status: MemoryStatus, day: Day, busy: Boolean, lastReady: Day?, retry: () -> Unit, openDay: () -> Unit, settings: () -> Unit, selectDate: (String) -> Unit) {
    val failed = status == MemoryStatus.FAILED
    val color = if (failed) Color(0xFF90553B) else Forest
    MemorySection(if (failed) MemorySand else Color.White, Modifier.testTag("memory-status-${status.name.lowercase()}")) {
        Icon(when (status) { MemoryStatus.RUNNING, MemoryStatus.WAITING -> WellfitIcons.Clock; MemoryStatus.OFF, MemoryStatus.KEY_REQUIRED -> WellfitIcons.Ai; MemoryStatus.FAILED -> WellfitIcons.Alert; else -> WellfitIcons.Library }, null, Modifier.size(28.dp), tint = color)
        Text(when (status) {
            MemoryStatus.OPEN -> if (day.hasData) "Bu gün henüz tamamlanmadı" else "Henüz değerlendirme yok"
            MemoryStatus.OFF -> "AI değerlendirmesi kapalı"
            MemoryStatus.KEY_REQUIRED -> "DeepSeek bağlantısı gerekli"
            MemoryStatus.WAITING -> if (day.recap?.error.isNullOrBlank()) "Değerlendirme sırada" else "Bağlantı bekleniyor"
            MemoryStatus.RUNNING -> "Değerlendirme hazırlanıyor"
            MemoryStatus.FAILED -> "Değerlendirme hazırlanamadı"
            else -> "Değerlendirme bulunamadı"
        }, style = MaterialTheme.typography.titleLarge)
        if (status == MemoryStatus.RUNNING) LinearProgressIndicator(Modifier.fillMaxWidth().height(4.dp), color = Forest, trackColor = MemoryGreen)
        if (failed && !day.recap?.error.isNullOrBlank()) Text(day.recap!!.error, style = MaterialTheme.typography.bodyMedium, color = color)
        when (status) {
            MemoryStatus.OPEN, MemoryStatus.MISSING -> {
                if (lastReady != null) MemoryButton("Son değerlendirmeyi aç", WellfitIcons.Review, !busy) { selectDate(lastReady.date) }
                OutlinedButton(openDay, enabled = !busy, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Forest.copy(alpha = .35f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Forest)) { Text("Günün kaydını aç"); Spacer(Modifier.width(8.dp)); Icon(WellfitIcons.Arrow, null, Modifier.size(17.dp)) }
            }
            MemoryStatus.OFF, MemoryStatus.KEY_REQUIRED -> MemoryButton("AI ayarlarını aç", WellfitIcons.Settings, !busy, settings)
            MemoryStatus.FAILED -> MemoryButton("Yeniden dene", WellfitIcons.Sync, !busy, retry)
            MemoryStatus.WAITING -> if (!day.recap?.error.isNullOrBlank()) {
                TextButton(retry, enabled = !busy, contentPadding = PaddingValues(horizontal = 0.dp)) { Text("Yeniden dene") }
            }
            else -> Unit
        }
    }
}
@Composable private fun MemoryButton(label: String, icon: ImageVector, enabled: Boolean, action: () -> Unit) {
    Button(action, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) { Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(label) }
}
