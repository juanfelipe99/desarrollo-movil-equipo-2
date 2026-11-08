package com.equipo2.vinilos.ui.artists.list

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.equipo2.vinilos.R
import com.equipo2.vinilos.databinding.FragmentArtistListBinding
import com.equipo2.vinilos.ui.artists.detail.ArtistDetailFragment
import com.equipo2.vinilos.ui.common.ScrollsToTop
import com.equipo2.vinilos.ui.common.bindPullToRefresh
import com.equipo2.vinilos.ui.common.collectWhileVisible
import com.equipo2.vinilos.ui.common.navigateIfPossible
import com.equipo2.vinilos.ui.common.smoothScrollToTop
import com.equipo2.vinilos.ui.state.UiState
import com.equipo2.vinilos.ui.state.showEmpty
import com.equipo2.vinilos.ui.state.showError
import com.equipo2.vinilos.ui.state.showLoading

/** Lista de artistas (HU03). */
class ArtistListFragment : Fragment(R.layout.fragment_artist_list), ScrollsToTop {

    private val viewModel: ArtistListViewModel by viewModels { ArtistListViewModel.Factory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentArtistListBinding.bind(view)
        setUpFilters(binding)
        setUpList(binding)
    }

    override fun scrollToTop() {
        view?.findViewById<RecyclerView>(R.id.artistList)?.smoothScrollToTop()
    }

    /** Todos, Músicos y Bandas: siempre hay uno elegido y el ViewModel lo recuerda al girar. */
    private fun setUpFilters(binding: FragmentArtistListBinding) {
        val chips = mapOf(
            ArtistFilter.ALL to binding.filterAll,
            ArtistFilter.MUSICIANS to binding.filterMusicians,
            ArtistFilter.BANDS to binding.filterBands,
        )

        viewLifecycleOwner.collectWhileVisible(viewModel.filter) { filter -> chips.getValue(filter).isChecked = true }

        binding.artistFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = chips.entries.firstOrNull { it.value.id in checkedIds }?.key ?: return@setOnCheckedStateChangeListener
            viewModel.selectFilter(filter)
        }
    }

    /** Filas que abren el detalle al tocar una, el contador, los estados de carga, vacío y error, y deslizar para recargar. */
    private fun setUpList(binding: FragmentArtistListBinding) {
        val adapter = ArtistAdapter { artist ->
            findNavController().navigateIfPossible(R.id.action_artistList_to_artistDetail, ArtistDetailFragment.args(artist))
        }
        binding.artistList.layoutManager = LinearLayoutManager(requireContext())
        // La lista ocupa toda la pantalla, así los cambios del listado no la obligan a medirse de nuevo
        binding.artistList.setHasFixedSize(true)
        binding.artistList.adapter = adapter
        bindPullToRefresh(binding.artistsRefresh, viewModel.pullRefresh, binding.artistList, binding.artistsMessage.root) {
            viewModel.refresh()
        }

        viewLifecycleOwner.collectWhileVisible(viewModel.state) { state ->
            when (state) {
                UiState.Loading ->
                    binding.artistsLoading.showLoading(getString(R.string.artist_list_loading), R.layout.view_skeleton_rows)
                UiState.Empty -> binding.artistsMessage.showEmpty(
                    getString(R.string.artist_list_empty_title),
                    getString(R.string.artist_list_empty_body),
                )
                is UiState.Error -> binding.artistsMessage.showError(getString(R.string.artist_list_object), state.reason) { viewModel.retry() }
                is UiState.Success -> adapter.submitList(state.data)
            }

            val count = (state as? UiState.Success)?.data?.size ?: 0
            binding.artistCount.text = resources.getQuantityString(R.plurals.artist_list_count, count, count)

            // Los filtros y el contador se quedan en el vacío para poder cambiar de filtro
            binding.artistHeader.isVisible = state is UiState.Success || state == UiState.Empty
            binding.artistsLoading.root.isVisible = state == UiState.Loading
            binding.artistsMessage.root.isVisible = state == UiState.Empty || state is UiState.Error
            binding.artistList.isVisible = state is UiState.Success
            binding.artistsRefresh.isEnabled = state != UiState.Loading
        }
    }
}
