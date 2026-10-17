package com.kettenki.kitra.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class EngineSelectionTest {

    @Test
    fun cloudIsMainEngineWithKeyAndConnection() {
        assertEquals(listOf(Engine.GEMINI_CLOUD, Engine.ML_KIT), EngineSelection.chain(true, true, NanoState.UNAVAILABLE))
        assertEquals(
            listOf(Engine.GEMINI_CLOUD, Engine.GEMINI_NANO, Engine.ML_KIT),
            EngineSelection.chain(true, true, NanoState.AVAILABLE),
        )
    }

    @Test
    fun withoutKeyOrOfflineUsesLocalEngines() {
        assertEquals(listOf(Engine.ML_KIT), EngineSelection.chain(false, true, NanoState.UNAVAILABLE))
        assertEquals(listOf(Engine.ML_KIT), EngineSelection.chain(true, false, NanoState.DOWNLOADABLE))
        assertEquals(listOf(Engine.GEMINI_NANO, Engine.ML_KIT), EngineSelection.chain(false, true, NanoState.AVAILABLE))
    }

    @Test
    fun overrideForcesSingleEngine() {
        assertEquals(listOf(Engine.ML_KIT), EngineSelection.chain(true, true, NanoState.AVAILABLE, Engine.ML_KIT))
    }

    @Test
    fun initialReasonExplainsWhyCloudIsSkipped() {
        assertEquals(BasicModeReason.NO_KEY, EngineSelection.initialBasicReason(hasKey = false, online = false))
        assertEquals(BasicModeReason.OFFLINE, EngineSelection.initialBasicReason(hasKey = true, online = false))
    }

    @Test
    fun cloudErrorsMapToReasons() {
        assertEquals(BasicModeReason.OFFLINE, EngineSelection.basicReasonFor(TranslationException.NoConnection()))
        assertEquals(BasicModeReason.QUOTA_EXCEEDED, EngineSelection.basicReasonFor(TranslationException.QuotaExceeded()))
        assertEquals(BasicModeReason.KEY_INVALID, EngineSelection.basicReasonFor(TranslationException.InvalidKey()))
        assertEquals(BasicModeReason.KEY_INVALID, EngineSelection.basicReasonFor(TranslationException.KeyTypeUnsupported()))
        assertEquals(BasicModeReason.CLOUD_FAILED, EngineSelection.basicReasonFor(TranslationException.Unavailable(503)))
    }

    @Test
    fun onlyLastEngineCannotFallBack() {
        assertTrue(EngineSelection.canFallBack(Engine.GEMINI_CLOUD, TranslationException.InvalidKey()))
        assertTrue(EngineSelection.canFallBack(Engine.GEMINI_NANO, TranslationException.EngineFailed()))
        assertFalse(EngineSelection.canFallBack(Engine.ML_KIT, TranslationException.NoTextFound()))
    }

    private class Scripted(override val engine: Engine, private val block: suspend FlowCollector<TranslationEvent>.() -> Unit) :
        TranslationProvider {
        var calls = 0
        override fun translate(image: EncodedImage, target: Language): Flow<TranslationEvent> = flow {
            calls++
            block()
        }
    }

    private val image = EncodedImage(byteArrayOf(1), "image/jpeg")

    @Test
    fun cloudSuccessHasNoBasicModeNotice() = runTest {
        val cloud = Scripted(Engine.GEMINI_CLOUD) { emit(TranslationEvent.Text("Hola")) }
        val events = FallbackTranslationProvider(listOf(cloud, Scripted(Engine.ML_KIT) {})).translate(image, Language.SPANISH).toList()
        assertTrue(events.none { it is TranslationEvent.BasicMode })
        assertEquals(listOf("Hola"), events.texts())
    }

    @Test
    fun cloudQuotaFallsBackWithQuotaReason() = runTest {
        val cloud = Scripted(Engine.GEMINI_CLOUD) { throw TranslationException.QuotaExceeded() }
        val mlkit = Scripted(Engine.ML_KIT) { emit(TranslationEvent.Text("Hola")) }
        val events = FallbackTranslationProvider(listOf(cloud, mlkit)).translate(image, Language.SPANISH).toList()
        assertEquals(
            listOf(
                TranslationEvent.EngineStarted(Engine.GEMINI_CLOUD),
                TranslationEvent.EngineStarted(Engine.ML_KIT),
                TranslationEvent.BasicMode(BasicModeReason.QUOTA_EXCEEDED),
                TranslationEvent.Text("Hola"),
            ),
            events,
        )
    }

    @Test
    fun invalidKeyFallsBackAndSaysSo() = runTest {
        val cloud = Scripted(Engine.GEMINI_CLOUD) { throw TranslationException.InvalidKey() }
        val mlkit = Scripted(Engine.ML_KIT) { emit(TranslationEvent.Text("Hola")) }
        val events = FallbackTranslationProvider(listOf(cloud, mlkit)).translate(image, Language.SPANISH).toList()
        assertTrue(events.contains(TranslationEvent.BasicMode(BasicModeReason.KEY_INVALID)))
    }

    @Test
    fun localOnlyChainUsesInitialReason() = runTest {
        val mlkit = Scripted(Engine.ML_KIT) { emit(TranslationEvent.Text("Hola")) }
        val events = FallbackTranslationProvider(listOf(mlkit), BasicModeReason.OFFLINE).translate(image, Language.SPANISH).toList()
        assertTrue(events.contains(TranslationEvent.BasicMode(BasicModeReason.OFFLINE)))
    }

    @Test
    fun nanoFailureFallsBackToMlKitKeepingReason() = runTest {
        val nano = Scripted(Engine.GEMINI_NANO) { throw TranslationException.EngineFailed() }
        val mlkit = Scripted(Engine.ML_KIT) { emit(TranslationEvent.Text("Hola")) }
        val events = FallbackTranslationProvider(listOf(nano, mlkit), BasicModeReason.NO_KEY).translate(image, Language.SPANISH).toList()
        assertEquals(2, events.count { it == TranslationEvent.BasicMode(BasicModeReason.NO_KEY) })
        assertEquals(listOf("Hola"), events.texts())
    }

    @Test
    fun failureAfterTextIsNotRetried() = runTest {
        val cloud = Scripted(Engine.GEMINI_CLOUD) {
            emit(TranslationEvent.Text("Medio"))
            throw TranslationException.NoConnection()
        }
        val mlkit = Scripted(Engine.ML_KIT) { emit(TranslationEvent.Text("Hola")) }
        try {
            FallbackTranslationProvider(listOf(cloud, mlkit)).translate(image, Language.SPANISH).toList()
            fail("Expected NoConnection")
        } catch (_: TranslationException.NoConnection) {
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
