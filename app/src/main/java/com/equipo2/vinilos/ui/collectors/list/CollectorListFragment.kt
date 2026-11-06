package com.equipo2.vinilos.ui.collectors.list

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.equipo2.vinilos.R
import com.equipo2.vinilos.databinding.FragmentCollectorListBinding
import com.equipo2.vinilos.ui.common.ScrollsToTop
import com.equipo2.vinilos.ui.common.bindPullToRefresh
import com.equipo2.vinilos.ui.common.collectWhileVisible
import com.equipo2.vinilos.ui.common.smoothScrollToTop
import com.equipo2.vinilos.ui.state.UiState
import com.equipo2.vinilos.ui.state.showEmpty
import com.equipo2.vinilos.ui.state.showError
import com.equipo2.vinilos.ui.state.showLoading

/** Lista de coleccionistas (HU05). */
class CollectorListFragment : Fragment(R.layout.fragment_collector_list), ScrollsToTop {

    private val viewModel: CollectorListViewModel by viewModels { CollectorListViewModel.Factory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpList(FragmentCollectorListBinding.bind(view))
    }

    override fun scrollToTop() {
        view?.findViewById<RecyclerView>(R.id.collectorList)?.smoothScrollToTop()
    }

    /** Filas con nombre, álbumes y correo, el total encima, los estados de carga, vacío y error, y deslizar para recargar. */
    private fun setUpList(binding: FragmentCollectorListBinding) {
        // La navegación al detalle (HU06) se conecta en la tarea T4
        val adapter = CollectorAdapter { }
        binding.collectorList.layoutManager = LinearLayoutManager(requireContext())
        binding.collectorList.adapter = adapter
        bindPullToRefresh(binding.collectorsRefresh, viewModel.pullRefresh, binding.collectorList, binding.collectorsMessage.root) {
            viewModel.refresh()
        }

        viewLifecycleOwner.collectWhileVisible(viewModel.state) { state ->
            when (state) {
                UiState.Loading ->
                    binding.collectorsLoading.showLoading(getString(R.string.collector_list_loading), R.layout.view_skeleton_rows)
                UiState.Empty -> binding.collectorsMessage.showEmpty(
                    getString(R.string.collector_list_empty_title),
                    getString(R.string.collector_list_empty_body),
                    R.drawable.ic_groups,
                )
                is UiState.Error ->
                    binding.collectorsMessage.showError(getString(R.string.collector_list_object), state.reason) { viewModel.retry() }
                is UiState.Success -> adapter.submitList(state.data)
            }

            // El total se queda en el vacío («0 coleccionistas»)
            binding.collectorHeader.isVisible = state is UiState.Success || state == UiState.Empty
            binding.collectorsLoading.root.isVisible = state == UiState.Loading
            binding.collectorsMessage.root.isVisible = state == UiState.Empty || state is UiState.Error
            binding.collectorList.isVisible = state is UiState.Success
            binding.collectorsRefresh.isEnabled = state != UiState.Loading
        }

        viewLifecycleOwner.collectWhileVisible(viewModel.count) { count ->
            binding.collectorCount.text = resources.getQuantityString(R.plurals.collector_list_count, count, count)
        }
    }
}
