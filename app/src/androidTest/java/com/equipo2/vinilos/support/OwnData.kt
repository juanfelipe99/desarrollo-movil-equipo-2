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
    private var count = 0

    /** Nombre único de esta ejecución, por ejemplo «E2E HU04 Banda 34-123456 1». */
    fun name(label: String) = "E2E $label $RUN_TAG ${++count}"

    fun album(name: String, description: String = BackVynils.TEST_DESCRIPTION): Int =
        BackVynils.createAlbum(name, description).also { albums += it }

    fun musician(name: String, description: String = BackVynils.TEST_DESCRIPTION): Int =
        BackVynils.createMusician(name, description).also { musicians += it }

    fun band(name: String): Int = BackVynils.createBand(name).also { bands += it }

    /** Primero los álbumes y luego los artistas, que la API no borra si una banda aún tiene integrantes. */
    fun deleteAll() {
        albums.forEach(BackVynils::deleteAlbum)
        BackVynils.deleteArtists(musicians, bands)
        albums.clear()
        musicians.clear()
        bands.clear()
    }
}
