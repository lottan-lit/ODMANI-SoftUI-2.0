package br.com.lottan.nativeapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import br.com.lottan.nativeapp.data.*
import kotlinx.coroutines.launch
import java.io.File

private val folderColors = listOf("neutral" to "Neutra", "peach" to "Pêssego", "sage" to "Sálvia", "lavender" to "Lavanda")
private val folderIcons = listOf("folder" to "Pasta", "library" to "Livros", "stats" to "Atividade", "home" to "Casa")
@Composable private fun folderTint(color: String): Color = when(color) {
    "peach" -> Color(0xffB77D60); "sage" -> Color(0xff66836D); "lavender" -> Color(0xff8D7DA6); else -> Ink
}

@Composable
fun FolderVisual(folder: Group, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cover = File(context.filesDir, "folder-covers/${folder.id}.jpg").exists()
    Box(modifier.background(LocalVisual.current.secondary, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
        if (cover) FolderCover(folder.id, 0) else Glyph(folder.icon, Modifier.size(36.dp), tint = folderTint(folder.color))
    }
}

@Composable
fun FolderStrip(vm: LibraryViewModel, go: (String) -> Unit) {
    val groups by vm.groups.collectAsState(); val links by vm.memberships.collectAsState()
    var deleting by remember { mutableStateOf<Group?>(null) }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        CardTitle("Minhas pastas", Modifier.weight(1f), maxLines = 1)
        OutlineAction("Criar pasta", Modifier.testTag("create-folder")) { go("folder/new") }
    }
    Caption("Organize seus documentos em pastas, sem duplicar os arquivos.", Modifier.padding(top = 6.dp, bottom = 12.dp))
    val folders = groups.filter { it.kind == "folders" }
    if (folders.isEmpty()) Caption("Crie uma pasta para organizar seus PDFs sem duplicá-los.")
    else LazyRow(Modifier.testTag("folder-strip"), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(folders, key = { it.id }) { folder ->
            var menu by remember { mutableStateOf(false) }
            SoftCard(Modifier.width(160.dp).testTag("folder-${folder.id}").clickable { go("folder/${folder.id}") }) {
                FolderVisual(folder, Modifier.fillMaxWidth().height(72.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(folder.name, Modifier.weight(1f), color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Box {
                        IconButton({ menu = true }, Modifier.size(40.dp).semanticsLabel("Opções da pasta ${folder.name}")) { Glyph("more") }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text("Editar pasta") }, onClick = { menu = false; go("folder/${folder.id}/edit") })
                            DropdownMenuItem(text = { Text("Excluir pasta") }, onClick = { menu = false; deleting = folder })
                        }
                    }
                }
                Caption("${links.count { it.folderId == folder.id }} PDFs")
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton({ go("organize") }) { Text("Organizar PDFs", fontSize = 14.sp); Spacer(Modifier.width(4.dp)); Glyph("forward", Modifier.size(16.dp)) } }
    deleting?.let { folder -> DeleteFolderDialog(folder, vm) { deleting = null } }
}

@Composable
fun DeleteFolderDialog(folder: Group, vm: LibraryViewModel, deleted: () -> Unit = {}, done: () -> Unit) {
    AlertDialog(onDismissRequest = done, title = { Text("Excluir pasta?") }, text = { Text("A pasta ${folder.name} e seus vínculos serão removidos. Os PDFs, notas e histórico permanecem na biblioteca.") },
        confirmButton = { TextButton({ val id = folder.id; vm.run { vm.repository.removeGroup(id); deleted() }; done() }) { Text("Excluir pasta") } }, dismissButton = { TextButton(done) { Text("Cancelar") } })
}

@Composable
private fun FolderFields(name: String, color: String, icon: String, onName: (String) -> Unit, onColor: (String) -> Unit, onIcon: (String) -> Unit) {
    FolderVisual(Group("preview", name.ifBlank { "Nova pasta" }, "folders", color, icon), Modifier.fillMaxWidth().height(88.dp))
    SoftField(name, { onName(it.take(60)) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Nome da pasta") }, isError = name.isNotEmpty() && name.isBlank())
    Caption("Cor opcional")
    Row(Modifier.horizontalScroll(rememberScrollState())) { folderColors.forEach { (key, label) ->
        TextButton({ onColor(key) }, Modifier.semantics { selected = color == key }) { Text(if (color == key) "✓ $label" else label, color = folderTint(key)) }
    } }
    Caption("Ícone opcional")
    Row(Modifier.horizontalScroll(rememberScrollState())) { folderIcons.forEach { (key, label) ->
        IconButton({ onIcon(key) }, Modifier.semantics { contentDescription = "Ícone $label"; selected = icon == key }) { Glyph(key, tint = if (icon == key) Ink else Muted) }
    } }
}

@Composable
fun FolderEditorScreen(vm: LibraryViewModel, folder: Group?, back: () -> Unit) {
    var name by rememberSaveable(folder?.id) { mutableStateOf(folder?.name ?: "") }
    var color by rememberSaveable(folder?.id) { mutableStateOf(folder?.color ?: "neutral") }
    var icon by rememberSaveable(folder?.id) { mutableStateOf(folder?.icon ?: "folder") }
    var error by remember { mutableStateOf<String?>(null) }; var saving by remember { mutableStateOf(false) }
    Page {
        Top(if (folder == null) "Criar pasta" else "Editar pasta", back, back)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FolderFields(name, color, icon, { name = it }, { color = it }, { icon = it })
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Primary(if (saving) "Salvando…" else if (folder == null) "Criar pasta" else "Salvar pasta", {
                saving = true
                vm.run { try { vm.repository.saveFolder(folder?.id, name, color, icon); back() } catch (e: Exception) { error = e.message } finally { saving = false } }
            }, name.isNotBlank() && !saving)
            TextButton(back) { Text("Cancelar") }
        }
    }
}

@Composable
fun CreateFolderDialog(vm: LibraryViewModel, close: () -> Unit, created: (Group) -> Unit = {}) {
    var name by rememberSaveable { mutableStateOf("") }; var color by rememberSaveable { mutableStateOf("neutral") }; var icon by rememberSaveable { mutableStateOf("folder") }
    var error by remember { mutableStateOf<String?>(null) }; var saving by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = { if (!saving) close() }, title = { Text("Criar pasta") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FolderFields(name, color, icon, { name = it }, { color = it }, { icon = it }); error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton({ saving = true; vm.run { try { val folder = vm.repository.saveFolder(null, name, color, icon); created(folder); close() } catch (e: Exception) { error = e.message } finally { saving = false } } }, enabled = name.isNotBlank() && !saving) { Text("Criar") } }, dismissButton = { TextButton(close, enabled = !saving) { Text("Cancelar") } })
}

@Composable
fun FolderSelection(folders: List<Group>, selected: Set<String>, change: (String, Boolean) -> Unit, create: () -> Unit) {
    folders.filter { it.kind == "folders" }.forEach { folder ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(folder.id in selected, { change(folder.id, it) }, Modifier.semanticsLabel("Pasta ${folder.name}"))
            Glyph(folder.icon, Modifier.size(20.dp), tint = folderTint(folder.color))
            Text(folder.name, Modifier.weight(1f).padding(start = 8.dp), color = Ink)
        }
    }
    if (folders.none { it.kind == "folders" }) Caption("Nenhuma pasta criada. O PDF continuará em Todos os PDFs.")
    TextButton(create) { Text("+ Criar pasta") }
}

