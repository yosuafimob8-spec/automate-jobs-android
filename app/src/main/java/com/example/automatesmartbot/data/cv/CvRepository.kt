package com.example.automatesmartbot.data.cv

import android.net.Uri
import com.example.automatesmartbot.data.security.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CvRepository(
    private val localStore: CvSelectionStore,
    private val remoteStore: CvRemoteStore? = null,
    private val sessionManager: SessionManager? = null
) {
    fun getLocalSelection(): CvSelection? = localStore.getSelection()

    suspend fun refreshStatus(): CvResult<CvRemoteStatus> = withContext(Dispatchers.IO) {
        remoteAccess()?.let { remote ->
            try {
                CvResult.Success(remote.getStatus())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CvResult.Failed(e.cvMessage("Gagal memuat status CV. Coba lagi."))
            }
        } ?: remoteUnavailableResult()
    }

    suspend fun saveLocalSelection(uri: Uri): CvResult<CvSelection> = withContext(Dispatchers.IO) {
        val readResult = localStore.read(uri)
        val selection = readResult.selection
            ?: return@withContext CvResult.Invalid(
                readResult.validation?.message ?: "CV tidak dapat dibaca."
            )

        try { localStore.parseCv(uri) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { return@withContext CvResult.Invalid(e.cvMessage("Teks CV tidak dapat dibaca.")) }

        localStore.persistReadPermission(uri)
        localStore.saveSelection(selection)
        CvResult.Success(selection)
    }

    suspend fun replace(uri: Uri): CvResult<CvRemoteFile> {
        return when (val localResult = saveLocalSelection(uri)) {
            is CvResult.Success -> upload(localResult.value)
            is CvResult.Invalid -> CvResult.Invalid(localResult.message)
            is CvResult.Failed -> CvResult.Failed(localResult.message)
            CvResult.NotAuthenticated -> CvResult.NotAuthenticated
            CvResult.BackendUnavailable -> CvResult.BackendUnavailable
        }
    }

    suspend fun uploadCurrent(): CvResult<CvRemoteFile> = upload(getLocalSelection())

    suspend fun upload(selection: CvSelection?): CvResult<CvRemoteFile> = withContext(Dispatchers.IO) {
        if (selection == null) {
            return@withContext CvResult.Invalid("Pilih file CV terlebih dahulu.")
        }

        val uri = runCatching { Uri.parse(selection.uri) }.getOrNull()
            ?: return@withContext CvResult.Invalid("CV tidak dapat dibaca.")
        val currentSelection = localStore.read(uri).selection
            ?: return@withContext CvResult.Invalid("CV tidak valid. Pilih ulang file PDF maksimal 2 MB.")

        try { localStore.parseCv(uri) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { return@withContext CvResult.Invalid(e.cvMessage("Teks CV tidak dapat dibaca.")) }

        remoteAccess()?.let { remote ->
            try {
                CvResult.Success(remote.upload(currentSelection))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CvResult.Failed(e.cvMessage("Gagal mengunggah CV. Coba lagi."))
            }
        } ?: remoteUnavailableResult()
    }

    suspend fun deleteRemote(): CvResult<Unit> = withContext(Dispatchers.IO) {
        remoteAccess()?.let { remote ->
            try {
                remote.delete()
                CvResult.Success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CvResult.Failed(e.cvMessage("Gagal menghapus CV di server. Coba lagi."))
            }
        } ?: remoteUnavailableResult()
    }

    suspend fun deleteLocal(): CvResult<Unit> = withContext(Dispatchers.IO) {
        val selection = localStore.getSelection()
        localStore.releaseReadPermission(selection)
        localStore.clearSelection()
        CvResult.Success(Unit)
    }

    private fun remoteAccess(): CvRemoteStore? {
        if (remoteStore == null) return null
        if (sessionManager?.getValidToken().isNullOrBlank()) return null
        return remoteStore
    }

    private fun remoteUnavailableResult(): CvResult<Nothing> {
        return if (remoteStore == null) {
            CvResult.BackendUnavailable
        } else {
            CvResult.NotAuthenticated
        }
    }

    private fun Exception.cvMessage(fallback: String): String {
        return localizedMessage?.takeIf { it.isNotBlank() } ?: fallback
    }
}
