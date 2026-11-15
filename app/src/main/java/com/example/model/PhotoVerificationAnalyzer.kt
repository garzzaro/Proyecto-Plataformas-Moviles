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
    val zeroDatabaseBytesConfirmed: Boolean = true,
    val dynamicRangeScore: Int = 0,
    val quadrantTextureScore: Int = 0,
    val duplicateRejected: Boolean = false,
    val perceptualHash: String = ""
)

/**
 * PhotoVerificationAnalyzer analiza una foto en memoria RAM para verificar un microhábito o
 * desbloquear el Modo Enfoque y la descarta de inmediato (0 bytes en Room y 0 archivos en disco).
 *
 * - **¿Para qué sirve?** Proporciona un motor de visión ligero embebido en la aplicación que certifica
 *   que la imagen capturada por el usuario contiene iluminación, rango dinámico, distribución espacial
 *   y nitidez de bordes reales, evitando capturas repetidas o pantallas sólidas sin almacenar bytes en Room.
 * - **¿Cómo funciona?**
 *   1. Muestrea la imagen a una cuadrícula en memoria de `64x64` píxeles para calcular luminosidad (ITU-R BT.601).
 *   2. Calcula el rango dinámico (`maxLum - minLum`) y la textura en 4 cuadrantes espaciales para detectar fotos reales.
 *   3. Mide varianza cromática RGB, nitidez de gradientes y firma perceptual **dHash (64 bits)** anti-duplicados en RAM.
 *   4. Destruye y recicla inmediatamente el Bitmap (`recycle()`), garantizando 0 bytes retenidos.
 * - **¿Qué pasa si lo quitamos?** La aplicación tendría que guardar miles de fotografías en disco y BD para verificar
 *   hábitos, saturando el almacenamiento del dispositivo y la cuota de la nube.
 */
object PhotoVerificationAnalyzer {

    private const val SAMPLE_GRID_SIZE = 64
    private const val MAX_RECENT_HASHES = 12
    private const val DHASH_DUPLICATE_THRESHOLD = 3
    private val recentVerificationHashes = ArrayDeque<Long>()

    /** Reinicia el historial efímero de huellas dHash en RAM (útil al cambiar de sesión o en pruebas). */
    fun resetRecentHashes() {
        synchronized(recentVerificationHashes) {
            recentVerificationHashes.clear()
        }
    }

    /**
     * Analiza un [Bitmap] en memoria RAM mediante una matriz ligera de `64x64` píxeles y descarta
     * el bitmap temporal sin guardar nada en disco ni en la base de datos Room.
     */
    fun analyzeBitmapAndDiscard(
        bitmap: Bitmap?,
        habitTitle: String = "",
        checkDuplicate: Boolean = false
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
        var minLuminance = 255.0
        var maxLuminance = 0.0
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
            if (lum < minLuminance) minLuminance = lum
            if (lum > maxLuminance) maxLuminance = lum
        }

        val meanLum = sumLuminance / totalPixels
        val brightnessScore = ((meanLum / 255.0) * 100.0).roundToInt().coerceIn(0, 100)
        val dynamicRangeScore = (((maxLuminance - minLuminance) / 180.0) * 100.0).roundToInt().coerceIn(0, 100)

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

        // Cobertura espacial por 4 cuadrantes para distinguir escenas reales de fondos planos
        val half = SAMPLE_GRID_SIZE / 2
        val quadrantSums = DoubleArray(4)
        val quadrantCounts = IntArray(4)
        for (y in 0 until SAMPLE_GRID_SIZE) {
            for (x in 0 until SAMPLE_GRID_SIZE) {
                val q = (if (y >= half) 2 else 0) + (if (x >= half) 1 else 0)
                quadrantSums[q] += luminances[y * SAMPLE_GRID_SIZE + x]
                quadrantCounts[q]++
            }
        }
        val quadrantMeans = DoubleArray(4) { q -> quadrantSums[q] / quadrantCounts[q].coerceAtLeast(1) }
        val quadrantVars = DoubleArray(4)
        for (y in 0 until SAMPLE_GRID_SIZE) {
            for (x in 0 until SAMPLE_GRID_SIZE) {
                val q = (if (y >= half) 2 else 0) + (if (x >= half) 1 else 0)
                val diff = luminances[y * SAMPLE_GRID_SIZE + x] - quadrantMeans[q]
                quadrantVars[q] += diff * diff
            }
        }
        var texturedQuadrants = 0
        var quadrantStdSum = 0.0
        for (q in 0 until 4) {
            val stdQ = sqrt(quadrantVars[q] / quadrantCounts[q].coerceAtLeast(1))
            quadrantStdSum += stdQ
            if (stdQ >= 3.5) texturedQuadrants++
        }
        val quadrantTextureScore = (((quadrantStdSum / 4.0) / 36.0) * 70.0 + texturedQuadrants * 7.5)
            .roundToInt().coerceIn(0, 100)

