package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.HabitItem
import com.example.model.TrackingSummary
import com.example.ui.theme.BadgeOrange
import com.example.ui.theme.DarkTealBackground
import com.example.ui.theme.DeepDarkTeal
import com.example.ui.theme.LightTealAccent
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGrayDark
import com.example.ui.theme.TextGrayLight
import com.example.ui.theme.TextLight
import com.example.ui.theme.isAppInDarkTheme

/**
 * Representa una notificación inteligente dentro del Centro de Notificaciones de HabitOcto.
 */
data class AppNotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timeLabel: String,
    val iconEmoji: String,
    val targetTab: Int,
    val isUnread: Boolean = true
)

/**
 * Generador dinámico de notificaciones reales basadas en el progreso diario, micro-hábitos,
 * estado de la mascota, Modo Enfoque, Feed de 7 días y Ranking de amigos.
 */
object NotificationGenerator {

    fun buildNotifications(
        habits: List<HabitItem>,
        summary: TrackingSummary,
        friendsCount: Int,
        postsCount: Int,
        isEnglish: Boolean = false
    ): List<AppNotificationItem> {
        val list = mutableListOf<AppNotificationItem>()

        // 1. Micro-hábitos pendientes hoy (targetTab = 0 -> Tracker)
        val microHabits = habits.filter { it.isMicroHabit }
        val pendingMicro = microHabits.filter { !it.isCompleted }
        val pendingAll = habits.filter { !it.isCompleted }

        if (pendingMicro.isNotEmpty()) {
            val sampleTitle = pendingMicro.first().title
            list += AppNotificationItem(
                id = "notif_micro_pending",
                title = if (isEnglish) {
                    "${pendingMicro.size} micro-habit(s) pending today"
                } else {
                    "Tienes ${pendingMicro.size} micro-hábito(s) pendiente(s) hoy"
                },
                message = if (isEnglish) {
                    "Next up: \"$sampleTitle\". Complete your 2-min micro-habits to protect your daily streak!"
                } else {
                    "Siguiente: \"$sampleTitle\". ¡Completa tus micro-hábitos de 2 min para impulsar tu racha hoy!"
                },
                timeLabel = if (isEnglish) "Now • Tracker" else "Ahora • Tracker",
                iconEmoji = "⚡",
                targetTab = 0,
                isUnread = true
            )
        } else {
            val completedCount = summary.completedToday
            val scheduledCount = maxOf(1, summary.scheduledToday)
            list += AppNotificationItem(
                id = "notif_micro_done",
                title = if (isEnglish) {
                    "Micro-habits up to date ($completedCount/$scheduledCount today)"
                } else {
                    "Micro-hábitos al día ($completedCount/$scheduledCount hoy)"
                },
                message = if (pendingAll.isNotEmpty()) {
                    if (isEnglish) {
                        "Great job on micro-habits! You still have ${pendingAll.size} main goal(s) left today."
                    } else {
                        "¡Excelente con tus micro-hábitos! Aún tienes ${pendingAll.size} hábito(s) por completar hoy."
                    }
                } else {
                    if (isEnglish) {
                        "100% of today's habits completed! Octo is celebrating your discipline."
                    } else {
                        "¡100% de tus hábitos de hoy completados! Octo está celebrando tu constancia."
                    }
                },
                timeLabel = if (isEnglish) "Today • Tracker" else "Hoy • Tracker",
                iconEmoji = "✅",
                targetTab = 0,
                isUnread = pendingAll.isNotEmpty()
            )
        }

        // 2. Estado de Racha y Mascota (targetTab = 2 -> Mascota)
        val streakDays = summary.microStreak
        val petEnergy = (45 + summary.todayPercent / 2 + minOf(20, streakDays * 4)).coerceIn(30, 100)
        val petMoodTextEs = when {
            petEnergy >= 85 -> "radiante y con energía al $petEnergy%"
            petEnergy >= 60 -> "feliz con $petEnergy% de energía"
            else -> "esperando que completes un hábito ($petEnergy% energía)"
        }
        val petMoodTextEn = when {
            petEnergy >= 85 -> "radiant at $petEnergy% energy"
            petEnergy >= 60 -> "happy with $petEnergy% energy"
            else -> "waiting for a habit completion ($petEnergy% energy)"
        }

        list += AppNotificationItem(
            id = "notif_streak_pet",
            title = if (isEnglish) {
                "Current Streak: $streakDays day(s) 🔥 • Pet Sanctuary"
            } else {
                "Racha actual: $streakDays día(s) 🔥 • Santuario de Mascota"
            },
            message = if (isEnglish) {
                "Your companion is $petMoodTextEn. Tap to visit the Sanctuary and check your level rewards!"
            } else {
                "Tu mascota está $petMoodTextEs. ¡Toca para visitar el Santuario y ver sus recompensas!"
            },
            timeLabel = if (isEnglish) "Live • Pet" else "En vivo • Mascota",
            iconEmoji = "🐙",
            targetTab = 2,
            isUnread = true
        )

        // 3. Modo Enfoque con bloqueo disponible en el Tracker (targetTab = 0 -> Tracker)
        list += AppNotificationItem(
            id = "notif_focus_mode",
            title = if (isEnglish) {
                "Focus Mode with Distraction Lock available"
            } else {
                "Modo Enfoque con bloqueo disponible"
            },
            message = if (isEnglish) {
                "Start a timed Focus Session in the Tracker with in-app distraction lock to finish your habits faster."
            } else {
                "Activa el temporizador de Modo Enfoque con bloqueo en el Tracker para concentrarte sin distracciones."
            },
            timeLabel = if (isEnglish) "Tip • Tracker" else "Consejo • Tracker",
            iconEmoji = "🎯",
            targetTab = 0,
            isUnread = true
        )

        // 4. Feed de 7 días y Ranking #Amigos (targetTab = 1 -> Feed o 2 -> Ranking)
        val safePosts = maxOf(0, postsCount)
        val safeFriends = maxOf(1, friendsCount)
        list += AppNotificationItem(
            id = "notif_feed_7days",
            title = if (isEnglish) {
                "Community Feed ($safePosts active posts • 7-day window)"
            } else {
                "Feed Social ($safePosts publicaciones activas • máx. 7 días)"
            },
            message = if (isEnglish) {
                "Posts stay active for up to 7 days and you have $safeFriends friend(s) in the #Friends Ranking. Share an optimized photo!"
            } else {
                "Las publicaciones duran máximo 7 días y tienes $safeFriends amigo(s) en el Ranking #Amigos. ¡Comparte tu logro con foto optimizada!"
            },
            timeLabel = if (isEnglish) "7d Feed • Social" else "Feed 7d • Comunidad",
            iconEmoji = "📸",
            targetTab = 1,
            isUnread = true
        )

        return list
    }
}

