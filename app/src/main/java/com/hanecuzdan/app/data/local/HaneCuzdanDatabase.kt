package com.hanecuzdan.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hanecuzdan.app.data.local.dao.BudgetDao
import com.hanecuzdan.app.data.local.dao.CategoryDao
import com.hanecuzdan.app.data.local.dao.LoanDao
import com.hanecuzdan.app.data.local.dao.SavingsGoalDao
import com.hanecuzdan.app.data.local.dao.SubscriptionDao
import com.hanecuzdan.app.data.local.dao.SyncQueueDao
import com.hanecuzdan.app.data.local.dao.TransactionDao
import com.hanecuzdan.app.data.local.entity.BudgetEntity
import com.hanecuzdan.app.data.local.entity.CategoryEntity
import com.hanecuzdan.app.data.local.entity.LoanEntity
import com.hanecuzdan.app.data.local.entity.SavingsGoalEntity
import com.hanecuzdan.app.data.local.entity.SubscriptionEntity
import com.hanecuzdan.app.data.local.entity.SyncQueueEntity
import com.hanecuzdan.app.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        SyncQueueEntity::class,
        SubscriptionEntity::class,
        SavingsGoalEntity::class,
        LoanEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class HaneCuzdanDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun loanDao(): LoanDao

    companion object {
        @Volatile
        private var INSTANCE: HaneCuzdanDatabase? = null

        fun getInstance(context: Context): HaneCuzdanDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HaneCuzdanDatabase::class.java,
                    "hanecuzdan.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
