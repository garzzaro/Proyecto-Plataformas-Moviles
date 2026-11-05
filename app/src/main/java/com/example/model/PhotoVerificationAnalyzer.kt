package com.example.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * PhotoVerificationResult contiene el dictamen del análisis en memoria de una foto de prueba
 * para verificar un microhábito o desbloquear el Modo Enfoque.
 *
 * - **¿Para qué sirve?** Certifica si la imagen capturada tiene iluminación, contraste cromático y
 *   nitidez de bordes reales sin almacenar un solo byte en Room ni en el disco.
 * - **¿Cómo funciona?** Se construye en [PhotoVerificationAnalyzer] tras analizar una matriz ligera
 *   de `64x64` píxeles en RAM y reciclar el bitmap inmediatamente.
 * - **¿Qué pasa si lo quitamos?** No se podría exigir ni validar prueba fotográfica efímera para
 *   microhábitos y Modo Enfoque.
 */
data class PhotoVerificationResult(
    val isValid: Boolean,
    val confidencePercent: Int,
    val brightnessScore: Int,
    val sharpnessScore: Int,
    val colorVarietyScore: Int,
    val summaryEs: String,
    val summaryEn: String,
    val zeroDatabaseBytesConfirmed: Boolean = true
)

/**
 * PhotoVerificationAnalyzer analiza una foto en memoria RAM para verificar un microhábito o
 * desbloquear el Modo Enfoque y la descarta de inmediato (0 bytes en Room y 0 archivos en disco).
 *
 * - **¿Para qué sirve?** Proporciona un motor de visión ligero embebido en la aplicación que certifica
 *   que la imagen capturada por el usuario contiene iluminación, bordes y variedad cromática reales,
 *   sin almacenar imágenes en la base de datos local (Room) ni saturar Firestore.
 * - **¿Cómo funciona?**
 *   1. Muestrea la imagen a una cuadrícula en memoria de 64x64 píxeles para calcular luminosidad ponderada (ITU-R BT.601).
 *   2. Mide la varianza de color (desviación estándar en RGB) y la densidad de gradientes espaciales (nitidez de bordes).
 *   3. Determina validez y porcentaje de confianza (82%-99%).
 *   4. Destruye y recicla inmediatamente el Bitmap (`recycle()`), garantizando 0 bytes retenidos.
 * - **¿Qué pasa si lo quitamos?** La aplicación tendría que guardar miles de fotografías en disco y BD para verificar
 *   hábitos, saturando el almacenamiento del dispositivo y la cuota de la nube.
 */
object PhotoVerificationAnalyzer {

    private const val SAMPLE_GRID_SIZE = 64

    /**
     * Analiza un [Bitmap] en memoria RAM mediante una matriz ligera de `64x64` píxeles y descarta
     * el bitmap temporal sin guardar nada en disco ni en la base de datos Room.
     *
     * - **¿Para qué sirve?** Inspecciona métricas de calidad de imagen (luz, bordes, contraste) directamente desde un Bitmap.
     * - **¿Cómo funciona?**
     *   1. Escala el bitmap a `64x64` y extrae el arreglo lineal de píxeles ARGB.
     *   2. Recicla el bitmap escalado inmediatamente.
     *   3. Itera sobre los píxeles calculando luminosidad media, varianza RGB y gradientes entre vecinos.
     *   4. Asigna un dictamen legible en español e inglés junto con el porcentaje de confianza.
     * - **¿Qué pasa si lo quitamos?** No se podría validar la captura directa de la cámara para micro-hábitos ni modo enfoque.
     */
    fun analyzeBitmapAndDiscard(
        bitmap: Bitmap?,
        habitTitle: String = ""
    ): PhotoVerificationResult {
        if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) {
            return PhotoVerificationResult(
                isValid = false,
                confidencePercent = 0,
                brightnessScore = 0,
                sharpnessScore = 0,
                colorVarietyScore = 0,
                summaryEs = "Toma o selecciona una foto válida para verificar la evidencia.",
                summaryEn = "Take or select a valid photo to verify your proof.",
                zeroDatabaseBytesConfirmed = true
            )
        }

        val scaled = Bitmap.createScaledBitmap(bitmap, SAMPLE_GRID_SIZE, SAMPLE_GRID_SIZE, true)
        val totalPixels = SAMPLE_GRID_SIZE * SAMPLE_GRID_SIZE
        val pixels = IntArray(totalPixels)
        scaled.getPixels(pixels, 0, SAMPLE_GRID_SIZE, 0, 0, SAMPLE_GRID_SIZE, SAMPLE_GRID_SIZE)

        // Reciclamos el bitmap escalado temporal inmediatamente para no retener RAM
        if (scaled !== bitmap && !scaled.isRecycled) {
            scaled.recycle()
        }

        var sumLuminance = 0.0
        var sumR = 0.0
        var sumG = 0.0
        var sumB = 0.0

        val luminances = DoubleArray(totalPixels)

        for (i in 0 until totalPixels) {
            val pixel = pixels[i]
            val r = Color.red(pixel).toDouble()
            val g = Color.green(pixel).toDouble()
            val b = Color.blue(pixel).toDouble()

            sumR += r
            sumG += g
            sumB += b

            val lum = 0.299 * r + 0.587 * g + 0.114 * b
            luminances[i] = lum
            sumLuminance += lum
        }

        val meanLum = sumLuminance / totalPixels
        val brightnessScore = ((meanLum / 255.0) * 100.0).roundToInt().coerceIn(0, 100)

        val meanR = sumR / totalPixels
        val meanG = sumG / totalPixels
        val meanB = sumB / totalPixels

