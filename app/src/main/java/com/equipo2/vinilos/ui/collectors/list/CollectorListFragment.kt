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
import com.equipo2.vinilos.ui.common.collectWhileVisible
import com.equipo2.vinilos.ui.common.smoothScrollToTop
import com.equipo2.vinilos.ui.state.UiState

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

    /** Filas con nombre, álbumes y correo, y el total de coleccionistas encima. */
    private fun setUpList(binding: FragmentCollectorListBinding) {
        // La navegación al detalle (HU06) se conecta en la tarea T4
        val adapter = CollectorAdapter { }
        binding.collectorList.layoutManager = LinearLayoutManager(requireContext())
        binding.collectorList.adapter = adapter

        viewLifecycleOwner.collectWhileVisible(viewModel.state) { state ->
            if (state is UiState.Success) adapter.submitList(state.data)

            binding.collectorHeader.isVisible = state is UiState.Success
            binding.collectorList.isVisible = state is UiState.Success
        }

        viewLifecycleOwner.collectWhileVisible(viewModel.count) { count ->
            binding.collectorCount.text = resources.getQuantityString(R.plurals.collector_list_count, count, count)
        }
    }
}
