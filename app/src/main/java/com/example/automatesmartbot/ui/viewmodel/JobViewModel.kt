package com.example.automatesmartbot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automatesmartbot.data.model.JobsV2Job
import com.example.automatesmartbot.data.repository.JobsV2Repository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JobViewModel(
    private val repository: JobsV2Repository = JobsV2Repository()
) : ViewModel() {

    private val _jobs = MutableStateFlow<List<JobsV2Job>>(emptyList())
    val jobs: StateFlow<List<JobsV2Job>> = _jobs.asStateFlow()

    private val _totalJobs = MutableStateFlow(0)
    val totalJobs: StateFlow<Int> = _totalJobs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var searchJob: Job? = null
    private var activePositions: List<String>? = null
    private var activeLocations: List<String>? = null
    private var activeSources: List<String>? = null
    private var activeCategories: List<String>? = null
    private var snapshotId: String? = null
    private var nextPage: Int? = null

    fun fetchJobs(positions: List<String>? = null, locations: List<String>? = null, sources: List<String>? = null, categories: List<String>? = null) {
        searchJob?.cancel()
        activePositions = positions?.takeIf { it.isNotEmpty() }
        activeLocations = locations?.takeIf { it.isNotEmpty() }
        // V2 uses company_catalog exclusively for spontaneous references. Restrict
        // the app search to documented LIVE source categories.
        activeSources = sources?.takeIf { it.isNotEmpty() }
            ?: listOf("company", "bumn", "government", "healthcare", "education", "job_portal")
        activeCategories = categories?.takeIf { it.isNotEmpty() }
        snapshotId = null
        nextPage = null
        _jobs.value = emptyList()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.search(activePositions, activeLocations, activeSources, activeCategories, 1, null)
            _isLoading.value = false
            result.fold(
                onSuccess = { res ->
                    _jobs.value = res.jobs
                    _totalJobs.value = res.total
                    snapshotId = res.cache.snapshotId
                    nextPage = res.pagination.nextPage
                },
                onFailure = { err ->
                    _jobs.value = emptyList()
                    _totalJobs.value = 0
                    _errorMessage.value = err.localizedMessage ?: "Gagal memuat lowongan"
                }
            )
        }
    }

    fun loadNextPage() {
        val page = nextPage ?: return
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            val result = repository.search(activePositions, activeLocations, activeSources, activeCategories, page, snapshotId)
            _isLoading.value = false
            result.onSuccess { res ->
                _jobs.value += res.jobs
                nextPage = res.pagination.nextPage
            }.onFailure { _errorMessage.value = it.localizedMessage }
        }
    }

    val hasMore: Boolean get() = nextPage != null
}
