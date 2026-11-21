package com.hanecuzdan.app.domain.model

import kotlinx.serialization.Serializable

/** Tekrarlayan ödeme döngüsü. */
@Serializable
enum class BillingCycle {
    MONTHLY,
    YEARLY
}

/** Abonelik ve sabit fatura modeli (Netflix, Kira, İnternet, Kredi vb.). */
@Serializable
data class Subscription(
    val id: String,
    val name: String,
    val amount: Double,
    val cycle: BillingCycle,
    val billingDay: Int, // 1..31
    val categoryName: String,
    val scope: DataScope = DataScope.FAMILY,
    val isActive: Boolean = true,
    val createdByUserId: String = ""
) {
    /** Yıllık abonelikleri aylık eşdeğer maliyete çevirir. */
    fun monthlyEquivalent(): Double = if (cycle == BillingCycle.YEARLY) amount / 12.0 else amount
}
