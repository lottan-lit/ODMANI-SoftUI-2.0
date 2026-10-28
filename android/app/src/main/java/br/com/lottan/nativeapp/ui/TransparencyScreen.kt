package br.com.lottan.nativeapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.lottan.nativeapp.BuildConfig
import br.com.lottan.nativeapp.data.*
import kotlinx.coroutines.CancellationException

@Composable
fun TransparencyScreen(back: () -> Unit, returnToImport: (() -> Unit)? = null) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var problem by remember { mutableStateOf(false) }
    val document by produceState<TransparencyDocument?>(null) {
        try { value = loadTransparency(context) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { problem = true }
    }
    Page {
        Top("Transparência e responsabilidade", back, back)
        if (document == null) Caption(if (problem) "Não foi possível carregar o documento local." else "Carregando documento local…")
        document?.let { doc ->
            Text(doc.description, color = Ink, fontSize = 16.sp, lineHeight = 24.sp, modifier = Modifier.padding(vertical = 16.dp))
            SoftCard(Modifier.fillMaxWidth()) {
                Text(doc.status, color = Ink, fontWeight = FontWeight.SemiBold)
                Caption("Documento ${doc.id}-${doc.version} · preparado em ${doc.prepared}")
                Caption("Vigência: ${doc.effective}")
                Caption("Texto integral disponível offline. Campos institucionais e condições editoriais pendentes estão identificados na documentação.")
            }
            Spacer(Modifier.height(12.dp))
            doc.sections.forEachIndexed { index, section ->
                key(section.id) {
                    var expanded by rememberSaveable(doc.version, section.id) { mutableStateOf(false) }
                    SoftCard(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .semantics { stateDescription = if (expanded) "Expandida" else "Recolhida" }
                            .clickable(role = Role.Button) { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}. ${section.title}", Modifier.weight(1f), color = Ink, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
                            Text(if (expanded) "−" else "+", Modifier.padding(start = 8.dp), color = Muted)
                        }
                        if (expanded) {
                            SelectionContainer {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    section.blocks.forEach { block ->
                                        Text(block.heading, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.semantics { heading() })
                                        Text(block.text, color = Ink, fontSize = 15.sp, lineHeight = 23.sp)
                                    }
                                }
                            }
                            if (section.sources.isNotEmpty()) {
                                Caption("Fontes oficiais consultadas em ${doc.prepared}. O texto acima é local; abrir a fonte externa pode exigir internet.", Modifier.padding(top = 12.dp))
                                section.sources.forEach { id -> doc.sources.firstOrNull { it.id == id }?.let { source ->
                                    TextButton({ uriHandler.openUri(source.url) }, contentPadding = PaddingValues(horizontal = 0.dp)) { Text(source.title) }
                                } }
                            }
                        }
                    }
                }
            }
            Caption("Aplicativo ${BuildConfig.VERSION_NAME} · código ${BuildConfig.VERSION_CODE}\nDocumento ${doc.id}-${doc.version} · aviso informativo ${ImportNotice.VERSION}")
            returnToImport?.let { action -> TextButton(action, Modifier.fillMaxWidth()) { Text("Voltar à importação") } }
        }
    }
}
