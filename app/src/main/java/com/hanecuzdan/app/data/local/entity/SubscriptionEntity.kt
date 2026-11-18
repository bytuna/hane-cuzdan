package com.hanecuzdan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hanecuzdan.app.domain.model.BillingCycle
import com.hanecuzdan.app.domain.model.DataScope
import com.hanecuzdan.app.domain.model.Subscription

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val amount: Double,
    val cycle: String,
    val billingDay: Int,
    val categoryName: String,
    val scope: String,
    val isActive: Boolean
) {
    fun toDomain() = Subscription(
        id = id,
        name = name,
        amount = amount,
        cycle = BillingCycle.valueOf(cycle),
        billingDay = billingDay,
        categoryName = categoryName,
        scope = DataScope.valueOf(scope),
        isActive = isActive
    )

    companion object {
        fun fromDomain(model: Subscription) = SubscriptionEntity(
            id = model.id,
            name = model.name,
            amount = model.amount,
            cycle = model.cycle.name,
            billingDay = model.billingDay,
            categoryName = model.categoryName,
            scope = model.scope.name,
            isActive = model.isActive
        )
    }
}
