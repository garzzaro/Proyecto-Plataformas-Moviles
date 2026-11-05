package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.TopNavigationBarSection
import com.example.model.FeedCommentItem
import com.example.model.FeedPostItem
import com.example.model.ImageOptimizer
import com.example.model.OriginalHabitData
import com.example.model.UserEntity
import com.example.ui.components.CardWhite
import com.example.ui.components.HabitImage
import com.example.ui.components.ScreenBackground
import com.example.ui.components.TextDark
import com.example.ui.components.TextGray
import com.example.ui.components.getCategoryBgColor
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.BorderGrayLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.LikePink
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.SuccessGreen

/**
 * Lista inicial de publicaciones: exactamente 2 con imagen y 2 únicamente de texto.
 */
val initialFeedPosts = OriginalHabitData.posts

/**
 * FeedScreen muestra la comunidad de usuarios compartiendo logros, fotos optimizadas de hábitos y motivación.
 *
 * - **¿Para qué sirve?** Fomenta la adherencia mediante el componente social con retención automática de 7 días,
 *   optimización de imágenes al subir fotos, comentarios en vivo y borrado exclusivo de "Mis Publicaciones".
 * - **¿Cómo funciona?** Utiliza un `LazyColumn` que renderiza la barra superior, un creador de publicaciones
 *   con módulo de compresión `ImageOptimizer`, filtros ("Todas (Últimos 7 días)" vs "Mis Publicaciones") y tarjetas interactivas.
 * - **¿Qué pasa si lo quitamos?** La segunda pestaña de la barra de navegación no tendría contenido visible.
 */
