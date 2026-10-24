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

@Dao
interface LibraryDao {
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

@Database(entities = [Book::class, Group::class, Day::class], version = 2, exportSchema = true)
abstract class LottanDatabase : RoomDatabase() {
    abstract fun library(): LibraryDao
}
