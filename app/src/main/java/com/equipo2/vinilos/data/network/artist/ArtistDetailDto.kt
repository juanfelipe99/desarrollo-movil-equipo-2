package com.equipo2.vinilos.data.network.artist

import com.equipo2.vinilos.data.model.Artist
import com.equipo2.vinilos.data.model.ArtistDetail
import com.equipo2.vinilos.data.model.ArtistPrize
import com.equipo2.vinilos.data.model.byName
import com.equipo2.vinilos.data.network.album.AlbumDto
import com.equipo2.vinilos.data.network.album.toAlbum
import com.equipo2.vinilos.data.network.fromApiDate
import kotlinx.serialization.Serializable

/** Respuesta de musicians/{id}. La API no dice aquí a qué banda pertenece el músico. */
@Serializable
data class MusicianDetailDto(
    val id: Int,
    val name: String,
    val image: String = "",
    val description: String = "",
    val birthDate: String,
    val albums: List<AlbumDto> = emptyList(),
    val performerPrizes: List<PerformerPrizeDto> = emptyList(),
)

/** Respuesta de bands/{id}, con sus integrantes. */
@Serializable
data class BandDetailDto(
    val id: Int,
    val name: String,
    val image: String = "",
    val description: String = "",
    val creationDate: String,
    val albums: List<AlbumDto> = emptyList(),
    val musicians: List<MusicianDto> = emptyList(),
    val performerPrizes: List<PerformerPrizeDto> = emptyList(),
)

/** Premio entregado a un artista. Solo trae la fecha, el nombre y la organización están en el premio. */
@Serializable
data class PerformerPrizeDto(
    val id: Int,
    val premiationDate: String,
)

/** Respuesta de prizes, con la lista de entregas de cada premio. */
@Serializable
data class PrizeDto(
    val id: Int,
    val name: String,
    val organization: String,
    val performerPrizes: List<PerformerPrizeDto> = emptyList(),
)

/** Premio de cada entrega, por el id de la entrega. Con una sola llamada a prizes se nombran todas. */
internal fun List<PrizeDto>.byPerformerPrize(): Map<Int, PrizeDto> =
    flatMap { prize -> prize.performerPrizes.map { it.id to prize } }.toMap()

internal fun MusicianDetailDto.toArtistDetail(band: Artist?, prizes: Map<Int, PrizeDto>) = ArtistDetail(
    artist = MusicianDto(id, name, image, birthDate).toArtist(),
    description = description,
    band = band,
    albums = albums.toAlbums(),
    prizes = performerPrizes.toArtistPrizes(prizes),
)

internal fun BandDetailDto.toArtistDetail(prizes: Map<Int, PrizeDto>) = ArtistDetail(
    artist = BandDto(id, name, image, creationDate).toArtist(),
    description = description,
    members = musicians.map { it.toArtist() }.sortedWith(byName { it.name }),
    albums = albums.toAlbums(),
    prizes = performerPrizes.toArtistPrizes(prizes),
)

private fun List<AlbumDto>.toAlbums() = map { it.toAlbum() }.sortedBy { it.releaseDate }

// Una entrega cuyo premio no aparece en prizes no se puede nombrar, así que no se muestra
private fun List<PerformerPrizeDto>.toArtistPrizes(prizes: Map<Int, PrizeDto>): List<ArtistPrize> =
    mapNotNull { delivery ->
        val prize = prizes[delivery.id] ?: return@mapNotNull null
        ArtistPrize(delivery.id, prize.name, prize.organization, fromApiDate(delivery.premiationDate))
    }.sortedBy { it.date }
