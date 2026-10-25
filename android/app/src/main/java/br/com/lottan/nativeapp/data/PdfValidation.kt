package br.com.lottan.nativeapp.data

import android.app.Service
import android.content.*
import android.graphics.pdf.PdfRenderer
import android.os.*
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/** Android 8's PDFium retains a process-wide error after an invalid open. Keep validation isolated. */
internal object PdfValidation {
    private val gate = Mutex()

    suspend fun check(context: Context, file: File) {
        if (Build.VERSION.SDK_INT != 26) return
        gate.withLock {
            withTimeout(15000) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val finished = AtomicBoolean(false)
                    var reply: Result<Unit>? = null
                    var bound = false
                    lateinit var connection: ServiceConnection
                    fun release() {
                        if (bound) { bound = false; runCatching { context.unbindService(connection) } }
                        descriptor.close()
                    }
                    fun finish(result: Result<Unit>) {
                        if (!finished.compareAndSet(false, true)) return
                        release()
                        if (continuation.isActive) continuation.resumeWith(result)
                    }
                    val receiver = Messenger(Handler(Looper.getMainLooper()) { message ->
                        reply = when (message.arg1) {
                            1 -> Result.success(Unit)
                            2 -> Result.failure(SecurityException("PDF protegido por senha."))
                            else -> Result.failure(IOException("PDF inválido ou danificado."))
                        }
                        // Complete after the worker has exited, so no request can reuse its native error.
                        true
                    })
                    connection = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                            try {
                                Messenger(binder).send(Message.obtain().apply {
                                    data = Bundle().apply { putParcelable("pdf", descriptor) }
                                    replyTo = receiver
                                })
                            } catch (error: Exception) { finish(Result.failure(error)) }
                        }
                        override fun onServiceDisconnected(name: ComponentName) {
                            finish(reply ?: Result.failure(IOException("Validação do PDF interrompida.")))
                        }
                        override fun onNullBinding(name: ComponentName) {
                            finish(Result.failure(IOException("Validação do PDF indisponível.")))
                        }
                        override fun onBindingDied(name: ComponentName) {
                            finish(reply ?: Result.failure(IOException("Validação do PDF interrompida.")))
                        }
                    }
                    continuation.invokeOnCancellation {
                        if (finished.compareAndSet(false, true)) release()
                    }
                    try {
                        bound = context.bindService(Intent(context, PdfValidationService::class.java), connection, Context.BIND_AUTO_CREATE)
                        if (!bound) finish(Result.failure(IOException("Não foi possível validar o PDF.")))
                    } catch (error: Exception) { finish(Result.failure(error)) }
                }
            }
        }
    }
}

/** Receives only a read-only descriptor; never accesses the library, preferences or network. */
class PdfValidationService : Service() {
    private val messenger by lazy {
        Messenger(Handler(Looper.getMainLooper()) { request ->
            val reply = request.replyTo
            @Suppress("DEPRECATION")
            val descriptor = request.data.getParcelable<ParcelFileDescriptor>("pdf")
            Thread {
                val result = runCatching {
                    requireNotNull(descriptor).use { fd ->
                        PdfRenderer(fd).use { renderer -> require(renderer.pageCount > 0) }
                    }
                }
                val status = if (result.isSuccess) 1 else if (result.exceptionOrNull() is SecurityException) 2 else 0
                runCatching { reply.send(Message.obtain().apply { arg1 = status }) }
                Handler(Looper.getMainLooper()).postDelayed({ android.os.Process.killProcess(android.os.Process.myPid()) }, 100)
            }.start()
            true
        })
    }
    override fun onBind(intent: Intent): IBinder = messenger.binder
}
