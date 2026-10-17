package app.kitra.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pantalla de 1080×2400, como las capturas de prueba de testdata/. */
class ScreenTextTest {
    private val w = 1080
    private val h = 2400

    private fun block(top: Int, left: Int = 40, lineHeight: Int = 46, vararg lines: String): OcrBlock {
        val ocrLines = lines.mapIndexed { i, text ->
            val t = top + i * (lineHeight + 14)
            OcrLine(text, Box(left, t, left + 900, t + lineHeight))
        }
        return OcrBlock(ocrLines, Box(left, top, left + 900, ocrLines.last().box.bottom))
    }

    @Test
    fun keepsArticleAndDropsInterface() {
        val blocks = listOf(
            block(25, lineHeight = 34, lines = arrayOf("10:42")),                      // reloj (barra de estado)
            OcrBlock(listOf(OcrLine("87 %", Box(900, 25, 1000, 59))), Box(900, 25, 1000, 59)), // batería
            block(130, lineHeight = 46, lines = arrayOf("Zürcher Tagblatt")),          // barra superior de la web
            block(300, lineHeight = 64, lines = arrayOf("Stadtrat will Velowege bis 2030", "verdoppeln")),
            block(500, lines = arrayOf("Die Stadt Zürich plant, ihr Velo-", "netz massiv auszubauen.")),
            block(680, lines = arrayOf("Der Entscheid fällt im Frühling.")),
            block(2125, left = 90, lines = arrayOf("OK")),                             // botón
            block(2320, lineHeight = 30, lines = arrayOf("Home")),                     // barra de navegación
        )
        assertEquals(
            listOf(
                "Stadtrat will Velowege bis 2030 verdoppeln",
                "Die Stadt Zürich plant, ihr Velonetz massiv auszubauen.",
                "Der Entscheid fällt im Frühling.",
            ),
            ScreenText.paragraphs(blocks, w, h),
        )
    }

    @Test
    fun mergesBlocksThatContinueTheSameParagraph() {
        val blocks = listOf(
            block(500, lines = arrayOf("Primera línea del párrafo")),
            block(560, lines = arrayOf("que sigue aquí.")),             // pegado y con la misma letra
            block(800, lines = arrayOf("Otro párrafo distinto.")),      // separado
        )
        assertEquals(
            listOf("Primera línea del párrafo que sigue aquí.", "Otro párrafo distinto."),
            ScreenText.paragraphs(blocks, w, h),
        )
    }

    @Test
    fun headlineAndBodyStaySeparate() {
        val blocks = listOf(
            block(300, lineHeight = 64, lines = arrayOf("Titular grande")),
            block(380, lineHeight = 40, lines = arrayOf("Cuerpo del artículo con texto normal.")),
        )
        assertEquals(2, ScreenText.paragraphs(blocks, w, h).size)
    }

    @Test
    fun keepsLongTextNearEdges() {
        // Un párrafo largo cerca del final de la pantalla no es un botón.
        val b = block(1900, lines = arrayOf("Este es un párrafo largo cerca del final de la pantalla."))
        assertTrue(ScreenText.isContent(b, w, h))
        // Pero una etiqueta corta en esa zona, sí.
        assertFalse(ScreenText.isContent(block(1900, lines = arrayOf("Aceptar todo")), w, h))
    }

    @Test
    fun dropsIsolatedBannerAtTheBottom() {
        val blocks = listOf(
            block(500, lines = arrayOf("Die Stadt Zürich plant, ihr Velonetz massiv auszubauen.")),
            block(2000, lines = arrayOf("Wir verwenden Cookies. Alle akzeptieren")), // banner con hueco enorme encima
        )
        assertEquals(listOf("Die Stadt Zürich plant, ihr Velonetz massiv auszubauen."), ScreenText.paragraphs(blocks, w, h))
    }

    @Test
    fun keepsBottomTextThatContinuesTheArticle() {
        // Pantalla llena de texto: el último párrafo, corto y abajo, sigue al anterior con el espaciado normal.
        val blocks = listOf(
            block(1700, lines = arrayOf("Un párrafo largo del artículo que llega casi hasta abajo", "y ocupa varias líneas de la pantalla.")),
            block(1900, lines = arrayOf("La decisión llegará en primavera.")),
        )
        assertEquals(2, ScreenText.paragraphs(blocks, w, h).size)
    }

    @Test
    fun shortTextInMiddleIsKept() {
        assertTrue(ScreenText.isContent(block(1200, lines = arrayOf("Hola mundo")), w, h))
    }

    @Test
    fun joinsCjkWithoutSpacesAndUndoesHyphens() {
        assertEquals("苏黎世计划扩建自行车道", ScreenText.joinLines(listOf("苏黎世计划", "扩建自行车道")))
        assertEquals("Velonetz", ScreenText.joinLines(listOf("Velo-", "netz")))
        assertEquals("Nord- Süd", ScreenText.joinLines(listOf("Nord-", "Süd")))
    }

    @Test
    fun detectsCjk() {
        assertTrue(ScreenText.cjkRatio("苏黎世 bike") > 0.3)
        assertEquals(0.0, ScreenText.cjkRatio("Zürich"), 0.0)
    }
}
