package br.com.lottan.nativeapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import br.com.lottan.nativeapp.data.*
import java.util.UUID

@Composable
private fun DocumentAction(icon: String, label: String, action: () -> Unit) {
    TextButton(action, Modifier.fillMaxWidth().heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
        Glyph(icon, Modifier.size(22.dp)); Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f), color = Ink)
    }
}

@Composable
fun DocumentActions(book: Book, vm: LibraryViewModel, workspace: Workspace, go: (String) -> Unit) {
    val saved = workspace.spaces[book.id] ?: DocumentSpace()
    var note by remember(book.id) { mutableStateOf(saved.note) }
    var tasks by remember(book.id) { mutableStateOf(saved.tasks) }
    var mode by remember(book.id) { mutableStateOf(saved.mode) }
    var rename by remember { mutableStateOf(false) }
    var title by remember(book.id, book.name) { mutableStateOf(book.name) }
    LaunchedEffect(note, tasks, mode) {
        kotlinx.coroutines.delay(350)
        vm.workspaceRepository.space(book.id, DocumentSpace(note, mode, tasks))
    }
    val latestSpace by rememberUpdatedState(DocumentSpace(note, mode, tasks))
    DisposableEffect(book.id) { onDispose { vm.saveSpace(book.id, latestSpace) } }
    Column(Modifier.padding(vertical = 12.dp)) {
        DocumentAction("pin", if (book.id in workspace.pinned) "Desafixar da Home" else "Fixar na Home") { vm.run { vm.workspaceRepository.pin(book.id) } }
        DocumentAction("book-check", if (workspace.main == book.id) "Leitura principal ✓ · remover" else "Definir como leitura principal") { vm.run { vm.workspaceRepository.main(if (workspace.main == book.id) "" else book.id) } }
        DocumentAction("edit", "Renomear título") { rename = true }
    }
    SoftCard(Modifier.fillMaxWidth()) {
        Text("Meu espaço", color = Ink)
        Segments(listOf("Anotações", "Checklist", "Desativado"), mode) { mode = it }
        if (mode == "Anotações") OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), label = { Text("Suas anotações") }, minLines = 4)
        if (mode == "Checklist") {
            tasks.forEachIndexed { index, task ->
                key(task.id) {
                    var pageText by rememberSaveable(book.id, task.id) { mutableStateOf(task.page?.plus(1)?.toString() ?: "") }
                    val validPage = pageText.toIntOrNull()?.takeIf { it in 1..book.pages }?.minus(1)
                    val pageError = pageText.isNotEmpty() && validPage == null
                    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        .background(LocalVisual.current.secondary, RoundedCornerShape(20.dp))
                        .border(1.dp, LocalVisual.current.border, RoundedCornerShape(20.dp)).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(task.done, { done -> tasks = tasks.map { if (it.id == task.id) it.copy(done = done) else it } })
                            OutlinedTextField(task.text, { text -> tasks = tasks.map { if (it.id == task.id) it.copy(text = text) else it } },
                                Modifier.weight(1f), shape = RoundedCornerShape(20.dp), label = { Text("Descrição") }, minLines = 1, maxLines = 6)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(pageText, { value ->
                                pageText = value.filter(Char::isDigit).take(6)
                                val target = pageText.toIntOrNull()?.takeIf { it in 1..book.pages }?.minus(1)
                                tasks = tasks.map { if (it.id == task.id) it.copy(page = target) else it }
                            }, Modifier.weight(1f), shape = RoundedCornerShape(20.dp), label = { Text("Página opcional") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = pageError,
                                supportingText = { if (pageError) Text("Use uma página de 1 a ${book.pages}") })
                            IconButton({ tasks = tasks.filter { it.id != task.id } }, Modifier.size(48.dp).semanticsLabel("Excluir item")) { Glyph("trash") }
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            if (validPage != null) TextButton({ vm.run { vm.repository.dao.progress(book.id, validPage, false); go("reader/${book.id}") } }, Modifier.weight(1f)) { Text("Abrir página ${validPage + 1}") }
                            else Spacer(Modifier.weight(1f))
                            if (index > 0) IconButton({ tasks = tasks.toMutableList().apply { add(index - 1, removeAt(index)) } }, Modifier.size(48.dp).semanticsLabel("Mover item para cima")) { Glyph("up") }
                            if (index < tasks.lastIndex) IconButton({ tasks = tasks.toMutableList().apply { add(index + 1, removeAt(index)) } }, Modifier.size(48.dp).semanticsLabel("Mover item para baixo")) { Glyph("down") }
                        }
                    }
                }
            }
            TextButton({ tasks = tasks + TaskItem(UUID.randomUUID().toString(), "") }) { Text("+ Novo item") }
        }
        if (mode == "Desativado") Caption("Anotações e checklist permanecem salvos.")
    }
    if (rename) AlertDialog(onDismissRequest = { rename = false }, title = { Text("Título de exibição") }, text = {
        OutlinedTextField(title, { title = it }, shape = RoundedCornerShape(20.dp), label = { Text("Nome do documento") })
    }, confirmButton = { TextButton({ vm.run { vm.repository.dao.put(book.copy(name = title.trim().take(250))) }; rename = false }, enabled = title.isNotBlank()) { Text("Salvar") } }, dismissButton = { TextButton({ rename = false }) { Text("Cancelar") } })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalPanel(workspace: Workspace, days: List<Day>, onEdit: () -> Unit) {
    val today = java.time.LocalDate.now()
    val recent = days.filter { it.date >= today.minusDays(6).toString() && it.date <= today.toString() }
    val daily = days.firstOrNull { it.date == today.toString() }?.visits ?: 0
    val weekly = recent.sumOf { it.visits }
    val active = recent.count { it.millis > 0 }
    val configured = workspace.daily > 0 || workspace.weekly > 0 || workspace.consistency > 0
    SoftCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Suas metas de leitura", Modifier.weight(1f), color = Ink)
            TextButton(onEdit, contentPadding = PaddingValues(horizontal = 8.dp)) { Text(if (configured) "Editar metas" else "Adicionar metas", fontSize = 12.sp) }
        }
        Spacer(Modifier.height(18.dp))
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(Triple("Diária", daily.toFloat(), workspace.daily), Triple("Semanal", weekly.toFloat(), workspace.weekly), Triple("Consistência", active.toFloat(), workspace.consistency)).forEach { (label, value, target) ->
                Column(Modifier.widthIn(min = 100.dp).padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { if (target > 0) (value / target).coerceIn(0f, 1f) else 0f }, modifier = Modifier.fillMaxSize(), color = Ink, trackColor = LocalVisual.current.secondary)
                        Text(if (target > 0) "${(value / target * 100).toInt().coerceIn(0, 100)}%" else "—", color = Ink, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(label, color = Muted, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text(if (target > 0) "${value.toInt()} / $target" else "Desativada", color = Muted, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}
