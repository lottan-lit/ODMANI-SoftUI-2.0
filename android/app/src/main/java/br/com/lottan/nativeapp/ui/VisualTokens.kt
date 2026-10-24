package br.com.lottan.nativeapp.ui

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One visual contract shared by screens, cards, controls and system bars. */
data class VisualPalette(
    val background: Color,
    val surface: Color,
    val secondary: Color,
    val text: Color,
    val muted: Color,
    val border: Color,
    val primary: Color,
    val onPrimary: Color,
)

object VisualTokens {
    val Light =
        VisualPalette(
            Color(0xffF5F6F8),
            Color.White,
            Color(0xffF0F1F3),
            Color(0xff171717),
            Color(0xff707070),
            Color(0xffE4E5E8),
            Color(0xff151515),
            Color.White,
        )
    val Dark =
        VisualPalette(
            Color(0xff141416),
            Color(0xff202023),
            Color(0xff29292D),
            Color(0xffF5F5F5),
            Color(0xffA5A5AA),
            Color(0xff36363B),
            Color(0xffF1F1F3),
            Color(0xff151515),
        )
    val CardRadius = 16.dp
    val CardElevation = 2.dp
    val BorderWidth = 1.dp
    val ScreenInset = 24.dp
    val CardInset = 16.dp
    val Space = 8.dp
    val Title = 24.sp
    val Subtitle = 18.sp
    val Body = 16.sp
    val Caption = 14.sp
    val Detail = 12.sp

    fun palette(dark: Boolean) = if (dark) Dark else Light
}

val LocalVisual = staticCompositionLocalOf { VisualTokens.Light }
