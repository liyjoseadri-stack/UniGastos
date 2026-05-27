package com.example.unigastos

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NotificacionesManager {
    private const val CANAL_PRESUPUESTO = "presupuesto_alertas"

    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_PRESUPUESTO,
                "Alertas de presupuesto",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando los gastos superan el presupuesto mensual."
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(canal)
        }
    }

    fun notificarPresupuestoExcedido(
        context: Context,
        usuario: String,
        totalGastos: Double,
        presupuesto: Double,
        esTutor: Boolean
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val exceso = totalGastos - presupuesto
        val titulo = if (esTutor) "Alerta del estudiante" else "Presupuesto excedido"
        val texto = if (esTutor) {
            "$usuario excedio su presupuesto por \$${String.format("%.2f", exceso)}."
        } else {
            "Excediste tu presupuesto por \$${String.format("%.2f", exceso)}."
        }

        val notification = NotificationCompat.Builder(context, CANAL_PRESUPUESTO)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(("presupuesto_$usuario").hashCode(), notification)
    }
}
