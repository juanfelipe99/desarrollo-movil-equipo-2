package com.equipo2.vinilos.ui.artists.members

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.equipo2.vinilos.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Pide confirmar que el músico dejará su banda actual para unirse a esta (HU12).
 * Es un DialogFragment para que siga abierto al girar. La confirmación llega con [onBandChangeConfirmed].
 */
class BandChangeDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val musician = args.getString(ARG_MUSICIAN_NAME)
        val currentBand = args.getString(ARG_CURRENT_BAND_NAME)
        val newBand = args.getString(ARG_NEW_BAND_NAME)

        val icon = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_warning)!!.mutate()
        icon.setTint(ContextCompat.getColor(requireContext(), R.color.md_primary))

        // Ícono y título centrados, como en el prototipo
        return MaterialAlertDialogBuilder(requireContext(), CENTERED_THEME)
            .setIcon(icon)
            .setTitle(R.string.band_change_title)
            .setMessage(getString(R.string.band_change_body, musician, currentBand, newBand))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.band_change_confirm) { _, _ -> setFragmentResult(REQUEST_KEY, Bundle.EMPTY) }
            .create()
    }

    companion object {
        private const val TAG = "bandChangeDialog"
        private const val REQUEST_KEY = "bandChangeConfirmed"
        private const val ARG_MUSICIAN_NAME = "musicianName"
        private const val ARG_CURRENT_BAND_NAME = "currentBandName"
        private const val ARG_NEW_BAND_NAME = "newBandName"

        private val CENTERED_THEME = com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered

        /** Abre el diálogo sobre la pantalla, salvo que ya esté abierto. */
        fun show(host: Fragment, musicianName: String, currentBandName: String, newBandName: String) {
            if (host.childFragmentManager.findFragmentByTag(TAG) != null) return
            BandChangeDialog().apply {
                arguments = bundleOf(
                    ARG_MUSICIAN_NAME to musicianName,
                    ARG_CURRENT_BAND_NAME to currentBandName,
                    ARG_NEW_BAND_NAME to newBandName,
                )
            }.show(host.childFragmentManager, TAG)
        }

        /** Ejecuta la acción al tocar «Cambiar de banda». Se llama en cada onViewCreated, así sirve también tras girar. */
        fun onBandChangeConfirmed(host: Fragment, action: () -> Unit) {
            host.childFragmentManager.setFragmentResultListener(REQUEST_KEY, host.viewLifecycleOwner) { _, _ -> action() }
        }
    }
}
