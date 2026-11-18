package com.hanecuzdan.app.domain.usecase

import com.hanecuzdan.app.domain.model.Loan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class LoanCalculatorTest {

    @Test
    fun `faizli ve faizsiz taksit tutari dogru hesaplanmali`() {
        val zeroInterest = LoanCalculator.calculateMonthlyInstallment(12000.0, 0.0, 12)
        assertEquals(1000.0, zeroInterest, 0.01)

        val withInterest = LoanCalculator.calculateMonthlyInstallment(100000.0, 3.0, 12)
        assertTrue(withInterest in 10000.0..10100.0)
    }

    @Test
    fun `gecmis baslangicli kredide odenmis ve kalan taksitler dogru hesaplanmali`() {
        val startCal = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 10, 10, 0, 0)
        }
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 18, 12, 0, 0)
        }

        val loan = Loan(
            id = "l1",
            name = "İhtiyaç Kredisi",
            principalAmount = 120000.0,
            monthlyInterestRate = 2.5,
            totalInstallments = 12,
            startDateMillis = startCal.timeInMillis,
            paymentDay = 15
        )

        val summary = LoanCalculator.summarize(loan, nowCal.timeInMillis)

        // Mayıs, Haziran, Temmuz, Ağustos, Eylül, Ekim, Kasım (18 >= 15) = 7 taksit ödenmiş, 5 kalmış
        assertEquals(7, summary.elapsedInstallments)
        assertEquals(5, summary.remainingInstallments)
        assertTrue(summary.isActive)
        assertTrue(summary.remainingDebtAmount > 0.0)
    }
}
