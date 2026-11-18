package com.equipo2.vinilos.ui.artists.albums

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.AlbumCandidate
import com.equipo2.vinilos.data.model.Artist
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.databinding.FragmentAlbumAddBinding
import com.equipo2.vinilos.ui.common.buttonProgress
import com.equipo2.vinilos.ui.common.collectWhileVisible
import com.equipo2.vinilos.ui.common.keepSnackbarsAbove
import com.equipo2.vinilos.ui.common.releaseTextFieldsOnDestroyView
import com.equipo2.vinilos.ui.common.showSnackbar
import com.equipo2.vinilos.ui.state.UiState
import com.equipo2.vinilos.ui.state.showEmpty
import com.equipo2.vinilos.ui.state.showError
import com.equipo2.vinilos.ui.state.showLoading
import kotlinx.coroutines.flow.combine

/** Por qué el listado puede no tener qué mostrar: el artista ya no existe o ya tiene todos los álbumes. */
private data class Reasons(val artistMissing: Boolean, val allAlreadyAdded: Boolean)

/** Lo que muestra el listado: los álbumes, el elegido, si se está enviando, la búsqueda y por qué no hay qué mostrar. */
private data class ListScreen(
    val state: UiState<List<AlbumCandidate>>,
    val selected: AlbumCandidate?,
    val submitting: Boolean,
    val query: String,
    val reasons: Reasons,
)

/**
 * Elegir el álbum que se agrega a un músico o a una banda (HU15): buscador, lista de selección única y «Elige un álbum».
 * Con una banda, avisa que reemplaza a los intérpretes del álbum y pide confirmarlo antes de enviar.
 */
class AlbumAddFragment : Fragment(R.layout.fragment_album_add) {

    private val artistId: Int by lazy { requireArguments().getInt(ARG_ARTIST_ID) }
    private val artistType: ArtistType by lazy { ArtistType.valueOf(requireNotNull(requireArguments().getString(ARG_ARTIST_TYPE))) }
    private val artistName: String by lazy { requireArguments().getString(ARG_ARTIST_NAME).orEmpty() }

    private val viewModel: AlbumAddViewModel by viewModels { AlbumAddViewModel.factory(artistId, artistType) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentAlbumAddBinding.bind(view)
        releaseTextFieldsOnDestroyView(view)
        binding.albumAddSubtitle.text = getString(R.string.album_add_subtitle, artistName)
        // Al agregar a una banda, la banda reemplaza a los intérpretes del álbum, así que se avisa desde el principio
        binding.albumAddBandNotice.isVisible = artistType == ArtistType.BAND
        setUpSearch(binding)
        setUpList(binding)
        setUpSubmitting(binding)
        setUpEvents(binding)

        viewLifecycleOwner.collectWhileVisible(viewModel.missingSelection) { missing ->
            binding.albumRequiredError.isVisible = missing
        }

        // Cancelar y la X de arriba vuelven al detalle sin guardar nada
        binding.albumAddCancelButton.setOnClickListener { findNavController().navigateUp() }
        binding.albumAddButton.setOnClickListener {
            hideKeyboard(binding)
            viewModel.add()
        }
    }

