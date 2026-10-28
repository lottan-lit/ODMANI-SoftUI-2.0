package br.com.lottan.nativeapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.lottan.nativeapp.data.ReaderComfort

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReaderChoices(items: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { (key, label) ->
            FilterChip(selected = selected == key, onClick = { onSelect(key) },
                label = { Text(label, fontSize = 12.sp) })
        }
    }
}

@Composable
private fun ReaderSection(title: String) {
    Spacer(Modifier.height(8.dp))
    Text(title, color = Ink, style = MaterialTheme.typography.titleMedium)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReaderAdjustments(
    comfort: ReaderComfort, direction: String, zoom: Float, fit: String, rotation: Int, page: Int,
    onChange: (ReaderComfort) -> Unit, onDismiss: () -> Unit, onFit: (String) -> Unit,
    onRotate: () -> Unit, onNavigate: (Int) -> Unit,
) {
    var custom by remember(comfort.background) { mutableStateOf(comfort.background.takeIf { it.startsWith("#") } ?: "") }
    var restore by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding().padding(16.dp), contentAlignment = Alignment.Center) {
            Surface(Modifier.widthIn(max = 560.dp).fillMaxWidth().heightIn(max = maxHeight * .88f), color = LocalVisual.current.surface) {
                Column {
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, bottom = 12.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Ajustes de leitura", color = Ink, style = MaterialTheme.typography.titleMedium)
                            Caption("Personalizações deste documento")
                        }
                        Icon("close", "Fechar ajustes", onDismiss)
                    }
                    HorizontalDivider(color = LocalVisual.current.border)
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ReaderSection("Conforto visual")
                        Text("Filtro de papel", color = Ink)
                        val filters = listOf("Neutro" to Color.White, "Creme" to Color(0xffFFF0CB), "Off-white" to Color(0xffFAF9F6), "Sépia" to Color(0xffD8BC88), "Rosado" to Color(0xffF5DCE1))
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            filters.forEach { (name, color) ->
                                FilterChip(selected = comfort.filter == name, onClick = { onChange(comfort.copy(filter = name)) }, label = { Text(name, fontSize = 12.sp) }, leadingIcon = { Box(Modifier.size(14.dp).background(color, CircleShape).border(1.dp, LocalVisual.current.border, CircleShape)) })
                            }
                        }
                        Text("Intensidade do filtro · ${(comfort.intensity * 100).toInt()}%", color = Ink)
                        Slider(comfort.intensity, { onChange(comfort.copy(intensity = it)) }, valueRange = 0f..0.7f)
                        Caption("O filtro é visual e não modifica o arquivo PDF.")
                        Text("Fundo externo ao PDF", color = Ink)
                        ReaderChoices(listOf("Preto", "Grafite", "Cinza", "Papel", "Sépia", "Branco").map { it to it }, comfort.background) { onChange(comfort.copy(background = it)) }
                        val customError = custom.isNotEmpty() && !custom.matches(Regex("#[A-Fa-f0-9]{6}"))
                        SoftField(custom, { value -> custom = value.take(7); if (custom.matches(Regex("#[A-Fa-f0-9]{6}"))) onChange(comfort.copy(background = custom)) }, Modifier.fillMaxWidth(), label = { Text("Cor personalizada #RRGGBB") }, singleLine = true, isError = customError)

                        ReaderSection("Exibição das páginas")
                        ReaderChoices(listOf("single" to "Página única", "vertical" to "Rolagem contínua", "" to "Global"), comfort.direction) { onChange(comfort.copy(direction = it)) }
                        Caption(if (comfort.direction.isBlank()) "Global: acompanha a preferência geral de navegação." else "Modo específico salvo para este PDF.")
                        Text("Ajuste da página", color = Ink)
                        ReaderChoices(listOf("page" to "Página inteira", "width" to "Ajustar à largura"), fit, onFit)
                        Text("Espaçamento entre páginas · ${comfort.spacing} dp", color = if (direction == "vertical") Ink else Muted)
                        Slider(comfort.spacing.toFloat(), { onChange(comfort.copy(spacing = it.toInt())) }, valueRange = 0f..40f, enabled = direction == "vertical")
                        if (direction != "vertical") Caption("Disponível na rolagem contínua.")
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Rotação da página · $rotation°", Modifier.weight(1f), color = Ink)
                            TextButton(onRotate) { Text("Girar 90°") }
                        }

                        ReaderSection("Tela e iluminação")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Usar brilho do sistema", Modifier.weight(1f), color = Ink)
                            Switch(comfort.brightness < 0, { system -> onChange(comfort.copy(brightness = if (system) -1f else .5f)) })
                        }
                        if (comfort.brightness >= 0) {
                            Text("Brilho manual · ${(comfort.brightness * 100).toInt()}%", color = Ink)
                            Slider(comfort.brightness, { onChange(comfort.copy(brightness = it)) }, valueRange = .05f..1f)
                        }
                        Text("Orientação da tela", color = Ink)
                        ReaderChoices(listOf("Sistema", "Retrato", "Paisagem").map { it to it }, comfort.orientation) { onChange(comfort.copy(orientation = it)) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Manter tela ativa", Modifier.weight(1f), color = Ink)
                            Switch(comfort.keepAwake, { onChange(comfort.copy(keepAwake = it)) })
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Modo imersivo", Modifier.weight(1f), color = Ink)
                            Switch(comfort.immersive, { onChange(comfort.copy(immersive = it)) })
                        }

                        ReaderSection("Preferências")
                        Caption("Modo, conforto e ajustes específicos são salvos automaticamente. O zoom atual pode ser lembrado ao reabrir este PDF.")
                        TextButton({ onChange(comfort.copy(zoom = zoom, fit = fit, rotation = rotation)) }) { Text("Lembrar zoom atual · ${(zoom * 100).toInt()}%") }
                        val mark = page.toString()
                        TextButton({ onChange(comfort.copy(bookmarks = if (mark in comfort.bookmarks) comfort.bookmarks - mark else comfort.bookmarks + mark)) }) { Text(if (mark in comfort.bookmarks) "Remover marcador desta página" else "Marcar página atual") }
                        comfort.bookmarks.sortedBy { it.toIntOrNull() ?: 0 }.forEach { value ->
                            value.toIntOrNull()?.let { target -> TextButton({ onNavigate(target) }) { Text("Abrir marcador · página ${target + 1}") } }
                        }
                        TextButton({ restore = true }) { Text("Restaurar ajustes padrão") }
                    }
                    HorizontalDivider(color = LocalVisual.current.border)
                    Box(Modifier.fillMaxWidth().padding(16.dp)) { Primary("Concluir", onDismiss) }
                }
            }
        }
    }
    if (restore) AlertDialog(onDismissRequest = { restore = false }, title = { Text("Restaurar ajustes?") }, text = { Text("O progresso e os marcadores serão preservados.") }, confirmButton = { TextButton({ onChange(ReaderComfort(bookmarks = comfort.bookmarks)); restore = false }) { Text("Restaurar") } }, dismissButton = { TextButton({ restore = false }) { Text("Cancelar") } })
}
