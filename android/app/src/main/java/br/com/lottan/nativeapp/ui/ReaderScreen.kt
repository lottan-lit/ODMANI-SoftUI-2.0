package br.com.lottan.nativeapp.ui

import android.app.Application
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.*
import androidx.lifecycle.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.lottan.nativeapp.data.Preferences

@Composable
fun ReaderScreen(id: String, prefs: Preferences, back: () -> Unit) {
    val application = LocalContext.current.applicationContext as Application
    val vm: ReaderViewModel =
        viewModel(key = "reader:$id", factory = ReaderViewModel.Factory(application, id))
    val book by vm.book.collectAsStateWithLifecycle()
    val page by vm.page.collectAsStateWithLifecycle()
    val image by vm.bitmap.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    var zoom by rememberSaveable(id) { mutableFloatStateOf(prefs.zoom) }
    var rotation by rememberSaveable(id) { mutableIntStateOf(prefs.rotation) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var jump by remember { mutableStateOf(false) }
    var number by remember { mutableStateOf("") }
    var fit by rememberSaveable { mutableStateOf(prefs.fit) }
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner, vm) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.resume()
            else if (event == Lifecycle.Event.ON_PAUSE) vm.pause()
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) vm.resume()
        onDispose {
            owner.lifecycle.removeObserver(observer)
            vm.pause()
        }
    }
    LaunchedEffect(page) {
        pan = Offset.Zero
        zoom = prefs.zoom
    }
    Column(Modifier.fillMaxSize().background(LocalVisual.current.background)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon("back", "Voltar", back)
            Text(
                book?.name ?: "Abrindo PDF…",
                Modifier.weight(1f),
                maxLines = 1,
                fontSize = 13.sp,
                color = Ink,
            )
            TextButton({ rotation = (rotation + 90) % 360 }) {
                Text("↻", color = Ink, fontSize = 24.sp)
            }
            TextButton({
                fit = if (fit == "width") "page" else "width"
                zoom = 1f
                pan = Offset.Zero
            }) {
                Text(if (fit == "width") "Página" else "Largura", fontSize = 11.sp, color = Ink)
            }
        }
        var viewport by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
        Box(
            Modifier.weight(1f)
                .fillMaxWidth()
                .padding(
                    horizontal =
                        when (prefs.margin) {
                            "compact" -> 4.dp
                            "wide" -> 24.dp
                            else -> 12.dp
                        }
                )
                .clipToBounds()
                .onSizeChanged {
                    viewport = it
                    vm.resize(it.width * 2)
                }
                .pointerInput(page, zoom) {
                    detectTapGestures(
                        onDoubleTap = {
                            zoom = if (zoom > 1f) 1f else 2f
                            pan = Offset.Zero
                        }
                    )
                }
                .pointerInput(page, viewport, prefs.direction) {
                    var swipe = Offset.Zero
                    detectTransformGestures { _, move, scale, _ ->
                        val previous = zoom
                        zoom = (zoom * scale).coerceIn(.75f, 5f)
                        if (zoom > 1f) {
                            val limitX = viewport.width * (zoom - 1) / 2
                            val limitY = viewport.height * (zoom - 1) / 2
                            pan =
                                Offset(
                                    (pan.x + move.x).coerceIn(-limitX, limitX),
                                    (pan.y + move.y).coerceIn(-limitY, limitY),
                                )
                        } else if (previous <= 1f && scale == 1f && prefs.direction != "single") {
                            swipe += move
                            val distance = if (prefs.direction == "vertical") swipe.y else swipe.x
                            if (kotlin.math.abs(distance) > 110) {
                                vm.show(page + if (distance < 0) 1 else -1)
                                swipe = Offset.Zero
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val bitmap = image
            if (bitmap != null) {
                val ratio = bitmap.width.toFloat() / bitmap.height
                Image(
                    bitmap.asImageBitmap(),
                    "Página ${page+1}",
                    Modifier.fillMaxWidth()
                        .then(
                            if (fit == "width")
                                Modifier.aspectRatio(ratio, matchHeightConstraintsFirst = false)
                            else Modifier.fillMaxHeight()
                        )
                        .graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            rotationZ = rotation.toFloat()
                            translationX = pan.x
                            translationY = pan.y
                        },
                    contentScale = if (fit == "width") ContentScale.FillWidth else ContentScale.Fit,
                )
            }
            if (loading) CircularProgressIndicator(Modifier.size(32.dp))
            if (error != null)
                Column(Modifier.padding(24.dp)) {
                    Text(error!!, color = Ink)
                    TextButton({ vm.show(page) }) { Text("Tentar novamente") }
                }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton({
                zoom = (zoom - .25f).coerceAtLeast(1f)
                pan = Offset.Zero
            }) {
                Text("−", color = Ink, fontSize = 24.sp)
            }
            Text("${"%.0f".format(zoom*100)}%", fontSize = 12.sp, color = Ink)
            TextButton({ zoom = (zoom + .25f).coerceAtMost(5f) }) {
                Text("+", color = Ink, fontSize = 24.sp)
            }
            TextButton({ vm.show(page - 1) }, enabled = page > 0) { Text("‹", fontSize = 27.sp) }
            TextButton({
                number = "${page+1}"
                jump = true
            }) {
                Text("${page+1} / ${book?.pages?:0}", fontSize = 12.sp, color = Ink)
            }
            TextButton({ vm.show(page + 1) }, enabled = page < (book?.pages ?: 1) - 1) {
                Text("›", fontSize = 27.sp)
            }
        }
        val total = book?.pages ?: 1
        var scrub by remember(page) { mutableFloatStateOf(page.toFloat()) }
        Slider(
            scrub,
            { scrub = it },
            Modifier.padding(horizontal = 20.dp),
            onValueChangeFinished = { vm.show(scrub.toInt()) },
            valueRange = 0f..(total - 1).coerceAtLeast(1).toFloat(),
            enabled = total > 1,
        )
        Caption(
            "${((page+1)*100f/total).toInt()}% lido",
            Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp),
        )
    }
    if (jump)
        AlertDialog(
            onDismissRequest = { jump = false },
            title = { Text("Ir para a página") },
            text = {
                OutlinedTextField(
                    number,
                    { number = it.filter(Char::isDigit) },
                    singleLine = true,
                    label = { Text("1 a ${book?.pages?:1}") },
                )
            },
            confirmButton = {
                TextButton({
                    val n = number.toIntOrNull()
                    if (n != null && n in 1..(book?.pages ?: 1)) {
                        vm.show(n - 1)
                        jump = false
                    }
                }) {
                    Text("Ir")
                }
            },
            dismissButton = { TextButton({ jump = false }) { Text("Cancelar") } },
        )
}
