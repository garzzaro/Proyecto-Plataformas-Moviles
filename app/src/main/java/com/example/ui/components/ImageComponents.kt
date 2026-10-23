package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R

/**
 * HabitImage es un componente reutilizable para la carga optimizada de imágenes dinámicas.
 *
 * - **¿Para qué sirve?** Estandariza la carga de imágenes usando Coil incorporando efectos de
 *   transición suaves (crossfade), imagen de reserva durante la carga (placeholder) y fallback
 *   ante fallos de conexión (error).
 * - **¿Cómo funciona?** Utiliza `ImageRequest.Builder` para configurar la memoria caché, animaciones
 *   y recursos de respaldo en caso de error.
 * - **¿Qué pasa si lo quitamos?** Las imágenes se cargarían abruptamente sin retroalimentación visual ni manejo de fallos.
 */
@Composable
fun HabitImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    @DrawableRes placeholderRes: Int = R.drawable.ic_launcher_background,
    @DrawableRes errorRes: Int = R.drawable.ic_launcher_background
) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(model)
            .crossfade(true)
            .placeholder(placeholderRes)
            .error(errorRes)
            .build(),
        placeholder = painterResource(id = placeholderRes),
        error = painterResource(id = errorRes),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}
