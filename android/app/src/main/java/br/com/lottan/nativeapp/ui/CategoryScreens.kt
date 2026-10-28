package br.com.lottan.nativeapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import br.com.lottan.nativeapp.data.*
import br.com.lottan.nativeapp.domain.ReadingRules
import kotlinx.coroutines.launch

/** Personal PDFs only: Lottan editorial works keep their identity and are not part of batch classification. */
fun personalBooks(books: List<Book>) = books.filter { it.origin != "lottan_verified" }

/** Searchable checklist of PDFs with Select all / Clear all. Selection always applies to the filtered result. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookPicker(books: List<Book>, selected: Set<String>, onChange: (Set<String>) -> Unit, modifier: Modifier = Modifier, maxHeight: Dp = 360.dp) {
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(books, query) { books.filter { normalize(it.name).contains(normalize(query)) } }
    Column(modifier) {
        SoftField(query, { query = it }, Modifier.fillMaxWidth().testTag("picker-search"), singleLine = true, label = { Text("Buscar PDFs") })
        FlowRow(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.Center) {
            Text("${selected.size} ${if (selected.size == 1) "selecionado" else "selecionados"}", color = Ink, fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp).testTag("picker-count"))
            TextButton({ onChange(selected + shown.map { it.id }) }, Modifier.testTag("picker-all")) { Text("Selecionar todos", fontSize = 13.sp) }
            TextButton({ onChange(selected - shown.map { it.id }.toSet()) }, Modifier.testTag("picker-none")) { Text("Desmarcar todos", fontSize = 13.sp) }
        }
        LazyColumn(Modifier.heightIn(max = maxHeight).testTag("picker-list")) {
            items(shown, key = { it.id }) { book ->
                Row(Modifier.fillMaxWidth().clickable { onChange(if (book.id in selected) selected - book.id else selected + book.id) }.testTag("pick-${book.id}"), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(book.id in selected, { onChange(if (it) selected + book.id else selected - book.id) }, Modifier.semanticsLabel("Selecionar ${ReadingRules.title(book.name)}"))
                    Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                        Text(ReadingRules.title(book.name), color = Ink, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Caption("${book.pages} páginas")
                    }
                }
            }
            if (shown.isEmpty()) item("none") { Caption("Nenhum PDF encontrado.", Modifier.padding(12.dp)) }
        }
    }
}

/** Assign one or many PDFs to categories (many-to-many). Only boxes the user touches are changed. */
@Composable
fun AssignCategoriesDialog(bookIds: Set<String>, vm: LibraryViewModel, close: () -> Unit, create: () -> Unit = {}) {
    val groups by vm.groups.collectAsState(); val links by vm.categoryLinks.collectAsState()
    val categories = groups.filter { it.kind == "categories" }
    val original = remember(bookIds) { bookIds.associateWith { id -> links.filter { it.bookId == id }.map { it.categoryId }.toSet() } }
    var checked by rememberSaveable(bookIds) { mutableStateOf(categories.filter { c -> bookIds.all { c.id in original.getValue(it) } }.map { it.id }.toSet()) }
    var touched by rememberSaveable(bookIds) { mutableStateOf(setOf<String>()) }
    AlertDialog(onDismissRequest = close, title = { Text("Categorias") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            categories.forEach { category ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(category.id in checked, { value -> touched = touched + category.id; checked = if (value) checked + category.id else checked - category.id }, Modifier.semanticsLabel("Categoria ${category.name}"))
                    Text(category.name, Modifier.weight(1f).padding(start = 4.dp), color = Ink)
                }
            }
            if (categories.isEmpty()) Caption("Nenhuma categoria criada ainda.")
            TextButton({ close(); create() }) { Text("+ Criar categoria") }
        }
    }, confirmButton = { TextButton({
        val changes = touched; val values = checked; val before = original
        vm.run { bookIds.forEach { id -> vm.repository.setBookCategories(id, (before.getValue(id) - changes) + (values intersect changes)) } }
        close()
    }) { Text("Salvar") } }, dismissButton = { TextButton(close) { Text("Cancelar") } })
}

/** Add/remove PDFs of one category (or folder, via [save]) in a single dialog. */
@Composable
fun MembersDialog(title: String, books: List<Book>, initial: Set<String>, close: () -> Unit, save: (Set<String>) -> Unit) {
    var checked by rememberSaveable(title) { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = close, title = { Text(title) }, text = {
        if (books.isEmpty()) Caption("Ainda não há PDFs pessoais. Importe um documento primeiro.")
        else BookPicker(books, checked, { checked = it })
    }, confirmButton = { TextButton({ save(checked); close() }) { Text("Salvar vínculos") } }, dismissButton = { TextButton(close) { Text("Cancelar") } })
}

