package com.equipo2.vinilos.ui.albums.tracks

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.Track
import com.equipo2.vinilos.databinding.SheetTrackAddBinding
import com.equipo2.vinilos.ui.common.AppSnackbar
import com.equipo2.vinilos.ui.common.buttonProgress
import com.equipo2.vinilos.ui.common.collectWhileVisible
import com.equipo2.vinilos.ui.common.showError
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputLayout

/** Hoja inferior para agregar un track al álbum (HU08). */
class TrackAddSheet : BottomSheetDialogFragment(R.layout.sheet_track_add) {

    private val viewModel: TrackAddViewModel by viewModels {
        TrackAddViewModel.factory(requireArguments().getInt(ARG_ALBUM_ID))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = SheetTrackAddBinding.bind(view)
        binding.trackAddSubtitle.text = getString(R.string.track_add_subtitle, requireArguments().getString(ARG_ALBUM_NAME))

        binding.trackNameInput.doAfterTextChanged { text -> viewModel.update { copy(name = text.toString()) } }
        binding.trackDurationInput.doAfterTextChanged { text -> viewModel.update { copy(duration = text.toString()) } }
        viewLifecycleOwner.collectWhileVisible(viewModel.errors) { errors ->
            TrackField.entries.forEach { field -> binding.fieldOf(field).showError(errors[field]) }
        }
        setUpSaving(binding)
        setUpResults(binding)

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

    /** Mientras envía: Agregar deshabilitado con «Agregando…», y la hoja no se puede cerrar para no perder el resultado. */
    private fun setUpSaving(binding: SheetTrackAddBinding) {
        val progress = requireContext().buttonProgress()

        viewLifecycleOwner.collectWhileVisible(viewModel.isSaving) { saving ->
            isCancelable = !saving
            binding.trackCancelButton.isEnabled = !saving
            binding.trackAddButton.isEnabled = !saving
            binding.trackAddButton.icon = progress.takeIf { saving }
            binding.trackAddButton.setText(if (saving) R.string.track_add_saving else R.string.action_add)
        }
    }

    /** Si se agregó, el detalle recibe el track y la hoja se cierra. Si falló, avisa dentro de la hoja. */
    private fun setUpResults(binding: SheetTrackAddBinding) {
        viewLifecycleOwner.collectWhileVisible(viewModel.events) { event ->
            when (event) {
                is TrackAddEvent.Added -> {
                    val navController = findNavController()
                    navController.previousBackStackEntry?.savedStateHandle?.set(ADDED_TRACK, event.track.toResult())
                    dismiss()
                }
                TrackAddEvent.Failed -> AppSnackbar.show(binding.root, getString(R.string.error_track_add_failed), isError = true)
            }
        }
    }

    /** Envía el track, o le da el foco al primer campo con error. */
    private fun add(binding: SheetTrackAddBinding) {
        if (viewModel.add()) return
        binding.fieldOf(viewModel.errors.value.keys.first()).editText?.requestFocus()
    }

    private fun SheetTrackAddBinding.fieldOf(field: TrackField): TextInputLayout = when (field) {
        TrackField.NAME -> trackNameField
        TrackField.DURATION -> trackDurationField
    }

    companion object {
        /** Clave con la que el detalle del álbum recibe el track recién agregado. */
        const val ADDED_TRACK = "addedTrack"

        /** Deben coincidir con los argumentos de trackAddSheet en nav_graph.xml. */
        private const val ARG_ALBUM_ID = "albumId"
        private const val ARG_ALBUM_NAME = "albumName"

        fun args(albumId: Int, albumName: String): Bundle = bundleOf(ARG_ALBUM_ID to albumId, ARG_ALBUM_NAME to albumName)

        /** El track recién agregado que llegó en [ADDED_TRACK]. */
        fun trackOf(result: Bundle) = Track(
            id = result.getInt(TRACK_ID),
            name = result.getString(TRACK_NAME).orEmpty(),
            duration = result.getString(TRACK_DURATION).orEmpty(),
        )

        private fun Track.toResult(): Bundle = bundleOf(TRACK_ID to id, TRACK_NAME to name, TRACK_DURATION to duration)

        private const val TRACK_ID = "id"
        private const val TRACK_NAME = "name"
        private const val TRACK_DURATION = "duration"
    }
}
