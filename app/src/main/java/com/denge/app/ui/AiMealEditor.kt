package com.denge.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable fun AiMealEditor(vm: DengeViewModel, date: LocalDate, busy: Boolean, dismiss: () -> Unit, manual: () -> Unit, settings: () -> Unit, saved: () -> Unit) {
    val draft by vm.aiMealDraft.collectAsStateWithLifecycle()
    var category by rememberSaveable { mutableStateOf("Öğle") }
    var time by rememberSaveable { mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    val edit = draft.meals.firstOrNull { it.id == editId }
    if (edit != null) {
        MealEditor(edit, busy, { editId = null }, { vm.editAiMeal(it); editId = null }, { vm.removeAiMeal(edit.id); editId = null })
        return
    }
    Editor("Yediklerini anlat", busy, dismiss) {
        Text(date.format(DateTimeFormatter.ofPattern("d MMMM EEEE", Turkish)), color = Forest, style = MaterialTheme.typography.labelLarge)
        if (draft.meals.isEmpty()) Text("Yemekleri, miktarları ve biliyorsan besin değerlerini yaz. Birden fazla öğünü de anlatabilirsin.", color = Muted)
        if (draft.loading) {
            Text(draft.text)
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = Forest)
            Text("Öğünlerin hazırlanıyor… Henüz günlüğüne eklenmedi.", color = Muted)
            TextButton({ vm.cancelAiMeal() }) { Text("İsteği iptal et") }
        } else {
            if (draft.meals.isEmpty()) {
                FormField(draft.text, vm::aiMealText, "Ne yedin, ne kadar?", max = 4000, lines = 4)
                Text("Örnek: Öğlen 150 g tavuk, 1 tabak pilav ve 1 bardak ayran yedim.", color = Muted, style = MaterialTheme.typography.bodySmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Kahvaltı", "Öğle", "Akşam", "Ara öğün").forEach { label -> FilterChip(category == label, { category = label }, label = { Text(label) }) }
                }
                FormField(time, { time = it }, "Saat belirtilmezse · SS:dd", max = 5)
                Text("Hazırla'ya dokunduğunda yalnızca bu metin ve öğün/saat seçimin DeepSeek'e gönderilir. Kayıt geçmişin paylaşılmaz. Kendi API hesabından ücretlendirilir.", color = Muted, style = MaterialTheme.typography.bodySmall)
            } else {
                Text(draft.text, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                TextButton({ vm.aiMealText(draft.text) }, enabled = !busy) { Text("Metni düzenle") }
            }
            if (!vm.graph.secrets.has("deepseek") && draft.meals.isEmpty()) {
                Text("AI ile eklemek için DeepSeek API anahtarını Ayarlar'a kaydet.", color = Forest)
                PrimaryButton("DeepSeek ayarlarını aç", Icons.Outlined.Settings, !busy, settings)
            } else if (draft.meals.isEmpty()) {
                PrimaryButton("Öğünleri hazırla", Icons.Outlined.AutoAwesome, !busy && draft.text.isNotBlank()) { vm.understandMeals(category, time) }
            }
        }
        draft.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (draft.message.isNotBlank()) Text(draft.message, color = Forest)
        if (draft.meals.isNotEmpty()) {
            Text("Kontrol et ve ekle", style = MaterialTheme.typography.titleMedium)
            Text("AI değerleri yaklaşık hesaplayabilir. Porsiyonları ve varsayımları kontrol et; her kaydı düzenleyebilir veya çıkarabilirsin.", color = Muted, style = MaterialTheme.typography.bodySmall)
            draft.meals.forEach { meal -> Panel {
                Text(meal.name, style = MaterialTheme.typography.titleMedium)
                Text("${meal.category} · ${meal.time} · ${number(meal.calories)} kcal", color = Forest)
                Text("P ${number(meal.protein)} · K ${macroNumber(meal.carbs)} · Y ${macroNumber(meal.fat)} g", style = MaterialTheme.typography.bodySmall)
                meal.aiNote?.takeIf { it.isNotBlank() }?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton({ editId = meal.id }, enabled = !busy) { Text("Düzenle") }
                    TextButton({ vm.removeAiMeal(meal.id) }, enabled = !busy) { Text("Çıkar") }
                }
            } }
            PrimaryButton("${draft.meals.size} öğünü günlüğe ekle", Icons.Outlined.Check, !busy) { vm.saveAiMeals(date, saved) }
        }
        TextButton(manual, enabled = !busy) { Text("Değerleri kendim gireceğim") }
    }
}
