package br.com.lottan.nativeapp

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import br.com.lottan.nativeapp.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Runs against a real v1 database; never touches the installed application's library. */
class MigrationTest {
    @Test
    fun versionOneKeepsLibraryHistoryAndAssignments() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-softui-test.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `books` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `pages` INTEGER NOT NULL, `bytes` INTEGER NOT NULL, `added` INTEGER NOT NULL, `page` INTEGER NOT NULL, `folder` TEXT NOT NULL, `category` TEXT NOT NULL, `completed` INTEGER NOT NULL, `lastOpened` INTEGER NOT NULL, PRIMARY KEY(`id`))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `groups` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `kind` TEXT NOT NULL, PRIMARY KEY(`id`))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `days` (`date` TEXT NOT NULL, `millis` INTEGER NOT NULL, `visits` INTEGER NOT NULL, PRIMARY KEY(`date`))"
            )
            db.execSQL(
                "INSERT INTO books VALUES ('document','Lottan pessoal.pdf',58,1024,10,57,'folder','category',1,99)"
            )
            db.execSQL("INSERT INTO groups VALUES ('folder','Estudos','folders')")
            db.execSQL("INSERT INTO days VALUES ('2026-10-24',60000,8)")
            db.version = 1
        }
        val migrated =
            Room.databaseBuilder(context, LottanDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
        try {
            val book = migrated.library().book("document")!!
            assertEquals(FolderBook("folder", "document"), migrated.library().memberships().first().single())
            assertEquals("personal", book.origin)
            assertEquals(57, book.page)
            assertTrue(book.completed)
            assertEquals("folder", book.folder)
            assertEquals("category", book.category)
            assertEquals(99L, book.lastOpened)
            assertEquals(60000L, migrated.library().day("2026-10-24")!!.millis)
            assertEquals(8L, migrated.library().day("2026-10-24")!!.visits)
            assertEquals("Estudos", migrated.library().groups().first().single().name)
        } finally {
            migrated.close()
            context.deleteDatabase(name)
        }
    }
}
