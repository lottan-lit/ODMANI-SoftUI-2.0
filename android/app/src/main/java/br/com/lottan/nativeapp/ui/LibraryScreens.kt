package br.com.lottan.nativeapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import br.com.lottan.nativeapp.data.Book
import br.com.lottan.nativeapp.data.FolderBook
import br.com.lottan.nativeapp.domain.ReadingRules

/** Both layouts consume the same filtered/sorted collection and the existing thumbnail cache. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    books: List<Book>, busy: Boolean, importPdf: () -> Unit, back: () -> Unit,
    go: (String) -> Unit, origin: String = "all", vm: LibraryViewModel, pinned: Set<String>, folder: br.com.lottan.nativeapp.data.Group? = null,
) {
    val preferences by vm.prefs.collectAsState()
    val groups by vm.groups.collectAsState()
    val categoryLinks by vm.categoryLinks.collectAsState()
    val categories = remember(groups) { groups.filter { it.kind == "categories" } }
    // "" = every document, "none" = without category, otherwise a category id. Combines with folder and origin filters.
    var categoryFilter by rememberSaveable { mutableStateOf("") }
    var assigningCategories by remember { mutableStateOf(false) }
    var selected by rememberSaveable(origin) { mutableStateOf(origin) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var requestSearchFocus by remember { mutableStateOf(false) }
    var selectedIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    var removing by remember { mutableStateOf(setOf<String>()) }
    var addMembers by remember { mutableStateOf(false) }
    var deletingFolder by remember { mutableStateOf(false) }
    var assigning by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Book?>(null) }
    var newName by rememberSaveable { mutableStateOf("") }
    var categorizing by remember { mutableStateOf<Book?>(null) }
    var sorting by remember { mutableStateOf(false) }
    val sort = preferences.librarySort
    val mode = preferences.libraryView
    val density = LocalDensity.current
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    val shown = remember(books, selected, query, sort, pinned, categoryFilter, categoryLinks) {
        val withCategory = categoryLinks.map { it.bookId }.toSet()
        val inCategory = categoryLinks.filter { it.categoryId == categoryFilter }.map { it.bookId }.toSet()
        books.filter { categoryFilter.isEmpty() || if (categoryFilter == "none") it.id !in withCategory else it.id in inCategory }.filter {
            selected == "all" || if (selected == "pinned") it.id in pinned else
                if (selected == "lottan_verified") it.origin == selected else it.origin != "lottan_verified"
        }.filter { normalize(it.name).contains(normalize(query)) }.let { list ->
            when (sort) {
                "Título" -> list.sortedBy { normalize(it.name) }
                "Progresso" -> list.sortedByDescending { if (it.lastOpened == 0L) 0f else (it.page + 1f) / it.pages.coerceAtLeast(1) }
                else -> list.sortedByDescending { maxOf(it.lastOpened, it.added) }
            }
        }
    }
    fun closeSearch() { query = ""; searchExpanded = false; keyboard?.hide() }
    fun toggleSelection(id: String) { selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id }
    BackHandler(searchExpanded) { closeSearch() }
    BackHandler(selectedIds.isNotEmpty()) { selectedIds = emptySet() }

    val header: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth()) {
        val actions: @Composable RowScope.() -> Unit = {
            LibraryIcon("search", if (searchExpanded) "Fechar pesquisa" else "Pesquisar documentos") {
                if (searchExpanded) closeSearch() else { searchExpanded = true; requestSearchFocus = true }
            }
            LibraryIcon("add", "Importar PDF", enabled = !busy, action = importPdf)
            LibraryIcon("menu", "Abrir menu") { go("menu") }
        }
        if (density.fontScale >= 1.3f) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LibraryIcon("back", "Voltar", action = back)
                Text(folder?.name ?: "Pastas", Modifier.weight(1f), color = Ink, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, content = actions)
        } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LibraryIcon("back", "Voltar", action = back)
            Text(folder?.name ?: "Pastas", Modifier.weight(1f), color = Ink, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            actions()
        }
        if (searchExpanded) {
            LaunchedEffect(requestSearchFocus) {
                if (requestSearchFocus) { focus.requestFocus(); keyboard?.show(); requestSearchFocus = false }
            }
            SoftField(query, { query = it }, Modifier.fillMaxWidth().padding(vertical = 8.dp).focusRequester(focus),
                placeholder = { Text("Buscar documentos") }, singleLine = true, trailingIcon = { if (query.isNotEmpty()) LibraryIcon("close", "Limpar pesquisa") { query = "" } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }))
        }
        val choices = listOf("all" to "Todos", "personal" to "Pessoais", "lottan_verified" to "Lottan") +
            if (origin == "pinned") listOf("pinned" to "Fixados") else emptyList()
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).glass(LocalDark.current, 14)
            .padding(4.dp)) {
            choices.forEach { (value, label) ->
                TextButton({ selected = value }, Modifier.weight(1f).background(
                    if (selected == value) LocalVisual.current.primary else androidx.compose.ui.graphics.Color.Transparent,
                    RoundedCornerShape(10.dp)).semantics { this.selected = selected == value }, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)) {
                    Text(label, fontSize = 12.sp, maxLines = 1, color = if (selected == value) LocalVisual.current.onPrimary else Muted)
                }
            }
        }
        if (categories.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp).testTag("category-filters"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(categoryFilter.isEmpty(), { categoryFilter = "" }, { Text("Todas as categorias", fontSize = 13.sp) })
                categories.forEach { c -> FilterChip(categoryFilter == c.id, { categoryFilter = c.id }, { Text(c.name, fontSize = 13.sp) }, Modifier.testTag("cat-filter-${c.id}")) }
                FilterChip(categoryFilter == "none", { categoryFilter = "none" }, { Text("Sem categoria", fontSize = 13.sp) })
            }
        }
        if (folder == null) FolderStrip(vm, go)
        else Column(Modifier.fillMaxWidth()) {
            FolderVisual(folder, Modifier.fillMaxWidth().height(90.dp))
            Caption("${books.size} PDFs nesta pasta")
            TextButton({ addMembers = true }) { Text("Adicionar PDFs existentes") }
            Row {
                TextButton({ go("folder/${folder.id}/edit") }) { Text("Editar pasta") }
                TextButton({ deletingFolder = true }) { Text("Excluir pasta") }
            }
        }
        HorizontalDivider(color = LocalVisual.current.border)
        FlowRow(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Column(Modifier.widthIn(min = 144.dp)) {
                Text("Meus documentos", color = Ink, style = MaterialTheme.typography.titleMedium)
                Caption("${shown.size} ${if (shown.size == 1) "documento encontrado" else "documentos encontrados"}")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    TextButton({ sorting = true }, Modifier.semantics { contentDescription = "Ordenar documentos" }) {
                        Text(sort, color = Muted, fontSize = 12.sp)
                        Glyph("down", Modifier.padding(start = 6.dp).size(16.dp), tint = Muted)
                    }
                    DropdownMenu(sorting, { sorting = false }) {
                        listOf("Recentes", "Título", "Progresso").forEach { value ->
                            DropdownMenuItem(text = { Text(if (value == sort) "✓ $value" else value) }, onClick = {
                                sorting = false; vm.run { vm.preferences.sort(value) }
                            })
                        }
                    }
                }
                LibraryIcon(if (mode == "grid") "list" else "grid", if (mode == "grid") "Exibir em lista" else "Exibir em grade") {
                    vm.run { vm.preferences.libraryView(if (mode == "grid") "list" else "grid") }
                }
            }
        }
        if (selectedIds.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().testTag("library-selection")) {
                Text("${selectedIds.size} ${if (selectedIds.size == 1) "selecionado" else "selecionados"}", color = Ink)
                FlowRow {
                    TextButton({ assigning = true }) { Text("Adicionar à pasta") }
                    TextButton({ assigningCategories = true }) { Text("Adicionar à categoria") }
                    TextButton({ removing = selectedIds }) { Text("Remover") }
                    TextButton({ selectedIds = emptySet() }) { Text("Cancelar seleção") }
                }
                Caption("Toque em outros documentos para selecionar.")
            }
        }
        }
    }
    val empty: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth()) {
        val title = when {
            folder != null && books.isEmpty() -> "Pasta vazia"
            books.isEmpty() -> "Sua biblioteca está pronta"
            query.isNotBlank() -> "Nenhum resultado"
            selected == "lottan_verified" -> "Nenhum documento Lottan"
            selected == "pinned" -> "Nenhum PDF fixado"
            else -> "Nenhum documento neste filtro"
        }
        EmptyLibraryState(title, when {
            folder != null && books.isEmpty() -> "Adicione documentos da biblioteca. Os arquivos não serão duplicados."
            books.isEmpty() -> "Importe um PDF para começar sua leitura."
            query.isNotBlank() -> "Não encontramos documentos para “$query” neste filtro."
            selected == "lottan_verified" -> "Nenhuma publicação editorial disponível. Seus PDFs pessoais continuam na aba Pessoais."
            else -> "Escolha outro filtro para ver seus documentos."
        })
        if (folder != null && books.isEmpty()) TextButton({ addMembers = true }) { Text("Adicionar documentos à pasta") }
        else if (books.isEmpty()) { Spacer(Modifier.height(12.dp)); Primary("＋ Importar PDF", importPdf, !busy) }
        else TextButton({ if (query.isNotBlank()) query = "" else selected = "all" }) { Text(if (query.isNotBlank()) "Limpar pesquisa" else "Ver todos os documentos") }
        }
    }
    val card: @Composable (Book, Boolean) -> Unit = { doc, list ->
        LibraryBook(doc, list, doc.id in selectedIds,
            open = { if (selectedIds.isEmpty()) go("book/${doc.id}") else toggleSelection(doc.id) },
            select = { toggleSelection(doc.id) },
            action = { action ->
                when (action) {
                    "open" -> go("reader/${doc.id}")
                    "details" -> go("book/${doc.id}")
                    "rename" -> { renaming = doc; newName = doc.name }
                    "folder" -> { selectedIds = setOf(doc.id); assigning = true }
                    "category" -> categorizing = doc
                    "select" -> toggleSelection(doc.id)
                    "unlink" -> folder?.let { group -> vm.run { vm.repository.dao.removeFolder(group.id, doc.id) } }
                    "remove" -> removing = setOf(doc.id)
                }
            }, inFolder = folder != null)
    }
    val padding = PaddingValues(start = VisualTokens.ScreenInset, end = VisualTokens.ScreenInset, top = 14.dp,
        bottom = 14.dp + LocalBottomNavClearance.current)
    Box(Modifier.fillMaxSize().testTag(if (mode == "list") "library-list" else "library-grid")) {
        if (mode == "list") LazyColumn(Modifier.fillMaxSize().testTag("document-grid"), state = listState,
            contentPadding = padding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item("heading") { header() }
            items(shown, key = { it.id }) { card(it, true) }
            if (shown.isEmpty()) item("empty") { empty() }
        } else LazyVerticalGrid(columns = GridCells.Adaptive(148.dp * maxOf(1f, density.fontScale / 1.3f)),
            modifier = Modifier.fillMaxSize().testTag("document-grid"), state = gridState, contentPadding = padding,
            horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item("heading", span = { GridItemSpan(maxLineSpan) }) { header() }
            items(shown, key = { it.id }) { card(it, false) }
            if (shown.isEmpty()) item("empty", span = { GridItemSpan(maxLineSpan) }) { empty() }
        }
    }
    if (removing.isNotEmpty()) AlertDialog(onDismissRequest = { removing = emptySet() }, title = { Text("Remover ${removing.size} PDFs?") },
        text = { Text("As cópias e os dados de leitura destes PDFs serão removidos da biblioteca. Os arquivos originais são preservados.") },
        confirmButton = { TextButton({ val ids = removing; ids.forEach(vm::remove); selectedIds = selectedIds - ids; removing = emptySet() }) { Text("Remover") } },
        dismissButton = { TextButton({ removing = emptySet() }) { Text("Cancelar") } })
    if (assigning) AssignFoldersDialog(selectedIds, vm) { assigning = false; selectedIds = emptySet() }
    if (assigningCategories) AssignCategoriesDialog(selectedIds, vm, { assigningCategories = false; selectedIds = emptySet() }, { go("category/new") })
    if (addMembers && folder != null) FolderMembersDialog(folder, vm) { addMembers = false }
    if (deletingFolder && folder != null) DeleteFolderDialog(folder, vm, deleted = back) { deletingFolder = false }
    renaming?.let { book ->
        AlertDialog(onDismissRequest = { renaming = null }, title = { Text("Renomear documento") }, text = {
            SoftField(newName, { newName = it.take(250) }, label = { Text("Novo título") })
        }, confirmButton = { TextButton({
            val name = newName.trim(); vm.run {
                vm.repository.dao.book(book.id)?.let { vm.repository.dao.put(it.copy(name = name)) }
            }; renaming = null
        }, enabled = newName.isNotBlank()) { Text("Salvar") } }, dismissButton = { TextButton({ renaming = null }) { Text("Cancelar") } })
    }
    categorizing?.let { book -> AssignCategoriesDialog(setOf(book.id), vm, { categorizing = null }, { go("category/new") }) }
}

@Composable
private fun LibraryIcon(glyph: String, label: String, enabled: Boolean = true, action: () -> Unit) {
    IconButton(action, Modifier.size(48.dp).semantics { contentDescription = label }, enabled = enabled) { Glyph(glyph, tint = if (enabled) Ink else Muted) }
}

@Composable
private fun DocumentMenu(book: Book, action: (String) -> Unit, onCover: Boolean = false, inFolder: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        if (onCover) IconButton({ expanded = true }, Modifier.size(48.dp).semantics { contentDescription = "Opções de ${ReadingRules.title(book.name)}" }) {
            Canvas(Modifier.size(24.dp)) {
                val unit = size.width / 24f
                listOf(5f, 12f, 19f).forEach { y ->
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2, y * unit)
                    drawCircle(androidx.compose.ui.graphics.Color.White, 2.25f * unit, center)
                    drawCircle(androidx.compose.ui.graphics.Color.Black, 1.5f * unit, center)
                }
            }
        } else LibraryIcon("more", "Opções de ${ReadingRules.title(book.name)}") { expanded = true }
        DropdownMenu(expanded, { expanded = false }) {
            listOf("open" to "Abrir documento", "details" to "Ver detalhes", "rename" to "Renomear", "folder" to "Organizar em pastas",
                "category" to "Alterar categoria", "select" to "Selecionar", "remove" to "Remover documento").plus(if (inFolder) listOf("unlink" to "Remover desta pasta") else emptyList()).forEach { (key, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { expanded = false; action(key) })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryBook(book: Book, list: Boolean, selected: Boolean, open: () -> Unit, select: () -> Unit, action: (String) -> Unit, inFolder: Boolean = false) {
    val shape = RoundedCornerShape(14.dp)
    val modifier = Modifier.fillMaxWidth().testTag("library-book-${book.id}")
        .clip(shape).background(if (selected) LocalVisual.current.secondary else androidx.compose.ui.graphics.Color.Transparent)
        .border(1.dp, if (selected) Ink else LocalVisual.current.border, shape)
        .combinedClickable(onClick = open, onLongClick = select).semantics { this.selected = selected }
    val description: @Composable () -> Unit = {
        Text(ReadingRules.title(book.name), Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp).testTag("library-title-${book.id}"), color = Ink,
            fontSize = 14.sp, lineHeight = 20.sp, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Column(Modifier.testTag("library-progress-${book.id}")) { ReadingProgress(book) }
    }
    if (list) Row(modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        PdfCover(book, Modifier.width(64.dp).aspectRatio(CoverRatio))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { description() }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            DocumentMenu(book, action, inFolder = inFolder)
            if (selected) Text("✓", color = Ink)
        }
    } else Column(modifier.padding(10.dp)) {
        Box {
            PdfCover(book, Modifier.fillMaxWidth().aspectRatio(CoverRatio))
            Box(Modifier.align(Alignment.TopEnd)) { DocumentMenu(book, action, onCover = true, inFolder = inFolder) }
            if (selected) Text("✓", Modifier.align(Alignment.TopStart).background(LocalVisual.current.surface, CircleShape).padding(8.dp), color = Ink)
        }
        description()
    }
}
@Composable
fun SearchScreen(books: List<Book>, back: () -> Unit, go: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val found =
        remember(books, query) { books.filter { normalize(it.name).contains(normalize(query)) } }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = VisualTokens.ScreenInset, end = VisualTokens.ScreenInset, top = 14.dp, bottom = 14.dp + LocalBottomNavClearance.current),
    ) {
        item("search") {
            Top("Buscar", back, { go("menu") })
            SoftField(
                query,
                { query = it },
                Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar pelo nome do PDF...") },
                singleLine = true,
                )
            Caption("Seus PDFs", Modifier.padding(vertical = 20.dp))
        }
        items(found, key = { it.id }) { doc -> DocumentRow(doc, { go("book/${doc.id}") }, null) }
        if (found.isEmpty()) item("empty") { Caption("Nenhum PDF encontrado.") }
    }
}
