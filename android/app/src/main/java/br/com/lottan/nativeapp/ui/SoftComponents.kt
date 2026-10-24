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
    Column(modifier.glass(LocalDark.current, 20).padding(16.dp), content = content)
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
    val fraction = ((book.page + 1f) / book.pages.coerceAtLeast(1)).coerceIn(0f, 1f)
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
        Caption("${book.page + 1} de ${book.pages}")
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

@Composable
fun SoftHome(
    books: List<Book>,
    busy: Boolean,
    importPdf: () -> Unit,
    go: (String) -> Unit,
    restart: (String) -> Unit,
) {
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
        val doc = books.maxByOrNull { it.lastOpened.takeIf { n -> n > 0 } ?: it.added }
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
                            if (doc.page >= doc.pages - 1) "▶  Ler novamente" else "▶  Continuar"
                        ) {
                            if (doc.page >= doc.pages - 1) restart(doc.id)
                            else go("reader/${doc.id}")
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
                                    if (doc.page >= doc.pages - 1) "▶  Ler novamente"
                                    else "▶  Continuar"
                                ) {
                                    if (doc.page >= doc.pages - 1) restart(doc.id)
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
            }
        }
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
            else BookShelf(official) { go("book/$it") }
        }
        val personal = books.filter { it.origin != "lottan_verified" }
        LibrarySection(
            "PDFs pessoais",
            "Seus arquivos, organizados do seu jeito.",
            { go("library?origin=personal") },
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item("import") {
                    Column(
                        Modifier.width(104.dp)
                            .heightIn(min = 172.dp)
                            .glass(LocalDark.current)
                            .clickable(enabled = !busy, onClick = importPdf)
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("＋", color = Ink, fontSize = 28.sp)
                        Text(
                            if (busy) "Importando…" else "Importar PDF",
                            color = Ink,
                            fontSize = 14.sp,
                        )
                    }
                }
                items(personal, key = { it.id }) { b -> PersonalBookCard(b) { go("book/${b.id}") } }
            }
            if (personal.isEmpty())
                Caption("Os PDFs que você importar aparecerão aqui.", Modifier.padding(top = 12.dp))
        }
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
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun PersonalBookCard(book: Book, open: () -> Unit) {
    Column(Modifier.width(104.dp).clickable(onClick = open)) {
        PdfCover(book, Modifier.width(104.dp).height(144.dp))
        Text(
            ReadingRules.title(book.name),
            Modifier.padding(top = 8.dp),
            color = Ink,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Caption("${book.page+1} de ${book.pages}")
    }
}

@Composable
fun NativeBookDetails(
    book: Book?,
    back: () -> Unit,
    go: (String) -> Unit,
    restart: (String) -> Unit,
) {
    Page {
        Top("Documento", back, { go("menu") })
        if (book == null) {
            EmptyLibraryState("PDF indisponível", "Este documento pode ter sido removido.")
            return@Page
        }
        PdfCover(
            book,
            Modifier.align(Alignment.CenterHorizontally)
                .padding(vertical = 20.dp)
                .widthIn(max = 200.dp)
                .fillMaxWidth()
                .height(272.dp),
        )
        Text(
            ReadingRules.title(book.name),
            color = Ink,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Caption(
            if (book.origin == "lottan_verified") "Parceira editorial — Lottan"
            else "PDF pessoal · salvo neste celular",
            Modifier.padding(top = 8.dp, bottom = 20.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ReadingStatsCard("Páginas", "${book.pages}", Modifier.weight(1f))
            ReadingStatsCard("Formato", "PDF", Modifier.weight(1f))
            ReadingStatsCard(
                "Tamanho",
                "${"%.1f".format(book.bytes/1048576.0)} MB",
                Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(20.dp))
        ReadingProgress(book)
        Spacer(Modifier.height(16.dp))
        SoftButton(
            if (book.page >= book.pages - 1) "▶  Ler novamente" else "▶  Continuar leitura"
        ) {
            if (book.page >= book.pages - 1) restart(book.id) else go("reader/${book.id}")
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SoftIconButton("library", "Organizar") { go("organize") }
            SoftIconButton("close", "Remover") { go("remove/${book.id}") }
        }
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
) {
    Page {
        Top("Estatísticas", back, menu)
        val ms = days.sumOf { it.millis }
        Row(
            Modifier.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ReadingStatsCard(
                "Tempo de leitura",
                "${ms/3600000}h ${ms%3600000/60000}min",
                Modifier.weight(1f),
            )
            ReadingStatsCard(
                "Páginas visitadas",
                "${days.sumOf { it.visits }}",
                Modifier.weight(1f),
            )
        }
        SoftCard(Modifier.fillMaxWidth()) {
            Text("Últimos 7 dias", color = Ink, fontWeight = FontWeight.Medium)
            val week = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
            val durations =
                week.map { date -> days.firstOrNull { it.date == date.toString() }?.millis ?: 0L }
            val maximum = durations.maxOrNull()?.coerceAtLeast(1L) ?: 1L
            Row(
                Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                week.forEachIndexed { index, date ->
                    Column(
                        Modifier.weight(1f).semantics {
                            contentDescription = "$date: ${durations[index]/60000} minutos"
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
                                        (if (durations[index] == 0L) 3f
                                            else
                                                (durations[index].toFloat() / maximum * 88f)
                                                    .coerceAtLeast(8f))
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
        Spacer(Modifier.height(20.dp))
        HeatMap(days)
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
            "O tempo é registrado somente com uma página renderizada e o leitor em primeiro plano. Páginas revisitadas também contam. Dados locais neste celular.",
            Modifier.padding(top = 20.dp),
        )
    }
}
