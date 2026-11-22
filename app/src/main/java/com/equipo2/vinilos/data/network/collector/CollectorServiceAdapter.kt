package com.equipo2.vinilos.data.network.collector

import com.equipo2.vinilos.data.model.Album
import com.equipo2.vinilos.data.model.Collector
import com.equipo2.vinilos.data.model.CollectorDetail
import com.equipo2.vinilos.data.network.ApiClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * [catalog] da el catálogo de álbumes. La app le pasa el del repositorio de álbumes, que ya lo tiene en memoria desde
 * que se abrió el catálogo, y de ahí sale el álbum de cada comentario sin pedir otra vez todos los álbumes.
 * Sin él, se piden a la API.
 */
class CollectorServiceAdapter(apiClient: ApiClient, private val catalog: (suspend () -> List<Album>)? = null) {

    private val api = apiClient.create(CollectorApi::class.java)

    // Sin el catálogo de la app, el álbum de cada comentario sale de la lista completa de álbumes, la misma para todos
    // los coleccionistas. Se guarda para no pedirla en cada detalle
    @Volatile private var albumByComment: Map<Int, CommentedAlbum>? = null

    suspend fun getCollectors(): List<Collector> = api.getCollectors().map { it.toCollector() }

    /**
     * Detalle de un coleccionista con los nombres de sus álbumes y el álbum de cada comentario.
     *
     * Las peticiones van a la vez y, si falla cualquiera, falla el detalle: mostrar «sin álbumes»
     * o «sin comentarios» por un error sería mostrar algo falso.
     */
    suspend fun getCollectorDetail(id: Int): CollectorDetail = coroutineScope {
        val collector = async { api.getCollector(id) }
        val albums = async { api.getCollectorAlbums(id) }
        val commented = async { commentedAlbums() }
        collector.await().toCollectorDetail(albums.await(), commented.await())
    }

    private suspend fun commentedAlbums(): Map<Int, CommentedAlbum> {
        catalog?.let { albums -> return albums().byComment() }
        return albumByComment ?: api.getCommentedAlbums().byComment().also { albumByComment = it }
    }

    /** Olvida los álbumes comentados guardados, para que el próximo detalle los pida a la API. */
    fun clearCache() {
        albumByComment = null
    }
}
