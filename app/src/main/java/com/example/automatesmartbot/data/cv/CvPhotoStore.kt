package com.example.automatesmartbot.data.cv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.UUID

/** Copy into private storage so gallery-provider permission changes cannot break the CV. */
class CvPhotoStore(private val context: Context) {
    fun deleteIfUnused(path: String, documents: List<CvDocument>) {
        if (path.isBlank() || documents.any { it.content.contact.photoPath == path }) return
        val file = File(path)
        if (file.canonicalFile.parentFile == File(context.filesDir, "cv_photos").canonicalFile) file.delete()
    }
    fun importPhoto(uri: Uri): String {
        val dir = File(context.filesDir, "cv_photos").apply { mkdirs() }
        val input = File.createTempFile("import", ".tmp", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { source ->
                input.outputStream().use { target -> target.write(source.readBytesLimited(12 * 1024 * 1024)) }
            } ?: error("Foto tidak dapat dibaca.")
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(input.path, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "File bukan gambar yang didukung." }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / inSampleSize > 1200) inSampleSize *= 2
            }
            val bitmap = BitmapFactory.decodeFile(input.path, opts) ?: error("Foto rusak.")
            val exif = ExifInterface(input)
            val matrix = Matrix().apply {
                if (exif.isFlipped) postScale(-1f, 1f)
                postRotate(exif.rotationDegrees.toFloat())
            }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            try { file.outputStream().use { require(rotated.compress(Bitmap.CompressFormat.JPEG, 88, it)) } }
            finally { if (rotated !== bitmap) rotated.recycle(); bitmap.recycle() }
            return file.path
        } finally { input.delete() }
    }
}

internal fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val n = read(buffer)
        if (n < 0) break
        require(output.size() + n <= limit) { "Ukuran file melebihi batas ${limit / 1024 / 1024} MB." }
        output.write(buffer, 0, n)
    }
    return output.toByteArray()
}
