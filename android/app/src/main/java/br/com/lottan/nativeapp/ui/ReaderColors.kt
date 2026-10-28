package br.com.lottan.nativeapp.ui

import androidx.compose.ui.graphics.Color

/** Single source for the six reading background colours and the paper filters (reader, previews and settings). */
val ReaderBackgrounds = listOf("Preto", "Grafite", "Cinza", "Papel", "Sépia", "Branco")
val ReaderFilters = listOf("Neutro", "Creme", "Off-white", "Sépia", "Rosado")

fun readerBackgroundColor(name: String): Color = when (name) {
    "Preto" -> Color.Black
    "Grafite" -> Color(0xff1D1D1F)
    "Cinza" -> Color(0xff777777)
    "Papel" -> Color(0xffFAF9F6)
    "Sépia" -> Color(0xffD8C5A4)
    "Branco" -> Color.White
    else -> runCatching { Color(android.graphics.Color.parseColor(name)) }.getOrDefault(Color.Black)
}

fun readerFilterColor(name: String): Color = when (name) {
    "Creme" -> Color(0xffFFF0CB)
    "Off-white" -> Color(0xffFAF9F6)
    "Sépia" -> Color(0xffD8BC88)
    "Rosado" -> Color(0xffF5DCE1)
    else -> Color.Transparent
}
