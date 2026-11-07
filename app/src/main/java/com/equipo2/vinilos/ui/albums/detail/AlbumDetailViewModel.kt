package com.equipo2.vinilos.ui.albums.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.equipo2.vinilos.VinilosApplication
import com.equipo2.vinilos.data.model.Album
import com.equipo2.vinilos.data.model.Track
import com.equipo2.vinilos.data.repository.AlbumRepository
import com.equipo2.vinilos.ui.state.PullRefresh
import com.equipo2.vinilos.ui.state.UiState
import com.equipo2.vinilos.ui.state.toErrorReason
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Detalle de un álbum (HU02): carga, éxito o error, y los tracks agregados (HU08). Se conserva al girar. */
class AlbumDetailViewModel(
    private val repository: AlbumRepository,
    private val albumId: Int,
    private val savedState: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<Album>>(UiState.Loading)
    val state: StateFlow<UiState<Album>> = _state.asStateFlow()

    /** Ids de los tracks agregados desde esta pantalla, que se marcan como «Nuevo». */
    val newTrackIds: StateFlow<IntArray> = savedState.getStateFlow(NEW_TRACK_IDS, IntArray(0))

    private var loading: Job? = null

    val pullRefresh = PullRefresh(viewModelScope)

    init {
        load()
    }

    fun retry() = load()

    /** Agrega el track al final de la lista sin volver a pedir el álbum, y lo marca como «Nuevo». */
    fun onTrackAdded(track: Track) {
        savedState[NEW_TRACK_IDS] = newTrackIds.value + track.id
        val album = (_state.value as? UiState.Success)?.data ?: return
        if (album.tracks.none { it.id == track.id }) _state.value = UiState.Success(album.copy(tracks = album.tracks + track))
    }

    /** Recarga al deslizar hacia abajo. El álbum sigue a la vista mientras llegan sus datos nuevos. */
    fun refresh() {
        // Recargar siempre va a la API
        repository.clearCache()
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
        private const val NEW_TRACK_IDS = "newTrackIds"

        fun factory(albumId: Int) = viewModelFactory {
            initializer { AlbumDetailViewModel(VinilosApplication.container.albumRepository, albumId, createSavedStateHandle()) }
        }
    }
}
