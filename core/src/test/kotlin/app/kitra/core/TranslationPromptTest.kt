package app.kitra.core

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationPromptTest {

    @Test
    fun promptMentionsTargetLanguageAndKeyRules() {
        val prompt = TranslationPrompt.systemInstruction(Language.SPANISH)
        assertTrue(prompt.contains("into Spanish"))
        assertTrue(prompt.contains("status bar"))
        assertTrue(prompt.contains("never summarise"))
        assertTrue(prompt.contains("already in Spanish"))
        // El modelo copiaba literalmente "[illegible]" en la salida en español: el prompt no debe dar ese ejemplo.
        assertTrue(!prompt.contains("[illegible]"))
    }

    @Test
    fun fakeProviderStreamsSeveralChunks() = runTest {
        val chunks = FakeTranslationProvider(chunkDelayMs = 0)
            .translate(EncodedImage(ByteArray(2048), "image/jpeg"), Language.SPANISH).toList()
        assertTrue(chunks.size > 5)
        assertTrue(chunks.joinToString("") { it.text }.contains("Proveedor falso"))
    }

    @Test
    fun languageFallsBackToSpanish() {
        assertTrue(Language.forSystemLocale("xx") == Language.SPANISH)
        assertTrue(Language.forSystemLocale("de") == Language.GERMAN)
    }
}
