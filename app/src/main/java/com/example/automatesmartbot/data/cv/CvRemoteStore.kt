package com.example.automatesmartbot.data.cv

data class CvRemoteFile(
    val id: String,
    val fileName: String,
    val sizeBytes: Long,
    val contentType: String? = null,
    val uploadedAt: String? = null,
    val sha256: String? = null
)

sealed interface CvRemoteStatus {
    data object NoCv : CvRemoteStatus
    data class Uploaded(val file: CvRemoteFile) : CvRemoteStatus
}

sealed interface CvResult<out T> {
    data class Success<T>(val value: T) : CvResult<T>
    data class Invalid(val message: String) : CvResult<Nothing>
    data class Failed(val message: String) : CvResult<Nothing>
    data object NotAuthenticated : CvResult<Nothing>
    data object BackendUnavailable : CvResult<Nothing>
}

interface CvRemoteStore {
    suspend fun getStatus(): CvRemoteStatus
    suspend fun upload(selection: CvSelection): CvRemoteFile
    suspend fun delete()
}
