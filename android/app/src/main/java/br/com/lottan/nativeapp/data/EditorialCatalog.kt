package br.com.lottan.nativeapp.data
import android.content.Context
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Publication(
    val id: String, val title: String, val author: String, val synopsis: String,
    val genre: String, val pages: Int, val language: String, val edition: String,
    val published: String, val isbn: String, val additional: String,
    val price: String, val availability: String, val productUrl: String, val coverAsset: String,
)
data class EditorialCatalog(val siteUrl: String = "", val commercialLinksEnabled: Boolean = false, val publications: List<Publication> = emptyList()) {
    fun officialUrl(value: String): Boolean = runCatching {
        val site = java.net.URI(siteUrl); val uri = java.net.URI(value)
        site.scheme == "https" && !site.host.isNullOrBlank() && uri.scheme == "https" && uri.host == site.host && uri.userInfo == null
    }.getOrDefault(false)
}
suspend fun loadEditorialCatalog(context: Context) = withContext(Dispatchers.IO) {
    val json = context.assets.open("lottan-catalog.json").bufferedReader().use { JSONObject(it.readText()) }
    val entries = json.getJSONArray("publications")
    EditorialCatalog(json.optString("siteUrl"), json.optBoolean("commercialLinksEnabled"), (0 until entries.length()).map { index ->
        val p = entries.getJSONObject(index)
        Publication(p.getString("id"), p.getString("title"), p.optString("author"), p.optString("synopsis"), p.optString("genre"), p.optInt("pages"), p.optString("language"), p.optString("edition"), p.optString("published"), p.optString("isbn"), p.optString("additional"), p.optString("price"), p.optString("availability"), p.optString("productUrl"), p.optString("coverAsset"))
    }.also { entries -> require(entries.map { it.id }.distinct().size == entries.size && entries.all { it.id.matches(Regex("[A-Za-z0-9_-]+")) }) })
}
