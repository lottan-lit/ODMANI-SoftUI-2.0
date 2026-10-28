package br.com.lottan.nativeapp.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import br.com.lottan.nativeapp.data.*
import br.com.lottan.nativeapp.domain.ReadingRules

val demoNames =
    listOf(
        "Temos Todo Tempo do Mundo",
        "A delicadeza dos dias",
        "entre tempos.",
        "O mundo mais perto",
    )

val LocalBottomNavClearance = staticCompositionLocalOf { 0.dp }

@Composable
fun LottanApp(vm: LibraryViewModel, incomingUri: android.net.Uri? = null, incomingHandled: () -> Unit = {}) {
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val duplicate by vm.duplicate.collectAsStateWithLifecycle()
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val ready by vm.ready.collectAsStateWithLifecycle()
    val workspace by vm.workspace.collectAsStateWithLifecycle()
    val books by vm.books.collectAsStateWithLifecycle()
    val groups by vm.groups.collectAsStateWithLifecycle()
    val days by vm.days.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val animationsOn = LocalAnimations.current
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route?.substringBefore("?") ?: "home"
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
            it?.let { uri -> vm.prepareImport(uri) }
        }
    fun go(page: String) {
        nav.navigate(page) { launchSingleTop = true }
    }
    fun back() {
        if (!nav.popBackStack()) go("home")
    }
    var showImportNotice by rememberSaveable { mutableStateOf(false) }
    var seenNoticeHere by rememberSaveable { mutableStateOf(false) }
    var pendingImportUri by rememberSaveable { mutableStateOf<android.net.Uri?>(null) }
    var returnToImport by rememberSaveable { mutableStateOf(false) }
    fun continueImport() {
        val uri = pendingImportUri
        pendingImportUri = null
        showImportNotice = false
        if (uri != null) vm.import(uri) else picker.launch(arrayOf("application/pdf"))
    }
    fun requestImport(uri: android.net.Uri? = null) {
        pendingImportUri = uri
        if (prefs.importNoticeVersion >= ImportNotice.VERSION || seenNoticeHere) continueImport()
        else {
            seenNoticeHere = true
            showImportNotice = true
            vm.run { vm.preferences.noticePresented(ImportNotice.VERSION) }
        }
    }
    LaunchedEffect(incomingUri, ready) {
        if (ready && incomingUri != null) { requestImport(incomingUri); incomingHandled() }
    }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.message.value = null
        }
    }
    if (!ready) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (showImportNotice) AlertDialog(
        onDismissRequest = { showImportNotice = false; pendingImportUri = null },
        title = { Text(ImportNotice.TITLE) },
        text = { Text(ImportNotice.TEXT) },
        confirmButton = { TextButton({ continueImport() }) { Text("Continuar") } },
        dismissButton = { TextButton({ showImportNotice = false; returnToImport = true; go("transparency") }) { Text("Ler regras completas") } },
    )
    duplicate?.let { (uri, existing) ->
        AlertDialog(onDismissRequest = { vm.duplicate.value = null }, title = { Text("PDF duplicado") }, text = { Text("Este arquivo já está salvo. Deseja abrir o existente ou importar outra cópia?") },
            confirmButton = { TextButton({ vm.duplicate.value = null; go("book/$existing") }) { Text("Abrir existente") } },
            dismissButton = { Row { TextButton({ vm.duplicate.value = null }) { Text("Cancelar") }; TextButton({ vm.duplicate.value = null; if (vm.duplicateForPreview) vm.prepareImport(uri, true) else vm.import(uri, true) }) { Text("Outra cópia") } } })
    }
    val usesFloatingNav = route in listOf("home", "library", "explore", "stats", "search")
    val density = LocalDensity.current
    val keyboardVisible = WindowInsets.ime.getBottom(density) > 0
    val showBottomNav = usesFloatingNav && !keyboardVisible
    val bottomInset = WindowInsets.navigationBars.union(WindowInsets.displayCutout)
        .asPaddingValues().calculateBottomPadding()
    var navigationHeightPx by remember { mutableIntStateOf(0) }
    val navigationHeight = with(density) { navigationHeightPx.toDp() }
    // The reader draws edge to edge: its own background (Conforto de leitura) must fill the areas behind the
    // system bars, so the reader applies the insets itself instead of the shell padding them with the theme colour.
    val readerFullBleed = route == "reader/{id}"
    val safeInsets = if (readerFullBleed) WindowInsets(0, 0, 0, 0)
    else if (usesFloatingNav) WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    else WindowInsets.safeDrawing
    CompositionLocalProvider(LocalBottomNavClearance provides if (showBottomNav) navigationHeight else 0.dp) {
    Background(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(safeInsets).then(if (readerFullBleed) Modifier else Modifier.imePadding())) {
            Box(
                Modifier.widthIn(max = if (readerFullBleed) Dp.Unspecified else if (route == "library") 840.dp else 600.dp)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .align(Alignment.TopCenter)
            ) {
                Box(Modifier.fillMaxSize()) {
                    NavHost(
                        nav,
                        startDestination = if (prefs.entered) "home" else "welcome",
                        enterTransition = {
                            if (animationsOn) fadeIn(tween(190)) + slideInVertically(tween(190)) { 6 } else EnterTransition.None
                        },
                        exitTransition = { if (animationsOn) fadeOut(tween(100)) else ExitTransition.None },
                        popEnterTransition = { if (animationsOn) fadeIn(tween(190)) else EnterTransition.None },
                        popExitTransition = { if (animationsOn) fadeOut(tween(100)) else ExitTransition.None },
                    ) {
                        composable("welcome") {
                            Page {
                                Text(
                                    "ODMANI",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-2).sp,
                                )
                                Spacer(Modifier.height(48.dp))
                                Text(
                                    "Leitura\nem outro\nritmo.",
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 46.sp,
                                    lineHeight = 49.sp,
                                    color = Ink,
                                )
                                Spacer(Modifier.height(24.dp))
                                Caption(
                                    "Seus livros, revistas e outros PDFs em uma experiência de leitura focada e sem anúncios."
                                )
                                Spacer(Modifier.height(45.dp))
                                Primary("Começar  →") {
                                    vm.enter()
                                    nav.navigate("home") { popUpTo("welcome") { inclusive = true } }
                                }
                            }
                        }
                        composable("home") {
                            SoftHome(
                                books,
                                busy,
                                { go("import") },
                                ::go,
                                workspace, days, vm,
                            ) { id ->
                                vm.restart(id) { go("reader/$id") }
                            }
                        }
                        composable(
                            "library?origin={origin}",
                            arguments =
                                listOf(
                                    androidx.navigation.navArgument("origin") {
                                        defaultValue = "all"
                                    }
                                ),
                        ) {
                            val origin = it.arguments?.getString("origin") ?: "all"
                            LibraryScreen(
                                books,
                                busy,
                                { go("import") },
                                { back() },
                                ::go,
                                origin, vm, workspace.pinned,
                            )
                        }
                        composable("folder/new") { FolderEditorScreen(vm, null) { back() } }
                        composable("folder/{id}/edit") {
                            val folder = groups.firstOrNull { group -> group.id == it.arguments?.getString("id") && group.kind == "folders" }
                            if (folder != null) FolderEditorScreen(vm, folder) { back() }
                            else Page { Top("Editar pasta", { back() }, {}); Caption("Esta pasta não está disponível.") }
                        }
                        composable("folder/{id}") {
                            FolderLibraryScreen(it.arguments?.getString("id") ?: "", vm, books, { back() }, ::go, { go("import") }, workspace.pinned, busy)
                        }
                        composable("book-folders/{id}") {
                            val id = it.arguments?.getString("id") ?: ""
                            AssignFoldersDialog(setOf(id), vm) { back() }
                        }
                        composable("book/{id}") {
                            val id = it.arguments?.getString("id") ?: ""
                            NativeBookDetails(
                                books.firstOrNull { b -> b.id == id },
                                { back() },
                                ::go,
                                vm, workspace,
                            ) {
                                vm.restart(it) { go("reader/$it") }
                            }
                        }
                        composable("remove/{id}") {
                            val id = it.arguments?.getString("id") ?: ""
                            AlertDialog(
                                onDismissRequest = { back() },
                                title = { Text("Remover PDF do aplicativo?") },
                                text = {
                                    Text(
                                        "A cópia salva na ODMANI será excluída. O arquivo original no celular será preservado."
                                    )
                                },
                                confirmButton = {
                                    TextButton({
                                        vm.remove(id)
                                        back()
                                    }) {
                                        Text("Remover")
                                    }
                                },
                                dismissButton = { TextButton({ back() }) { Text("Cancelar") } },
                            )
                        }
                        composable("search") { SearchScreen(books, { back() }, ::go) }
                        composable("explore") { EditorialExplore(catalog, { back() }, { go("menu") }) { go("official/$it") } }
                        composable("official/{id}") {
                            val id = it.arguments?.getString("id") ?: ""
                            EditorialDetails(catalog.publications.firstOrNull { p -> p.id == id }, catalog, { back() })
                        }
                        composable("stats") {
                            SoftStats(books, days, groups, { back() }, { go("menu") }, workspace, vm, scale = { go("goals?scale=true") }) { go("goals") }
                        }
                        composable("goals?scale={scale}", arguments = listOf(androidx.navigation.navArgument("scale") { type = androidx.navigation.NavType.BoolType; defaultValue = false })) {
                            GoalsScreen(vm, workspace, it.arguments?.getBoolean("scale") ?: false) { back() }
                        }
                        composable("organize") {
                            OrganizeScreen(vm, books, groups, { back() }, { go("menu") }, { go("reader/$it") }, ::go)
                        }
                        composable("category/new") {
                            CategoryCreateScreen(vm, books, groups, { back() }, { nav.navigate("library?origin=all") { popUpTo("home") } })
                        }
                        composable("menu") {
                            Page {
                                Top("Menu", { back() }, { go("menu") })
                                Caption("DO SEU JEITO")
                                Text(
                                    "Seu espaço de leitura.",
                                    Modifier.padding(top = 10.dp),
                                    fontSize = 29.sp,
                                    fontFamily = FontFamily.Serif,
                                    color = Ink,
                                )
                                Caption(
                                    "Ajuste os detalhes. Encontre seu ritmo.",
                                    Modifier.padding(vertical = 12.dp),
                                )
                                Caption("APARÊNCIA", Modifier.padding(top = 18.dp, bottom = 12.dp))
                                Glass {
                                    Text("Tema do aplicativo", color = Ink)
                                    Caption("Biblioteca, menus e leitura")
                                    ThemePicker(prefs.theme, vm::theme)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(prefs.highContrast, { v -> vm.run { vm.preferences.flag("highContrast", v) } }); Text("Contraste elevado", color = Ink)
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Glass(Modifier.fillMaxWidth().testTag("menu-animations")) {
                                    SubtitleText("Animações do aplicativo")
                                    Caption("Transições curtas e discretas. Também respeitam a opção de reduzir movimento do Android.")
                                    Segments(listOf("Ativadas", "Desativadas"), if (animationsOn) "Ativadas" else "Desativadas") { v -> vm.run { vm.preferences.flag("animations", v == "Ativadas") } }
                                }
                                MenuSection(
                                    "LEITURA",
                                    listOf(
                                        "Páginas do PDF" to "pdf-settings",
                                        "Conforto de leitura" to "reading-settings",
                                    ),
                                    ::go,
                                )
                                MenuSection(
                                    "SUA BIBLIOTECA",
                                    listOf(
                                        "Pastas e categorias" to "organize",
                                        "Importar PDF" to "import",
                                    ),
                                    ::go,
                                )
                                MenuSection("HÁBITOS DE LEITURA", listOf("Metas de leitura, registro e exibição" to "goals"), ::go)
                                MenuSection("DADOS E ARMAZENAMENTO", listOf("Armazenamento e miniaturas" to "storage"), ::go)
                                MenuSection(
                                    "SOBRE E INFORMAÇÕES LEGAIS",
                                    listOf(
                                        "Regras e responsabilidades" to "transparency",
                                        "Ajuda e próximos passos" to "help",
                                        "Sobre a ODMANI" to "about",
                                    ),
                                    ::go,
                                )
                                Caption(
                                    "ODMANI · Desenvolvido pela AUDMANI\nParceira editorial — Lottan",
                                    Modifier.padding(top = 25.dp),
                                )
                            }
                        }
                        composable("storage") {
                            Page {
                                Top("Dados e armazenamento", { back() }, { go("menu") })
                                Caption("PDFs: ${"%.1f".format(books.sumOf { it.bytes } / 1048576.0)} MB")
                                TextButton({ vm.run { vm.clearThumbnailCache() } }) { Text("Limpar cache de miniaturas") }
                                Caption("Apenas miniaturas temporárias serão removidas. PDFs, progresso, anotações e checklists são preservados.")
                                Caption("Backup completo e restauração ainda não estão disponíveis.")
                            }
                        }
                        composable("pdf-settings") {
                            PdfSettings(vm, prefs, { back() }, { go("menu") })
                        }
                        composable("reading-settings") {
                            ReadingSettings(vm, prefs, { back() }, { go("menu") }) { go("demo/0") }
                        }
                        composable("import") { entry ->
                            LaunchedEffect(entry.id) {
                                if (vm.importPreview.value == null) { vm.importedBook.value = null; vm.importFolders.value = emptySet() }
                            }
                            AddPdfScreen(vm, { requestImport() }, { back() }, ::go)
                        }
                        composable("help") {
                            Page {
                                Top("Ajuda", { back() }, { go("menu") })
                                Glass {
                                    Text("Seu PDF, no seu ritmo.", fontSize = 24.sp, color = Ink)
                                    Caption(
                                        "Importar\nEscolha um PDF no seletor do Android. Uma cópia fica no app para ler offline.\n\nLer\nAbra o PDF na biblioteca. Use as setas, a barra ou toque no número para ir a uma página.\n\nAmpliar e mover\nUse pinça ou os botões − e +. Dois toques alternam o zoom. Arraste para mover o documento.\n\nAparência\nO tema vale para a interface. PDFs mantêm suas cores originais.\n\nAnotações e checklist estão nos detalhes de cada PDF. Busca dentro do PDF, sincronização, arquivos protegidos por senha e OCR ainda não estão disponíveis."
                                    )
                                }
                            }
                        }
                        composable("transparency") {
                            androidx.activity.compose.BackHandler(enabled = returnToImport) {
                                returnToImport = false
                                back()
                                showImportNotice = true
                            }
                            val resumeImport: (() -> Unit)? = if (returnToImport) ({ returnToImport = false; back(); showImportNotice = true }) else null
                            TransparencyScreen(back = { if (returnToImport) { returnToImport = false; back(); showImportNotice = true } else back() }, returnToImport = resumeImport)
                        }
                        composable("about") {
                            Page {
                                Top("Sobre a ODMANI", { back() }, { go("menu") })
                                Glass {
                                    Text(
                                        "ODMANI",
                                        fontSize = 24.sp,
                                        color = Ink,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        "Leitura em outro ritmo.",
                                        fontSize = 24.sp,
                                        fontFamily = FontFamily.Serif,
                                        color = Ink,
                                    )
                                    Caption(
                                        "Desenvolvido pela AUDMANI.\nParceira editorial — Lottan.\n\n${if (br.com.lottan.nativeapp.BuildConfig.PUBLIC_VERSION.isBlank()) "Versão de teste ${br.com.lottan.nativeapp.BuildConfig.VERSION_NAME}" else "Versão ${br.com.lottan.nativeapp.BuildConfig.PUBLIC_VERSION}"} · nativa\n\nLeitura offline de PDFs, zoom, rotação, tema e posição salva. Seus PDFs ficam apenas neste celular.\n\nAndroid 8 ou posterior. Sem anúncios e sem conta.\n\nAtualização da versão nativa: PDFs, preferências e histórico preservados. A biblioteca do antigo MVP web não é migrada automaticamente."
                                    )
                                }
                            }
                        }
                        composable("detail/{index}") {
                            val index =
                                it.arguments?.getString("index")?.toIntOrNull()?.coerceIn(0, 3) ?: 0
                            DetailScreen(index, prefs, vm, { back() }, ::go)
                        }
                        composable("demo/{index}") {
                            val index =
                                it.arguments?.getString("index")?.toIntOrNull()?.coerceIn(0, 3) ?: 0
                            DemoReader(index, prefs, vm, { back() }, ::go)
                        }
                        composable("reader/{id}") {
                            val id = it.arguments?.getString("id") ?: ""
                            ReaderScreen(id, prefs) { back() }
                        }
                    }
                }
            }
            if (showBottomNav) {
                // Draw-only overlay: content can scroll behind it; only capsules handle taps.
                val background = LocalVisual.current.background
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(navigationHeight)
                    .testTag("navigation-gradient")
                    .background(Brush.verticalGradient(
                        0f to background.copy(alpha = 0f),
                        ((navigationHeight - bottomInset).value / 2f / navigationHeight.value.coerceAtLeast(1f)).coerceIn(0f, 1f) to background.copy(alpha = 0f),
                        1f to background,
                    )))
                Box(Modifier.align(Alignment.BottomCenter).widthIn(max = 600.dp).fillMaxWidth()
                    .testTag("floating-navigation")
                    .onSizeChanged { navigationHeightPx = it.height }
                    .padding(bottom = bottomInset)) {
                    BottomNav(route, ::go)
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = LocalBottomNavClearance.current))
            if (busy)
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Glass {
                        CircularProgressIndicator()
                        Caption("Processando PDF…")
                    }
                }
        }
    }
    }
}

