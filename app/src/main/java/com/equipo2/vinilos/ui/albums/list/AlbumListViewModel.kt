package com.equipo2.vinilos.ui.albums.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Álbum del catálogo, con el distintivo «Nuevo» si se acaba de crear. */
data class AlbumItem(val album: Album, val isNew: Boolean)

/** Catálogo de álbumes (HU01): carga, éxito, vacío o error, y se conserva al girar. */
class AlbumListViewModel(
    private val repository: AlbumRepository,
    private val savedState: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    private val catalog = MutableStateFlow<UiState<List<Album>>>(UiState.Loading)

    // Ids creados desde esta pantalla, del más antiguo al más reciente
    private val newAlbumIds = savedState.getStateFlow(NEW_ALBUM_IDS, IntArray(0))

    /** Estado de la pantalla, con los álbumes nuevos primero y el más reciente arriba. */
    val state: StateFlow<UiState<List<AlbumItem>>> = combine(catalog, newAlbumIds) { catalog, newIds ->
        when (catalog) {
            is UiState.Success -> UiState.Success(itemsOf(catalog.data, newIds))
            UiState.Loading -> UiState.Loading
            UiState.Empty -> UiState.Empty
            is UiState.Error -> catalog
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private var loading: Job? = null

    val pullRefresh = PullRefresh(viewModelScope)

    init {
        load()
    }

    fun retry() = load()

    /** Recarga al deslizar hacia abajo. Los álbumes siguen a la vista mientras llegan los nuevos. */
    fun refresh() {
        if (catalog.value !is UiState.Success) return load(force = true)
        if (loading?.isActive == true) return
        loading = pullRefresh.launch({ repository.getAlbums() }) { albums -> catalog.value = catalogOf(albums) }
    }

    /** Marca el álbum recién creado como «Nuevo» y recarga para que aparezca. */
    fun onAlbumCreated(albumId: Int) {
        savedState[NEW_ALBUM_IDS] = (newAlbumIds.value.filter { it != albumId } + albumId).toIntArray()
        load(force = true)
    }

    private fun load(force: Boolean = false) {
        // Si ya hay una petición en curso no se lanza otra, salvo que haya que traer datos nuevos
        if (!force && loading?.isActive == true) return
        loading?.cancel()

        catalog.value = UiState.Loading
        loading = viewModelScope.launch {
            catalog.value = repository.getAlbums().fold(
                onSuccess = ::catalogOf,
                onFailure = { UiState.Error(it.toErrorReason()) },
            )
        }
    }

    private fun catalogOf(albums: List<Album>): UiState<List<Album>> = if (albums.isEmpty()) UiState.Empty else UiState.Success(albums)

    private fun itemsOf(albums: List<Album>, newIds: IntArray): List<AlbumItem> {
        val (fresh, rest) = albums.partition { it.id in newIds }
        val newestFirst = fresh.sortedByDescending { newIds.indexOf(it.id) }
        return newestFirst.map { AlbumItem(it, isNew = true) } + rest.map { AlbumItem(it, isNew = false) }
    }

    companion object {
        private const val NEW_ALBUM_IDS = "newAlbumIds"

        val Factory = viewModelFactory {
            initializer { AlbumListViewModel(VinilosApplication.container.albumRepository, createSavedStateHandle()) }
        }
    }
}
