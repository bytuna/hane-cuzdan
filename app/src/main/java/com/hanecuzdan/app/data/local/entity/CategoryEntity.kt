package com.hanecuzdan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hanecuzdan.app.domain.model.Category
import com.hanecuzdan.app.domain.model.DataScope
import com.hanecuzdan.app.domain.model.TransactionType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconKey: String,
    val colorHex: Long,
    val scope: String,
    val isDefault: Boolean,
    val type: String = TransactionType.EXPENSE.name
) {
    fun toDomain() = Category(
        id = id,
        name = name,
        iconKey = iconKey,
        colorHex = colorHex,
        scope = DataScope.valueOf(scope),
        isDefault = isDefault,
        type = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE)
    )

    companion object {
        fun fromDomain(model: Category) = CategoryEntity(
            id = model.id,
            name = model.name,
            iconKey = model.iconKey,
            colorHex = model.colorHex,
            scope = model.scope.name,
            isDefault = model.isDefault,
            type = model.type.name
        )
    }
}
