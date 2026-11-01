package com.equipo2.vinilos.support

import com.equipo2.vinilos.data.model.Album
import com.equipo2.vinilos.data.model.NewAlbum
import com.equipo2.vinilos.data.model.NewTrack
import com.equipo2.vinilos.data.model.Role
import com.equipo2.vinilos.data.model.ThemeMode
import com.equipo2.vinilos.data.model.Track
import com.equipo2.vinilos.data.repository.AlbumRepository
import com.equipo2.vinilos.data.repository.RoleRepository
import com.equipo2.vinilos.data.repository.ThemeModeRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow

/** Guarda lo que recibe y responde lo que la prueba le indique. */
class FakeAlbumRepository : AlbumRepository {

    val created = mutableListOf<NewAlbum>()

    /** Respuesta de la próxima creación, que la prueba completa cuando quiera. */
    var response = CompletableDeferred<Result<Album>>()

    override suspend fun createAlbum(album: NewAlbum): Result<Album> {
        created += album
        return response.await()
    }

    /** Cuántas veces se pidió el catálogo. */
    var albumRequests = 0
        private set

    /** Respuesta del catálogo, que puede esperar un CompletableDeferred para probar la carga. */
    var albums: suspend () -> Result<List<Album>> = { Result.success(emptyList()) }

    override suspend fun getAlbums(): Result<List<Album>> {
        albumRequests++
        return albums()
    }

    /** Ids pedidos al detalle, en orden. */
    val detailRequests = mutableListOf<Int>()

    /** Respuesta del detalle, que puede esperar un CompletableDeferred para probar la carga. */
    var detail: suspend () -> Result<Album> = { Result.failure(UnsupportedOperationException()) }

    override suspend fun getAlbumById(id: Int): Result<Album> {
        detailRequests += id
        return detail()
    }

    /** Tracks enviados, con el id de su álbum, en orden. */
    val addedTracks = mutableListOf<Pair<Int, NewTrack>>()

    /** Respuesta del próximo track, que la prueba completa cuando quiera. */
    var trackResponse = CompletableDeferred<Result<Track>>()

    override suspend fun addTrack(albumId: Int, track: NewTrack): Result<Track> {
        addedTracks += albumId to track
        return trackResponse.await()
    }
}

class FakeRoleRepository(initial: Role) : RoleRepository {

    override val role = MutableStateFlow(initial)

    override suspend fun setRole(role: Role) {
        this.role.value = role
    }
}

class FakeThemeModeRepository(initial: ThemeMode) : ThemeModeRepository {

    override val themeMode = MutableStateFlow(initial)

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }
}
