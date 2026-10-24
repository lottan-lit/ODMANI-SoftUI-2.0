package br.com.lottan.nativeapp

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.lottan.nativeapp.data.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    @Test
    fun progressAndGroupRemovalPreserveBook() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, LottanDatabase::class.java).build()
        try {
            val dao = db.library()
            dao.put(Book("test", "Livro.pdf", 5, 100, folder = "folder"))
            dao.progress("test", 4, true)
            dao.put(Group("folder", "Estudos", "folders"))
            dao.clearFolder("folder")
            dao.deleteGroup("folder")
            val b = dao.book("test")!!
            Assert.assertEquals(4, b.page)
            Assert.assertTrue(b.completed)
            Assert.assertEquals("", b.folder)
        } finally {
            db.close()
        }
    }
}
