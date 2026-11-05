package com.example.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * ImageOptimizationResult contiene las métricas reales de compresión y escalado de una imagen.
 *
 * - **¿Para qué sirve?** Informa la URI local optimizada, el peso original vs optimizado,
 *   las dimensiones resultantes y el porcentaje de ahorro de almacenamiento/memoria.
 * - **¿Cómo funciona?** Se genera tras procesar un Uri o Bitmap en [ImageOptimizer].
 * - **¿Qué pasa si lo quitamos?** No tendríamos trazabilidad ni evidencia del ahorro de espacio
 *   al subir fotos de perfil o imágenes al Feed.
 */
data class ImageOptimizationResult(
    val optimizedUri: String,
    val originalSizeBytes: Long,
    val optimizedSizeBytes: Long,
    val width: Int,
    val height: Int,
    val savingsPercent: Int
)

/**
 * ImageOptimizer comprime y redimensiona imágenes automáticamente al subir foto de perfil o post al Feed.
 *
 * - **¿Para qué sirve?** Evita almacenar bitmaps gigantes en memoria o en almacenamiento interno,
 *   reduciendo hasta un 85-95% el tamaño de las imágenes sin pérdida visual perceptible.
 * - **¿Cómo funciona?**
 *   1. Lee dimensiones con `inJustDecodeBounds = true` sin cargar el bitmap completo en RAM.
 *   2. Calcula `inSampleSize` en potencias de 2 según el objetivo (`400px` para avatar, `1024px` para feed).
 *   3. Escala proporcionalmente (y recorta cuadrado centrado `384x384` si es avatar).
 *   4. Comprime en JPEG con calidad calibrada (`82` avatar, `80` post) en `filesDir/optimized_media`.
 *   5. Recicla bitmaps intermedios inmediatamente.
 */
object ImageOptimizer {

    private const val MEDIA_DIR_NAME = "optimized_media"
    private const val MAX_AVATAR_BOUNDS = 400
    private const val MAX_AVATAR_FINAL = 384
    private const val MAX_POST_DIM = 1024
    private const val AVATAR_QUALITY = 82
    private const val POST_QUALITY = 80

    /**
     * Optimiza una imagen proveniente de un [Uri] (galería o selector de archivos) y la persiste
     * comprimida en el almacenamiento interno de la aplicación (`filesDir/optimized_media`).
     */
    fun optimizeImageFromUri(
        context: Context,
        sourceUri: Uri,
        forAvatar: Boolean = false
    ): ImageOptimizationResult {
        val resolver = context.contentResolver

        // 1. Medir tamaño original del stream
        val originalSizeBytes = try {
            resolver.openAssetFileDescriptor(sourceUri, "r")?.use { afd ->
                if (afd.length > 0) afd.length else -1L
            }?.takeIf { it > 0 } ?: resolver.openInputStream(sourceUri)?.use { stream ->
                val buffer = ByteArray(8192)
                var total = 0L
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    total += read
                }
                total
            } ?: 0L
        } catch (_: Exception) {
            0L
        }

        // 2. Leer dimensiones sin cargar el bitmap en RAM
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        resolver.openInputStream(sourceUri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }

        val maxDim = if (forAvatar) MAX_AVATAR_BOUNDS else MAX_POST_DIM
        val sampleSize = calculateInSampleSize(
            rawWidth = boundsOptions.outWidth,
            rawHeight = boundsOptions.outHeight,
            maxDimension = maxDim
        )

        // 3. Decodificar bitmap sub-muestreado
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decodedBitmap = resolver.openInputStream(sourceUri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: throw IllegalArgumentException("No se pudo decodificar la imagen seleccionada")

        return processAndSaveBitmap(
            context = context,
            sourceBitmap = decodedBitmap,
            originalSizeBytes = if (originalSizeBytes > 0L) originalSizeBytes else decodedBitmap.allocationByteCount.toLong(),
            forAvatar = forAvatar,
            recycleSource = true
        )
    }

    /**
     * Optimiza un [Bitmap] en memoria (por ejemplo, capturado con la cámara) y lo guarda
     * comprimido en `filesDir/optimized_media`.
     */
    fun optimizeBitmapToInternalFile(
        context: Context,
        bitmap: Bitmap,
        forAvatar: Boolean = false
    ): ImageOptimizationResult {
        // Estimamos el tamaño sin comprimir o en PNG/JPEG 100% para comparar el ahorro real
        val estimatedRawBytes = try {
            val rawOut = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, rawOut)
            max(rawOut.size().toLong(), bitmap.byteCount.toLong() / 3L)
        } catch (_: Exception) {
            bitmap.byteCount.toLong()
        }

