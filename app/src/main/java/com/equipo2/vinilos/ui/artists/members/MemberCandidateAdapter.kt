package com.equipo2.vinilos.ui.artists.members

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.MusicianCandidate
import com.equipo2.vinilos.data.model.MusicianStatus
import com.equipo2.vinilos.databinding.ItemMemberCandidateBinding
import com.equipo2.vinilos.ui.common.initialsOf

/** Fila del listado: el músico, si es el elegido y si se puede tocar mientras no haya un envío en curso. */
data class MemberRow(
    val candidate: MusicianCandidate,
    val selected: Boolean,
    val enabled: Boolean,
)

/** Lista de selección única. Al tocar un músico que se puede agregar avisa con él. */
class MemberCandidateAdapter(
    private val onCandidateClick: (MusicianCandidate) -> Unit,
) : ListAdapter<MemberRow, MemberCandidateAdapter.MemberViewHolder>(MemberRowDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemMemberCandidateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = MemberViewHolder(binding)
        binding.root.setOnClickListener {
            // Al animar un cambio de la lista la posición puede no existir todavía
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onCandidateClick(getItem(position).candidate)
        }
        return holder
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) = holder.bind(getItem(position))

    class MemberViewHolder(private val binding: ItemMemberCandidateBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: MemberRow) {
            val context = binding.root.context
            val musician = row.candidate.musician
            val status = row.candidate.status

            binding.memberInitials.text = initialsOf(musician.name)
            // Sin imagen se borra la anterior de la fila reciclada y quedan las iniciales
            binding.memberPhoto.load(musician.image.ifBlank { null })
            binding.memberName.text = musician.name
            binding.memberStatus.text = when (status) {
                MusicianStatus.NoBand -> context.getString(R.string.member_status_no_band)
                is MusicianStatus.InOtherBand -> context.getString(R.string.member_status_in_band, status.bandName)
                MusicianStatus.AlreadyMember -> context.getString(R.string.member_status_member)
            }

            // El aviso de que dejará su banda solo acompaña al músico elegido
            val leavesBand = (status as? MusicianStatus.InOtherBand)?.takeIf { row.selected }
            binding.memberLeavesBand.isVisible = leavesBand != null
            leavesBand?.let { binding.memberLeavesBand.text = context.getString(R.string.member_leaves_band, it.bandName) }

            binding.memberRadio.isChecked = row.selected
            binding.root.isSelected = row.selected

            // Los integrantes actuales se ven atenuados y no responden; durante el envío nadie responde
            val selectable = row.candidate.selectable
            binding.root.isEnabled = selectable && row.enabled
            binding.memberRowContent.alpha = if (selectable) 1f else DISABLED_ALPHA
            binding.memberRadio.isEnabled = selectable

            // El radio no recibe el foco, así que la fila le dice a TalkBack si está elegido
            ViewCompat.setStateDescription(
                binding.root,
                context.getString(if (row.selected) R.string.cd_member_selected else R.string.cd_member_not_selected),
            )
        }
    }

    private object MemberRowDiff : DiffUtil.ItemCallback<MemberRow>() {
        override fun areItemsTheSame(oldItem: MemberRow, newItem: MemberRow) =
            oldItem.candidate.musician.id == newItem.candidate.musician.id

        override fun areContentsTheSame(oldItem: MemberRow, newItem: MemberRow) = oldItem == newItem
    }

    private companion object {
        const val DISABLED_ALPHA = 0.5f
    }
}
