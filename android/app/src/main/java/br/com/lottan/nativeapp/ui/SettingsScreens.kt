package br.com.lottan.nativeapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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

/** Title + description on the left, switch aligned to the right. */
@Composable
fun SwitchRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            SubtitleText(title)
            Caption(description, Modifier.padding(top = 2.dp))
        }
        Switch(checked, onChange, Modifier.semanticsLabel(title))
    }
}

/** Compact grid of real colour samples with name and selected state. */
@Composable
private fun SwatchGrid(items: List<String>, selected: String, color: (String) -> Color, columns: Int = 3, tag: String, onSelect: (String) -> Unit) {
    val p = LocalVisual.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { name ->
                    val active = name == selected
                    Column(
                        Modifier.weight(1f).testTag("$tag-$name").clip(RoundedCornerShape(16.dp)).clickable { onSelect(name) }
                            .border(if (active) 2.dp else 1.dp, if (active) p.text else p.border, RoundedCornerShape(16.dp)).padding(10.dp)
                            .semantics { this.selected = active; contentDescription = "$name${if (active) ", selecionado" else ""}" },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val sample = color(name)
                        Box(Modifier.fillMaxWidth().height(36.dp).background(if (sample == Color.Transparent) Color.White else sample, RoundedCornerShape(10.dp)).border(1.dp, p.border, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            if (active) Text("✓", color = if (sample.luminance() > .5f || sample == Color.Transparent) Color.Black else Color.White, fontSize = 16.sp)
                        }
                        Text(name, Modifier.padding(top = 6.dp), color = if (active) p.text else p.muted, fontSize = 12.sp, maxLines = 1)
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
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
    fun save(value: br.com.lottan.nativeapp.data.ReaderComfort) = vm.run { vm.workspaceRepository.comfort("global", value) }
    Page {
        Top("Conforto de leitura", back, menu)
        // Group 1: app interface
        SoftCard(Modifier.fillMaxWidth().testTag("comfort-interface")) {
            CardTitle("Interface")
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SubtitleText("Tamanho do texto", Modifier.weight(1f))
                Text("${prefs.textSize.toInt()} sp", color = Muted, fontSize = 14.sp)
            }
            Slider(prefs.textSize, { value -> vm.run { vm.preferences.text(value) } }, valueRange = 16f..24f)
            Caption("Afeta os textos do aplicativo e respeita as configurações de fonte do Android. Não modifica a diagramação dos PDFs.")
        }
        Spacer(Modifier.height(20.dp))
        // Group 2: PDF appearance
        SoftCard(Modifier.fillMaxWidth().testTag("comfort-appearance")) {
            CardTitle("Aparência dos PDFs")
            Caption("Documentos com preferências próprias mantêm suas escolhas.", Modifier.padding(top = 2.dp, bottom = 12.dp))
            SubtitleText("Cor de fundo")
            Spacer(Modifier.height(8.dp))
            SwatchGrid(ReaderBackgrounds, comfort.background, ::readerBackgroundColor, tag = "bg") { save(comfort.copy(background = it)) }
            Spacer(Modifier.height(20.dp))
            SubtitleText("Filtro de papel")
            Spacer(Modifier.height(8.dp))
            SwatchGrid(ReaderFilters, comfort.filter, { name -> if (name == "Neutro") Color.White else readerFilterColor(name) }, tag = "filter") { save(comfort.copy(filter = it)) }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SubtitleText("Intensidade do filtro", Modifier.weight(1f))
                Text("${(comfort.intensity * 100).toInt()}%", color = Muted, fontSize = 14.sp)
            }
            Slider(comfort.intensity, { value -> save(comfort.copy(intensity = value)) }, valueRange = 0f..0.7f)
        }
        Spacer(Modifier.height(20.dp))
        // Group 3: reading behaviour
        SoftCard(Modifier.fillMaxWidth().testTag("comfort-behavior")) {
            CardTitle("Comportamento de leitura")
            Spacer(Modifier.height(4.dp))
            SwitchRow("Manter tela ativa", "Impede que a tela apague automaticamente durante a leitura.", comfort.keepAwake, { save(comfort.copy(keepAwake = it)) })
            SwitchRow("Alternar controles ao tocar", "Mostra ou oculta os controles do leitor com um toque na tela.", prefs.hideControls, { value -> vm.run { vm.preferences.hide(value) } })
        }
        Spacer(Modifier.height(8.dp))
        TextButton({ save(br.com.lottan.nativeapp.data.ReaderComfort()); vm.run { vm.preferences.hide(true) } }) { Text("Restaurar conforto global") }
    }
}
