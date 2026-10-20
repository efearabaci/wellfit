package com.denge.app.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
val Tabs = listOf(Tab("Bugün", WellfitIcons.Today), Tab("Gidişat", WellfitIcons.Chart), Tab("Hafıza", WellfitIcons.Memory))

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun DengeApp(vm: DengeViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val error by vm.loadError.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val keys by vm.keysRevision.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var dateString by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var lastToday by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val date = LocalDate.parse(dateString)
    var modal by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedMealId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedWorkoutId by rememberSaveable { mutableStateOf<String?>(null) }
    var restoreCandidate by remember { mutableStateOf<AppState?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    // Settings used to be the fourth bottom tab; older saved state can still carry that index.
    val currentTab = tab.coerceIn(0, Tabs.lastIndex)
    val ready = state != null && state!!.profile.onboarded
    val visibleDay = state?.day(date)
    val canAddMeal = ready && !settingsOpen && currentTab == 0 && modal == null && visibleDay != null && !visibleDay.closed
    BackHandler(enabled = settingsOpen) { settingsOpen = false }
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
    Scaffold(
        containerColor = CanvasColor,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (ready) TopAppBar(
                navigationIcon = {
                    if (settingsOpen) IconButton({ settingsOpen = false }) { Icon(WellfitIcons.Back, "Geri") }
                },
                title = {
                    if (settingsOpen) Text("Ayarlar", style = MaterialTheme.typography.titleLarge)
                    else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(32.dp).background(Forest, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(WellfitIcons.Brand, null, tint = Color.White, modifier = Modifier.size(23.dp)) }
                        Text("Wellfit", fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
                    }
                },
                actions = {
                    if (!settingsOpen && currentTab == 0 && visibleDay != null) {
                        Tag(if (visibleDay.closed) "Gün kapalı" else "Gün açık", Forest, Color(0xFFE5ECE6))
                    }
                    if (!settingsOpen) {
                        IconButton({ settingsOpen = true }) { Icon(WellfitIcons.Settings, "Ayarlar", Modifier.size(23.dp)) }
                        Spacer(Modifier.width(4.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CanvasColor)
            )
        },
        bottomBar = {
            if (ready && !settingsOpen) Column {
                HorizontalDivider(color = Line)
                NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                Tabs.forEachIndexed { index, item -> NavigationBarItem(selected = currentTab == index, onClick = { tab = index }, icon = { Icon(item.icon, item.title) }, label = { Text(item.title) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Forest, selectedTextColor = Forest, indicatorColor = Sage, unselectedIconColor = Muted, unselectedTextColor = Muted)) }
                }
            }
        },
        floatingActionButton = {
            if (canAddMeal) ExtendedFloatingActionButton(
                onClick = { selectedMealId = null; vm.resetAiMeal(); modal = "meal" },
                icon = { Icon(WellfitIcons.Add, null, Modifier.size(20.dp)) },
                text = { Text("Öğün ekle", style = MaterialTheme.typography.labelLarge) },
                modifier = Modifier.testTag("meal-fab"),
                containerColor = Forest,
                contentColor = Color.White,
                shape = RoundedCornerShape(14.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp, pressedElevation = 6.dp)
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        when {
            error != null -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center) {
                EmptyState("Kayıtlar okunamadı", error!!, WellfitIcons.Shield)
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
                    if (settingsOpen) SettingsScreen(s, busy, keys, vm, onHealth = {
                        if (vm.graph.steps.available()) health.launch(vm.graph.steps.permissions)
                        else vm.events.tryEmit("Health Connect bu telefonda yok veya güncellenmesi gerekiyor. Adımları manuel girebilirsin.")
                    }, onExport = { export.launch("wellfit-yedek-${LocalDate.now()}.json") }, onImport = { restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, onPrivacy = { context.startActivity(Intent(context, PrivacyActivity::class.java)) })
                    else when (currentTab) {
                        0 -> TodayScreen(s, d, date, busy, onPrevious = { dateString = date.minusDays(1).toString() }, onNext = { if (date < LocalDate.now()) dateString = date.plusDays(1).toString() }, onToday = { dateString = LocalDate.now().toString() },
                            addMeal = { selectedMealId = null; vm.resetAiMeal(); modal = "meal" }, editMeal = { selectedMealId = it.id; modal = "meal" },
                            metrics = { modal = "metrics" }, sync = { vm.syncSteps(date, true) }, note = { modal = "note" },
                            workout = { selectedWorkoutId = null; modal = "workout" }, editWorkout = { selectedWorkoutId = it.id; modal = "workout" },
                            hevy = { vm.hevy(date) }, rest = { vm.rest(date) }, close = { modal = "close" }, reopen = { modal = "reopen" }, memory = { tab = 2 },
                            fabClearance = canAddMeal)
                        1 -> ProgressScreen(s) { dateString = it; tab = 0 }
                        else -> MemoryScreen(s, date, busy, { vm.retry(date) }, { modal = "prompt" }, { markdown.launch("wellfit-hafiza-${LocalDate.now()}.md") }, { dateString = it })
                    }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter), color = Forest, trackColor = Sage)
                }
                if (!settingsOpen) when (modal) {
                    "meal" -> MealEditor(selectedMeal, busy, { modal = null }, { vm.meal(date, it) { modal = null } }, { selectedMeal?.let { vm.deleteMeal(date, it.id) { modal = null } } }, onAi = if (selectedMeal == null) ({ modal = "ai-meal" }) else null)
                    "ai-meal" -> AiMealEditor(vm, date, busy, dismiss = { vm.cancelAiMeal(); modal = null }, manual = { vm.cancelAiMeal(); modal = "meal" }, settings = { vm.cancelAiMeal(); modal = null; settingsOpen = true }, saved = { modal = null })
                    "metrics" -> MetricsEditor(d, busy, { modal = null }) { steps, weight -> vm.metrics(date, steps, weight) { modal = null } }
                    "note" -> NoteEditor(d.note, busy, { modal = null }) { vm.note(date, it) { modal = null } }
                    "workout" -> WorkoutEditor(selectedWorkout, busy, { modal = null }, { vm.workout(date, it) { modal = null } }, { selectedWorkout?.let { vm.deleteWorkout(date, it.id) { modal = null } } })
                    "close" -> AlertDialog(onDismissRequest = { if (!busy) modal = null }, icon = { Icon(WellfitIcons.Complete, null) }, title = { Text("Günü kapat") }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
