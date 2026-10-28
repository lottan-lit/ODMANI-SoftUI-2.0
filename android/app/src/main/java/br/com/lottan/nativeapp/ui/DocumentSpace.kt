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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
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
        if (mode == "Anotações") SoftField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("Suas anotações") }, minLines = 4)
        if (mode == "Checklist") {
            var editing by rememberSaveable(book.id) { mutableStateOf<String?>(null) }
            var deleting by rememberSaveable(book.id) { mutableStateOf<String?>(null) }
            tasks.forEachIndexed { index, task -> key(task.id) {
                var options by remember { mutableStateOf(false) }
                Row(Modifier.fillMaxWidth().testTag("task-${task.id}").padding(bottom = 8.dp)
                    .background(LocalVisual.current.secondary, RoundedCornerShape(16.dp)).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(task.done, { done -> tasks = tasks.map { if (it.id == task.id) it.copy(done = done) else it } }, Modifier.semanticsLabel("Concluir tarefa ${task.text}"))
                    Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        Text(task.text.ifBlank { "Tarefa sem descrição" }, Modifier.fillMaxWidth().clickable { editing = task.id }.padding(vertical = 6.dp),
                            color = if (task.done) Muted else Ink, fontSize = 14.sp, lineHeight = 20.sp,
                            textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None)
                        task.page?.let { target ->
                            if (target in 0 until book.pages) Box(Modifier.fillMaxWidth().heightIn(min = 32.dp).clickable { vm.run { vm.repository.position(book.id, target); go("reader/${book.id}") } }, contentAlignment = Alignment.CenterStart) { Text("Página ${target + 1}", color = Muted, fontSize = 12.sp) }
                            else Caption("Página vinculada indisponível")
                        }
                    }
                    Box {
                        IconButton({ options = true }, Modifier.size(48.dp).semanticsLabel("Ações da tarefa ${task.text}")) { Glyph("more") }
                        DropdownMenu(options, { options = false }) {
                            DropdownMenuItem(text = { Text("Editar") }, onClick = { options = false; editing = task.id })
                            DropdownMenuItem(text = { Text("Alterar página") }, onClick = { options = false; editing = task.id })
                            if (index > 0) DropdownMenuItem(text = { Text("Mover para cima") }, onClick = { options = false; tasks = tasks.toMutableList().apply { add(index - 1, removeAt(index)) } })
                            if (index < tasks.lastIndex) DropdownMenuItem(text = { Text("Mover para baixo") }, onClick = { options = false; tasks = tasks.toMutableList().apply { add(index + 1, removeAt(index)) } })
                            DropdownMenuItem(text = { Text("Excluir tarefa") }, onClick = { options = false; if (task.text.isBlank()) tasks = tasks.filter { it.id != task.id } else deleting = task.id })
                        }
                    }
                }
            } }
            TextButton({ editing = "" }) { Text("+ Nova tarefa") }
            editing?.let { id ->
                TaskEditor(book.pages, tasks.firstOrNull { it.id == id }, { editing = null }) { value ->
                    tasks = if (id.isEmpty()) tasks + value else tasks.map { if (it.id == id) value else it }
                    editing = null
                }
            }
            deleting?.let { id -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Excluir tarefa?") }, text = { Text("A tarefa será removida do checklist. O PDF não será alterado.") }, confirmButton = { TextButton({ tasks = tasks.filter { it.id != id }; deleting = null }) { Text("Excluir") } }, dismissButton = { TextButton({ deleting = null }) { Text("Cancelar") } }) }
        }
        if (mode == "Desativado") Caption("Anotações e checklist permanecem salvos.")
    }
    if (rename) AlertDialog(onDismissRequest = { rename = false }, title = { Text("Título de exibição") }, text = {
        SoftField(title, { title = it }, label = { Text("Nome do documento") })
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
            CardTitle("Suas metas de leitura", Modifier.weight(1f))
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

@Composable
private fun TaskEditor(pages: Int, task: TaskItem?, cancel: () -> Unit, save: (TaskItem) -> Unit) {
    var text by rememberSaveable(task?.id) { mutableStateOf(task?.text ?: "") }
    var page by rememberSaveable(task?.id) { mutableStateOf(task?.page?.plus(1)?.toString() ?: "") }
    var discard by remember { mutableStateOf(false) }
    val number = page.toIntOrNull()
    val validPage = page.isEmpty() || number != null && number in 1..pages
    fun dismiss() { if (text != (task?.text ?: "") || page != (task?.page?.plus(1)?.toString() ?: "")) discard = true else cancel() }
    AlertDialog(onDismissRequest = { dismiss() }, title = { Text(if (task == null) "Nova tarefa" else "Editar tarefa") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SoftField(text, { text = it }, Modifier.fillMaxWidth(), label = { Text("Descrição da tarefa") }, minLines = 1, maxLines = 5)
            SoftField(page, { page = it.filter(Char::isDigit).take(9) }, Modifier.fillMaxWidth(), label = { Text("Página opcional") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = !validPage,
                supportingText = { if (!validPage) Text("Informe uma página de 1 a $pages") })
        }
    }, confirmButton = { TextButton({ save(TaskItem(task?.id ?: UUID.randomUUID().toString(), text.trim(), task?.done ?: false, if (page.isEmpty()) null else number!! - 1)) }, enabled = text.isNotBlank() && validPage) { Text("Salvar tarefa") } }, dismissButton = { TextButton({ dismiss() }) { Text("Cancelar") } })
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Descartar alterações?") }, text = { Text("As alterações ainda não foram salvas.") }, confirmButton = { TextButton(cancel) { Text("Descartar") } }, dismissButton = { TextButton({ discard = false }) { Text("Continuar editando") } })
}
