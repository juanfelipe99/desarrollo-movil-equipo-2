package com.equipo2.vinilos.ui.artists.prizes

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.PrizeCandidate
import com.equipo2.vinilos.databinding.ItemPrizeCandidateBinding
import com.equipo2.vinilos.ui.common.toLongText
import java.time.LocalDate

/**
 * Fila del listado: el premio, si es el elegido, si se puede tocar mientras no haya un envío en curso, si es la última
 * y si se acaba de crear desde esta pantalla (HU13).
 */
data class PrizeRow(
    val candidate: PrizeCandidate,
    val selected: Boolean,
    val enabled: Boolean,
    val isLast: Boolean,
    val isNew: Boolean = false,
)

/** Lista de selección única de premios. Al tocar uno avisa con él. Ninguno se deshabilita por haberlo recibido antes. */
class PrizeCandidateAdapter(
    private val onCandidateClick: (PrizeCandidate) -> Unit,
) : ListAdapter<PrizeRow, PrizeCandidateAdapter.PrizeViewHolder>(PrizeRowDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrizeViewHolder {
        val binding = ItemPrizeCandidateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = PrizeViewHolder(binding)
        binding.root.setOnClickListener {
            // Al animar un cambio de la lista la posición puede no existir todavía
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onCandidateClick(getItem(position).candidate)
        }
        return holder
    }

    override fun onBindViewHolder(holder: PrizeViewHolder, position: Int) = holder.bind(getItem(position))

    class PrizeViewHolder(private val binding: ItemPrizeCandidateBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: PrizeRow) {
            val context = binding.root.context
            val prize = row.candidate.prize

            binding.prizeName.text = prize.name
            binding.prizeNewBadge.isVisible = row.isNew
            binding.prizeOrganization.text = prize.organization

            val received = row.candidate.receivedDates
            binding.prizeReceived.isVisible = received.isNotEmpty()
            if (received.isNotEmpty()) {
                binding.prizeReceived.text = context.getString(R.string.prize_already_received, received.toSentence(context))
            }

            binding.prizeRadio.isChecked = row.selected
            binding.root.isSelected = row.selected
            binding.root.isEnabled = row.enabled
            // La tarjeta ya cierra la lista con su borde
            binding.prizeDivider.isVisible = !row.isLast

            // El radio no recibe el foco, así que la fila le dice a TalkBack si está elegido
            ViewCompat.setStateDescription(
                binding.root,
                context.getString(if (row.selected) R.string.cd_prize_selected else R.string.cd_prize_not_selected),
            )
        }
    }

    private object PrizeRowDiff : DiffUtil.ItemCallback<PrizeRow>() {
        override fun areItemsTheSame(oldItem: PrizeRow, newItem: PrizeRow) = oldItem.candidate.prize.id == newItem.candidate.prize.id

        override fun areContentsTheSame(oldItem: PrizeRow, newItem: PrizeRow) = oldItem == newItem
    }
}

/** Fechas como «el 10 de diciembre de 1978», «el … y el …» o «el …, el … y el …». */
private fun List<LocalDate>.toSentence(context: Context): String {
    val dates = map { context.getString(R.string.prize_received_date, it.toLongText()) }
    if (dates.size == 1) return dates.first()
    return context.getString(R.string.prize_received_dates_join, dates.dropLast(1).joinToString(", "), dates.last())
}
