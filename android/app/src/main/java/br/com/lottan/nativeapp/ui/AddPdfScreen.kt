package br.com.lottan.nativeapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.lottan.nativeapp.data.*

@Composable
fun AddPdfScreen(vm: LibraryViewModel, select: () -> Unit, back: () -> Unit, go: (String) -> Unit) {
    val preview by vm.importPreview.collectAsState(); val folders by vm.groups.collectAsState()
    val selected by vm.importFolders.collectAsState(); val busy by vm.busy.collectAsState(); val imported by vm.importedBook.collectAsState()
    var discarding by remember { mutableStateOf(false) }
    fun leave() { if (preview != null || busy) discarding = true else back() }
    BackHandler(preview != null || busy) { leave() }
    Page {
        Top("Adicionar PDF", { leave() }, { go("menu") })
        Caption("Selecione um PDF, confira os dados e escolha como organizá-lo.", Modifier.padding(vertical = 12.dp))
        SoftCard(Modifier.fillMaxWidth()) {
            Glyph("document", Modifier.size(32.dp))
            Text("PDFs do seu dispositivo", color = Ink)
            Caption("PDF de até 200 MB, sem senha. O arquivo original será preservado.")
            TextButton(select, enabled = !busy) { Text(if (preview == null) "Selecionar PDF" else "Selecionar outro PDF") }
        }
        preview?.let { value ->
            Spacer(Modifier.height(16.dp))
            SoftCard(Modifier.fillMaxWidth()) {
                Text("Prévia do documento", color = Ink)
                Text(value.name, color = Ink)
                val size = if (value.bytes >= 1_048_576L) "%.1f MB".format(value.bytes / 1_048_576.0) else "${maxOf(1L, value.bytes / 1024)} KB"
                Caption("${value.pages} páginas · $size")
                Caption("PDF validado para abertura. A importação será concluída somente ao confirmar.")
            }
            Text("Organização inicial", color = Ink, modifier = Modifier.padding(top = 16.dp))
            Caption("Escolha zero, uma ou várias pastas. Uma única cópia do PDF será armazenada.")
            FolderSelection(folders, selected, { id, checked -> vm.importFolders.value = if (checked) selected + id else selected - id }, { go("folder/new") })
            Primary("Concluir importação", { vm.finishImport() }, !busy)
            TextButton({ discarding = true }, enabled = !busy) { Text("Cancelar seleção") }
        }
        if (preview == null) imported?.let { book ->
            Spacer(Modifier.height(16.dp))
            SoftCard(Modifier.fillMaxWidth()) {
                Text("PDF importado", color = Ink)
                Text(book.name, color = Ink)
                TextButton({ go("book/${book.id}") }) { Text("Ver detalhes do documento") }
            }
        }
    }
    if (discarding) AlertDialog(onDismissRequest = { discarding = false }, title = { Text("Descartar seleção?") }, text = { Text("O PDF ainda não importado será descartado da área temporária. O original será preservado.") }, confirmButton = { TextButton({ vm.discardImport(); discarding = false; back() }) { Text("Descartar seleção") } }, dismissButton = { TextButton({ discarding = false }) { Text("Continuar aqui") } })
}
