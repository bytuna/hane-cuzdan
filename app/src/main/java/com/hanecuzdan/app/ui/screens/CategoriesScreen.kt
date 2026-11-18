package com.hanecuzdan.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hanecuzdan.app.domain.model.Category
import com.hanecuzdan.app.domain.model.DataScope
import com.hanecuzdan.app.domain.model.TransactionType
import java.util.UUID

@Composable
fun CategoriesScreen(
    categories: List<Category>,
    onUpsertCategory: (Category) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var newScope by remember { mutableStateOf(DataScope.FAMILY) }
    var newType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Kategoriler", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                SegmentedButton(
                    selected = newType == TransactionType.EXPENSE,
                    onClick = { newType = TransactionType.EXPENSE },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Gider") }
                SegmentedButton(
                    selected = newType == TransactionType.INCOME,
                    onClick = { newType = TransactionType.INCOME },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Gelir") }
            }
            SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                SegmentedButton(
                    selected = newScope == DataScope.FAMILY,
                    onClick = { newScope = DataScope.FAMILY },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Aile") }
                SegmentedButton(
                    selected = newScope == DataScope.PERSONAL,
                    onClick = { newScope = DataScope.PERSONAL },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Kişisel") }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Yeni Kategori Adı") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(
                onClick = {
                    if (newName.isNotBlank()) {
                        onUpsertCategory(
                            Category(
                                id = "cat_${UUID.randomUUID()}",
                                name = newName.trim(),
                                iconKey = "label",
                                colorHex = 0xFF006C51,
                                scope = newScope,
                                isDefault = false,
                                type = newType
                            )
                        )
                        newName = ""
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Kategori Ekle")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories, key = { it.id }) { cat ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cat.name, style = MaterialTheme.typography.titleMedium)
                            val typeLabel = if (cat.type == TransactionType.INCOME) "Gelir" else "Gider"
                            val scopeLabel = if (cat.scope == DataScope.FAMILY) "Aile" else "Kişisel"
                            Text(
                                text = "$typeLabel • $scopeLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            IconButton(onClick = { editingCategory = cat }) {
                                Icon(Icons.Default.Edit, contentDescription = "Düzenle")
                            }
                            IconButton(onClick = { onDeleteCategory(cat.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Sil")
                            }
                        }
                    }
                }
            }
        }
    }

    editingCategory?.let { cat ->
        EditCategoryDialog(
            category = cat,
            onDismiss = { editingCategory = null },
            onConfirm = { updated ->
                onUpsertCategory(updated)
                editingCategory = null
            }
        )
    }
}

@Composable
private fun EditCategoryDialog(
    category: Category,
    onDismiss: () -> Unit,
    onConfirm: (Category) -> Unit
) {
    var name by remember { mutableStateOf(category.name) }
    var type by remember { mutableStateOf(category.type) }
    var scope by remember { mutableStateOf(category.scope) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kategoriyi Düzenle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Kategori Adı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Gider") }
                    SegmentedButton(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Gelir") }
                }
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = scope == DataScope.FAMILY,
                        onClick = { scope = DataScope.FAMILY },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Aile") }
                    SegmentedButton(
                        selected = scope == DataScope.PERSONAL,
                        onClick = { scope = DataScope.PERSONAL },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Kişisel") }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(category.copy(name = name.trim(), type = type, scope = scope))
                    }
                }
            ) { Text("Kaydet") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}
