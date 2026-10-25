package br.com.lottan.nativeapp

import android.app.Application
import androidx.room.Room
import br.com.lottan.nativeapp.data.*

class LottanApplication : Application() {
    val background =
        kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
        )
    val database by lazy {
        Room.databaseBuilder(this, LottanDatabase::class.java, "lottan.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }
    val library by lazy { LibraryRepository(this, database) }
    val thumbnails by lazy { ThumbnailRepository(this, library) }
    val preferences by lazy { PreferenceRepository(this) }
}
