package br.com.lottan.nativeapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import br.com.lottan.nativeapp.data.Book

/** Lazy composition bounds the number of displayed bitmaps in addition to the thumbnail cache. */
@Composable
fun LibraryScreen(
    books: List<Book>,
    busy: Boolean,
    importPdf: () -> Unit,
    back: () -> Unit,
    go: (String) -> Unit,
    origin: String = "all",
) {
    var selected by rememberSaveable(origin) { mutableStateOf(origin) }
    val shown =
        books.filter {
            selected == "all" ||
                if (selected == "lottan_verified") it.origin == selected
                else it.origin != "lottan_verified"
        }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = VisualTokens.ScreenInset, vertical = 14.dp),
    ) {
        item("heading") {
            Top("Biblioteca", back, { go("menu") })
            val choices = listOf("Todos", "Pessoais", "Lottan")
            Segments(
                choices,
                choices[
                    listOf("all", "personal", "lottan_verified").indexOf(selected).coerceAtLeast(0)],
            ) {
                selected = listOf("all", "personal", "lottan_verified")[choices.indexOf(it)]
            }
            Spacer(Modifier.height(12.dp))
            Primary("▥ Pastas e categorias") { go("organize") }
            Caption(
                "${shown.size} PDF(s) salvo(s) neste celular",
                Modifier.padding(vertical = 14.dp),
            )
            Primary("＋ Importar PDF", importPdf, !busy)
        }
        items(shown, key = { it.id }) { doc ->
            DocumentRow(doc, { go("book/${doc.id}") }, { go("remove/${doc.id}") })
        }
        if (shown.isEmpty())
            item("empty") {
                Spacer(Modifier.height(16.dp))
                EmptyLibraryState(
                    if (selected == "lottan_verified") "Biblioteca Lottan"
                    else "Sua biblioteca está pronta",
                    if (selected == "lottan_verified")
                        "Nenhum conteúdo editorial verificado disponível. Seus arquivos pessoais continuam na aba Pessoais."
                    else
                        "Importe um PDF para começar. Você pode organizar os arquivos em pastas e categorias.",
                )
            }
    }
}

@Composable
fun SearchScreen(books: List<Book>, back: () -> Unit, go: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val found =
        remember(books, query) { books.filter { normalize(it.name).contains(normalize(query)) } }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = VisualTokens.ScreenInset, vertical = 14.dp),
    ) {
        item("search") {
            Top("Buscar", back, { go("menu") })
            OutlinedTextField(
                query,
                { query = it },
                Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar pelo nome do PDF...") },
                singleLine = true,
                shape = CircleShape,
            )
            Caption("Seus PDFs", Modifier.padding(vertical = 20.dp))
        }
        items(found, key = { it.id }) { doc -> DocumentRow(doc, { go("book/${doc.id}") }, null) }
        if (found.isEmpty()) item("empty") { Caption("Nenhum PDF encontrado.") }
        item("demonstration") { Caption("Demonstração", Modifier.padding(vertical = 20.dp)) }
        items(
            demoNames.withIndex().filter { normalize(it.value).contains(normalize(query)) },
            key = { "demo-${it.index}" },
        ) { item ->
            TextButton({ go("detail/${item.index}") }) { Text(item.value, color = Ink) }
        }
    }
}
