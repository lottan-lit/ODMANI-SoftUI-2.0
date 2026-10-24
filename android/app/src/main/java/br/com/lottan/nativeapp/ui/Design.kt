package br.com.lottan.nativeapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import br.com.lottan.nativeapp.domain.ReadingRules

val LocalDark = staticCompositionLocalOf { false }
val Ink: Color
    @Composable get() = LocalVisual.current.text
val Muted: Color
    @Composable get() = LocalVisual.current.muted

@Composable
fun LottanTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = VisualTokens.palette(dark)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme =
        base.copy(
            primary = p.primary,
            onPrimary = p.onPrimary,
            primaryContainer = p.secondary,
            onPrimaryContainer = p.text,
            secondary = p.muted,
            onSecondary = p.surface,
            secondaryContainer = p.secondary,
            onSecondaryContainer = p.text,
            tertiary = p.muted,
            onTertiary = p.surface,
            tertiaryContainer = p.secondary,
            onTertiaryContainer = p.text,
            background = p.background,
            onBackground = p.text,
            surface = p.surface,
            onSurface = p.text,
            surfaceVariant = p.secondary,
            onSurfaceVariant = p.muted,
            surfaceTint = Color.Transparent,
            outline = p.border,
            outlineVariant = p.border,
            inverseSurface = p.text,
            inverseOnSurface = p.background,
            inversePrimary = p.onPrimary,
            surfaceContainer = p.surface,
            surfaceContainerLow = p.surface,
            surfaceContainerHigh = p.secondary,
            surfaceContainerHighest = p.secondary,
            surfaceContainerLowest = p.background,
            error = p.text,
            onError = p.surface,
            errorContainer = p.secondary,
            onErrorContainer = p.text,
        )
    val type =
        Typography(
            titleLarge =
                androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = VisualTokens.Title,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    lineHeight = 30.sp,
                ),
            titleMedium =
                androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = VisualTokens.Subtitle,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    lineHeight = 24.sp,
                ),
            bodyLarge =
                androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = VisualTokens.Body,
                    lineHeight = 24.sp,
                ),
            bodyMedium =
                androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = VisualTokens.Caption,
                    lineHeight = 21.sp,
                ),
            bodySmall =
                androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = VisualTokens.Detail,
                    lineHeight = 18.sp,
                ),
        )
    CompositionLocalProvider(LocalDark provides dark, LocalVisual provides p) {
        MaterialTheme(colorScheme = scheme, typography = type, content = content)
    }
}

@Composable
fun Background(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.background(LocalVisual.current.background), content = content)
}

/** Compatibility name for existing callers; now an opaque Soft UI surface, without glass/blur. */
fun Modifier.glass(dark: Boolean, radius: Int = 16): Modifier {
    val p = VisualTokens.palette(dark)
    val shape = RoundedCornerShape(radius.dp)
    return shadow(
            VisualTokens.CardElevation,
            shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = if (dark) .12f else .035f),
            spotColor = Color.Black.copy(alpha = if (dark) .16f else .055f),
        )
        .clip(shape)
        .background(p.surface)
        .border(VisualTokens.BorderWidth, p.border, shape)
}

@Composable
fun Glass(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    SoftCard(modifier, content)
}

