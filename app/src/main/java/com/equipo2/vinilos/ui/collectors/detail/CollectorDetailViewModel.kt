package com.equipo2.vinilos.ui.collectors.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.equipo2.vinilos.VinilosApplication
import com.equipo2.vinilos.data.model.CollectorDetail
import com.equipo2.vinilos.data.repository.CollectorRepository
import com.equipo2.vinilos.ui.state.PullRefresh
import com.equipo2.vinilos.ui.state.UiState
import com.equipo2.vinilos.ui.state.toErrorReason
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Detalle de un coleccionista (HU06): carga, éxito o error, y se conserva al girar. */
class CollectorDetailViewModel(
    private val repository: CollectorRepository,
    private val collectorId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<CollectorDetail>>(UiState.Loading)
    val state: StateFlow<UiState<CollectorDetail>> = _state.asStateFlow()

    private var loading: Job? = null

    val pullRefresh = PullRefresh(viewModelScope)

    init {
        load()
    }

    fun retry() = load()

    /** Recarga al deslizar hacia abajo. El coleccionista sigue a la vista mientras llegan sus datos nuevos. */
    fun refresh() {
        // Recargar siempre va a la API
        repository.clearCache()
        if (_state.value !is UiState.Success) return load()
        if (loading?.isActive == true) return
        loading = pullRefresh.launch({ repository.getCollectorDetail(collectorId) }) { detail ->
            _state.value = UiState.Success(detail)
        }
    }

    private fun load() {
        // Si ya hay una petición en curso no se lanza otra
        if (loading?.isActive == true) return

        _state.value = UiState.Loading
        loading = viewModelScope.launch {
            _state.value = repository.getCollectorDetail(collectorId).fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.toErrorReason()) },
            )
        }
    }

    companion object {
        fun factory(collectorId: Int) = viewModelFactory {
            initializer { CollectorDetailViewModel(VinilosApplication.container.collectorRepository, collectorId) }
        }
    }
}
