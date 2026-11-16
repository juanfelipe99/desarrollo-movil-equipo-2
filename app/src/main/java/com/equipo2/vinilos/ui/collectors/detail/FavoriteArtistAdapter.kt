package com.equipo2.vinilos.ui.collectors.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.equipo2.vinilos.data.model.Artist
import com.equipo2.vinilos.databinding.ItemFavoriteArtistBinding
import com.equipo2.vinilos.ui.common.initialsOf

/** Fila horizontal con los artistas favoritos de un coleccionista: foto o iniciales y nombre (HU06). */
class FavoriteArtistAdapter : ListAdapter<Artist, FavoriteArtistAdapter.FavoriteViewHolder>(FavoriteDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        FavoriteViewHolder(ItemFavoriteArtistBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) = holder.bind(getItem(position))

    class FavoriteViewHolder(private val binding: ItemFavoriteArtistBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(artist: Artist) {
            binding.favoriteInitials.text = initialsOf(artist.name)
            // Sin imagen se borra la anterior de la fila reciclada y quedan las iniciales
            binding.favoritePhoto.load(artist.image.ifBlank { null })
            binding.favoriteName.text = artist.name
        }
    }

    private object FavoriteDiff : DiffUtil.ItemCallback<Artist>() {
        override fun areItemsTheSame(oldItem: Artist, newItem: Artist) = oldItem.id == newItem.id && oldItem.type == newItem.type
        override fun areContentsTheSame(oldItem: Artist, newItem: Artist) = oldItem == newItem
    }
}
