package com.denge.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.denge.app.domain.*

private val GoalsSaver = listSaver<Goals, Int>(save = { listOf(it.calories, it.protein, it.carbs, it.fat, it.steps) }, restore = { Goals(it[0], it[1], it[2], it[3], it[4]) })
@Composable fun GoalFields(goals: Goals, onChange: (Goals) -> Unit) {
    // Keep invalid intermediate text local so deleting a field never re-inserts its old value.
    var calories by rememberSaveable { mutableStateOf(goals.calories.toString()) }
    var protein by rememberSaveable { mutableStateOf(goals.protein.toString()) }
    var carbs by rememberSaveable { mutableStateOf(goals.carbs.toString()) }
    var fat by rememberSaveable { mutableStateOf(goals.fat.toString()) }
    var steps by rememberSaveable { mutableStateOf(goals.steps.toString()) }
    fun emit() = onChange(Goals(calories.toIntOrNull() ?: -1, protein.toIntOrNull() ?: -1, carbs.toIntOrNull() ?: -1, fat.toIntOrNull() ?: -1, steps.toIntOrNull() ?: -1))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FormField(calories, { calories = it; emit() }, "Kalori · kcal", true, Modifier.weight(1f), max = 5)
        FormField(steps, { steps = it; emit() }, "Adım", true, Modifier.weight(1f), max = 6)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FormField(protein, { protein = it; emit() }, "Protein · g", true, Modifier.weight(1f), max = 4)
        FormField(carbs, { carbs = it; emit() }, "Karb. · g", true, Modifier.weight(1f), max = 4)
        FormField(fat, { fat = it; emit() }, "Yağ · g", true, Modifier.weight(1f), max = 4)
    }
}
@Composable fun Onboarding(profile: Profile, busy: Boolean, save: (Profile) -> Unit) {
    var name by rememberSaveable { mutableStateOf(profile.name) }
    var goals by rememberSaveable(stateSaver = GoalsSaver) { mutableStateOf(profile.goals) }
    var target by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize().safeDrawingPadding(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { Spacer(Modifier.height(20.dp)); IconTile(Icons.Outlined.Spa, size = 64); Spacer(Modifier.height(26.dp)); Text("Biraz daha iyi,\nher gün.", style = MaterialTheme.typography.headlineLarge); Spacer(Modifier.height(12.dp)); Text("Beslenmen, hareketin ve sana özel günlük özetin. Kendi ritmini bulacağın sakin bir alan.", color = Muted, style = MaterialTheme.typography.bodyLarge) }
        item { Panel {
            Text("Önce seni tanıyalım", style = MaterialTheme.typography.titleLarge)
            FormField(name, { name = it }, "Sana nasıl hitap edelim?", max = 100)
            FormField(target, { target = it }, "Hedef kilo · kg (isteğe bağlı)", true, max = 6)
        } }
        item { Panel {
            Text("Günlük hedeflerin", style = MaterialTheme.typography.titleLarge)
            Text("Bunlar başlangıç değerleri. Kendi planına göre düzenleyebilir, daha sonra değiştirebilirsin.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            GoalFields(goals) { goals = it }
        } }
        item {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            PrimaryButton("Kendi dengemi kur", Icons.Outlined.ArrowForward, !busy) {
                runCatching { goals.validate(); require(target.isBlank() || target.decimal()?.let { it in 20.0..500.0 } == true) { "Hedef kiloyu kontrol et." } }
                    .onSuccess { save(profile.copy(name = name.trim(), goals = goals, targetWeight = target.decimal())) }.onFailure { error = it.message }
            }
            Text("Kayıtların telefonunda. AI bağlantısını istediğin zaman Ayarlar'dan açabilirsin.", Modifier.padding(top = 14.dp), style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}
@Composable fun SettingsScreen(state: AppState, busy: Boolean, keysRevision: Int, vm: DengeViewModel, onHealth: () -> Unit, onExport: () -> Unit, onImport: () -> Unit, onPrivacy: () -> Unit) {
    val p = state.profile
    var name by rememberSaveable(p.name) { mutableStateOf(p.name) }
    var goals by rememberSaveable(p.goals, stateSaver = GoalsSaver) { mutableStateOf(p.goals) }
    var target by rememberSaveable(p.targetWeight) { mutableStateOf(p.targetWeight?.toString().orEmpty()) }
    var memory by rememberSaveable(p.memory) { mutableStateOf(p.memory) }
    var model by rememberSaveable(p.model) { mutableStateOf(p.model) }
    var consent by rememberSaveable(p.aiConsent) { mutableStateOf(p.aiConsent) }
    var healthEnabled by rememberSaveable(p.healthEnabled) { mutableStateOf(p.healthEnabled) }
    // Secrets intentionally are not saved in instance state.
    var deepseek by remember { mutableStateOf("") }
    var hevy by remember { mutableStateOf("") }
    val hasDeepseek = remember(keysRevision) { vm.graph.secrets.has("deepseek") }
    val hasHevy = remember(keysRevision) { vm.graph.secrets.has("hevy") }
    var error by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item { Text("Senin düzenin.", style = MaterialTheme.typography.headlineLarge); Text("Hedefler, bağlantılar ve verilerin.", color = Muted) }
        item { Panel {
            Section("Profil ve hedefler")
            FormField(name, { name = it }, "Adın", max = 100)
            FormField(target, { target = it }, "Hedef kilo · kg", true, max = 6)
            GoalFields(goals) { goals = it }
            Text("Hedef değişiklikleri bugünün açık kaydına ve yeni günlere uygulanır. Kapalı günlerin hedefleri korunur.", style = MaterialTheme.typography.bodySmall, color = Muted)
        } }
        item { Panel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { IconTile(Icons.Outlined.AutoAwesome); Column { Text("DeepSeek", style = MaterialTheme.typography.titleLarge); Text(if (hasDeepseek) "Anahtar güvenli olarak kayıtlı" else "Gününü anlayan bir özet", color = Muted, style = MaterialTheme.typography.bodySmall) } }
            OutlinedTextField(deepseek, { if (it.length <= 512) deepseek = it }, Modifier.fillMaxWidth(), label = { Text(if (hasDeepseek) "Yeni API anahtarı (isteğe bağlı)" else "DeepSeek API anahtarı") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(15.dp))
            FormField(model, { model = it }, "Model", max = 100)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("AI ile günlük değerlendirme", style = MaterialTheme.typography.titleMedium); Text("Günü kapatırken profil notların ve o tarihe kadar tüm kayıtların DeepSeek'e gönderilir.", style = MaterialTheme.typography.bodySmall, color = Muted) }
                Switch(consent, { consent = it })
            }
            Text("Ücretlendirme kendi DeepSeek hesabından yapılır. Anahtarlar yedeklere eklenmez.", style = MaterialTheme.typography.bodySmall, color = Muted)
            if (hasDeepseek) TextButton({ vm.removeKey("deepseek") }, enabled = !busy) { Text("DeepSeek anahtarını kaldır") }
        } }
        item { Panel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { IconTile(Icons.Outlined.FitnessCenter, color = Color(0xFF817297), bg = Color(0xFFEEEAF4)); Column { Text("Hevy", style = MaterialTheme.typography.titleLarge); Text(if (hasHevy) "Anahtar kayıtlı" else "Hareketlerin ve setlerin bir arada", color = Muted, style = MaterialTheme.typography.bodySmall) } }
            Text("Hevy Pro hesabındaki hevy.com/settings?developer sayfasından API anahtarı al. Günlük ekranda 'Hevy'den al'a dokun.", style = MaterialTheme.typography.bodyMedium, color = Muted)
            OutlinedTextField(hevy, { if (it.length <= 512) hevy = it }, Modifier.fillMaxWidth(), label = { Text(if (hasHevy) "Yeni Hevy anahtarı (isteğe bağlı)" else "Hevy API anahtarı") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(15.dp))
            if (hasHevy) TextButton({ vm.removeKey("hevy") }, enabled = !busy) { Text("Hevy anahtarını kaldır") }
        } }
        item { Panel {
            Section("Adımların", "Health Connect veya manuel giriş")
            Text("Huawei Sağlık verisi Health Connect'e aktarılıyorsa günlük toplamı okuyabiliriz. Huawei'ye doğrudan bağlantı kurulmaz. Aktarım yoksa adımları Huawei Sağlık'tan kendin girebilirsin.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Otomatik güncelle", style = MaterialTheme.typography.titleMedium); Text("Uygulama açıldığında. Manuel giriş korunur.", color = Muted, style = MaterialTheme.typography.bodySmall) }; Switch(healthEnabled, { healthEnabled = it }) }
            OutlinedButton(onHealth, Modifier.fillMaxWidth(), enabled = !busy) { Icon(Icons.Outlined.Link, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Health Connect iznini ver") }
        } }
        item { Panel {
            Section("AI'nın hatırlayacakları")
            Text("Tercihlerin, rutinlerin, bilmesini istediğin koşullar. Bu notlar her özete eklenir.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            FormField(memory, { memory = it }, "Örneğin: Haftada 3 gün kuvvet çalışıyorum…", max = 20000, lines = 4)
        } }
        item {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            PrimaryButton("Ayarları kaydet", Icons.Outlined.Check, !busy) {
                runCatching { goals.validate(); require(target.isBlank() || target.decimal()?.let { it in 20.0..500.0 } == true) { "Hedef kiloyu kontrol et." }; require(model.matches(Regex("[a-zA-Z0-9._:-]{1,100}"))) { "Geçerli bir model adı gir." } }
                    .onSuccess { vm.profile(p.copy(name = name.trim(), goals = goals, targetWeight = target.decimal(), memory = memory.trim(), model = model.trim(), aiConsent = consent, healthEnabled = healthEnabled), deepseek.trim(), hevy.trim()) { deepseek = ""; hevy = "" } }
                    .onFailure { error = it.message }
            }
        }
        item { Panel(color = Color(0xFFEDF1E6)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Shield, null, tint = Forest); Text("Hafızanı yanında tut", style = MaterialTheme.typography.titleLarge) }
            Text("Kayıtlar telefonda saklanır ve her değişiklikte yerel kurtarma kopyası yazılır. Telefon kaybına ve uygulamayı kaldırmaya karşı JSON yedeğini farklı bir konuma kaydet.", style = MaterialTheme.typography.bodyMedium, color = Muted)
            Text(state.lastExportAt?.let { "Son dışa aktarım: ${it.take(10)}" } ?: "Henüz dışa aktarılmış yedek yok", style = MaterialTheme.typography.bodySmall, color = Forest)
            PrimaryButton("JSON yedeği kaydet", Icons.Outlined.FileDownload, !busy, onExport)
            OutlinedButton(onImport, Modifier.fillMaxWidth(), enabled = !busy) { Text("Yedekten içe aktar") }
        } }
        item { TextButton(onPrivacy, Modifier.fillMaxWidth()) { Text("Gizlilik ve veri kullanımı") }; Text("DENGE  /  1.0.0\nKüçük adımlar. Kalıcı alışkanlıklar.", Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Muted, style = MaterialTheme.typography.bodySmall) }
    }
}
