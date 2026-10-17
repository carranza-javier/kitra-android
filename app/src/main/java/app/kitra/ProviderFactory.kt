package app.kitra

import app.kitra.core.FakeTranslationProvider
import app.kitra.core.TranslationProvider
import app.kitra.core.gemini.GeminiProvider
import app.kitra.net.UrlConnectionTransport
import app.kitra.settings.Settings

object ProviderFactory {
    /** Proveedor configurado, o null si falta la clave. */
    fun create(settings: Settings): TranslationProvider? {
        if (settings.useFakeProvider) return FakeTranslationProvider()
        val key = settings.apiKey.load() ?: return null
        return gemini(key, settings.model)
    }

    fun gemini(apiKey: String, model: String): TranslationProvider =
        GeminiProvider(apiKey = apiKey, model = model, transport = UrlConnectionTransport())
}
