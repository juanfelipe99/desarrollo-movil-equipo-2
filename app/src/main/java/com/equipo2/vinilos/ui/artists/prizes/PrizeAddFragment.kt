package com.equipo2.vinilos.ui.artists.prizes

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
import com.equipo2.vinilos.data.model.Artist
import com.equipo2.vinilos.data.model.ArtistPrize
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.model.PrizeCandidate
import com.equipo2.vinilos.databinding.FragmentPrizeAddBinding
import com.equipo2.vinilos.ui.artists.prizes.create.PrizeCreateFragment
import com.equipo2.vinilos.ui.common.buttonProgress
import com.equipo2.vinilos.ui.common.collectResult
import com.equipo2.vinilos.ui.common.collectWhileVisible
import com.equipo2.vinilos.ui.common.keepSnackbarsAbove
import com.equipo2.vinilos.ui.common.navigateIfPossible
import com.equipo2.vinilos.ui.common.reconnectDatePicker
import com.equipo2.vinilos.ui.common.releaseTextFieldsOnDestroyView
import com.equipo2.vinilos.ui.common.showError
import com.equipo2.vinilos.ui.common.showPastDatePicker
import com.equipo2.vinilos.ui.common.showSnackbar
import com.equipo2.vinilos.ui.common.toShortText
import com.equipo2.vinilos.ui.state.UiState
import com.equipo2.vinilos.ui.state.addEmptyNote
import com.equipo2.vinilos.ui.state.showEmpty
import com.equipo2.vinilos.ui.state.showError
import com.equipo2.vinilos.ui.state.showLoading
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/** Lo que muestra el listado: los premios, el elegido, si se está enviando, si el artista ya no existe y los recién creados. */
private data class ListScreen(
    val state: UiState<List<PrizeCandidate>>,
    val selectedId: Int?,
    val saving: Boolean,
    val artistMissing: Boolean,
    val newIds: IntArray,
)

/** Elegir el premio que recibió un artista y la fecha en que lo recibió (HU14). */
class PrizeAddFragment : Fragment(R.layout.fragment_prize_add) {

    private val viewModel: PrizeAddViewModel by viewModels {
        val args = requireArguments()
        PrizeAddViewModel.factory(args.getInt(ARG_ARTIST_ID), ArtistType.valueOf(requireNotNull(args.getString(ARG_ARTIST_TYPE))))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentPrizeAddBinding.bind(view)
        releaseTextFieldsOnDestroyView(view)
        binding.prizeAddSubtitle.text = getString(R.string.prize_add_subtitle, requireArguments().getString(ARG_ARTIST_NAME))
        binding.prizeEmpty.addEmptyNote(getString(R.string.prize_add_empty))
        setUpList(binding)
        setUpDate(binding)
        setUpErrors(binding)
        setUpSaving(binding)
        setUpEvents(binding)
        setUpCreatePrize(binding)

        binding.prizeCancelButton.setOnClickListener { findNavController().navigateUp() }
        binding.prizeSaveButton.setOnClickListener { save(binding) }
    }

