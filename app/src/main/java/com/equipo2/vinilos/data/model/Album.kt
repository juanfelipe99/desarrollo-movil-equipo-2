package com.equipo2.vinilos.data.model

import java.time.LocalDate

data class Track(
    val id: Int,
    val name: String,
    val duration: String,
)

data class AlbumComment(
    val id: Int,
    val description: String,
    val rating: Int,
    val author: String? = null,
)

data class Performer(
    val id: Int,
    val name: String,
    val image: String,
    val type: ArtistType = ArtistType.MUSICIAN,
)

data class Album(
    val id: Int,
    val name: String,
    val cover: String,
    val releaseDate: LocalDate,
    val description: String,
    val genre: String,
    val recordLabel: String,
    val tracks: List<Track> = emptyList(),
    val performers: List<Performer> = emptyList(),
    val comments: List<AlbumComment> = emptyList(),
)

data class NewTrack(
    val name: String,
    val duration: String,
)

data class NewAlbum(
    val name: String,
    val cover: String,
    val releaseDate: LocalDate,
    val description: String,
    val genre: String,
    val recordLabel: String,
)

// Únicos géneros y sellos que acepta la API, cualquier otro responde 400
object AlbumCatalog {
    val genres = listOf("Classical", "Salsa", "Rock", "Folk")
    val recordLabels = listOf("Sony Music", "EMI", "Discos Fuentes", "Elektra", "Fania Records")
}
