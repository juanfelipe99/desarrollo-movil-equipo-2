package com.equipo2.vinilos.ui.artists.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.Album
import com.equipo2.vinilos.databinding.ItemArtistAlbumBinding

/** Carrusel horizontal con los álbumes del artista: portada, nombre, año y género. Al tocar uno avisa con su álbum. */
class ArtistAlbumAdapter(
    private val onAlbumClick: (Album) -> Unit,
) : ListAdapter<Album, ArtistAlbumAdapter.ArtistAlbumViewHolder>(AlbumDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistAlbumViewHolder {
        val holder = ArtistAlbumViewHolder(ItemArtistAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        holder.itemView.setOnClickListener {
            // Al animar un cambio de la lista la posición puede no existir todavía
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onAlbumClick(getItem(position))
        }
        return holder
    }

    override fun onBindViewHolder(holder: ArtistAlbumViewHolder, position: Int) = holder.bind(getItem(position))

    class ArtistAlbumViewHolder(private val binding: ItemArtistAlbumBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(album: Album) {
            binding.artistAlbumCover.load(album.cover)
            binding.artistAlbumName.text = album.name
            binding.artistAlbumSubtitle.text =
                binding.root.context.getString(R.string.album_list_subtitle, album.releaseDate.year, album.genre)
        }
    }

    private object AlbumDiff : DiffUtil.ItemCallback<Album>() {
        override fun areItemsTheSame(oldItem: Album, newItem: Album) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Album, newItem: Album) = oldItem == newItem
    }
}
