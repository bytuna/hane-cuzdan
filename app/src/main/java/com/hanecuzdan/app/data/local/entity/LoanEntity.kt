package com.hanecuzdan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hanecuzdan.app.domain.model.DataScope
import com.hanecuzdan.app.domain.model.Loan

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey val id: String,
    val name: String,
    val principalAmount: Double,
    val monthlyInterestRate: Double,
    val totalInstallments: Int,
    val startDateMillis: Long,
    val paymentDay: Int,
    val scope: String
) {
    fun toDomain() = Loan(
        id = id,
        name = name,
        principalAmount = principalAmount,
        monthlyInterestRate = monthlyInterestRate,
        totalInstallments = totalInstallments,
        startDateMillis = startDateMillis,
        paymentDay = paymentDay,
        scope = DataScope.valueOf(scope)
    )

    companion object {
        fun fromDomain(model: Loan) = LoanEntity(
            id = model.id,
            name = model.name,
            principalAmount = model.principalAmount,
            monthlyInterestRate = model.monthlyInterestRate,
            totalInstallments = model.totalInstallments,
            startDateMillis = model.startDateMillis,
            paymentDay = model.paymentDay,
            scope = model.scope.name
        )
    }
}
