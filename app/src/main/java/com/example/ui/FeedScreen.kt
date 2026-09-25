package com.example.ui

import android.widget.Toast
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
import coil.compose.AsyncImage
import com.example.R
import com.example.TopNavigationBarSection
import com.example.model.FeedPostItem
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
val initialFeedPosts = listOf(
    FeedPostItem(
        id = "p1",
        authorName = "Sofia Morales",
        authorAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
        habitTitle = "Plancha 1 Min",
        habitCategory = "Ejercicio",
        content = "¡Completé mi micro hábito antes del desayuno! Día 14 consecutivo sin fallar 💪",
        postImageUrl = "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=800", // Publicación 1 con foto
        timeAgo = "Hace 15 min",
        likesCount = 12,
        isLiked = false,
        streakCount = 14
    ),
    FeedPostItem(
        id = "p2",
        authorName = "Carlos Mendoza",
        authorAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200",
        habitTitle = "Beber Agua",
        habitCategory = "Hidratación",
        content = "Ya alcancé 6 de los 8 vasos recomendados. ¡Mantenerse bien hidratado durante el estudio cambia totalmente la concentración! 💧",
        postImageUrl = null, // Publicación 2 solo texto
        timeAgo = "Hace 1 hora",
        likesCount = 8,
        isLiked = true,
        streakCount = 7
    ),
    FeedPostItem(
        id = "p3",
        authorName = "Lucia Ramos",
        authorAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
        habitTitle = "Meditación",
        habitCategory = "Mindfulness",
        content = "10 minutos de respiración consciente en el parque para empezar el día con paz mental 🧘‍♀️✨",
        postImageUrl = "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=800", // Publicación 3 con foto
        timeAgo = "Hace 3 horas",
        likesCount = 19,
        isLiked = false,
        streakCount = 21
    ),
    FeedPostItem(
        id = "p4",
        authorName = "Diego Castillo",
        authorAvatarUrl = "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=200",
        habitTitle = "Estudiar Código",
        habitCategory = "Productividad",
        content = "Terminé la práctica completa de Compose y Jetpack en Android Studio con Kotlin. ¡La consistencia diaria en pequeños bloques de 45 minutos rinde increíbles frutos!",
        postImageUrl = null, // Publicación 4 solo texto
        timeAgo = "Hace 5 horas",
        likesCount = 25,
        isLiked = true,
        streakCount = 9
    )
)

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
    modifier: Modifier = Modifier
) {
    // Contexto de Android para emitir notificaciones visuales emergentes
    val context = LocalContext.current

    // Estado reactivo del texto del nuevo post
    var newPostContent by remember { mutableStateOf("") }

    // Estado reactivo de la categoría elegida para el post
    var selectedCategoryForPost by remember { mutableStateOf("General") }

    // Estado reactivo que contiene la URL de la imagen adjunta opcional
    var attachedImageUrl by remember { mutableStateOf<String?>(null) }

    // Opciones de categorías disponibles para clasificar las publicaciones del feed
    val categories = listOf("General", "Ejercicio", "Hidratación", "Mindfulness", "Productividad", "Nutrición")

    // Colección de fotografías de prueba para simular la selección de imagen de galería
    val sampleGalleryImages = listOf(
        "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=800",
        "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=800",
        "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=800",
        "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=800"
    )

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
                            AsyncImage(
                                model = attachedImageUrl,
                                placeholder = painterResource(id = R.drawable.ic_launcher_background),
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
                            onClick = {
                                attachedImageUrl = sampleGalleryImages.random()
                                Toast.makeText(context, "Imagen seleccionada para adjuntar", Toast.LENGTH_SHORT).show()
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
                                    newPostContent = ""
                                    attachedImageUrl = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                            shape = RoundedCornerShape(20.dp),
                            enabled = newPostContent.isNotBlank() || attachedImageUrl != null
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
                    text = "Comunidad HabitOcto",
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
                AsyncImage(
                    model = post.authorAvatarUrl,
                    placeholder = painterResource(id = R.drawable.ic_launcher_background),
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
                AsyncImage(
                    model = post.postImageUrl,
                    placeholder = painterResource(id = R.drawable.ic_launcher_background),
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
