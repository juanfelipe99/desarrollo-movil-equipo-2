package com.equipo2.vinilos.ui.collectors.detail

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.CollectorComment
import com.equipo2.vinilos.databinding.ItemCollectorCommentBinding
import com.equipo2.vinilos.ui.common.showStars
import com.equipo2.vinilos.ui.common.setClickLabel

/** Comentarios de un coleccionista. Al tocar el nombre del álbum avisa con el comentario para abrir ese álbum (HU06). */
class CommentAdapter(
    private val onAlbumClick: (CollectorComment) -> Unit,
) : ListAdapter<CollectorComment, CommentAdapter.CommentViewHolder>(CommentDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val holder = CommentViewHolder(ItemCollectorCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        holder.albumLink.setOnClickListener {
            // Al animar un cambio de la lista la posición puede no existir todavía
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onAlbumClick(getItem(position))
        }
        return holder
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) = holder.bind(getItem(position))

    class CommentViewHolder(private val binding: ItemCollectorCommentBinding) : RecyclerView.ViewHolder(binding.root) {

        val albumLink = binding.commentAlbum

        init {
            // El nombre del álbum se ve como un enlace
            binding.commentAlbum.paintFlags = binding.commentAlbum.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        }

        fun bind(comment: CollectorComment) {
            val context = binding.root.context
            binding.commentAlbum.text = comment.albumName
            binding.commentAlbum.contentDescription = context.getString(R.string.cd_collector_detail_comment_album, comment.albumName)
            binding.commentAlbum.setClickLabel(context.getString(R.string.action_view_detail))
            binding.commentStars.showStars(comment.rating)
            binding.commentRating.text = context.getString(R.string.album_detail_comment_rating, comment.rating)
            binding.commentRatingRow.contentDescription = context.getString(R.string.cd_album_detail_comment_rating, comment.rating)
            binding.commentText.text = context.getString(R.string.album_detail_comment_text, comment.description)
        }
    }

    private object CommentDiff : DiffUtil.ItemCallback<CollectorComment>() {
        override fun areItemsTheSame(oldItem: CollectorComment, newItem: CollectorComment) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CollectorComment, newItem: CollectorComment) = oldItem == newItem
    }
}