@Composable
fun FeedScreen(
    posts: List<FeedPostItem>,
    onToggleLike: (String) -> Unit,
    onPublishPost: (String, String, String?) -> Unit,
    modifier: Modifier = Modifier,
    publicationVersion: Int = 0,
    isSaving: Boolean = false,
    user: UserEntity? = null,
    level: Int = 1,
    onProfileClick: () -> Unit = {},
    onDeleteOwnPost: (String) -> Unit = {},
    onAddComment: (postId: String, commentText: String) -> Unit = { _, _ -> },
    isEnglish: Boolean = false
) {
    val context = LocalContext.current

    // Estado reactivo del texto del nuevo post
    var newPostContent by rememberSaveable { mutableStateOf("") }

    // Estado reactivo de la categoría elegida para el post
    var selectedCategoryForPost by rememberSaveable { mutableStateOf("General") }

    // Estado reactivo que contiene la URL de la imagen adjunta optimizada
    var attachedImageUrl by rememberSaveable { mutableStateOf<String?>(null) }

    // Etiqueta informativa del ahorro logrado por ImageOptimizer
    var attachedImageSizeLabel by rememberSaveable { mutableStateOf<String?>(null) }

    // Filtro principal: false = Todas (Últimos 7 días), true = Solo Mis Publicaciones
    var showOnlyOwnPosts by rememberSaveable { mutableStateOf(false) }

    // Filtro opcional por categoría al tocar el chip de categoría de una tarjeta
    var activeCategoryFilter by rememberSaveable { mutableStateOf<String?>(null) }

    // Opciones de categorías disponibles para clasificar las publicaciones del feed
    val categories = listOf("General", "Ejercicio", "Hidratación", "Mindfulness", "Productividad", "Nutrición")

    var submitted by rememberSaveable { mutableStateOf(false) }
    var lastPublicationVersion by rememberSaveable { mutableStateOf(publicationVersion) }

    // El borrador se limpia después de confirmar el guardado, no al pulsar Publicar.
    LaunchedEffect(publicationVersion) {
        if (publicationVersion != lastPublicationVersion && submitted) {
            newPostContent = ""
            attachedImageUrl = null
            attachedImageSizeLabel = null
            submitted = false
        }
        lastPublicationVersion = publicationVersion
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // Algunos proveedores no requieren persistable permission si optimizamos a archivo interno
            }
            val result = ImageOptimizer.optimizeImageFromUri(
                context = context,
                sourceUri = uri,
                forAvatar = false
            )
            attachedImageUrl = result.optimizedUri
            attachedImageSizeLabel = ImageOptimizer.formatSizeLabel(result, isEnglish)
        }
    }

    // Filtrado automático: máximo 7 días de vida (daysRemaining > 0), filtro "Mis Publicaciones" y filtro de categoría
    val visiblePosts = remember(posts, showOnlyOwnPosts, activeCategoryFilter, user?.displayName) {
        posts
            .filter { it.daysRemaining > 0 }
            .filter { post ->
                if (!showOnlyOwnPosts) true
                else post.isOwnPost || (user != null && post.authorName.equals(user.displayName, ignoreCase = true))
            }
            .filter { post ->
                activeCategoryFilter == null || post.habitCategory.equals(activeCategoryFilter, ignoreCase = true)
            }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera superior compartida de la aplicación
        item {
            TopNavigationBarSection(
                user = user,
                level = level,
                onProfileClick = onProfileClick
            )
        }

        // Tarjeta para redactar y publicar una nueva historia en el feed
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEnglish) "Share today's achievement" else "Comparte tu logro de hoy",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = PrimaryTeal.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (isEnglish) "⏳ Active for 7 days" else "⏳ Dura 7 días",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryTeal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Campo de entrada de texto para el mensaje de la publicación
                    OutlinedTextField(
                        enabled = !isSaving,
                        value = newPostContent,
                        onValueChange = { newPostContent = it },
                        placeholder = {
                            Text(
                                text = if (isEnglish) "What habit did you complete today?" else "¿Qué hábito completaste hoy?",
                                color = TextGray
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderGrayLight,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        ),
                        maxLines = 3
                    )

                    // Vista previa de la foto seleccionada y optimizada con insignia verde de ahorro
                    if (attachedImageUrl != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                HabitImage(
                                    model = attachedImageUrl,
                                    contentDescription = "Imagen adjunta optimizada",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = {
                                        attachedImageUrl = null
                                        attachedImageSizeLabel = null
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Quitar foto",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            attachedImageSizeLabel?.let { sizeLabel ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SuccessGreen.copy(alpha = 0.14f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = sizeLabel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SuccessGreen
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Campo de texto para escribir una categoría personalizada
                    OutlinedTextField(
                        enabled = !isSaving,
                        value = selectedCategoryForPost,
                        onValueChange = { selectedCategoryForPost = it },
                        placeholder = {
                            Text(
                                text = if (isEnglish) "Category (e.g. Study, Fitness, Gaming...)" else "Categoría (ej. Estudio, Fitness, Gaming...)",
                                color = TextGray,
                                fontSize = 12.sp
                            )
                        },
                        label = {
                            Text(
                                text = if (isEnglish) "Post Category" else "Categoría de la publicación",
                                fontSize = 11.sp
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderGrayLight,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        )
                    )

                    // Selector horizontal de la categoría que corresponde a la publicación
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { cat ->
                            val isSelected = selectedCategoryForPost.equals(cat, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedCategoryForPost = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else TextDark
                                )
                            }
                        }
                    }

                    // Fila con botones para adjuntar foto optimizada y enviar publicación
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            enabled = !isSaving,
                            onClick = {
                                photoPicker.launch(arrayOf("image/*"))
                            },
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AddPhotoAlternate,
                                contentDescription = "Subir foto",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (attachedImageUrl == null) {
                                    if (isEnglish) "Add Optimized Photo" else "Añadir Foto"
                                } else {
                                    if (isEnglish) "Change Photo" else "Cambiar Foto"
                                },
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                if (newPostContent.isNotBlank() || attachedImageUrl != null) {
                                    onPublishPost(newPostContent, selectedCategoryForPost, attachedImageUrl)
                                    submitted = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                            shape = RoundedCornerShape(20.dp),
                            enabled = !isSaving && (newPostContent.isNotBlank() || attachedImageUrl != null)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Send,
                                contentDescription = "Publicar",
                                modifier = Modifier.size(16.dp),
                                tint = TextDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEnglish) "Publish" else "Publicar",
                                color = TextDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Título de la sección social + Selector de filtro: "Todas (Últimos 7 días)" vs "Mis Publicaciones"
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "Your Feed (7-Day Retention)" else "Tu Feed",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = if (isEnglish) "${visiblePosts.size} posts" else "${visiblePosts.size} publicaciones",
                        fontSize = 13.sp,
                        color = TextGray
                    )
                }

                // Selector: Todas (Últimos 7 días) vs Mis Publicaciones
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardWhite, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val allSelected = !showOnlyOwnPosts
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (allSelected) PrimaryTeal else Color.Transparent)
                            .clickable { showOnlyOwnPosts = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEnglish) "All (Last 7 days)" else "Todas (Últimos 7 días)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (allSelected) Color.White else TextDark
                        )
                    }

                    val ownSelected = showOnlyOwnPosts
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (ownSelected) PrimaryTeal else Color.Transparent)
                            .clickable { showOnlyOwnPosts = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEnglish) "My Posts" else "Mis Publicaciones",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (ownSelected) Color.White else TextDark
                        )
                    }
                }

                // Si hay un filtro de categoría activo, permitir quitarlo con un toque
                if (activeCategoryFilter != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { activeCategoryFilter = null },
                            shape = RoundedCornerShape(50),
                            color = PrimaryTeal.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isEnglish) {
                                        "Category: $activeCategoryFilter ✕"
                                    } else {
                                        "Categoría: $activeCategoryFilter ✕"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryTeal
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = if (isEnglish) {
                    "Posts automatically expire after 7 days. You can delete your own posts at any time (deleting a post also removes its comments and likes)."
                } else {
                    "Las publicaciones duran máximo 7 días. Puedes borrar solo Tus Publicaciones (los comentarios y likes se eliminan junto a la publicación)."
                },
                modifier = Modifier.padding(horizontal = 20.dp),
                color = TextGray,
                fontSize = 12.sp
            )
        }

        if (visiblePosts.isEmpty()) {
            item {
                Text(
                    text = if (showOnlyOwnPosts) {
                        if (isEnglish) "You don't have active posts from the last 7 days." else "No tienes publicaciones propias activas en los últimos 7 días."
                    } else {
                        if (isEnglish) "Share your first achievement to get started." else "Comparte tu primer logro para comenzar."
                    },
                    modifier = Modifier.padding(20.dp),
                    color = TextGray
                )
            }
        }

        // Renderizado dinámico de las publicaciones del feed (máximo 7 días de antigüedad)
        items(items = visiblePosts, key = { it.id }) { post ->
            // La autoría se decide por authorId en el ViewModel; nunca por nombre (podría repetirse).
            FeedPostCard(
                post = post,
                onLikeClick = { onToggleLike(post.id) },
                onDeleteOwnPost = { onDeleteOwnPost(post.id) },
                onAddComment = { commentText -> onAddComment(post.id, commentText) },
                onCategoryClick = { category ->
                    activeCategoryFilter = if (activeCategoryFilter.equals(category, ignoreCase = true)) null else category
                },
                currentUserName = user?.displayName ?: if (isEnglish) "You" else "Tú",
                isEnglish = isEnglish,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // Espaciador final para evitar colisión con barras de navegación
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * FeedPostCard dibuja la tarjeta visual de una publicación individual en el feed.
 *
 * - Muestra la etiqueta de expiración (`⏳ Xd restantes`, máximo 7 días).
 * - Solo si `post.isOwnPost == true`, muestra el botón de papelera roja para eliminar la publicación
 *   junto con todos sus comentarios y likes tras confirmar en un `AlertDialog`.
 * - Incluye sección interactiva de comentarios y botón real de compartir (`Intent.ACTION_SEND`).
 */
@Composable
fun FeedPostCard(
    post: FeedPostItem,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteOwnPost: () -> Unit = {},
    onAddComment: (String) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    currentUserName: String = "Tú",
    isEnglish: Boolean = false
) {
    val context = LocalContext.current
    var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var showCommentsSection by rememberSaveable { mutableStateOf(false) }
    var commentDraft by rememberSaveable { mutableStateOf("") }
    var localAddedComments by remember(post.id) { mutableStateOf<List<FeedCommentItem>>(emptyList()) }

    val allComments = remember(post.comments, localAddedComments) {
        val existingIds = post.comments.map { it.id }.toSet()
        post.comments + localAddedComments.filter { it.id !in existingIds }
    }

    val safeDaysRemaining = post.daysRemaining.coerceIn(1, 7)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabecera: Avatar del usuario, nombre, hábito completado, expiración 7d, categoría interactiva y borrar propio
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HabitImage(
                    model = post.authorAvatarUrl,
                    contentDescription = "Avatar de ${post.authorName}",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = post.authorName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (post.isOwnPost) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PrimaryTeal.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (isEnglish) "You" else "Tú",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryTeal,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${post.habitTitle} • ${post.timeAgo}",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryTeal.copy(alpha = 0.12f),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        val remainingMillis = remember(post.createdAtMillis) {
                            val expiryMillis = post.createdAtMillis + 7L * 24L * 3600_000L
                            maxOf(0L, expiryMillis - System.currentTimeMillis())
                        }
                        val remDays = (remainingMillis / (24L * 3600_000L)).toInt()
                        val remHours = ((remainingMillis % (24L * 3600_000L)) / 3600_000L).toInt()
                        val durationText = if (isEnglish) {
                            if (remDays > 0) "⏳ ${remDays}d ${remHours}h left (7d life)" else "⏳ ${remHours}h left (expires soon)"
                        } else {
                            if (remDays > 0) "⏳ Le quedan: ${remDays}d y ${remHours}h" else "⏳ Le quedan: ${remHours}h"
                        }
                        Text(
                            text = durationText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryTeal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Chip compacto e interactivo con el nombre de la categoría que al tocarlo filtra el Feed
                Surface(
                    onClick = { onCategoryClick(post.habitCategory) },
                    shape = RoundedCornerShape(12.dp),
                    color = getCategoryBgColor(post.habitCategory)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(post.habitCategory),
                            contentDescription = post.habitCategory,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = post.habitCategory,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // SOLO si post.isOwnPost == true, botón de papelera roja para borrar la publicación propia
                if (post.isOwnPost) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Borrar mi publicación",
                            tint = DangerRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Contenido textual de la publicación
            if (post.content.isNotBlank()) {
                Text(
                    text = post.content,
                    fontSize = 14.sp,
                    color = TextDark,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Imagen ilustrativa si la publicación incluye foto
            if (post.postImageUrl != null) {
                HabitImage(
                    model = post.postImageUrl,
                    contentDescription = "Imagen del post",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Insignia de racha en días si el hábito tiene racha activa
            if (post.streakCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "🔥", fontSize = 14.sp)
                    Text(
                        text = if (isEnglish) {
                            "${post.streakCount}-day streak"
                        } else {
                            "Racha de ${post.streakCount} días"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Fila de interacciones: Me gusta, Comentar (con contador) y Compartir real
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón interactivo de me gusta con contador dinámico
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onLikeClick() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Me gusta",
                        tint = if (post.isLiked) LikePink else TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.likesCount}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (post.isLiked) LikePink else TextGray
                    )
                }

                // Botón interactivo para desplegar y agregar comentarios
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showCommentsSection = !showCommentsSection }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comentar",
                        tint = if (showCommentsSection) PrimaryTeal else TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) {
                            "Comment (${allComments.size})"
                        } else {
                            "Comentar (${allComments.size})"
                        },
                        fontSize = 13.sp,
                        fontWeight = if (showCommentsSection) FontWeight.Bold else FontWeight.Normal,
                        color = if (showCommentsSection) PrimaryTeal else TextGray
                    )
                }

                // Botón para compartir la publicación externamente con Intent.ACTION_SEND
                IconButton(
                    onClick = {
                        val shareText = buildString {
                            append("🐙 HabitOcto • ${post.authorName}\n")
                            append("🎯 ${post.habitTitle} (${post.habitCategory})\n")
                            if (post.content.isNotBlank()) {
                                append("\"${post.content}\"\n")
                            }
                            if (post.streakCount > 0) {
                                append("🔥 Racha de ${post.streakCount} días")
                            }
                        }
                        runCatching {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    sendIntent,
                                    if (isEnglish) "Share achievement" else "Compartir logro"
                                )
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Compartir",
                        tint = TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Sección desplegable de comentarios asociados a la publicación
            AnimatedVisibility(visible = showCommentsSection) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (allComments.isEmpty()) {
                        Text(
                            text = if (isEnglish) {
                                "No comments yet. Be the first to encourage ${post.authorName}!"
                            } else {
                                "Aún no hay comentarios. ¡Sé el primero en motivar a ${post.authorName}!"
                            },
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    } else {
                        allComments.forEach { comment ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardWhite, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = comment.authorName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryTeal
                                    )
                                    Text(
                                        text = comment.content,
                                        fontSize = 12.sp,
                                        color = TextDark
                                    )
                                }
                                Text(
                                    text = comment.timeAgo,
                                    fontSize = 10.sp,
                                    color = TextGray
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = commentDraft,
                            onValueChange = { commentDraft = it },
                            placeholder = {
                                Text(
                                    text = if (isEnglish) "Write a comment..." else "Escribe un comentario...",
                                    fontSize = 12.sp,
                                    color = TextGray
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                val clean = commentDraft.trim()
                                if (clean.isNotEmpty()) {
                                    localAddedComments = localAddedComments + FeedCommentItem(
                                        id = "local_${System.currentTimeMillis()}",
                                        postId = post.id,
                                        authorName = currentUserName,
                                        content = clean,
                                        timeAgo = if (isEnglish) "Just now" else "Ahora"
                                    )
                                    onAddComment(clean)
                                    commentDraft = ""
                                }
                            },
                            enabled = commentDraft.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Send,
                                contentDescription = "Enviar comentario",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmación para borrar publicación propia junto con sus comentarios y likes
    if (showDeleteConfirmDialog && post.isOwnPost) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = if (isEnglish) "Delete your post?" else "¿Eliminar tu publicación?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isEnglish) {
                        "Delete your post? The post will be permanently deleted along with all its comments (${allComments.size}) and likes (${post.likesCount})."
                    } else {
                        "¿Eliminar tu publicación? Se borrarán permanentemente la publicación junto con todos sus comentarios (${allComments.size}) y likes (${post.likesCount})."
                    },
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteOwnPost()
                        Toast.makeText(
                            context,
                            if (isEnglish) {
                                "Post, comments, and likes deleted"
                            } else {
                                "Publicación, comentarios y likes eliminados"
                            },
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text(
                        text = if (isEnglish) "Delete permanently" else "Eliminar",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(if (isEnglish) "Cancel" else "Cancelar")
                }
            }
        )
    }
}
