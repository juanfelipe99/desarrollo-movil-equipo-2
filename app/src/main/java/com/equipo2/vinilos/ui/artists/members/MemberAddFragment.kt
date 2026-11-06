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
import com.equipo2.vinilos.data.model.MusicianCandidate
import com.equipo2.vinilos.databinding.FragmentMemberAddBinding
import com.equipo2.vinilos.ui.common.buttonProgress
import com.equipo2.vinilos.ui.common.collectWhileVisible
import com.equipo2.vinilos.ui.state.UiState
import com.equipo2.vinilos.ui.state.showEmpty
import com.equipo2.vinilos.ui.state.showError
import com.equipo2.vinilos.ui.state.showLoading
import kotlinx.coroutines.flow.combine

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
        setUpEvents()

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

    /** Filas con el estado de cada músico, el elegido resaltado, y los estados de carga, vacío, sin resultados y error. */
    private fun setUpList(binding: FragmentMemberAddBinding) {
        val adapter = MemberCandidateAdapter { candidate -> select(binding, candidate) }
        binding.memberList.layoutManager = LinearLayoutManager(requireContext())
        binding.memberList.adapter = adapter

        val screen = combine(viewModel.state, viewModel.selected, viewModel.isSubmitting, ::Triple)
        viewLifecycleOwner.collectWhileVisible(screen) { (state, selected, submitting) ->
            val noResults = state is UiState.Success && state.data.isEmpty()
            when (state) {
                UiState.Loading ->
                    binding.memberLoading.showLoading(getString(R.string.member_add_loading), R.layout.view_skeleton_rows)
                UiState.Empty -> binding.memberMessage.showEmpty(
                    getString(R.string.member_add_empty_title),
                    getString(R.string.member_add_empty_body),
                    R.drawable.ic_groups,
                )
                is UiState.Error ->
                    binding.memberMessage.showError(getString(R.string.member_add_object), state.reason) { viewModel.retry() }
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

    /** Si el músico es de otra banda se pide confirmar el cambio, y al aceptar se envía sin volver a preguntar. */
    private fun setUpEvents() {
        BandChangeDialog.onBandChangeConfirmed(this) { viewModel.confirmAdd() }

        viewLifecycleOwner.collectWhileVisible(viewModel.events) { event ->
            when (event) {
                is MemberAddEvent.ConfirmBandChange -> BandChangeDialog.show(
                    host = this,
                    musicianName = event.musician.name,
                    currentBandName = event.currentBandName,
                    newBandName = requireArguments().getString(ARG_BAND_NAME).orEmpty(),
                )
                // El regreso al detalle y los avisos de error todavía no se muestran
                is MemberAddEvent.Added, is MemberAddEvent.Failed, MemberAddEvent.BandNotFound -> Unit
            }
        }
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
        const val ARG_BAND_ID = "bandId"
        const val ARG_BAND_NAME = "bandName"

        fun args(bandId: Int, bandName: String) = bundleOf(ARG_BAND_ID to bandId, ARG_BAND_NAME to bandName)
    }
}