@Composable
fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().testTag("scrolling-page")
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 14.dp + LocalBottomNavClearance.current),
        content = content,
    )
}

fun normalize(text: String) =
    java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .lowercase()

@Composable
fun BookShelf(books: List<Book>, modifier: Modifier = Modifier, open: (String) -> Unit) {
    if (books.size in 1..3) BoxWithConstraints(modifier.fillMaxWidth()) {
        val column = (maxWidth - 24.dp) / 3
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            books.forEach { b -> key(b.id) { PersonalBookCard(b, column) { open(b.id) } } }
        }
    } else LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(books, key = { it.id }) { b -> PersonalBookCard(b) { open(b.id) } }
    }
}

@Composable
fun DemoShelf(indices: List<Int> = listOf(0, 1, 2, 3), open: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(indices) { i ->
            Column(Modifier.width(85.dp).clickable { open(i) }) {
                Cover(demoNames[i], Modifier.width(85.dp).height(124.dp), i)
                Text(
                    demoNames[i],
                    Modifier.padding(top = 9.dp),
                    fontSize = 12.sp,
                    color = Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Caption(listOf("15% lido", "Concluído", "31% lido", "8% lido")[i])
            }
        }
    }
}

@Composable
fun DocumentRow(book: Book, open: () -> Unit, remove: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp).glass(LocalDark.current).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).clickable(onClick = open),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PdfCover(book, Modifier.width(48.dp).aspectRatio(CoverRatio))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    ReadingRules.title(book.name),
                    fontSize = 14.sp,
                    color = Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Caption(
                    "${book.page+1} de ${book.pages} páginas · ${"%.1f".format(book.bytes/1048576.0)} MB"
                )
            }
        }
        if (remove != null) Icon("close", "Remover ${book.name}", remove)
    }
}

@Composable
fun MenuSection(title: String, rows: List<Pair<String, String>>, go: (String) -> Unit) {
    Caption(title, Modifier.padding(top = 24.dp, bottom = 12.dp))
    Glass(Modifier.fillMaxWidth()) {
        rows.forEach { (name, route) ->
            TextButton({ go(route) }, Modifier.fillMaxWidth()) {
                Text(name, Modifier.weight(1f), color = Ink, fontSize = 14.sp)
                Text("›", color = Muted, fontSize = 22.sp)
            }
        }
    }
}
