package com.equipo2.vinilos.support

import com.equipo2.vinilos.data.model.Album
import com.equipo2.vinilos.data.model.AlbumCandidate
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.model.Performer
import com.equipo2.vinilos.data.repository.ArtistAlbumRepository
import kotlinx.coroutines.CompletableDeferred
import java.time.LocalDate

/** Guarda lo que recibe y responde lo que la prueba le indique. */
class FakeArtistAlbumRepository : ArtistAlbumRepository {

    /** Artistas cuyo listado de álbumes se pidió, con su tipo, en orden. */
    val candidateRequests = mutableListOf<Pair<Int, ArtistType>>()

    /** Respuesta del listado, que puede esperar un CompletableDeferred para probar la carga. */
    var candidates: suspend () -> Result<List<AlbumCandidate>> = { Result.success(emptyList()) }

    override suspend fun getAlbumCandidates(artistId: Int, type: ArtistType): Result<List<AlbumCandidate>> {
        candidateRequests += artistId to type
        return candidates()
    }

    /** Un envío: a quién y qué álbum. */
    data class Added(val artistId: Int, val type: ArtistType, val albumId: Int)

    /** Envíos, en orden. */
    val added = mutableListOf<Added>()

    /** Respuesta del próximo envío, que la prueba completa cuando quiera. */
    var addResponse = CompletableDeferred<Result<Unit>>()

    override suspend fun addAlbum(artistId: Int, type: ArtistType, albumId: Int): Result<Unit> {
        added += Added(artistId, type, albumId)
        return addResponse.await()
    }
}

fun albumCandidate(id: Int, name: String, alreadyAdded: Boolean = false, vararg performers: Performer) = AlbumCandidate(
    Album(
        id = id,
        name = name,
        cover = "https://ejemplo.com/$id.jpg",
        releaseDate = LocalDate.of(1978, 1, 1),
        description = "Descripción",
        genre = "Salsa",
        recordLabel = "Fania Records",
        performers = performers.toList(),
    ),
    alreadyAdded,
)
