package com.equipo2.vinilos.ui.artists.detail

import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.NavBackStackEntry
import androidx.navigation.fragment.findNavController
import coil3.load
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.Album
import com.equipo2.vinilos.data.model.Artist
import com.equipo2.vinilos.data.model.ArtistDetail
import com.equipo2.vinilos.data.model.ArtistPrize
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.model.Role
import com.equipo2.vinilos.databinding.FragmentArtistDetailBinding
import com.equipo2.vinilos.databinding.ItemArtistPrizeBinding
import com.equipo2.vinilos.ui.albums.detail.AlbumDetailFragment
import com.equipo2.vinilos.ui.artists.members.MemberAddFragment
import com.equipo2.vinilos.ui.common.bindExpandableText
import com.equipo2.vinilos.ui.common.bindPullToRefresh
import com.equipo2.vinilos.ui.common.collectWhileVisible
import com.equipo2.vinilos.ui.common.initialsOf
import com.equipo2.vinilos.ui.common.navigateIfPossible
import com.equipo2.vinilos.ui.common.showSnackbar
import com.equipo2.vinilos.ui.common.toLongText
import com.equipo2.vinilos.ui.role.RoleViewModel
import com.equipo2.vinilos.ui.state.UiState
import com.equipo2.vinilos.ui.state.addEmptyNote
import com.equipo2.vinilos.ui.state.addEmptySection
import com.equipo2.vinilos.ui.state.showError
import com.equipo2.vinilos.ui.state.showLoading

/**
 * Detalle de un músico o de una banda (HU04). Desde aquí se pasa del músico a su banda y de la banda a sus integrantes,
 * y el coleccionista agrega integrantes a la banda (HU12).
 */
class ArtistDetailFragment : Fragment(R.layout.fragment_artist_detail) {

    private val roleViewModel: RoleViewModel by activityViewModels { RoleViewModel.Factory }
    private val viewModel: ArtistDetailViewModel by viewModels {
        val args = requireArguments()
        ArtistDetailViewModel.factory(args.getInt(ARG_ARTIST_ID), ArtistType.valueOf(requireNotNull(args.getString(ARG_ARTIST_TYPE))))
    }

    /** Entrada de esta pantalla, para no abrir dos detalles con dos toques seguidos. */
    private var ownEntry: NavBackStackEntry? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ownEntry = findNavController().currentBackStackEntry
        val binding = FragmentArtistDetailBinding.bind(view)
        binding.membersList.adapter = MemberAdapter(::openArtist)
        binding.albumsList.adapter = ArtistAlbumAdapter { album -> openAlbum(album) }

        bindPullToRefresh(binding.artistRefresh, viewModel.pullRefresh, binding.artistScroll, binding.artistError.root) {
            viewModel.refresh()
        }

        viewLifecycleOwner.collectWhileVisible(viewModel.state) { state ->
            when (state) {
                UiState.Loading ->
                    binding.artistLoading.showLoading(getString(R.string.artist_detail_loading), R.layout.view_skeleton_detail)
                is UiState.Error -> binding.artistError.showError(getString(R.string.artist_detail_object), state.reason) { viewModel.retry() }
                is UiState.Success -> showArtist(binding, state.data)
                UiState.Empty -> Unit
            }

            binding.artistLoading.root.isVisible = state == UiState.Loading
            binding.artistError.root.isVisible = state is UiState.Error
            binding.artistContent.isVisible = state is UiState.Success
            binding.artistRefresh.isEnabled = state != UiState.Loading
        }

