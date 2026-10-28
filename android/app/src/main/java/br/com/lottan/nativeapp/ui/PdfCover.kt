package br.com.lottan.nativeapp.ui

import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import br.com.lottan.nativeapp.LottanApplication
import br.com.lottan.nativeapp.data.Book
import br.com.lottan.nativeapp.domain.CoverFraming
import br.com.lottan.nativeapp.domain.ReadingRules
import kotlinx.coroutines.CancellationException

private sealed interface CoverState {
    data object Loading : CoverState

    data class Ready(val bitmap: Bitmap, val analysis: CoverFraming.Analysis) : CoverState

    data object Failed : CoverState
}

/** Uniform corner radius for every cover card (token range 6 to 8 dp). */
private val CoverShape = RoundedCornerShape(8.dp)

/**
 * Library cover. The card keeps the size its caller gives it (all grids use [CoverRatio]); the cover adapts inside it:
 * fill using page margins, a small crop, or the whole page over a backdrop taken from the cover. No distortion, and the
 * original PDF is untouched.
 */
@Composable
fun PdfCover(book: Book, modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as LottanApplication
    var state by remember(book.id, book.bytes) { mutableStateOf<CoverState>(CoverState.Loading) }
    LaunchedEffect(book.id, book.bytes) {
        state = CoverState.Loading
        state =
            try {
                val bitmap = app.thumbnails.load(book)
                CoverState.Ready(bitmap, app.thumbnails.framing(book, bitmap))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                CoverState.Failed
            } catch (_: OutOfMemoryError) {
                CoverState.Failed
            }
    }
    val p = LocalVisual.current
    BoxWithConstraints(
        modifier
            .shadow(VisualTokens.CardElevation, CoverShape)
            .clip(CoverShape)
            .background(p.secondary)
            .border(1.dp, p.border, CoverShape),
        contentAlignment = Alignment.Center,
    ) {
        val ratio = if (constraints.hasBoundedHeight && constraints.maxHeight > 0) constraints.maxWidth.toFloat() / constraints.maxHeight else CoverRatio
        when (val cover = state) {
            is CoverState.Ready -> {
                val plan = remember(cover.analysis, ratio) { CoverFraming.plan(cover.analysis, ratio) }
                val description = "Capa de ${ReadingRules.title(book.name)}"
                if (plan.fill) {
                    val image = remember(cover.bitmap) { cover.bitmap.asImageBitmap() }
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize().semantics { contentDescription = description }) {
                        drawImage(
                            image,
                            srcOffset = IntOffset(plan.window.left, plan.window.top),
                            srcSize = IntSize(plan.window.width, plan.window.height),
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                            filterQuality = FilterQuality.High,
                        )
                    }
                } else {
                    Box(Modifier.fillMaxSize().background(Color(cover.analysis.edgeColor)), contentAlignment = Alignment.Center) {
                        Image(cover.bitmap.asImageBitmap(), description, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    }
                }
            }
            CoverState.Loading ->
                CircularProgressIndicator(Modifier.size(18.dp), color = p.muted, strokeWidth = 1.5.dp)
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
