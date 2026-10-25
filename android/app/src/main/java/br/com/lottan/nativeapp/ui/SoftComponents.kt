package br.com.lottan.nativeapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import br.com.lottan.nativeapp.data.*
import br.com.lottan.nativeapp.domain.ReadingRules
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Reusable surfaces; no blur or filter is ever applied to PDF pixels. */
@Composable
fun SoftCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.glass(LocalDark.current, 24).padding(16.dp), content = content)
}

@Composable
fun SoftButton(label: String, enabled: Boolean = true, action: () -> Unit) {
    Button(
        onClick = action,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(label, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SoftIconButton(icon: String, label: String, action: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(48.dp)
                .glass(LocalDark.current, 24)
                .clickable(onClick = action)
                .semantics {
                    contentDescription = label
                    role = Role.Button
                },
            contentAlignment = Alignment.Center,
        ) {
            Glyph(icon, Modifier.requiredSize(22.dp))
        }
        Text(label, Modifier.padding(top = 8.dp), color = Muted, fontSize = 12.sp)
    }
}

@Composable
fun ReadingProgress(book: Book) {
    val fraction = ((if (book.lastOpened == 0L) 0f else book.page + 1f) / book.pages.coerceAtLeast(1)).coerceIn(0f, 1f)
    LinearProgressIndicator(
        progress = { fraction },
        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
        color = Ink,
        trackColor = LocalVisual.current.secondary,
    )
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Caption("${if (book.lastOpened == 0L) 0 else book.page + 1} de ${book.pages}")
        Caption("${(fraction * 100).toInt()}%")
    }
}

