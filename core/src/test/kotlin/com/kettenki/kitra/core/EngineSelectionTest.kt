package com.kettenki.kitra.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class EngineSelectionTest {

    @Test
    fun withoutKeyOrNanoUsesMlKit() {
        assertEquals(listOf(Engine.ML_KIT), EngineSelection.chain(false, false, NanoState.UNAVAILABLE))
        assertEquals(listOf(Engine.ML_KIT), EngineSelection.chain(false, false, NanoState.DOWNLOADABLE))
        assertEquals(listOf(Engine.ML_KIT), EngineSelection.chain(true, false, NanoState.UNAVAILABLE))
    }

    @Test
    fun prefersNanoWhenReady() {
        assertEquals(listOf(Engine.GEMINI_NANO, Engine.ML_KIT), EngineSelection.chain(false, false, NanoState.AVAILABLE))
    }

    @Test
    fun cloudOnlyWhenEnabledAndKeyed() {
        assertEquals(
            listOf(Engine.GEMINI_CLOUD, Engine.GEMINI_NANO, Engine.ML_KIT),
            EngineSelection.chain(true, true, NanoState.AVAILABLE),
        )
        assertEquals(listOf(Engine.GEMINI_NANO, Engine.ML_KIT), EngineSelection.chain(false, true, NanoState.AVAILABLE))
    }

    @Test
    fun overrideForcesSingleEngine() {
        assertEquals(listOf(Engine.ML_KIT), EngineSelection.chain(true, true, NanoState.AVAILABLE, Engine.ML_KIT))
    }

    @Test
    fun cloudFallsBackOnlyForNetworkOrQuota() {
        assertTrue(EngineSelection.canFallBack(Engine.GEMINI_CLOUD, TranslationException.NoConnection()))
        assertTrue(EngineSelection.canFallBack(Engine.GEMINI_CLOUD, TranslationException.QuotaExceeded()))
        assertTrue(!EngineSelection.canFallBack(Engine.GEMINI_CLOUD, TranslationException.InvalidKey()))
        assertTrue(EngineSelection.canFallBack(Engine.GEMINI_NANO, TranslationException.EngineFailed()))
        assertTrue(!EngineSelection.canFallBack(Engine.ML_KIT, TranslationException.NoTextFound()))
    }

    private class Scripted(override val engine: Engine, private val block: suspend kotlinx.coroutines.flow.FlowCollector<TranslationEvent>.() -> Unit) :
        TranslationProvider {
        var calls = 0
        override fun translate(image: EncodedImage, target: Language): Flow<TranslationEvent> = flow {
            calls++
            block()
        }
    }

    private val image = EncodedImage(byteArrayOf(1), "image/jpeg")

    @Test
    fun nanoFailureBeforeTextFallsBackToMlKit() = runTest {
        val nano = Scripted(Engine.GEMINI_NANO) { throw TranslationException.EngineFailed() }
        val mlkit = Scripted(Engine.ML_KIT) { emit(TranslationEvent.Text("Hola")) }
        val events = FallbackTranslationProvider(listOf(nano, mlkit)).translate(image, Language.SPANISH).toList()
        assertEquals(
            listOf(
                TranslationEvent.EngineStarted(Engine.GEMINI_NANO),
                TranslationEvent.EngineStarted(Engine.ML_KIT),
                TranslationEvent.Text("Hola"),
            ),
            events,
        )
    }

    @Test
    fun cloudNetworkFailureFallsBackWithNotice() = runTest {
        val cloud = Scripted(Engine.GEMINI_CLOUD) { throw TranslationException.NoConnection() }
        val mlkit = Scripted(Engine.ML_KIT) { emit(TranslationEvent.Text("Hola")) }
        val events = FallbackTranslationProvider(listOf(cloud, mlkit)).translate(image, Language.SPANISH).toList()
        assertTrue(events.contains(TranslationEvent.Notice(NoticeKind.CLOUD_FELL_BACK_TO_DEVICE)))
        assertEquals(listOf("Hola"), events.texts())
    }

    @Test
    fun cloudInvalidKeyIsShownNotHidden() = runTest {
        val cloud = Scripted(Engine.GEMINI_CLOUD) { throw TranslationException.InvalidKey() }
        val mlkit = Scripted(Engine.ML_KIT) { emit(TranslationEvent.Text("Hola")) }
        try {
            FallbackTranslationProvider(listOf(cloud, mlkit)).translate(image, Language.SPANISH).toList()
            fail("Expected InvalidKey")
        } catch (_: TranslationException.InvalidKey) {
        }
        assertEquals(0, mlkit.calls)
    }

    @Test
    fun failureAfterTextIsNotRetried() = runTest {
        val nano = Scripted(Engine.GEMINI_NANO) {
            emit(TranslationEvent.Text("Medio"))
            throw TranslationException.EngineFailed()
        }
        val mlkit = Scripted(Engine.ML_KIT) { emit(TranslationEvent.Text("Hola")) }
        try {
            FallbackTranslationProvider(listOf(nano, mlkit)).translate(image, Language.SPANISH).toList()
            fail("Expected EngineFailed")
        } catch (_: TranslationException.EngineFailed) {
        }
        assertEquals(0, mlkit.calls)
    }

    @Test
    fun lastEngineErrorPropagates() = runTest {
        val mlkit = Scripted(Engine.ML_KIT) { throw TranslationException.NoTextFound() }
        try {
            FallbackTranslationProvider(listOf(mlkit)).translate(image, Language.SPANISH).toList()
            fail("Expected NoTextFound")
        } catch (_: TranslationException.NoTextFound) {
        }
    }
}