/**
 * Diálogo modal del Centro de Notificaciones Inteligentes (botón de campana superior derecha).
 */
@Composable
fun NotificationsCenterDialog(
    notifications: List<AppNotificationItem>,
    isEnglish: Boolean = false,
    onDismiss: () -> Unit,
    onSelectNotification: (AppNotificationItem) -> Unit,
    onMarkAllRead: () -> Unit
) {
    val isDark = isAppInDarkTheme
    val unreadCount = notifications.count { it.isUnread }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 28.dp),
            shape = RoundedCornerShape(28.dp),
            color = if (isDark) Color(0xFF121824) else Color(0xFFF8FAFC),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Cabecera con gradiente Teal y contador de no leídas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = if (isDark) {
                                    listOf(DeepDarkTeal, DarkTealBackground)
                                } else {
                                    listOf(PrimaryTeal, DarkTealBackground)
                                }
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(Color.White.copy(alpha = 0.16f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = if (isEnglish) "Notifications" else "Notificaciones",
                                    tint = SecondaryTeal,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (isEnglish) "Notifications" else "Notificaciones",
                                        color = Color.White,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    if (unreadCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .background(BadgeOrange, RoundedCornerShape(50))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isEnglish) "$unreadCount new" else "$unreadCount nuevas",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (isEnglish) {
                                        "Tap any alert to jump directly to its section"
                                    } else {
                                        "Toca una alerta para ir directo a su sección"
                                    },
                                    color = Color.White.copy(alpha = 0.82f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isEnglish) "Close" else "Cerrar",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Barra de estado superior y botón "Marcar todas como leídas" / "Mark all as read"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (unreadCount > 0) {
                            if (isEnglish) "$unreadCount unread notification(s)" else "$unreadCount notificación(es) sin leer"
                        } else {
                            if (isEnglish) "No pending notifications" else "Sin notificaciones pendientes"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (unreadCount > 0) PrimaryTeal else (if (isDark) TextGrayDark else TextGrayLight)
                    )

                    OutlinedButton(
                        onClick = onMarkAllRead,
                        enabled = unreadCount > 0,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (unreadCount > 0) PrimaryTeal else (if (isDark) TextGrayDark else TextGrayLight)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEnglish) "Mark all as read" else "Marcar todas como leídas",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Lista de tarjetas clicables
                if (notifications.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = if (isEnglish) "No pending notifications" else "Sin notificaciones pendientes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isDark) TextLight else TextDark
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(notifications, key = { it.id }) { item ->
                            NotificationCardItem(
                                item = item,
                                isDark = isDark,
                                onClick = {
                                    onSelectNotification(item)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                // Botón inferior de Cerrar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                    ) {
                        Text(
                            text = if (isEnglish) "Close" else "Cerrar",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCardItem(
    item: AppNotificationItem,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val cardBg = when {
        item.isUnread && isDark -> Color(0xFF0E2A26)
        item.isUnread && !isDark -> Color(0xFFE6F4F1)
        isDark -> Color(0xFF1E293B)
        else -> Color.White
    }
    val borderColor = if (item.isUnread) {
        SecondaryTeal.copy(alpha = 0.65f)
    } else {
        Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isUnread) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isDark) Color(0xFF134E4A) else Color(0xFFCCFBF1),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.iconEmoji,
                    fontSize = 20.sp
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = if (isDark) TextLight else TextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (item.isUnread) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(BadgeOrange, CircleShape)
                        )
                    }
                }

                Text(
                    text = item.message,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = if (isDark) LightTealAccent.copy(alpha = 0.9f) else Color(0xFF334155)
                )

                Text(
                    text = item.timeLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal
                )
            }

            Icon(
                imageVector = Icons.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = PrimaryTeal,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
