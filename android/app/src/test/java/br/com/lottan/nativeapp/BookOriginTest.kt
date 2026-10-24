package br.com.lottan.nativeapp

import br.com.lottan.nativeapp.data.Book
import org.junit.Assert.*
import org.junit.Test

class BookOriginTest {
    @Test
    fun namedLikePublisherIsStillPersonal() {
        assertEquals("personal", Book("1", "Lottan - Temos Todo Tempo.pdf", 58, 1000).origin)
    }

    @Test
    fun copyingProgressDoesNotChangeOriginOrHistory() {
        val old =
            Book("1", "Documento.pdf", 58, 1000, page = 57, completed = true, lastOpened = 999)
        val restarted = old.copy(page = 0)
        assertTrue(restarted.completed)
        assertEquals(old.origin, restarted.origin)
        assertEquals(old.lastOpened, restarted.lastOpened)
    }
}
