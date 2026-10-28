package br.com.lottan.nativeapp.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object ImportNotice {
    const val VERSION = 1
    const val TITLE = "Antes de importar seu documento"
    const val TEXT = "O ODMANI permite que você leia e organize arquivos PDF pessoais. Os documentos importados são de sua responsabilidade. A Lottan e o ODMANI não produzem, verificam ou endossam esses arquivos. Respeite os direitos de terceiros e consulte nossas regras de utilização."
}

data class LegalSource(val id: String, val title: String, val url: String)
data class LegalBlock(val heading: String, val text: String)
data class LegalSection(val id: String, val title: String, val blocks: List<LegalBlock>, val sources: List<String>)
data class TransparencyDocument(val id: String, val version: String, val prepared: String, val effective: String, val status: String, val title: String, val description: String, val sections: List<LegalSection>, val sources: List<LegalSource>)

suspend fun loadTransparency(context: Context): TransparencyDocument = withContext(Dispatchers.IO) {
    val doc = context.assets.open("transparency.json").bufferedReader().use { JSONObject(it.readText()) }
    val sources = doc.getJSONArray("sources")
    val sections = doc.getJSONArray("sections")
    TransparencyDocument(doc.getString("id"), doc.getString("version"), doc.getString("prepared"), doc.getString("effective"), doc.getString("status"), doc.getString("title"), doc.getString("description"),
        (0 until sections.length()).map { i ->
            val section = sections.getJSONObject(i); val blocks = section.getJSONArray("blocks"); val refs = section.getJSONArray("sources")
            LegalSection(section.getString("id"), section.getString("title"), (0 until blocks.length()).map { j -> blocks.getJSONObject(j).let { LegalBlock(it.getString("heading"), it.getString("text")) } }, (0 until refs.length()).map { refs.getString(it) })
        }, (0 until sources.length()).map { i -> sources.getJSONObject(i).let { LegalSource(it.getString("id"), it.getString("title"), it.getString("url")) } })
}
