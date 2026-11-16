package com.equipo2.vinilos.ui.collectors.detail

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.AlbumStatus
import com.equipo2.vinilos.data.model.CollectorAlbum
import com.equipo2.vinilos.databinding.ItemCollectorAlbumBinding

/** Álbumes de la colección de un coleccionista con su precio y su estado, Activo o Inactivo (HU06). */
class CollectorAlbumAdapter : ListAdapter<CollectorAlbum, CollectorAlbumAdapter.CollectorAlbumViewHolder>(CollectorAlbumDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        CollectorAlbumViewHolder(ItemCollectorAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: CollectorAlbumViewHolder, position: Int) = holder.bind(getItem(position))

    class CollectorAlbumViewHolder(private val binding: ItemCollectorAlbumBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(album: CollectorAlbum) {
            val context = binding.root.context
            val isActive = album.status == AlbumStatus.ACTIVE
            val status = context.getString(if (isActive) R.string.collector_detail_status_active else R.string.collector_detail_status_inactive)

            binding.collectorAlbumName.text = album.name
            binding.collectorAlbumPrice.text = context.getString(R.string.collector_detail_album_price, album.price)

            val color = ContextCompat.getColor(context, if (isActive) R.color.md_on_primary_container else R.color.md_on_surface_variant)
            binding.collectorAlbumStatus.setBackgroundResource(if (isActive) R.drawable.bg_status_active else R.drawable.bg_status_inactive)
            binding.collectorAlbumStatusIcon.setImageResource(if (isActive) R.drawable.ic_check_circle else R.drawable.ic_pause_circle)
            ImageViewCompat.setImageTintList(binding.collectorAlbumStatusIcon, ColorStateList.valueOf(color))
            binding.collectorAlbumStatusLabel.text = status
            binding.collectorAlbumStatusLabel.setTextColor(color)

            // TalkBack lee la fila completa de una vez
            binding.root.contentDescription = context.getString(R.string.cd_collector_detail_album, album.name, album.price, status)
        }
    }

    private object CollectorAlbumDiff : DiffUtil.ItemCallback<CollectorAlbum>() {
        override fun areItemsTheSame(oldItem: CollectorAlbum, newItem: CollectorAlbum) = oldItem.albumId == newItem.albumId
        override fun areContentsTheSame(oldItem: CollectorAlbum, newItem: CollectorAlbum) = oldItem == newItem
    }
}
