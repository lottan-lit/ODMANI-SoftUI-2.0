package br.com.lottan.nativeapp.ui
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FolderCover(id: String, revision: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null, id, revision) {
        value = withContext(Dispatchers.IO) { android.graphics.BitmapFactory.decodeFile(File(context.filesDir, "folder-covers/$id.jpg").path) }
    }
    Box(modifier.fillMaxWidth().aspectRatio(2f).background(LocalVisual.current.secondary), contentAlignment = Alignment.Center) {
        val image = bitmap
        if (image == null) Glyph("library")
        else Image(image.asImageBitmap(), "Capa da pasta", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
fun FolderPreview(uri: android.net.Uri, crop: Float) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            try {
                val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, options) }
                var sample = 1
                while (options.outWidth / sample > 1024 || options.outHeight / sample > 1024) sample *= 2
                options.inJustDecodeBounds = false; options.inSampleSize = sample
                context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, options) }
            } catch (_: Exception) { null }
        }
    }
    val image = bitmap
    if (image != null) Image(image.asImageBitmap(), "Prévia do recorte", Modifier.fillMaxWidth().aspectRatio(2f), contentScale = ContentScale.Crop, alignment = BiasAlignment(crop * 2 - 1, crop * 2 - 1))
}
