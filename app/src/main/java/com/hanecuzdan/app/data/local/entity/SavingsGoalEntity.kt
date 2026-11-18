package com.hanecuzdan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hanecuzdan.app.domain.model.DataScope
import com.hanecuzdan.app.domain.model.SavingsGoal

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val scope: String,
    val createdAt: Long
) {
    fun toDomain() = SavingsGoal(
        id = id,
        title = title,
        targetAmount = targetAmount,
        savedAmount = savedAmount,
        scope = DataScope.valueOf(scope),
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(model: SavingsGoal) = SavingsGoalEntity(
            id = model.id,
            title = model.title,
            targetAmount = model.targetAmount,
            savedAmount = model.savedAmount,
            scope = model.scope.name,
            createdAt = model.createdAt
        )
    }
}
