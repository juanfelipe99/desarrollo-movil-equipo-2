package com.equipo2.vinilos.ui.albums.detail

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil3.load
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.Album
import com.equipo2.vinilos.data.model.AlbumComment
import com.equipo2.vinilos.data.model.Performer
import com.equipo2.vinilos.data.model.Role
import com.equipo2.vinilos.data.model.Track
import com.equipo2.vinilos.databinding.FragmentAlbumDetailBinding
import com.equipo2.vinilos.databinding.ItemAlbumCommentBinding
import com.equipo2.vinilos.databinding.ItemAlbumInfoBinding
import com.equipo2.vinilos.databinding.ItemTrackBinding
import com.equipo2.vinilos.databinding.ViewPerformerChipBinding
import com.equipo2.vinilos.ui.albums.tracks.TrackAddSheet
import com.equipo2.vinilos.ui.common.bindPullToRefresh
import com.equipo2.vinilos.ui.common.collectWhileVisible
import com.equipo2.vinilos.ui.common.initialsOf
import com.equipo2.vinilos.ui.common.navigateIfPossible
import com.equipo2.vinilos.ui.common.showSnackbar
import com.equipo2.vinilos.ui.common.toLongText
import com.equipo2.vinilos.ui.role.RoleViewModel
import com.equipo2.vinilos.ui.state.UiState
import com.equipo2.vinilos.ui.state.addEmptySection
import com.equipo2.vinilos.ui.state.showError
import com.equipo2.vinilos.ui.state.showLoading

/** Detalle de álbum (HU02), donde el coleccionista también agrega tracks (HU08). */
class AlbumDetailFragment : Fragment(R.layout.fragment_album_detail) {

    private val roleViewModel: RoleViewModel by activityViewModels { RoleViewModel.Factory }
    private val viewModel: AlbumDetailViewModel by viewModels {
        AlbumDetailViewModel.factory(requireArguments().getInt(ARG_ALBUM_ID))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentAlbumDetailBinding.bind(view)
        bindPullToRefresh(binding.albumRefresh, viewModel.pullRefresh, binding.albumScroll, binding.albumError.root) {
            viewModel.refresh()
        }

        viewLifecycleOwner.collectWhileVisible(viewModel.state) { state ->
            when (state) {
                UiState.Loading ->
                    binding.albumLoading.showLoading(getString(R.string.album_detail_loading), R.layout.view_skeleton_detail)
                is UiState.Error -> binding.albumError.showError(getString(R.string.album_detail_object), state.reason) { viewModel.retry() }
                is UiState.Success -> showAlbum(binding, state.data)
                UiState.Empty -> Unit
            }

            binding.albumLoading.root.isVisible = state == UiState.Loading
            binding.albumError.root.isVisible = state is UiState.Error
            binding.albumContent.isVisible = state is UiState.Success
            binding.albumRefresh.isEnabled = state != UiState.Loading
        }

        // Solo el coleccionista agrega tracks, y el vacío de tracks le dice cómo
        viewLifecycleOwner.collectWhileVisible(roleViewModel.role) { role ->
            binding.addTrackButton.isVisible = role == Role.COLLECTOR
            (viewModel.state.value as? UiState.Success)?.let { showTracks(binding, it.data.tracks) }
        }
        showAddedTrack()
    }

    /** Al cerrarse la hoja con un track nuevo, aparece al final de la lista marcado y con el aviso «Track agregado». */
    private fun showAddedTrack() {
        val savedState = findNavController().currentBackStackEntry?.savedStateHandle ?: return

        viewLifecycleOwner.collectWhileVisible(savedState.getStateFlow<Bundle?>(TrackAddSheet.ADDED_TRACK, null)) { added ->
            if (added == null) return@collectWhileVisible
            viewModel.onTrackAdded(TrackAddSheet.trackOf(added))
            showSnackbar(getString(R.string.snackbar_track_added))
            // Se quita para que el aviso no se repita al girar
            savedState.remove<Bundle>(TrackAddSheet.ADDED_TRACK)
        }
    }

    private fun showAlbum(binding: FragmentAlbumDetailBinding, album: Album) {
        binding.albumCover.load(album.cover)
        binding.albumName.text = album.name
        showPerformers(binding, album.performers)

        val releaseDate = album.releaseDate.toLongText()
        binding.albumReleaseDate.show(R.drawable.ic_calendar_today, R.string.album_detail_release_date_label, releaseDate)
        binding.albumGenre.show(R.drawable.ic_music_note, R.string.album_detail_genre_label, album.genre)
        binding.albumRecordLabel.show(R.drawable.ic_sell, R.string.album_detail_record_label_label, album.recordLabel)

        showDescription(binding, album.description)
        showTracks(binding, album.tracks)
        binding.addTrackButton.setOnClickListener {
            findNavController().navigateIfPossible(R.id.action_albumDetail_to_trackAdd, TrackAddSheet.args(album.id, album.name))
        }
        showComments(binding, album.comments)
    }

    private fun ItemAlbumInfoBinding.show(@DrawableRes icon: Int, @StringRes label: Int, value: String) {
        infoIcon.setImageResource(icon)
        infoLabel.setText(label)
        infoValue.text = value
    }

