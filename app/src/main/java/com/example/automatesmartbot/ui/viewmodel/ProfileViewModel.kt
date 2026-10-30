package com.example.automatesmartbot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automatesmartbot.data.model.UserPreferences
import com.example.automatesmartbot.data.model.UserProfile
import com.example.automatesmartbot.data.model.UserQuota
import com.example.automatesmartbot.data.repository.ProfileRepository
import com.example.automatesmartbot.data.repository.ProfileResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    object Loading : ProfileUiState
    object Unauthorized : ProfileUiState
    object Offline : ProfileUiState
    data class Error(val message: String) : ProfileUiState
    data class Success(
        val profile: UserProfile,
        val quota: UserQuota,
        val preferences: UserPreferences
    ) : ProfileUiState
}

class ProfileViewModel(
    private val repository: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            when (val result = repository.fetchProfile()) {
                is ProfileResult.Success -> {
                    _uiState.value = ProfileUiState.Success(result.profile, result.quota, result.preferences)
                }
                is ProfileResult.Unauthorized -> {
                    _uiState.value = ProfileUiState.Unauthorized
                }
                is ProfileResult.Offline -> {
                    _uiState.value = ProfileUiState.Offline
                }
                is ProfileResult.Error -> {
                    _uiState.value = ProfileUiState.Error(result.message)
                }
            }
        }
    }
}
