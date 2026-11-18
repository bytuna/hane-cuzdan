package com.hanecuzdan.app.core.backup

import com.hanecuzdan.app.domain.model.Budget
import com.hanecuzdan.app.domain.model.Category
import com.hanecuzdan.app.domain.model.Transaction
import kotlinx.serialization.Serializable

/** GitHub yedekleme için JSON anlık görüntü modeli. */
@Serializable
data class BackupSnapshot(
    val exportedAt: Long,
    val appVersion: String = "1.0.0",
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val budgets: List<Budget>
)
