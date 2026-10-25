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
            Color(0xffFAF9F6),
            Color(0xffF2F0EC),
            Color(0xffE8E6E2),
            Color(0xff222222),
            Color(0xff6D6C68),
            Color(0xffD8D6D1),
            Color(0xff151515),
            Color.White,
        )
    val Dark =
        VisualPalette(
            Color(0xff000000),
            Color(0xff121212),
            Color(0xff1D1D1F),
            Color(0xffF5F5F7),
            Color(0xffA1A1A6),
            Color(0xff36363B),
            Color(0xffF1F1F3),
            Color(0xff151515),
        )
    val Graphite = Dark.copy(background = Color(0xff18181A), surface = Color(0xff232326), secondary = Color(0xff303035))
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
