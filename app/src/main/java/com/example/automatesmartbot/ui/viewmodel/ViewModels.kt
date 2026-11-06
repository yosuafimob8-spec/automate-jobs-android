package com.example.automatesmartbot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automatesmartbot.data.model.*
import com.example.automatesmartbot.data.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: DashboardRepository = DashboardRepository()
) : ViewModel() {
    private var membershipRequestVersion = 0
    private val _status = MutableStateFlow<DashboardStatusResponse?>(null)
    val status: StateFlow<DashboardStatusResponse?> = _status.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadStatus() {
        val requestVersion = ++membershipRequestVersion
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getDashboardStatus()
            if (requestVersion == membershipRequestVersion) {
                _status.value = result.getOrNull()
                _isLoading.value = false
            }
        }
    }

    fun reset() {
        membershipRequestVersion++ // Reject any in-flight status reply for the old account.
        _status.value = null
        _isLoading.value = false
    }
}

class EmailViewModel(
    private val repository: EmailRepository = EmailRepository()
) : ViewModel() {
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _resultMessage = MutableStateFlow<String?>(null)
    val resultMessage: StateFlow<String?> = _resultMessage.asStateFlow()

    fun sendEmail(request: EmailApplyRequest) {
        viewModelScope.launch {
            _isSending.value = true
            val result = repository.sendEmail(request)
            _resultMessage.value = result.fold(
                onSuccess = { it.message },
                onFailure = { it.localizedMessage ?: "Gagal mengirim email" }
            )
            _isSending.value = false
        }
    }

    fun clearMessage() {
        _resultMessage.value = null
    }
}

class ActivityViewModel(
    private val repository: ActivityRepository = ActivityRepository()
) : ViewModel() {
    private val _activities = MutableStateFlow<List<ActivityItemModel>>(emptyList())
    val activities: StateFlow<List<ActivityItemModel>> = _activities.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadActivities() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.getActivities()
            result.fold(
                onSuccess = { _activities.value = it },
                onFailure = {
                    _activities.value = emptyList()
                    _errorMessage.value = it.localizedMessage ?: "Gagal memuat riwayat aktivitas"
                }
            )
            _isLoading.value = false
        }
    }
}

class UserViewModel(
    private val repository: UserRepository = UserRepository()
) : ViewModel() {
    private val _profile = MutableStateFlow<UserProfileResponse?>(null)
    val profile: StateFlow<UserProfileResponse?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadProfile(isPro: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getUserProfile(isPro)
            _profile.value = result.getOrNull()
            _isLoading.value = false
        }
    }
}
