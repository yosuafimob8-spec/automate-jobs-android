package com.example.automatesmartbot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automatesmartbot.data.model.JobDto
import com.example.automatesmartbot.data.model.JobFilters
import com.example.automatesmartbot.data.repository.JobRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JobViewModel(
    private val repository: JobRepository = JobRepository()
) : ViewModel() {

    private val _jobs = MutableStateFlow<List<JobDto>>(emptyList())
    val jobs: StateFlow<List<JobDto>> = _jobs.asStateFlow()

    private val _totalJobs = MutableStateFlow(0)
    val totalJobs: StateFlow<Int> = _totalJobs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var searchJob: Job? = null

    fun fetchJobs(position: String?, location: String?) {
        if (!JobFilters.isValid(position, location)) {
            searchJob?.cancel()
            _isLoading.value = false
            _jobs.value = emptyList()
            _totalJobs.value = 0
            _errorMessage.value = JobFilters.VALIDATION_MESSAGE
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.getJobs(position, location)
            _isLoading.value = false
            result.fold(
                onSuccess = { res ->
                    _jobs.value = res.jobs ?: emptyList()
                    _totalJobs.value = res.total ?: (res.jobs?.size ?: 0)
                },
                onFailure = { err ->
                    _jobs.value = emptyList()
                    _totalJobs.value = 0
                    _errorMessage.value = err.localizedMessage ?: "Gagal memuat lowongan"
                }
            )
        }
    }
}
