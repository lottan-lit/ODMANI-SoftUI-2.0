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
            "Estas preferências são aplicadas ao abrir seus PDFs. A navegação exibe uma página por vez; a direção escolhida define o gesto para trocar de página."
        )
        Glass(Modifier.padding(top = 18.dp)) {
            Text("Ajuste da página", color = Ink)
            Caption("Como o documento ocupa a tela")
            Segments(
                listOf("Largura", "Página inteira"),
                if (prefs.fit == "width") "Largura" else "Página inteira",
            ) { v ->
                vm.run { vm.preferences.fit(if (v == "Largura") "width" else "page") }
            }
            Text("Navegação", color = Ink)
            Segments(
                listOf("Vertical", "Horizontal", "Uma página"),
                when (prefs.direction) {
                    "vertical" -> "Vertical"
                    "horizontal" -> "Horizontal"
                    else -> "Uma página"
                },
            ) { v ->
                vm.run {
                    vm.preferences.direction(
                        when (v) {
                            "Vertical" -> "vertical"
                            "Horizontal" -> "horizontal"
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
                { v -> vm.run { vm.preferences.zoom(v) } },
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
    Page {
        Top("Aparência e leitura", back, menu)
        SoftCard(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            Text("Tema da interface", color = Ink)
            ThemePicker(prefs.theme, vm::theme)
            Caption("O PDF mantém suas cores e diagramação originais.")
        }
        Glass {
            Text(
                "O mesmo céu, de outros anos, ainda parecia distante, mas estranhamente familiar.",
                fontFamily = FontFamily.Serif,
                fontSize = prefs.textSize.sp,
                lineHeight = (prefs.textSize * prefs.lineHeight).sp,
                color = Ink,
            )
        }
        Glass(Modifier.padding(vertical = 20.dp)) {
            Text("Tamanho do texto   ${prefs.textSize.toInt()}sp", color = Ink)
            Slider(
                prefs.textSize,
                { v -> vm.run { vm.preferences.text(v) } },
                valueRange = 16f..26f,
                steps = 9,
            )
            Text("Espaçamento entre linhas   ${"%.1f".format(prefs.lineHeight)}", color = Ink)
            Slider(
                prefs.lineHeight,
                { v -> vm.run { vm.preferences.spacing(v) } },
                valueRange = 1.3f..2f,
                steps = 6,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ocultar controles ao tocar", fontSize = 14.sp, color = Ink)
                    Caption("Toque novamente para mostrar")
                }
                Switch(prefs.hideControls, { v -> vm.run { vm.preferences.hide(v) } })
            }
        }
        Caption(
            "Tamanho e espaçamento valem para o texto de demonstração. PDFs preservam a diagramação original; use zoom no leitor."
        )
        Spacer(Modifier.height(20.dp))
        Primary("Experimentar na leitura", tryDemo)
    }
}
