package com.hanecuzdan.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hanecuzdan.app.data.local.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC")
    suspend fun getAllSnapshot(): List<SavingsGoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteById(id: String)
}
