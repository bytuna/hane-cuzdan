package com.hanecuzdan.app.domain.model

import kotlinx.serialization.Serializable

/** Bütçe limiti periyodu. */
@Serializable
enum class BudgetPeriod {
    WEEKLY,
    MONTHLY
}

/** Bütçe eşik uyarı seviyesi. */
enum class BudgetThresholdLevel {
    NONE,
    WARNING_80,
    EXCEEDED_100
}

/** Kategori veya toplam bütçe limiti modeli. */
@Serializable
data class Budget(
    val id: String,
    val categoryId: String?, // null ise toplam bütçe limiti
    val categoryName: String,
    val limitAmount: Double,
    val period: BudgetPeriod,
    val scope: DataScope = DataScope.FAMILY
)

/** Bütçe değerlendirme sonucu. */
data class BudgetStatus(
    val budget: Budget,
    val spentAmount: Double,
    val usageRatio: Double,
    val thresholdLevel: BudgetThresholdLevel
)
