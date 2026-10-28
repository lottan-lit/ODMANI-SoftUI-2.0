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
data class Group(@PrimaryKey val id: String, val name: String, val kind: String,
    @ColumnInfo(defaultValue = "'neutral'") val color: String = "neutral",
    @ColumnInfo(defaultValue = "'folder'") val icon: String = "folder")

@Entity(tableName = "days")
data class Day(@PrimaryKey val date: String, val millis: Long = 0, val visits: Long = 0)

@Entity(tableName = "folder_books", primaryKeys = ["folderId", "bookId"],
    foreignKeys = [ForeignKey(entity = Group::class, parentColumns = ["id"], childColumns = ["folderId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Book::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["bookId"])])
data class FolderBook(val folderId: String, val bookId: String)

/** Many-to-many link between a PDF and a user category (groups.kind = 'categories'). Never duplicates files. */
@Entity(tableName = "book_categories", primaryKeys = ["categoryId", "bookId"],
    foreignKeys = [ForeignKey(entity = Group::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Book::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["bookId"])])
data class BookCategory(val categoryId: String, val bookId: String)

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
    @Query("SELECT * FROM book_categories") fun categoryLinks(): Flow<List<BookCategory>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addCategory(value: BookCategory)
    @Query("DELETE FROM book_categories WHERE categoryId=:category AND bookId=:book") suspend fun removeCategory(category: String, book: String)
    @Query("DELETE FROM book_categories WHERE categoryId=:id") suspend fun removeCategoryLinks(id: String)
    @Query("DELETE FROM book_categories WHERE bookId=:id") suspend fun removeBookCategories(id: String)
    @Query("SELECT * FROM book_categories") suspend fun categoryLinksNow(): List<BookCategory>
    @Query("SELECT * FROM folder_books") suspend fun membershipsNow(): List<FolderBook>
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

    @Query("UPDATE groups SET name=:name, color=:color, icon=:icon WHERE id=:id") suspend fun updateGroup(id: String, name: String, color: String, icon: String)

    @Query("SELECT * FROM groups ORDER BY name COLLATE NOCASE") fun groups(): Flow<List<Group>>

    @Query("SELECT * FROM days ORDER BY date") fun days(): Flow<List<Day>>

    @Query("SELECT * FROM books WHERE id=:id") suspend fun book(id: String): Book?

    @Upsert suspend fun put(book: Book)

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

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE groups ADD COLUMN color TEXT NOT NULL DEFAULT 'neutral'")
        db.execSQL("ALTER TABLE groups ADD COLUMN icon TEXT NOT NULL DEFAULT 'folder'")
        db.execSQL("CREATE TABLE folder_books_new (folderId TEXT NOT NULL, bookId TEXT NOT NULL, PRIMARY KEY(folderId, bookId), FOREIGN KEY(folderId) REFERENCES groups(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(bookId) REFERENCES books(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("INSERT OR IGNORE INTO folder_books_new SELECT f.folderId, f.bookId FROM folder_books f JOIN groups g ON g.id=f.folderId JOIN books b ON b.id=f.bookId")
        db.execSQL("INSERT OR IGNORE INTO folder_books_new SELECT b.folder, b.id FROM books b JOIN groups g ON g.id=b.folder WHERE b.folder != '' AND g.kind='folders'")
        db.execSQL("DROP TABLE folder_books")
        db.execSQL("ALTER TABLE folder_books_new RENAME TO folder_books")
        db.execSQL("CREATE INDEX index_folder_books_bookId ON folder_books(bookId)")
    }
}

/** Adds the many-to-many category table. Additive only: books.category stays in place and is copied into the new links. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS book_categories (categoryId TEXT NOT NULL, bookId TEXT NOT NULL, PRIMARY KEY(categoryId, bookId), FOREIGN KEY(categoryId) REFERENCES groups(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(bookId) REFERENCES books(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_book_categories_bookId ON book_categories(bookId)")
        db.execSQL("INSERT OR IGNORE INTO book_categories (categoryId, bookId) SELECT b.category, b.id FROM books b JOIN groups g ON g.id=b.category WHERE b.category != '' AND g.kind='categories'")
    }
}

@Database(entities = [Book::class, Group::class, Day::class, FolderBook::class, BookCategory::class, ReadPage::class, ReadingSession::class, ReadingPageEvent::class], version = 7, exportSchema = true)
abstract class LottanDatabase : RoomDatabase() {
    abstract fun library(): LibraryDao
}
