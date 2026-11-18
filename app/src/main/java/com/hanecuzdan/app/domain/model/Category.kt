package com.hanecuzdan.app.domain.model

import kotlinx.serialization.Serializable

/** Gider/Gelir kategorisi domain modeli. */
@Serializable
data class Category(
    val id: String,
    val name: String,
    val iconKey: String,
    val colorHex: Long,
    val scope: DataScope = DataScope.FAMILY,
    val isDefault: Boolean = false,
    val type: TransactionType = TransactionType.EXPENSE
)

/** Uygulama ilk kurulumunda gelen hazır gider ve gelir kategorileri. */
object DefaultCategories {
    val list: List<Category> = listOf(
        // Gider Kategorileri
        Category("cat_market", "Market", "shopping_cart", 0xFF2E7D32, DataScope.FAMILY, isDefault = true, TransactionType.EXPENSE),
        Category("cat_arac", "Araç", "directions_car", 0xFF1565C0, DataScope.FAMILY, isDefault = true, TransactionType.EXPENSE),
        Category("cat_saglik", "Sağlık", "medical_services", 0xFFC62828, DataScope.FAMILY, isDefault = true, TransactionType.EXPENSE),
        Category("cat_eglence", "Eğlence", "movie", 0xFF6A1B9A, DataScope.FAMILY, isDefault = true, TransactionType.EXPENSE),
        Category("cat_kredi", "Banka Kredi", "account_balance", 0xFF4E342E, DataScope.FAMILY, isDefault = true, TransactionType.EXPENSE),
        Category("cat_kredi_karti", "Banka Kredi Kartı", "credit_card", 0xFF00838F, DataScope.FAMILY, isDefault = true, TransactionType.EXPENSE),
        Category("cat_fatura", "Fatura", "receipt_long", 0xFFEF6C00, DataScope.FAMILY, isDefault = true, TransactionType.EXPENSE),
        Category("cat_egitim", "Eğitim", "school", 0xFF283593, DataScope.FAMILY, isDefault = true, TransactionType.EXPENSE),
        Category("cat_giyim", "Giyim", "checkroom", 0xFFAD1457, DataScope.FAMILY, isDefault = true, TransactionType.EXPENSE),
        Category("cat_kira", "Kira", "home", 0xFF37474F, DataScope.FAMILY, isDefault = true, TransactionType.EXPENSE),
        // Gelir Kategorileri (iltan987/gelir-gider ilhamıyla)
        Category("cat_maas", "Maaş", "payments", 0xFF2E7D32, DataScope.FAMILY, isDefault = true, TransactionType.INCOME),
        Category("cat_ek_gelir", "Ek Gelir", "work", 0xFF00897B, DataScope.FAMILY, isDefault = true, TransactionType.INCOME),
        Category("cat_kira_geliri", "Kira Geliri", "home_work", 0xFF1565C0, DataScope.FAMILY, isDefault = true, TransactionType.INCOME),
        Category("cat_yatirim", "Yatırım / Faiz", "trending_up", 0xFF6A1B9A, DataScope.FAMILY, isDefault = true, TransactionType.INCOME),
        Category("cat_hediye", "İade / Hediye", "card_giftcard", 0xFFEF6C00, DataScope.FAMILY, isDefault = true, TransactionType.INCOME)
    )
}
