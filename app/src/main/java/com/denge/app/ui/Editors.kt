package com.denge.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.denge.app.domain.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

fun String.decimal(): Double? = replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
@Composable fun FormField(value: String, onChange: (String) -> Unit, label: String, numeric: Boolean = false, modifier: Modifier = Modifier, max: Int = 200, lines: Int = 1) {
    OutlinedTextField(value, { if (it.length <= max) onChange(it) }, modifier.fillMaxWidth(), label = { Text(label) }, singleLine = lines == 1, minLines = lines, shape = RoundedCornerShape(15.dp), keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text))
}
@Composable fun Editor(title: String, busy: Boolean, dismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = { if (!busy) dismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 780.dp).imePadding(), shape = RoundedCornerShape(28.dp), color = CanvasColor) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(dismiss, enabled = !busy) { Icon(Icons.Outlined.Close, "Kapat") }
                }
                content()
            }
        }
    }
}
@Composable fun MealEditor(meal: Meal?, busy: Boolean, dismiss: () -> Unit, save: (Meal) -> Unit, delete: () -> Unit) {
    var name by rememberSaveable(meal?.id) { mutableStateOf(meal?.name.orEmpty()) }
    var category by rememberSaveable(meal?.id) { mutableStateOf(meal?.category ?: "Öğle") }
    var time by rememberSaveable(meal?.id) { mutableStateOf(meal?.time ?: LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))) }
    var calories by rememberSaveable(meal?.id) { mutableStateOf(meal?.calories?.toString().orEmpty()) }
    var protein by rememberSaveable(meal?.id) { mutableStateOf(meal?.protein?.toString().orEmpty()) }
    var carbs by rememberSaveable(meal?.id) { mutableStateOf(meal?.carbs?.toString().orEmpty()) }
    var fat by rememberSaveable(meal?.id) { mutableStateOf(meal?.fat?.toString().orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    Editor(if (meal == null) "Yeni öğün" else "Öğünü düzenle", busy, dismiss) {
        Text("Hesapladığın değerleri ekle. Hepsi bu.", color = Muted)
        FormField(name, { name = it }, "Ne yedin?")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Kahvaltı", "Öğle", "Akşam", "Ara öğün").forEach { FilterChip(selected = category == it, onClick = { category = it }, label = { Text(it) }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FormField(calories, { calories = it }, "Kalori · kcal", true, Modifier.weight(1f), max = 5)
            FormField(time, { time = it }, "Saat · SS:dd", modifier = Modifier.weight(1f), max = 5)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormField(protein, { protein = it }, "Protein · g", true, Modifier.weight(1f), max = 7)
            FormField(carbs, { carbs = it }, "Karb. · g", true, Modifier.weight(1f), max = 7)
            FormField(fat, { fat = it }, "Yağ · g", true, Modifier.weight(1f), max = 7)
        }
        Text("Makroları gram olarak gir. Ondalık değerlerde virgül veya nokta kullanabilirsin.", style = MaterialTheme.typography.bodySmall, color = Muted)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        PrimaryButton("Öğünü kaydet", Icons.Outlined.Check, !busy) {
            runCatching {
                val c = calories.toIntOrNull() ?: error("Kaloriyi tam sayı olarak gir.")
                val p = protein.decimal() ?: error("Protein miktarını gir.")
                val k = carbs.decimal() ?: error("Karbonhidrat miktarını gir.")
                val f = fat.decimal() ?: error("Yağ miktarını gir.")
                val parsed = LocalTime.parse(time).format(DateTimeFormatter.ofPattern("HH:mm"))
                (meal?.copy(name = name.trim(), category = category, time = parsed, calories = c, protein = p, carbs = k, fat = f)
                    ?: Meal(name = name.trim(), category = category, time = parsed, calories = c, protein = p, carbs = k, fat = f)).also { it.validate() }
            }.onSuccess(save).onFailure { error = if (it is java.time.format.DateTimeParseException) "Saati 14:30 biçiminde gir." else it.message }
        }
        if (meal != null) TextButton({ confirmDelete = true }, Modifier.fillMaxWidth(), enabled = !busy) { Text("Öğünü sil", color = MaterialTheme.colorScheme.error) }
    }
    if (confirmDelete) DeleteDialog("Bu öğün silinsin mi?", { confirmDelete = false }, { confirmDelete = false; delete() })
}
@Composable fun MetricsEditor(day: Day, busy: Boolean, dismiss: () -> Unit, save: (Int?, Double?) -> Unit) {
    var steps by rememberSaveable { mutableStateOf(day.steps?.toString().orEmpty()) }
    var weight by rememberSaveable { mutableStateOf(day.weight?.toString().orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    Editor("Adımların ve kilon", busy, dismiss) {
        Text("Huawei Sağlık'taki günlük toplamı buraya yazabilirsin.", color = Muted)
        FormField(steps, { steps = it }, "Günlük adım", true, max = 6)
        FormField(weight, { weight = it }, "Kilo · kg (isteğe bağlı)", true, max = 6)
        Text("Boş alan 'bilinmiyor' olarak kaydedilir. Manuel adım girişi otomatik eşitlemenin yerine geçer.", style = MaterialTheme.typography.bodySmall, color = Muted)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        PrimaryButton("Kaydet", enabled = !busy) {
            val s = if (steps.isBlank()) null else steps.toIntOrNull()
            val w = if (weight.isBlank()) null else weight.decimal()
            when {
                steps.isNotBlank() && (s == null || s !in 0..200000) -> error = "Adım 0–200.000 arasında tam sayı olmalı."
                weight.isNotBlank() && (w == null || w !in 20.0..500.0) -> error = "Kiloyu 20–500 kg arasında gir."
                else -> save(s, w)
            }
        }
    }
}
@Composable fun NoteEditor(note: String, busy: Boolean, dismiss: () -> Unit, save: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(note) }
    Editor("Gününden bir not", busy, dismiss) {
        Text("Nasıl hissettin? AI özetin bu notu da dikkate alır.", color = Muted)
        FormField(text, { text = it }, "Bugün…", max = 10000, lines = 5)
        PrimaryButton("Notu kaydet", enabled = !busy) { save(text.trim()) }
    }
}
@Composable fun WorkoutEditor(workout: Workout?, busy: Boolean, dismiss: () -> Unit, save: (Workout) -> Unit, delete: () -> Unit) {
    var title by rememberSaveable { mutableStateOf(workout?.title.orEmpty()) }
    var minutes by rememberSaveable { mutableStateOf(workout?.minutes?.toString().orEmpty()) }
    var kind by rememberSaveable { mutableStateOf(workout?.kind ?: "Kuvvet") }
    var notes by rememberSaveable { mutableStateOf(workout?.notes.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val imported = workout?.source == "Hevy"
    Editor(if (imported) "Hevy antrenmanın" else "Antrenman ekle", busy, dismiss) {
        if (imported) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Tag("${minutes} dk · Hevy")
            SelectionContainer { Text(notes, style = MaterialTheme.typography.bodyMedium) }
            Text("Hareketler ve setler Hevy'den alındı. Değişiklikleri Hevy'de yapıp yeniden eşitleyebilirsin.", color = Muted, style = MaterialTheme.typography.bodySmall)
        } else {
            FormField(title, { title = it }, "Antrenman adı", max = 300)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf("Kuvvet", "Kardiyo", "Yürüyüş", "Diğer").forEach { FilterChip(kind == it, { kind = it }, label = { Text(it) }) } }
            FormField(minutes, { minutes = it }, "Süre · dakika", true, max = 4)
            FormField(notes, { notes = it }, "Hareketler, setler veya notlar", max = 20000, lines = 4)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            PrimaryButton("Antrenmanı kaydet", enabled = !busy) {
                val m = minutes.toIntOrNull()
                if (title.isBlank() || m == null || m !in 1..1440) error = "Antrenman adı ve 1–1440 dakika arasında bir süre gir."
                else save(workout?.copy(title = title.trim(), minutes = m, kind = kind, notes = notes.trim()) ?: Workout(title = title.trim(), minutes = m, kind = kind, notes = notes.trim()))
            }
        }
        if (workout != null) TextButton({ confirmDelete = true }, Modifier.fillMaxWidth(), enabled = !busy) { Text("Bu kaydı kaldır", color = MaterialTheme.colorScheme.error) }
    }
    if (confirmDelete) DeleteDialog("Antrenman kaydı kaldırılsın mı?", { confirmDelete = false }, { confirmDelete = false; delete() })
}
@Composable fun DeleteDialog(title: String, dismiss: () -> Unit, delete: () -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { Text("Bu işlem geri alınamaz.") }, confirmButton = { TextButton(delete) { Text("Sil", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(dismiss) { Text("Vazgeç") } })
}
@Composable fun PromptViewer(prompt: String, dismiss: () -> Unit) {
    Dialog(dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().padding(20.dp), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Gönderim kaydı", style = MaterialTheme.typography.titleLarge); IconButton(dismiss) { Icon(Icons.Outlined.Close, "Kapat") } }
                Text("Sistem talimatı ve kapatılan günün değişmez veri kopyası. ${number(prompt.length)} karakter veri.", style = MaterialTheme.typography.bodySmall, color = Muted)
                SelectionContainer { LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { Text(MemoryBuilder.SYSTEM, style = MaterialTheme.typography.bodySmall) }
                    prompt.chunked(2500).forEach { chunk -> item { Text(chunk, style = MaterialTheme.typography.bodySmall) } }
                } }
            }
        }
    }
}
