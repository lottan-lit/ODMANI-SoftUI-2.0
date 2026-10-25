package br.com.lottan.nativeapp.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import br.com.lottan.nativeapp.LottanApplication
import br.com.lottan.nativeapp.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
    val groups =
        repository.groups.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val days =
        repository.days.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val prefs = preferences.state.stateIn(viewModelScope, SharingStarted.Eagerly, Preferences())
    val ready = MutableStateFlow(false)
    val busy = MutableStateFlow(false)
    val duplicate = MutableStateFlow<Pair<Uri, String>?>(null)
    val coverRevision = MutableStateFlow(0)
    val message = MutableStateFlow<String?>(null)

    init {
        run { catalog.value = loadEditorialCatalog(application) }
        viewModelScope.launch {
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
                repository.import(uri, allowDuplicate)
                message.value = "PDF importado. Toque no arquivo para ler."
            } catch (e: DuplicatePdfException) {
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

    fun group(name: String, kind: String) = run {
        val clean = name.trim()
        if (groups.value.any { it.kind == kind && it.name.equals(clean, true) })
            error("Esse nome já existe.")
        repository.group(clean, kind)
    }
}
