package br.com.lottan.nativeapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.lottan.nativeapp.data.Workspace

/** Real defaults: the three goals start disabled, chart scale 30 / 30, both display switches on. */
object GoalDefaults {
    const val DAILY = 0
    const val WEEKLY = 0
    const val CONSISTENCY = 0
    const val CHART_PAGES = 30
    const val CHART_MINUTES = 30
    const val RECORD = true
    const val SHOW_GOALS = true
}

/** Parsed and validated copy of everything the Metas page edits. */
data class GoalsForm(
    val dailyOn: Boolean, val daily: String, val weeklyOn: Boolean, val weekly: String,
    val consistencyOn: Boolean, val consistency: String,
    val scalePages: String, val scaleMinutes: String, val record: Boolean, val showGoals: Boolean,
) {
    val dailyValue get() = if (dailyOn) daily.toIntOrNull() ?: 0 else 0
    val weeklyValue get() = if (weeklyOn) weekly.toIntOrNull() ?: 0 else 0
    val consistencyValue get() = if (consistencyOn) consistency.toIntOrNull() ?: 0 else 0
    val dailyValid get() = !dailyOn || (daily.toIntOrNull() ?: 0) in 1..10000
    val weeklyValid get() = !weeklyOn || (weekly.toIntOrNull() ?: 0) in 1..70000
    val consistencyValid get() = !consistencyOn || (consistency.toIntOrNull() ?: 0) in 1..7
    val pagesValid get() = (scalePages.toIntOrNull() ?: 0) in 1..1000000
    val minutesValid get() = (scaleMinutes.toIntOrNull() ?: 0) in 1..525600
    val valid get() = dailyValid && weeklyValid && consistencyValid && pagesValid && minutesValid

    companion object {
        fun of(daily: Int, weekly: Int, consistency: Int, chartPages: Int, chartMinutes: Int, record: Boolean, showGoals: Boolean) = GoalsForm(
            daily > 0, daily.toString(), weekly > 0, weekly.toString(), consistency > 0, consistency.toString(),
            chartPages.toString(), chartMinutes.toString(), record, showGoals)
        fun defaults() = of(GoalDefaults.DAILY, GoalDefaults.WEEKLY, GoalDefaults.CONSISTENCY, GoalDefaults.CHART_PAGES, GoalDefaults.CHART_MINUTES, GoalDefaults.RECORD, GoalDefaults.SHOW_GOALS)
    }

    /** Two forms are the same when they would store the same values (disabled goals ignore their typed number). */
    fun sameAs(other: GoalsForm) = dailyValue == other.dailyValue && weeklyValue == other.weeklyValue && consistencyValue == other.consistencyValue &&
        scalePages.toIntOrNull() == other.scalePages.toIntOrNull() && scaleMinutes.toIntOrNull() == other.scaleMinutes.toIntOrNull() &&
        record == other.record && showGoals == other.showGoals
}

