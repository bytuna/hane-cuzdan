package com.hanecuzdan.app.domain.model

import kotlinx.serialization.Serializable

/** Tasarruf hedefi / kumbara modeli (Acil Durum, Tatil, Araç vb.). */
@Serializable
data class SavingsGoal(
    val id: String,
    val title: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val scope: DataScope = DataScope.FAMILY,
    val createdAt: Long = System.currentTimeMillis()
) {
    val progressRatio: Float
        get() = if (targetAmount > 0.0) (savedAmount / targetAmount).toFloat().coerceIn(0f, 1f) else 0f
}
