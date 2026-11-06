package com.equipo2.vinilos.ui.artists.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.equipo2.vinilos.VinilosApplication
import com.equipo2.vinilos.data.model.ArtistDetail
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.repository.ArtistRepository
import com.equipo2.vinilos.ui.state.PullRefresh
import com.equipo2.vinilos.ui.state.UiState
import com.equipo2.vinilos.ui.state.toErrorReason
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Detalle de un músico o de una banda (HU04): carga, éxito o error. Se conserva al girar. */
class ArtistDetailViewModel(
    private val repository: ArtistRepository,
    private val artistId: Int,
    private val artistType: ArtistType,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<ArtistDetail>>(UiState.Loading)
    val state: StateFlow<UiState<ArtistDetail>> = _state.asStateFlow()

    private var loading: Job? = null

    val pullRefresh = PullRefresh(viewModelScope)

    init {
        load()
    }

    fun retry() = load()

    /** Recarga al deslizar hacia abajo. El artista sigue a la vista mientras llegan sus datos nuevos. */
    fun refresh() {
        if (_state.value !is UiState.Success) return load()
        if (loading?.isActive == true) return
        loading = pullRefresh.launch({ repository.getArtistDetail(artistId, artistType) }) { detail ->
            _state.value = UiState.Success(detail)
        }
    }

    private fun load() {
        // Si ya hay una petición en curso no se lanza otra
        if (loading?.isActive == true) return

        _state.value = UiState.Loading
        loading = viewModelScope.launch {
            _state.value = repository.getArtistDetail(artistId, artistType).fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.toErrorReason()) },
            )
        }
    }

    companion object {
        fun factory(artistId: Int, artistType: ArtistType) = viewModelFactory {
            initializer { ArtistDetailViewModel(VinilosApplication.container.artistRepository, artistId, artistType) }
        }
    }
}
