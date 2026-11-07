package com.equipo2.vinilos.data.network.artist

import com.equipo2.vinilos.data.model.MusicianCandidate
import com.equipo2.vinilos.data.network.ApiClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class BandServiceAdapter(apiClient: ApiClient) {

    private val api = apiClient.create(ArtistApi::class.java)

    // Músicos y bandas se piden a la vez, y si falla cualquiera de los dos falla el listado
    suspend fun getMusicianCandidates(bandId: Int): List<MusicianCandidate> = coroutineScope {
        val musicians = async { api.getMusicians() }
        val bands = async { api.getBands() }
        musicianCandidates(bandId, musicians.await(), bands.await())
    }

    suspend fun addMusician(bandId: Int, musicianId: Int) = api.addMusicianToBand(bandId, musicianId)
}
