package com.hanecuzdan.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.graphics.vector.ImageVector

enum class DrawerGroup(val title: String) {
    OVERVIEW("Genel Bakış"),
    HOUSEHOLD_PLANNING("Hane & Planlama"),
    COMMITMENTS_ARCHIVE("Taahhüt & Arşiv"),
    MANAGEMENT("Yönetim & Ayarlar")
}

/** Navigation Drawer ve Alt Bar hedefleri (mantıksal kullanım sırasına göre). */
enum class AppDestination(
    val label: String,
    val icon: ImageVector,
    val group: DrawerGroup
) {
    // 1. Genel Bakış
    DASHBOARD("Ana Sayfa", Icons.Default.Dashboard, DrawerGroup.OVERVIEW),
    MONTHLY_EXPENSES("Aylık Harcama", Icons.Default.ReceiptLong, DrawerGroup.OVERVIEW),
    UPCOMING_PAYMENTS("Gelecek Ödemeler", Icons.Default.EventAvailable, DrawerGroup.OVERVIEW),
    SMART_INSIGHTS("Akıllı Danışman", Icons.Default.AutoAwesome, DrawerGroup.OVERVIEW),

    // 2. Hane & Planlama
    SCOPE_TRANSACTIONS("Hane & Kişisel", Icons.Default.Groups, DrawerGroup.HOUSEHOLD_PLANNING),
    BUDGET_LIMITS("Bütçe Limitleri", Icons.Default.Speed, DrawerGroup.HOUSEHOLD_PLANNING),
    SAVINGS_GOALS("Hedefler", Icons.Default.Savings, DrawerGroup.HOUSEHOLD_PLANNING),

    // 3. Taahhüt & Arşiv
    SUBSCRIPTIONS("Abonelikler", Icons.Default.Autorenew, DrawerGroup.COMMITMENTS_ARCHIVE),
    LOANS("Krediler & Taksitler", Icons.Default.AccountBalance, DrawerGroup.COMMITMENTS_ARCHIVE),
    RECEIPT_ARCHIVE("Fiş Arşivi", Icons.Default.ReceiptLong, DrawerGroup.COMMITMENTS_ARCHIVE),

    // 4. Yönetim & Ayarlar (Güvenlik ayarları Ayarlar sekmesi altındadır)
    CATEGORIES("Kategoriler", Icons.Default.Category, DrawerGroup.MANAGEMENT),
    SETTINGS("Ayarlar", Icons.Default.Settings, DrawerGroup.MANAGEMENT)
}
