package com.kettenki.kitra.ui.translate

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kettenki.kitra.R
import com.kettenki.kitra.core.Engine
import com.kettenki.kitra.core.NoticeKind
import com.kettenki.kitra.core.ProgressStep
import com.kettenki.kitra.core.TranslationEvent
import com.kettenki.kitra.core.TranslationException
import com.kettenki.kitra.engine.Engines
import com.kettenki.kitra.image.ImagePreprocessor
import com.kettenki.kitra.image.ImageUnreadableException
import com.kettenki.kitra.settings.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TranslateUiState(
    val text: String = "",
    val inProgress: Boolean = false,
    val progress: ProgressStep? = null,
    /** Motor que ha producido (o está produciendo) el texto: decide la atribución. */
    val engine: Engine? = null,
    val notice: NoticeKind? = null,
    @StringRes val error: Int? = null,
    val targetLanguageCode: String = "",
)

class TranslateViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = Settings(app)
    private val _state = MutableStateFlow(TranslateUiState())
    val state: StateFlow<TranslateUiState> = _state.asStateFlow()

    private var currentUri: Uri? = null
    private var job: Job? = null

    /** Empieza a traducir. Ignora repeticiones para la misma imagen (no hay trabajo duplicado). */
    fun start(uri: Uri, force: Boolean = false) {
        if (!force && uri == currentUri) return
        currentUri = uri
        job?.cancel()
        val target = settings.targetLanguage
        _state.value = TranslateUiState(inProgress = true, targetLanguageCode = target.code)
        job = viewModelScope.launch {
            val started = SystemClock.elapsedRealtime()
            var firstTextLogged = false
            try {
                val app = getApplication<Application>()
                val image = withContext(Dispatchers.Default) { ImagePreprocessor(app.contentResolver).encode(uri) }
                // Solo métricas (tamaño y tiempos), nunca el contenido.
                Log.i(TAG, "Image encoded: ${image.bytes.size / 1024} KB in ${SystemClock.elapsedRealtime() - started} ms")
                val provider = Engines.create(app, settings)
                provider.translate(image, target).collect { event ->
                    when (event) {
                        is TranslationEvent.EngineStarted -> _state.update { it.copy(engine = event.engine, progress = null) }
                        is TranslationEvent.Progress -> _state.update { it.copy(progress = event.step) }
                        is TranslationEvent.Notice -> _state.update { it.copy(notice = event.kind) }
                        is TranslationEvent.Text -> {
                            if (!firstTextLogged) {
                                firstTextLogged = true
                                Log.i(TAG, "First text after ${SystemClock.elapsedRealtime() - started} ms (${_state.value.engine})")
                            }
                            _state.update { it.copy(text = it.text + event.text, progress = null) }
                        }
                    }
                }
                Log.i(TAG, "Translation finished after ${SystemClock.elapsedRealtime() - started} ms (${_state.value.engine})")
                _state.update { it.copy(text = it.text.trim(), inProgress = false, progress = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Translation failed: ${e.javaClass.simpleName}")
                _state.update { it.copy(inProgress = false, progress = null, error = errorMessage(e)) }
            }
        }
    }

    fun retry() {
        currentUri?.let { start(it, force = true) }
    }

    /** Cerrar mientras traduce cancela el trabajo en curso. */
    fun cancel() {
        job?.cancel()
        job = null
    }

    private fun errorMessage(e: Exception): Int = when (e) {
        is ImageUnreadableException -> R.string.error_image
        is TranslationException.InvalidKey -> R.string.error_invalid_key
        is TranslationException.QuotaExceeded -> R.string.error_quota
        is TranslationException.Unavailable -> R.string.error_unavailable
        is TranslationException.NoConnection -> R.string.error_no_connection
        is TranslationException.ModelNotFound -> R.string.error_model
        is TranslationException.Blocked -> R.string.error_blocked
        is TranslationException.EmptyResponse -> R.string.error_empty
        is TranslationException.ImageTooLarge -> R.string.error_too_large
        is TranslationException.NoTextFound -> R.string.error_no_text
        is TranslationException.UnknownLanguage -> R.string.error_unknown_language
        is TranslationException.LanguageNotSupported -> R.string.error_language_not_supported
        is TranslationException.DownloadFailed -> R.string.error_download
        else -> R.string.error_unknown
    }

    private companion object {
        const val TAG = "Kitra"
    }
}
