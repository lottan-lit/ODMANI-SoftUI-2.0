package br.com.lottan.nativeapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import br.com.lottan.nativeapp.data.*
import br.com.lottan.nativeapp.domain.ReadingRules
import kotlinx.coroutines.flow.first
import androidx.compose.ui.platform.testTag
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** What the user is doing in the document list of Organizar PDFs. */
private enum class BatchAction { ADD, REMOVE }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrganizeScreen(
    vm: LibraryViewModel,
    books: List<Book>,
    groups: List<Group>,
    back: () -> Unit,
    menu: () -> Unit,
    open: (String) -> Unit,
    go: (String) -> Unit = {},
) {
    val revision by vm.coverRevision.collectAsState()
    val folderLinks by vm.memberships.collectAsState()
    val categoryLinks by vm.categoryLinks.collectAsState()
    var coverUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var crop by remember { mutableFloatStateOf(.5f) }
    var coverGroup by remember { mutableStateOf("") }
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { coverUri = it; crop = .5f }
    var tab by rememberSaveable { mutableStateOf("Pastas") }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf("Título") }
    var filter by rememberSaveable { mutableStateOf("") } // "" = all, "none" = unclassified, otherwise group id
    var selected by rememberSaveable { mutableStateOf(setOf<String>()) }
    var batch by remember { mutableStateOf<BatchAction?>(null) }
    var assigning by remember { mutableStateOf<Book?>(null) }
    var assignKind by remember { mutableStateOf("folders") }
    var membersOf by remember { mutableStateOf<Group?>(null) }
    var renaming by remember { mutableStateOf<Group?>(null) }
    var deleting by remember { mutableStateOf<Group?>(null) }
    var clearing by remember { mutableStateOf<Book?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val folders = remember(groups) { groups.filter { it.kind == "folders" } }
    val categories = remember(groups) { groups.filter { it.kind == "categories" } }
    val isFolders = tab == "Pastas"
    val current = if (isFolders) folders else categories
    // Counters are always separate: PDFs, folders and categories never share one number.
    val folderCount = remember(folderLinks) { folderLinks.groupingBy { it.folderId }.eachCount() }
    val categoryCount = remember(categoryLinks) { categoryLinks.groupingBy { it.categoryId }.eachCount() }
    val folderNames = remember(folderLinks, folders) { folderLinks.groupBy({ it.bookId }, { l -> folders.firstOrNull { it.id == l.folderId }?.name }).mapValues { it.value.filterNotNull().sorted() } }
    val categoryNames = remember(categoryLinks, categories) { categoryLinks.groupBy({ it.bookId }, { l -> categories.firstOrNull { it.id == l.categoryId }?.name }).mapValues { it.value.filterNotNull().sorted() } }
    val pool = remember(books) { personalBooks(books) }
    val shown = remember(pool, query, sort, filter, tab, folderLinks, categoryLinks) {
        val linked: Set<String> = when {
            filter.isEmpty() -> emptySet()
            isFolders -> folderLinks.filter { it.folderId == filter }.map { it.bookId }.toSet()
            else -> categoryLinks.filter { it.categoryId == filter }.map { it.bookId }.toSet()
        }
        val any: Set<String> = if (isFolders) folderLinks.map { it.bookId }.toSet() else categoryLinks.map { it.bookId }.toSet()
        pool.filter { b ->
            when (filter) { "" -> true; "none" -> b.id !in any; else -> b.id in linked }
        }.filter { normalize(it.name).contains(normalize(query)) }.let { list ->
            if (sort == "Recentes") list.sortedByDescending { maxOf(it.lastOpened, it.added) } else list.sortedBy { normalize(it.name) }
        }
    }
    // Selection only ever counts documents that still exist; filtering never drops hidden selections silently.
    val validSelection = selected intersect pool.map { it.id }.toSet()
    LaunchedEffect(notice) { if (notice != null) { kotlinx.coroutines.delay(3500); notice = null } }
    Column(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.weight(1f).testTag("organize-list"), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 14.dp + LocalBottomNavClearance.current)) {
        item("head") {
            Column {
                Top("Organizar PDFs", back, menu)
                Caption("Organize seus documentos sem alterar os arquivos originais.")
                Segments(listOf("Pastas", "Categorias"), tab) { tab = it; filter = ""; selected = emptySet() }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    CardTitle(if (isFolders) "Minhas pastas" else "Minhas categorias", Modifier.weight(1f), maxLines = 1)
                    OutlineAction(if (isFolders) "Criar pasta" else "Criar categoria", Modifier.testTag("organize-create")) { go(if (isFolders) "folder/new" else "category/new") }
                }
                Caption("${folders.size} ${if (folders.size == 1) "pasta" else "pastas"} · ${categories.size} ${if (categories.size == 1) "categoria" else "categorias"} · ${pool.size} ${if (pool.size == 1) "PDF pessoal" else "PDFs pessoais"}", Modifier.padding(top = 6.dp, bottom = 8.dp).testTag("organize-counters"))
                if (current.isEmpty()) Caption(if (isFolders) "Nenhuma pasta criada. Crie uma para agrupar PDFs sem duplicá-los." else "Nenhuma categoria criada. Crie uma para classificar seus PDFs.")
            }
        }
        items(current, key = { it.id }) { g ->
            var menuOpen by remember { mutableStateOf(false) }
            val count = if (isFolders) folderCount[g.id] ?: 0 else categoryCount[g.id] ?: 0
            Row(Modifier.fillMaxWidth().testTag("group-${g.id}").clip(RoundedCornerShape(14.dp)).clickable { filter = if (filter == g.id) "" else g.id; selected = emptySet() }
                .background(if (filter == g.id) LocalVisual.current.secondary else androidx.compose.ui.graphics.Color.Transparent).padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isFolders) Glyph(g.icon, Modifier.size(20.dp)) else Text("#", color = Ink, fontSize = 18.sp)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(g.name, color = Ink, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Caption("$count ${if (count == 1) "PDF" else "PDFs"}")
                }
                Box {
                    IconButton({ menuOpen = true }, Modifier.size(44.dp).semanticsLabel("Opções de ${g.name}")) { Glyph("more") }
                    DropdownMenu(menuOpen, { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Adicionar ou remover PDFs") }, onClick = { menuOpen = false; membersOf = g })
                        DropdownMenuItem(text = { Text(if (isFolders) "Editar pasta" else "Renomear") }, onClick = { menuOpen = false; if (isFolders) go("folder/${g.id}/edit") else renaming = g })
                        if (isFolders) {
                            DropdownMenuItem(text = { Text("Renomear") }, onClick = { menuOpen = false; renaming = g })
                            DropdownMenuItem(text = { Text("Escolher ou alterar capa") }, onClick = { menuOpen = false; coverGroup = g.id; picker.launch(arrayOf("image/*")) })
                        }
                        DropdownMenuItem(text = { Text(if (isFolders) "Excluir pasta" else "Excluir categoria") }, onClick = { menuOpen = false; deleting = g })
                    }
                }
            }
            HorizontalDivider(color = LocalVisual.current.border.copy(alpha = .6f))
        }
        item("tools") {
            Column(Modifier.padding(top = 20.dp)) {
                HorizontalDivider(color = LocalVisual.current.border)
                CardTitle("Documentos", Modifier.padding(top = 16.dp))
                Caption("${shown.size} ${if (shown.size == 1) "PDF" else "PDFs"} ${if (filter.isEmpty() && query.isBlank()) "pessoais" else "no filtro atual"}", Modifier.padding(bottom = 8.dp).testTag("organize-doc-count"))
                SoftField(query, { query = it }, Modifier.fillMaxWidth().testTag("organize-search"), singleLine = true, placeholder = { Text("Buscar por título") },
                    trailingIcon = { if (query.isNotEmpty()) IconButton({ query = "" }) { Glyph("close", Modifier.size(18.dp)) } })
                FlowRow(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(filter.isEmpty(), { filter = ""; selected = emptySet() }, { Text("Todos · ${pool.size}", fontSize = 13.sp) }, Modifier.testTag("filter-all"))
                    FilterChip(filter == "none", { filter = "none"; selected = emptySet() }, { Text(if (isFolders) "Sem pasta" else "Sem categoria", fontSize = 13.sp) }, Modifier.testTag("filter-none"))
                    current.find { it.id == filter }?.let { g -> FilterChip(true, { filter = "" }, { Text(g.name, fontSize = 13.sp) }, trailingIcon = { Glyph("close", Modifier.size(14.dp)) }) }
                    FilterChip(sort == "Título", { sort = "Título" }, { Text("A–Z", fontSize = 13.sp) })
                    FilterChip(sort == "Recentes", { sort = "Recentes" }, { Text("Recentes", fontSize = 13.sp) })
                }
                // Selection and batch actions
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${validSelection.size} ${if (validSelection.size == 1) "selecionado" else "selecionados"}", Modifier.weight(1f).testTag("organize-selected"), color = Ink, fontSize = 14.sp)
                    TextButton({ selected = selected + shown.map { it.id } }, Modifier.testTag("organize-all")) { Text("Selecionar todos", fontSize = 13.sp) }
                    TextButton({ selected = selected - shown.map { it.id }.toSet() }, Modifier.testTag("organize-none")) { Text("Desmarcar todos", fontSize = 13.sp) }
                }
                if (validSelection.isNotEmpty()) FlowRow(Modifier.fillMaxWidth().testTag("organize-batch"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlineAction(if (isFolders) "Adicionar a pasta" else "Adicionar a categoria", glyph = "add", enabled = current.isNotEmpty()) { batch = BatchAction.ADD }
                    OutlineAction("Remover associação", glyph = null, enabled = current.isNotEmpty()) { batch = BatchAction.REMOVE }
                }
                notice?.let { Caption(it, Modifier.padding(top = 8.dp).testTag("organize-notice")) }
                if (pool.isEmpty()) EmptyLibraryState("Nenhum PDF pessoal", "Importe um PDF para organizá-lo em pastas e categorias.")
                else if (shown.isEmpty()) Caption("Nenhum PDF encontrado neste filtro.", Modifier.padding(vertical = 12.dp))
            }
        }
        items(shown, key = { "doc-${it.id}" }) { book ->
            var menuOpen by remember { mutableStateOf(false) }
            val checked = book.id in selected
            val folderSummary = folderNames[book.id].orEmpty(); val categorySummary = categoryNames[book.id].orEmpty()
            Row(Modifier.fillMaxWidth().testTag("doc-${book.id}").clickable { selected = if (checked) selected - book.id else selected + book.id }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked, { selected = if (it) selected + book.id else selected - book.id }, Modifier.semanticsLabel("Selecionar ${ReadingRules.title(book.name)}"))
                Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                    Text(ReadingRules.title(book.name), color = Ink, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Caption("Pastas: ${folderSummary.joinToString().ifEmpty { "nenhuma" }} · Categorias: ${categorySummary.joinToString().ifEmpty { "nenhuma" }}")
                }
                Box {
                    IconButton({ menuOpen = true }, Modifier.size(44.dp).semanticsLabel("Ações de ${ReadingRules.title(book.name)}")) { Glyph("more") }
                    DropdownMenu(menuOpen, { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Abrir PDF") }, onClick = { menuOpen = false; open(book.id) })
                        DropdownMenuItem(text = { Text("Associar a pastas") }, onClick = { menuOpen = false; assigning = book; assignKind = "folders" })
                        DropdownMenuItem(text = { Text("Associar a categorias") }, onClick = { menuOpen = false; assigning = book; assignKind = "categories" })
                        DropdownMenuItem(text = { Text("Remover todas as associações") }, onClick = { menuOpen = false; clearing = book })
                    }
                }
            }
            HorizontalDivider(color = LocalVisual.current.border.copy(alpha = .6f))
        }
    }
    }
    // ---- Dialogs ----
    batch?.let { action ->
        var target by remember { mutableStateOf(current.firstOrNull { it.id == filter }?.id ?: current.firstOrNull()?.id ?: "") }
        val ids = validSelection; val group = current.firstOrNull { it.id == target }
        AlertDialog(onDismissRequest = { batch = null }, title = { Text(if (action == BatchAction.ADD) "Adicionar ${ids.size} ${if (ids.size == 1) "PDF" else "PDFs"}" else "Remover associação de ${ids.size} ${if (ids.size == 1) "PDF" else "PDFs"}") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Caption(if (isFolders) "Escolha a pasta." else "Escolha a categoria.")
                current.forEach { g -> Row(Modifier.fillMaxWidth().clickable { target = g.id }, verticalAlignment = Alignment.CenterVertically) { RadioButton(target == g.id, { target = g.id }); Text(g.name, color = Ink) } }
                Caption(if (action == BatchAction.ADD) "Os PDFs não são copiados nem alterados." else "Apenas o vínculo é removido. Os PDFs permanecem na biblioteca.", Modifier.padding(top = 8.dp))
            }
        }, confirmButton = { TextButton({
            val g = group ?: return@TextButton
            batch = null
            vm.run {
                if (action == BatchAction.ADD) {
                    val created = if (isFolders) vm.repository.addBooksToFolder(g.id, ids) else vm.repository.addBooksToCategory(g.id, ids)
                    notice = "${ids.size} ${if (ids.size == 1) "PDF associado" else "PDFs associados"} a ${g.name}" + if (created < ids.size) " (${ids.size - created} já estavam)." else "."
                } else {
                    if (isFolders) vm.repository.removeBooksFromFolder(g.id, ids) else vm.repository.removeBooksFromCategory(g.id, ids)
                    notice = "Associação com ${g.name} removida de ${ids.size} ${if (ids.size == 1) "PDF" else "PDFs"}."
                }
                selected = emptySet()
            }
        }, enabled = group != null) { Text("Confirmar") } }, dismissButton = { TextButton({ batch = null }) { Text("Cancelar") } })
    }
    assigning?.let { book ->
        if (assignKind == "folders") AssignFoldersDialog(setOf(book.id), vm) { assigning = null } else AssignCategoriesDialog(setOf(book.id), vm, { assigning = null }, { go("category/new") })
    }
    clearing?.let { book ->
        AlertDialog(onDismissRequest = { clearing = null }, title = { Text("Remover associações?") }, text = { Text("${ReadingRules.title(book.name)} deixa todas as pastas e categorias. O PDF e seu progresso permanecem.") },
            confirmButton = { TextButton({ val id = book.id; clearing = null; vm.run { vm.repository.setFolders(id, emptySet()); vm.repository.setBookCategories(id, emptySet()) } }) { Text("Remover") } }, dismissButton = { TextButton({ clearing = null }) { Text("Cancelar") } })
    }
    membersOf?.let { g ->
        val initial = if (g.kind == "folders") folderLinks.filter { it.folderId == g.id }.map { it.bookId }.toSet() else categoryLinks.filter { it.categoryId == g.id }.map { it.bookId }.toSet()
        MembersDialog("PDFs em ${g.name}", pool, initial, { membersOf = null }) { ids ->
            vm.run { if (g.kind == "folders") vm.repository.setFolderMembers(g.id, ids + folderLinks.filter { it.folderId == g.id }.map { it.bookId }.toSet().filter { id -> pool.none { it.id == id } })
                else vm.repository.setCategoryMembers(g.id, ids + categoryLinks.filter { it.categoryId == g.id }.map { it.bookId }.toSet().filter { id -> pool.none { it.id == id } }) }
        }
    }
    renaming?.let { RenameGroupDialog(it, vm) { renaming = null } }
    deleting?.let { g -> if (g.kind == "folders") DeleteFolderDialog(g, vm) { deleting = null; if (filter == g.id) filter = "" } else DeleteCategoryDialog(g, vm) { deleting = null; if (filter == g.id) filter = "" } }
    if (coverUri != null) AlertDialog(onDismissRequest = { coverUri = null }, title = { Text("Enquadramento horizontal") }, text = { Column {
        Caption("A imagem mantém suas proporções. Ajuste a região do recorte: início, centro ou fim.")
        FolderPreview(coverUri!!, crop)
        Slider(crop, { crop = it }, valueRange = 0f..1f)
    } }, confirmButton = { TextButton({ val uri = coverUri!!; val group = coverGroup; val position = crop; coverUri = null; vm.run { vm.repository.cover(group, uri, position); vm.coverRevision.value++ } }) { Text("Salvar capa") } }, dismissButton = { TextButton({ coverUri = null }) { Text("Cancelar") } })
}

