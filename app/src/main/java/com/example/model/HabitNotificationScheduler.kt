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
 *
 * - **¿Para qué sirve?**
 *   Envía alertas motivacionales y avisos periódicos a la barra de estado de Android
 *   para recordar al usuario mantener su racha activa, cuidar a su pulpo/mascota en el
 *   santuario y cumplir sus micro-hábitos diarios programados.
 *
 * - **¿Cómo funciona?**
 *   1. Crea y registra el `NotificationChannel` requerido a partir de Android 8.0 (API 26).
 *   2. Verifica si el usuario concedió el permiso de tiempo de ejecución `POST_NOTIFICATIONS` (Android 13+).
 *   3. Lanza recordatorios rotativos en español e inglés configurando un `PendingIntent` seguro hacia `MainActivity`.
 *   4. Ejecuta un ciclo periódico en segundo plano con Corrutinas (`Dispatchers.Default`) con intervalo configurable.
 *
 * - **¿Qué pasa si lo quitamos?**
 *   La aplicación carecería de recordatorios automáticos en la barra de estado del sistema operativo,
 *   dependiendo exclusivamente de que el usuario recuerde abrir la app manualmente.
 */
object HabitNotificationScheduler {

    /** Identificador único del canal de notificaciones en Android */
    const val CHANNEL_ID = "habitocto_reminders_channel"

    /** Nombre legible para el usuario en los ajustes de notificaciones del sistema */
    const val CHANNEL_NAME = "Recordatorios de Hábitos HabitOcto"

    /** Base de IDs para rotación de notificaciones activas */
    private const val NOTIFICATION_BASE_ID = 2000

    /** Job de la corrutina en segundo plano que controla la periodicidad */
    private var periodicJob: Job? = null

    /** Índice rotativo para alternar los mensajes motivacionales */
    private var reminderIndex = 0

    /** Lista de tips motivacionales en Español */
    private val REMINDER_TIPS_ES = listOf(
        "🐙 ¡Hora de hidratarte! Toma un vaso de agua para cuidar a tu pulpo.",
        "⚡ Mantén tu racha: completa al menos un microhábito en HabitOcto hoy.",
        "✨ Tu mascota te espera en el Santuario para entrenar y subir de nivel.",
        "📖 ¿Listo para leer una página o meditar 5 minutos? ¡Cada paso cuenta!",
        "🏆 Revisa el Ranking de amigos con código #XXX y sube de posición."
    )

    /** Lista de tips motivacionales en Inglés */
    private val REMINDER_TIPS_EN = listOf(
        "🐙 Stay hydrated! Drink a glass of water to care for your Octo.",
        "⚡ Keep your streak alive: complete at least one micro-habit today.",
        "✨ Your pet is waiting in the Sanctuary to train and level up.",
        "📖 Ready to read a page or meditate for 5 min? Every step counts!",
        "🏆 Check the #XXX Friend Ranking and climb the leaderboard."
    )

    /**
     * Crea el canal de notificación requerido a partir de Android 8.0 (API 26).
     *
     * - **¿Para qué sirve?** Registrar la categoría del canal en Android para que el sistema permita mostrar notificaciones.
     * - **¿Cómo funciona?** Instancia un `NotificationChannel` con importancia por defecto, luces y vibración.
     * - **¿Qué pasa si lo quitamos?** En Android 8.0 o superior, las notificaciones se descartarían silenciosamente sin mostrarse.
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
     * Comprueba si la aplicación tiene autorización para enviar notificaciones (Android 13+ / API 33+).
     *
     * - **¿Para qué sirve?** Evita lanzar excepciones de seguridad al intentar publicar en la barra de estado.
     * - **¿Cómo funciona?** Evalúa `checkSelfPermission` contra `POST_NOTIFICATIONS` en API 33+, o `areNotificationsEnabled()` en versiones previas.
     * - **¿Qué pasa si lo quitamos?** La app arrojaría `SecurityException` en dispositivos con Android 13 o superior.
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
     * Envía una notificación real a la barra de estado de Android si cuenta con los permisos requeridos.
     *
     * - **¿Para qué sirve?** Publica de forma inmediata un recordatorio motivacional interactivo.
     * - **¿Cómo funciona?**
     *   1. Garantiza la existencia del canal.
     *   2. Construye un `PendingIntent` con flags de inmutabilidad (`FLAG_IMMUTABLE`) que reabre `MainActivity`.
     *   3. Asigna icono, título, estilo expandible (`BigTextStyle`) y auto-cancelación al ser pulsado.
     *   4. Despacha la notificación mediante `NotificationManagerCompat.notify()`.
     * - **¿Qué pasa si lo quitamos?** Los botones de prueba y los temporizadores automáticos no podrían alertar al usuario.
     */
    fun sendReminder(
        context: Context,
        title: String? = null,
        message: String? = null,
        isEnglish: Boolean = false
    ): Boolean {
        // Garantizar canal en Android O+
        createNotificationChannel(context)

        // Verificar permisos en tiempo de ejecución
        if (!hasNotificationPermission(context)) {
            return false
        }

        // Selección del tip motivacional rotativo según el idioma configurado
        val tips = if (isEnglish) REMINDER_TIPS_EN else REMINDER_TIPS_ES
        val chosenMessage = message ?: tips[reminderIndex % tips.size]
        val chosenTitle = title ?: if (isEnglish) "🐙 HabitOcto Reminder" else "🐙 Recordatorio de HabitOcto"
        reminderIndex += 1

        // Intent seguro para retornar a la actividad principal al tocar la alerta
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // Construcción de la notificación compatible
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
     * Inicia el ciclo periódico de recordatorios en segundo plano.
     *
     * - **¿Para qué sirve?** Mantener activas alertas automáticas regulares sin requerir servicios en primer plano pesados.
     * - **¿Cómo funciona?** Lanza un bucle de corrutina vigilando `isActive`, aplicando un `delay` equivalente al intervalo en minutos y llamando a `sendReminder`.
     * - **¿Qué pasa si lo quitamos?** Solo existirían recordatorios manuales al presionar el botón de prueba en Perfil.
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
                // Espera el intervalo configurado (por defecto 20 minutos)
                delay(intervalMinutes * 60_000L)
                sendReminder(context, isEnglish = isEnglish)
            }
        }
    }

    /**
     * Detiene el ciclo periódico si el usuario desactiva las notificaciones o la app se destruye.
     *
     * - **¿Para qué sirve?** Liberar los recursos de memoria y ciclos de CPU asociados al temporizador de corrutinas.
     * - **¿Cómo funciona?** Cancela el `Job` de la corrutina activa y lo anula.
     * - **¿Qué pasa si lo quitamos?** El temporizador podría seguir consumiendo ciclos innecesarios si la sesión finaliza.
     */
    fun stopPeriodicReminders() {
        periodicJob?.cancel()
        periodicJob = null
    }
}
