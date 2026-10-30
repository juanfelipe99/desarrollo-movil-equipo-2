package com.equipo2.vinilos.ui.albums.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.equipo2.vinilos.VinilosApplication
import com.equipo2.vinilos.data.model.Album
import com.equipo2.vinilos.data.repository.AlbumRepository
import com.equipo2.vinilos.ui.state.PullRefresh
import com.equipo2.vinilos.ui.state.UiState
import com.equipo2.vinilos.ui.state.toErrorReason
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Detalle de un álbum (HU02): carga, éxito o error, y se conserva al girar. */
class AlbumDetailViewModel(
    private val repository: AlbumRepository,
    private val albumId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<Album>>(UiState.Loading)
    val state: StateFlow<UiState<Album>> = _state.asStateFlow()

    private var loading: Job? = null

    val pullRefresh = PullRefresh(viewModelScope)

    init {
        load()
    }

    fun retry() = load()

    /** Recarga al deslizar hacia abajo. El álbum sigue a la vista mientras llegan sus datos nuevos. */
    fun refresh() {
        if (_state.value !is UiState.Success) return load()
        if (loading?.isActive == true) return
        loading = pullRefresh.launch({ repository.getAlbumById(albumId) }) { album -> _state.value = UiState.Success(album) }
    }

    private fun load() {
        // Si ya hay una petición en curso no se lanza otra
        if (loading?.isActive == true) return

        _state.value = UiState.Loading
        loading = viewModelScope.launch {
            _state.value = repository.getAlbumById(albumId).fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.toErrorReason()) },
            )
        }
    }

    companion object {
        fun factory(albumId: Int) = viewModelFactory {
            initializer { AlbumDetailViewModel(VinilosApplication.container.albumRepository, albumId) }
        }
    }
}
