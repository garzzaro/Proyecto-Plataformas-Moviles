package com.example.ui

import android.widget.Toast
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.HabitImage
import com.example.TopNavigationBarSection
import com.example.model.FeedPostItem
import com.example.model.OriginalHabitData
import com.example.ui.components.CardWhite
import com.example.ui.components.ScreenBackground
import com.example.ui.components.TextDark
import com.example.ui.components.TextGray
import com.example.ui.components.getCategoryBgColor
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.BorderGrayLight
import com.example.ui.theme.LikePink
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryTeal

/**
 * Lista inicial de publicaciones: exactamente 2 con imagen y 2 únicamente de texto.
 */
val initialFeedPosts = OriginalHabitData.posts

/**
 * FeedScreen muestra la comunidad de usuarios compartiendo logros, fotos de hábitos y motivación.
 *
 * - **¿Para qué sirve?** Fomenta la adherencia mediante el componente social y la gamificación en equipo.
 * - **¿Cómo funciona?** Utiliza un `LazyColumn` que renderiza la barra superior, un creador de publicaciones
 *   con opción de adjuntar imagen y tarjetas de posts con fotos y reacciones.
 * - **¿Qué pasa si lo quitamos?** La segunda pestaña de la barra de navegación no tendría contenido visible.
 */
@Composable
fun FeedScreen(
    posts: List<FeedPostItem>,
    onToggleLike: (String) -> Unit,
    onPublishPost: (String, String, String?) -> Unit,
    modifier: Modifier = Modifier,
    publicationVersion: Int = 0,
    isSaving: Boolean = false
) {
    // Contexto de Android para emitir notificaciones visuales emergentes
    val context = LocalContext.current

    // Estado reactivo del texto del nuevo post
    var newPostContent by rememberSaveable { mutableStateOf("") }

    // Estado reactivo de la categoría elegida para el post
    var selectedCategoryForPost by rememberSaveable { mutableStateOf("General") }

    // Estado reactivo que contiene la URL de la imagen adjunta opcional
    var attachedImageUrl by rememberSaveable { mutableStateOf<String?>(null) }

    // Opciones de categorías disponibles para clasificar las publicaciones del feed
    val categories = listOf("General", "Ejercicio", "Hidratación", "Mindfulness", "Productividad", "Nutrición")

    var submitted by rememberSaveable { mutableStateOf(false) }
    var lastPublicationVersion by rememberSaveable { mutableStateOf(publicationVersion) }
    // El borrador se limpia después de confirmar el guardado, no al pulsar Publicar.
    LaunchedEffect(publicationVersion) {
        if (publicationVersion != lastPublicationVersion && submitted) {
            newPostContent = ""
            attachedImageUrl = null
            submitted = false
        }
        lastPublicationVersion = publicationVersion
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                // Conserva el acceso a la imagen después de cerrar y reabrir la app.
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                attachedImageUrl = uri.toString()
            } catch (_: SecurityException) {
                Toast.makeText(context, "No se pudo conservar el permiso de la imagen. Selecciona otra.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Columna con reciclaje dinámico eficiente para desplazamiento vertical suave
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera superior compartida de la aplicación
        item {
            TopNavigationBarSection()
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
                    // Título de la tarjeta de creación
                    Text(
                        text = "Comparte tu logro de hoy",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Campo de entrada de texto para el mensaje de la publicación
                    OutlinedTextField(
                        enabled = !isSaving,
                        value = newPostContent,
                        onValueChange = { newPostContent = it },
                        placeholder = { Text("¿Qué hábito completaste hoy?", color = TextGray) },
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

                    // Vista previa de la foto seleccionada con botón para retirarla
                    if (attachedImageUrl != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            HabitImage(
                                model = attachedImageUrl,
                                contentDescription = "Imagen adjunta",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { attachedImageUrl = null },
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
                    }

                    // Selector horizontal de la categoría que corresponde a la publicación
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { cat ->
                            val isSelected = selectedCategoryForPost == cat
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

                    // Fila con botones para adjuntar foto y enviar publicación
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botón secundario para simular la carga de foto desde la galería
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
                                text = if (attachedImageUrl == null) "Añadir Foto" else "Cambiar Foto",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Botón de confirmación para agregar el nuevo post a la lista
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
                                text = "Publicar",
                                color = TextDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Título de la sección social de la comunidad
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tu Feed",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = "${posts.size} publicaciones",
                    fontSize = 13.sp,
                    color = TextGray
                )
            }
        }

        item {
            Text("Publicaciones guardadas en este dispositivo. La comunidad se conectará al incorporar un servidor.",
                modifier = Modifier.padding(horizontal = 20.dp), color = TextGray, fontSize = 12.sp)
        }
        if (posts.isEmpty()) {
            item { Text("Comparte tu primer logro para comenzar.", modifier = Modifier.padding(20.dp), color = TextGray) }
        }

        // Renderizado dinámico de las publicaciones del feed
        items(items = posts, key = { it.id }) { post ->
            FeedPostCard(
                post = post,
                onLikeClick = { onToggleLike(post.id) },
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
 * - **¿Para qué sirve?** Presenta la información del autor, contenido, imagen y barra de interacciones.
 * - **¿Cómo funciona?** Evalúa si la publicación contiene una fotografía para dibujarla con Coil, junto a la racha y botones de me gusta.
 * - **¿Qué pasa si lo quitamos?** Las publicaciones del feed no tendrían representación visual en pantalla.
 */
@Composable
fun FeedPostCard(
    post: FeedPostItem,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabecera: Avatar del usuario, nombre, hábito completado y tiempo transcurrido
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
                    Text(
                        text = post.authorName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "${post.habitTitle} • ${post.timeAgo}",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }

                // Burbuja decorativa con el icono de la categoría
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(getCategoryBgColor(post.habitCategory), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(post.habitCategory),
                        contentDescription = post.habitCategory,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
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
                        text = "Racha de ${post.streakCount} días",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Fila de interacciones: Me gusta, comentar y compartir
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón interactivo de me gusta con contador dinámico
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLikeClick() }
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

                // Botón para acceder a comentarios
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comentar",
                        tint = TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Comentar",
                        fontSize = 13.sp,
                        color = TextGray
                    )
                }

                // Botón para compartir la publicación externamente
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Compartir",
                        tint = TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
