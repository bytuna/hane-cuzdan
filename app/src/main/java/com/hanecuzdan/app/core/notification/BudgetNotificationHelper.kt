package com.hanecuzdan.app.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hanecuzdan.app.domain.model.BudgetStatus
import com.hanecuzdan.app.domain.model.BudgetThresholdLevel

/** Bütçe limiti %80 ve %100 eşiklerine ulaşıldığında yerel bildirim gönderir. */
class BudgetNotificationHelper(private val context: Context) {

    init {
        createChannel()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Bütçe Limit Uyarıları",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Kategori veya toplam bütçe limit eşikleri aşıldığında uyarır."
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun notifyIfNeeded(statuses: List<BudgetStatus>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        statuses.forEachIndexed { index, status ->
            val (title, body) = when (status.thresholdLevel) {
                BudgetThresholdLevel.WARNING_80 -> {
                    "Bütçe Uyarısı (%80)" to "${status.budget.categoryName} limitinizin %${(status.usageRatio * 100).toInt()}'ine ulaştınız."
                }
                BudgetThresholdLevel.EXCEEDED_100 -> {
                    "Bütçe Limiti Aşıldı!" to "${status.budget.categoryName} limiti aşıldı (${status.spentAmount.toInt()} / ${status.budget.limitAmount.toInt()} TL)."
                }
                BudgetThresholdLevel.NONE -> return@forEachIndexed
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(1000 + index, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "budget_alerts"
    }
}
