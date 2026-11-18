package com.hanecuzdan.app.ui.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.hanecuzdan.app.domain.model.SyncStatusSummary
import com.hanecuzdan.app.domain.model.TimePeriod
import com.hanecuzdan.app.domain.usecase.DashboardMetrics
import com.hanecuzdan.app.ui.theme.HaneCuzdanTheme
import org.junit.Rule
import org.junit.Test

class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `ozet kartlari ve zaman filtreleri gorunur olmali`() {
        val state = DashboardUiState(
            isLoading = false,
            selectedPeriod = TimePeriod.MONTHLY,
            metrics = DashboardMetrics(
                totalAssets = 25000.0,
                remainingMonthlyLimit = 7500.0,
                todayExpense = 320.0
            ),
            syncStatus = SyncStatusSummary()
        )

        composeTestRule.setContent {
            HaneCuzdanTheme {
                DashboardContent(
                    state = state,
                    onPeriodSelected = {},
                    onAddClick = {},
                    onDeleteTransaction = {},
                    onEditTransaction = {},
                    onSyncClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Kalan Bütçe").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bu Ay Kalan Limit").assertIsDisplayed()
        composeTestRule.onNodeWithText("Aylık Harcama ↗").assertIsDisplayed()
        composeTestRule.onNodeWithText("Aylık").assertIsDisplayed()
    }
}
