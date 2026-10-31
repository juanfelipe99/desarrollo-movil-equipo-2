package com.equipo2.vinilos.ui.albums.tracks

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import com.equipo2.vinilos.R
import com.equipo2.vinilos.databinding.SheetTrackAddBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** Hoja inferior para agregar un track al álbum (HU08). */
class TrackAddSheet : BottomSheetDialogFragment(R.layout.sheet_track_add) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = SheetTrackAddBinding.bind(view)
        binding.trackAddSubtitle.text = getString(R.string.track_add_subtitle, requireArguments().getString(ARG_ALBUM_NAME))
    }

    override fun onStart() {
        super.onStart()
        // Se abre completa, sin la posición intermedia
        (dialog as BottomSheetDialog).behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    companion object {
        /** Deben coincidir con los argumentos de trackAddSheet en nav_graph.xml. */
        private const val ARG_ALBUM_ID = "albumId"
        private const val ARG_ALBUM_NAME = "albumName"

        fun args(albumId: Int, albumName: String): Bundle = bundleOf(ARG_ALBUM_ID to albumId, ARG_ALBUM_NAME to albumName)
    }
}