        return processAndSaveBitmap(
            context = context,
            sourceBitmap = bitmap,
            originalSizeBytes = estimatedRawBytes,
            forAvatar = forAvatar,
            recycleSource = false
        )
    }

    private fun processAndSaveBitmap(
        context: Context,
        sourceBitmap: Bitmap,
        originalSizeBytes: Long,
        forAvatar: Boolean,
        recycleSource: Boolean
    ): ImageOptimizationResult {
        val transformedBitmap = if (forAvatar) {
            val cropped = centerCropSquare(sourceBitmap)
            val scaled = if (cropped.width > MAX_AVATAR_FINAL) {
                Bitmap.createScaledBitmap(cropped, MAX_AVATAR_FINAL, MAX_AVATAR_FINAL, true)
            } else {
                cropped
            }
            if (scaled !== cropped && cropped !== sourceBitmap && !cropped.isRecycled) {
                cropped.recycle()
            }
            scaled
        } else {
            scaleProportionally(sourceBitmap, MAX_POST_DIM)
        }

        if (recycleSource && transformedBitmap !== sourceBitmap && !sourceBitmap.isRecycled) {
            sourceBitmap.recycle()
        }

        val mediaDir = File(context.filesDir, MEDIA_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }

        val prefix = if (forAvatar) "avatar" else "post"
        val uniqueSuffix = "${System.currentTimeMillis()}_${transformedBitmap.width}x${transformedBitmap.height}"
        val outputFile = File(mediaDir, "${prefix}_${uniqueSuffix}.jpg")

        val quality = if (forAvatar) AVATAR_QUALITY else POST_QUALITY
        FileOutputStream(outputFile).use { out ->
            transformedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.flush()
        }

        val finalWidth = transformedBitmap.width
        val finalHeight = transformedBitmap.height

        if (transformedBitmap !== sourceBitmap && !transformedBitmap.isRecycled) {
            transformedBitmap.recycle()
        } else if (recycleSource && !transformedBitmap.isRecycled) {
            transformedBitmap.recycle()
        }

        val optimizedBytes = outputFile.length().coerceAtLeast(1L)
        val baselineBytes = max(originalSizeBytes, optimizedBytes)
        val savingsPercent = if (baselineBytes > optimizedBytes) {
            (((baselineBytes - optimizedBytes).toDouble() / baselineBytes.toDouble()) * 100.0)
                .roundToInt()
                .coerceIn(1, 99)
        } else {
            0
        }

        return ImageOptimizationResult(
            optimizedUri = Uri.fromFile(outputFile).toString(),
            originalSizeBytes = baselineBytes,
            optimizedSizeBytes = optimizedBytes,
            width = finalWidth,
            height = finalHeight,
            savingsPercent = savingsPercent
        )
    }

    /**
     * Calcula el factor `inSampleSize` como potencia de 2 para acercarse a [maxDimension].
     */
    fun calculateInSampleSize(rawWidth: Int, rawHeight: Int, maxDimension: Int): Int {
        if (rawWidth <= 0 || rawHeight <= 0 || maxDimension <= 0) return 1
        var inSampleSize = 1
        val largestSide = max(rawWidth, rawHeight)
        if (largestSide > maxDimension) {
            val halfSide = largestSide / 2
            while ((halfSide / inSampleSize) >= maxDimension) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun centerCropSquare(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        if (width == height) return source
        val side = minOf(width, height)
        val xOffset = (width - side) / 2
        val yOffset = (height - side) / 2
        return Bitmap.createBitmap(source, xOffset, yOffset, side, side)
    }

    private fun scaleProportionally(source: Bitmap, maxDimension: Int): Bitmap {
        val width = source.width
        val height = source.height
        val largest = max(width, height)
        if (largest <= maxDimension || largest <= 0) return source
        val ratio = maxDimension.toFloat() / largest.toFloat()
        val targetWidth = (width * ratio).roundToInt().coerceAtLeast(1)
        val targetHeight = (height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    /**
     * Formatea un resumen legible del resultado de optimización en español o inglés.
     * Ejemplo: `"Optimizada: 2.4 MB → 118 KB (-95%)"`
     */
    fun formatSizeLabel(result: ImageOptimizationResult, isEnglish: Boolean = false): String {
        val prefix = if (isEnglish) "Optimized" else "Optimizada"
        val origText = formatBytes(result.originalSizeBytes)
        val optText = formatBytes(result.optimizedSizeBytes)
        val savingsSuffix = if (result.savingsPercent > 0) " (-${result.savingsPercent}%)" else ""
        return "$prefix: $origText → $optText$savingsSuffix"
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.0f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.1f MB", mb)
    }
}
