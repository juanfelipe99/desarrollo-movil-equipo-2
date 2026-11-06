package com.equipo2.vinilos.data.network.artist

import com.equipo2.vinilos.data.model.Artist
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.network.fromApiDate
import kotlinx.serialization.Serializable

@Serializable
data class MusicianDto(
    val id: Int,
    val name: String,
    val image: String = "",
    val birthDate: String,
)

@Serializable
data class BandDto(
    val id: Int,
    val name: String,
    val image: String = "",
    val creationDate: String,
    val musicians: List<BandMemberDto> = emptyList(),
)

/** Integrante dentro de una banda. Solo se lee el id, que basta para saber a qué banda pertenece. */
@Serializable
data class BandMemberDto(
    val id: Int,
)

internal fun MusicianDto.toArtist() = Artist(
    id = id,
    name = name,
    image = image,
    type = ArtistType.MUSICIAN,
    date = fromApiDate(birthDate),
)

internal fun BandDto.toArtist() = Artist(
    id = id,
    name = name,
    image = image,
    type = ArtistType.BAND,
    date = fromApiDate(creationDate),
)
