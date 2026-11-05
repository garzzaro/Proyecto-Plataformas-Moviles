package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.AvatarCatalog
import com.example.model.ImageAssetModel
import com.example.model.PetAccessory
import com.example.model.PetHabitat
import com.example.model.PetType
import com.example.model.UserEntity
import com.example.model.visualModel
import com.example.ui.theme.BadgeOrange
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryTeal

/**
 * HabitImage es un componente reutilizable para la carga optimizada de imágenes dinámicas.
 *
 * - **¿Para qué sirve?** Estandariza la carga de imágenes usando Coil incorporando efectos de
 *   transición suaves (crossfade), imagen de reserva durante la carga (placeholder) y fallback
 *   seguro a `R.drawable.ic_pulpo` ante fallos de conexión o URLs rotas.
 * - **¿Cómo funciona?** Utiliza `AvatarCatalog.sanitizeModel` e `ImageRequest.Builder` para
 *   descartar URLs vacías o rotas (como `pulpo.avif`) y mostrar siempre un recurso válido.
 * - **¿Qué pasa si lo quitamos?** Las imágenes se cargarían abruptamente o mostrarían fondos vacíos.
 */
@Composable
fun HabitImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    @DrawableRes placeholderRes: Int = R.drawable.ic_pulpo,
    @DrawableRes errorRes: Int = R.drawable.ic_pulpo
) {
    val context = LocalContext.current
    val resolvedFallback = if (model is ImageAssetModel) model.fallbackResId else errorRes
    val sanitizedData = AvatarCatalog.sanitizeModel(model, fallbackResId = resolvedFallback)

    if (sanitizedData is Int) {
        Image(
            painter = painterResource(id = sanitizedData),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(sanitizedData)
                .crossfade(true)
                .build(),
            placeholder = painterResource(id = placeholderRes),
            error = painterResource(id = resolvedFallback),
            fallback = painterResource(id = resolvedFallback),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

/**
 * Badge interactivo de la barra superior que muestra el Avatar del usuario actual,
 * su insignia de nivel (`LVL X`) y su código de amigo (`#XXX`), navegando al Perfil al tocarlo.
 */
@Composable
fun UserAvatarHeaderChip(
    user: UserEntity?,
    level: Int = 1,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val code = user?.friendCode?.takeIf { it.isNotBlank() && it != "000" } ?: "482"
    val avatarAsset = AvatarCatalog.resolveAvatar(user?.avatarUrl)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .border(1.dp, SecondaryTeal.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Código #XXX y Nivel
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "#$code",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Box(
                modifier = Modifier
                    .background(BadgeOrange, RoundedCornerShape(8.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "LVL $level",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Avatar circular
        HabitImage(
            model = avatarAsset,
            contentDescription = "Ir a Perfil (${user?.displayName ?: "Mi perfil"})",
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .border(1.5.dp, PrimaryTeal, CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * Ilustración vectorial en Canvas/Compose para cada tipo de mascota y accesorio,
 * combinando `R.drawable.ic_pulpo` para Octo y gráficos vectoriales personalizados para las demás mascotas.
 */
@Composable
fun PetVectorIllustration(
    petType: PetType,
    accessory: PetAccessory = PetAccessory.NONE,
    size: Dp = 135.dp,
    modifier: Modifier = Modifier
) {
    val visual = petType.visualModel()
    val primaryColor = Color(visual.primaryColorHex)
    val secondaryColor = Color(visual.secondaryColorHex)
    val auraColor = Color(visual.auraColorHex)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (petType == PetType.OCTO) {
            HabitImage(
                model = R.drawable.ic_pulpo,
                contentDescription = petType.displayName,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(3.dp, SecondaryTeal, CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Canvas(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(3.dp, SecondaryTeal, CircleShape)
            ) {
                val w = this.size.width
                val h = this.size.height

                // Fondo circular con gradiente radial del hábitat/mascota
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(auraColor.copy(alpha = 0.55f), primaryColor.copy(alpha = 0.9f)),
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = w * 0.55f
                    )
                )

                // Orejas / Cuernos / Branquias en vector según el tipo de mascota
                when (petType) {
                    PetType.DRAGON -> {
                        val leftHorn = Path().apply {
                            moveTo(w * 0.30f, h * 0.34f)
                            lineTo(w * 0.18f, h * 0.14f)
                            lineTo(w * 0.40f, h * 0.28f)
                            close()
                        }
                        val rightHorn = Path().apply {
                            moveTo(w * 0.70f, h * 0.34f)
                            lineTo(w * 0.82f, h * 0.14f)
                            lineTo(w * 0.60f, h * 0.28f)
                            close()
                        }
                        drawPath(leftHorn, color = secondaryColor)
                        drawPath(rightHorn, color = secondaryColor)
                    }
                    PetType.FOX -> {
                        val leftEar = Path().apply {
                            moveTo(w * 0.24f, h * 0.42f)
                            lineTo(w * 0.20f, h * 0.15f)
                            lineTo(w * 0.44f, h * 0.30f)
                            close()
                        }
                        val rightEar = Path().apply {
                            moveTo(w * 0.76f, h * 0.42f)
                            lineTo(w * 0.80f, h * 0.15f)
                            lineTo(w * 0.56f, h * 0.30f)
                            close()
                        }
                        drawPath(leftEar, color = primaryColor)
                        drawPath(rightEar, color = primaryColor)
                    }
                    PetType.PANDA -> {
                        drawCircle(
                            color = Color(0xFF263238),
                            radius = w * 0.12f,
                            center = Offset(w * 0.27f, h * 0.27f)
                        )
                        drawCircle(
                            color = Color(0xFF263238),
                            radius = w * 0.12f,
                            center = Offset(w * 0.73f, h * 0.27f)
                        )
                    }
                    else -> {
                        // Ajolote: branquias externas rosadas
                        drawRoundRect(
                            color = Color(0xFFFF4081),
                            topLeft = Offset(w * 0.10f, h * 0.34f),
                            size = Size(w * 0.16f, h * 0.06f),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                        drawRoundRect(
                            color = Color(0xFFFF4081),
                            topLeft = Offset(w * 0.74f, h * 0.34f),
                            size = Size(w * 0.16f, h * 0.06f),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                    }
                }

                // Cabeza / Cuerpo central
                drawCircle(
                    color = if (petType == PetType.PANDA) secondaryColor else primaryColor,
                    radius = w * 0.31f,
                    center = Offset(w * 0.5f, h * 0.54f)
                )

                // Ojos expresivos
                drawCircle(
                    color = Color(0xFF1E293B),
                    radius = w * 0.045f,
                    center = Offset(w * 0.40f, h * 0.50f)
                )
                drawCircle(
                    color = Color(0xFF1E293B),
                    radius = w * 0.045f,
                    center = Offset(w * 0.60f, h * 0.50f)
                )
                drawCircle(
                    color = Color.White,
                    radius = w * 0.016f,
                    center = Offset(w * 0.39f, h * 0.485f)
                )
                drawCircle(
                    color = Color.White,
                    radius = w * 0.016f,
                    center = Offset(w * 0.59f, h * 0.485f)
                )
            }

            // Emoji nítido superpuesto para reforzar identidad visual
            Text(
                text = petType.iconEmoji,
                fontSize = (size.value * 0.46f).sp
            )
        }

        // Accesorio superpuesto
        if (accessory != PetAccessory.NONE) {
            Text(
                text = accessory.emoji,
                fontSize = (size.value * 0.28f).sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-6).dp)
            )
        }
    }
}

/**
 * Decoración vectorial en Canvas para enriquecer el fondo de cada hábitat.
 */
@Composable
fun HabitatVectorDecorCanvas(
    habitat: PetHabitat,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (habitat) {
            PetHabitat.AQUARIUM -> {
                // Burbujas marinas
                drawCircle(Color.White.copy(alpha = 0.14f), radius = 18f, center = Offset(w * 0.15f, h * 0.25f), style = Stroke(width = 3f))
                drawCircle(Color.White.copy(alpha = 0.10f), radius = 12f, center = Offset(w * 0.82f, h * 0.35f), style = Stroke(width = 2.5f))
                drawCircle(Color.White.copy(alpha = 0.12f), radius = 24f, center = Offset(w * 0.88f, h * 0.70f), style = Stroke(width = 3f))
            }
            PetHabitat.ZEN_GARDEN -> {
                // Ondas de jardín zen
                drawCircle(Color.White.copy(alpha = 0.08f), radius = w * 0.35f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = 2f))
                drawCircle(Color.White.copy(alpha = 0.06f), radius = w * 0.45f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = 2f))
            }
            PetHabitat.COZY_ROOM -> {
                // Luces neon suaves
                drawRoundRect(
                    color = Color(0xFF00E5FF).copy(alpha = 0.12f),
                    topLeft = Offset(w * 0.08f, h * 0.12f),
                    size = Size(w * 0.84f, h * 0.76f),
                    cornerRadius = CornerRadius(32f, 32f),
                    style = Stroke(width = 3f)
                )
            }
            PetHabitat.SPACE_STATION -> {
                // Estrellas cósmicas
                drawCircle(Color.White.copy(alpha = 0.35f), radius = 4f, center = Offset(w * 0.18f, h * 0.20f))
                drawCircle(Color.White.copy(alpha = 0.28f), radius = 5f, center = Offset(w * 0.78f, h * 0.18f))
                drawCircle(Color.White.copy(alpha = 0.22f), radius = 3.5f, center = Offset(w * 0.85f, h * 0.65f))
                drawCircle(Color.White.copy(alpha = 0.30f), radius = 4.5f, center = Offset(w * 0.12f, h * 0.72f))
            }
        }
    }
}
