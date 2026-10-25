package br.com.lottan.nativeapp
import androidx.test.platform.app.InstrumentationRegistry
import br.com.lottan.nativeapp.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID
class WorkspacePersistenceTest {
    @Test fun switchingSpaceModeRetainsNotesAndTasks() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = WorkspaceRepository(context)
        val id = UUID.randomUUID().toString()
        val task = TaskItem("task", "Conferir página 2", true)
        repository.space(id, DocumentSpace("Minha anotação", "Checklist", listOf(task)))
        repository.space(id, DocumentSpace("Minha anotação", "Desativado", listOf(task)))
        val reopened = WorkspaceRepository(context).state.first().spaces[id]!!
        assertEquals("Minha anotação", reopened.note)
        assertEquals("Desativado", reopened.mode)
        assertEquals(listOf(task), reopened.tasks)
    }
    @Test fun pinsMainGoalsAndComfortAreIndependentAndPersisted() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = WorkspaceRepository(context)
        val before = repository.state.first()
        val id = UUID.randomUUID().toString()
        try {
            repository.pin(id); repository.main(id); repository.goals(25, 125, 4)
            val comfort = ReaderComfort(background = "Sépia", filter = "Creme", zoom = 1.5f, bookmarks = setOf("2"), spacing = 20)
            repository.comfort(id, comfort)
            val reopened = WorkspaceRepository(context)
            val data = reopened.state.first()
            assertTrue(id in data.pinned); assertEquals(id, data.main)
            assertEquals(25, data.daily); assertEquals(125, data.weekly); assertEquals(4, data.consistency)
            assertEquals(comfort, reopened.comfort(id).first())
            repository.pin(id)
            assertEquals(id, repository.state.first().main)
        } finally { repository.main(before.main); repository.goals(before.daily, before.weekly, before.consistency) }
    }
}
