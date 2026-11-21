package com.hanecuzdan.app.data.repository

import com.hanecuzdan.app.data.local.dao.CategoryDao
import com.hanecuzdan.app.data.local.dao.SyncQueueDao
import com.hanecuzdan.app.data.local.entity.CategoryEntity
import com.hanecuzdan.app.data.local.entity.SyncQueueEntity
import com.hanecuzdan.app.domain.model.Category
import com.hanecuzdan.app.domain.model.DefaultCategories
import com.hanecuzdan.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val syncQueueDao: SyncQueueDao
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
        val custom = category.copy(isDefault = false)
        categoryDao.upsert(CategoryEntity.fromDomain(custom))
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entityId = custom.id,
                entityType = "CATEGORY",
                action = "UPSERT",
                payloadJson = Json.encodeToString(custom)
            )
        )
    }

    override suspend fun deleteCategory(categoryId: String) {
        categoryDao.deleteById(categoryId)
        syncQueueDao.enqueue(
            SyncQueueEntity(
                entityId = categoryId,
                entityType = "CATEGORY",
                action = "DELETE",
                payloadJson = "{}"
            )
        )
    }
}
