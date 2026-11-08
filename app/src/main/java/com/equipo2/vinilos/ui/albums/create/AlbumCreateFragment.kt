package com.equipo2.vinilos.ui.albums.create

import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil3.load
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.AlbumCatalog
import com.equipo2.vinilos.databinding.FragmentAlbumCreateBinding
import com.equipo2.vinilos.ui.common.buttonProgress
import com.equipo2.vinilos.ui.common.collectWhileVisible
import com.equipo2.vinilos.ui.common.keepSnackbarsAbove
import com.equipo2.vinilos.ui.common.reconnectDatePicker
import com.equipo2.vinilos.ui.common.releaseTextFieldsOnDestroyView
import com.equipo2.vinilos.ui.common.showError
import com.equipo2.vinilos.ui.common.showPastDatePicker
import com.equipo2.vinilos.ui.common.showSnackbar
import com.equipo2.vinilos.ui.common.toShortText
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Formulario para crear un álbum (HU07). */
class AlbumCreateFragment : Fragment(R.layout.fragment_album_create) {

    private val viewModel: AlbumCreateViewModel by viewModels { AlbumCreateViewModel.Factory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentAlbumCreateBinding.bind(view)
        releaseTextFieldsOnDestroyView(view)
        setUpTextFields(binding)
        setUpCoverPreview(binding)
        setUpReleaseDate(binding)
        setUpDropdowns(binding)
        setUpErrors(binding)
        setUpSaving(binding)
        setUpResults(binding)

        binding.cancelButton.setOnClickListener { findNavController().navigateUp() }
        binding.saveButton.setOnClickListener { save(binding) }
    }

    private fun setUpTextFields(binding: FragmentAlbumCreateBinding) {
        binding.nameInput.doAfterTextChanged { text -> viewModel.update { copy(name = text.toString()) } }
        binding.coverInput.doAfterTextChanged { text -> viewModel.update { copy(cover = text.toString()) } }
        binding.descriptionInput.doAfterTextChanged { text -> viewModel.update { copy(description = text.toString()) } }
    }

    /** Con una URL válida muestra la portada, o el marcador si no carga. */
    private fun setUpCoverPreview(binding: FragmentAlbumCreateBinding) {
        binding.coverInput.doAfterTextChanged { text ->
            val url = text.toString().trim()
            val isValid = isValidCoverUrl(url)

            binding.coverPreview.isVisible = isValid
            if (isValid && url != binding.coverPreviewUrl.text.toString()) {
                binding.coverPreviewUrl.text = url
                binding.coverPreviewImage.load(url)
            }
        }
    }

    /** El campo y su ícono abren el selector, y el campo muestra la fecha elegida. */
    private fun setUpReleaseDate(binding: FragmentAlbumCreateBinding) {
        val onPicked = { date: LocalDate -> viewModel.update { copy(releaseDate = date) } }
        val openPicker = {
            showPastDatePicker(DATE_PICKER_TAG, R.string.release_date_picker_title, viewModel.form.value.releaseDate, onPicked)
        }
        binding.releaseDateInput.setOnClickListener { openPicker() }
        binding.releaseDateField.setEndIconOnClickListener { openPicker() }
        reconnectDatePicker(DATE_PICKER_TAG, onPicked)

        val releaseDate = viewModel.form.map { it.releaseDate }.distinctUntilChanged()
        viewLifecycleOwner.collectWhileVisible(releaseDate) { date -> binding.releaseDateInput.setText(date?.toShortText()) }
    }

    /** Género y sello solo ofrecen los valores que acepta la API. */
    private fun setUpDropdowns(binding: FragmentAlbumCreateBinding) {
        binding.genreInput.bindOptions(AlbumCatalog.genres) { genre -> viewModel.update { copy(genre = genre) } }
        binding.recordLabelInput.bindOptions(AlbumCatalog.recordLabels) { label -> viewModel.update { copy(recordLabel = label) } }
    }

    private fun MaterialAutoCompleteTextView.bindOptions(options: List<String>, onSelected: (String) -> Unit) {
        setSimpleItems(options.toTypedArray())
        setOnItemClickListener { _, _, position, _ -> onSelected(options[position]) }
    }

    private fun setUpErrors(binding: FragmentAlbumCreateBinding) {
        viewLifecycleOwner.collectWhileVisible(viewModel.errors) { errors ->
            AlbumField.entries.forEach { field -> binding.fieldOf(field).showError(errors[field]) }
        }
    }

    /** Mientras guarda: barra de progreso y Guardar deshabilitado con «Guardando…». */
    private fun setUpSaving(binding: FragmentAlbumCreateBinding) {
        val progress = requireContext().buttonProgress()

        viewLifecycleOwner.collectWhileVisible(viewModel.isSaving) { saving ->
            binding.savingProgress.isVisible = saving
            binding.saveButton.isEnabled = !saving
            binding.saveButton.icon = progress.takeIf { saving }
            binding.saveButton.setText(if (saving) R.string.album_create_saving else R.string.action_save)
        }
    }

    /** Si se creó vuelve a Álbumes con el id nuevo, y si falló avisa encima de los botones. */
    private fun setUpResults(binding: FragmentAlbumCreateBinding) {
        keepSnackbarsAbove(binding.actionBar)

        viewLifecycleOwner.collectWhileVisible(viewModel.events) { event ->
            when (event) {
                is AlbumCreateEvent.Saved -> {
                    val navController = findNavController()
                    navController.previousBackStackEntry?.savedStateHandle?.set(CREATED_ALBUM_ID, event.albumId)
                    navController.navigateUp()
                }
                AlbumCreateEvent.Failed -> showSnackbar(getString(R.string.error_album_create_failed), isError = true)
            }
        }
    }

    /** Envía el álbum, o lleva la pantalla al primer error y le da el foco si es de texto. */
    private fun save(binding: FragmentAlbumCreateBinding) {
        if (viewModel.save()) return

        val first = binding.fieldOf(viewModel.errors.value.keys.first())
        binding.formScroll.post { binding.formScroll.smoothScrollTo(0, first.top) }
        first.editText?.takeIf { it.inputType != InputType.TYPE_NULL }?.requestFocus()
    }

    private fun FragmentAlbumCreateBinding.fieldOf(field: AlbumField): TextInputLayout = when (field) {
        AlbumField.NAME -> nameField
        AlbumField.COVER -> coverField
        AlbumField.RELEASE_DATE -> releaseDateField
        AlbumField.GENRE -> genreField
        AlbumField.RECORD_LABEL -> recordLabelField
        AlbumField.DESCRIPTION -> descriptionField
    }

    companion object {
        /** Clave con la que Álbumes recibe el id del álbum recién creado. */
        const val CREATED_ALBUM_ID = "createdAlbumId"

        private const val DATE_PICKER_TAG = "releaseDatePicker"
    }
}
