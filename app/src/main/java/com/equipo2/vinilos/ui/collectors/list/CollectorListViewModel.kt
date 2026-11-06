package com.equipo2.vinilos.ui.collectors.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.equipo2.vinilos.VinilosApplication
import com.equipo2.vinilos.data.model.Collector
import com.equipo2.vinilos.data.repository.CollectorRepository
import com.equipo2.vinilos.ui.state.PullRefresh
import com.equipo2.vinilos.ui.state.UiState
import com.equipo2.vinilos.ui.state.toErrorReason
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale

/** Listado de coleccionistas (HU05): carga, éxito, vacío o error, y se conserva al girar. */
class CollectorListViewModel(private val repository: CollectorRepository) : ViewModel() {

    private val collectors = MutableStateFlow<UiState<List<Collector>>>(UiState.Loading)

    val state: StateFlow<UiState<List<Collector>>> = collectors.asStateFlow()

    /** Total de coleccionistas para el contador: 0 mientras carga, en el vacío o en el error. */
    val count: StateFlow<Int> = collectors
        .map { (it as? UiState.Success)?.data?.size ?: 0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private var loading: Job? = null

    val pullRefresh = PullRefresh(viewModelScope)

    init {
        load()
    }

    fun retry() = load()

    /** Recarga al deslizar hacia abajo. Los coleccionistas siguen a la vista mientras llegan los nuevos. */
    fun refresh() {
        if (collectors.value !is UiState.Success) return load()
        if (loading?.isActive == true) return
        loading = pullRefresh.launch({ repository.getCollectors() }) { collectors -> this.collectors.value = collectorsOf(collectors) }
    }

    private fun load() {
        // Si ya hay una petición en curso no se lanza otra
        if (loading?.isActive == true) return

        collectors.value = UiState.Loading
        loading = viewModelScope.launch {
            collectors.value = repository.getCollectors().fold(
                onSuccess = ::collectorsOf,
                onFailure = { UiState.Error(it.toErrorReason()) },
            )
        }
    }

    private fun collectorsOf(collectors: List<Collector>): UiState<List<Collector>> =
        if (collectors.isEmpty()) UiState.Empty else UiState.Success(sortedByName(collectors))

    // En orden alfabético sin importar tildes ni mayúsculas
    private fun sortedByName(collectors: List<Collector>): List<Collector> {
        val collator = Collator.getInstance(Locale.forLanguageTag("es")).apply { strength = Collator.PRIMARY }
        return collectors.sortedWith(compareBy(collator) { it.name })
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { CollectorListViewModel(VinilosApplication.container.collectorRepository) }
        }
    }
}
