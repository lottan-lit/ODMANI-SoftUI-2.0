package br.com.lottan.nativeapp.ui
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import br.com.lottan.nativeapp.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
private fun EditorialCover(publication: Publication, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null, publication.coverAsset) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                require(publication.coverAsset.startsWith("lottan/covers/") && !publication.coverAsset.contains(".."))
                context.assets.open(publication.coverAsset).use { android.graphics.BitmapFactory.decodeStream(it) }
            }.getOrNull()
        }
    }
    Box(modifier.background(LocalVisual.current.secondary), contentAlignment = Alignment.Center) {
        val image = bitmap
        if (image != null) Image(image.asImageBitmap(), "Capa oficial de ${publication.title}", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        else Caption("Capa indisponível")
    }
}
@Composable
fun EditorialExplore(catalog: EditorialCatalog, back: () -> Unit, menu: () -> Unit, open: (String) -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("Todos") }
    Page {
        Top("Explorar", back, menu, showMenu = true)
        Text("Biblioteca Lottan", color = Ink, fontSize = 30.sp)
        Caption("Editora parceira da ODMANI")
        if (catalog.publications.isEmpty()) {
            Spacer(Modifier.height(100.dp))
            EmptyLibraryState("Novas histórias estão a caminho.", "A Biblioteca Lottan ainda não possui publicações disponíveis. Em breve, você poderá descobrir novas obras por aqui.")
            if (catalog.officialUrl(catalog.siteUrl)) TextButton({ context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(catalog.siteUrl))) }) { Text("Conheça a Lottan") }
        } else {
            val highlight = catalog.publications.first()
            SoftCard(Modifier.fillMaxWidth().padding(vertical = 20.dp).clickable { open(highlight.id) }) {
                Caption("DESTAQUE EDITORIAL")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditorialCover(highlight, Modifier.width(96.dp).height(140.dp))
                    Column { Text(highlight.title, color = Ink, fontSize = 22.sp); Caption(highlight.author); Text(highlight.price, color = Ink); Caption(highlight.availability) }
                }
            }
            SoftField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Buscar título ou autor") }, singleLine = true)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                (listOf("Todos") + catalog.publications.map { it.genre }.filter { it.isNotBlank() }.distinct()).forEach { g -> FilterChip(selected = g == genre, onClick = { genre = g }, label = { Text(g) }) }
            }
            Text("Novidades", color = Ink, fontSize = 20.sp)
            catalog.publications.filter { (genre == "Todos" || it.genre == genre) && normalize(it.title + " " + it.author).contains(normalize(query)) }.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { p -> Column(Modifier.weight(1f).clickable { open(p.id) }) {
                        EditorialCover(p, Modifier.fillMaxWidth().aspectRatio(.7f)); Text(p.title, color = Ink); Caption(p.author); Text(p.price, color = Ink); Caption(p.availability)
                    } }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
@Composable
fun EditorialDetails(publication: Publication?, catalog: EditorialCatalog, back: () -> Unit) {
    val context = LocalContext.current
    Page {
        Top("Publicação Lottan", back, {})
        val p = publication
        if (p == null) { EmptyLibraryState("Publicação indisponível", "Esta obra não está no catálogo atual."); return@Page }
        EditorialCover(p, Modifier.fillMaxWidth().height(260.dp))
        Text(p.title, color = Ink, fontSize = 28.sp); Caption(p.author)
        Text(p.synopsis, color = Ink, modifier = Modifier.padding(vertical = 20.dp))
        listOf("Gênero" to p.genre, "Páginas" to p.pages.takeIf { it > 0 }?.toString().orEmpty(), "Idioma" to p.language, "Edição" to p.edition, "Publicação" to p.published, "ISBN" to p.isbn, "Informações adicionais" to p.additional).filter { it.second.isNotBlank() }.forEach { (label, value) -> Caption("$label: $value", Modifier.padding(vertical = 4.dp)) }
        Text(p.price, color = Ink, fontSize = 22.sp); Caption(p.availability)
        if (catalog.commercialLinksEnabled && catalog.officialUrl(p.productUrl)) SoftButton("Ver no site da Lottan") {
            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(p.productUrl)))
        }
    }
}
