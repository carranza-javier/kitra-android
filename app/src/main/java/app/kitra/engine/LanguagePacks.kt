package app.kitra.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import app.kitra.KitraApp
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Estado del paquete de un idioma para la traducción local de ML Kit (unos 30 MB cada uno). */
enum class PackState { UNKNOWN, NOT_DOWNLOADED, WAITING_FOR_WIFI, DOWNLOADING, READY, FAILED }

/**
 * Paquetes de idioma de la traducción local de ML Kit.
 * - El del idioma de destino se descarga durante la configuración inicial, preferiblemente con wifi.
 * - Los de origen, bajo demanda la primera vez que se detectan (lo hace [MlKitProvider]).
 * - Se pueden borrar desde «Avanzado».
 * El inglés viene incluido con ML Kit y no se descarga ni se borra.
 */
object LanguagePacks {
    private const val TAG = "Kitra"
    private val manager get() = RemoteModelManager.getInstance()

    private val _states = MutableStateFlow<Map<String, PackState>>(emptyMap())
    val states: StateFlow<Map<String, PackState>> = _states.asStateFlow()

    fun isBuiltIn(code: String) = code == TranslateLanguage.ENGLISH

    private fun model(code: String) = TranslateRemoteModel.Builder(code).build()

    suspend fun isDownloaded(code: String): Boolean =
        isBuiltIn(code) || manager.isModelDownloaded(model(code)).await()

    /** Descarga y espera. Lanza una excepción si falla. */
    suspend fun download(code: String, wifiOnly: Boolean) {
        if (isBuiltIn(code)) return
        val conditions = DownloadConditions.Builder().apply { if (wifiOnly) requireWifi() }.build()
        manager.download(model(code), conditions).await()
    }

    suspend fun downloaded(): List<String> =
        manager.getDownloadedModels(TranslateRemoteModel::class.java).await()
            .map { it.language }
            .filterNot(::isBuiltIn)
            .sorted()

    suspend fun delete(code: String) {
        if (isBuiltIn(code)) return
        manager.deleteDownloadedModel(model(code)).await()
        set(code, PackState.NOT_DOWNLOADED)
    }

    suspend fun refresh(code: String) {
        val ready = runCatching { isDownloaded(code) }.getOrDefault(false)
        _states.update { current ->
            val now = current[code]
            // No pisar una descarga en curso.
            if (!ready && (now == PackState.DOWNLOADING || now == PackState.WAITING_FOR_WIFI)) current
            else current + (code to if (ready) PackState.READY else PackState.NOT_DOWNLOADED)
        }
    }

    /**
     * Pide la descarga en segundo plano (sigue aunque se cierre la pantalla).
     * Con [wifiOnly], ML Kit espera a que haya wifi.
     */
    fun request(context: Context, code: String, wifiOnly: Boolean) {
        if (isBuiltIn(code)) {
            set(code, PackState.READY)
            return
        }
        val waiting = wifiOnly && !isUnmetered(context)
        set(code, if (waiting) PackState.WAITING_FOR_WIFI else PackState.DOWNLOADING)
        KitraApp.scope.launch {
            try {
                download(code, wifiOnly)
                set(code, PackState.READY)
            } catch (e: Exception) {
                Log.w(TAG, "Language pack download failed: ${e.javaClass.simpleName}")
                set(code, PackState.FAILED)
            }
        }
    }

    fun isUnmetered(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    private fun set(code: String, state: PackState) = _states.update { it + (code to state) }
}
