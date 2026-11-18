package com.equipo2.vinilos.ui.artists.albums

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.AlbumCandidate
import com.equipo2.vinilos.databinding.ItemAlbumCandidateBinding

/** Fila del listado: el álbum, si es el elegido y si se puede tocar mientras no haya un envío en curso. */
data class AlbumRow(
    val candidate: AlbumCandidate,
    val selected: Boolean,
    val enabled: Boolean,
)

/** Lista de selección única. Al tocar un álbum que se puede agregar avisa con él. */
class AlbumCandidateAdapter(
    private val onCandidateClick: (AlbumCandidate) -> Unit,
) : ListAdapter<AlbumRow, AlbumCandidateAdapter.AlbumCandidateViewHolder>(AlbumRowDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumCandidateViewHolder {
        val binding = ItemAlbumCandidateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = AlbumCandidateViewHolder(binding)
        binding.root.setOnClickListener {
            // Al animar un cambio de la lista la posición puede no existir todavía
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) onCandidateClick(getItem(position).candidate)
        }
        return holder
    }

    override fun onBindViewHolder(holder: AlbumCandidateViewHolder, position: Int) = holder.bind(getItem(position))

    class AlbumCandidateViewHolder(private val binding: ItemAlbumCandidateBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: AlbumRow) {
            val context = binding.root.context
            val album = row.candidate.album

            // Los mismos datos y textos que el ítem del catálogo (HU01): portada, nombre, año, género e intérpretes
            binding.albumCandidateCover.load(album.cover.ifBlank { null })
            binding.albumCandidateName.text = album.name
            val performers = album.performers.joinToString(", ") { it.name }
                .ifEmpty { context.getString(R.string.album_list_no_performers) }
            binding.albumCandidateDetails.text = context.getString(
                R.string.album_candidate_details,
                context.getString(R.string.album_list_subtitle, album.releaseDate.year, album.genre),
                performers,
            )

            binding.albumCandidateRadio.isChecked = row.selected
            binding.root.isSelected = row.selected

            // Los que el artista ya tiene se ven atenuados, con un check y su aviso, y no responden.
            // Durante el envío no responde ninguna fila
            val selectable = row.candidate.selectable
            binding.root.isEnabled = selectable && row.enabled
            binding.albumRowContent.alpha = if (selectable) 1f else DISABLED_ALPHA
            binding.albumCandidateRadio.isVisible = selectable
            binding.albumCandidateCheck.isVisible = !selectable
            binding.albumCandidateAlreadyAdded.isVisible = !selectable

            // El radio no recibe el foco, así que la fila le dice a TalkBack si está elegido
            ViewCompat.setStateDescription(
                binding.root,
                when {
                    !selectable -> context.getString(R.string.album_candidate_already_added)
                    row.selected -> context.getString(R.string.cd_album_selected)
                    else -> context.getString(R.string.cd_album_not_selected)
                },
            )
        }
    }

    private object AlbumRowDiff : DiffUtil.ItemCallback<AlbumRow>() {
        override fun areItemsTheSame(oldItem: AlbumRow, newItem: AlbumRow) =
            oldItem.candidate.album.id == newItem.candidate.album.id

        override fun areContentsTheSame(oldItem: AlbumRow, newItem: AlbumRow) = oldItem == newItem
    }

    private companion object {
        const val DISABLED_ALPHA = 0.5f
    }
}
