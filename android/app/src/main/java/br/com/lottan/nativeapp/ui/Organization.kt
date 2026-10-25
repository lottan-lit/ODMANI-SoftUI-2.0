package br.com.lottan.nativeapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import br.com.lottan.nativeapp.data.*
import br.com.lottan.nativeapp.domain.ReadingRules
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun OrganizeScreen(
    vm: LibraryViewModel,
    books: List<Book>,
    groups: List<Group>,
    back: () -> Unit,
    menu: () -> Unit,
    open: (String) -> Unit,
) {
    val revision by vm.coverRevision.collectAsState()
    var coverUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var crop by remember { mutableFloatStateOf(.5f) }
    var coverGroup by remember { mutableStateOf("") }
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { coverUri = it; crop = .5f }
    val memberships by vm.memberships.collectAsState()
    var rename by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var kind by rememberSaveable { mutableStateOf("Pastas") }
    var filter by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    val key = if (kind == "Pastas") "folders" else "categories"
    val current = groups.filter { it.kind == key }
    Page {
        Top("Organizar PDFs", back, menu)
        Caption("Organização local. Seus arquivos originais permanecem intactos.")
        Segments(listOf("Pastas", "Categorias"), kind) {
            kind = it
            filter = ""
        }
        OutlinedTextField(
            name,
            { name = it.take(60) },
            Modifier.fillMaxWidth(),
            label = { Text(if (kind == "Pastas") "Nova pasta" else "Nova categoria") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(10.dp))
        Primary(
            if (kind == "Pastas") "＋ Criar pasta" else "＋ Criar categoria",
            {
                vm.group(name, key)
                name = ""
            },
            name.isNotBlank(),
        )
        TextButton({ filter = "" }) { Text("Todos · ${books.size}", color = Ink) }
        current.chunked(2).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 10.dp),
            ) {
                row.forEach { g ->
                    Glass(Modifier.weight(1f).clickable { filter = g.id }) {
                        if (kind == "Pastas") FolderCover(g.id, revision)
                        else Text("#", color = Ink, fontSize = 22.sp)
                        Text(g.name, color = Ink, fontSize = 14.sp)
                        Caption(
                            "${books.count{if(key=="folders")memberships.any { m -> m.bookId == it.id && m.folderId == g.id } else it.category==g.id}} PDFs"
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (filter.isNotEmpty() && kind == "Pastas") TextButton({ coverGroup = filter; picker.launch(arrayOf("image/*")) }) { Text("Escolher / alterar capa") }
        if (filter.isNotEmpty()) TextButton({ newName = current.find { it.id == filter }?.name ?: ""; rename = true }) { Text("Renomear grupo") }
        if (filter.isNotEmpty())
            TextButton({
                val groupId = filter
                vm.run { vm.repository.removeGroup(groupId) }
                filter = ""
            }) {
                Text("Excluir grupo (preserva PDFs)", color = Ink)
            }
        Text(
            current.find { it.id == filter }?.name ?: "Seus PDFs",
            Modifier.padding(vertical = 20.dp),
            fontSize = 15.sp,
            color = Ink,
        )
        if (filter.isNotEmpty()) TextButton({ adding = !adding }) { Text(if (adding) "Ver documentos do grupo" else "Adicionar documentos") }
        val list =
            books.filter {
                filter.isEmpty() || adding ||
                    if (key == "folders") memberships.any { m -> m.bookId == it.id && m.folderId == filter } else it.category == filter
            }
        if (list.isEmpty()) {
            if (filter.isNotEmpty()) EmptyLibraryState(if (kind == "Pastas") "Pasta vazia" else "Categoria vazia", "Adicione documentos pelo gerenciamento deste grupo. Seus PDFs permanecem na Biblioteca.")
            else Caption("Nenhum PDF neste grupo. Importe um arquivo ou organize pela opção Todos.")
        }
        if (filter.isNotEmpty() && key == "folders" && !adding) {
            list.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { book -> Column(Modifier.weight(1f)) {
                        PdfCover(book, Modifier.fillMaxWidth().aspectRatio(.7f).clickable { open(book.id) })
                        Text(ReadingRules.title(book.name), color = Ink, maxLines = 2)
                        TextButton({ vm.run { vm.repository.dao.removeFolder(filter, book.id) } }) { Text("Remover da pasta") }
                    } }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else {
        list.forEach { book ->
            Column(Modifier.padding(bottom = 22.dp)) {
                Text(ReadingRules.title(book.name), fontSize = 14.sp, color = Ink)
                groups.filter { it.kind == "folders" }.forEach { folder ->
                    val member = memberships.any { it.bookId == book.id && it.folderId == folder.id }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(member, { selected -> vm.run {
                            if (selected) vm.repository.dao.addFolder(FolderBook(folder.id, book.id))
                            else vm.repository.dao.removeFolder(folder.id, book.id)
                        } })
                        Text(folder.name, color = Ink)
                    }
                }
                GroupSelect("Categoria", book.category, groups.filter { it.kind == "categories" }) {
                    vm.run { vm.repository.assign(book.id, "categories", it) }
                }
                TextButton({ open(book.id) }) { Text("Abrir PDF →", color = Ink) }
            }
        }
        }
    }
    if (coverUri != null) AlertDialog(onDismissRequest = { coverUri = null }, title = { Text("Enquadramento horizontal") }, text = { Column {
        Caption("A imagem mantém suas proporções. Ajuste a região do recorte: início, centro ou fim.")
        FolderPreview(coverUri!!, crop)
        Slider(crop, { crop = it }, valueRange = 0f..1f)
    } }, confirmButton = { TextButton({ val uri = coverUri!!; val group = coverGroup; val position = crop; coverUri = null; vm.run { vm.repository.cover(group, uri, position); vm.coverRevision.value++ } }) { Text("Salvar capa") } }, dismissButton = { TextButton({ coverUri = null }) { Text("Cancelar") } })
    if (rename) AlertDialog(onDismissRequest = { rename = false }, title = { Text("Renomear grupo") }, text = { OutlinedTextField(newName, { newName = it.take(60) }) }, confirmButton = { TextButton({ vm.run { vm.repository.dao.renameGroup(filter, newName.trim()) }; rename = false }, enabled = newName.isNotBlank()) { Text("Salvar") } }, dismissButton = { TextButton({ rename = false }) { Text("Cancelar") } })
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
                shape = RoundedCornerShape(12.dp),
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
    val dark = LocalDark.current
    val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    Glass(Modifier.fillMaxWidth()) {
        Text("Seu ritmo de leitura", fontSize = 15.sp, color = Ink)
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
                .horizontalScroll(rememberScrollState())
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
