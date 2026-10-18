package com.lhy.deepseekmobile.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Images are normalised once, at attach time: long edge <= 1600px, JPEG q88.
 * That keeps uploads small (vision tokens scale with pixels) and makes the
 * thumbnails instant to reopen later.
 */
class ImageStore(private val context: Context, private val dir: File) {

    fun dirFor(date: Long): File = File(dir, "images").apply { mkdirs() }

    suspend fun import(uri: Uri, id: String): File? {
        val bitmap = decode(uri) ?: return null
        val scaled = downscale(bitmap, MAX_SIDE)
        val target = File(File(dir, "images").apply { mkdirs() }, "$id.jpg")
        target.outputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
        }
        if (scaled !== bitmap) scaled.recycle()
        bitmap.recycle()
        return target
    }

    fun dataUrl(file: File, maxSide: Int = 1536, quality: Int = 85): String? {
        val original = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        val scaled = downscale(original, maxSide)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        if (scaled !== original) scaled.recycle()
        original.recycle()
        val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64"
    }

    fun sizeOf(file: File): Pair<Int, Int> {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        return opts.outWidth to opts.outHeight
    }

    private fun decode(uri: Uri): Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= 28) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
            }
        } else {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            var sample = 1
            while (opts.outWidth / sample > 3200 || opts.outHeight / sample > 3200) sample *= 2
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: return null
            val rotation = runCatching {
                when (ExifInterface(context.contentResolver.openInputStream(uri)!!).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }.getOrDefault(0f)
            if (rotation != 0f) {
                Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rotation) }, true)
                    .also { if (it !== bmp) bmp.recycle() }
            } else bmp
        }
    }.getOrNull()

    private fun downscale(bitmap: Bitmap, maxSide: Int): Bitmap {
        val long = maxOf(bitmap.width, bitmap.height)
        if (long <= maxSide) return bitmap
        val ratio = maxSide.toFloat() / long
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
    }

    companion object {
        private const val MAX_SIDE = 1600
    }
}
