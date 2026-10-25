package br.com.lottan.nativeapp.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val id: String,
    val name: String,
    val pages: Int,
    val bytes: Long,
    val added: Long = System.currentTimeMillis(),
    val page: Int = 0,
    val folder: String = "",
    val category: String = "",
    val completed: Boolean = false,
    val lastOpened: Long = 0,
    @ColumnInfo(defaultValue = "'personal'") val origin: String = "personal",
)

@Entity(tableName = "groups")
data class Group(@PrimaryKey val id: String, val name: String, val kind: String)

@Entity(tableName = "days")
data class Day(@PrimaryKey val date: String, val millis: Long = 0, val visits: Long = 0)

@Entity(tableName = "folder_books", primaryKeys = ["folderId", "bookId"])
data class FolderBook(val folderId: String, val bookId: String)

@Entity(tableName = "read_pages", primaryKeys = ["bookId", "page"])
data class ReadPage(val bookId: String, val page: Int)

@Entity(tableName = "reading_sessions")
data class ReadingSession(@PrimaryKey val id: String, val started: Long, val millis: Long = 0, val visits: Long = 0)

@Entity(tableName = "reading_page_events", primaryKeys = ["sessionId", "bookId", "page"])
data class ReadingPageEvent(val sessionId: String, val bookId: String, val page: Int, val date: String)

@Dao
interface LibraryDao {
    @Query("SELECT EXISTS(SELECT 1 FROM reading_page_events WHERE date=:date AND bookId=:bookId AND page=:page)") suspend fun countedToday(date: String, bookId: String, page: Int): Boolean
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun event(value: ReadingPageEvent): Long
    @Query("SELECT * FROM reading_sessions ORDER BY started") fun sessions(): Flow<List<ReadingSession>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun put(value: ReadingSession): Long
    @Query("UPDATE reading_sessions SET millis=millis+:millis, visits=visits+:visits WHERE id=:id") suspend fun sessionActivity(id: String, millis: Long, visits: Long)
    @Query("SELECT COUNT(*) FROM read_pages") fun uniquePages(): Flow<Int>
    @Query("SELECT * FROM folder_books") fun memberships(): Flow<List<FolderBook>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addFolder(value: FolderBook)
    @Query("DELETE FROM folder_books WHERE folderId=:folder AND bookId=:book") suspend fun removeFolder(folder: String, book: String)
    @Query("DELETE FROM folder_books WHERE folderId=:id") suspend fun removeMemberships(id: String)
    @Query("DELETE FROM folder_books WHERE bookId=:id") suspend fun removeBookMemberships(id: String)
    @Query("DELETE FROM read_pages WHERE bookId=:id") suspend fun removeReadPages(id: String)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun readPage(value: ReadPage)
    @Query("UPDATE books SET completed=completed OR :completed WHERE id=:id") suspend fun completion(id: String, completed: Boolean)
    @Query("SELECT COUNT(*) FROM read_pages WHERE bookId=:id") suspend fun readCount(id: String): Int
    @Query("UPDATE groups SET name=:name WHERE id=:id") suspend fun renameGroup(id: String, name: String)
    @Query("SELECT * FROM books ORDER BY added DESC") fun books(): Flow<List<Book>>

    @Query("SELECT * FROM groups ORDER BY name COLLATE NOCASE") fun groups(): Flow<List<Group>>

    @Query("SELECT * FROM days ORDER BY date") fun days(): Flow<List<Day>>

    @Query("SELECT * FROM books WHERE id=:id") suspend fun book(id: String): Book?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(book: Book)

    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun put(group: Group)

    @Query("SELECT * FROM days WHERE date=:date") suspend fun day(date: String): Day?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(day: Day)

    @Query("DELETE FROM books WHERE id=:id") suspend fun delete(id: String)

    @Query("UPDATE books SET folder=:value WHERE id=:id")
    suspend fun folder(id: String, value: String)

    @Query("UPDATE books SET category=:value WHERE id=:id")
    suspend fun category(id: String, value: String)

    @Query("UPDATE books SET folder='' WHERE folder=:id") suspend fun clearFolder(id: String)

    @Query("UPDATE books SET category='' WHERE category=:id") suspend fun clearCategory(id: String)

    @Query("DELETE FROM groups WHERE id=:id") suspend fun deleteGroup(id: String)

    @Query(
        "UPDATE books SET page=:page, completed=completed OR :completed, lastOpened=:opened WHERE id=:id"
    )
    suspend fun progress(
        id: String,
        page: Int,
        completed: Boolean,
        opened: Long = System.currentTimeMillis(),
    )
}

val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE books ADD COLUMN origin TEXT NOT NULL DEFAULT 'personal'")
        }
    }

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS folder_books (folderId TEXT NOT NULL, bookId TEXT NOT NULL, PRIMARY KEY(folderId, bookId))")
        db.execSQL("INSERT OR IGNORE INTO folder_books (folderId, bookId) SELECT folder, id FROM books WHERE folder != ''")
        db.execSQL("CREATE TABLE IF NOT EXISTS read_pages (bookId TEXT NOT NULL, page INTEGER NOT NULL, PRIMARY KEY(bookId, page))")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS reading_sessions (id TEXT NOT NULL, started INTEGER NOT NULL, millis INTEGER NOT NULL, visits INTEGER NOT NULL, PRIMARY KEY(id))")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS reading_page_events (sessionId TEXT NOT NULL, bookId TEXT NOT NULL, page INTEGER NOT NULL, date TEXT NOT NULL, PRIMARY KEY(sessionId, bookId, page))")
    }
}

@Database(entities = [Book::class, Group::class, Day::class, FolderBook::class, ReadPage::class, ReadingSession::class, ReadingPageEvent::class], version = 5, exportSchema = true)
abstract class LottanDatabase : RoomDatabase() {
    abstract fun library(): LibraryDao
}
