package com.hanecuzdan.app.ui.transaction

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.hanecuzdan.app.core.media.ReceiptImageManager
import com.hanecuzdan.app.core.media.ReceiptOcrManager
import com.hanecuzdan.app.domain.model.Category
import com.hanecuzdan.app.domain.model.DataScope
import com.hanecuzdan.app.domain.model.Transaction
import com.hanecuzdan.app.domain.model.TransactionType
import com.hanecuzdan.app.ui.util.TurkishCurrencyVisualTransformation
import com.hanecuzdan.app.ui.util.parseTurkishCurrency
import com.hanecuzdan.app.ui.util.sanitizeCurrencyInput
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionSheet(
    categories: List<Category>,
    recentTitles: List<String> = emptyList(),
    existingTransaction: Transaction? = null,
    familyId: String,
    userName: String,
    receiptImageManager: ReceiptImageManager,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    val context = LocalContext.current
    var scope by remember { mutableStateOf(existingTransaction?.scope ?: DataScope.FAMILY) }
    var type by remember { mutableStateOf(existingTransaction?.type ?: TransactionType.EXPENSE) }
    var title by remember { mutableStateOf(existingTransaction?.title ?: "") }
    var selectedTimestamp by remember { mutableStateOf(existingTransaction?.timestamp ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var amountText by remember {
        mutableStateOf(
            existingTransaction?.amount?.let {
                sanitizeCurrencyInput(it.toString().replace('.', ','))
            } ?: ""
        )
    }
    var note by remember { mutableStateOf(existingTransaction?.note ?: "") }
    val filteredCategories = remember(categories, type) {
        categories.filter { it.type == type }.ifEmpty { categories }
    }
    var selectedCategory by remember(type, filteredCategories) {
        mutableStateOf(
            filteredCategories.firstOrNull { it.id == existingTransaction?.categoryId }
                ?: filteredCategories.firstOrNull()
        )
    }
    val coroutineScope = rememberCoroutineScope()
    var receiptPath by remember { mutableStateOf(existingTransaction?.receiptPath) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var scanInfo by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = tempPhotoUri
        if (success && uri != null) {
            receiptPath = receiptImageManager.compressAndSaveFromUri(uri)
            isScanning = true
            scanInfo = "Fiş taranıyor..."
            coroutineScope.launch {
                val scanned = ReceiptOcrManager.scanReceiptUri(context, uri)
                isScanning = false
                if (!scanned.merchantTitle.isNullOrBlank() && title.isBlank()) {
                    title = scanned.merchantTitle
                }
                scanned.totalAmount?.let { amt ->
                    amountText = sanitizeCurrencyInput(String.format(Locale.US, "%.2f", amt).replace('.', ','))
                }
                scanned.suggestedCategoryId?.let { catId ->
                    categories.firstOrNull { it.id == catId }?.let { selectedCategory = it }
                }
                scanInfo = if (scanned.totalAmount != null || !scanned.merchantTitle.isNullOrBlank()) {
                    "Fiş tarandı ve ilgili alanlar dolduruldu ✓"
                } else {
                    "Fiş kaydedildi, tutar otomatik okunamadı — elle girebilirsiniz."
                }
            }
        }
    }

    fun launchCameraSafely() {
        runCatching {
            val file = receiptImageManager.createTempCaptureFile()
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        }.onFailure {
            errorText = "Kamera açılamadı: ${it.localizedMessage ?: "Bilinmeyen hata"}"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCameraSafely()
        } else {
            errorText = "Fiş fotoğrafı çekmek için kamera izni gereklidir."
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (existingTransaction == null) "Yeni İşlem Ekle" else "İşlemi Düzenle",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text("1. Kapsam Seçimi (Aile mi, Kişisel mi?)", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = scope == DataScope.FAMILY,
                    onClick = { scope = DataScope.FAMILY },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Aile Havuzu") }
                SegmentedButton(
                    selected = scope == DataScope.PERSONAL,
                    onClick = { scope = DataScope.PERSONAL },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Kişisel (Yerel)") }
            }

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

            OutlinedTextField(
                value = title,
                onValueChange = { title = it; errorText = null },
                label = { Text("Açıklama / Başlık") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (recentTitles.isNotEmpty() && title.isBlank()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    recentTitles.take(5).forEach { suggestion ->
                        SuggestionChip(
                            onClick = { title = suggestion },
                            label = { Text(suggestion) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = sanitizeCurrencyInput(it); errorText = null },
                label = { Text("Tutar (₺)") },
                placeholder = { Text("Örn: 50.000,00") },
                suffix = { Text("₺") },
                visualTransformation = TurkishCurrencyVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            val dateText = remember(selectedTimestamp) {
                SimpleDateFormat("dd MMMM yyyy", Locale("tr", "TR")).format(Date(selectedTimestamp))
            }
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null)
                Text(" İşlem Tarihi: $dateText")
            }

            Text("Kategori", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredCategories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory?.id == cat.id,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.name) }
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Not (İsteğe bağlı)") },
                modifier = Modifier.fillMaxWidth()
            )

            if (isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (scanInfo != null) {
                Text(text = scanInfo!!, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
            if (errorText != null) {
                Text(text = errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        val hasCam = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasCam) {
                            launchCameraSafely()
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Text(if (receiptPath == null) " Fiş Çek" else " Fiş Eklendi ✓")
                }

                Button(
                    onClick = {
                        val amount = parseTurkishCurrency(amountText)
                        if (title.isBlank() || amount == null || amount <= 0.0) {
                            errorText = "Lütfen geçerli bir başlık ve tutar girin."
                            return@Button
                        }
                        val cat = selectedCategory ?: return@Button

                        val tx = Transaction(
                            id = existingTransaction?.id ?: UUID.randomUUID().toString(),
                            title = title.trim(),
                            amount = amount,
                            type = type,
                            scope = scope,
                            categoryId = cat.id,
                            categoryName = cat.name,
                            timestamp = selectedTimestamp,
                            note = note.trim(),
                            receiptPath = receiptPath,
                            createdByUserId = userName,
                            familyId = if (scope == DataScope.FAMILY) familyId else null
                        )
                        onSave(tx)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Text(" Kaydet")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedTimestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { selectedTimestamp = it }
                        showDatePicker = false
                    }
                ) { Text("Tamam") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("İptal") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
