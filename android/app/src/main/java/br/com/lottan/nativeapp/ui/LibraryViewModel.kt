package br.com.lottan.nativeapp.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import br.com.lottan.nativeapp.LottanApplication
import br.com.lottan.nativeapp.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LottanApplication
    val workspaceRepository = WorkspaceRepository(application)
    val globalComfort = workspaceRepository.comfort("global").stateIn(viewModelScope, SharingStarted.Eagerly, ReaderComfort())
    val workspace = workspaceRepository.state.stateIn(viewModelScope, SharingStarted.Eagerly, Workspace())
    fun saveSpace(id: String, value: DocumentSpace) = run { workspaceRepository.space(id, value) }
    val catalog = MutableStateFlow(EditorialCatalog())
    val repository = app.library
    val preferences = app.preferences
    val books =
        repository.books.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sessions = repository.dao.sessions().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val uniquePages = repository.dao.uniquePages().stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val memberships = repository.dao.memberships().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val categoryLinks = repository.dao.categoryLinks().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val groups =
        repository.groups.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val days =
        repository.days.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val prefs = preferences.state.stateIn(viewModelScope, SharingStarted.Eagerly, Preferences())
    val ready = MutableStateFlow(false)
    val busy = MutableStateFlow(false)
    val importPreview = MutableStateFlow<PreparedPdf?>(null)
    val importFolders = MutableStateFlow<Set<String>>(emptySet())
    val importedBook = MutableStateFlow<Book?>(null)
    var duplicateForPreview = false
    private var previewJob: Job? = null
    val duplicate = MutableStateFlow<Pair<Uri, String>?>(null)
    val coverRevision = MutableStateFlow(0)
    val message = MutableStateFlow<String?>(null)

    init {
        run { catalog.value = loadEditorialCatalog(application) }
        viewModelScope.launch {
            repository.cleanupStaging()
            preferences.state.first()
            ready.value = true
        }
    }

    fun run(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                message.value = e.message ?: "Não foi possível concluir. Tente novamente."
            }
        }
    }

    fun import(uri: Uri, allowDuplicate: Boolean = false) {
        run {
            busy.value = true
            try {
                importedBook.value = repository.import(uri, allowDuplicate)
                message.value = "PDF importado. Toque no arquivo para ler."
            } catch (e: DuplicatePdfException) {
                duplicateForPreview = false
                duplicate.value = uri to e.existingId
            } catch (e: Exception) {
                throw IllegalArgumentException(
                    e.message?.takeIf { it.contains("200 MB") }
                        ?: "Não foi possível abrir este PDF. Ele pode estar danificado ou protegido por senha.",
                    e,
                )
            } finally {
                busy.value = false
            }
        }
    }

    fun prepareImport(uri: Uri, allowDuplicate: Boolean = false) {
        val previous = previewJob
        previous?.cancel()
        previewJob = viewModelScope.launch {
            previous?.join()
            busy.value = true
            try {
                importPreview.value?.let { repository.discardImport(it) }
                importPreview.value = null
                importedBook.value = null
                importPreview.value = repository.prepareImport(uri, allowDuplicate)
            } catch (e: DuplicatePdfException) {
                duplicateForPreview = true
                duplicate.value = uri to e.existingId
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { message.value = if (e is java.io.IOException) "Não foi possível ler ou armazenar o PDF. Verifique o arquivo e o espaço disponível." else e.message ?: "Não foi possível preparar o PDF." }
            finally { busy.value = false }
        }
    }

    fun discardImport() = run {
        previewJob?.cancelAndJoin()
        importPreview.value?.let { repository.discardImport(it) }
        importPreview.value = null
        importFolders.value = emptySet()
    }

    fun finishImport() = run {
        if (busy.value) return@run
        val value = importPreview.value ?: return@run
        val folders = importFolders.value
        busy.value = true
        try {
            importedBook.value = repository.finishImport(value, folders)
            importPreview.value = null
            importFolders.value = emptySet()
            message.value = "PDF importado e organizado."
        } catch (e: Exception) { importPreview.value = null; throw e }
        finally { busy.value = false }
    }

    suspend fun clearThumbnailCache() { app.thumbnails.clear(); message.value = "Cache de miniaturas limpo." }

    fun remove(id: String) = run {
        repository.remove(id)
        app.thumbnails.evict(id)
    }

    fun restart(id: String, open: () -> Unit) = run {
        repository.restart(id)
        open()
    }

    fun theme(v: String) = run { preferences.theme(v) }

    fun enter() = run { preferences.enter() }

    /** Persists the whole Metas page. Both stores are written; if the second write fails the first is rolled back. */
    fun saveGoalsPage(daily: Int, weekly: Int, consistency: Int, chartPages: Int, chartMinutes: Int, record: Boolean, showGoals: Boolean, done: (Throwable?) -> Unit) {
        viewModelScope.launch {
            val before = prefs.value; val goalsBefore = workspace.value
            try {
                preferences.saveGoalsPage(chartPages, chartMinutes, record, showGoals)
                try { workspaceRepository.goals(daily, weekly, consistency) }
                catch (e: Exception) {
                    preferences.saveGoalsPage(before.chartPages, before.chartMinutes, before.recordActivity, before.showGoals)
                    workspaceRepository.goals(goalsBefore.daily, goalsBefore.weekly, goalsBefore.consistency)
                    throw e
                }
                done(null)
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { done(e) }
        }
    }

    fun group(name: String, kind: String) = run {
        val clean = name.trim()
        if (groups.value.any { it.kind == kind && it.name.equals(clean, true) })
            error("Esse nome já existe.")
        repository.group(clean, kind)
    }
}
