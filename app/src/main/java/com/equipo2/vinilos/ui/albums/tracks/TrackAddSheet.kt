package com.equipo2.vinilos.ui.albums.tracks

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import com.equipo2.vinilos.R
import com.equipo2.vinilos.databinding.SheetTrackAddBinding
import com.equipo2.vinilos.ui.common.collectWhileVisible
import com.equipo2.vinilos.ui.common.showError
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputLayout

/** Hoja inferior para agregar un track al álbum (HU08). */
class TrackAddSheet : BottomSheetDialogFragment(R.layout.sheet_track_add) {

    private val viewModel: TrackAddViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = SheetTrackAddBinding.bind(view)
        binding.trackAddSubtitle.text = getString(R.string.track_add_subtitle, requireArguments().getString(ARG_ALBUM_NAME))

        binding.trackNameInput.doAfterTextChanged { text -> viewModel.update { copy(name = text.toString()) } }
        binding.trackDurationInput.doAfterTextChanged { text -> viewModel.update { copy(duration = text.toString()) } }
        viewLifecycleOwner.collectWhileVisible(viewModel.errors) { errors ->
            TrackField.entries.forEach { field -> binding.fieldOf(field).showError(errors[field]) }
        }

        // Cancelar, Atrás, tocar fuera o deslizar hacia abajo cierran sin guardar
        binding.trackCancelButton.setOnClickListener { dismiss() }
        binding.trackAddButton.setOnClickListener { add(binding) }
    }

    override fun onStart() {
        super.onStart()
        // Se abre completa, sin la posición intermedia
        (dialog as BottomSheetDialog).behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    /** Si falta algo, le da el foco al primer campo con error. */
    private fun add(binding: SheetTrackAddBinding) {
        if (viewModel.add()) return
        binding.fieldOf(viewModel.errors.value.keys.first()).editText?.requestFocus()
    }

    private fun SheetTrackAddBinding.fieldOf(field: TrackField): TextInputLayout = when (field) {
        TrackField.NAME -> trackNameField
        TrackField.DURATION -> trackDurationField
    }

    companion object {
        /** Deben coincidir con los argumentos de trackAddSheet en nav_graph.xml. */
        private const val ARG_ALBUM_ID = "albumId"
        private const val ARG_ALBUM_NAME = "albumName"

        fun args(albumId: Int, albumName: String): Bundle = bundleOf(ARG_ALBUM_ID to albumId, ARG_ALBUM_NAME to albumName)
    }
}
