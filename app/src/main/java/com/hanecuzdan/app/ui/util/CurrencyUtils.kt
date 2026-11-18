package com.hanecuzdan.app.ui.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.NumberFormat
import java.util.Locale

private val trLocale = Locale("tr", "TR")

/** Double tutarı Türkçe binlik noktalı ve ondalık virgüllü formatlar (ör. 50.000,00 ₺). */
fun formatTurkishCurrency(amount: Double, showDecimals: Boolean = true): String {
    val formatter = NumberFormat.getNumberInstance(trLocale).apply {
        minimumFractionDigits = if (showDecimals) 2 else 0
        maximumFractionDigits = 2
    }
    return "${formatter.format(amount)} ₺"
}

/**
 * Kullanıcı girişini temizler: yalnızca rakam ve tek bir ondalık virgülü kabul eder.
 * Klavyeden '.' basılsa bile ondalık virgülüne (',') dönüştürür, en fazla 2 ondalık basamağa izin verir.
 */
fun sanitizeCurrencyInput(input: String): String {
    val sb = StringBuilder()
    var hasComma = false
    var decimalCount = 0

    for (ch in input) {
        when {
            ch.isDigit() -> {
                if (hasComma) {
                    if (decimalCount < 2) {
                        sb.append(ch)
                        decimalCount++
                    }
                } else {
                    sb.append(ch)
                }
            }
            (ch == ',' || ch == '.') && !hasComma -> {
                if (sb.isEmpty()) sb.append('0')
                sb.append(',')
                hasComma = true
            }
        }
    }
    return sb.toString()
}

/** Temizlenmiş Türkçe para metnini Double'a çevirir (ör. "50000,50" -> 50000.5). */
fun parseTurkishCurrency(sanitized: String): Double? {
    if (sanitized.isBlank()) return null
    return sanitized.replace(',', '.').toDoubleOrNull()
}

/** Ham rakam girişini ("50000,25") ekranda otomatik binlik noktalı ("50.000,25") gösterir. */
class TurkishCurrencyVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val commaIndex = raw.indexOf(',')
        val intPart = if (commaIndex >= 0) raw.substring(0, commaIndex) else raw
        val decPart = if (commaIndex >= 0) raw.substring(commaIndex) else ""

        val formattedInt = buildString {
            for (i in intPart.indices) {
                if (i > 0 && (intPart.length - i) % 3 == 0) {
                    append('.')
                }
                append(intPart[i])
            }
        }

        val transformed = formattedInt + decPart
        val dotCount = formattedInt.length - intPart.length

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val clampedIntOffset = offset.coerceAtMost(intPart.length)
                val dotsBefore = countDotsBefore(intPart.length, clampedIntOffset)
                return if (offset <= intPart.length) {
                    offset + dotsBefore
                } else {
                    (offset + dotCount).coerceAtMost(transformed.length)
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset >= formattedInt.length) {
                    return (offset - dotCount).coerceIn(0, raw.length)
                }
                var orig = 0
                var trans = 0
                while (trans < offset && orig < intPart.length) {
                    if (formattedInt[trans] == '.') {
                        trans++
                    } else {
                        trans++
                        orig++
                    }
                }
                return orig.coerceIn(0, raw.length)
            }
        }

        return TransformedText(AnnotatedString(transformed), offsetMapping)
    }

    private fun countDotsBefore(totalIntLen: Int, origOffset: Int): Int {
        if (totalIntLen <= 3 || origOffset <= 0) return 0
        var dots = 0
        for (i in 1 until origOffset) {
            if ((totalIntLen - i) % 3 == 0) dots++
        }
        if (origOffset < totalIntLen && (totalIntLen - origOffset) % 3 == 0) {
            dots++
        }
        return dots
    }
}
