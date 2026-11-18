package com.hanecuzdan.app.core.export

import com.hanecuzdan.app.domain.model.DataScope
import com.hanecuzdan.app.domain.model.Transaction
import com.hanecuzdan.app.domain.model.TransactionType
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportManagerTest {

    @Test
    fun `csv icerigi baslik ve islem satirlarini icermeli`() {
        val txs = listOf(
            Transaction(
                id = "tx1",
                title = "Migros Alışveriş",
                amount = 450.75,
                type = TransactionType.EXPENSE,
                scope = DataScope.FAMILY,
                categoryId = "cat_market",
                categoryName = "Market",
                timestamp = 1_700_000_000_000L,
                note = "Haftalık mutfak"
            )
        )

        val csv = ExportManager.buildCsvContent(txs)

        assertTrue(csv.contains("Başlık,Tutar,Tür,Kapsam,Kategori,Tarih,Not"))
        assertTrue(csv.contains("Migros Alışveriş"))
        assertTrue(csv.contains("450.75"))
        assertTrue(csv.contains("Aile"))
        assertTrue(csv.contains("Market"))
    }
}
