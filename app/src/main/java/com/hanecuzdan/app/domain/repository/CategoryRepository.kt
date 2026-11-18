package com.hanecuzdan.app.domain.repository

import com.hanecuzdan.app.domain.model.Category
import kotlinx.coroutines.flow.Flow

/** Kategori yönetimi için repository sözleşmesi. */
interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>
    suspend fun getAllSnapshot(): List<Category>
    suspend fun seedDefaultsIfEmpty()
    suspend fun addCustomCategory(category: Category)
    suspend fun deleteCategory(categoryId: String)
}
