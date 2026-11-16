package com.equipo2.vinilos.ui.collectors.detail

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavBackStackEntry
import androidx.navigation.fragment.findNavController
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.Collector
import com.equipo2.vinilos.data.model.CollectorComment
import com.equipo2.vinilos.data.model.CollectorDetail
import com.equipo2.vinilos.databinding.FragmentCollectorDetailBinding
import com.equipo2.vinilos.databinding.ItemAlbumInfoBinding
import com.equipo2.vinilos.ui.albums.detail.AlbumDetailFragment
import com.equipo2.vinilos.ui.common.bindPullToRefresh
import com.equipo2.vinilos.ui.common.collectWhileVisible
import com.equipo2.vinilos.ui.common.initialsOf
import com.equipo2.vinilos.ui.common.navigateIfPossible
import com.equipo2.vinilos.ui.state.UiState
import com.equipo2.vinilos.ui.state.addEmptyNote
import com.equipo2.vinilos.ui.state.showError
import com.equipo2.vinilos.ui.state.showLoading

/** Detalle de un coleccionista (HU06): contacto, artistas favoritos, colección de álbumes y comentarios. */
class CollectorDetailFragment : Fragment(R.layout.fragment_collector_detail) {

    private val viewModel: CollectorDetailViewModel by viewModels {
        CollectorDetailViewModel.factory(requireArguments().getInt(ARG_COLLECTOR_ID))
    }

    /** Entrada de esta pantalla, para no abrir dos álbumes con dos toques seguidos. */
    private var ownEntry: NavBackStackEntry? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ownEntry = findNavController().currentBackStackEntry
        val binding = FragmentCollectorDetailBinding.bind(view)
        binding.favoritesList.adapter = FavoriteArtistAdapter()
        binding.collectorAlbumsList.adapter = CollectorAlbumAdapter()
        binding.commentsList.adapter = CommentAdapter(::openAlbum)

        bindPullToRefresh(binding.collectorRefresh, viewModel.pullRefresh, binding.collectorScroll, binding.collectorError.root) {
            viewModel.refresh()
        }

        viewLifecycleOwner.collectWhileVisible(viewModel.state) { state ->
            when (state) {
                UiState.Loading ->
                    binding.collectorLoading.showLoading(getString(R.string.collector_detail_loading), R.layout.view_skeleton_detail)
                is UiState.Error ->
                    binding.collectorError.showError(getString(R.string.collector_detail_object), state.reason) { viewModel.retry() }
                is UiState.Success -> showCollector(binding, state.data)
                UiState.Empty -> Unit
            }

            binding.collectorLoading.root.isVisible = state == UiState.Loading
            binding.collectorError.root.isVisible = state is UiState.Error
            binding.collectorContent.isVisible = state is UiState.Success
            binding.collectorRefresh.isEnabled = state != UiState.Loading
        }
    }

    private fun showCollector(binding: FragmentCollectorDetailBinding, detail: CollectorDetail) {
        val collector = detail.collector
        val albumCount = detail.albums.size

        binding.collectorInitials.text = initialsOf(collector.name)
        binding.collectorName.text = collector.name
        binding.collectorSubtitle.text = resources.getQuantityString(R.plurals.collector_detail_subtitle, albumCount, albumCount)

        showContact(binding.collectorPhone, R.drawable.ic_phone, R.string.collector_detail_phone_label, detail.telephone)
        showContact(binding.collectorEmail, R.drawable.ic_mail, R.string.collector_detail_email_label, collector.email)

        (binding.favoritesList.adapter as FavoriteArtistAdapter).submitList(detail.favorites)
        binding.favoritesList.isVisible = detail.favorites.isNotEmpty()
        showEmptyNote(binding.favoritesEmpty, detail.favorites.isEmpty(), R.string.collector_detail_favorites_empty_body)

        binding.collectorAlbumsCount.text =
            resources.getQuantityString(R.plurals.collector_detail_albums_count, albumCount, albumCount)
        (binding.collectorAlbumsList.adapter as CollectorAlbumAdapter).submitList(detail.albums)
        binding.collectorAlbumsList.isVisible = detail.albums.isNotEmpty()
        showEmptyNote(binding.collectorAlbumsEmpty, detail.albums.isEmpty(), R.string.collector_detail_albums_empty_body)

        (binding.commentsList.adapter as CommentAdapter).submitList(detail.comments)
        binding.commentsList.isVisible = detail.comments.isNotEmpty()
        showEmptyNote(binding.commentsEmpty, detail.comments.isEmpty(), R.string.collector_detail_comments_empty_body)
    }

    /** Teléfono o correo. Si la API no lo trae se dice que no está registrado en vez de dejar la fila en blanco. */
    private fun showContact(row: ItemAlbumInfoBinding, @DrawableRes icon: Int, @StringRes label: Int, value: String) {
        row.infoIcon.setImageResource(icon)
        row.infoLabel.setText(label)
        row.infoValue.text = value.ifBlank { getString(R.string.collector_detail_no_data) }
    }

    private fun showEmptyNote(container: ViewGroup,isEmpty: Boolean, @StringRes body: Int) {
        container.removeAllViews()
        if (isEmpty) container.addEmptyNote(getString(body))
    }

    /** Abre el detalle del álbum comentado sin salir de la pestaña. */
    private fun openAlbum(comment: CollectorComment) {
        val navController = findNavController()
        // Con dos toques seguidos el segundo ya no está en esta pantalla
        if (navController.currentBackStackEntry != ownEntry) return
        navController.navigateIfPossible(R.id.action_collectorDetail_to_albumDetail, AlbumDetailFragment.args(comment.albumId))
    }

    companion object {
        /** Debe coincidir con el argumento de collectorDetailFragment en nav_graph.xml. */
        const val ARG_COLLECTOR_ID = "collectorId"

        fun args(collector: Collector) = bundleOf(ARG_COLLECTOR_ID to collector.id)
    }
}
