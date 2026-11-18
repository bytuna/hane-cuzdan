package com.hanecuzdan.app.core.export

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.hanecuzdan.app.core.backup.BackupSnapshot
import com.hanecuzdan.app.domain.model.DataScope
import com.hanecuzdan.app.domain.model.Transaction
import com.hanecuzdan.app.domain.model.TransactionType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Tüm bütçe hareketlerini CSV veya PDF formatında dışa aktarır. */
object ExportManager {

    fun buildCsvContent(transactions: List<Transaction>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val sb = StringBuilder("\uFEFF") // UTF-8 BOM (Excel Türkçe karakter uyumu)
        sb.appendLine("Başlık,Tutar,Tür,Kapsam,Kategori,Tarih,Not")
        for (tx in transactions) {
            val typeStr = if (tx.type == TransactionType.INCOME) "Gelir" else "Gider"
            val scopeStr = if (tx.scope == DataScope.FAMILY) "Aile" else "Kişisel"
            val dateStr = dateFormat.format(Date(tx.timestamp))
            val cleanTitle = tx.title.replace("\"", "\"\"")
            val cleanNote = tx.note.replace("\"", "\"\"")
            sb.appendLine("\"$cleanTitle\",${tx.amount},$typeStr,$scopeStr,\"${tx.categoryName}\",$dateStr,\"$cleanNote\"")
        }
        return sb.toString()
    }

    fun exportCsvToFile(context: Context, transactions: List<Transaction>): File {
        val dir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val file = File(dir, "hanecuzdan_rapor_${System.currentTimeMillis()}.csv")
        file.writeText(buildCsvContent(transactions), Charsets.UTF_8)
        return file
    }

    fun exportPdfToFile(context: Context, transactions: List<Transaction>): File {
        val dir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val file = File(dir, "hanecuzdan_rapor_${System.currentTimeMillis()}.pdf")

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 11f }
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        var y = 50f
        canvas.drawText("HaneCüzdan - Bütçe Hareketleri Raporu", 40f, y, titlePaint)
        y += 30f

        for (tx in transactions) {
            if (y > 800f) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
            }
            val sign = if (tx.type == TransactionType.INCOME) "+" else "-"
            val scopeStr = if (tx.scope == DataScope.FAMILY) "Aile" else "Kişisel"
            val line = "${dateFormat.format(Date(tx.timestamp))} | ${tx.title} | ${tx.categoryName} ($scopeStr) | $sign${tx.amount} TL"
            canvas.drawText(line, 40f, y, bodyPaint)
            y += 20f
        }

        document.finishPage(page)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    fun exportJsonBackupToFile(context: Context, snapshot: BackupSnapshot): File {
        val dir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val file = File(dir, "hanecuzdan_yedek_${System.currentTimeMillis()}.json")
        val json = Json { prettyPrint = true }
        file.writeText(json.encodeToString(snapshot), Charsets.UTF_8)
        return file
    }

    fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Raporu Paylaş"))
    }
}
