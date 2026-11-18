package com.hanecuzdan.app.data.repository

import com.hanecuzdan.app.data.local.dao.CategoryDao
import com.hanecuzdan.app.data.local.entity.CategoryEntity
import com.hanecuzdan.app.domain.model.Category
import com.hanecuzdan.app.domain.model.DefaultCategories
import com.hanecuzdan.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllSnapshot(): List<Category> =
        categoryDao.getAllSnapshot().map { it.toDomain() }

    override suspend fun seedDefaultsIfEmpty() {
        if (categoryDao.count() == 0) {
            categoryDao.upsertAll(DefaultCategories.list.map { CategoryEntity.fromDomain(it) })
        }
    }

    override suspend fun addCustomCategory(category: Category) {
        categoryDao.upsert(CategoryEntity.fromDomain(category.copy(isDefault = false)))
    }

    override suspend fun deleteCategory(categoryId: String) {
        categoryDao.deleteById(categoryId)
    }
}
