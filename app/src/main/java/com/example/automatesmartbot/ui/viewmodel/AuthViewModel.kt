package com.example.automatesmartbot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automatesmartbot.data.model.LoginRequest
import com.example.automatesmartbot.data.model.RegisterRequest
import com.example.automatesmartbot.data.repository.AuthRepository
import com.example.automatesmartbot.data.repository.ProfileSetupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun login(
        email: String,
        password: String,
        onLoggedIn: (profileCompleted: Boolean?) -> Unit
    ) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Email dan password tidak boleh kosong."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.login(LoginRequest(email, password))
            result.fold(
                onSuccess = { _ ->
                    // Onboarding is only driven by the server profile state after a
                    // successful login. A null result means the check failed, never
                    // that the profile is complete.
                    val profileCompleted = ProfileSetupRepository().checkServerProfileCompletion()
                    _isLoading.value = false
                    _successMessage.value = "Login berhasil!"
                    onLoggedIn(profileCompleted)
                },
                onFailure = { err ->
                    _isLoading.value = false
                    _errorMessage.value = err.localizedMessage ?: "Terjadi kesalahan. Coba lagi."
                }
            )
        }
    }

    fun register(email: String, password: String, confirmPassword: String, onRegistered: () -> Unit) {
        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _errorMessage.value = "Semua field harus diisi."
            return
        }
        if (password != confirmPassword) {
            _errorMessage.value = "Konfirmasi password tidak cocok."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.register(RegisterRequest(email, password))
            _isLoading.value = false
            result.fold(
                onSuccess = { _ ->
                    _successMessage.value = "Registrasi berhasil! Silakan masuk."
                    onRegistered()
                },
                onFailure = { err ->
                    _errorMessage.value = err.localizedMessage ?: "Terjadi kesalahan. Coba lagi."
                }
            )
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}