        // Huella perceptual dHash de 64 bits (cuadrícula 9x8 sobre luminancia en RAM)
        val dHashLong = computeDifferenceHash64(luminances)
        val dHashHex = dHashLong.toULong().toString(16).padStart(16, '0')
        val isDuplicateCapture = if (checkDuplicate && (colorVarietyScore >= 5 || sharpnessScore >= 5)) {
            synchronized(recentVerificationHashes) {
                recentVerificationHashes.any { prev ->
                    java.lang.Long.bitCount(prev xor dHashLong) <= DHASH_DUPLICATE_THRESHOLD
                }
            }
        } else {
            false
        }

        val passesVisualChecks = brightnessScore in 6..98 &&
            (colorVarietyScore >= 5 || sharpnessScore >= 5) &&
            (dynamicRangeScore >= 4 || sharpnessScore >= 8)
        val isValid = passesVisualChecks && !isDuplicateCapture

        if (isValid && checkDuplicate) {
            synchronized(recentVerificationHashes) {
                recentVerificationHashes.addLast(dHashLong)
                while (recentVerificationHashes.size > MAX_RECENT_HASHES) {
                    recentVerificationHashes.removeFirst()
                }
            }
        }

        val confidencePercent = if (isValid) {
            val lightBalance = 100 - abs(brightnessScore - 52)
            val rawConfidence = 82 + (
                (lightBalance * 0.04) +
                    (sharpnessScore * 0.05) +
                    (colorVarietyScore * 0.04) +
                    (dynamicRangeScore * 0.02) +
                    (quadrantTextureScore * 0.02)
                ).roundToInt()
            rawConfidence.coerceIn(82, 99)
        } else {
            minOf(brightnessScore.coerceAtLeast(5), maxOf(colorVarietyScore, sharpnessScore, dynamicRangeScore))
                .coerceIn(5, 45)
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
            isDuplicateCapture -> Pair(
                "Evidencia duplicada detectada (dHash $dHashHex). Toma una foto nueva de tu progreso actual.",
                "Duplicate proof detected (dHash $dHashHex). Capture a fresh photo of your current progress."
            )
            !isValid -> Pair(
                "Imagen sin detalle suficiente (nitidez $sharpnessScore%, color $colorVarietyScore%, rango $dynamicRangeScore%). Enfoca el objeto real.",
                "Image lacks detail (sharpness $sharpnessScore%, color $colorVarietyScore%, range $dynamicRangeScore%). Focus on the real object."
            )
            else -> Pair(
                "Prueba verificada$habitLabelEs ($confidencePercent% confianza · Luz $brightnessScore% · Nitidez $sharpnessScore% · Rango $dynamicRangeScore%). 0 B guardados en BD.",
                "Proof verified$habitLabelEn ($confidencePercent% confidence · Light $brightnessScore% · Sharpness $sharpnessScore% · Range $dynamicRangeScore%). 0 B saved in DB."
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
            zeroDatabaseBytesConfirmed = true,
            dynamicRangeScore = dynamicRangeScore,
            quadrantTextureScore = quadrantTextureScore,
            duplicateRejected = isDuplicateCapture,
            perceptualHash = dHashHex
        )
    }

    private fun computeDifferenceHash64(luminances: DoubleArray): Long {
        var hash = 0L
        var bitIndex = 0
        for (row in 0 until 8) {
            val y = ((row + 0.5) * SAMPLE_GRID_SIZE / 8.0).toInt().coerceIn(0, SAMPLE_GRID_SIZE - 1)
            for (col in 0 until 8) {
                val x1 = ((col * SAMPLE_GRID_SIZE) / 9.0).toInt().coerceIn(0, SAMPLE_GRID_SIZE - 1)
                val x2 = (((col + 1) * SAMPLE_GRID_SIZE) / 9.0).toInt().coerceIn(0, SAMPLE_GRID_SIZE - 1)
                val left = luminances[y * SAMPLE_GRID_SIZE + x1]
                val right = luminances[y * SAMPLE_GRID_SIZE + x2]
                if (left > right) {
                    hash = hash or (1L shl bitIndex)
                }
                bitIndex++
            }
        }
        return hash
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