    /** Un chip por intérprete, con su foto o sus iniciales. */
    private fun showPerformers(binding: FragmentAlbumDetailBinding, performers: List<Performer>) {
        binding.albumPerformers.removeAllViews()
        binding.albumNoPerformers.isVisible = performers.isEmpty()

        performers.forEach { performer ->
            val chip = ViewPerformerChipBinding.inflate(layoutInflater, binding.albumPerformers, true)
            chip.performerInitials.text = initialsOf(performer.name)
            chip.performerName.text = performer.name
            chip.performerPhoto.load(performer.image)
        }
    }

    /** Los textos largos se cortan en la última palabra antes de 170 caracteres, y «Ver más» los muestra completos. */
    private fun showDescription(binding: FragmentAlbumDetailBinding, description: String) {
        val isLong = description.length > DESCRIPTION_PREVIEW_LENGTH
        val preview = description.take(DESCRIPTION_PREVIEW_LENGTH).substringBeforeLast(' ').trimEnd() + "…"
        var expanded = false

        fun render() {
            binding.albumDescription.text = if (expanded || !isLong) description else preview
            binding.albumDescriptionToggle.setText(if (expanded) R.string.album_detail_collapse else R.string.album_detail_expand)
        }

        binding.albumDescriptionToggle.isVisible = isLong
        binding.albumDescriptionToggle.setOnClickListener {
            expanded = !expanded
            render()
        }
        render()
    }

    private fun showTracks(binding: FragmentAlbumDetailBinding, tracks: List<Track>) {
        binding.tracksCount.text = resources.getQuantityString(R.plurals.album_detail_tracks_count, tracks.size, tracks.size)
        binding.tracksList.removeAllViews()
        binding.tracksEmpty.removeAllViews()
        binding.tracksListEnd.isVisible = tracks.isNotEmpty()

        if (tracks.isEmpty()) {
            val body = if (roleViewModel.role.value == Role.COLLECTOR) {
                R.string.album_detail_tracks_empty_body_collector
            } else {
                R.string.album_detail_tracks_empty_body
            }
            binding.tracksEmpty.addEmptySection(
                R.drawable.ic_music_note,
                getString(R.string.album_detail_tracks_empty_title),
                getString(body),
            )
            return
        }

        val newIds = viewModel.newTrackIds.value
        tracks.forEachIndexed { index, track ->
            val row = ItemTrackBinding.inflate(layoutInflater, binding.tracksList, true)
            row.trackNumber.text = getString(R.string.album_detail_track_number, index + 1)
            row.trackName.text = track.name
            row.trackDuration.text = track.duration

            val isNew = track.id in newIds
            row.trackNewBadge.isVisible = isNew
            if (isNew) row.trackRow.setBackgroundResource(R.color.new_row)
        }
    }

    private fun showComments(binding: FragmentAlbumDetailBinding, comments: List<AlbumComment>) {
        binding.commentsCount.text = resources.getQuantityString(R.plurals.album_detail_comments_count, comments.size, comments.size)
        binding.commentsList.removeAllViews()

        if (comments.isEmpty()) {
            binding.commentsList.addSectionSpacing()
            binding.commentsList.addEmptySection(
                R.drawable.ic_chat_bubble,
                getString(R.string.album_detail_comments_empty_title),
                getString(R.string.album_detail_comments_empty_body),
            )
            return
        }

        comments.forEach { comment ->
            binding.commentsList.addSectionSpacing()
            val card = ItemAlbumCommentBinding.inflate(layoutInflater, binding.commentsList, true)
            val author = comment.author ?: getString(R.string.album_detail_comment_author_unknown)
            card.commentInitials.text = initialsOf(author)
            card.commentAuthor.text = author
            card.commentRating.text = getString(R.string.album_detail_comment_rating, comment.rating)
            card.commentRatingRow.contentDescription = getString(R.string.cd_album_detail_comment_rating, comment.rating)
            card.commentText.text = getString(R.string.album_detail_comment_text, comment.description)
            showStars(card.commentStars, comment.rating)
        }
    }

    /** Cinco estrellas, rellenas hasta la calificación. */
    private fun showStars(container: LinearLayout, rating: Int) {
        val size = resources.getDimensionPixelSize(R.dimen.rating_star)
        val gap = resources.getDimensionPixelSize(R.dimen.rating_star_gap)
        repeat(MAX_RATING) { index ->
            val star = ImageView(requireContext())
            star.setImageResource(R.drawable.ic_star)
            val color = if (index < rating) R.color.md_tertiary else R.color.md_outline_variant
            star.setColorFilter(ContextCompat.getColor(requireContext(), color))
            container.addView(star, LinearLayout.LayoutParams(size, size).apply { if (index > 0) marginStart = gap })
        }
    }

    private fun LinearLayout.addSectionSpacing() {
        addView(View(context), LinearLayout.LayoutParams(0, resources.getDimensionPixelSize(R.dimen.detail_section_gap)))
    }

    companion object {
        /** Debe coincidir con el argumento de albumDetailFragment en nav_graph.xml. */
        private const val ARG_ALBUM_ID = "albumId"

        fun args(albumId: Int): Bundle = bundleOf(ARG_ALBUM_ID to albumId)

        private const val DESCRIPTION_PREVIEW_LENGTH = 170
        private const val MAX_RATING = 5
    }
}
