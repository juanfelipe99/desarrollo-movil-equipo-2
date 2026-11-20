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
import com.equipo2.vinilos.ui.artists.albums.AlbumAddFragment
import com.equipo2.vinilos.ui.artists.members.MemberAddFragment
import com.equipo2.vinilos.ui.artists.prizes.PrizeAddFragment
import com.equipo2.vinilos.ui.common.bindExpandableText
import com.equipo2.vinilos.ui.common.bindPullToRefresh
import com.equipo2.vinilos.ui.common.collectResult
import com.equipo2.vinilos.ui.common.collectWhileVisible
import com.equipo2.vinilos.ui.common.initialsOf
import com.equipo2.vinilos.ui.common.navigateIfPossible
import com.equipo2.vinilos.ui.common.ownBackStackEntry
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
 * y el coleccionista agrega integrantes a la banda (HU12), álbumes (HU15) y premios (HU14) a cualquier artista.
 */
class ArtistDetailFragment : Fragment(R.layout.fragment_artist_detail) {

    private val roleViewModel: RoleViewModel by activityViewModels { RoleViewModel.Factory }
    private val viewModel: ArtistDetailViewModel by viewModels {
        val args = requireArguments()
        ArtistDetailViewModel.factory(args.getInt(ARG_ARTIST_ID), ArtistType.valueOf(requireNotNull(args.getString(ARG_ARTIST_TYPE))))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        // Solo el coleccionista agrega integrantes, álbumes y premios, y el vacío de cada sección le dice cómo
        viewLifecycleOwner.collectWhileVisible(roleViewModel.role) { role ->
            binding.addMemberButton.isVisible = role == Role.COLLECTOR
            binding.addAlbumButton.isVisible = role == Role.COLLECTOR
            binding.addPrizeButton.isVisible = role == Role.COLLECTOR
            (viewModel.state.value as? UiState.Success)?.let {
                showMembers(binding, it.data.members)
                showAlbums(binding, it.data.albums)
                showPrizes(binding, it.data.prizes)
            }
        }
        viewLifecycleOwner.collectWhileVisible(viewModel.newMemberIds) {
            (viewModel.state.value as? UiState.Success)?.let { showMembers(binding, it.data.members) }
        }
        viewLifecycleOwner.collectWhileVisible(viewModel.newPrizeIds) {
            (viewModel.state.value as? UiState.Success)?.let { showPrizes(binding, it.data.prizes) }
        }
        viewLifecycleOwner.collectWhileVisible(viewModel.newAlbumIds) {
            (viewModel.state.value as? UiState.Success)?.let { showAlbums(binding, it.data.albums) }
        }
        showAddedMember()
        showAddedAlbum()
        showAddedPrize()
    }

    /** Al volver de Agregar álbum, el álbum aparece primero en el carrusel marcado y con el aviso «Álbum agregado». */
    private fun showAddedAlbum() {
        collectResult<Bundle>(AlbumAddFragment.ADDED_ALBUM) { added ->
            viewModel.onAlbumAdded(AlbumAddFragment.albumOf(added))
            showSnackbar(getString(R.string.snackbar_album_added))
        }
    }

    /** Al volver de Agregar integrante, el músico aparece al final de la lista marcado y con el aviso «Integrante agregado». */
    private fun showAddedMember() {
        collectResult<Bundle>(MemberAddFragment.ADDED_MEMBER) { added ->
            viewModel.onMemberAdded(MemberAddFragment.memberOf(added))
            showSnackbar(getString(R.string.snackbar_member_added))
        }
    }

