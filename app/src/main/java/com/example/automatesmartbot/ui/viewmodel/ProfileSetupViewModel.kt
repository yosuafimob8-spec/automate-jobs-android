package com.example.automatesmartbot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automatesmartbot.data.model.ProfileRequest
import com.example.automatesmartbot.data.model.ProfileResponse
import com.example.automatesmartbot.data.repository.ProfileSetupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileSetupViewModel(
    private val repository: ProfileSetupRepository = ProfileSetupRepository()
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _profile = MutableStateFlow<ProfileResponse?>(null)
    val profile: StateFlow<ProfileResponse?> = _profile.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.fetchProfile()
            _isLoading.value = false
            result.fold(
                onSuccess = { prof ->
                    _profile.value = prof
                },
                onFailure = { err ->
                    _errorMessage.value = err.localizedMessage
                }
            )
        }
    }

    fun saveProfile(
        fullName: String,
        whatsapp: String,
        domicile: String,
        education: String,
        desiredPositionsStr: String,
        preferredLocationsStr: String,
        applicationEmail: String,
        onSuccess: () -> Unit
    ) {
        if (fullName.isBlank() || whatsapp.isBlank() || domicile.isBlank() || education.isBlank() || applicationEmail.isBlank()) {
            _errorMessage.value = "Harap isi semua field wajib bertanda (*)."
            return
        }

        val desiredPositionsList = desiredPositionsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val preferredLocationsList = preferredLocationsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            val request = ProfileRequest(
                fullName = fullName,
                whatsapp = whatsapp,
                domicile = domicile,
                education = education,
                desiredPositions = desiredPositionsList,
                preferredLocations = preferredLocationsList,
                applicationEmail = applicationEmail
            )

            val result = repository.saveProfile(request)
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    _successMessage.value = "Profil berhasil disimpan."
                    onSuccess()
                },
                onFailure = { err ->
                    _errorMessage.value = err.localizedMessage ?: "Gagal menyimpan profil. Coba lagi."
                }
            )
        }
    }
}

