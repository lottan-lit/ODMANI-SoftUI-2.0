package br.com.lottan.nativeapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import br.com.lottan.nativeapp.data.Preferences

val demoAuthors = listOf("Apolo Sangretta", "Helena C.", "ODMANI Revista", "ODMANI Jornal")
val demoTotals = listOf(312, 184, 64, 24)
val sampleText =
    listOf(
        "O pátio estava mais vazio do que o normal. O sinal havia tocado há poucos minutos e, mesmo assim, alguns continuavam sentados nos degraus, falando sobre algo que eu não cheguei a ouvir.",
        "Olivia estava um pouco mais à frente, com o cabelo preso de qualquer jeito, segurando um caderno contra o peito. Ela parecia concentrada em alguma conversa, mas não ria. Só ouvia.",
        "Eu passei por eles sem pressa, como sempre. Não havia nenhum motivo para me aproximar, e ainda assim eu olhei. Ela percebeu e virou o rosto por um instante. Nada além disso.",
        "O mesmo céu, de outros anos, ainda parecia distante, mas estranhamente familiar.",
    )

@Composable
fun DetailScreen(
    index: Int,
    prefs: Preferences,
    vm: LibraryViewModel,
    back: () -> Unit,
    go: (String) -> Unit,
) {
    Page {
        Top("Sobre o livro", back, { go("menu") })
        Cover(
            demoNames[index],
            Modifier.align(Alignment.CenterHorizontally).width(168.dp).height(242.dp),
            index,
        )
        Text(
            demoNames[index],
            Modifier.padding(top = 24.dp).align(Alignment.CenterHorizontally),
            fontSize = 22.sp,
            color = Ink,
        )
        Caption(
            demoAuthors[index],
            Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(vertical = 30.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            listOf(
                    "${demoTotals[index]}" to "Páginas",
                    (if (index < 2) "Romance" else if (index == 2) "Revistas" else "Jornais") to
                        "Categoria",
                    "Prévia" to "Demonstração",
                )
                .forEach { (v, k) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(v, fontSize = 13.sp, color = Ink)
                        Caption(k)
                    }
                }
        }
        Primary(
            if (prefs.demoPages[index] == demoTotals[index]) "▶ Ler novamente"
            else "▶ Continuar leitura"
        ) {
            go("demo/$index")
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TextButton({ vm.run { vm.preferences.toggle("favorites", index) } }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (index.toString() in prefs.favorites) "♥" else "♡",
                        fontSize = 25.sp,
                        color = Ink,
                    )
                    Caption("Favorito")
                }
            }
            TextButton({ go("reading-settings") }) {
                Column {
                    Text("Aa", fontSize = 22.sp, color = Ink)
                    Caption("Leitura")
                }
            }
            TextButton({ go("pdf-settings") }) {
                Column {
                    Text("▤", fontSize = 22.sp, color = Ink)
                    Caption("Página")
                }
            }
        }
        Caption(
            "Título e conteúdo de demonstração",
            Modifier.align(Alignment.CenterHorizontally).padding(top = 20.dp),
        )
    }
}

@Composable
fun DemoReader(
    index: Int,
    prefs: Preferences,
    vm: LibraryViewModel,
    back: () -> Unit,
    go: (String) -> Unit,
) {
    var hidden by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        if (!hidden)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon("back", "Voltar", back)
                Spacer(Modifier.weight(1f))
                TextButton({ go("reading-settings") }) { Text("Aa", color = Ink) }
                TextButton({ vm.run { vm.preferences.toggle("bookmarks", index) } }) {
                    Text(if (index.toString() in prefs.bookmarks) "▣" else "♧", color = Ink)
                }
                Icon("menu", "Abrir menu") { go("menu") }
            }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).clickable(
                enabled = prefs.hideControls
            ) {
                hidden = !hidden
            }
        ) {
            Caption(
                demoNames[index],
                Modifier.align(Alignment.CenterHorizontally).padding(top = 35.dp),
            )
            Caption(
                "TEXTO DE DEMONSTRAÇÃO",
                Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp, bottom = 30.dp),
            )
            sampleText.forEach {
                Text(
                    it,
                    Modifier.padding(bottom = 23.dp),
                    fontFamily = FontFamily.Serif,
                    fontSize = prefs.textSize.sp,
                    lineHeight = (prefs.textSize * prefs.lineHeight).sp,
                    color = Ink,
                )
            }
        }
        if (!hidden)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    prefs.demoPages[index].toFloat(),
                    { v -> vm.run { vm.preferences.demoPage(index, v.toInt()) } },
                    Modifier.weight(1f),
                    valueRange = 1f..demoTotals[index].toFloat(),
                )
                Caption("${prefs.demoPages[index]} de ${demoTotals[index]}")
            }
    }
}

/** Three side-by-side areas with the real theme tokens: Preto absoluto, Grafite and Claro off-white. */
@Composable
private fun AutomaticThemePreview() {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    Row(Modifier.fillMaxWidth().height(64.dp).clip(shape).testTag("theme-auto-preview")) {
        listOf(VisualTokens.Dark, VisualTokens.Graphite, VisualTokens.Light).forEach { palette ->
            Column(Modifier.weight(1f).fillMaxHeight().background(palette.background).padding(horizontal = 6.dp, vertical = 12.dp)) {
                repeat(3) { index ->
                    Box(Modifier.fillMaxWidth(if (index == 2) .5f else .85f).height(3.dp).background(palette.muted))
                    Spacer(Modifier.height(7.dp))
                }
            }
        }
    }
}

@Composable
fun ThemePicker(theme: String, select: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf("dark" to "Preto absoluto", "graphite" to "Grafite", "light" to "Claro off-white", "system" to "Automático").chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (key, label) ->
                    val palette = when (key) { "dark" -> VisualTokens.Dark; "graphite" -> VisualTokens.Graphite; else -> VisualTokens.Light }
                    Column(Modifier.weight(1f).glass(LocalDark.current, 12).clickable { select(key) }.padding(12.dp)) {
                        if (key == "system") AutomaticThemePreview()
                        else Box(Modifier.fillMaxWidth().height(64.dp).background(palette.background, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))) {
                            Column(Modifier.padding(12.dp)) {
                                repeat(3) { index ->
                                    Box(Modifier.fillMaxWidth(if (index == 2) .5f else .85f).height(3.dp).background(palette.muted))
                                    Spacer(Modifier.height(7.dp))
                                }
                            }
                        }
                        Text(label, Modifier.padding(top = 8.dp), fontSize = 12.sp, color = Ink)
                        if (key == "system") Text("Acompanha o tema claro ou escuro do dispositivo.", fontSize = 12.sp, lineHeight = 16.sp, color = Muted)
                        Text(if (theme == key) "✓ Selecionado" else "Selecionar", fontSize = 12.sp, color = Muted)
                    }
                }
            }
        }
    }
}
