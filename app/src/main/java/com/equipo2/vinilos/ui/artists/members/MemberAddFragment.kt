package com.equipo2.vinilos.ui.artists.members

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
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.Artist
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.model.MusicianCandidate
import com.equipo2.vinilos.databinding.FragmentMemberAddBinding
import com.equipo2.vinilos.ui.common.SnackbarAction
import com.equipo2.vinilos.ui.common.buttonProgress
import com.equipo2.vinilos.ui.common.collectWhileVisible
import com.equipo2.vinilos.ui.common.keepSnackbarsAbove
import com.equipo2.vinilos.ui.common.showSnackbar
import com.equipo2.vinilos.ui.state.UiState
import com.equipo2.vinilos.ui.state.showEmpty
import com.equipo2.vinilos.ui.state.showError
import com.equipo2.vinilos.ui.state.showLoading
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/** Lo que muestra el listado: los músicos, el elegido, si se está enviando y si la banda ya no existe. */
private data class ListScreen(
    val state: UiState<List<MusicianCandidate>>,
    val selected: MusicianCandidate?,
    val submitting: Boolean,
    val bandMissing: Boolean,
)

/** Elegir el músico que se agrega a una banda (HU12): buscador, lista de selección única y «Elige un músico». */
class MemberAddFragment : Fragment(R.layout.fragment_member_add) {

    private val viewModel: MemberAddViewModel by viewModels {
        MemberAddViewModel.factory(requireArguments().getInt(ARG_BAND_ID))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentMemberAddBinding.bind(view)
        binding.memberAddSubtitle.text = getString(R.string.member_add_subtitle, requireArguments().getString(ARG_BAND_NAME))
        setUpSearch(binding)
        setUpList(binding)
        setUpSubmitting(binding)
        setUpEvents(binding)

        viewLifecycleOwner.collectWhileVisible(viewModel.missingSelection) { missing ->
            binding.memberRequiredError.isVisible = missing
        }

        binding.memberCancelButton.setOnClickListener { findNavController().navigateUp() }
        binding.memberAddButton.setOnClickListener {
            hideKeyboard(binding)
            viewModel.add()
        }
    }

