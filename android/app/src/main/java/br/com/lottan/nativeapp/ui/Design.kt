package br.com.lottan.nativeapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import br.com.lottan.nativeapp.domain.ReadingRules

val LocalDark = staticCompositionLocalOf { false }
val Ink: Color
    @Composable get() = LocalVisual.current.text
val Muted: Color
    @Composable get() = LocalVisual.current.muted

/** Global switch for functional UI transitions (menu option + Android "remove animations"). */
val LocalAnimations = staticCompositionLocalOf { true }

/** Fast, soft transitions. With animations off every spec snaps instantly. */
@Composable
fun <T> softTween(millis: Int = 160): FiniteAnimationSpec<T> = if (LocalAnimations.current) tween(millis, easing = FastOutSlowInEasing) else snap()

/** One typographic scale for every screen: titles of equivalent level look identical everywhere. */
object AppText {
    val Screen = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold)
    val Card = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
    val Subtitle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
    val Info = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal)
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier, maxLines: Int = 2) =
    Text(text, modifier, color = Ink, style = AppText.Screen, maxLines = maxLines, overflow = TextOverflow.Ellipsis)

/** Titles of cards and home/stat sections (Últimos 7 dias, Suas metas, Seu ritmo, PDFs pessoais...). */
@Composable
fun CardTitle(text: String, modifier: Modifier = Modifier, maxLines: Int = 2) =
    Text(text, modifier, color = Ink, style = AppText.Card, maxLines = maxLines, overflow = TextOverflow.Ellipsis)

@Composable
fun SubtitleText(text: String, modifier: Modifier = Modifier) = Text(text, modifier, color = Ink, style = AppText.Subtitle)

@Composable
fun InfoText(text: String, modifier: Modifier = Modifier) = Text(text, modifier, color = Ink, style = AppText.Info)

/** Reference field style taken from Metas de leitura: 20 dp corners, thin border, theme colours, same label spacing. */
@Composable
fun SoftField(
    value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    label: (@Composable () -> Unit)? = null, placeholder: (@Composable () -> Unit)? = null, supportingText: (@Composable () -> Unit)? = null, isError: Boolean = false,
    singleLine: Boolean = false, minLines: Int = 1, maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default, keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val p = LocalVisual.current
    OutlinedTextField(
        value, onValueChange, modifier, enabled = enabled, shape = RoundedCornerShape(20.dp),
        textStyle = AppText.Info.copy(color = if (enabled) p.text else p.muted),
        label = label, placeholder = placeholder,
        supportingText = supportingText, isError = isError, singleLine = singleLine,
        minLines = minLines, maxLines = maxLines, keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
        trailingIcon = trailingIcon,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = p.text, unfocusedBorderColor = p.border, disabledBorderColor = p.border.copy(alpha = .5f),
            focusedContainerColor = p.surface, unfocusedContainerColor = p.surface, disabledContainerColor = p.surface,
            errorContainerColor = p.surface, cursorColor = p.text,
            focusedLabelColor = p.text, unfocusedLabelColor = p.muted, disabledLabelColor = p.muted.copy(alpha = .6f),
        ),
    )
}

