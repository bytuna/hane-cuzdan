package com.hanecuzdan.app

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hanecuzdan.app.data.local.entity.SavingsGoalEntity
import com.hanecuzdan.app.data.local.entity.SubscriptionEntity
import com.hanecuzdan.app.domain.model.DataScope
import com.hanecuzdan.app.domain.model.SyncConnectionState
import com.hanecuzdan.app.domain.model.Transaction
import com.hanecuzdan.app.domain.model.TransactionType
import com.hanecuzdan.app.ui.dashboard.DashboardContent
import com.hanecuzdan.app.ui.dashboard.DashboardViewModel
import com.hanecuzdan.app.ui.navigation.AppDestination
import com.hanecuzdan.app.ui.navigation.HaneCuzdanDrawerContent
import com.hanecuzdan.app.ui.screens.BudgetLimitsScreen
import com.hanecuzdan.app.ui.screens.CategoriesScreen
import com.hanecuzdan.app.ui.screens.ExpenseSplitScreen
import com.hanecuzdan.app.ui.screens.MonthlyExpensesScreen
import com.hanecuzdan.app.ui.screens.ReceiptArchiveScreen
import com.hanecuzdan.app.ui.screens.SavingsGoalsScreen
import com.hanecuzdan.app.ui.screens.ScopeTransactionsScreen
import com.hanecuzdan.app.ui.screens.SettingsScreen
import com.hanecuzdan.app.ui.screens.SmartInsightsScreen
import com.hanecuzdan.app.ui.screens.SubscriptionsScreen
import com.hanecuzdan.app.ui.screens.UpcomingPaymentsScreen
import kotlinx.coroutines.flow.map
import java.util.UUID
import com.hanecuzdan.app.ui.security.LockScreen
import com.hanecuzdan.app.ui.security.SecuritySettingsScreen
import com.hanecuzdan.app.ui.theme.HaneCuzdanTheme
import com.hanecuzdan.app.ui.transaction.AddTransactionSheet
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as HaneCuzdanApp
        val container = app.container

        setContent {
            var themeMode by remember { mutableStateOf(container.securityPreferences.themeMode) }
            val isDark = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            HaneCuzdanTheme(darkTheme = isDark) {
                var isUnlocked by remember {
                    mutableStateOf(!container.securityPreferences.isAppLockEnabled)
                }

                if (!isUnlocked) {
                    LockScreen(
                        securityPreferences = container.securityPreferences,
                        onUnlocked = { isUnlocked = true }
                    )
                } else {
                    MainScaffold(
                        app = app,
                        onThemeModeChanged = { themeMode = it },
                        onSaveBudget = { budget ->
                            lifecycleScope.launch { container.budgetRepository.upsertBudget(budget) }
                        },
                        onDeleteBudget = { id ->
                            lifecycleScope.launch { container.budgetRepository.deleteBudget(id) }
                        },
                        onAddCategory = { cat ->
                            lifecycleScope.launch { container.categoryRepository.addCustomCategory(cat) }
                        },
                        onDeleteCategory = { id ->
                            lifecycleScope.launch { container.categoryRepository.deleteCategory(id) }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    app: HaneCuzdanApp,
    onThemeModeChanged: (Int) -> Unit,
    onSaveBudget: (com.hanecuzdan.app.domain.model.Budget) -> Unit,
    onDeleteBudget: (String) -> Unit,
    onAddCategory: (com.hanecuzdan.app.domain.model.Category) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    val container = app.container
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(
            container.transactionRepository,
            container.categoryRepository,
            container.budgetRepository,
            container.syncRepository,
            container.subscriptionDao,
            container.metricsCalculator,
            container.budgetEvaluator,
            container.budgetNotificationHelper
        )
    )

    val state by viewModel.uiState.collectAsState()
    val subscriptions by remember(container) {
        container.subscriptionDao.observeAll().map { list -> list.map { it.toDomain() } }
    }.collectAsState(initial = emptyList())
    val savingsGoals by remember(container) {
        container.savingsGoalDao.observeAll().map { list -> list.map { it.toDomain() } }
    }.collectAsState(initial = emptyList())
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var navStack by remember { mutableStateOf(listOf(AppDestination.DASHBOARD)) }
    val currentDestination = navStack.last()
    var showAddSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    BackHandler(enabled = drawerState.isOpen || navStack.size > 1) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (navStack.size > 1) {
            navStack = navStack.dropLast(1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HaneCuzdanDrawerContent(
                selectedDestination = currentDestination,
                userName = container.securityPreferences.userName,
                familyId = container.securityPreferences.familyId,
                onDestinationSelected = { dest ->
                    if (dest == AppDestination.DASHBOARD) {
                        navStack = listOf(AppDestination.DASHBOARD)
                    } else if (dest != currentDestination) {
                        navStack = navStack + dest
                    }
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentDestination.label) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menü")
                        }
                    },
                    actions = {
                        val syncIcon = when (state.syncStatus.state) {
                            SyncConnectionState.SYNCED -> Icons.Default.CloudDone
                            SyncConnectionState.SYNCING -> Icons.Default.CloudSync
                            SyncConnectionState.OFFLINE_PENDING, SyncConnectionState.ERROR -> Icons.Default.CloudOff
                        }
                        IconButton(onClick = { viewModel.triggerSync() }) {
                            Icon(syncIcon, contentDescription = "Senkronizasyon Durumu")
                        }
                    }
                )
            },
            bottomBar = {
                val quickTabs = listOf(
                    AppDestination.DASHBOARD,
                    AppDestination.SUBSCRIPTIONS,
                    AppDestination.SAVINGS_GOALS,
                    AppDestination.RECEIPT_ARCHIVE
                )
                NavigationBar {
                    quickTabs.forEach { dest ->
                        NavigationBarItem(
                            selected = currentDestination == dest,
                            onClick = {
                                if (dest == AppDestination.DASHBOARD) {
                                    navStack = listOf(AppDestination.DASHBOARD)
                                } else if (dest != currentDestination) {
                                    navStack = navStack + dest
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label, maxLines = 1) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentDestination) {
                    AppDestination.DASHBOARD -> DashboardContent(
                        state = state,
                        onPeriodSelected = viewModel::selectPeriod,
                        onAddClick = {
                            editingTransaction = null
                            showAddSheet = true
                        },
                        onDeleteTransaction = { viewModel.deleteTransaction(it.id) },
                        onEditTransaction = {
                            editingTransaction = it
                            showAddSheet = true
                        },
                        onSyncClick = viewModel::triggerSync,
                        onMonthlyExpenseClick = {
                            navStack = navStack + AppDestination.MONTHLY_EXPENSES
                        },
                        onUpcomingPaymentsClick = {
                            navStack = navStack + AppDestination.UPCOMING_PAYMENTS
                        }
                    )
                    AppDestination.UPCOMING_PAYMENTS -> UpcomingPaymentsScreen(
                        subscriptions = subscriptions,
                        onRecordPayment = { sub ->
                            viewModel.saveTransaction(
                                Transaction(
                                    id = UUID.randomUUID().toString(),
                                    title = sub.name,
                                    amount = sub.amount,
                                    type = TransactionType.EXPENSE,
                                    scope = sub.scope,
                                    categoryId = "cat_fatura",
                                    categoryName = sub.categoryName,
                                    timestamp = System.currentTimeMillis(),
                                    note = "Gelecek ödeme erken işlendi",
                                    createdByUserId = container.securityPreferences.userName,
                                    familyId = if (sub.scope == DataScope.FAMILY) container.securityPreferences.familyId else null
                                )
                            )
                        }
                    )
                    AppDestination.MONTHLY_EXPENSES -> MonthlyExpensesScreen(
                        allTransactions = state.transactions,
                        subscriptions = subscriptions
                    )
                    AppDestination.SMART_INSIGHTS -> SmartInsightsScreen(
                        transactions = state.transactions,
                        monthlyBudgetPool = state.metrics.totalBudgetPool,
                        subscriptions = subscriptions
                    )
                    AppDestination.SUBSCRIPTIONS -> SubscriptionsScreen(
                        subscriptions = subscriptions,
                        onAddSubscription = { sub ->
                            scope.launch { container.subscriptionDao.upsert(SubscriptionEntity.fromDomain(sub)) }
                        },
                        onDeleteSubscription = { id ->
                            scope.launch { container.subscriptionDao.deleteById(id) }
                        },
                        onRecordPayment = { sub ->
                            viewModel.saveTransaction(
                                Transaction(
                                    id = UUID.randomUUID().toString(),
                                    title = sub.name,
                                    amount = sub.amount,
                                    type = TransactionType.EXPENSE,
                                    scope = sub.scope,
                                    categoryId = "cat_fatura",
                                    categoryName = sub.categoryName,
                                    timestamp = System.currentTimeMillis(),
                                    note = "Abonelik / Sabit ödeme",
                                    createdByUserId = container.securityPreferences.userName,
                                    familyId = if (sub.scope == DataScope.FAMILY) container.securityPreferences.familyId else null
                                )
                            )
                        }
                    )
                    AppDestination.SAVINGS_GOALS -> SavingsGoalsScreen(
                        goals = savingsGoals,
                        onUpsertGoal = { goal ->
                            scope.launch { container.savingsGoalDao.upsert(SavingsGoalEntity.fromDomain(goal)) }
                        },
                        onDeleteGoal = { id ->
                            scope.launch { container.savingsGoalDao.deleteById(id) }
                        }
                    )
                    AppDestination.EXPENSE_SPLIT -> ExpenseSplitScreen()
                    AppDestination.FAMILY_POOL -> ScopeTransactionsScreen(
                        title = "Aile Havuzu",
                        subtitle = "Bulut tabanlı senkronize ortak aile harcamaları ve abonelikleri",
                        scope = DataScope.FAMILY,
                        allTransactions = state.transactions,
                        subscriptions = subscriptions,
                        onDelete = { viewModel.deleteTransaction(it.id) },
                        onEdit = { editingTransaction = it; showAddSheet = true }
                    )
                    AppDestination.PERSONAL_PROFILE -> ScopeTransactionsScreen(
                        title = "Kişisel Profil",
                        subtitle = "Yalnızca bu cihazda tutulan kişisel harcamalar ve abonelikler",
                        scope = DataScope.PERSONAL,
                        allTransactions = state.transactions,
                        subscriptions = subscriptions,
                        onDelete = { viewModel.deleteTransaction(it.id) },
                        onEdit = { editingTransaction = it; showAddSheet = true }
                    )
                    AppDestination.RECEIPT_ARCHIVE -> ReceiptArchiveScreen(
                        transactionsWithReceipts = state.transactions.filter { it.receiptPath != null }
                    )
                    AppDestination.CATEGORIES -> CategoriesScreen(
                        categories = state.categories,
                        onUpsertCategory = onAddCategory,
                        onDeleteCategory = onDeleteCategory
                    )
                    AppDestination.BUDGET_LIMITS -> BudgetLimitsScreen(
                        categories = state.categories,
                        statuses = state.budgetStatuses,
                        onSaveBudget = onSaveBudget,
                        onDeleteBudget = onDeleteBudget
                    )
                    AppDestination.SETTINGS -> SettingsScreen(
                        securityPreferences = container.securityPreferences,
                        transactions = state.transactions,
                        categories = state.categories,
                        budgets = state.budgets,
                        onThemeModeChanged = onThemeModeChanged,
                        onRestoreSnapshot = { snapshot ->
                            snapshot.categories.forEach { container.categoryRepository.addCustomCategory(it) }
                            snapshot.budgets.forEach { container.budgetRepository.upsertBudget(it) }
                            snapshot.transactions.forEach { container.transactionRepository.upsertTransaction(it) }
                        }
                    )
                    AppDestination.SECURITY -> SecuritySettingsScreen(
                        securityPreferences = container.securityPreferences
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        val recentTitles = remember(state.transactions) {
            state.transactions.map { it.title }.distinct().take(5)
        }
        AddTransactionSheet(
            categories = state.categories,
            recentTitles = recentTitles,
            existingTransaction = editingTransaction,
            familyId = container.securityPreferences.familyId,
            userName = container.securityPreferences.userName,
            receiptImageManager = container.receiptImageManager,
            onDismiss = { showAddSheet = false },
            onSave = { tx ->
                viewModel.saveTransaction(tx)
                showAddSheet = false
            }
        )
    }
}