@Composable
fun AssignFoldersDialog(bookIds: Set<String>, vm: LibraryViewModel, close: () -> Unit) {
    val groups by vm.groups.collectAsState(); val memberships by vm.memberships.collectAsState()
    val original = remember(bookIds) { bookIds.associateWith { id -> memberships.filter { it.bookId == id }.map { it.folderId }.toSet() } }
    var checked by rememberSaveable(bookIds) { mutableStateOf(groups.filter { g -> bookIds.all { g.id in original.getValue(it) } }.map { it.id }.toSet()) }
    var touched by rememberSaveable(bookIds) { mutableStateOf(setOf<String>()) }; var creating by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = close, title = { Text("Organizar em pastas") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            FolderSelection(groups, checked, { id, value -> touched = touched + id; checked = if (value) checked + id else checked - id }, { creating = true })
        }
    }, confirmButton = { TextButton({
        val changes = touched; val values = checked; val before = original
        vm.run { bookIds.forEach { id -> vm.repository.setFolders(id, (before.getValue(id) - changes) + (values intersect changes)) } }
        close()
    }) { Text("Salvar") } }, dismissButton = { TextButton(close) { Text("Cancelar") } })
    if (creating) CreateFolderDialog(vm, { creating = false }) { g -> checked = checked + g.id; touched = touched + g.id }
}

@Composable
fun FolderLibraryScreen(folderId: String, vm: LibraryViewModel, books: List<Book>, back: () -> Unit, go: (String) -> Unit, importPdf: () -> Unit, pinned: Set<String>, busy: Boolean) {
    val groups by vm.groups.collectAsState(); val links by vm.memberships.collectAsState()
    val folder = groups.firstOrNull { it.id == folderId && it.kind == "folders" }
    if (folder == null) Page { Top("Pasta", back, back); Caption("Esta pasta não está disponível.") }
    else LibraryScreen(books.filter { book -> links.any { it.bookId == book.id && it.folderId == folderId } }, busy, importPdf, back, go, vm = vm, pinned = pinned, folder = folder)
}

@Composable
fun FolderMembersDialog(folder: Group, vm: LibraryViewModel, close: () -> Unit) {
    val books by vm.books.collectAsState(); val links by vm.memberships.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var checked by rememberSaveable(folder.id) { mutableStateOf(links.filter { it.folderId == folder.id }.map { it.bookId }.toSet()) }
    AlertDialog(onDismissRequest = close, title = { Text("Adicionar PDFs existentes") }, text = {
        Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
            SoftField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Buscar PDFs") })
            books.filter { normalize(it.name).contains(normalize(query)) }.forEach { book ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(book.id in checked, { checked = if (it) checked + book.id else checked - book.id }, Modifier.semanticsLabel("Vincular ${book.name}"))
                    Text(book.name, Modifier.weight(1f), color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (books.isEmpty()) Caption("Ainda não há PDFs. Importe um documento na página Adicionar PDF.")
        }
    }, confirmButton = { TextButton({ val ids = checked; vm.run { vm.repository.setFolderMembers(folder.id, ids) }; close() }) { Text("Salvar vínculos") } }, dismissButton = { TextButton(close) { Text("Cancelar") } })
}
