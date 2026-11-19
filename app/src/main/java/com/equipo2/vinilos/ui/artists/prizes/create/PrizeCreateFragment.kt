package com.equipo2.vinilos.ui.artists.prizes.create

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.equipo2.vinilos.R
import com.equipo2.vinilos.databinding.FragmentPrizeCreateBinding
import com.equipo2.vinilos.ui.common.buttonProgress
import com.equipo2.vinilos.ui.common.collectWhileVisible
import com.equipo2.vinilos.ui.common.keepSnackbarsAbove
import com.equipo2.vinilos.ui.common.releaseTextFieldsOnDestroyView
import com.equipo2.vinilos.ui.common.showError
import com.equipo2.vinilos.ui.common.showSnackbar
import com.google.android.material.textfield.TextInputLayout

/** Formulario para crear un premio (HU13). */
class PrizeCreateFragment : Fragment(R.layout.fragment_prize_create) {

    private val viewModel: PrizeCreateViewModel by viewModels { PrizeCreateViewModel.Factory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentPrizeCreateBinding.bind(view)
        releaseTextFieldsOnDestroyView(view)
        setUpTextFields(binding)
        setUpErrors(binding)
        setUpSaving(binding)
        setUpResults(binding)

        binding.cancelButton.setOnClickListener { findNavController().navigateUp() }
        binding.saveButton.setOnClickListener { save(binding) }
    }

    private fun setUpTextFields(binding: FragmentPrizeCreateBinding) {
        binding.nameInput.doAfterTextChanged { text -> viewModel.update { copy(name = text.toString()) } }
        binding.organizationInput.doAfterTextChanged { text -> viewModel.update { copy(organization = text.toString()) } }
        binding.descriptionInput.doAfterTextChanged { text -> viewModel.update { copy(description = text.toString()) } }
    }

    private fun setUpErrors(binding: FragmentPrizeCreateBinding) {
        viewLifecycleOwner.collectWhileVisible(viewModel.errors) { errors ->
            PrizeField.entries.forEach { field -> binding.fieldOf(field).showError(errors[field]) }
        }
    }

    /** Mientras crea: barra de progreso y Crear premio deshabilitado con «Creando…». */
    private fun setUpSaving(binding: FragmentPrizeCreateBinding) {
        val progress = requireContext().buttonProgress()

        viewLifecycleOwner.collectWhileVisible(viewModel.isSaving) { saving ->
            binding.savingProgress.isVisible = saving
            binding.saveButton.isEnabled = !saving
            binding.saveButton.icon = progress.takeIf { saving }
            binding.saveButton.setText(if (saving) R.string.prize_create_saving else R.string.prize_create_action)
        }
    }

    /** Si falla avisa encima de los botones y conserva lo escrito. El regreso tras crearlo llega en T5. */
    private fun setUpResults(binding: FragmentPrizeCreateBinding) {
        keepSnackbarsAbove(binding.actionBar)

        viewLifecycleOwner.collectWhileVisible(viewModel.events) { event ->
            when (event) {
                is PrizeCreateEvent.Created -> Unit
                PrizeCreateEvent.Failed -> showSnackbar(getString(R.string.error_prize_create_failed), isError = true)
            }
        }
    }

    /** Si hay errores, lleva la pantalla al primero y le da el foco. */
    private fun save(binding: FragmentPrizeCreateBinding) {
        if (viewModel.save()) return

        val first = binding.fieldOf(viewModel.errors.value.keys.first())
        binding.formScroll.post { binding.formScroll.smoothScrollTo(0, first.top) }
        first.editText?.requestFocus()
    }

    private fun FragmentPrizeCreateBinding.fieldOf(field: PrizeField): TextInputLayout = when (field) {
        PrizeField.NAME -> nameField
        PrizeField.ORGANIZATION -> organizationField
        PrizeField.DESCRIPTION -> descriptionField
    }
}
