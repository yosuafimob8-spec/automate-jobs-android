package com.example.automatesmartbot.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.automatesmartbot.data.cv.CvRemoteFile
import com.example.automatesmartbot.data.cv.CvRemoteStatus
import com.example.automatesmartbot.data.cv.CvRepository
import com.example.automatesmartbot.data.cv.CvResult
import com.example.automatesmartbot.data.cv.CvSelection
import com.example.automatesmartbot.data.cv.CvSelectionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CvUiState(
    val localSelection: CvSelection? = null,
    val serverState: CvServerState = CvServerState.Checking,
    val isBusy: Boolean = false,
    val message: String? = null
)

sealed interface CvServerState {
    data object Checking : CvServerState
    data object Unavailable : CvServerState
    data object NotAuthenticated : CvServerState
    data object NotUploaded : CvServerState
    data class Uploaded(val file: CvRemoteFile) : CvServerState
    data class Failed(val message: String) : CvServerState
}

class CvViewModel(
    application: Application,
    private val repository: CvRepository
) : AndroidViewModel(application) {
    constructor(application: Application) : this(
        application,
        CvRepository(CvSelectionStore(application))
    )

    private val _state = MutableStateFlow(
        CvUiState(localSelection = repository.getLocalSelection())
    )
    val state: StateFlow<CvUiState> = _state.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true, message = null)
            val result = repository.refreshStatus()
            _state.value = _state.value.copy(
                isBusy = false,
                serverState = result.toServerState(),
                message = (result as? CvResult.Failed)?.message
            )
        }
    }

    fun selectFile(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true, message = null)
            when (val localResult = repository.saveLocalSelection(uri)) {
                is CvResult.Success -> {
                    _state.value = _state.value.copy(
                        localSelection = localResult.value,
                        isBusy = false,
                        message = "CV disimpan di perangkat."
                    )
                    if (_state.value.serverState.canUpload()) {
                        uploadCurrent()
                    }
                }
                is CvResult.Invalid -> {
                    _state.value = _state.value.copy(
                        isBusy = false,
                        message = localResult.message
                    )
                }
                is CvResult.Failed -> {
                    _state.value = _state.value.copy(
                        isBusy = false,
                        message = localResult.message
                    )
                }
                CvResult.NotAuthenticated -> {
                    _state.value = _state.value.copy(
                        isBusy = false,
                        serverState = CvServerState.NotAuthenticated
                    )
                }
                CvResult.BackendUnavailable -> {
                    _state.value = _state.value.copy(
                        isBusy = false,
                        serverState = CvServerState.Unavailable
                    )
                }
            }
        }
    }

    fun replace(uri: Uri) = selectFile(uri)

    fun uploadCurrent() {
        val selection = _state.value.localSelection
        if (selection == null) {
            _state.value = _state.value.copy(message = "Pilih file CV terlebih dahulu.")
            return
        }
        if (!_state.value.serverState.canUpload()) {
            _state.value = _state.value.copy(
                message = if (_state.value.serverState == CvServerState.NotAuthenticated) {
                    "Sesi Anda berakhir. Silakan masuk kembali."
                } else {
                    "Penyimpanan CV di server belum tersedia."
                }
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true, message = null)
            val result = repository.upload(selection)
            _state.value = _state.value.copy(isBusy = false, message = result.message())
            if (result is CvResult.Success<*>) {
                refreshStatus()
            } else {
                _state.value = _state.value.copy(serverState = result.toServerState())
            }
        }
    }

    fun deleteLocal() {
        viewModelScope.launch {
            repository.deleteLocal()
            _state.value = _state.value.copy(
                localSelection = null,
                message = "CV di perangkat dihapus."
            )
        }
    }

    fun deleteRemote() {
        if (_state.value.serverState == CvServerState.Unavailable) {
            _state.value = _state.value.copy(
                message = "Penyimpanan CV di server belum tersedia."
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isBusy = true, message = null)
            val result = repository.deleteRemote()
            _state.value = _state.value.copy(
                isBusy = false,
                message = result.message(),
                serverState = if (result is CvResult.Success<*>) {
                    CvServerState.NotUploaded
                } else {
                    result.toServerState()
                }
            )
        }
    }

    private fun CvRemoteStatus.toServerState(): CvServerState {
        return when (this) {
            CvRemoteStatus.NoCv -> CvServerState.NotUploaded
            is CvRemoteStatus.Uploaded -> CvServerState.Uploaded(file)
        }
    }

    private fun CvResult<*>.toServerState(): CvServerState {
        return when (this) {
            is CvResult.Success<*> -> CvServerState.Checking
            is CvResult.Invalid -> CvServerState.Failed(message)
            is CvResult.Failed -> CvServerState.Failed(message)
            CvResult.NotAuthenticated -> CvServerState.NotAuthenticated
            CvResult.BackendUnavailable -> CvServerState.Unavailable
        }
    }

    private fun CvResult<*>.message(): String? {
        return when (this) {
            is CvResult.Success<*> -> null
            is CvResult.Invalid -> message
            is CvResult.Failed -> message
            CvResult.NotAuthenticated -> "Sesi Anda berakhir. Silakan masuk kembali."
            CvResult.BackendUnavailable -> "Penyimpanan CV di server belum tersedia."
        }
    }

    private fun CvServerState.canUpload(): Boolean {
        return this is CvServerState.NotUploaded ||
            this is CvServerState.Uploaded ||
            this is CvServerState.Failed
    }
}