        // Solo el coleccionista agrega integrantes, y el vacío de integrantes le dice cómo
        viewLifecycleOwner.collectWhileVisible(roleViewModel.role) { role ->
            binding.addMemberButton.isVisible = role == Role.COLLECTOR
            (viewModel.state.value as? UiState.Success)?.let { showMembers(binding, it.data.members) }
        }
        viewLifecycleOwner.collectWhileVisible(viewModel.newMemberIds) {
            (viewModel.state.value as? UiState.Success)?.let { showMembers(binding, it.data.members) }
        }
        showAddedMember()
    }

    /** Al volver de Agregar integrante, el músico aparece al final de la lista marcado y con el aviso «Integrante agregado». */
    private fun showAddedMember() {
        val savedState = ownEntry?.savedStateHandle ?: return

        viewLifecycleOwner.collectWhileVisible(savedState.getStateFlow<Bundle?>(MemberAddFragment.ADDED_MEMBER, null)) { added ->
            if (added == null) return@collectWhileVisible
            viewModel.onMemberAdded(MemberAddFragment.memberOf(added))
            showSnackbar(getString(R.string.snackbar_member_added))
            // Se quita para que el aviso no se repita al girar
            savedState.remove<Bundle>(MemberAddFragment.ADDED_MEMBER)
        }
    }

    private fun showArtist(binding: FragmentArtistDetailBinding, detail: ArtistDetail) {
        val artist = detail.artist
        val isMusician = artist.type == ArtistType.MUSICIAN

        binding.artistInitials.text = initialsOf(artist.name)
        binding.artistPhoto.load(artist.image.ifBlank { null })
        binding.artistPhoto.contentDescription = getString(R.string.artist_photo_description, artist.name)
        binding.artistName.text = artist.name
        binding.artistTypeIcon.setImageResource(if (isMusician) R.drawable.ic_mic else R.drawable.ic_groups)
        binding.artistTypeLabel.setText(if (isMusician) R.string.artist_type_musician else R.string.artist_type_band)

        binding.artistDate.infoIcon.setImageResource(R.drawable.ic_calendar_today)
        binding.artistDate.infoLabel.setText(if (isMusician) R.string.artist_detail_birth_date_label else R.string.artist_detail_creation_date_label)
        binding.artistDate.infoValue.text = artist.date.toLongText()
        showBand(binding, detail.band, isMusician)

        bindExpandableText(binding.artistDescription, binding.artistDescriptionToggle, detail.description)

        binding.membersSection.isVisible = !isMusician
        if (!isMusician) {
            showMembers(binding, detail.members)
            binding.addMemberButton.setOnClickListener { openMemberAdd(artist) }
        }
        showAlbums(binding, detail)
        showPrizes(binding, detail.prizes)
    }

    /** La banda del músico, subrayada como un enlace que abre su detalle, o «No pertenece a una banda». */
    private fun showBand(binding: FragmentArtistDetailBinding, band: Artist?, isMusician: Boolean) {
        binding.artistBandDivider.isVisible = isMusician
        binding.artistBandRow.isVisible = isMusician
        if (!isMusician) return

        binding.artistBand.infoIcon.setImageResource(R.drawable.ic_groups)
        binding.artistBand.infoLabel.setText(R.string.artist_detail_band_label)
        val value = binding.artistBand.infoValue
        if (band == null) {
            value.setText(R.string.artist_detail_no_band)
            value.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_on_surface))
            value.paintFlags = value.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
            binding.artistBandRow.contentDescription = null
            binding.artistBandRow.setOnClickListener(null)
            binding.artistBandRow.isClickable = false
            return
        }
        value.text = band.name
        value.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_primary))
        value.paintFlags = value.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.artistBandRow.contentDescription = getString(R.string.cd_artist_detail_band, band.name)
        binding.artistBandRow.setOnClickListener { openArtist(band) }
    }

    private fun showMembers(binding: FragmentArtistDetailBinding, members: List<Artist>) {
        binding.membersCount.text = resources.getQuantityString(R.plurals.artist_detail_members_count, members.size, members.size)
        val newIds = viewModel.newMemberIds.value
        (binding.membersList.adapter as MemberAdapter).submitList(members.map { BandMemberRow(it, isNew = it.id in newIds) })
        binding.membersList.isVisible = members.isNotEmpty()

        binding.membersEmpty.removeAllViews()
        if (members.isEmpty()) {
            val body = if (roleViewModel.role.value == Role.COLLECTOR) {
                R.string.artist_detail_members_empty_body_collector
            } else {
                R.string.artist_detail_members_empty_body
            }
            binding.membersEmpty.addEmptySection(
                R.drawable.ic_groups,
                getString(R.string.artist_detail_members_empty_title),
                getString(body),
            )
        }
    }

    private fun showAlbums(binding: FragmentArtistDetailBinding, detail: ArtistDetail) {
        val albums = detail.albums
        binding.albumsCount.text = resources.getQuantityString(R.plurals.artist_detail_albums_count, albums.size, albums.size)
        (binding.albumsList.adapter as ArtistAlbumAdapter).submitList(albums)
        binding.albumsList.isVisible = albums.isNotEmpty()

        binding.albumsEmpty.removeAllViews()
        if (albums.isEmpty()) {
            binding.albumsEmpty.addEmptyNote(getString(R.string.artist_detail_albums_empty_body))
        }
    }

    private fun showPrizes(binding: FragmentArtistDetailBinding, prizes: List<ArtistPrize>) {
        binding.prizesCount.text = resources.getQuantityString(R.plurals.artist_detail_prizes_count, prizes.size, prizes.size)
        binding.prizesList.removeAllViews()

        if (prizes.isEmpty()) {
            binding.prizesList.addEmptyNote(getString(R.string.artist_detail_prizes_empty_body))
            return
        }

        val gap = resources.getDimensionPixelSize(R.dimen.detail_section_gap)
        prizes.forEachIndexed { index, prize ->
            val card = ItemArtistPrizeBinding.inflate(layoutInflater, binding.prizesList, true)
            card.prizeName.text = prize.name
            card.prizeOrganization.text = prize.organization
            card.prizeDate.text = prize.date.toLongText()
            if (index > 0) card.root.updateLayoutParams<LinearLayout.LayoutParams> { topMargin = gap }
        }
    }

    /** Abre el detalle de un álbum del carrusel. */
    private fun openAlbum(album: Album) {
        val navController = findNavController()
        // Con dos toques seguidos el segundo ya no está en esta pantalla
        if (navController.currentBackStackEntry != ownEntry) return
        navController.navigateIfPossible(R.id.action_artistDetail_to_albumDetail, AlbumDetailFragment.args(album.id))
    }

    /** Abre el detalle de la banda del músico o de un integrante de la banda. */
    private fun openArtist(artist: Artist) {
        val navController = findNavController()
        // Con dos toques seguidos el segundo ya no está en esta pantalla
        if (navController.currentBackStackEntry != ownEntry) return
        navController.navigateIfPossible(R.id.action_artistDetail_to_artistDetail, args(artist))
    }

    /** Abre Agregar integrante para la banda (HU12). */
    private fun openMemberAdd(band: Artist) {
        val navController = findNavController()
        if (navController.currentBackStackEntry != ownEntry) return
        navController.navigateIfPossible(R.id.action_artistDetail_to_memberAdd, MemberAddFragment.args(band.id, band.name))
    }

    companion object {
        /** Deben coincidir con los argumentos de artistDetailFragment en nav_graph.xml. */
        const val ARG_ARTIST_ID = "artistId"

        // Músicos y bandas se consultan en rutas distintas de la API, por eso viaja también el tipo
        const val ARG_ARTIST_TYPE = "artistType"

        fun args(artist: Artist) = args(artist.id, artist.type)

        fun args(artistId: Int, type: ArtistType) = bundleOf(ARG_ARTIST_ID to artistId, ARG_ARTIST_TYPE to type.name)
    }
}
