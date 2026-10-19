package com.denge.app.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.denge.app.PrivacyActivity
import com.denge.app.domain.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val Turkish = Locale.forLanguageTag("tr-TR")
data class Tab(val title: String, val icon: ImageVector)
val Tabs = listOf(Tab("Bugün", Icons.Outlined.GridView), Tab("Gidişat", Icons.Outlined.Insights), Tab("Hafıza", Icons.Outlined.AutoAwesome), Tab("Ayarlar", Icons.Outlined.Tune))

@Composable fun DengeApp(vm: DengeViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val error by vm.loadError.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val keys by vm.keysRevision.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var dateString by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var lastToday by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val date = LocalDate.parse(dateString)
    var modal by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedMealId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedWorkoutId by rememberSaveable { mutableStateOf<String?>(null) }
    var restoreCandidate by remember { mutableStateOf<AppState?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(vm) { vm.events.collect { snackbar.showSnackbar(it) } }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let { vm.export(it) } }
    val markdown = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { it?.let { vm.export(it, true) } }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { uri -> vm.readBackup(uri) { s -> restoreCandidate = s } } }
    val health = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        if (granted.containsAll(vm.graph.steps.permissions)) { vm.healthGranted(); vm.syncSteps(date, true) }
        else vm.events.tryEmit("Adım izni verilmedi. Manuel giriş kullanabilirsin.")
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val today = LocalDate.now().toString()
        if (dateString == lastToday && lastToday != today) dateString = today
        lastToday = today
        vm.syncSteps(LocalDate.parse(dateString))
    }
    LaunchedEffect(dateString, state?.profile?.healthEnabled) { if (state != null) vm.syncSteps(date) }
    Scaffold(containerColor = CanvasColor, snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
        if (state != null && state!!.profile.onboarded) NavigationBar(containerColor = CanvasColor, tonalElevation = 0.dp) {
            Tabs.forEachIndexed { index, item -> NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = { Icon(item.icon, item.title) }, label = { Text(item.title) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Forest, selectedTextColor = Forest, indicatorColor = Sage, unselectedIconColor = Muted, unselectedTextColor = Muted)) }
        }
    }) { padding ->
        when {
            error != null -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center) {
                EmptyState("Kayıtları koruyalım", error!!, Icons.Outlined.Shield)
                PrimaryButton("JSON yedeğinden içe aktar") { restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
                TextButton(onClick = { vm.recoverLocal() }) { Text("Telefondaki kurtarma kopyasını dene") }
            }
            state == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Forest) }
            !state!!.profile.onboarded -> Onboarding(state!!.profile, busy) { vm.profile(it.copy(onboarded = true), "", "") }
            else -> {
                val s = state!!; val d = s.day(date)
                val selectedMeal = d.meals.firstOrNull { it.id == selectedMealId }
                val selectedWorkout = d.workouts.firstOrNull { it.id == selectedWorkoutId }
                Box(Modifier.fillMaxSize().padding(padding)) {
                    when (tab) {
                        0 -> TodayScreen(s, d, date, busy, onPrevious = { dateString = date.minusDays(1).toString() }, onNext = { if (date < LocalDate.now()) dateString = date.plusDays(1).toString() }, onToday = { dateString = LocalDate.now().toString() },
                            addMeal = { selectedMealId = null; modal = "meal" }, editMeal = { selectedMealId = it.id; modal = "meal" },
                            metrics = { modal = "metrics" }, sync = { vm.syncSteps(date, true) }, note = { modal = "note" },
                            workout = { selectedWorkoutId = null; modal = "workout" }, editWorkout = { selectedWorkoutId = it.id; modal = "workout" },
                            hevy = { vm.hevy(date) }, rest = { vm.rest(date) }, close = { modal = "close" }, reopen = { modal = "reopen" }, memory = { tab = 2 })
                        1 -> ProgressScreen(s) { dateString = it; tab = 0 }
                        2 -> MemoryScreen(s, date, busy, { vm.retry(date) }, { modal = "prompt" }, { markdown.launch("denge-hafiza-${LocalDate.now()}.md") }, { dateString = it })
                        3 -> SettingsScreen(s, busy, keys, vm, onHealth = {
                            if (vm.graph.steps.available()) health.launch(vm.graph.steps.permissions)
                            else vm.events.tryEmit("Health Connect bu telefonda yok veya güncellenmesi gerekiyor. Adımları manuel girebilirsin.")
                        }, onExport = { export.launch("denge-yedek-${LocalDate.now()}.json") }, onImport = { restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, onPrivacy = { context.startActivity(Intent(context, PrivacyActivity::class.java)) })
                    }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter), color = Forest, trackColor = Sage)
                }
                when (modal) {
                    "meal" -> MealEditor(selectedMeal, busy, { modal = null }, { vm.meal(date, it) { modal = null } }, { selectedMeal?.let { vm.deleteMeal(date, it.id) { modal = null } } })
                    "metrics" -> MetricsEditor(d, busy, { modal = null }) { steps, weight -> vm.metrics(date, steps, weight) { modal = null } }
                    "note" -> NoteEditor(d.note, busy, { modal = null }) { vm.note(date, it) { modal = null } }
                    "workout" -> WorkoutEditor(selectedWorkout, busy, { modal = null }, { vm.workout(date, it) { modal = null } }, { selectedWorkout?.let { vm.deleteWorkout(date, it.id) { modal = null } } })
                    "close" -> AlertDialog(onDismissRequest = { if (!busy) modal = null }, icon = { Icon(Icons.Outlined.Nightlight, null) }, title = { Text("Bugünlük bu kadar mı?") }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${d.meals.size} öğün · ${number(d.calories)} kcal\n${d.steps?.let { "${number(it)} adım" } ?: "Adım girilmedi"} · ${d.workouts.size} antrenman")
                        if (d.workoutStatus == "unknown") Text("Antrenman durumun belirtilmedi. Özette eksik bilgi olarak işaretlenecek.")
                        Text(if (s.profile.aiConsent && vm.graph.secrets.has("deepseek")) "Gün kapanacak; kayıt geçmişin ve kalıcı notların DeepSeek'e gönderilip özetin hazırlanacak." else "Gün kaydedilecek. AI özeti için Ayarlar'dan DeepSeek anahtarını ve paylaşımını etkinleştirebilirsin.")
                        Text("Daha sonra yeniden açıp düzenleyebilirsin.", color = Muted)
                    } }, confirmButton = { Button(enabled = !busy, onClick = { vm.close(date) { modal = null; tab = 2 } }) { Text("Günü kapat") } }, dismissButton = { TextButton(onClick = { modal = null }, enabled = !busy) { Text("Devam et") } })
                    "reopen" -> AlertDialog(onDismissRequest = { modal = null }, title = { Text("Günü yeniden aç") }, text = { Text("Kayıtları düzenleyebilirsin. Mevcut AI özeti arşivlenir; tekrar kapattığında güncel verilerle yeni bir özet hazırlanır.") }, confirmButton = { Button(onClick = { vm.reopen(date) { modal = null } }, enabled = !busy) { Text("Yeniden aç") } }, dismissButton = { TextButton(onClick = { modal = null }) { Text("Vazgeç") } })
                    "prompt" -> PromptViewer(d.recap?.prompt ?: "Bu gün henüz kapatılmadı.") { modal = null }
                }
            }
        }
        restoreCandidate?.let { imported -> AlertDialog(onDismissRequest = { restoreCandidate = null }, title = { Text("Yedeği içe aktar") }, text = { Text("${imported.days.size} günlük kayıt doğrulandı. Telefonda bulunmayan günler eklenecek. Aynı tarihli yerel kayıtlar korunacak. API anahtarları yedeklerde bulunmaz.") }, confirmButton = { Button(enabled = !busy, onClick = { vm.restore(imported) { restoreCandidate = null } }) { Text("Kayıtları birleştir") } }, dismissButton = { TextButton(onClick = { restoreCandidate = null }) { Text("Vazgeç") } }) }
    }
}
