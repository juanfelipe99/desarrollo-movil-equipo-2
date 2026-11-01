package com.equipo2.vinilos.data.network.album

import com.equipo2.vinilos.data.model.Album
import com.equipo2.vinilos.data.model.AlbumComment
import com.equipo2.vinilos.data.model.NewAlbum
import com.equipo2.vinilos.data.model.NewTrack
import com.equipo2.vinilos.data.model.Performer
import com.equipo2.vinilos.data.model.Track
import com.equipo2.vinilos.data.network.fromApiDate
import com.equipo2.vinilos.data.network.toApiDate
import kotlinx.serialization.Serializable

@Serializable
data class AlbumDto(
    val id: Int,
    val name: String,
    val cover: String,
    val releaseDate: String,
    val description: String,
    val genre: String,
    val recordLabel: String,
    val tracks: List<TrackDto> = emptyList(),
    val performers: List<PerformerDto> = emptyList(),
    val comments: List<CommentDto> = emptyList(),
)

@Serializable
data class CreateAlbumRequest(
    val name: String,
    val cover: String,
    val releaseDate: String,
    val description: String,
    val genre: String,
    val recordLabel: String,
)
@Serializable
data class AddTrackRequest(
    val name: String,
    val duration: String,
)

@Serializable
data class TrackDto(
    val id: Int,
    val name: String,
    val duration: String,
)

@Serializable
data class PerformerDto(
    val id: Int,
    val name: String,
    val image: String,
)

@Serializable
data class CommentDto(
    val id: Int,
    val description: String,
    val rating: Int,
)

@Serializable
data class CommentAuthorDto(
    val name: String,
    val comments: List<CommentIdDto> = emptyList(),
)

@Serializable
data class CommentIdDto(val id: Int)


internal fun NewAlbum.toRequest() = CreateAlbumRequest(
    name = name,
    cover = cover,
    releaseDate = toApiDate(releaseDate),
    description = description,
    genre = genre,
    recordLabel = recordLabel,
)


internal fun NewTrack.toRequest() = AddTrackRequest(name = name, duration = duration)

internal fun TrackDto.toTrack() = Track(id = id, name = name, duration = duration)

internal fun AlbumDto.toAlbum(commentAuthors: Map<Int, String> = emptyMap()) = Album(
    id = id,
    name = name,
    cover = cover,
    releaseDate = fromApiDate(releaseDate),
    description = description,
    genre = genre,
    recordLabel = recordLabel,
    tracks = tracks.map { it.toTrack() },
    performers = performers.map { performer ->
        Performer(
            id = performer.id,
            name = performer.name,
            image = performer.image,
        )
    },
    comments = comments.map { comment ->
        AlbumComment(
            id = comment.id,
            description = comment.description,
            rating = comment.rating,
            author = commentAuthors[comment.id],
        )
    },
)

