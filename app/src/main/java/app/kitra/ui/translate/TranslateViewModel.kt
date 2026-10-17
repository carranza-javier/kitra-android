package app.kitra.ui.translate

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.kitra.ProviderFactory
import app.kitra.R
import app.kitra.core.TranslationException
import app.kitra.image.ImagePreprocessor
import app.kitra.image.ImageUnreadableException
import app.kitra.settings.Settings
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
    @StringRes val error: Int? = null,
    val needsSetup: Boolean = false,
)

class TranslateViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = Settings(app)
    private val _state = MutableStateFlow(TranslateUiState())
    val state: StateFlow<TranslateUiState> = _state.asStateFlow()

    private var currentUri: Uri? = null
    private var job: Job? = null

    /** Empieza a traducir. Ignora repeticiones para la misma imagen (no hay llamadas duplicadas). */
    fun start(uri: Uri, force: Boolean = false) {
        if (!force && uri == currentUri) return
        currentUri = uri
        job?.cancel()
        val provider = ProviderFactory.create(settings)
        if (provider == null) {
            _state.value = TranslateUiState(needsSetup = true)
            return
        }
        _state.value = TranslateUiState(inProgress = true)
        job = viewModelScope.launch {
            val started = SystemClock.elapsedRealtime()
            var firstChunkLogged = false
            try {
                val image = withContext(Dispatchers.Default) { ImagePreprocessor(getApplication<Application>().contentResolver).encode(uri) }
                // Solo métricas (tamaño y tiempos), nunca el contenido.
                Log.i(TAG, "Image encoded: ${image.bytes.size / 1024} KB in ${SystemClock.elapsedRealtime() - started} ms")
                provider.translate(image, settings.targetLanguage).collect { chunk ->
                    if (!firstChunkLogged) {
                        firstChunkLogged = true
                        Log.i(TAG, "First text after ${SystemClock.elapsedRealtime() - started} ms")
                    }
                    _state.update { it.copy(text = it.text + chunk.text) }
                }
                Log.i(TAG, "Translation finished after ${SystemClock.elapsedRealtime() - started} ms")
                _state.update { it.copy(text = it.text.trim(), inProgress = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Translation failed: ${e.javaClass.simpleName}")
                _state.update { it.copy(inProgress = false, error = errorMessage(e)) }
            }
        }
    }

    fun retry() {
        currentUri?.let { start(it, force = true) }
    }

    /** Cerrar mientras traduce cancela la petición en curso. */
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
        else -> R.string.error_unknown
    }

    private companion object {
        const val TAG = "Kitra"
    }
}
