package com.equipo2.vinilos.support

import android.os.Build

/**
 * Marca de esta ejecución de las pruebas. Va en el nombre de todo lo que crean en BackVynils, así dos ejecuciones
 * contra la misma API, como las de API 21 y 34 en GitHub Actions, no se confunden ni se borran los datos entre sí.
 */
val RUN_TAG: String = "${Build.VERSION.SDK_INT}-${System.currentTimeMillis() % 1_000_000}"

/**
 * Lo que una prueba crea en BackVynils. Las pruebas contra la API solo revisan estos datos, nunca los que ya había,
 * porque otra persona o la otra ejecución de GitHub Actions puede estar cambiando la API al mismo tiempo.
 * Al terminar se borra solo lo que se creó aquí.
 */
class OwnData {
    private val albums = mutableListOf<Int>()
    private val musicians = mutableListOf<Int>()
    private val bands = mutableListOf<Int>()
    private val collectors = mutableListOf<Int>()

    /** Comentarios creados, con el álbum al que pertenecen, que la API pide para borrarlos. */
    private val comments = mutableListOf<Pair<Int, Int>>()
    private val prizes = mutableListOf<Int>()
    private var count = 0

    /** Nombre único de esta ejecución, por ejemplo «E2E HU04 Banda 34-123456 1». */
    fun name(label: String) = "E2E $label $RUN_TAG ${++count}"

    fun album(name: String, description: String = BackVynils.TEST_DESCRIPTION): Int =
        BackVynils.createAlbum(name, description).also { albums += it }

    fun musician(name: String, description: String = BackVynils.TEST_DESCRIPTION): Int =
        BackVynils.createMusician(name, description).also { musicians += it }

    fun band(name: String): Int = BackVynils.createBand(name).also { bands += it }

    fun collector(name: String): Int = BackVynils.createCollector(name).also { collectors += it }

    fun comment(albumId: Int, collectorId: Int, description: String, rating: Int): Int =
        BackVynils.addComment(albumId, collectorId, description, rating).also { comments += albumId to it }

    /** Las pruebas solo entregan premios creados aquí, así al borrarlos se van también sus entregas. */
    fun prize(name: String, organization: String = "Organización E2E"): Int =
        BackVynils.createPrize(name, organization).also { prizes += it }

    /** Un premio que creó la app durante la prueba (HU13), para borrarlo al terminar con lo demás. */
    fun keepPrize(id: Int) {
        if (id !in prizes) prizes += id
    }

    /**
     * Primero los comentarios y los coleccionistas, que la API no borra mientras tengan álbumes o favoritos.
     * Después los premios con sus entregas, porque la API no borra un artista que tenga premios.
     * Al final los álbumes y los artistas, que tampoco se borran si una banda aún tiene integrantes.
     */
    fun deleteAll() {
        comments.forEach { (albumId, commentId) -> BackVynils.deleteComment(albumId, commentId) }
        collectors.forEach(BackVynils::deleteCollector)
        prizes.forEach(BackVynils::deletePrize)
        albums.forEach(BackVynils::deleteAlbum)
        BackVynils.deleteArtists(musicians, bands)
        comments.clear()
        collectors.clear()
        prizes.clear()
        albums.clear()
        musicians.clear()
        bands.clear()
    }
}
