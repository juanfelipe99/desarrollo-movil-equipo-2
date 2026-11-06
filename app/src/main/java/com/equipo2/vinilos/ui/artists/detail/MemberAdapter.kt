package com.equipo2.vinilos.ui.artists.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.equipo2.vinilos.data.model.Artist
import com.equipo2.vinilos.databinding.ItemArtistMemberBinding
import com.equipo2.vinilos.ui.common.initialsOf

/** Integrantes de una banda. Al tocar uno avisa con su músico. */
class MemberAdapter(
    private val onMemberClick: (Artist) -> Unit,
) : ListAdapter<Artist, MemberAdapter.MemberViewHolder>(MemberDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val holder = MemberViewHolder(ItemArtistMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        holder.itemView.setOnClickListener {
            // Al animar un cambio de la lista la posición puede no existir todavía
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onMemberClick(getItem(position))
        }
        return holder
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) = holder.bind(getItem(position))

    class MemberViewHolder(private val binding: ItemArtistMemberBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(member: Artist) {
            binding.memberInitials.text = initialsOf(member.name)
            // Sin imagen se borra la anterior de la fila reciclada y quedan las iniciales
            binding.memberPhoto.load(member.image.ifBlank { null })
            binding.memberName.text = member.name
        }
    }

    private object MemberDiff : DiffUtil.ItemCallback<Artist>() {
        override fun areItemsTheSame(oldItem: Artist, newItem: Artist) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Artist, newItem: Artist) = oldItem == newItem
    }
}
