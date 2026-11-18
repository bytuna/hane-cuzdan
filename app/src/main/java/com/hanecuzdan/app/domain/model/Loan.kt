package com.hanecuzdan.app.domain.model

/** Kredi / Taksitli ödeme domain modeli. */
data class Loan(
    val id: String,
    val name: String,
    val principalAmount: Double,      // Anapara (₺)
    val monthlyInterestRate: Double,  // Aylık faiz oranı (%, örn: 3.49)
    val totalInstallments: Int,       // Toplam taksit sayısı (ay)
    val startDateMillis: Long,        // Kredi başlangıç tarihi
    val paymentDay: Int,              // Her ayın ödeme günü (1..31)
    val scope: DataScope = DataScope.FAMILY
)