    /** Al volver de Agregar premio, el premio aparece primero marcado y con el aviso «Premio agregado». */
    private fun showAddedPrize() {
        collectResult<Bundle>(PrizeAddFragment.ADDED_PRIZE) { added ->
            viewModel.onPrizeAdded(PrizeAddFragment.prizeOf(added))
            showSnackbar(getString(R.string.snackbar_prize_added))
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

        bindExpandableText(binding.artistDescription, binding.artistDescriptionToggle, detail.description, viewModel.descriptionExpanded) {
            viewModel.descriptionExpanded = it
        }

        binding.membersSection.isVisible = !isMusician
        if (!isMusician) {
            showMembers(binding, detail.members)
            binding.addMemberButton.setOnClickListener { openMemberAdd(artist) }
        }
        showAlbums(binding, detail.albums)
        binding.addAlbumButton.setOnClickListener { openAlbumAdd(artist) }
        showPrizes(binding, detail.prizes)
        binding.addPrizeButton.setOnClickListener { openPrizeAdd(artist) }
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

    /**
     * Los álbumes agregados desde aquí van primero y marcados como «Nuevo», el último antes, también tras recargar.
     * Los demás siguen en el orden en que llegan.
     */
    private fun showAlbums(binding: FragmentArtistDetailBinding, albums: List<Album>) {
        binding.albumsCount.text = resources.getQuantityString(R.plurals.artist_detail_albums_count, albums.size, albums.size)
        val newIds = viewModel.newAlbumIds.value
        val rows = albums
            .sortedBy { album -> newIds.indexOf(album.id).let { if (it < 0) Int.MAX_VALUE else it } }
            .map { ArtistAlbumRow(it, isNew = it.id in newIds) }

        val adapter = binding.albumsList.adapter as ArtistAlbumAdapter
        val firstBefore = adapter.currentList.firstOrNull()?.album?.id
        adapter.submitList(rows) {
            // Al insertar al principio, el carrusel se queda en el álbum que veía y el nuevo quedaría fuera de la vista
            val first = rows.firstOrNull()
            if (first != null && first.isNew && first.album.id != firstBefore) binding.albumsList.scrollToPosition(0)
        }
        binding.albumsList.isVisible = albums.isNotEmpty()

        binding.albumsEmpty.removeAllViews()
        if (albums.isEmpty()) {
            val body = if (roleViewModel.role.value == Role.COLLECTOR) {
                R.string.artist_detail_albums_empty_body_collector
            } else {
                R.string.artist_detail_albums_empty_body
            }
            binding.albumsEmpty.addEmptyNote(getString(body))
        }
    }

    private fun showPrizes(binding: FragmentArtistDetailBinding, prizes: List<ArtistPrize>) {
        binding.prizesCount.text = resources.getQuantityString(R.plurals.artist_detail_prizes_count, prizes.size, prizes.size)
        binding.prizesList.removeAllViews()

        if (prizes.isEmpty()) {
            val body = if (roleViewModel.role.value == Role.COLLECTOR) {
                R.string.artist_detail_prizes_empty_body_collector
            } else {
                R.string.artist_detail_prizes_empty_body
            }
            binding.prizesList.addEmptyNote(getString(body))
            return
        }

        val gap = resources.getDimensionPixelSize(R.dimen.detail_section_gap)
        val newIds = viewModel.newPrizeIds.value
        prizes.forEachIndexed { index, prize ->
            val card = ItemArtistPrizeBinding.inflate(layoutInflater, binding.prizesList, true)
            card.prizeName.text = prize.name
            card.prizeOrganization.text = prize.organization
            card.prizeDate.text = prize.date.toLongText()
            // El premio recién agregado lleva «Nuevo» y el fondo de fila nueva
            if (prize.id in newIds) {
                card.prizeNewBadge.isVisible = true
                card.root.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.new_row))
            }
            if (index > 0) card.root.updateLayoutParams<LinearLayout.LayoutParams> { topMargin = gap }
        }
    }

    /** Abre el detalle de un álbum del carrusel. */
    private fun openAlbum(album: Album) {
        val navController = findNavController()
        // Si ya no es la pantalla actual, un toque anterior ya abrió otra
        if (navController.currentBackStackEntry != ownBackStackEntry) return
        navController.navigateIfPossible(R.id.action_artistDetail_to_albumDetail, AlbumDetailFragment.args(album.id))
    }

    /** Abre el detalle de la banda del músico o de un integrante de la banda. */
    private fun openArtist(artist: Artist) {
        val navController = findNavController()
        // Si ya no es la pantalla actual, un toque anterior ya abrió otra
        if (navController.currentBackStackEntry != ownBackStackEntry) return
        navController.navigateIfPossible(R.id.action_artistDetail_to_artistDetail, args(artist))
    }

    /** Abre Agregar integrante para la banda (HU12). */
    private fun openMemberAdd(band: Artist) {
        val navController = findNavController()
        // Si ya no es la pantalla actual, un toque anterior ya abrió otra
        if (navController.currentBackStackEntry != ownBackStackEntry) return
        navController.navigateIfPossible(R.id.action_artistDetail_to_memberAdd, MemberAddFragment.args(band.id, band.name))
    }

    /** Abre Agregar álbum para el músico o la banda (HU15). */
    private fun openAlbumAdd(artist: Artist) {
        val navController = findNavController()
        // Si ya no es la pantalla actual, un toque anterior ya abrió otra
        if (navController.currentBackStackEntry != ownBackStackEntry) return
        navController.navigateIfPossible(R.id.action_artistDetail_to_albumAdd, AlbumAddFragment.args(artist))
    }

    /** Abre Agregar premio para el artista (HU14). */
    private fun openPrizeAdd(artist: Artist) {
        val navController = findNavController()
        // Si ya no es la pantalla actual, un toque anterior ya abrió otra
        if (navController.currentBackStackEntry != ownBackStackEntry) return
        navController.navigateIfPossible(R.id.action_artistDetail_to_prizeAdd, PrizeAddFragment.args(artist))
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
