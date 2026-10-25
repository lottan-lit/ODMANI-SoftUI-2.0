package br.com.lottan.nativeapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.lottan.nativeapp.data.Workspace

@Composable
private fun GoalField(label: String, value: String, active: Boolean, maximum: Int, onActive: (Boolean) -> Unit, onValue: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = Ink)
        Switch(active, onActive)
    }
    OutlinedTextField(value, { onValue(it.filter(Char::isDigit).take(5)) }, Modifier.fillMaxWidth(), enabled = active,
        shape = RoundedCornerShape(20.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        label = { Text(if (active) "Quantidade" else "Meta desativada") },
        isError = active && ((value.toIntOrNull() ?: 0) !in 1..maximum),
        supportingText = { if (active) Text("De 1 a $maximum") })
}

@Composable
fun GoalsScreen(vm: LibraryViewModel, workspace: Workspace, back: () -> Unit) {
    var daily by rememberSaveable { mutableStateOf(workspace.daily.toString()) }
    var weekly by rememberSaveable { mutableStateOf(workspace.weekly.toString()) }
    var consistency by rememberSaveable { mutableStateOf(workspace.consistency.toString()) }
    var dailyOn by rememberSaveable { mutableStateOf(workspace.daily > 0) }
    var weeklyOn by rememberSaveable { mutableStateOf(workspace.weekly > 0) }
    var consistencyOn by rememberSaveable { mutableStateOf(workspace.consistency > 0) }
    var restore by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val prefs by vm.prefs.collectAsState()
    val valid = (!dailyOn || (daily.toIntOrNull() ?: 0) in 1..10000) && (!weeklyOn || (weekly.toIntOrNull() ?: 0) in 1..70000) && (!consistencyOn || (consistency.toIntOrNull() ?: 0) in 1..7)
    Page {
        Top("Metas de leitura", back, {})
        Caption("Configure seu ritmo. Você pode desativar cada meta sem apagar seu histórico.")
        Spacer(Modifier.height(20.dp))
        SoftCard(Modifier.fillMaxWidth()) {
            GoalField("Páginas por dia", daily, dailyOn, 10000, { dailyOn = it }, { daily = it })
            Spacer(Modifier.height(16.dp))
            GoalField("Páginas por semana", weekly, weeklyOn, 70000, { weeklyOn = it }, { weekly = it })
            Spacer(Modifier.height(16.dp))
            GoalField("Dias ativos por semana", consistency, consistencyOn, 7, { consistencyOn = it }, { consistency = it })
        }
        Spacer(Modifier.height(24.dp))
        Primary(if (saving) "Salvando…" else "Salvar metas", {
            saving = true
            vm.run { try {
                vm.workspaceRepository.goals(if (dailyOn) daily.toInt() else 0, if (weeklyOn) weekly.toInt() else 0, if (consistencyOn) consistency.toInt() else 0)
                back()
            } finally { saving = false } }
        }, valid && !saving)
        TextButton({ restore = true }) { Text("Restaurar valores padrão") }
        Spacer(Modifier.height(24.dp))
        SoftCard(Modifier.fillMaxWidth()) {
            Text("Registro e privacidade", color = Ink)
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Registrar novas sessões", Modifier.weight(1f), color = Ink); Switch(prefs.recordActivity, { value -> vm.run { vm.preferences.flag("recordActivity", value) } }) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Mostrar metas na Home", Modifier.weight(1f), color = Ink); Switch(prefs.showGoals, { value -> vm.run { vm.preferences.flag("showGoals", value) } }) }
            Caption("Essas preferências são salvas imediatamente. Desativar o registro preserva os dados anteriores.")
        }
    }
    if (restore) AlertDialog(onDismissRequest = { restore = false }, title = { Text("Restaurar metas?") }, text = { Text("O padrão deixa as três metas desativadas. Seu histórico será preservado. Toque em Salvar metas para aplicar.") }, confirmButton = { TextButton({ daily = "0"; weekly = "0"; consistency = "0"; dailyOn = false; weeklyOn = false; consistencyOn = false; restore = false }) { Text("Restaurar") } }, dismissButton = { TextButton({ restore = false }) { Text("Cancelar") } })
}