@Composable
fun GroupSelect(label: String, value: String, groups: List<Group>, select: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Caption(label)
        Box {
            OutlinedButton(
                { expanded = true },
                Modifier.fillMaxWidth(),
                ) {
                Text(
                    groups.find { it.id == value }?.name
                        ?: if (label == "Pasta") "Sem pasta" else "Sem categoria",
                    Modifier.weight(1f),
                    color = Ink,
                )
                Text("⌄", color = Ink)
            }
            DropdownMenu(expanded, { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(if (label == "Pasta") "Sem pasta" else "Sem categoria") },
                    onClick = {
                        select("")
                        expanded = false
                    },
                )
                if (groups.isEmpty()) DropdownMenuItem(
                    text = { Text("Nenhuma categoria cadastrada") },
                    onClick = {}, enabled = false,
                )
                groups.forEach { g ->
                    DropdownMenuItem(
                        text = { Text(g.name) },
                        onClick = {
                            select(g.id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun HeatMap(days: List<Day>, sessions: List<ReadingSession> = emptyList()) {
    var period by rememberSaveable { mutableStateOf("Mês") }
    var detail by rememberSaveable {
        mutableStateOf("Toque em um dia para ver o tempo de leitura.")
    }
    val end = LocalDate.now()
    val start =
        when (period) {
            "Semana" -> end.minusDays(6)
            "Ano" -> end.minusYears(1)
            else -> end.minusDays(29)
        }
    val first = start.minusDays((start.dayOfWeek.value - 1).toLong())
    val values = days.associateBy { it.date }
    val total = (java.time.temporal.ChronoUnit.DAYS.between(first, end) + 1).toInt()
    val columns = (total + 6) / 7
    val heatScroll = rememberScrollState()
    LaunchedEffect(period, first, end, columns) {
        if (period == "Ano") {
            androidx.compose.runtime.snapshotFlow { heatScroll.maxValue }.first { it > 0 && it < Int.MAX_VALUE }
            heatScroll.scrollTo(heatScroll.maxValue)
        } else heatScroll.scrollTo(0)
    }
    val dark = LocalDark.current
    val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    Glass(Modifier.fillMaxWidth()) {
        CardTitle("Seu ritmo de leitura")
        Segments(listOf("Semana", "Mês", "Ano"), period) { period = it }
        Caption("${start.format(fmt)} – ${end.format(fmt)}")
        val periodDays = days.filter { it.date >= start.toString() && it.date <= end.toString() }
        val millis = periodDays.sumOf { it.millis }
        val countDays = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1
        val periodSessions = sessions.filter { java.time.Instant.ofEpochMilli(it.started).atZone(java.time.ZoneId.systemDefault()).toLocalDate() in start..end }
        Caption("Tempo total: ${millis / 3600000}h ${millis % 3600000 / 60000}min")
        Caption("Média diária: ${millis / countDays / 60000} min · Dias ativos: ${periodDays.count { it.millis > 0 }}")
        Caption(if (periodSessions.isEmpty()) "Páginas por sessão: sem sessões detalhadas registradas nesta versão." else "Páginas por sessão: ${"%.1f".format(periodSessions.sumOf { it.visits }.toFloat() / periodSessions.size)} · ${periodSessions.size} sessões")
        Row(
            Modifier.fillMaxWidth()
                .testTag("activity-heatmap").horizontalScroll(heatScroll)
                .padding(vertical = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(columns) { col ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(7) { row ->
                        val day = first.plusDays((col * 7 + row).toLong())
                        if (day < start || day > end) Spacer(Modifier.size(16.dp))
                        else {
                            val seconds = (values[day.toString()]?.millis ?: 0) / 1000
                            val level = ReadingRules.level(seconds)
                            val palette =
                                if (dark)
                                    listOf(
                                        0xff333333,
                                        0xff555555,
                                        0xff888888,
                                        0xffbbbbbb,
                                        0xffeeeeee,
                                    )
                                else
                                    listOf(
                                        0xffdddddd,
                                        0xffaaaaaa,
                                        0xff777777,
                                        0xff555555,
                                        0xff222222,
                                    )
                            val text = "${day.format(fmt)} · ${seconds/60} min ${seconds%60} s"
                            Box(
                                Modifier.size(16.dp)
                                    .background(Color(palette[level]), RoundedCornerShape(3.dp))
                                    .clickable { detail = text }
                                    .semanticsLabel(text)
                            )
                        }
                    }
                }
            }
        }
        Caption(detail)
        Caption("Menos ░ ▒ ▓ █ Mais · intensidade pelo tempo", Modifier.padding(top = 12.dp))
        Caption(
            "O histórico diário desta instalação registra apenas leitura real.",
            Modifier.padding(top = 10.dp),
        )
    }
}
