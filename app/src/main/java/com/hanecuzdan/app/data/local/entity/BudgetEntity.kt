package com.hanecuzdan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hanecuzdan.app.domain.model.Budget
import com.hanecuzdan.app.domain.model.BudgetPeriod
import com.hanecuzdan.app.domain.model.DataScope

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val categoryId: String?,
    val categoryName: String,
    val limitAmount: Double,
    val period: String,
    val scope: String
) {
    fun toDomain() = Budget(
        id = id,
        categoryId = categoryId,
        categoryName = categoryName,
        limitAmount = limitAmount,
        period = BudgetPeriod.valueOf(period),
        scope = DataScope.valueOf(scope)
    )

    companion object {
        fun fromDomain(model: Budget) = BudgetEntity(
            id = model.id,
            categoryId = model.categoryId,
            categoryName = model.categoryName,
            limitAmount = model.limitAmount,
            period = model.period.name,
            scope = model.scope.name
        )
    }
}
