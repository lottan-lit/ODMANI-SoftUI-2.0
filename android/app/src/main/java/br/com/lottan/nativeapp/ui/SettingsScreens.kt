package br.com.lottan.nativeapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import br.com.lottan.nativeapp.data.Preferences

@Composable
fun PdfSettings(vm: LibraryViewModel, prefs: Preferences, back: () -> Unit, menu: () -> Unit) {
    Page {
        Top("Páginas do PDF", back, menu)
        Caption("Prepare a página para o seu jeito de ler.")
        Glass(Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
            Text("ODMANI", fontSize = 10.sp, color = Muted)
            Text(
                "Uma página,\noutro ritmo.",
                Modifier.padding(vertical = 20.dp),
                fontFamily = FontFamily.Serif,
                fontSize = 22.sp,
                color = Ink,
            )
            Caption("Preferências · ${prefs.rotation}° · ${"%.0f".format(prefs.zoom*100)}%")
        }
        Caption(
            "A página única usa controles explícitos. O modo Vertical permite rolagem contínua. Ajustes de zoom e conforto também podem ser salvos por documento."
        )
        Glass(Modifier.padding(top = 18.dp)) {
            Text("Ajuste da página", color = Ink)
            Caption("Como o documento ocupa a tela")
            Segments(
                listOf("Largura", "Página inteira"),
                if (prefs.fit == "width") "Largura" else "Página inteira",
            ) { v ->
                vm.run { vm.preferences.fit(if (v == "Largura") "width" else "page"); vm.workspaceRepository.comfort("global", vm.globalComfort.value.copy(fit = if (v == "Largura") "width" else "page")) }
            }
            Text("Navegação", color = Ink)
            Segments(
                listOf("Uma página", "Vertical"),
                when (prefs.direction) {
                    "vertical" -> "Vertical"
                    else -> "Uma página"
                },
            ) { v ->
                vm.run {
                    vm.preferences.direction(
                        when (v) {
                            "Vertical" -> "vertical"
                            else -> "single"
                        }
                    )
                }
            }
            Text("Orientação da página", color = Ink)
            Segments(listOf("0°", "90°", "180°", "270°"), "${prefs.rotation}°") { v ->
                vm.run { vm.preferences.rotation(v.dropLast(1).toInt()) }
            }
            Text("Margens", color = Ink)
            Segments(
                listOf("Estreitas", "Normais", "Amplas"),
                when (prefs.margin) {
                    "compact" -> "Estreitas"
                    "wide" -> "Amplas"
                    else -> "Normais"
                },
            ) { v ->
                vm.run {
                    vm.preferences.margin(
                        when (v) {
                            "Estreitas" -> "compact"
                            "Amplas" -> "wide"
                            else -> "normal"
                        }
                    )
                }
            }
            Text("Zoom   ${"%.0f".format(prefs.zoom*100)}%", color = Ink)
            Slider(
                prefs.zoom,
                { v -> vm.run { vm.preferences.zoom(v); vm.workspaceRepository.comfort("global", vm.globalComfort.value.copy(zoom = v)) } },
                valueRange = .75f..1.5f,
                steps = 14,
            )
        }
        TextButton({ vm.run { vm.preferences.resetPdf() } }) {
            Text("Restaurar ajustes da página", color = Ink)
        }
    }
}

@Composable
fun ReadingSettings(
    vm: LibraryViewModel,
    prefs: Preferences,
    back: () -> Unit,
    menu: () -> Unit,
    tryDemo: () -> Unit,
) {
    val comfort by vm.globalComfort.collectAsState()
    Page {
        Top("Aparência e leitura", back, menu)
        SoftCard(Modifier.fillMaxWidth()) {
            Text("Tamanho do texto da interface", color = Ink)
            Slider(prefs.textSize, { value -> vm.run { vm.preferences.text(value) } }, valueRange = 16f..24f)
            Caption("O tamanho respeita também a configuração de fonte do Android. PDFs mantêm sua diagramação.")
        }
        Spacer(Modifier.height(20.dp))
        SoftCard(Modifier.fillMaxWidth()) {
            Text("Conforto global dos PDFs", color = Ink)
            Caption("Documentos com preferências próprias mantêm suas escolhas.")
            listOf("Preto", "Grafite", "Cinza", "Papel", "Sépia", "Branco").chunked(3).forEach { row ->
                Segments(row, comfort.background) { vm.run { vm.workspaceRepository.comfort("global", comfort.copy(background = it)) } }
            }
            Text("Filtro de papel", color = Ink)
            listOf("Neutro", "Creme", "Off-white", "Sépia", "Rosado").chunked(3).forEach { row ->
                Segments(row, comfort.filter) { vm.run { vm.workspaceRepository.comfort("global", comfort.copy(filter = it)) } }
            }
            Slider(comfort.intensity, { value -> vm.run { vm.workspaceRepository.comfort("global", comfort.copy(intensity = value)) } }, valueRange = 0f..0.7f)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(comfort.keepAwake, { value -> vm.run { vm.workspaceRepository.comfort("global", comfort.copy(keepAwake = value)) } }); Text("Manter tela ativa", color = Ink)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(prefs.hideControls, { value -> vm.run { vm.preferences.hide(value) } }); Text("Alternar controles ao tocar", color = Ink)
            }
            TextButton({ vm.run { vm.workspaceRepository.comfort("global", br.com.lottan.nativeapp.data.ReaderComfort()) } }) { Text("Restaurar conforto global") }
        }
    }
}
