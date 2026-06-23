package com.wishesapp.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

object ImageUtil {

    /** Decode a URI from the gallery/camera, resize to [maxDim], return base64 JPEG strings. */
    fun encodeImageUri(context: Context, uri: Uri): Pair<String, String>? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(input)
            input.close()
            val full = resizeBitmap(original, 1080)
            val small = resizeBitmap(original, 200)
            Pair(bitmapToBase64(small), bitmapToBase64(full))
        } catch (e: Exception) {
            null
        }
    }

    private fun resizeBitmap(src: Bitmap, maxDim: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= maxDim && h <= maxDim) return src
        val scale = maxDim.toFloat() / maxOf(w, h)
        val matrix = Matrix().apply { postScale(scale, scale) }
        return Bitmap.createBitmap(src, 0, 0, w, h, matrix, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    /** Decode a base64 string to Bitmap. */
    fun decodeBase64(base64: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Decode base64 to Bitmap with on-device disk caching.
     * [cacheKey] should be unique per image (e.g. "${wishId}_small" or "${wishId}_full").
     * First call decodes from base64 and saves to cacheDir/wish_cache/<key>.jpg.
     * Subsequent calls load from the cache file directly.
     */
    fun getOrDecodeWithCache(context: Context, base64: String, cacheKey: String): Bitmap? {
        val cacheFile = File(context.cacheDir, "wish_cache/$cacheKey.jpg")
        if (cacheFile.exists()) {
            val cached = BitmapFactory.decodeFile(cacheFile.absolutePath)
            if (cached != null) return cached
        }
        val bitmap = decodeBase64(base64) ?: return null
        try {
            cacheFile.parentFile?.mkdirs()
            cacheFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        } catch (_: Exception) { /* cache write failure is non-fatal */ }
        return bitmap
    }

    /** Remove cached images for a deleted wish. */
    fun deleteCachedWish(context: Context, wishId: String) {
        File(context.cacheDir, "wish_cache/${wishId}_small.jpg").delete()
        File(context.cacheDir, "wish_cache/${wishId}_full.jpg").delete()
    }

    /** Save a bitmap to the widget image cache directory and return the file path. */
    fun saveBitmapForWidget(context: Context, bitmap: Bitmap, wishId: String): String {
        val dir = File(context.cacheDir, "widget_images").also { it.mkdirs() }
        val file = File(dir, "$wishId.jpg")
        file.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it)
        }
        return file.absolutePath
    }

    /**
     * Save a bitmap to the device photo gallery (Pictures album).
     * On Android 10+ uses scoped storage; on older versions requires WRITE_EXTERNAL_STORAGE.
     * Returns true on success.
     */
    fun saveToGallery(context: Context, bitmap: Bitmap, displayName: String): Boolean {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Wishes")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            ) ?: return false
            context.contentResolver.openOutputStream(uri)?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }
            true
        } catch (_: Exception) { false }
    }
}
