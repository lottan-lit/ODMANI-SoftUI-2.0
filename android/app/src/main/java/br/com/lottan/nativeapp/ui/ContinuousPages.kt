package br.com.lottan.nativeapp.ui

import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import br.com.lottan.nativeapp.data.Book

@Composable
fun ContinuousPages(vm: ReaderViewModel, book: Book, state: LazyListState, filter: ColorFilter?, width: Int, zoom: Float, pan: androidx.compose.ui.geometry.Offset, spacing: Int) {
    LazyColumn(state = state, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing.dp)) {
        items(book.pages, key = { it }) { index ->
            val bitmap by produceState<Bitmap?>(null, index, width) {
                try { value = vm.renderPage(index, width.coerceIn(360, 1800)) }
                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (_: Exception) { value = null }
            }
            val image = bitmap
            if (image == null) Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                Caption("Carregando página ${index + 1}…")
            } else Image(image.asImageBitmap(), "Página ${index + 1}", Modifier.fillMaxWidth().aspectRatio(image.width.toFloat() / image.height).graphicsLayer { scaleX = zoom; scaleY = zoom; translationX = pan.x; translationY = pan.y }, colorFilter = filter, contentScale = ContentScale.Fit)
        }
    }
}