@Composable
fun EmptyLibraryState(title: String, description: String) {
    SoftCard(Modifier.fillMaxWidth()) {
        Text(title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Caption(description, Modifier.padding(top = 8.dp))
    }
}

@Composable
fun LibrarySection(
    title: String,
    description: String,
    action: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            Modifier.weight(1f),
            color = Ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        TextButton(onClick = action) {
            Text("Ver tudo", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.width(4.dp))
            Glyph("forward", Modifier.size(16.dp))
        }
    }
    Caption(description, Modifier.padding(bottom = 12.dp))
    content()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SoftHome(
    books: List<Book>,
    busy: Boolean,
    importPdf: () -> Unit,
    go: (String) -> Unit,
    workspace: Workspace,
    days: List<Day>,
    vm: LibraryViewModel,
    restart: (String) -> Unit,
) {
    val preferences by vm.prefs.collectAsState()
    Page {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "ODMANI",
                Modifier.weight(1f),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                color = Ink,
            )
            Icon("search", "Buscar") { go("search") }
            Icon("menu", "Abrir menu") { go("menu") }
        }
        val doc = books.firstOrNull { it.id == workspace.main } ?: books.maxByOrNull { it.lastOpened.takeIf { n -> n > 0 } ?: it.added }
        if (doc != null) {
            Text(
                "Continuar lendo",
                color = Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            SoftCard(Modifier.fillMaxWidth()) {
                BoxWithConstraints {
                    val narrow =
                        maxWidth < 310.dp ||
                            androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
                    if (narrow) {
                        Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PdfCover(doc, Modifier.width(76.dp).height(112.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                ContinueTitle(doc)
                                Caption("PDF · neste celular", Modifier.padding(top = 8.dp))
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        ReadingProgress(doc)
                        Spacer(Modifier.height(12.dp))
                        SoftButton(
                            if (doc.completed) "▶  Ler novamente" else "▶  Continuar"
                        ) {
                            if (doc.completed) restart(doc.id)
                            else go("reader/${doc.id}")
                        }
                        }
                    } else
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PdfCover(doc, Modifier.width(92.dp).height(140.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                ContinueTitle(doc)
                                Caption("PDF · neste celular", Modifier.padding(vertical = 8.dp))
                                ReadingProgress(doc)
                                Spacer(Modifier.height(12.dp))
                                SoftButton(
                                    if (doc.completed) "▶  Ler novamente"
                                    else "▶  Continuar"
                                ) {
                                    if (doc.completed) restart(doc.id)
                                    else go("reader/${doc.id}")
                                }
                            }
                        }
                }
            }
        } else {
            SoftCard(Modifier.fillMaxWidth()) {
                Text(
                    "Sua biblioteca começa aqui.",
                    color = Ink,
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Caption(
                    "Importe seu primeiro PDF e continue a leitura mesmo sem internet.",
                    Modifier.padding(vertical = 16.dp),
                )
                SoftButton("＋ Importar PDF", !busy, importPdf)
                TextButton({ go("explore") }) { Text("Explorar a Lottan") }
            }
        }
        if (preferences.showGoals) { Spacer(Modifier.height(24.dp)); GoalPanel(workspace, days) { go("goals") } }
        val pinned = books.filter { it.id in workspace.pinned }
        if (pinned.isNotEmpty()) LibrarySection("Fixados", "Seus documentos favoritos", { go("library?origin=pinned") }) { BookShelf(pinned.take(3)) { go("book/$it") } }
        val official = books.filter { it.origin == "lottan_verified" }
        LibrarySection(
            "Biblioteca Lottan",
            "Parceira editorial",
            { go("library?origin=lottan_verified") },
        ) {
            if (official.isEmpty())
                EmptyLibraryState(
                    "Conteúdo editorial em breve",
                    "Nenhum documento oficial da parceira disponível neste celular.",
                )
            else BookShelf(official.take(3)) { go("book/$it") }
        }
        val personal = books.filter { it.origin != "lottan_verified" }.sortedByDescending { maxOf(it.added, it.lastOpened) }
        FlowRow(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("PDFs pessoais", Modifier.widthIn(min = 120.dp), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            TextButton(importPdf, enabled = !busy, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("+ Adicionar", fontSize = 12.sp) }
            TextButton({ go("library?origin=personal") }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Ver tudo →", fontSize = 12.sp, color = Muted) }
        }
        if (personal.isEmpty()) Caption("Adicione seu primeiro PDF pelo botão + Adicionar.")
        else BookShelf(personal.take(3)) { go("book/$it") }
        Caption("Desenvolvido pela AUDMANI", Modifier.padding(top = 24.dp, bottom = 8.dp))
    }
}

@Composable
private fun ContinueTitle(book: Book) {
    Text(
        ReadingRules.title(book.name),
        color = Ink,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Medium,
        maxLines = if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f) 1 else 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun PersonalBookCard(book: Book, open: () -> Unit) {
    val enlarged = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
    val width = if (enlarged) 180.dp else 104.dp
    Column(Modifier.width(width).clickable(onClick = open)) {
        PdfCover(book, Modifier.width(width).height(if (enlarged) 200.dp else 144.dp))
        Text(
            ReadingRules.title(book.name),
            Modifier.padding(top = 8.dp),
            color = Ink,
            fontSize = 12.sp,
            maxLines = if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f) 1 else 2,
            overflow = TextOverflow.Ellipsis,
        )
        Caption("${if (book.lastOpened == 0L) 0 else book.page+1} de ${book.pages}")
    }
}

@Composable
fun NativeBookDetails(
    book: Book?,
    back: () -> Unit,
    go: (String) -> Unit,
    vm: LibraryViewModel,
    workspace: Workspace,
    restart: (String) -> Unit,
) {
    Page {
        var options by remember { mutableStateOf(false) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Icon("back", "Voltar", back)
            Box {
                TextButton({ options = true }, Modifier.semantics { contentDescription = "Opções do documento" }) { Text("⋮", color = Ink, fontSize = 24.sp) }
                DropdownMenu(options, { options = false }) {
                    if (book != null) {
                        DropdownMenuItem(text = { Text("Adicionar à pasta") }, onClick = { options = false; go("organize") })
                        DropdownMenuItem(text = { Text("Remover da biblioteca") }, onClick = { options = false; go("remove/${book.id}") })
                    }
                }
            }
        }
        if (book == null) {
            EmptyLibraryState("PDF indisponível", "Este documento pode ter sido removido.")
            return@Page
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PdfCover(book, Modifier.width(112.dp).height(164.dp))
            Column(Modifier.weight(1f)) {
                Text(ReadingRules.title(book.name), color = Ink, fontSize = 22.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
                Caption(if (book.origin == "lottan_verified") "Lottan" else "PDF pessoal · neste celular", Modifier.padding(vertical = 8.dp))
                SoftButton(if (book.completed) "Ler novamente" else if (book.lastOpened == 0L) "Ler" else "Continuar") {
                    if (book.completed) restart(book.id) else go("reader/${book.id}")
                }
            }
        }
        Caption("${book.pages} páginas  ·  PDF  ·  ${"%.1f".format(book.bytes / 1048576.0)} MB")
        Spacer(Modifier.height(20.dp))
        ReadingProgress(book)
        Row(
            Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SoftIconButton("library", "Organizar") { go("organize") }
            SoftIconButton("close", "Remover") { go("remove/${book.id}") }
        }
        DocumentActions(book, vm, workspace, go)
        if (book.completed)
            Caption(
                "Leitura concluída anteriormente. Seu histórico está preservado.",
                Modifier.padding(top = 24.dp),
            )
    }
}

@Composable
fun ReadingStatsCard(label: String, value: String, modifier: Modifier = Modifier) {
    SoftCard(modifier) {
        Text(value, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Caption(label, Modifier.padding(top = 4.dp))
    }
}

@Composable
fun SoftStats(
    books: List<Book>,
    days: List<Day>,
    groups: List<Group>,
    back: () -> Unit,
    menu: () -> Unit,
    workspace: Workspace,
    vm: LibraryViewModel,
    goals: () -> Unit,
) {
    val sessions by vm.sessions.collectAsState()
    val unique by vm.uniquePages.collectAsState()
    Page {
        Top("Estatísticas", back, menu, showMenu = true)
        val ms = days.sumOf { it.millis }
        Row(
            Modifier.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ReadingStatsCard(
                "Tempo de leitura",
                if (ms < 60000) "${ms / 1000}s" else "${ms/3600000}h ${ms%3600000/60000}min",
                Modifier.weight(1f),
            )
            ReadingStatsCard(
                "Páginas visitadas",
                "${days.sumOf { it.visits }}",
                Modifier.weight(1f),
            )
        }
        var metric by remember { mutableStateOf("Tempo") }
        SoftCard(Modifier.fillMaxWidth()) {
            Segments(listOf("Páginas", "Tempo"), metric) { metric = it }
            Text("Últimos 7 dias", color = Ink, fontWeight = FontWeight.Medium)
            val week = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
            val durations =
                week.map { date -> days.firstOrNull { it.date == date.toString() }?.let { if (metric == "Tempo") it.millis else it.visits } ?: 0L }
            val maximum = ReadingRules.chartMaximum(durations, metric == "Tempo")
            Caption(if (metric == "Tempo") "Escala: 0–${maximum / 60000} minutos" else "Escala: 0–$maximum páginas visitadas")
            Row(
                Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                week.forEachIndexed { index, date ->
                    Column(
                        Modifier.weight(1f).semantics {
                            contentDescription = if (metric == "Tempo") "$date: ${durations[index]/60000} minutos" else "$date: ${durations[index]} páginas visitadas"
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.height(88.dp).fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                Modifier.width(16.dp)
                                    .height(
                                        (if (durations[index] == 0L) 0f
                                            else
                                                (durations[index].toFloat() / maximum * 88f)
                                                    .coerceAtLeast(0f))
                                            .dp
                                    )
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (date == LocalDate.now()) Ink
                                        else Muted.copy(alpha = .45f)
                                    )
                            )
                        }
                        Caption(
                            date.dayOfWeek.getDisplayName(
                                TextStyle.NARROW,
                                Locale.forLanguageTag("pt-BR"),
                            ),
                            Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
            if (durations.all { it == 0L })
                Caption("Nenhuma leitura registrada nesta semana.", Modifier.padding(top = 12.dp))
        }
        Spacer(Modifier.height(24.dp))
        GoalPanel(workspace, days, goals)
        Spacer(Modifier.height(24.dp))
        HeatMap(days, sessions)
        Caption("$unique páginas únicas com permanência registrada nesta versão.", Modifier.padding(top = 12.dp))
        Row(
            Modifier.padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ReadingStatsCard("Concluídos", "${books.count { it.completed }}", Modifier.weight(1f))
            ReadingStatsCard("PDFs na biblioteca", "${books.size}", Modifier.weight(1f))
        }
        SoftCard(Modifier.fillMaxWidth()) {
            Text("Sua biblioteca por categoria", color = Ink, fontWeight = FontWeight.Medium)
            Caption(
                "Quantidade de PDFs · não representa tempo de leitura",
                Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            if (books.isEmpty()) Caption("Importe um PDF para começar.")
            else
                books
                    .groupingBy { b ->
                        groups.firstOrNull { it.id == b.category && it.kind == "categories" }?.name
                            ?: "Sem categoria"
                    }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .forEach { (name, count) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                name,
                                Modifier.weight(1f),
                                color = Ink,
                                fontSize = 14.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("$count", color = Muted, fontSize = 14.sp)
                        }
                        LinearProgressIndicator(
                            progress = { count.toFloat() / books.size },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = Ink,
                            trackColor = LocalVisual.current.secondary,
                        )
                    }
        }
        Caption(
            "O tempo é registrado somente com uma página renderizada e o leitor em primeiro plano. Nas novas leituras, cada página conta uma vez por dia, após 5 segundos. Dados locais neste celular.",
            Modifier.padding(top = 20.dp),
        )
    }
}
