package com.equipo2.vinilos.ui.artists.albums

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
 * Pide confirmar que la banda reemplazará a los intérpretes que tiene el álbum (HU15).
 * Es un DialogFragment para que siga abierto al girar. La confirmación llega con [onReplaceConfirmed].
 */
class AlbumReplaceDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val albumName = args.getString(ARG_ALBUM_NAME)
        val performers = args.getString(ARG_PERFORMERS)
        val performerCount = args.getInt(ARG_PERFORMER_COUNT)
        val bandName = args.getString(ARG_BAND_NAME)

        val icon = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_warning)!!.mutate()
        icon.setTint(ContextCompat.getColor(requireContext(), R.color.md_primary))

        // Ícono y título centrados, como en el prototipo y en el cambio de banda (HU12)
        return MaterialAlertDialogBuilder(requireContext(), CENTERED_THEME)
            .setIcon(icon)
            .setTitle(R.string.album_replace_title)
            .setMessage(resources.getQuantityString(R.plurals.album_replace_body, performerCount, albumName, performers, bandName))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.album_replace_confirm) { _, _ -> setFragmentResult(REQUEST_KEY, Bundle.EMPTY) }
            .create()
    }

    companion object {
        private const val TAG = "albumReplaceDialog"
        private const val REQUEST_KEY = "albumReplaceConfirmed"
        private const val ARG_ALBUM_NAME = "albumName"
        private const val ARG_PERFORMERS = "performers"
        private const val ARG_PERFORMER_COUNT = "performerCount"
        private const val ARG_BAND_NAME = "bandName"

        private val CENTERED_THEME = com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered

        /** Abre el diálogo sobre la pantalla, salvo que ya esté abierto. [performers] son los nombres ya unidos en una frase. */
        fun show(host: Fragment, albumName: String, performers: String, performerCount: Int, bandName: String) {
            if (host.childFragmentManager.findFragmentByTag(TAG) != null) return
            AlbumReplaceDialog().apply {
                arguments = bundleOf(
                    ARG_ALBUM_NAME to albumName,
                    ARG_PERFORMERS to performers,
                    ARG_PERFORMER_COUNT to performerCount,
                    ARG_BAND_NAME to bandName,
                )
            }.show(host.childFragmentManager, TAG)
        }

        /** Ejecuta la acción al tocar «Agregar y reemplazar». Se llama en cada onViewCreated, así sirve también tras girar. */
        fun onReplaceConfirmed(host: Fragment, action: () -> Unit) {
            host.childFragmentManager.setFragmentResultListener(REQUEST_KEY, host.viewLifecycleOwner) { _, _ -> action() }
        }
    }
}
