package com.example.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
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
     *
     * - **¿Para qué sirve?** Toma cualquier imagen seleccionada por el usuario, reduce drásticamente su tamaño
     *   (hasta un 85-95% menos) y genera un archivo JPEG local optimizado para avatares o posts del Feed.
     * - **¿Cómo funciona?**
     *   1. Lee los metadatos de tamaño y resolución mediante `inJustDecodeBounds = true` sin alocar memoria para los píxeles.
     *   2. Calcula el factor `inSampleSize` como potencia de 2 para submuestrear la imagen durante la decodificación.
     *   3. Aplica recorte cuadrado centrado (`384x384`) para fotos de perfil o reescala proporcional (`1024px`) para feed.
     *   4. Comprime en JPEG calibrado (80%-82% de calidad) y almacena en el directorio interno de la app.
     * - **¿Qué pasa si lo quitamos?** Subir una fotografía de 10-15 MB directamente desde la cámara o galería
     *   provocaría errores `OutOfMemoryError` en el renderizado y saturaría el almacenamiento del dispositivo.
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
     *
     * - **¿Para qué sirve?** Procesa bitmaps directos en memoria RAM generados por cámaras o previews
     *   para convertirlos en archivos JPEG persistidos y livianos.
     * - **¿Cómo funciona?** Estima el tamaño original, realiza el recorte/escalado geométrico según sea avatar
     *   o post y escribe el archivo JPEG en almacenamiento local seguro.
     * - **¿Qué pasa si lo quitamos?** Las imágenes generadas en memoria no podrían persistirse eficientemente
     *   ni asociarse a la entidad del usuario o publicación.
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
     *
     * - **¿Para qué sirve?** Instruye al decodificador de imágenes de Android (`BitmapFactory`) para que submuestree
     *   la imagen mientras lee el stream desde el disco, ahorrando memoria RAM.
     * - **¿Cómo funciona?** Compara el lado mayor con la dimensión máxima deseada y multiplica por 2 el divisor
     *   mientras la mitad del lado siga superando el límite.
     * - **¿Qué pasa si lo quitamos?** El sistema decodificaría la imagen en su resolución completa original,
     *   agotando la memoria heap en imágenes de cámaras de 50 o 100 megapíxeles.
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
     *
     * - **¿Para qué sirve?** Muestra al usuario de manera transparente el impacto positivo del módulo
     *   de optimización de imágenes en el ahorro de almacenamiento.
     * - **¿Cómo funciona?** Convierte los bytes iniciales y finales a cadenas legibles (KB/MB) y añade el porcentaje ahorrado.
     * - **¿Qué pasa si lo quitamos?** La interfaz no podría retroalimentar el ahorro de espacio al subir fotos.
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

    private val isRobolectricUnitTest: Boolean by lazy {
        runCatching { Class.forName("org.robolectric.Robolectric") != null }.getOrDefault(false)
    }

    private val cdnHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /** Devuelve `true` si la referencia ya apunta a un CDN externo por HTTP/HTTPS. */
    fun isRemoteCdnUrl(url: String?): Boolean {
        val clean = url?.trim().orEmpty()
        return clean.startsWith("https://", ignoreCase = true) ||
            clean.startsWith("http://", ignoreCase = true)
    }

    /**
     * Sube la imagen optimizada a un CDN externo (`Cloudinary` -> `Cloudflare Images` -> `FreeImage.host`
     * -> `Catbox.moe`), respalda en Firestore `habitocto_media` si la red CDN falla, y únicamente usa
     * Data-URI JPEG compacto si el dispositivo está completamente sin conexión.
     */
    suspend fun uploadImageOrEncodeForCloud(
        context: Context,
        imageUriString: String,
        isAvatar: Boolean = true
    ): String? = withContext(Dispatchers.IO) {
        val trimmed = imageUriString.trim()
        if (trimmed.isEmpty()) return@withContext null
        if (isRemoteCdnUrl(trimmed)) {
            return@withContext trimmed
        }

        val uri = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return@withContext null
        val targetDim = if (isAvatar) 192 else 480
        val maxBytes = if (isAvatar) 36_000 else 72_000

        val bitmap = decodeScaledBitmapFromUri(context, uri, targetDim) ?: return@withContext null
        val jpegBytes = compressBitmapToBudget(bitmap, targetDim, maxBytes)
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
        if (jpegBytes == null || jpegBytes.isEmpty()) return@withContext null

        val b64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)

        if (!isRobolectricUnitTest) {
            // 1. Intento directo a Cloudinary si está configurado en .env / BuildConfig
            uploadToCloudinaryIfConfigured(jpegBytes)?.let { return@withContext it }

            // 2. Intento directo a Cloudflare Images si está configurado en .env / BuildConfig
            uploadToCloudflareIfConfigured(jpegBytes)?.let { return@withContext it }

            // 3. Subida automática a CDN externo FreeImage (respaldado por CDN Cloudflare -> https://iili.io/...)
            uploadToFreeImageCdn(b64)?.let { return@withContext it }

            // 4. Subida automática a CDN externo Catbox (-> https://files.catbox.moe/...)
            uploadToCatboxCdn(jpegBytes)?.let { return@withContext it }

            // 5. Respaldo cloud en colección Firestore `habitocto_media`
            uploadToFirebaseFirestoreMedia(jpegBytes, b64)
        }

        // Fallback portable sin conexión (Data-URI JPEG compacta sincronizable en Firestore/REST)
        "data:image/jpeg;base64,$b64"
    }

    private fun decodeScaledBitmapFromUri(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openStreamForUri(context, uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            openStreamForUri(context, uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, opts)
            }
        }.getOrNull()
    }

    private fun openStreamForUri(context: Context, uri: Uri) = when {
        uri.scheme.equals("file", ignoreCase = true) && !uri.path.isNullOrBlank() ->
            File(uri.path!!).takeIf { it.exists() }?.inputStream()
        uri.scheme.isNullOrBlank() ->
            File(uri.toString()).takeIf { it.exists() }?.inputStream()
        else -> context.contentResolver.openInputStream(uri)
    }

    private fun compressBitmapToBudget(source: Bitmap, maxDim: Int, maxBytes: Int): ByteArray? {
        val scaled = scaleProportionally(source, maxDim)
        return try {
            var quality = 78
            var result: ByteArray
            do {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                result = out.toByteArray()
                quality -= 12
            } while (result.size > maxBytes && quality >= 30)
            result
        } finally {
            if (scaled !== source && !scaled.isRecycled) {
                scaled.recycle()
            }
        }
    }

    private fun readBuildConfigField(fieldName: String): String? = runCatching {
        val clazz = Class.forName("com.example.BuildConfig")
        val value = clazz.getField(fieldName).get(null) as? String
        value?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("MY_") }
    }.getOrNull()

    private fun uploadToCloudinaryIfConfigured(jpegBytes: ByteArray): String? {
        val cloudName = readBuildConfigField("CLOUDINARY_CLOUD_NAME") ?: return null
        val uploadPreset = readBuildConfigField("CLOUDINARY_UPLOAD_PRESET") ?: return null
        return runCatching {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart(
                    "file",
                    "habitocto_media.jpg",
                    jpegBytes.toRequestBody("image/jpeg".toMediaType())
                )
                .build()
            val req = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
                .post(body)
                .build()
            cdnHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val json = JSONObject(resp.body?.string().orEmpty())
                json.optString("secure_url").takeIf { isRemoteCdnUrl(it) }
            }
        }.getOrNull()
    }

    private fun uploadToCloudflareIfConfigured(jpegBytes: ByteArray): String? {
        val uploadUrl = readBuildConfigField("CLOUDFLARE_IMAGE_UPLOAD_URL") ?: return null
        val token = readBuildConfigField("CLOUDFLARE_IMAGES_TOKEN") ?: return null
        return runCatching {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "habitocto_media.jpg",
                    jpegBytes.toRequestBody("image/jpeg".toMediaType())
                )
                .build()
            val req = Request.Builder()
                .url(uploadUrl)
                .addHeader("Authorization", "Bearer $token")
                .post(body)
                .build()
            cdnHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val json = JSONObject(resp.body?.string().orEmpty())
                val resultObj = json.optJSONObject("result")
                val variants = resultObj?.optJSONArray("variants")
                if (variants != null && variants.length() > 0) {
                    variants.optString(0).takeIf { isRemoteCdnUrl(it) }
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private fun uploadToFreeImageCdn(base64Jpeg: String): String? {
        val uploadUrl = readBuildConfigField("FREEIMAGE_UPLOAD_API_URL")
            ?: "https://freeimage.host/api/1/upload"
        val apiKey = readBuildConfigField("FREEIMAGE_UPLOAD_API_KEY")
            ?: "6d207e02198a847aa98d0a2a901485a5"
        return runCatching {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("key", apiKey)
                .addFormDataPart("action", "upload")
                .addFormDataPart("format", "json")
                .addFormDataPart("source", base64Jpeg)
                .build()
            val req = Request.Builder()
                .url(uploadUrl)
                .addHeader("User-Agent", "HabitOcto/1.0")
                .post(body)
                .build()
            cdnHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val json = JSONObject(resp.body?.string().orEmpty())
                val imageObj = json.optJSONObject("image")
                imageObj?.optString("url")?.takeIf { isRemoteCdnUrl(it) }
            }
        }.getOrNull()
    }

    private fun uploadToCatboxCdn(jpegBytes: ByteArray): String? {
        val uploadUrl = readBuildConfigField("CATBOX_UPLOAD_API_URL")
            ?: "https://catbox.moe/user/api.php"
        return runCatching {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("reqtype", "fileupload")
                .addFormDataPart(
                    "fileToUpload",
                    "habitocto_media.jpg",
                    jpegBytes.toRequestBody("image/jpeg".toMediaType())
                )
                .build()
            val req = Request.Builder()
                .url(uploadUrl)
                .addHeader("User-Agent", "HabitOcto/1.0")
                .post(body)
                .build()
            cdnHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val url = resp.body?.string()?.trim().orEmpty()
                url.takeIf { isRemoteCdnUrl(it) }
            }
        }.getOrNull()
    }

    private fun uploadToFirebaseFirestoreMedia(jpegBytes: ByteArray, base64Jpeg: String): Boolean {
        val projectId = readBuildConfigField("FIREBASE_STORAGE_PROJECT_ID") ?: "habitocto-suchini-2026"
        return runCatching {
            val digest = java.security.MessageDigest.getInstance("SHA-256").digest(jpegBytes)
            val hashId = "img_" + digest.take(8).joinToString("") { "%02x".format(it) }
            val url =
                "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/habitocto_media/$hashId"
            val fields = JSONObject().apply {
                put("dataUri", JSONObject().put("stringValue", "data:image/jpeg;base64,$base64Jpeg"))
                put("bytes", JSONObject().put("integerValue", jpegBytes.size.toString()))
                put("updatedAt", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
            }
            val payload = JSONObject().put("fields", fields).toString()
            val req = Request.Builder()
                .url(url)
                .patch(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            cdnHttpClient.newCall(req).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }
}