    /** Filtra mientras se escribe. El campo recuerda lo escrito al girar, y al volver a escribirlo el ViewModel ya lo tiene. */
    private fun setUpSearch(binding: FragmentAlbumAddBinding) {
        binding.albumSearchInput.doAfterTextChanged { text -> viewModel.search(text.toString()) }
        binding.albumSearchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) hideKeyboard(binding)
            actionId == EditorInfo.IME_ACTION_SEARCH
        }
    }

    /**
     * Filas con cada álbum y el elegido resaltado, el conteo, y los estados de carga, vacío, sin resultados y error con Reintentar.
     * Si el artista ya no existe se dice eso en lugar del error, porque reintentar no lo arregla.
     */
    private fun setUpList(binding: FragmentAlbumAddBinding) {
        val adapter = AlbumCandidateAdapter { candidate -> select(binding, candidate) }
        binding.albumAddList.layoutManager = LinearLayoutManager(requireContext())
        binding.albumAddList.adapter = adapter
        // Al elegir, la fila cambia en su lugar sin el fundido, que la hacía parpadear
        (binding.albumAddList.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        // La búsqueda va aparte porque dos búsquedas sin resultados dan la misma lista vacía y el mensaje debe mostrar la última
        val reasons = combine(viewModel.artistMissing, viewModel.allAlreadyAdded, ::Reasons)
        val screen = combine(viewModel.state, viewModel.selected, viewModel.isSubmitting, viewModel.query, reasons, ::ListScreen)
        viewLifecycleOwner.collectWhileVisible(screen) { (state, selected, submitting, query, reasons) ->
            val noResults = state is UiState.Success && state.data.isEmpty()
            when (state) {
                UiState.Loading ->
                    binding.albumAddLoading.showLoading(getString(R.string.album_add_loading), R.layout.view_skeleton_rows)
                UiState.Empty -> if (reasons.allAlreadyAdded) {
                    binding.albumAddMessage.showEmpty(
                        getString(R.string.album_add_all_added_title),
                        getString(R.string.album_add_all_added_body),
                        R.drawable.ic_album,
                    )
                } else {
                    binding.albumAddMessage.showEmpty(
                        getString(R.string.album_add_empty_title),
                        getString(R.string.album_add_empty_body),
                        R.drawable.ic_album,
                    )
                }
                is UiState.Error -> if (reasons.artistMissing) {
                    binding.albumAddMessage.showEmpty(
                        getString(R.string.album_add_artist_missing_title),
                        getString(R.string.album_add_artist_missing_body),
                        R.drawable.ic_mic,
                    )
                } else {
                    binding.albumAddMessage.showError(getString(R.string.album_add_object), state.reason) { viewModel.retry() }
                }
                is UiState.Success -> {
                    adapter.submitList(
                        state.data.map { candidate ->
                            val isSelected = candidate == selected
                            AlbumRow(
                                candidate,
                                selected = isSelected,
                                enabled = !submitting,
                                replaced = if (isSelected) candidate.performersReplacedBy(artistId, artistType) else emptyList(),
                            )
                        },
                    )
                    if (noResults) binding.albumAddMessage.showEmpty(
                        getString(R.string.album_add_no_results_title),
                        getString(R.string.album_add_no_results_body, query.trim()),
                        R.drawable.ic_album,
                    )
                }
            }
            showCount(binding, state, query, reasons.allAlreadyAdded)

            // Buscador y botones se quedan en el vacío y sin resultados, así se puede cambiar la búsqueda o cancelar
            val showsContent = state is UiState.Success || state == UiState.Empty
            binding.albumAddHeader.isVisible = showsContent
            binding.albumAddFooter.isVisible = showsContent
            binding.albumAddLoading.root.isVisible = state == UiState.Loading
            binding.albumAddMessage.root.isVisible = state == UiState.Empty || state is UiState.Error || noResults
            binding.albumAddList.isVisible = state is UiState.Success && !noResults
        }
    }

    /**
     * Sin búsqueda, cuántos álbumes hay en el catálogo, y al buscar, cuántos coinciden.
     * Si el artista ya tiene todos no se muestra, porque «0 álbumes en el catálogo» sería falso.
     */
    private fun showCount(binding: FragmentAlbumAddBinding, state: UiState<List<AlbumCandidate>>, query: String, allAlreadyAdded: Boolean) {
        val count = when (state) {
            is UiState.Success -> state.data.size
            UiState.Empty -> 0.takeUnless { allAlreadyAdded }
            else -> null
        }
        binding.albumAddCount.isVisible = count != null
        if (count == null) return
        val plurals = if (query.isBlank()) R.plurals.album_list_count else R.plurals.album_add_result_count
        binding.albumAddCount.text = resources.getQuantityString(plurals, count, count)
    }

    /** Mientras se envía: barra de progreso, «Agregando…» y nada se puede tocar, para no enviar dos veces. */
    private fun setUpSubmitting(binding: FragmentAlbumAddBinding) {
        val progress = requireContext().buttonProgress()

        viewLifecycleOwner.collectWhileVisible(viewModel.isSubmitting) { submitting ->
            binding.albumAddSubmittingProgress.isVisible = submitting
            binding.albumSearchField.isEnabled = !submitting
            binding.albumAddCancelButton.isEnabled = !submitting
            binding.albumAddButton.isEnabled = !submitting
            binding.albumAddButton.icon = progress.takeIf { submitting }
            binding.albumAddButton.setText(if (submitting) R.string.album_add_saving else R.string.action_add)
        }
    }

    /**
     * Si la banda reemplazará intérpretes se pide confirmar, y al aceptar se envía sin volver a preguntar.
     * Si se agregó vuelve al detalle con el álbum, y si falló avisa encima de los botones y conserva el elegido.
     */
    private fun setUpEvents(binding: FragmentAlbumAddBinding) {
        AlbumReplaceDialog.onReplaceConfirmed(this) { viewModel.confirmAdd() }
        keepSnackbarsAbove(binding.albumAddFooter)

        viewLifecycleOwner.collectWhileVisible(viewModel.events) { event ->
            when (event) {
                is AlbumAddEvent.ConfirmReplace -> AlbumReplaceDialog.show(
                    host = this,
                    albumName = event.album.name,
                    performers = requireContext().performerNames(event.replaced),
                    performerCount = event.replaced.size,
                    bandName = artistName,
                )
                is AlbumAddEvent.Added -> returnToArtist(event.album.id)
                is AlbumAddEvent.Failed -> showFailure(event.failure)
            }
        }
    }

    /** El aviso no lleva botón, como en el prototipo. La selección se conserva para volver a tocar Agregar. */
    private fun showFailure(failure: AlbumAddFailure) {
        val message = when (failure) {
            AlbumAddFailure.CONNECTION -> R.string.error_album_add_connection
            // El ViewModel ya está recargando el listado
            AlbumAddFailure.NOT_FOUND -> R.string.error_album_add_not_found
            AlbumAddFailure.SERVER -> R.string.error_album_add_failed
        }
        showSnackbar(getString(message), isError = true)
    }

    /** Vuelve al detalle con el id del álbum agregado, para que lo muestre primero y marcado como «Nuevo». */
    private fun returnToArtist(albumId: Int) {
        val navController = findNavController()
        navController.previousBackStackEntry?.savedStateHandle?.set(ADDED_ALBUM, albumId)
        navController.navigateUp()
    }

    private fun select(binding: FragmentAlbumAddBinding, candidate: AlbumCandidate) {
        hideKeyboard(binding)
        viewModel.select(candidate)
    }

    // Al elegir o agregar se baja el teclado para que se vean la lista y los botones
    private fun hideKeyboard(binding: FragmentAlbumAddBinding) {
        binding.albumSearchInput.clearFocus()
        val keyboard = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        keyboard.hideSoftInputFromWindow(binding.albumSearchInput.windowToken, 0)
    }

    companion object {
        /** Clave con la que el detalle del artista recibe el id del álbum agregado. */
        const val ADDED_ALBUM = "addedAlbum"

        /** Deben coincidir con los argumentos de albumAddFragment en nav_graph.xml. */
        const val ARG_ARTIST_ID = "artistId"
        const val ARG_ARTIST_TYPE = "artistType"
        const val ARG_ARTIST_NAME = "artistName"

        fun args(artist: Artist) = bundleOf(ARG_ARTIST_ID to artist.id, ARG_ARTIST_TYPE to artist.type.name, ARG_ARTIST_NAME to artist.name)
    }
}
