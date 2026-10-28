package br.com.lottan.nativeapp.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.*

private val Context.store by preferencesDataStore("lottan_native")

data class Preferences(
    val theme: String = "dark",
    val entered: Boolean = false,
    val fit: String = "width",
    val rotation: Int = 0,
    val zoom: Float = 1f,
    val direction: String = "single",
    val textSize: Float = 18f,
    val lineHeight: Float = 1.5f,
    val margin: String = "normal",
    val hideControls: Boolean = true,
    val favorites: Set<String> = emptySet(),
    val bookmarks: Set<String> = emptySet(),
    val demoPages: List<Int> = listOf(48, 184, 20, 2),
    val recordActivity: Boolean = true,
    val showGoals: Boolean = true,
    val highContrast: Boolean = false,
    val librarySort: String = "Recentes",
    val libraryView: String = "grid",
    val importNoticeVersion: Int = 0,
    val chartPages: Int = 30,
    val chartMinutes: Int = 30,
    val animations: Boolean = true,
)

class PreferenceRepository(private val context: Context) {
    val state =
        context.store.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { p ->
                Preferences(
                    p[stringPreferencesKey("theme")] ?: "dark",
                    p[booleanPreferencesKey("entered")] ?: false,
                    p[stringPreferencesKey("fit")] ?: "width",
                    p[intPreferencesKey("rotation")] ?: 0,
                    p[floatPreferencesKey("zoom")] ?: 1f,
                    p[stringPreferencesKey("direction")] ?: "single",
                    p[floatPreferencesKey("textSize")] ?: 18f,
                    p[floatPreferencesKey("lineHeight")] ?: 1.5f,
                    p[stringPreferencesKey("margin")] ?: "normal",
                    p[booleanPreferencesKey("hideControls")] ?: true,
                    p[stringSetPreferencesKey("favorites")] ?: emptySet(),
                    p[stringSetPreferencesKey("bookmarks")] ?: emptySet(),
                    (0..3).map { p[intPreferencesKey("demo$it")] ?: listOf(48, 184, 20, 2)[it] },
                    p[booleanPreferencesKey("recordActivity")] ?: true,
                    p[booleanPreferencesKey("showGoals")] ?: true,
                    p[booleanPreferencesKey("highContrast")] ?: false,
                    p[stringPreferencesKey("librarySort")] ?: "Recentes",
                    p[stringPreferencesKey("libraryView")]?.takeIf { it in setOf("grid", "list") } ?: "grid",
                    p[intPreferencesKey("importNoticeVersion")] ?: 0,
                    (p[intPreferencesKey("chartPages")] ?: 30).coerceIn(1, 1000000),
                    (p[intPreferencesKey("chartMinutes")] ?: 30).coerceIn(1, 525600),
                    p[booleanPreferencesKey("animations")] ?: true,
                )
            }

    suspend fun chartScale(pages: Int, minutes: Int) {
        require(pages in 1..1000000 && minutes in 1..525600)
        context.store.edit { it[intPreferencesKey("chartPages")] = pages; it[intPreferencesKey("chartMinutes")] = minutes }
    }

    /** One DataStore edit: chart scale and the two display/recording switches are persisted together or not at all. */
    suspend fun saveGoalsPage(pages: Int, minutes: Int, record: Boolean, showGoals: Boolean) {
        require(pages in 1..1000000 && minutes in 1..525600)
        context.store.edit {
            it[intPreferencesKey("chartPages")] = pages; it[intPreferencesKey("chartMinutes")] = minutes
            it[booleanPreferencesKey("recordActivity")] = record; it[booleanPreferencesKey("showGoals")] = showGoals
        }
    }

    suspend fun noticePresented(version: Int) { context.store.edit { it[intPreferencesKey("importNoticeVersion")] = maxOf(it[intPreferencesKey("importNoticeVersion")] ?: 0, version) } }

    suspend fun libraryView(value: String) {
        require(value in setOf("grid", "list"))
        context.store.edit { it[stringPreferencesKey("libraryView")] = value }
    }

    suspend fun sort(value: String) { context.store.edit { it[stringPreferencesKey("librarySort")] = value } }

    suspend fun flag(key: String, value: Boolean) { context.store.edit { it[booleanPreferencesKey(key)] = value } }

    suspend fun theme(v: String) {
        context.store.edit { it[stringPreferencesKey("theme")] = v }
    }

    suspend fun enter() {
        context.store.edit { it[booleanPreferencesKey("entered")] = true }
    }

    suspend fun fit(v: String) {
        context.store.edit { it[stringPreferencesKey("fit")] = v }
    }

    suspend fun rotation(v: Int) {
        context.store.edit { it[intPreferencesKey("rotation")] = v }
    }

    suspend fun zoom(v: Float) {
        context.store.edit { it[floatPreferencesKey("zoom")] = v }
    }

    suspend fun direction(v: String) {
        context.store.edit { it[stringPreferencesKey("direction")] = v }
    }

    suspend fun text(v: Float) {
        context.store.edit { it[floatPreferencesKey("textSize")] = v }
    }

    suspend fun spacing(v: Float) {
        context.store.edit { it[floatPreferencesKey("lineHeight")] = v }
    }

    suspend fun margin(v: String) {
        context.store.edit { it[stringPreferencesKey("margin")] = v }
    }

    suspend fun hide(v: Boolean) {
        context.store.edit { it[booleanPreferencesKey("hideControls")] = v }
    }

    suspend fun toggle(key: String, id: Int) {
        context.store.edit {
            val k = stringSetPreferencesKey(key)
            val set = (it[k] ?: emptySet()).toMutableSet()
            if (!set.add(id.toString())) set.remove(id.toString())
            it[k] = set
        }
    }

    suspend fun demoPage(id: Int, page: Int) {
        context.store.edit { it[intPreferencesKey("demo$id")] = page }
    }

    suspend fun resetPdf() {
        context.store.edit {
            it[stringPreferencesKey("fit")] = "width"
            it[intPreferencesKey("rotation")] = 0
            it[floatPreferencesKey("zoom")] = 1f
            it[stringPreferencesKey("direction")] = "single"
            it[stringPreferencesKey("margin")] = "normal"
        }
    }
}
