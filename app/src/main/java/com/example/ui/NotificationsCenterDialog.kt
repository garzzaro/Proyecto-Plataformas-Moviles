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
import com.example.model.FeedPostItem
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

/** Origen de la notificación: Sistema, Hábito o Mascota. */
enum class NotificationSender { SYSTEM, HABIT, PET }

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
    val isUnread: Boolean = true,
    val sender: NotificationSender = NotificationSender.SYSTEM
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
        isEnglish: Boolean = false,
        feedPosts: List<FeedPostItem> = emptyList(),
        currentUserName: String? = null
    ): List<AppNotificationItem> {
        val list = mutableListOf<AppNotificationItem>()

        // 0. Interacciones sociales en tus publicaciones del Feed (Comentarios y Likes recibidos)
        val cleanSelfName = currentUserName?.substringBefore("#")?.trim()?.lowercase()
        feedPosts
            .filter { it.isOwnPost }
            .forEach { ownPost ->
                ownPost.comments
                    .filter { comment ->
                        val authorClean = comment.authorName.substringBefore("#").trim().lowercase()
                        cleanSelfName == null || (authorClean != cleanSelfName && authorClean != "tú" && authorClean != "mi perfil")
                    }
                    .takeLast(3)
                    .reversed()
                    .forEach { comment ->
                        list += AppNotificationItem(
                            id = "notif_feed_comment_${ownPost.id}_${comment.id}",
                            title = if (isEnglish) {
                                "💬 ${comment.authorName} commented on your post"
                            } else {
                                "💬 ${comment.authorName} comentó tu publicación"
                            },
                            message = "\"${comment.content}\" · ${ownPost.habitTitle}",
                            timeLabel = if (isEnglish) "Feed • ${comment.timeAgo}" else "Feed • ${comment.timeAgo}",
                            iconEmoji = "💬",
                            targetTab = 1,
                            isUnread = true,
                            sender = NotificationSender.SYSTEM
                        )
                    }

                if (ownPost.likesCount > 0) {
                    list += AppNotificationItem(
                        id = "notif_feed_likes_${ownPost.id}_${ownPost.likesCount}",
                        title = if (isEnglish) {
                            "❤️ ${ownPost.likesCount} like(s) on your post"
                        } else {
                            "❤️ ¡Tienes ${ownPost.likesCount} Me Gusta en tu publicación!"
                        },
                        message = if (isEnglish) {
                            "Your achievement \"${ownPost.habitTitle}\" received reactions from your friends."
                        } else {
                            "Tu logro \"${ownPost.habitTitle}\" recibió reacciones en el Feed compartido."
                        },
                        timeLabel = if (isEnglish) "Feed • Likes" else "Feed • Me Gusta",
                        iconEmoji = "❤️",
                        targetTab = 1,
                        isUnread = true,
                        sender = NotificationSender.SYSTEM
                    )
                }
            }

        // 1. Micro-hábitos pendientes hoy (targetTab = 0 -> Tracker)
        val microHabits = habits.filter { it.isMicroHabit }
        val pendingMicro = microHabits.filter { !it.isCompleted }
        val pendingAll = habits.filter { !it.isCompleted }

        if (pendingMicro.isNotEmpty()) {
            val sampleTitle = pendingMicro.first().title
            list += AppNotificationItem(
                id = "notif_micro_pending",
                title = if (isEnglish) {
                    "🎯 Habit: ${pendingMicro.size} micro-habit(s) pending"
                } else {
                    "🎯 Hábito: ${pendingMicro.size} micro-hábito(s) pendiente(s)"
                },
                message = if (isEnglish) {
                    "Next up: \"$sampleTitle\". Take a proof photo and validate your steps to protect your streak!"
                } else {
                    "Siguiente: \"$sampleTitle\". ¡Toma foto de evidencia y valida tus pasos para subir tu racha!"
                },
                timeLabel = if (isEnglish) "Habit • Tracker" else "Hábito • Tracker",
                iconEmoji = "⚡",
                targetTab = 0,
                isUnread = true,
                sender = NotificationSender.HABIT
            )
        } else {
            val completedCount = summary.completedToday
            val scheduledCount = maxOf(1, summary.scheduledToday)
            list += AppNotificationItem(
                id = "notif_micro_done",
                title = if (isEnglish) {
                    "🎯 Habit: Daily goal ($completedCount/$scheduledCount)"
                } else {
                    "🎯 Hábito: Meta al día ($completedCount/$scheduledCount)"
                },
                message = if (pendingAll.isNotEmpty()) {
                    if (isEnglish) {
                        "Great job on micro-habits! You still have ${pendingAll.size} active habit(s) left."
                    } else {
                        "¡Excelente con tus micro-hábitos! Aún tienes ${pendingAll.size} hábito(s) activo(s) por completar."
                    }
                } else {
                    if (isEnglish) {
                        "100% of active habits completed! All your pending tasks are cleared."
                    } else {
                        "¡100% de tus hábitos completados! Ya limpiaste todos tus pendientes de hoy."
                    }
                },
                timeLabel = if (isEnglish) "Habit • Tracker" else "Hábito • Tracker",
                iconEmoji = "✅",
                targetTab = 0,
                isUnread = pendingAll.isNotEmpty(),
                sender = NotificationSender.HABIT
            )
        }

        // 2. Mascota: Racha, XP real por hábitos y Minijuego del Santuario (targetTab = 2 -> Mascota)
        val streakDays = maxOf(summary.overallStreak, summary.microStreak)
        val petEnergy = (45 + summary.todayPercent / 2 + minOf(20, streakDays * 4)).coerceIn(30, 100)
        val petMoodTextEs = when {
            petEnergy >= 85 -> "súper motivada ($petEnergy% energía)"
            petEnergy >= 60 -> "animada con $petEnergy% de energía"
            else -> "esperando que completes un hábito para ganar XP ($petEnergy%)"
        }
        val petMoodTextEn = when {
            petEnergy >= 85 -> "super motivated ($petEnergy% energy)"
            petEnergy >= 60 -> "happy with $petEnergy% energy"
            else -> "waiting for a habit completion to earn XP ($petEnergy%)"
        }

        list += AppNotificationItem(
            id = "notif_streak_pet",
            title = if (isEnglish) {
                "🐙 Pet: Streak $streakDays day(s) & Sanctuary"
            } else {
                "🐙 Mascota: Racha de $streakDays día(s) y Juego"
            },
            message = if (isEnglish) {
                "I'm $petMoodTextEn. Remember: XP only rises by completing habits, and happiness rises in the Sanctuary mini-game!"
            } else {
                "Estoy $petMoodTextEs. Recuerda: ¡subo de XP solo con tus hábitos y de felicidad con el minijuego del Santuario!"
            },
            timeLabel = if (isEnglish) "Pet • Sanctuary" else "Mascota • Santuario",
            iconEmoji = "🐙",
            targetTab = 2,
            isUnread = true,
            sender = NotificationSender.PET
        )

        // 3. Sistema: Seguridad del Modo Enfoque estricto (targetTab = 0 -> Tracker)
        list += AppNotificationItem(
            id = "notif_focus_mode",
            title = if (isEnglish) {
                "⚙️ System: Strict Focus Lock Ready"
            } else {
                "⚙️ Sistema: Modo Enfoque Estricto activo"
            },
            message = if (isEnglish) {
                "Screen Pinning + anti-exit lock keeps other apps blocked until you finish or verify with a photo."
            } else {
                "El anclaje de pantalla y escudo anti-salida bloquean otras apps hasta que termines o valides con foto."
            },
            timeLabel = if (isEnglish) "System • Security" else "Sistema • Seguridad",
            iconEmoji = "🛡️",
            targetTab = 0,
            isUnread = true,
            sender = NotificationSender.SYSTEM
        )

        // 4. Sistema / Comunidad: Feed Compartido entre Amigos (targetTab = 1 -> Feed)
        val safePosts = maxOf(0, postsCount)
        val safeFriends = maxOf(1, friendsCount)
        list += AppNotificationItem(
            id = "notif_feed_7days",
            title = if (isEnglish) {
                "⚙️ System: Shared Friends Feed ($safePosts posts)"
            } else {
                "⚙️ Sistema: Feed Compartido ($safePosts publicaciones)"
            },
            message = if (isEnglish) {
                "Cloud sync active across $safeFriends friend(s) with unique system #Tag and cloud avatars."
            } else {
                "Sincronización activa entre $safeFriends amigo(s) con #Tag único del sistema y fotos de perfil en la nube."
            },
            timeLabel = if (isEnglish) "System • Cloud Feed" else "Sistema • Nube Social",
            iconEmoji = "☁️",
            targetTab = 1,
            isUnread = true,
            sender = NotificationSender.SYSTEM
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

                val badgeBg = when (item.sender) {
                    NotificationSender.SYSTEM -> Color(0xFF0284C7).copy(alpha = 0.16f)
                    NotificationSender.HABIT -> Color(0xFF0F766E).copy(alpha = 0.16f)
                    NotificationSender.PET -> Color(0xFF9333EA).copy(alpha = 0.16f)
                }
                val badgeText = when (item.sender) {
                    NotificationSender.SYSTEM -> "⚙️ Sistema"
                    NotificationSender.HABIT -> "🎯 Hábito"
                    NotificationSender.PET -> "🐙 Mascota"
                }
                val badgeFg = when (item.sender) {
                    NotificationSender.SYSTEM -> if (isDark) Color(0xFF7DD3FC) else Color(0xFF0369A1)
                    NotificationSender.HABIT -> if (isDark) Color(0xFF5EEAD4) else Color(0xFF0F766E)
                    NotificationSender.PET -> if (isDark) Color(0xFFD8B4FE) else Color(0xFF7E22CE)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeFg,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = item.timeLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )
                }
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
