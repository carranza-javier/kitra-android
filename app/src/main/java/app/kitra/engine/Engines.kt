package app.kitra.engine

import android.content.Context
import android.util.Log
import app.kitra.core.Engine
import app.kitra.core.EngineSelection
import app.kitra.core.FakeTranslationProvider
import app.kitra.core.FallbackTranslationProvider
import app.kitra.core.NanoState
import app.kitra.core.TranslationProvider
import app.kitra.core.gemini.GeminiProvider
import app.kitra.net.UrlConnectionTransport
import app.kitra.settings.Settings

/** Construye el proveedor de cada traducción según el móvil y los ajustes. El usuario no elige nada. */
object Engines {
    private const val TAG = "Kitra"

    suspend fun create(context: Context, settings: Settings): TranslationProvider {
        if (settings.useFakeProvider) return FakeTranslationProvider()
        val override = settings.engineOverride
        val key = if (settings.cloudEnabled || override == Engine.GEMINI_CLOUD) settings.apiKey.load() else null
        val nano = if (override == null || override == Engine.GEMINI_NANO) Nano.state() else NanoState.UNAVAILABLE
        val chain = EngineSelection.chain(settings.cloudEnabled, key != null, nano, override)
        val providers = chain.mapNotNull { engine ->
            when (engine) {
                Engine.GEMINI_CLOUD -> key?.let { gemini(it, settings.model) }
                Engine.GEMINI_NANO -> NanoProvider()
                Engine.ML_KIT -> MlKitProvider(context.applicationContext)
                Engine.FAKE -> FakeTranslationProvider()
            }
        }.ifEmpty { listOf(MlKitProvider(context.applicationContext)) }
        Log.i(TAG, "Engines: ${providers.map { it.engine }} (Nano: $nano)")
        return FallbackTranslationProvider(providers)
    }

    fun gemini(apiKey: String, model: String): TranslationProvider =
        GeminiProvider(apiKey = apiKey, model = model, transport = UrlConnectionTransport())
}