    /**
     * Filas con cada premio y el elegido resaltado, y los estados de carga, vacío y error.
     * Si el artista ya no existe se dice eso en lugar del error, porque reintentar no lo arregla.
     */
    private fun setUpList(binding: FragmentPrizeAddBinding) {
        val adapter = PrizeCandidateAdapter { candidate -> viewModel.select(candidate) }
        binding.prizeList.layoutManager = LinearLayoutManager(requireContext())
        binding.prizeList.adapter = adapter
        // Al elegir, la fila cambia en su lugar sin el fundido, que la hacía parpadear
        (binding.prizeList.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        val screen = combine(
            viewModel.state, viewModel.selectedPrizeId, viewModel.isSaving, viewModel.artistMissing, viewModel.newPrizeIds, ::ListScreen,
        )
        viewLifecycleOwner.collectWhileVisible(screen) { (state, selectedId, saving, artistMissing, newIds) ->
            when (state) {
                UiState.Loading ->
                    binding.prizeLoading.showLoading(getString(R.string.prize_add_loading), R.layout.view_skeleton_rows)
                is UiState.Error -> if (artistMissing) {
                    binding.prizeMessage.showEmpty(
                        getString(R.string.prize_add_artist_missing_title),
                        getString(R.string.prize_add_artist_missing_body),
                        R.drawable.ic_mic,
                    )
                } else {
                    binding.prizeMessage.showError(getString(R.string.prize_add_object), state.reason) { viewModel.retry() }
                }
                is UiState.Success -> {
                    val last = state.data.lastIndex
                    adapter.submitList(
                        state.data.mapIndexed { index, candidate ->
                            PrizeRow(
                                candidate,
                                selected = candidate.prize.id == selectedId,
                                enabled = !saving,
                                isLast = index == last,
                                isNew = candidate.prize.id in newIds,
                            )
                        },
                    )
                }
                UiState.Empty -> Unit
            }

            // El formulario y los botones se quedan en el vacío, que dice que aún no hay premios
            val showsForm = state is UiState.Success || state == UiState.Empty
            binding.prizeScroll.isVisible = showsForm
            binding.prizeFooter.isVisible = showsForm
            binding.prizeCard.isVisible = state is UiState.Success
            binding.prizeEmpty.isVisible = state == UiState.Empty
            binding.prizeLoading.root.isVisible = state == UiState.Loading
            binding.prizeMessage.root.isVisible = state is UiState.Error
        }
    }

    /** El campo y su ícono abren el selector, que no deja elegir días futuros, y el campo muestra la fecha elegida. */
    private fun setUpDate(binding: FragmentPrizeAddBinding) {
        val onPicked = { date: LocalDate -> viewModel.setDate(date) }
        val openPicker = {
            showPastDatePicker(DATE_PICKER_TAG, R.string.premiation_date_picker_title, viewModel.date.value, onPicked)
        }
        binding.premiationDateInput.setOnClickListener { openPicker() }
        binding.premiationDateField.setEndIconOnClickListener { openPicker() }
        reconnectDatePicker(DATE_PICKER_TAG, onPicked)

        viewLifecycleOwner.collectWhileVisible(viewModel.date) { date -> binding.premiationDateInput.setText(date?.toShortText()) }
    }

    private fun setUpErrors(binding: FragmentPrizeAddBinding) {
        viewLifecycleOwner.collectWhileVisible(viewModel.prizeMissing) { missing -> binding.prizeRequiredError.isVisible = missing }
        viewLifecycleOwner.collectWhileVisible(viewModel.dateError) { error -> binding.premiationDateField.showError(error) }
    }

    /** Mientras guarda: barra de progreso, «Guardando…» y nada se puede tocar, para no enviar dos veces. */
    private fun setUpSaving(binding: FragmentPrizeAddBinding) {
        val progress = requireContext().buttonProgress()

        viewLifecycleOwner.collectWhileVisible(viewModel.isSaving) { saving ->
            binding.prizeSavingProgress.isVisible = saving
            binding.premiationDateField.isEnabled = !saving
            binding.prizeCancelButton.isEnabled = !saving
            binding.prizeCreateButton.isEnabled = !saving
            binding.prizeSaveButton.isEnabled = !saving
            binding.prizeSaveButton.icon = progress.takeIf { saving }
            binding.prizeSaveButton.setText(if (saving) R.string.prize_add_saving else R.string.action_save)
        }
    }

    /** Si se agregó vuelve al detalle con el premio, y si falló avisa encima de los botones y conserva lo elegido. */
    private fun setUpEvents(binding: FragmentPrizeAddBinding) {
        keepSnackbarsAbove(binding.prizeFooter)

        viewLifecycleOwner.collectWhileVisible(viewModel.events) { event ->
            when (event) {
                is PrizeAddEvent.Added -> returnToArtist(event.prize)
                is PrizeAddEvent.Failed -> showFailure(binding, event.failure)
            }
        }
    }

    /**
     * «Crear premio nuevo» abre Crear premio (HU13). Al volver con el premio creado, aparece primero, elegido y marcado
     * como «Nuevo», con el aviso «Premio creado».
     */
    private fun setUpCreatePrize(binding: FragmentPrizeAddBinding) {
        binding.prizeCreateButton.setOnClickListener { findNavController().navigateIfPossible(R.id.action_prizeAdd_to_prizeCreate) }

        collectResult<Bundle>(PrizeCreateFragment.CREATED_PRIZE) { created ->
            viewModel.onPrizeCreated(PrizeCreateFragment.prizeOf(created))
            showSnackbar(getString(R.string.snackbar_prize_created))
            scrollTo(binding, null)
        }
    }

    /** El aviso no lleva botón, como en el prototipo. Una fecha que la API no acepta se dice en su campo. */
    private fun showFailure(binding: FragmentPrizeAddBinding, failure: PrizeAddFailure) {
        val message = when (failure) {
            PrizeAddFailure.CONNECTION -> R.string.error_prize_add_connection
            // El ViewModel ya está recargando el listado
            PrizeAddFailure.NOT_FOUND -> R.string.error_prize_add_not_found
            PrizeAddFailure.SERVER -> R.string.error_prize_add_failed
            PrizeAddFailure.INVALID_DATE -> {
                scrollTo(binding, binding.premiationDateField)
                return
            }
        }
        showSnackbar(getString(message), isError = true)
    }

    /** Vuelve al detalle con el premio agregado, que lo pone primero, lo marca como «Nuevo» y muestra «Premio agregado». */
    private fun returnToArtist(prize: ArtistPrize) {
        val navController = findNavController()
        navController.previousBackStackEntry?.savedStateHandle?.set(ADDED_PRIZE, prize.toResult())
        navController.navigateUp()
    }

    /** Envía el premio, o lleva la pantalla al primer error: arriba si falta el premio, o al campo de la fecha. */
    private fun save(binding: FragmentPrizeAddBinding) {
        if (viewModel.save()) return
        scrollTo(binding, if (viewModel.prizeMissing.value) null else binding.premiationDateField)
    }

    /** Lleva la pantalla hasta la vista, o al inicio con null. */
    private fun scrollTo(binding: FragmentPrizeAddBinding, target: View?) {
        binding.prizeScroll.post { binding.prizeScroll.smoothScrollTo(0, target?.top ?: 0) }
    }

    companion object {
        /** Clave con la que el detalle del artista recibe el premio agregado. */
        const val ADDED_PRIZE = "addedPrize"

        /** Deben coincidir con los argumentos de prizeAddFragment en nav_graph.xml. */
        const val ARG_ARTIST_ID = "artistId"
        const val ARG_ARTIST_TYPE = "artistType"
        const val ARG_ARTIST_NAME = "artistName"

        private const val DATE_PICKER_TAG = "premiationDatePicker"

        private const val PRIZE_ID = "id"
        private const val PRIZE_NAME = "name"
        private const val PRIZE_ORGANIZATION = "organization"
        private const val PRIZE_DATE = "date"

        fun args(artist: Artist) = bundleOf(ARG_ARTIST_ID to artist.id, ARG_ARTIST_TYPE to artist.type.name, ARG_ARTIST_NAME to artist.name)

        /** El premio recién agregado que llegó en [ADDED_PRIZE]. */
        fun prizeOf(result: Bundle) = ArtistPrize(
            id = result.getInt(PRIZE_ID),
            name = result.getString(PRIZE_NAME).orEmpty(),
            organization = result.getString(PRIZE_ORGANIZATION).orEmpty(),
            date = LocalDate.parse(result.getString(PRIZE_DATE)),
        )

        private fun ArtistPrize.toResult(): Bundle =
            bundleOf(PRIZE_ID to id, PRIZE_NAME to name, PRIZE_ORGANIZATION to organization, PRIZE_DATE to date.toString())
    }
}