    /** Filtra mientras se escribe. El campo recuerda lo escrito al girar, y al volver a escribirlo el ViewModel ya lo tiene. */
    private fun setUpSearch(binding: FragmentMemberAddBinding) {
        binding.memberSearchInput.doAfterTextChanged { text -> viewModel.search(text.toString()) }
        binding.memberSearchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) hideKeyboard(binding)
            actionId == EditorInfo.IME_ACTION_SEARCH
        }
    }

    /**
     * Filas con el estado de cada músico, el elegido resaltado, y los estados de carga, vacío, sin resultados y error.
     * Si la banda ya no existe se dice eso en lugar del error, porque reintentar no lo arregla.
     */
    private fun setUpList(binding: FragmentMemberAddBinding) {
        val adapter = MemberCandidateAdapter { candidate -> select(binding, candidate) }
        binding.memberList.layoutManager = LinearLayoutManager(requireContext())
        binding.memberList.adapter = adapter

        val screen = combine(viewModel.state, viewModel.selected, viewModel.isSubmitting, viewModel.bandMissing, ::ListScreen)
        viewLifecycleOwner.collectWhileVisible(screen) { (state, selected, submitting, bandMissing) ->
            val noResults = state is UiState.Success && state.data.isEmpty()
            when (state) {
                UiState.Loading ->
                    binding.memberLoading.showLoading(getString(R.string.member_add_loading), R.layout.view_skeleton_rows)
                UiState.Empty -> binding.memberMessage.showEmpty(
                    getString(R.string.member_add_empty_title),
                    getString(R.string.member_add_empty_body),
                    R.drawable.ic_groups,
                )
                is UiState.Error -> if (bandMissing) {
                    binding.memberMessage.showEmpty(
                        getString(R.string.member_add_band_missing_title),
                        getString(R.string.member_add_band_missing_body),
                        R.drawable.ic_groups,
                    )
                } else {
                    binding.memberMessage.showError(getString(R.string.member_add_object), state.reason) { viewModel.retry() }
                }
                is UiState.Success -> {
                    adapter.submitList(state.data.map { MemberRow(it, selected = it == selected, enabled = !submitting) })
                    if (noResults) binding.memberMessage.showEmpty(
                        getString(R.string.member_add_no_results_title),
                        getString(R.string.member_add_no_results_body, viewModel.query.value.trim()),
                        R.drawable.ic_search,
                    )
                }
            }

            // Buscador y botones se quedan en el vacío y sin resultados, así se puede cambiar la búsqueda o cancelar
            val showsContent = state is UiState.Success || state == UiState.Empty
            binding.memberHeader.isVisible = showsContent
            binding.memberFooter.isVisible = showsContent
            binding.memberLoading.root.isVisible = state == UiState.Loading
            binding.memberMessage.root.isVisible = state == UiState.Empty || state is UiState.Error || noResults
            binding.memberList.isVisible = state is UiState.Success && !noResults
        }
    }

    /** Mientras se envía: barra de progreso, «Agregando…» y nada se puede tocar, para no enviar dos veces. */
    private fun setUpSubmitting(binding: FragmentMemberAddBinding) {
        val progress = requireContext().buttonProgress()

        viewLifecycleOwner.collectWhileVisible(viewModel.isSubmitting) { submitting ->
            binding.memberSubmittingProgress.isVisible = submitting
            binding.memberSearchField.isEnabled = !submitting
            binding.memberCancelButton.isEnabled = !submitting
            binding.memberAddButton.isEnabled = !submitting
            binding.memberAddButton.icon = progress.takeIf { submitting }
            binding.memberAddButton.setText(if (submitting) R.string.member_add_saving else R.string.action_add)
        }
    }

    /**
     * Si el músico es de otra banda se pide confirmar el cambio, y al aceptar se envía sin volver a preguntar.
     * Si falla, el aviso sale encima de los botones y el músico sigue elegido.
     */
    private fun setUpEvents(binding: FragmentMemberAddBinding) {
        BandChangeDialog.onBandChangeConfirmed(this) { viewModel.confirmAdd() }
        keepSnackbarsAbove(binding.memberFooter)

        viewLifecycleOwner.collectWhileVisible(viewModel.events) { event ->
            when (event) {
                is MemberAddEvent.ConfirmBandChange -> BandChangeDialog.show(
                    host = this,
                    musicianName = event.musician.name,
                    currentBandName = event.currentBandName,
                    newBandName = requireArguments().getString(ARG_BAND_NAME).orEmpty(),
                )
                is MemberAddEvent.Added -> returnToBand(event.musician)
                is MemberAddEvent.Failed -> showFailure(event.failure)
            }
        }
    }

    private fun showFailure(failure: MemberAddFailure) = when (failure) {
        // Reintentar envía el mismo músico sin volver a pedir la confirmación del cambio de banda
        MemberAddFailure.CONNECTION -> showSnackbar(
            getString(R.string.error_member_add_connection),
            isError = true,
            action = SnackbarAction(getString(R.string.error_network_retry)) { viewModel.confirmAdd() },
        )
        // El ViewModel ya está recargando el listado
        MemberAddFailure.NOT_FOUND -> showSnackbar(getString(R.string.error_member_add_not_found), isError = true)
        MemberAddFailure.SERVER -> showSnackbar(getString(R.string.error_member_add_failed), isError = true)
    }

    /** Vuelve al detalle con el músico agregado, que lo marca como «Nuevo» y muestra el aviso «Integrante agregado». */
    private fun returnToBand(musician: Artist) {
        val navController = findNavController()
        navController.previousBackStackEntry?.savedStateHandle?.set(ADDED_MEMBER, musician.toResult())
        navController.navigateUp()
    }

    private fun select(binding: FragmentMemberAddBinding, candidate: MusicianCandidate) {
        hideKeyboard(binding)
        viewModel.select(candidate)
    }

    // Al elegir o agregar se baja el teclado para que se vean la lista y los botones
    private fun hideKeyboard(binding: FragmentMemberAddBinding) {
        binding.memberSearchInput.clearFocus()
        val keyboard = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        keyboard.hideSoftInputFromWindow(binding.memberSearchInput.windowToken, 0)
    }

    companion object {
        /** Clave con la que el detalle de la banda recibe el músico agregado. */
        const val ADDED_MEMBER = "addedMember"

        /** Deben coincidir con los argumentos de memberAddFragment en nav_graph.xml. */
        const val ARG_BAND_ID = "bandId"
        const val ARG_BAND_NAME = "bandName"

        private const val MUSICIAN_ID = "id"
        private const val MUSICIAN_NAME = "name"
        private const val MUSICIAN_IMAGE = "image"
        private const val MUSICIAN_BIRTH_DATE = "birthDate"

        fun args(bandId: Int, bandName: String) = bundleOf(ARG_BAND_ID to bandId, ARG_BAND_NAME to bandName)

        /** El músico recién agregado que llegó en [ADDED_MEMBER]. */
        fun memberOf(result: Bundle) = Artist(
            id = result.getInt(MUSICIAN_ID),
            name = result.getString(MUSICIAN_NAME).orEmpty(),
            image = result.getString(MUSICIAN_IMAGE).orEmpty(),
            type = ArtistType.MUSICIAN,
            date = LocalDate.parse(result.getString(MUSICIAN_BIRTH_DATE)),
        )

        private fun Artist.toResult(): Bundle =
            bundleOf(MUSICIAN_ID to id, MUSICIAN_NAME to name, MUSICIAN_IMAGE to image, MUSICIAN_BIRTH_DATE to date.toString())
    }
}
