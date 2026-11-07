package com.equipo2.vinilos.ui.artists.detail

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.Artist
import com.equipo2.vinilos.databinding.ItemArtistMemberBinding
import com.equipo2.vinilos.ui.common.initialsOf

/** Integrante de la banda en su detalle. [isNew] si se agregó desde esta pantalla (HU12). */
data class BandMemberRow(val member: Artist, val isNew: Boolean = false)

/** Integrantes de una banda. Al tocar uno avisa con su músico. */
class MemberAdapter(
    private val onMemberClick: (Artist) -> Unit,
) : ListAdapter<BandMemberRow, MemberAdapter.MemberViewHolder>(MemberDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val holder = MemberViewHolder(ItemArtistMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        holder.itemView.setOnClickListener {
            // Al animar un cambio de la lista la posición puede no existir todavía
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onMemberClick(getItem(position).member)
        }
        return holder
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) = holder.bind(getItem(position))

    class MemberViewHolder(private val binding: ItemArtistMemberBinding) : RecyclerView.ViewHolder(binding.root) {

        private val normalBackground: Drawable? = binding.root.background
        private val newBackground by lazy { ContextCompat.getDrawable(binding.root.context, R.drawable.bg_member_row_new) }

        fun bind(row: BandMemberRow) {
            val member = row.member
            binding.memberInitials.text = initialsOf(member.name)
            // Sin imagen se borra la anterior de la fila reciclada y quedan las iniciales
            binding.memberPhoto.load(member.image.ifBlank { null })
            binding.memberName.text = member.name

            binding.memberNewBadge.isVisible = row.isNew
            binding.root.background = if (row.isNew) newBackground else normalBackground
        }
    }

    private object MemberDiff : DiffUtil.ItemCallback<BandMemberRow>() {
        override fun areItemsTheSame(oldItem: BandMemberRow, newItem: BandMemberRow) = oldItem.member.id == newItem.member.id
        override fun areContentsTheSame(oldItem: BandMemberRow, newItem: BandMemberRow) = oldItem == newItem
    }
}
