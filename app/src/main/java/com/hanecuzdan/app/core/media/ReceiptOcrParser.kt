package com.hanecuzdan.app.core.media

import java.util.Locale

data class ReceiptScanResult(
    val merchantTitle: String? = null,
    val totalAmount: Double? = null,
    val suggestedCategoryId: String? = null,
    val rawText: String = ""
)

/** Türkçe fiş ve fatura OCR metninden işletme adı, toplam tutar ve kategori tahmini çıkarır. */
object ReceiptOcrParser {

    private val amountRegex = Regex("""[*₺]?\s*((?:\d{1,3}(?:[.,]\d{3})+|\d+)[.,]\d{2})""")
    private val totalKeywords = listOf("GENEL TOPLAM", "TOPLAM", "ÖDENECEK", "ODENECEK", "TUTAR", "TOPKDV")

    fun parse(rawText: String): ReceiptScanResult {
        if (rawText.isBlank()) return ReceiptScanResult()

        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val merchant = extractMerchantTitle(lines)
        val total = extractTotalAmount(lines)
        val categoryId = suggestCategoryId(rawText.lowercase(Locale("tr", "TR")))

        return ReceiptScanResult(
            merchantTitle = merchant,
            totalAmount = total,
            suggestedCategoryId = categoryId,
            rawText = rawText
        )
    }

    private fun extractMerchantTitle(lines: List<String>): String? {
        return lines.firstOrNull { line ->
            val upper = line.uppercase(Locale("tr", "TR"))
            line.length >= 3 &&
                line.any { it.isLetter() } &&
                !upper.startsWith("TARİH") &&
                !upper.startsWith("TARIH") &&
                !upper.startsWith("SAAT") &&
                !upper.startsWith("FİŞ") &&
                !upper.startsWith("FIS") &&
                !upper.startsWith("VD") &&
                !upper.startsWith("V.D")
        }?.take(40)
    }

    private fun extractTotalAmount(lines: List<String>): Double? {
        // Önce "TOPLAM" veya "GENEL TOPLAM" geçen satırlara bak (alttan yukarı)
        for (i in lines.indices.reversed()) {
            val upper = lines[i].uppercase(Locale("tr", "TR"))
            if (totalKeywords.any { upper.contains(it) } && !upper.contains("KDV")) {
                parseAmountInLine(lines[i])?.let { return it }
                // Tutar bir alt satırda olabilir
                if (i + 1 < lines.size) {
                    parseAmountInLine(lines[i + 1])?.let { return it }
                }
            }
        }
        // Bulunamazsa fişteki en büyük makul tutarı al
        return lines.mapNotNull { parseAmountInLine(it) }.maxOrNull()
    }

    private fun parseAmountInLine(line: String): Double? {
        val match = amountRegex.findAll(line).lastOrNull() ?: return null
        val token = match.groupValues[1]
        return normalizeNumber(token)
    }

    private fun normalizeNumber(token: String): Double? {
        val lastComma = token.lastIndexOf(',')
        val lastDot = token.lastIndexOf('.')
        val normalized = when {
            lastComma > lastDot -> token.replace(".", "").replace(',', '.')
            lastDot > lastComma -> token.replace(",", "")
            else -> token
        }
        return normalized.toDoubleOrNull()?.takeIf { it > 0.0 && it < 10_000_000.0 }
    }

    private fun suggestCategoryId(lowerText: String): String? = when {
        listOf("migros", "bim", "a101", "şok", "sok", "carrefour", "market", "manav", "fırın", "kasap")
            .any { lowerText.contains(it) } -> "cat_market"
        listOf("opet", "shell", "bp", "petrol", "akaryakıt", "benzin", "motorin", "otopark", "hgs")
            .any { lowerText.contains(it) } -> "cat_arac"
        listOf("eczane", "hastane", "klinik", "sağlık", "saglik", "medikal", "ilaç")
            .any { lowerText.contains(it) } -> "cat_saglik"
        listOf("fatura", "elektrik", "doğalgaz", "dogalgaz", "su bedeli", "turkcell", "vodafone", "telekom")
            .any { lowerText.contains(it) } -> "cat_fatura"
        listOf("waikiki", "lcw", "defacto", "zara", "koton", "giyim", "ayakkabı", "mavi")
            .any { lowerText.contains(it) } -> "cat_giyim"
        listOf("sinema", "cafe", "kahve", "starbucks", "restoran", "lokanta", "burger", "pizza")
            .any { lowerText.contains(it) } -> "cat_eglence"
        else -> null
    }
}
