package br.com.lottan.nativeapp.ui

import android.app.Application
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.*
import androidx.lifecycle.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.lottan.nativeapp.data.Preferences

@Composable
fun ReaderScreen(id: String, prefs: Preferences, back: () -> Unit) {
    val application = LocalContext.current.applicationContext as Application
    val vm: ReaderViewModel =
        viewModel(key = "reader:$id", factory = ReaderViewModel.Factory(application, id))
    val comfort by vm.comfort.collectAsStateWithLifecycle()
    val direction = comfort.direction.ifBlank { prefs.direction }
    var settings by remember { mutableStateOf(false) }
    val view = LocalView.current
    DisposableEffect(view, comfort.keepAwake) {
        val previous = view.keepScreenOn
        view.keepScreenOn = comfort.keepAwake
        onDispose { view.keepScreenOn = previous }
    }
    val activity = LocalContext.current as? android.app.Activity
    DisposableEffect(activity, comfort.brightness, comfort.orientation, comfort.immersive) {
        val window = activity?.window
        val orientation = activity?.requestedOrientation
        val brightness = window?.attributes?.screenBrightness
        if (activity != null) activity.requestedOrientation = when (comfort.orientation) {
            "Retrato" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "Paisagem" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        if (window != null) {
            val attrs = window.attributes; attrs.screenBrightness = comfort.brightness; window.attributes = attrs
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
            if (comfort.immersive) controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            else controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (activity != null && orientation != null && !activity.isChangingConfigurations) activity.requestedOrientation = orientation
            if (window != null) {
                val attrs = window.attributes; attrs.screenBrightness = brightness ?: -1f; window.attributes = attrs
                androidx.core.view.WindowCompat.getInsetsController(window, view).show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    val outside = when (comfort.background) {
        "Grafite" -> Color(0xff1D1D1F); "Cinza" -> Color(0xff777777)
        "Papel" -> Color(0xffFAF9F6); "Sépia" -> Color(0xffD8C5A4); "Branco" -> Color.White
        else -> runCatching { Color(android.graphics.Color.parseColor(comfort.background)) }.getOrDefault(Color.Black)
    }
    val tint = when (comfort.filter) {
        "Creme" -> Color(0xffFFF0CB); "Off-white" -> Color(0xffFAF9F6)
        "Sépia" -> Color(0xffD8BC88); "Rosado" -> Color(0xffF5DCE1)
        else -> Color.Transparent
    }
    val book by vm.book.collectAsStateWithLifecycle()
    val page by vm.page.collectAsStateWithLifecycle()
    val image by vm.bitmap.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    var controls by rememberSaveable(id) { mutableStateOf(true) }
    var zoom by rememberSaveable(id) { mutableFloatStateOf(prefs.zoom) }
    var comfortInitialized by rememberSaveable(id) { mutableStateOf(false) }
    var rotation by rememberSaveable(id) { mutableIntStateOf(prefs.rotation) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var jump by remember { mutableStateOf(false) }
    var number by remember { mutableStateOf("") }
    var fit by rememberSaveable { mutableStateOf(prefs.fit) }
    val currentPage by rememberUpdatedState(page)
    val continuous = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var continuousReady by remember { mutableStateOf(false) }
    LaunchedEffect(book?.id, direction) {
        val b = book
        if (b != null && direction == "vertical") {
            continuous.scrollToItem(page)
            continuousReady = true
            snapshotFlow { continuous.firstVisibleItemIndex }.distinctUntilChanged().collect { index ->
                if (index != currentPage) { vm.interact(); vm.show(index) }
            }
        }
    }
    fun navigate(target: Int) {
        vm.interact()
        val next = target.coerceIn(0, ((book?.pages ?: 1) - 1).coerceAtLeast(0))
        vm.show(next)
        if (direction == "vertical") scope.launch { continuous.scrollToItem(next) }
    }
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
    LaunchedEffect(comfort.rotation) { comfort.rotation?.let { rotation = it } }
    LaunchedEffect(vm) {
        if (!comfortInitialized) {
            val stored = br.com.lottan.nativeapp.data.WorkspaceRepository(application).comfort(id).first()
            zoom = stored.zoom; fit = stored.fit; comfortInitialized = true
        }
    }
    LaunchedEffect(page) {
        pan = Offset.Zero
    }
    Column(Modifier.fillMaxSize().background(LocalVisual.current.background)) {
        if (controls) Row(
            Modifier.fillMaxWidth().background(Color.Black).heightIn(min = 52.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(back, Modifier.size(48.dp).semanticsLabel("Voltar")) { Glyph("back", tint = Color(0xffF5F5F7)) }
            Text(book?.name ?: "Abrindo PDF…", Modifier.weight(1f).padding(horizontal = 8.dp), maxLines = 1,
                overflow = TextOverflow.Ellipsis, fontSize = 14.sp, color = Color(0xffF5F5F7))
            IconButton({ settings = true; vm.interact() }, Modifier.size(48.dp).semanticsLabel("Ajustes de leitura")) {
                Glyph("sliders", tint = Color(0xffF5F5F7))
            }
        }
        var viewport by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
        Box(
            Modifier.weight(1f)
                .fillMaxWidth()
                .background(outside)
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
                        onTap = { vm.interact(); if (prefs.hideControls) controls = !controls },
                        onDoubleTap = {
                            vm.interact();
                            zoom = if (zoom > 1f) 1f else 2f
                            pan = Offset.Zero
                        }
                    )
                }
                .pointerInput(page, viewport, direction) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        var pressed: Boolean
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val multiple = event.changes.count { it.pressed } > 1
                            if (multiple || zoom > 1f) {
                                val scale = event.calculateZoom()
                                val move = event.calculatePan()
                                if (scale != 1f || move != Offset.Zero) {
                                    vm.interact()
                                    zoom = (zoom * scale).coerceIn(.75f, 5f)
                                    val limitX = (viewport.width * (zoom - 1) / 2).coerceAtLeast(0f)
                                    val limitY = (viewport.height * (zoom - 1) / 2).coerceAtLeast(0f)
                                    pan = Offset((pan.x + move.x).coerceIn(-limitX, limitX), (pan.y + move.y).coerceIn(-limitY, limitY))
                                    event.changes.forEach { it.consume() }
                                }
                            }
                            pressed = event.changes.any { it.pressed }
                        } while (pressed)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val bitmap = image
            if (direction == "vertical" && book != null) {
                ContinuousPages(vm, book!!, continuous,
                    if (tint == Color.Transparent) null else ColorFilter.tint(tint.copy(alpha = comfort.intensity), BlendMode.Multiply), viewport.width * 2, zoom, pan, comfort.spacing)
            } else if (bitmap != null) {
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
                    colorFilter = if (tint == Color.Transparent) null else ColorFilter.tint(tint.copy(alpha = comfort.intensity), BlendMode.Multiply),
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
        if (controls) {
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
            TextButton({ navigate(page - 1) }, enabled = page > 0) { Text("‹", fontSize = 27.sp) }
            TextButton({
                number = "${page+1}"
                jump = true
            }) {
                Text("${page+1} / ${book?.pages?:0}", fontSize = 12.sp, color = Ink)
            }
            TextButton({ navigate(page + 1) }, enabled = page < (book?.pages ?: 1) - 1) {
                Text("›", fontSize = 27.sp)
            }
        }
        val total = book?.pages ?: 1
        var scrub by remember(page) { mutableFloatStateOf(page.toFloat()) }
        Slider(
            scrub,
            { scrub = it },
            Modifier.padding(horizontal = 20.dp),
            onValueChangeFinished = { navigate(scrub.toInt()) },
            valueRange = 0f..(total - 1).coerceAtLeast(1).toFloat(),
            enabled = total > 1,
        )
        Caption(
            "${((page+1)*100f/total).toInt()}% lido",
            Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp),
        )
        }
    }
    if (settings) ReaderAdjustments(
        comfort = comfort, direction = direction, zoom = zoom, fit = fit, rotation = rotation, page = page,
        onChange = { value -> if (value.zoom != comfort.zoom) zoom = value.zoom; vm.comfort(value) }, onDismiss = { settings = false },
        onFit = { value -> fit = value; zoom = 1f; pan = Offset.Zero; vm.comfort(comfort.copy(fit = value, zoom = 1f)) },
        onRotate = { rotation = (rotation + 90) % 360; vm.comfort(comfort.copy(rotation = rotation)) },
        onNavigate = { navigate(it); settings = false },
    )
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
                        navigate(n - 1)
                        jump = false
                    }
                }) {
                    Text("Ir")
                }
            },
            dismissButton = { TextButton({ jump = false }) { Text("Cancelar") } },
        )
}
