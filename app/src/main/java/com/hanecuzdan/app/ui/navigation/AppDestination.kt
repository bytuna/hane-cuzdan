package com.hanecuzdan.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.graphics.vector.ImageVector

/** Navigation Drawer ve Alt Bar hedefleri (mantıksal kullanım sırasına göre). */
enum class AppDestination(
    val label: String,
    val icon: ImageVector
) {
    // 1. Genel Bakış & Harcama Takibi
    DASHBOARD("Ana Sayfa", Icons.Default.Dashboard),
    SMART_INSIGHTS("Akıllı Danışman", Icons.Default.AutoAwesome),
    MONTHLY_EXPENSES("Aylık Harcama", Icons.Default.ReceiptLong),
    UPCOMING_PAYMENTS("Gelecek Ödemeler", Icons.Default.EventAvailable),

    // 2. Hane & Kişisel Alanlar
    FAMILY_POOL("Aile Havuzu", Icons.Default.Groups),
    PERSONAL_PROFILE("Kişisel Profil", Icons.Default.AccountCircle),
    EXPENSE_SPLIT("Masraf Bölüş", Icons.Default.Calculate),

    // 3. Planlama & Arşiv
    BUDGET_LIMITS("Bütçe Limitleri", Icons.Default.Speed),
    SUBSCRIPTIONS("Abonelikler", Icons.Default.Autorenew),
    SAVINGS_GOALS("Hedefler", Icons.Default.Savings),
    CATEGORIES("Kategoriler", Icons.Default.Category),
    RECEIPT_ARCHIVE("Fiş Arşivi", Icons.Default.ReceiptLong),

    // 4. Güvenlik & Ayarlar (Bulut yedekleme yalnızca Firebase üzerinden yapılır)
    SECURITY("Güvenlik", Icons.Default.Lock),
    SETTINGS("Ayarlar", Icons.Default.Settings)
}
