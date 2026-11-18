package com.hanecuzdan.app.ui.util

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyUtilsTest {

    @Test
    fun `binlik nokta ve ondalik virgul gorunumu dogru olusturulmali`() {
        val vt = TurkishCurrencyVisualTransformation()
        assertEquals("50.000", vt.filter(AnnotatedString("50000")).text.text)
        assertEquals("1.250.000,75", vt.filter(AnnotatedString("1250000,75")).text.text)
    }

    @Test
    fun `giris temizleme ve double donusumu dogru calismali`() {
        val clean = sanitizeCurrencyInput("50000.75abc")
        assertEquals("50000,75", clean)
        assertEquals(50000.75, parseTurkishCurrency(clean)!!, 0.001)
    }
}
