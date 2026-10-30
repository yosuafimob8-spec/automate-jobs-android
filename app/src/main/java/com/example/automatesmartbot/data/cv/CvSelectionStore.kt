package com.example.automatesmartbot.data.cv

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.example.automatesmartbot.util.CvValidation
import com.example.automatesmartbot.util.CvValidator

data class CvSelection(
    val uri: String,
    val displayName: String,
    val sizeBytes: Long
)

data class CvReadResult(
    val selection: CvSelection? = null,
    val validation: CvValidation? = null
)

class CvSelectionStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelection(): CvSelection? {
        val uri = preferences.getString(KEY_URI, null)?.takeIf { it.isNotBlank() } ?: return null
        val name = preferences.getString(KEY_NAME, null)?.takeIf { it.isNotBlank() } ?: return null
        val size = preferences.getLong(KEY_SIZE, 0L)
        if (size <= 0L) return null
        return CvSelection(uri = uri, displayName = name, sizeBytes = size)
    }

    fun saveSelection(selection: CvSelection) {
        preferences.edit()
            .putString(KEY_URI, selection.uri)
            .putString(KEY_NAME, selection.displayName)
            .putLong(KEY_SIZE, selection.sizeBytes)
            .apply()
    }

    fun clearSelection() {
        preferences.edit()
            .remove(KEY_URI)
            .remove(KEY_NAME)
            .remove(KEY_SIZE)
            .apply()
    }

    fun read(uri: Uri): CvReadResult {
        var displayName: String? = null
        var sizeBytes: Long? = null

        appContext.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                    displayName = cursor.getString(nameIndex)
                }
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        }

        if (sizeBytes == null || sizeBytes!! <= 0L) {
            sizeBytes = runCatching {
                appContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                    descriptor.length.takeIf { it > 0L }
                }
            }.getOrNull()
        }

        if (displayName.isNullOrBlank()) {
            displayName = uri.lastPathSegment?.substringAfterLast('/')
        }

        val headerBytes = runCatching {
            appContext.contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArray(5)
                val read = stream.read(buffer)
                if (read > 0) buffer.copyOf(read) else null
            }
        }.getOrNull()

        val validation = CvValidator.validate(
            displayName = displayName,
            mimeType = appContext.contentResolver.getType(uri),
            sizeBytes = sizeBytes,
            headerBytes = headerBytes
        )
        if (!validation.isValid) {
            return CvReadResult(validation = validation)
        }

        return CvReadResult(
            selection = CvSelection(
                uri = uri.toString(),
                displayName = displayName.orEmpty(),
                sizeBytes = sizeBytes ?: 0L
            )
        )
    }

    fun persistReadPermission(uri: Uri) {
        runCatching {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    fun releaseReadPermission(selection: CvSelection?) {
        val uri = selection?.uri?.let(Uri::parse) ?: return
        runCatching {
            appContext.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    companion object {
        private const val PREFS_NAME = "local_cv_selection"
        private const val KEY_URI = "cv_uri"
        private const val KEY_NAME = "cv_name"
        private const val KEY_SIZE = "cv_size"
    }
}