@Composable
fun RenameGroupDialog(group: Group, vm: LibraryViewModel, close: () -> Unit) {
    var name by rememberSaveable(group.id) { mutableStateOf(group.name) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val label = if (group.kind == "categories") "categoria" else "pasta"
    AlertDialog(onDismissRequest = close, title = { Text("Renomear $label") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftField(name, { name = it.take(60); error = null }, Modifier.fillMaxWidth().testTag("rename-field"), singleLine = true, label = { Text("Nome") }, isError = error != null, supportingText = error?.let { { Text(it) } })
        }
    }, confirmButton = { TextButton({
        scope.launch {
            try {
                if (group.kind == "categories") vm.repository.saveCategory(group.id, name) else vm.repository.saveFolder(group.id, name, group.color, group.icon)
                close()
            } catch (e: Exception) { error = e.message }
        }
    }, enabled = name.isNotBlank()) { Text("Salvar") } }, dismissButton = { TextButton(close) { Text("Cancelar") } })
}

@Composable
fun DeleteCategoryDialog(category: Group, vm: LibraryViewModel, close: () -> Unit) {
    AlertDialog(onDismissRequest = close, title = { Text("Excluir categoria?") }, text = { Text("A categoria ${category.name} e seus vínculos serão removidos. Os PDFs, notas e histórico permanecem na biblioteca.") },
        confirmButton = { TextButton({ val id = category.id; vm.run { vm.repository.removeGroup(id) }; close() }) { Text("Excluir categoria") } }, dismissButton = { TextButton(close) { Text("Cancelar") } })
}

/**
 * Category creation in four steps: name (or suggestion) -> pick PDFs -> "Adicionar X PDFs" -> result.
 * The category is created at step 1, so leaving after that keeps an empty category (allowed).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryCreateScreen(vm: LibraryViewModel, books: List<Book>, groups: List<Group>, back: () -> Unit, openLibrary: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    var name by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf("") }
    var categoryName by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(setOf<String>()) }
    var added by rememberSaveable { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val existing = groups.filter { it.kind == "categories" }.map { it.name }
    val personal = remember(books) { personalBooks(books) }
    Page {
        Top(when (step) { 1 -> "Criar categoria"; 2 -> "Adicionar PDFs"; else -> "Categoria criada" }, { back() }, {})
        when (step) {
            1 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Caption("Dê o nome que quiser. As sugestões abaixo são opcionais.")
                SoftField(name, { name = it.take(CategoryRules.MAX_LENGTH + 10); error = null }, Modifier.fillMaxWidth().testTag("category-name"), singleLine = true, label = { Text("Nome da categoria") },
                    isError = error != null, supportingText = { Text(error ?: "Até ${CategoryRules.MAX_LENGTH} caracteres") })
                Caption("Sugestões")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryRules.suggestions.filter { s -> existing.none { it.equals(s, true) } }.forEach { s ->
                        FilterChip(name.equals(s, true), { name = s; error = null }, { Text(s, fontSize = 13.sp) }, Modifier.testTag("suggest-$s"))
                    }
                }
                Primary(if (busy) "Criando…" else "Criar categoria", {
                    busy = true
                    scope.launch {
                        try {
                            val group = vm.repository.saveCategory(null, name)
                            categoryId = group.id; categoryName = group.name; step = 2
                        } catch (e: Exception) { error = e.message } finally { busy = false }
                    }
                }, name.isNotBlank() && !busy)
                TextButton({ back() }) { Text("Cancelar") }
            }
            2 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Caption("Categoria “$categoryName” criada. Escolha PDFs pessoais para adicionar agora ou conclua sem documentos.")
                BookPicker(personal, selected, { selected = it }, maxHeight = 420.dp)
                Primary(if (busy) "Adicionando…" else "Adicionar ${selected.size} ${if (selected.size == 1) "PDF" else "PDFs"}", {
                    busy = true
                    scope.launch {
                        try { added = vm.repository.addBooksToCategory(categoryId, selected); step = 3 }
                        catch (e: Exception) { error = e.message } finally { busy = false }
                    }
                }, selected.isNotEmpty() && !busy)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                TextButton({ added = 0; step = 3 }) { Text("Concluir sem adicionar") }
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SoftCard(Modifier.fillMaxWidth().testTag("category-result")) {
                    Text(if (added == 0) "Categoria $categoryName criada, ainda sem PDFs." else "$added ${if (added == 1) "PDF adicionado" else "PDFs adicionados"} à categoria $categoryName.", color = Ink, fontSize = 16.sp)
                }
                Primary("Voltar à biblioteca", openLibrary)
                TextButton({ back() }) { Text("Voltar à organização") }
            }
        }
    }
}