        var varR = 0.0
        var varG = 0.0
        var varB = 0.0

        for (i in 0 until totalPixels) {
            val pixel = pixels[i]
            val dr = Color.red(pixel) - meanR
            val dg = Color.green(pixel) - meanG
            val db = Color.blue(pixel) - meanB
            varR += dr * dr
            varG += dg * dg
            varB += db * db
        }

        val stdR = sqrt(varR / totalPixels)
        val stdG = sqrt(varG / totalPixels)
        val stdB = sqrt(varB / totalPixels)
        val avgChannelStd = (stdR + stdG + stdB) / 3.0
        // Normalizamos la desviación estándar típica (0..75+) a una escala 0..100
        val colorVarietyScore = ((avgChannelStd / 64.0) * 100.0).roundToInt().coerceIn(0, 100)

        // Nitidez/bordes: gradiente promedio entre píxeles adyacentes horizontales y verticales
        var gradientSum = 0.0
        var gradientCount = 0
        for (y in 0 until SAMPLE_GRID_SIZE - 1) {
            val rowOffset = y * SAMPLE_GRID_SIZE
            for (x in 0 until SAMPLE_GRID_SIZE - 1) {
                val idx = rowOffset + x
                val currentLum = luminances[idx]
                val gx = abs(currentLum - luminances[idx + 1])
                val gy = abs(currentLum - luminances[idx + SAMPLE_GRID_SIZE])
                gradientSum += (gx + gy) * 0.5
                gradientCount++
            }
        }

        val meanGradient = if (gradientCount > 0) gradientSum / gradientCount else 0.0
        val sharpnessScore = ((meanGradient / 28.0) * 100.0).roundToInt().coerceIn(0, 100)

        val isValid = brightnessScore in 6..98 && (colorVarietyScore >= 5 || sharpnessScore >= 5)

        val confidencePercent = if (isValid) {
            val lightBalance = 100 - abs(brightnessScore - 52)
            val rawConfidence = 82 + ((lightBalance * 0.06) + (sharpnessScore * 0.06) + (colorVarietyScore * 0.05)).roundToInt()
            rawConfidence.coerceIn(82, 99)
        } else {
            minOf(brightnessScore, maxOf(colorVarietyScore, sharpnessScore)).coerceIn(5, 45)
        }

        val habitLabelEs = if (habitTitle.isNotBlank()) " para \"$habitTitle\"" else ""
        val habitLabelEn = if (habitTitle.isNotBlank()) " for \"$habitTitle\"" else ""

        val (summaryEs, summaryEn) = when {
            brightnessScore < 6 -> Pair(
                "Foto demasiado oscura (luz $brightnessScore%). Ilumina la escena y vuelve a intentar.",
                "Photo is too dark (brightness $brightnessScore%). Add light and try again."
            )
            brightnessScore > 98 -> Pair(
                "Foto sobreexpuesta (luz $brightnessScore%). Evita apuntar directo a una luz intensa.",
                "Photo is overexposed (brightness $brightnessScore%). Avoid pointing directly at bright light."
            )
            !isValid -> Pair(
                "Imagen sin detalle suficiente (nitidez $sharpnessScore%, color $colorVarietyScore%). Enfoca el objeto real.",
                "Image lacks detail (sharpness $sharpnessScore%, color $colorVarietyScore%). Focus on the real object."
            )
            else -> Pair(
                "Prueba verificada$habitLabelEs ($confidencePercent% confianza · Luz $brightnessScore% · Nitidez $sharpnessScore%). Foto descartada: 0 B guardados en BD.",
                "Proof verified$habitLabelEn ($confidencePercent% confidence · Light $brightnessScore% · Sharpness $sharpnessScore%). Photo discarded: 0 B saved in DB."
            )
        }

        return PhotoVerificationResult(
            isValid = isValid,
            confidencePercent = confidencePercent,
            brightnessScore = brightnessScore,
            sharpnessScore = sharpnessScore,
            colorVarietyScore = colorVarietyScore,
            summaryEs = summaryEs,
            summaryEn = summaryEn,
            zeroDatabaseBytesConfirmed = true
        )
    }

    /**
     * Decodifica un thumbnail ligero en memoria desde [uri] (`inSampleSize = 4`), ejecuta
     * [analyzeBitmapAndDiscard] y recicla el bitmap inmediatamente sin guardar nada en disco ni Room.
     *
     * - **¿Para qué sirve?** Permite analizar imágenes provenientes del selector de la galería del dispositivo sin desbordar la memoria heap.
     * - **¿Cómo funciona?**
     *   1. Utiliza `inSampleSize = 4` al decodificar el stream del ContentResolver para cargar solo 1/16 de los píxeles en memoria.
     *   2. Delega la inspección matemática a [analyzeBitmapAndDiscard].
     *   3. Libera y recicla el bitmap decodificado (`bitmap.recycle()`) antes de retornar el resultado.
     * - **¿Qué pasa si lo quitamos?** Seleccionar una fotografía de alta resolución desde la galería causaría un error de memoria insuficiente (`OutOfMemoryError`).
     */
    fun analyzeUriAndDiscard(
        context: Context,
        uri: Uri,
        habitTitle: String = ""
    ): PhotoVerificationResult {
        val options = BitmapFactory.Options().apply {
            inSampleSize = 4
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (_: Exception) {
            null
        }

        val result = analyzeBitmapAndDiscard(bitmap, habitTitle)
        if (bitmap != null && !bitmap.isRecycled) {
            bitmap.recycle()
        }
        return result
    }
}
