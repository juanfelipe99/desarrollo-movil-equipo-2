package com.equipo2.vinilos.ui.collectors.detail

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.Collector
import com.equipo2.vinilos.databinding.FragmentCollectorDetailBinding
import com.equipo2.vinilos.ui.state.showEmpty

/** Detalle de coleccionista (HU06). Por ahora solo avisa que llegará en la próxima versión. */
class CollectorDetailFragment : Fragment(R.layout.fragment_collector_detail) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FragmentCollectorDetailBinding.bind(view).collectorSoon.showEmpty(
            getString(R.string.coming_soon_title),
            getString(R.string.collector_detail_soon_body),
            R.drawable.ic_groups,
        )
    }

    companion object {
        const val ARG_COLLECTOR_ID = "collectorId"

        fun args(collector: Collector) = bundleOf(ARG_COLLECTOR_ID to collector.id)
    }
}
