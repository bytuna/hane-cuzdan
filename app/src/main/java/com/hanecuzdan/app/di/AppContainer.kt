package com.hanecuzdan.app.di

import android.content.Context
import com.hanecuzdan.app.core.backup.GitHubBackupEngine
import com.hanecuzdan.app.core.media.ReceiptImageManager
import com.hanecuzdan.app.core.notification.BudgetNotificationHelper
import com.hanecuzdan.app.core.security.SecurityPreferences
import com.hanecuzdan.app.core.update.AppUpdateManager
import com.hanecuzdan.app.data.local.HaneCuzdanDatabase
import com.hanecuzdan.app.data.remote.FirestoreFamilyDataSource
import com.hanecuzdan.app.data.repository.BudgetRepositoryImpl
import com.hanecuzdan.app.data.repository.CategoryRepositoryImpl
import com.hanecuzdan.app.data.repository.TransactionRepositoryImpl
import com.hanecuzdan.app.data.sync.SyncManager
import com.hanecuzdan.app.domain.repository.BudgetRepository
import com.hanecuzdan.app.domain.repository.CategoryRepository
import com.hanecuzdan.app.domain.repository.SyncRepository
import com.hanecuzdan.app.domain.repository.TransactionRepository
import com.hanecuzdan.app.domain.usecase.BudgetEvaluator
import com.hanecuzdan.app.domain.usecase.DashboardMetricsCalculator

/** Uygulama genelindeki bağımlılıkları yöneten hafif DI konteyneri. */
class AppContainer(context: Context) {

    private val database = HaneCuzdanDatabase.getInstance(context)
    val securityPreferences = SecurityPreferences(context)
    val receiptImageManager = ReceiptImageManager(context)
    val budgetNotificationHelper = BudgetNotificationHelper(context)
    val appUpdateManager = AppUpdateManager(context)
    val gitHubBackupEngine = GitHubBackupEngine()
    val subscriptionDao = database.subscriptionDao()
    val savingsGoalDao = database.savingsGoalDao()
    val loanDao = database.loanDao()

    val transactionRepository: TransactionRepository = TransactionRepositoryImpl(
        transactionDao = database.transactionDao(),
        syncQueueDao = database.syncQueueDao()
    )

    val categoryRepository: CategoryRepository = CategoryRepositoryImpl(
        categoryDao = database.categoryDao()
    )

    val budgetRepository: BudgetRepository = BudgetRepositoryImpl(
        budgetDao = database.budgetDao(),
        syncQueueDao = database.syncQueueDao()
    )

    val syncRepository: SyncRepository = SyncManager(
        context = context,
        syncQueueDao = database.syncQueueDao(),
        transactionDao = database.transactionDao(),
        budgetDao = database.budgetDao(),
        remoteDataSource = FirestoreFamilyDataSource(),
        familyIdProvider = { securityPreferences.familyId },
        memberIdProvider = { securityPreferences.memberId },
        userNameProvider = { securityPreferences.userName }
    )

    val budgetEvaluator = BudgetEvaluator()
    val metricsCalculator = DashboardMetricsCalculator()
}
