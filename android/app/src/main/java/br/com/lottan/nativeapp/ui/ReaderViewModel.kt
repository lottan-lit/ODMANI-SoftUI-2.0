package br.com.lottan.nativeapp.ui

import android.app.Application
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.lifecycle.*
import br.com.lottan.nativeapp.LottanApplication
import br.com.lottan.nativeapp.data.*
import br.com.lottan.nativeapp.domain.ReadingRules
import java.time.Instant
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ReaderViewModel(application: Application, private val id: String) :
    AndroidViewModel(application) {
    private val app = application as LottanApplication
    private val workspace = WorkspaceRepository(application)
    val comfort = workspace.comfort(id).stateIn(viewModelScope, SharingStarted.Eagerly, ReaderComfort())
    fun comfort(value: ReaderComfort) { viewModelScope.launch { workspace.comfort(id, value) } }
    private val repository = app.library
    private val session = PdfSession(repository.file(id), application)
    val book = MutableStateFlow<Book?>(null)
    val bitmap = MutableStateFlow<Bitmap?>(null)
    val page = MutableStateFlow(0)
    val error = MutableStateFlow<String?>(null)
    val loading = MutableStateFlow(false)
    private var renderJob: Job? = null
    private var readingSession = ""
    private var sessionStarted = 0L
    private val qualified = mutableSetOf<Int>()
    private var renderedPage = -1
    private var visitJob: Job? = null
    private var lastInteraction = SystemClock.elapsedRealtime()
    fun interact() {
        val now = SystemClock.elapsedRealtime()
        // Flush an expired idle window before moving its cutoff; pinch frames need no database write.
        if (now - lastInteraction >= 120000L) flushClock(false)
        lastInteraction = now
    }
    private var width = 1080
    private var ticker: Job? = null
    private var started = 0L
    private val clockLock = Mutex()

    init {
        viewModelScope.launch {
            book.value = repository.dao.book(id)
            page.value = book.value?.page ?: 0
            show(page.value)
        }
    }

    suspend fun renderPage(target: Int, pixels: Int) = session.render(target, pixels)

    fun resize(pixels: Int) {
        val next = pixels.coerceIn(360, 1800)
        if (next != width) {
            width = next
            show(page.value)
        }
    }

    fun show(target: Int) {
        flushClock(false)
        visitJob?.cancel()
        val b = book.value ?: return
        val next = ReadingRules.page(target, b.pages)
        page.value = next
        app.background.launch { clockLock.withLock { if (page.value == next) repository.position(id, next) } }
        renderedPage = -1
        renderJob?.cancel()
        renderJob =
            viewModelScope.launch {
                loading.value = true
                error.value = null
                try {
                    val image = session.render(next, width)
                    ensureActive()
                    bitmap.value = image
                    // Loading time is excluded from activity and qualification.
                    started = if (started != 0L) SystemClock.elapsedRealtime() else 0L
                    renderedPage = next
                    qualifyPage()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    bitmap.value = null
                    error.value =
                        "Não foi possível renderizar a página. Tente outra página ou outro PDF."
                } catch (e: OutOfMemoryError) {
                    bitmap.value = null
                    error.value =
                        "Memória insuficiente. Feche outros aplicativos e tente novamente."
                } finally {
                    loading.value = false
                }
            }
    }

    private fun qualifyPage() {
        visitJob?.cancel()
        val target = page.value
        if (started == 0L || renderedPage != target || target in qualified) return
        visitJob = viewModelScope.launch {
            delay(5000)
            if (started != 0L && renderedPage == target && page.value == target) {
                repository.visit(id, target, readingSession, sessionStarted)
                qualified.add(target)
            }
        }
    }

    @Synchronized
    fun resume() {
        if (started != 0L) return
        if (readingSession.isEmpty()) {
            readingSession = java.util.UUID.randomUUID().toString()
            sessionStarted = System.currentTimeMillis()
        }
        lastInteraction = SystemClock.elapsedRealtime()
        started = lastInteraction
        qualifyPage()
        ticker = viewModelScope.launch {
            while (isActive) { delay(15000); flushClock(false) }
        }
    }

    @Synchronized
    private fun flushClock(stop: Boolean) {
        if (started == 0L) return
        val now = SystemClock.elapsedRealtime()
        val elapsed = if (renderedPage == page.value && bitmap.value != null)
            ReadingRules.activeMillis(started, now, lastInteraction) else 0L
        // Keep the actual active interval, including its date, rather than moving idle time to now.
        val wallStart = Instant.now().minusMillis(now - started)
        started = if (stop) 0L else now
        val key = readingSession
        val sessionStart = sessionStarted
        if (elapsed > 0L) app.background.launch {
            repository.record(wallStart, wallStart.plusMillis(elapsed), key, sessionStart)
        }
    }

    @Synchronized
    fun pause() {
        visitJob?.cancel()
        ticker?.cancel()
        ticker = null
        flushClock(true)
        val current = page.value
        if (book.value != null) app.background.launch {
            clockLock.withLock { if (page.value == current) repository.position(id, current) }
        }
    }

    override fun onCleared() {
        pause()
        app.background.launch { session.close() }
        super.onCleared()
    }

    class Factory(private val app: Application, private val id: String) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ReaderViewModel(app, id) as T
        }
    }
}
