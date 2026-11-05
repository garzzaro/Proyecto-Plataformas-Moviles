package com.example.model

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * HabitNotificationScheduler gestiona el canal de notificaciones y el envío periódico
 * de recordatorios de hábitos hacia la barra de notificaciones del sistema Android.
 */
object HabitNotificationScheduler {

    const val CHANNEL_ID = "habitocto_reminders_channel"
    const val CHANNEL_NAME = "Recordatorios de Hábitos HabitOcto"
    private const val NOTIFICATION_BASE_ID = 2000

    private var periodicJob: Job? = null
    private var reminderIndex = 0

    private val REMINDER_TIPS_ES = listOf(
        "🐙 ¡Hora de hidratarte! Toma un vaso de agua para cuidar a tu pulpo.",
        "⚡ Mantén tu racha: completa al menos un microhábito en HabitOcto hoy.",
        "✨ Tu mascota te espera en el Santuario para entrenar y subir de nivel.",
        "📖 ¿Listo para leer una página o meditar 5 minutos? ¡Cada paso cuenta!",
        "🏆 Revisa el Ranking de amigos con código #XXX y sube de posición."
    )

    private val REMINDER_TIPS_EN = listOf(
        "🐙 Stay hydrated! Drink a glass of water to care for your Octo.",
        "⚡ Keep your streak alive: complete at least one micro-habit today.",
        "✨ Your pet is waiting in the Sanctuary to train and level up.",
        "📖 Ready to read a page or meditate for 5 min? Every step counts!",
        "🏆 Check the #XXX Friend Ranking and climb the leaderboard."
    )

    /**
     * Crea el canal de notificación requerido a partir de Android 8.0 (API 26).
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                description = "Notificaciones periódicas para motivar el cumplimiento de tus hábitos"
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Comprueba si la aplicación tiene autorización para enviar notificaciones (Android 13+).
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Envía una notificación real al sistema operativo si cuenta con permiso.
     */
    fun sendReminder(
        context: Context,
        title: String? = null,
        message: String? = null,
        isEnglish: Boolean = false
    ): Boolean {
        createNotificationChannel(context)

        if (!hasNotificationPermission(context)) {
            return false
        }

        val tips = if (isEnglish) REMINDER_TIPS_EN else REMINDER_TIPS_ES
        val chosenMessage = message ?: tips[reminderIndex % tips.size]
        val chosenTitle = title ?: if (isEnglish) "🐙 HabitOcto Reminder" else "🐙 Recordatorio de HabitOcto"
        reminderIndex += 1

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pulpo)
            .setContentTitle(chosenTitle)
            .setContentText(chosenMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(chosenMessage))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationId = NOTIFICATION_BASE_ID + (reminderIndex % 50)
        return try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    /**
     * Inicia el ciclo periódico de recordatorios (por ejemplo, cada 20 minutos).
     */
    fun startPeriodicReminders(
        context: Context,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
        intervalMinutes: Long = 20L,
        isEnglish: Boolean = false
    ) {
        if (periodicJob?.isActive == true) return

        periodicJob = scope.launch {
            createNotificationChannel(context)
            while (isActive) {
                // Espera el intervalo configurado
                delay(intervalMinutes * 60_000L)
                sendReminder(context, isEnglish = isEnglish)
            }
        }
    }

    /**
     * Detiene el ciclo periódico si es necesario.
     */
    fun stopPeriodicReminders() {
        periodicJob?.cancel()
        periodicJob = null
    }
}
