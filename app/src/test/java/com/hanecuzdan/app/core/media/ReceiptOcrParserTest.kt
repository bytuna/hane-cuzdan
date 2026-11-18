package com.hanecuzdan.app.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptOcrParserTest {

    @Test
    fun `migros fisinden baslik toplam tutar ve market kategorisi cikarilmali`() {
        val rawText = """
            MİGROS TİCARET A.Ş.
            TARİH: 18/11/2026 SAAT: 14:22
            SÜT 1L             45,00
            EKMEK              15,50
            TOPLAM            *1.245,75
            KDV               124,57
        """.trimIndent()

        val result = ReceiptOcrParser.parse(rawText)

        assertEquals("MİGROS TİCARET A.Ş.", result.merchantTitle)
        assertEquals(1245.75, result.totalAmount!!, 0.01)
        assertEquals("cat_market", result.suggestedCategoryId)
    }

    @Test
    fun `akaryakit fisinden arac kategorisi ve tutar cikarilmali`() {
        val rawText = """
            OPET PETROLCÜLÜK A.Ş.
            MOTORİN 30 LT
            GENEL TOPLAM: 1350,00 TL
        """.trimIndent()

        val result = ReceiptOcrParser.parse(rawText)

        assertEquals("OPET PETROLCÜLÜK A.Ş.", result.merchantTitle)
        assertEquals(1350.0, result.totalAmount!!, 0.01)
        assertEquals("cat_arac", result.suggestedCategoryId)
    }
}
