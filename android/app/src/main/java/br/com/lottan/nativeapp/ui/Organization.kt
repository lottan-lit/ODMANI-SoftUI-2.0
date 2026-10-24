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
                        Text(if (kind == "Pastas") "▱" else "#", color = Ink, fontSize = 22.sp)
                        Text(g.name, color = Ink, fontSize = 14.sp)
                        Caption(
                            "${books.count{if(key=="folders")it.folder==g.id else it.category==g.id}} PDFs"
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (filter.isNotEmpty())
            TextButton({
                vm.run { vm.repository.removeGroup(filter) }
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
        val list =
            books.filter {
                filter.isEmpty() ||
                    if (key == "folders") it.folder == filter else it.category == filter
            }
        if (list.isEmpty())
            Caption("Nenhum PDF neste grupo. Importe um arquivo ou organize pela opção Todos.")
        list.forEach { book ->
            Column(Modifier.padding(bottom = 22.dp)) {
                Text(ReadingRules.title(book.name), fontSize = 14.sp, color = Ink)
                GroupSelect("Pasta", book.folder, groups.filter { it.kind == "folders" }) {
                    vm.run { vm.repository.assign(book.id, "folders", it) }
                }
                GroupSelect("Categoria", book.category, groups.filter { it.kind == "categories" }) {
                    vm.run { vm.repository.assign(book.id, "categories", it) }
                }
                TextButton({ open(book.id) }) { Text("Abrir PDF →", color = Ink) }
            }
        }
    }
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
fun HeatMap(days: List<Day>) {
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