@Composable
fun Glyph(name: String, modifier: Modifier = Modifier, tint: Color = Ink) {
    Canvas(modifier.size(22.dp)) {
        val sx = size.width / 24
        val sy = size.height / 24
        fun line(x: Float, y: Float, x2: Float, y2: Float) =
            drawLine(
                tint,
                Offset(x * sx, y * sy),
                Offset(x2 * sx, y2 * sy),
                1.7f * sx,
                StrokeCap.Round,
            )
        fun path(vararg points: Pair<Float, Float>, closed: Boolean = false) {
            val p = Path()
            points.forEachIndexed { i, v ->
                if (i == 0) p.moveTo(v.first * sx, v.second * sy)
                else p.lineTo(v.first * sx, v.second * sy)
            }
            if (closed) p.close()
            drawPath(
                p,
                tint,
                style = Stroke(1.7f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        when (name) {
            "home" ->
                path(
                    4f to 10f,
                    12f to 3f,
                    20f to 10f,
                    20f to 20f,
                    14f to 20f,
                    14f to 14f,
                    10f to 14f,
                    10f to 20f,
                    4f to 20f,
                    closed = true,
                )
            "explore" -> {
                drawCircle(tint, 9 * sx, Offset(12 * sx, 12 * sy), style = Stroke(1.7f * sx))
                path(16f to 8f, 13.5f to 13.5f, 8f to 16f, 10.5f to 10.5f, closed = true)
            }
            "stats" -> {
                drawCircle(tint, 8 * sx, Offset(12 * sx, 12 * sy), style = Stroke(1.7f * sx))
                path(12f to 7f, 12f to 12f, 15f to 14f)
            }
            "library" -> {
                drawRoundRect(
                    tint,
                    Offset(5 * sx, 4 * sy),
                    Size(14 * sx, 6 * sy),
                    CornerRadius(2 * sx),
                    style = Stroke(1.7f * sx),
                )
                drawRoundRect(
                    tint,
                    Offset(5 * sx, 13 * sy),
                    Size(14 * sx, 7 * sy),
                    CornerRadius(2 * sx),
                    style = Stroke(1.7f * sx),
                )
            }
            "search" -> {
                drawCircle(
                    tint,
                    6.5f * sx,
                    Offset(10.5f * sx, 10.5f * sy),
                    style = Stroke(1.7f * sx),
                )
                line(16f, 16f, 21f, 21f)
            }
            "back" -> {
                path(13f to 5f, 6f to 12f, 13f to 19f)
                line(6f, 12f, 21f, 12f)
            }
            "forward" -> {
                path(11f to 5f, 18f to 12f, 11f to 19f)
                line(3f, 12f, 18f, 12f)
            }
            "close" -> {
                line(6f, 6f, 18f, 18f)
                line(18f, 6f, 6f, 18f)
            }
            else -> {
                line(4f, 6f, 20f, 6f)
                line(4f, 12f, 20f, 12f)
                line(4f, 18f, 20f, 18f)
            }
        }
    }
}

@Composable
fun Icon(name: String, label: String, onClick: () -> Unit) {
    IconButton(onClick, Modifier.size(44.dp)) {
        Box(Modifier.semanticsLabel(label)) { Glyph(name) }
    }
}

fun Modifier.semanticsLabel(label: String) =
    this.then(Modifier.semantics { this.contentDescription = label })

@Composable
fun Top(title: String, back: (() -> Unit)?, menu: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (back != null) Icon("back", "Voltar", back) else Spacer(Modifier.width(44.dp))
        Text(
            title,
            fontSize = VisualTokens.Subtitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            color = Ink,
        )
        Icon("menu", "Abrir menu", menu)
    }
}

@Composable
fun Primary(label: String, onClick: () -> Unit, enabled: Boolean) {
    SoftButton(label, enabled, onClick)
}

@Composable
fun Primary(label: String, onClick: () -> Unit) {
    Primary(label, onClick, true)
}

@Composable
fun Caption(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, fontSize = 12.sp, lineHeight = 18.sp, color = Muted)
}

@Composable
fun Segments(items: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 16.dp).glass(LocalDark.current, 14).padding(4.dp)
    ) {
        items.forEach { label ->
            TextButton(
                { onSelect(label) },
                Modifier.weight(1f)
                    .background(
                        if (selected == label) LocalVisual.current.primary else Color.Transparent,
                        RoundedCornerShape(11.dp),
                    ),
            ) {
                Text(
                    label,
                    fontSize = 11.sp,
                    color = if (selected == label) LocalVisual.current.onPrimary else Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun Cover(name: String, modifier: Modifier = Modifier, demo: Int = -1) {
    Box(
        modifier
            .clip(RoundedCornerShape(3.dp))
            .background(
                Brush.linearGradient(
                    if (demo == 1) listOf(Color(0xff777777), Color(0xff999999))
                    else listOf(Color(0xffdedede), Color(0xffeeeeee))
                )
            )
            .padding(12.dp)
    ) {
        if (demo == 0)
            Canvas(Modifier.matchParentSize()) {
                val p =
                    Path().apply {
                        moveTo(size.width * .6f, size.height * .7f)
                        lineTo(size.width, size.height * .5f)
                        lineTo(size.width, size.height)
                        lineTo(size.width * .6f, size.height)
                        close()
                    }
                drawPath(p, Color.DarkGray)
            }
        if (demo == 1)
            Box(
                Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = 22.dp)
                    .size(48.dp)
                    .background(
                        Brush.radialGradient(listOf(Color(0xffeeeeee), Color(0xff777777))),
                        CircleShape,
                    )
            )
        if (demo == 2)
            Box(
                Modifier.align(Alignment.BottomCenter)
                    .width(50.dp)
                    .height(74.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xff333333), Color(0xffaaaaaa))),
                        RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                    )
            )
        Column {
            if (demo < 0) {
                Text("P D F", fontSize = 7.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
            }
            Text(
                ReadingRules.title(name),
                fontFamily = FontFamily.Serif,
                fontSize = if (demo < 0) 17.sp else 13.sp,
                lineHeight = if (demo < 0) 19.sp else 14.sp,
                color = if (demo == 1) Color.White else Color(0xff292929),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            "ODMANI",
            Modifier.align(Alignment.BottomStart),
            fontSize = 7.sp,
            letterSpacing = 1.sp,
            color = if (demo == 1) Color.White else Color.DarkGray,
        )
    }
}

@Composable
fun BottomNav(route: String, go: (String) -> Unit) {
    val p = LocalVisual.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f, fill = false)
                .widthIn(max = 236.dp)
                .height(53.dp)
                .glass(LocalDark.current, 30)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("home" to "Início", "explore" to "Explorar", "stats" to "Atividade").forEach {
                (id, label) ->
                val active = route == id
                val fg = if (active) p.onPrimary else p.muted
                Row(
                    Modifier.weight(if (active) 2.1f else 1f)
                        .clip(CircleShape)
                        .background(if (active) p.primary else Color.Transparent)
                        .clickable { go(id) }
                        .height(45.dp)
                        .widthIn(min = 46.dp)
                        .padding(horizontal = if (active) 12.dp else 10.dp)
                        .semantics {
                            contentDescription = label
                            selected = active
                            role = Role.Tab
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Glyph(id, modifier = Modifier.requiredSize(22.dp), tint = fg)
                    if (active) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            label,
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp,
                            color = fg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        Box(
            Modifier.size(53.dp)
                .glass(LocalDark.current, 30)
                .clickable { go("library") }
                .semantics {
                    contentDescription = "Biblioteca"
                    selected = route == "library"
                    role = Role.Tab
                },
            contentAlignment = Alignment.Center,
        ) {
            Glyph("library", tint = if (route == "library") p.text else p.muted)
        }
    }
}