@Composable
fun LottanTheme(dark: Boolean, graphite: Boolean = false, uiScale: Float = 1f, highContrast: Boolean = false, animations: Boolean = true, content: @Composable () -> Unit) {
    val basePalette = if (graphite) VisualTokens.Graphite else VisualTokens.palette(dark)
    val p = if (highContrast) basePalette.copy(muted = basePalette.text, border = basePalette.text.copy(alpha = .45f)) else basePalette
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
    val density = androidx.compose.ui.platform.LocalDensity.current
    CompositionLocalProvider(LocalDark provides dark, LocalVisual provides p, LocalAnimations provides animations,
        androidx.compose.ui.platform.LocalDensity provides Density(density.density, density.fontScale * uiScale)) {
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
            "pin" -> {
                path(9f to 3f, 15f to 3f, 14f to 9f, 18f to 13f, 6f to 13f, 10f to 9f, closed = true)
                line(12f, 13f, 12f, 21f)
            }
            "book-check" -> {
                path(12f to 6f, 8f to 4f, 3f to 4f, 3f to 18f, 8f to 18f, 12f to 20f, 12f to 6f, 16f to 4f, 21f to 4f, 21f to 11f)
                path(15f to 16f, 18f to 19f, 22f to 14f)
            }
            "add" -> { line(12f, 4f, 12f, 20f); line(4f, 12f, 20f, 12f) }
            "folder" -> path(3f to 6f, 9f to 6f, 11f to 9f, 21f to 9f, 21f to 20f, 3f to 20f, closed = true)
            "grid" -> {
                listOf(4f to 4f, 14f to 4f, 4f to 14f, 14f to 14f).forEach { (x, y) ->
                    path(x to y, x + 6f to y, x + 6f to y + 6f, x to y + 6f, closed = true)
                }
            }
            "list" -> { line(8f, 6f, 21f, 6f); line(8f, 12f, 21f, 12f); line(8f, 18f, 21f, 18f); line(3f, 6f, 4f, 6f); line(3f, 12f, 4f, 12f); line(3f, 18f, 4f, 18f) }
            "more" -> { listOf(5f, 12f, 19f).forEach { y -> drawCircle(tint, 1.5f * sx, Offset(12f * sx, y * sy)) } }
            "edit" -> {
                path(4f to 16f, 15f to 5f, 19f to 9f, 8f to 20f, 4f to 20f, closed = true)
                line(13f, 7f, 17f, 11f)
            }
            "trash" -> {
                line(4f, 6f, 20f, 6f); path(7f to 6f, 8f to 21f, 16f to 21f, 17f to 6f)
                path(9f to 6f, 9f to 3f, 15f to 3f, 15f to 6f); line(10f, 10f, 10f, 17f); line(14f, 10f, 14f, 17f)
            }
            "sliders" -> {
                line(3f, 6f, 21f, 6f); line(3f, 12f, 21f, 12f); line(3f, 18f, 21f, 18f)
                line(8f, 3f, 8f, 9f); line(16f, 9f, 16f, 15f); line(10f, 15f, 10f, 21f)
            }
            "up" -> path(6f to 15f, 12f to 9f, 18f to 15f)
            "down" -> path(6f to 9f, 12f to 15f, 18f to 9f)
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
fun Top(title: String, back: (() -> Unit)?, menu: () -> Unit, showMenu: Boolean = back == null) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (back != null) Icon("back", "Voltar", back) else Spacer(Modifier.width(44.dp))
        ScreenTitle(title, Modifier.weight(1f).padding(horizontal = 8.dp))
        if (showMenu) Icon("menu", "Abrir menu", menu) else Spacer(Modifier.width(44.dp))
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
            val fill by animateColorAsState(if (selected == label) LocalVisual.current.primary else Color.Transparent, softTween(), label = "segment")
            TextButton(
                { onSelect(label) },
                Modifier.weight(1f)
                    .background(fill, RoundedCornerShape(11.dp)),
            ) {
                Text(
                    label,
                    fontSize = 12.sp,
                    color = if (selected == label) LocalVisual.current.onPrimary else Muted,
                    maxLines = 1,
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
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val labels = maxWidth >= 380.dp && androidx.compose.ui.platform.LocalDensity.current.fontScale < 1.3f
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).testTag("main-nav-capsule").height(53.dp).glass(LocalDark.current, 30).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf("home" to "Home", "stats" to "Atividades", "library" to "Pastas", "explore" to "Explorar").forEach { (id, label) ->
                    val active = route == id
                    val fg = if (active) p.onPrimary else p.muted
                    val weight by animateFloatAsState(if (active && labels) 2.1f else 1f, softTween(200), label = "nav-weight")
                    val fill by animateColorAsState(if (active) p.primary else Color.Transparent, softTween(), label = "nav-fill")
                    Row(Modifier.weight(weight).clip(CircleShape)
                        .background(fill).clickable { go(id) }.height(45.dp)
                        .padding(horizontal = 6.dp).semantics { contentDescription = label; selected = active; role = Role.Tab },
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Glyph(id, Modifier.requiredSize(22.dp), tint = fg)
                        if (active && labels) {
                            // Icon + label are one group centred in the bubble: the label takes only its own width.
                            Spacer(Modifier.width(6.dp))
                            Text(label, Modifier.weight(1f, fill = false), fontSize = 12.sp, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Box(Modifier.size(53.dp).testTag("nav-add-pdf").glass(LocalDark.current, 30).clickable { go("import") }
                .semantics { contentDescription = "Adicionar PDF"; role = Role.Button }, contentAlignment = Alignment.Center) { Glyph("add", tint = p.muted) }
        }
    }
}
