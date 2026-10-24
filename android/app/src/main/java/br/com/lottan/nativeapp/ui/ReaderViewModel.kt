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

class ReaderViewModel(application: Application, private val id: String) :
    AndroidViewModel(application) {
    private val app = application as LottanApplication
    private val repository = app.library
    private val session = PdfSession(repository.file(id))
    val book = MutableStateFlow<Book?>(null)
    val bitmap = MutableStateFlow<Bitmap?>(null)
    val page = MutableStateFlow(0)
    val error = MutableStateFlow<String?>(null)
    val loading = MutableStateFlow(false)
    private var renderJob: Job? = null
    private var lastVisited = -1
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

    fun resize(pixels: Int) {
        val next = pixels.coerceIn(360, 1800)
        if (next != width) {
            width = next
            show(page.value)
        }
    }

    fun show(target: Int) {
        val b = book.value ?: return
        val next = ReadingRules.page(target, b.pages)
        page.value = next
        renderJob?.cancel()
        renderJob =
            viewModelScope.launch {
                loading.value = true
                error.value = null
                try {
                    val image = session.render(next, width)
                    ensureActive()
                    bitmap.value = image
                    if (lastVisited != next) {
                        repository.visit(id, next)
                        lastVisited = next
                    }
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

    @Synchronized
    fun resume() {
        if (started != 0L) return
        started = SystemClock.elapsedRealtime()
        ticker =
            viewModelScope.launch {
                while (isActive) {
                    delay(15000)
                    saveElapsed(capture(false))
                }
            }
    }

    @Synchronized
    private fun capture(stop: Boolean): Long {
        if (started == 0L) return 0
        val now = SystemClock.elapsedRealtime()
        val elapsed = now - started
        started = if (stop) 0 else now
        return if (bitmap.value != null) elapsed else 0
    }

    private suspend fun saveElapsed(elapsed: Long) {
        if (elapsed > 0) {
            val end = Instant.now()
            repository.record(end.minusMillis(elapsed), end)
        }
    }

    @Synchronized
    fun pause() {
        ticker?.cancel()
        ticker = null
        val elapsed = capture(true)
        app.background.launch { saveElapsed(elapsed) }
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
