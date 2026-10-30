package com.example.automatesmartbot.util

data class CvValidation(
    val isValid: Boolean,
    val message: String? = null
)

object CvValidator {
    const val MAX_SIZE_BYTES: Long = 2L * 1024L * 1024L
    private const val PDF_MIME = "application/pdf"
    private const val PDF_ALT_MIME = "application/x-pdf"
    private val PDF_HEADER = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D)

    fun validate(
        displayName: String?,
        mimeType: String?,
        sizeBytes: Long?,
        headerBytes: ByteArray?
    ): CvValidation {
        val name = displayName?.trim().orEmpty()
        if (name.isEmpty()) {
            return CvValidation(false, "Nama file CV tidak dapat dibaca.")
        }
        if (!name.endsWith(".pdf", ignoreCase = true)) {
            return CvValidation(false, "CV harus berupa file PDF.")
        }

        val normalizedMime = mimeType?.trim()?.lowercase()
        if (!normalizedMime.isNullOrEmpty() &&
            normalizedMime != PDF_MIME &&
            normalizedMime != PDF_ALT_MIME &&
            normalizedMime != "application/octet-stream"
        ) {
            return CvValidation(false, "CV harus berupa file PDF.")
        }

        if (sizeBytes == null || sizeBytes <= 0L) {
            return CvValidation(false, "Ukuran file CV tidak dapat dibaca.")
        }
        if (sizeBytes > MAX_SIZE_BYTES) {
            return CvValidation(false, "Ukuran CV maksimal 2 MB.")
        }
        if (!isPdfHeader(headerBytes)) {
            return CvValidation(false, "File tidak valid. Pilih PDF yang benar.")
        }

        return CvValidation(true)
    }

    fun isPdfHeader(headerBytes: ByteArray?): Boolean {
        if (headerBytes == null || headerBytes.size < PDF_HEADER.size) return false
        return PDF_HEADER.indices.all { index -> headerBytes[index] == PDF_HEADER[index] }
    }

    fun formatSize(sizeBytes: Long): String {
        return when {
            sizeBytes >= 1024L * 1024L -> {
                val value = sizeBytes / (1024.0 * 1024.0)
                String.format(java.util.Locale.US, "%.1f MB", value)
            }
            sizeBytes >= 1024L -> {
                val value = sizeBytes / 1024.0
                String.format(java.util.Locale.US, "%.1f KB", value)
            }
            else -> "$sizeBytes B"
        }
    }
}
