package com.equipo2.vinilos.ui.collectors.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.Collector
import com.equipo2.vinilos.databinding.ItemCollectorBinding
import com.equipo2.vinilos.ui.common.initialsOf

/** Filas del listado. Al tocar una avisa con su coleccionista. */
class CollectorAdapter(
    private val onCollectorClick: (Collector) -> Unit,
) : ListAdapter<Collector, CollectorAdapter.CollectorViewHolder>(CollectorDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectorViewHolder {
        val holder = CollectorViewHolder(ItemCollectorBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        holder.itemView.setOnClickListener {
            // Al animar un cambio de la lista la posición puede no existir todavía
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onCollectorClick(getItem(position))
        }
        return holder
    }

    override fun onBindViewHolder(holder: CollectorViewHolder, position: Int) = holder.bind(getItem(position))

    class CollectorViewHolder(private val binding: ItemCollectorBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(collector: Collector) {
            binding.collectorInitials.text = initialsOf(collector.name)
            binding.collectorName.text = collector.name
            binding.collectorAlbumCount.text = binding.root.resources.getQuantityString(
                R.plurals.collector_album_count,
                collector.albumCount,
                collector.albumCount,
            )
            binding.collectorEmail.text = collector.email
        }
    }

    private object CollectorDiff : DiffUtil.ItemCallback<Collector>() {
        override fun areItemsTheSame(oldItem: Collector, newItem: Collector) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Collector, newItem: Collector) = oldItem == newItem
    }
}
