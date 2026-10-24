package br.com.lottan.nativeapp.ui

import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import br.com.lottan.nativeapp.LottanApplication
import br.com.lottan.nativeapp.data.Book
import br.com.lottan.nativeapp.domain.ReadingRules
import kotlinx.coroutines.CancellationException

private sealed interface CoverState {
    data object Loading : CoverState

    data class Ready(val bitmap: Bitmap) : CoverState

    data object Failed : CoverState
}

@Composable
fun PdfCover(book: Book, modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as LottanApplication
    var state by remember(book.id, book.bytes) { mutableStateOf<CoverState>(CoverState.Loading) }
    LaunchedEffect(book.id, book.bytes) {
        state = CoverState.Loading
        state =
            try {
                CoverState.Ready(app.thumbnails.load(book))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                CoverState.Failed
            } catch (_: OutOfMemoryError) {
                CoverState.Failed
            }
    }
    val p = LocalVisual.current
    // The wrapper may be rounded; the original page itself is neither clipped nor decorated.
    Box(
        modifier
            .shadow(VisualTokens.CardElevation, RoundedCornerShape(8.dp))
            .background(p.secondary, RoundedCornerShape(8.dp))
            .border(1.dp, p.border, RoundedCornerShape(8.dp))
            .padding(1.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (val cover = state) {
            is CoverState.Ready ->
                Image(
                    cover.bitmap.asImageBitmap(),
                    "Capa original de ${ReadingRules.title(book.name)}",
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            CoverState.Loading ->
                CircularProgressIndicator(
                    Modifier.size(18.dp),
                    color = p.muted,
                    strokeWidth = 1.5.dp,
                )
            CoverState.Failed ->
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PDF", fontSize = 10.sp, color = p.muted)
                    Text(
                        ReadingRules.title(book.name),
                        fontSize = 11.sp,
                        color = p.text,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
        }
    }
}
