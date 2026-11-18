package com.equipo2.vinilos.ui.artists.albums

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
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
import com.equipo2.vinilos.ui.common.showSnackbar
import com.equipo2.vinilos.ui.state.UiState
import com.equipo2.vinilos.ui.state.showEmpty
import com.equipo2.vinilos.ui.state.showError
import com.equipo2.vinilos.ui.state.showLoading
import kotlinx.coroutines.flow.combine

/** Lo que muestra el listado: los álbumes, el elegido, si se está enviando y por qué no hay qué mostrar. */
private data class ListScreen(
    val state: UiState<List<AlbumCandidate>>,
    val selected: AlbumCandidate?,
    val submitting: Boolean,
    val artistMissing: Boolean,
    val allAlreadyAdded: Boolean,
)

/** Elegir el álbum que se agrega a un músico o a una banda (HU15): lista de selección única y «Elige un álbum». */
class AlbumAddFragment : Fragment(R.layout.fragment_album_add) {

    private val viewModel: AlbumAddViewModel by viewModels {
        val args = requireArguments()
        AlbumAddViewModel.factory(args.getInt(ARG_ARTIST_ID), ArtistType.valueOf(requireNotNull(args.getString(ARG_ARTIST_TYPE))))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentAlbumAddBinding.bind(view)
        binding.albumAddSubtitle.text = getString(R.string.album_add_subtitle, requireArguments().getString(ARG_ARTIST_NAME))
        setUpList(binding)
        setUpSubmitting(binding)
        setUpEvents(binding)

        viewLifecycleOwner.collectWhileVisible(viewModel.missingSelection) { missing ->
            binding.albumRequiredError.isVisible = missing
        }

        // Cancelar y la X de arriba vuelven al detalle sin guardar nada
        binding.albumAddCancelButton.setOnClickListener { findNavController().navigateUp() }
        binding.albumAddButton.setOnClickListener { viewModel.add() }
    }

    /**
     * Filas con cada álbum y el elegido resaltado, y los estados de carga, vacío y error con Reintentar.
     * Si el artista ya no existe se dice eso en lugar del error, porque reintentar no lo arregla.
     */
    private fun setUpList(binding: FragmentAlbumAddBinding) {
        val adapter = AlbumCandidateAdapter { candidate -> viewModel.select(candidate) }
        binding.albumAddList.layoutManager = LinearLayoutManager(requireContext())
        binding.albumAddList.adapter = adapter
        // Al elegir, la fila cambia en su lugar sin el fundido, que la hacía parpadear
        (binding.albumAddList.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        val screen = combine(
            viewModel.state,
            viewModel.selected,
            viewModel.isSubmitting,
            viewModel.artistMissing,
            viewModel.allAlreadyAdded,
            ::ListScreen,
        )
        viewLifecycleOwner.collectWhileVisible(screen) { (state, selected, submitting, artistMissing, allAlreadyAdded) ->
            when (state) {
                UiState.Loading ->
                    binding.albumAddLoading.showLoading(getString(R.string.album_add_loading), R.layout.view_skeleton_rows)
                UiState.Empty -> if (allAlreadyAdded) {
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
                is UiState.Error -> if (artistMissing) {
                    binding.albumAddMessage.showEmpty(
                        getString(R.string.album_add_artist_missing_title),
                        getString(R.string.album_add_artist_missing_body),
                        R.drawable.ic_mic,
                    )
                } else {
                    binding.albumAddMessage.showError(getString(R.string.album_add_object), state.reason) { viewModel.retry() }
                }
                is UiState.Success ->
                    adapter.submitList(state.data.map { AlbumRow(it, selected = it == selected, enabled = !submitting) })
            }

            // El texto y los botones se quedan en el vacío, así se puede cancelar desde ahí
            val showsContent = state is UiState.Success || state == UiState.Empty
            binding.albumAddHeader.isVisible = showsContent
            binding.albumAddFooter.isVisible = showsContent
            binding.albumAddLoading.root.isVisible = state == UiState.Loading
            binding.albumAddMessage.root.isVisible = state == UiState.Empty || state is UiState.Error
            binding.albumAddList.isVisible = state is UiState.Success
        }
    }

    /** Mientras se envía: barra de progreso, «Agregando…» y nada se puede tocar, para no enviar dos veces. */
    private fun setUpSubmitting(binding: FragmentAlbumAddBinding) {
        val progress = requireContext().buttonProgress()

        viewLifecycleOwner.collectWhileVisible(viewModel.isSubmitting) { submitting ->
            binding.albumAddSubmittingProgress.isVisible = submitting
            binding.albumAddCancelButton.isEnabled = !submitting
            binding.albumAddButton.isEnabled = !submitting
            binding.albumAddButton.icon = progress.takeIf { submitting }
            binding.albumAddButton.setText(if (submitting) R.string.album_add_saving else R.string.action_add)
        }
    }

    /** Si se agregó vuelve al detalle con el álbum, y si falló avisa encima de los botones y conserva el elegido. */
    private fun setUpEvents(binding: FragmentAlbumAddBinding) {
        keepSnackbarsAbove(binding.albumAddFooter)

        viewLifecycleOwner.collectWhileVisible(viewModel.events) { event ->
            when (event) {
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
