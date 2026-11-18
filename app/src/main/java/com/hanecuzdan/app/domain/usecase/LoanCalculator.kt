package com.hanecuzdan.app.domain.usecase

import com.hanecuzdan.app.domain.model.Loan
import java.util.Calendar
import kotlin.math.pow

/** Kredi hesaplama özeti (taksit tutarı, bitiş tarihi, geçmişten bugüne ödenen & kalan). */
data class LoanSummary(
    val loan: Loan,
    val monthlyInstallment: Double,
    val endDateMillis: Long,
    val elapsedInstallments: Int,
    val remainingInstallments: Int,
    val totalPaybackAmount: Double,
    val totalInterestAmount: Double,
    val remainingDebtAmount: Double,
    val isActive: Boolean
)

/** Aylık amortisman satırı (Anapara / Faiz / Kalan Anapara). */
data class AmortizationRow(
    val installmentNumber: Int,
    val principalPart: Double,
    val interestPart: Double,
    val remainingPrincipal: Double
)

/** Eşit taksitli (anüite) kredi, bitiş tarihi ve geçmiş başlangıçlı kalan taksit hesaplayıcısı. */
object LoanCalculator {

    fun calculateMonthlyInstallment(
        principal: Double,
        monthlyRatePercent: Double,
        installments: Int
    ): Double {
        if (principal <= 0.0 || installments <= 0) return 0.0
        if (monthlyRatePercent <= 0.0) return principal / installments
        val r = monthlyRatePercent / 100.0
        val factor = (1.0 + r).pow(installments)
        return principal * (r * factor) / (factor - 1.0)
    }

    fun calculateEndDateMillis(startDateMillis: Long, totalInstallments: Int): Long {
        return Calendar.getInstance().apply {
            timeInMillis = startDateMillis
            add(Calendar.MONTH, totalInstallments.coerceAtLeast(1))
        }.timeInMillis
    }

    fun summarize(loan: Loan, nowMillis: Long = System.currentTimeMillis()): LoanSummary {
        val monthly = calculateMonthlyInstallment(
            principal = loan.principalAmount,
            monthlyRatePercent = loan.monthlyInterestRate,
            installments = loan.totalInstallments
        )
        val endMillis = calculateEndDateMillis(loan.startDateMillis, loan.totalInstallments)

        val elapsed = countElapsedInstallments(
            startDateMillis = loan.startDateMillis,
            paymentDay = loan.paymentDay,
            totalInstallments = loan.totalInstallments,
            nowMillis = nowMillis
        )
        val remaining = (loan.totalInstallments - elapsed).coerceAtLeast(0)
        val totalPayback = monthly * loan.totalInstallments
        val totalInterest = (totalPayback - loan.principalAmount).coerceAtLeast(0.0)
        val remainingDebt = monthly * remaining

        return LoanSummary(
            loan = loan,
            monthlyInstallment = monthly,
            endDateMillis = endMillis,
            elapsedInstallments = elapsed,
            remainingInstallments = remaining,
            totalPaybackAmount = totalPayback,
            totalInterestAmount = totalInterest,
            remainingDebtAmount = remainingDebt,
            isActive = remaining > 0
        )
    }

    /**
     * Kredi başlangıç tarihi bugünden eskiyse, geçen taksit sayısını hesaplar.
     * İçinde bulunulan ayın ödeme günü henüz gelmediyse o ay henüz ödenmemiş sayılır.
     */
    fun countElapsedInstallments(
        startDateMillis: Long,
        paymentDay: Int,
        totalInstallments: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        if (nowMillis < startDateMillis) return 0
        val startCal = Calendar.getInstance().apply { timeInMillis = startDateMillis }
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }

        val yearDiff = nowCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)
        val monthDiff = nowCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH)
        var monthsPassed = yearDiff * 12 + monthDiff

        if (nowCal.get(Calendar.DAY_OF_MONTH) >= paymentDay) {
            monthsPassed += 1
        }
        return monthsPassed.coerceIn(0, totalInstallments)
    }

    /** Kredi için ilk/tüm taksitlerin anapara ve faiz kırılımını üretir. */
    fun buildAmortizationSchedule(loan: Loan): List<AmortizationRow> {
        val monthly = calculateMonthlyInstallment(
            loan.principalAmount,
            loan.monthlyInterestRate,
            loan.totalInstallments
        )
        val r = loan.monthlyInterestRate / 100.0
        var balance = loan.principalAmount
        val rows = mutableListOf<AmortizationRow>()

        for (i in 1..loan.totalInstallments) {
            val interest = balance * r
            val principalPart = (monthly - interest).coerceAtMost(balance)
            balance = (balance - principalPart).coerceAtLeast(0.0)
            rows.add(
                AmortizationRow(
                    installmentNumber = i,
                    principalPart = principalPart,
                    interestPart = interest,
                    remainingPrincipal = balance
                )
            )
        }
        return rows
    }
}