@Composable
private fun GoalField(label: String, value: String, active: Boolean, maximum: Int, onActive: (Boolean) -> Unit, onValue: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SubtitleText(label, Modifier.weight(1f))
        Switch(active, onActive, Modifier.semanticsLabel(label))
    }
    Spacer(Modifier.height(8.dp))
    SoftField(value, { onValue(it.filter(Char::isDigit).take(5)) }, Modifier.fillMaxWidth(), enabled = active, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        label = { Text(if (active) "Valor" else "Meta desativada") },
        isError = active && ((value.toIntOrNull() ?: 0) !in 1..maximum),
        supportingText = { Text(if (active) "Limite permitido: 1 a ${"%,d".format(maximum).replace(',', '.')}" else "Ative para definir um valor") })
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GoalsScreen(vm: LibraryViewModel, workspace: Workspace, startAtScale: Boolean = false, back: () -> Unit) {
    val prefs by vm.prefs.collectAsState()
    // The stored state is the baseline: "dirty" means the form differs from what is persisted right now.
    val stored = GoalsForm.of(workspace.daily, workspace.weekly, workspace.consistency, prefs.chartPages, prefs.chartMinutes, prefs.recordActivity, prefs.showGoals)
    var dailyOn by rememberSaveable { mutableStateOf(stored.dailyOn) }; var daily by rememberSaveable { mutableStateOf(stored.daily) }
    var weeklyOn by rememberSaveable { mutableStateOf(stored.weeklyOn) }; var weekly by rememberSaveable { mutableStateOf(stored.weekly) }
    var consistencyOn by rememberSaveable { mutableStateOf(stored.consistencyOn) }; var consistency by rememberSaveable { mutableStateOf(stored.consistency) }
    var scalePages by rememberSaveable { mutableStateOf(stored.scalePages) }; var scaleMinutes by rememberSaveable { mutableStateOf(stored.scaleMinutes) }
    var record by rememberSaveable { mutableStateOf(stored.record) }; var showGoals by rememberSaveable { mutableStateOf(stored.showGoals) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var savedNotice by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    val form = GoalsForm(dailyOn, daily, weeklyOn, weekly, consistencyOn, consistency, scalePages, scaleMinutes, record, showGoals)
    val dirty = !form.sameAs(stored)
    val scalePosition = remember { BringIntoViewRequester() }
    LaunchedEffect(startAtScale) { if (startAtScale) { withFrameNanos { }; scalePosition.bringIntoView() } }
    fun tryBack() { if (dirty && !saving) confirmLeave = true else back() }
    BackHandler(dirty) { tryBack() }
    fun edit() { savedNotice = false; error = null }
    Page {
        Top("Metas de leitura", { tryBack() }, {})
        Caption("Configure seu ritmo. Você pode desativar cada meta sem apagar seu histórico.")
        Spacer(Modifier.height(20.dp))
        // Group 1
        SoftCard(Modifier.fillMaxWidth().testTag("goals-group-goals")) {
            CardTitle("Suas metas")
            Spacer(Modifier.height(16.dp))
            GoalField("Páginas por dia", daily, dailyOn, 10000, { dailyOn = it; edit() }, { daily = it; edit() })
            Spacer(Modifier.height(20.dp))
            GoalField("Páginas por semana", weekly, weeklyOn, 70000, { weeklyOn = it; edit() }, { weekly = it; edit() })
            Spacer(Modifier.height(20.dp))
            GoalField("Dias ativos por semana", consistency, consistencyOn, 7, { consistencyOn = it; edit() }, { consistency = it; edit() })
        }
        Spacer(Modifier.height(20.dp))
        // Group 2
        SoftCard(Modifier.fillMaxWidth().testTag("weekly-scale").bringIntoViewRequester(scalePosition)) {
            CardTitle("Escala dos gráficos")
            Caption("A escala controla a apresentação visual do gráfico e não apaga registros que ultrapassem o limite.", Modifier.padding(top = 2.dp, bottom = 16.dp))
            SoftField(scalePages, { scalePages = it.filter(Char::isDigit).take(7); edit() }, Modifier.fillMaxWidth().testTag("scale-pages"), singleLine = true, label = { Text("Limite máximo de páginas") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = !form.pagesValid, supportingText = { Text("Limite permitido: 1 a 1.000.000 · padrão 30") })
            Spacer(Modifier.height(12.dp))
            SoftField(scaleMinutes, { scaleMinutes = it.filter(Char::isDigit).take(6); edit() }, Modifier.fillMaxWidth().testTag("scale-minutes"), singleLine = true, label = { Text("Limite máximo de minutos") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = !form.minutesValid, supportingText = { Text("Limite permitido: 1 a 525.600 · padrão 30") })
        }
        Spacer(Modifier.height(20.dp))
        // Group 3
        SoftCard(Modifier.fillMaxWidth().testTag("goals-group-display")) {
            CardTitle("Registro e exibição")
            Spacer(Modifier.height(8.dp))
            SwitchRow("Registrar novas sessões", "Quando ativado, o ODMANI registra novas atividades de leitura para alimentar suas estatísticas e acompanhar seu progresso.", record, { record = it; edit() })
            Caption("Ao desativar, os registros anteriores são preservados. Novas sessões deixam de ser contabilizadas nas estatísticas.", Modifier.padding(bottom = 12.dp))
            SwitchRow("Mostrar metas na Home", "Exibe os indicadores de metas diárias, semanais e consistência na tela inicial.", showGoals, { showGoals = it; edit() })
            Caption("Desativar esta opção apenas oculta os indicadores da Home. Suas metas e seu histórico continuam preservados.")
        }
        Spacer(Modifier.height(24.dp))
        // Unified save area
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp)) }
        if (savedNotice && !dirty) Caption("Alterações salvas.", Modifier.padding(bottom = 8.dp).testTag("goals-saved"))
        Primary(if (saving) "Salvando…" else "Salvar alterações", {
            saving = true; error = null
            vm.saveGoalsPage(form.dailyValue, form.weeklyValue, form.consistencyValue, form.scalePages.toInt(), form.scaleMinutes.toInt(), form.record, form.showGoals) { failure ->
                saving = false
                if (failure == null) savedNotice = true else error = "Não foi possível salvar. Seus valores foram mantidos; tente novamente. (${failure.message ?: "erro desconhecido"})"
            }
        }, form.valid && dirty && !saving)
        TextButton({ confirmRestore = true }, Modifier.testTag("goals-restore")) { Text("Restaurar valores padrão") }
    }
    if (confirmRestore) AlertDialog(onDismissRequest = { confirmRestore = false }, title = { Text("Restaurar valores padrão?") },
        text = { Text("Os campos voltam aos valores padrão, mas nada é salvo até você tocar em Salvar alterações. Seu histórico é preservado.") },
        confirmButton = { TextButton({
            val d = GoalsForm.defaults()
            dailyOn = d.dailyOn; daily = d.daily; weeklyOn = d.weeklyOn; weekly = d.weekly; consistencyOn = d.consistencyOn; consistency = d.consistency
            scalePages = d.scalePages; scaleMinutes = d.scaleMinutes; record = d.record; showGoals = d.showGoals
            edit(); confirmRestore = false
        }) { Text("Restaurar") } }, dismissButton = { TextButton({ confirmRestore = false }) { Text("Cancelar") } })
    if (confirmLeave) AlertDialog(onDismissRequest = { confirmLeave = false }, title = { Text("Descartar alterações?") }, text = { Text("Há alterações que ainda não foram salvas.") },
        confirmButton = { TextButton({ confirmLeave = false; back() }) { Text("Descartar alterações") } },
        dismissButton = { TextButton({ confirmLeave = false }) { Text("Continuar editando") } })
}
