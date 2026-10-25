package br.com.lottan.nativeapp.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

private val Context.workspaceStore by preferencesDataStore("odmani_workspace")
data class TaskItem(val id: String, val text: String, val done: Boolean = false, val page: Int? = null)
data class DocumentSpace(val note: String = "", val mode: String = "Anotações", val tasks: List<TaskItem> = emptyList())
data class Workspace(
    val pinned: Set<String> = emptySet(), val main: String = "",
    val spaces: Map<String, DocumentSpace> = emptyMap(),
    val daily: Int = 0, val weekly: Int = 0, val consistency: Int = 0,
)
class WorkspaceRepository(private val context: Context) {
    fun comfort(id: String) = context.workspaceStore.data.map { p ->
        val j = JSONObject(p[stringPreferencesKey("reader:$id")] ?: p[stringPreferencesKey("reader:global")] ?: "{}")
        val marks = j.optJSONArray("bookmarks") ?: JSONArray()
        ReaderComfort(j.optString("background", "Preto"), j.optString("filter", "Neutro"), j.optDouble("intensity", .25).toFloat(), j.optBoolean("awake"), j.optDouble("zoom", 1.0).toFloat(), j.optString("fit", "page"), (0 until marks.length()).map { marks.getString(it) }.toSet(), j.optDouble("brightness", -1.0).toFloat(), j.optString("orientation", "Sistema"), j.optBoolean("immersive"), j.optInt("spacing", 12), j.optString("direction"), j.optInt("rotation", -1).takeIf { it in listOf(0, 90, 180, 270) })
    }
    suspend fun comfort(id: String, value: ReaderComfort) { context.workspaceStore.edit { p ->
        val j = JSONObject().put("background", value.background).put("filter", value.filter).put("intensity", value.intensity.toDouble()).put("awake", value.keepAwake).put("zoom", value.zoom.toDouble()).put("fit", value.fit).put("bookmarks", JSONArray(value.bookmarks.toList())).put("brightness", value.brightness.toDouble()).put("orientation", value.orientation).put("immersive", value.immersive).put("spacing", value.spacing).put("direction", value.direction).put("rotation", value.rotation ?: -1)
        p[stringPreferencesKey("reader:$id")] = j.toString()
    } }
    val state = context.workspaceStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { p ->
        val json = JSONObject(p[stringPreferencesKey("spaces")] ?: "{}")
        val spaces = json.keys().asSequence().associateWith { id ->
            val d = json.getJSONObject(id)
            val tasks = d.optJSONArray("tasks") ?: JSONArray()
            DocumentSpace(d.optString("note"), d.optString("mode", "Anotações"), (0 until tasks.length()).map { n ->
                val t = tasks.getJSONObject(n); TaskItem(t.getString("id"), t.getString("text"), t.optBoolean("done"), t.optInt("page", -1).takeIf { it >= 0 })
            })
        }
        Workspace(p[stringSetPreferencesKey("pinned")] ?: emptySet(), p[stringPreferencesKey("main")] ?: "", spaces,
            p[intPreferencesKey("daily")] ?: 0, p[intPreferencesKey("weekly")] ?: 0, p[intPreferencesKey("consistency")] ?: 0)
    }
    suspend fun pin(id: String) { context.workspaceStore.edit { p ->
        val k = stringSetPreferencesKey("pinned"); val set = (p[k] ?: emptySet()).toMutableSet()
        if (!set.add(id)) set.remove(id); p[k] = set
    } }
    suspend fun main(id: String) { context.workspaceStore.edit { it[stringPreferencesKey("main")] = id } }
    suspend fun goals(daily: Int, weekly: Int, consistency: Int) { context.workspaceStore.edit {
        it[intPreferencesKey("daily")] = daily.coerceIn(0, 10000)
        it[intPreferencesKey("weekly")] = weekly.coerceIn(0, 70000)
        it[intPreferencesKey("consistency")] = consistency.coerceIn(0, 7)
    } }
    suspend fun space(id: String, value: DocumentSpace) { context.workspaceStore.edit { p ->
        val k = stringPreferencesKey("spaces"); val json = JSONObject(p[k] ?: "{}")
        val tasks = JSONArray(); value.tasks.forEach { t -> tasks.put(JSONObject().put("id", t.id).put("text", t.text).put("done", t.done).put("page", t.page ?: -1)) }
        json.put(id, JSONObject().put("note", value.note).put("mode", value.mode).put("tasks", tasks)); p[k] = json.toString()
    } }
}

data class ReaderComfort(val background: String = "Preto", val filter: String = "Neutro", val intensity: Float = .25f, val keepAwake: Boolean = false, val zoom: Float = 1f, val fit: String = "page", val bookmarks: Set<String> = emptySet(), val brightness: Float = -1f, val orientation: String = "Sistema", val immersive: Boolean = false, val spacing: Int = 12, val direction: String = "", val rotation: Int? = null)
