package com.kettenki.kitra.engine

import android.content.Context
import android.util.Log
import com.kettenki.kitra.core.Engine
import com.kettenki.kitra.core.EngineSelection
import com.kettenki.kitra.core.FakeTranslationProvider
import com.kettenki.kitra.core.FallbackTranslationProvider
import com.kettenki.kitra.core.NanoState
import com.kettenki.kitra.core.TranslationProvider
import com.kettenki.kitra.core.gemini.GeminiProvider
import com.kettenki.kitra.net.UrlConnectionTransport
import com.kettenki.kitra.settings.Settings

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
