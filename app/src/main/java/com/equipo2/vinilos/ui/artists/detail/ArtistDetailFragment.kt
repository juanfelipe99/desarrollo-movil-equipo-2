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
import androidx.fragment.app.viewModels
import androidx.navigation.NavBackStackEntry
import androidx.navigation.fragment.findNavController
import coil3.load
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.Artist
import com.equipo2.vinilos.data.model.ArtistDetail
import com.equipo2.vinilos.data.model.ArtistPrize
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.databinding.FragmentArtistDetailBinding
import com.equipo2.vinilos.databinding.ItemArtistPrizeBinding
import com.equipo2.vinilos.ui.common.bindExpandableText
import com.equipo2.vinilos.ui.common.bindPullToRefresh
import com.equipo2.vinilos.ui.common.collectWhileVisible
import com.equipo2.vinilos.ui.common.initialsOf
import com.equipo2.vinilos.ui.common.navigateIfPossible
import com.equipo2.vinilos.ui.common.toLongText
import com.equipo2.vinilos.ui.state.UiState
import com.equipo2.vinilos.ui.state.addEmptySection
import com.equipo2.vinilos.ui.state.showError
import com.equipo2.vinilos.ui.state.showLoading

/** Detalle de un músico o de una banda (HU04). Desde aquí se pasa del músico a su banda y de la banda a sus integrantes. */
class ArtistDetailFragment : Fragment(R.layout.fragment_artist_detail) {

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
        binding.albumsList.adapter = ArtistAlbumAdapter()

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
        showBand(binding, detail.band)

        bindExpandableText(binding.artistDescription, binding.artistDescriptionToggle, detail.description)

        binding.membersSection.isVisible = !isMusician
        if (!isMusician) showMembers(binding, detail.members)
        showAlbums(binding, detail)
        showPrizes(binding, detail.prizes)
    }

    /** La banda del músico, subrayada como un enlace que abre su detalle. Un músico sin banda no muestra la fila. */
    private fun showBand(binding: FragmentArtistDetailBinding, band: Artist?) {
        binding.artistBandDivider.isVisible = band != null
        binding.artistBandRow.isVisible = band != null
        if (band == null) return

        binding.artistBand.infoIcon.setImageResource(R.drawable.ic_groups)
        binding.artistBand.infoLabel.setText(R.string.artist_detail_band_label)
        binding.artistBand.infoValue.text = band.name
        binding.artistBand.infoValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_primary))
        binding.artistBand.infoValue.paintFlags = binding.artistBand.infoValue.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.artistBandRow.contentDescription = getString(R.string.cd_artist_detail_band, band.name)
        binding.artistBandRow.setOnClickListener { openArtist(band) }
    }

    private fun showMembers(binding: FragmentArtistDetailBinding, members: List<Artist>) {
        binding.membersCount.text = resources.getQuantityString(R.plurals.artist_detail_members_count, members.size, members.size)
        (binding.membersList.adapter as MemberAdapter).submitList(members)
        binding.membersList.isVisible = members.isNotEmpty()

        binding.membersEmpty.removeAllViews()
        if (members.isEmpty()) {
            binding.membersEmpty.addEmptySection(
                R.drawable.ic_groups,
                getString(R.string.artist_detail_members_empty_title),
                getString(R.string.artist_detail_members_empty_body),
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
            binding.albumsEmpty.addEmptySection(
                R.drawable.ic_album,
                getString(R.string.artist_detail_albums_empty_title),
                getString(R.string.artist_detail_albums_empty_body),
            )
        }
    }

    private fun showPrizes(binding: FragmentArtistDetailBinding, prizes: List<ArtistPrize>) {
        binding.prizesCount.text = resources.getQuantityString(R.plurals.artist_detail_prizes_count, prizes.size, prizes.size)
        binding.prizesList.removeAllViews()

        if (prizes.isEmpty()) {
            binding.prizesList.addEmptySection(
                R.drawable.ic_trophy,
                getString(R.string.artist_detail_prizes_empty_title),
                getString(R.string.artist_detail_prizes_empty_body),
            )
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

    /** Abre el detalle de la banda del músico o de un integrante de la banda. */
    private fun openArtist(artist: Artist) {
        val navController = findNavController()
        // Con dos toques seguidos el segundo ya no está en esta pantalla
        if (navController.currentBackStackEntry != ownEntry) return
        navController.navigateIfPossible(R.id.action_artistDetail_to_artistDetail, args(artist))
    }

    companion object {
        /** Deben coincidir con los argumentos de artistDetailFragment en nav_graph.xml. */
        const val ARG_ARTIST_ID = "artistId"

        // Músicos y bandas se consultan en rutas distintas de la API, por eso viaja también el tipo
        const val ARG_ARTIST_TYPE = "artistType"

        fun args(artist: Artist) = bundleOf(ARG_ARTIST_ID to artist.id, ARG_ARTIST_TYPE to artist.type.name)
    }
}
