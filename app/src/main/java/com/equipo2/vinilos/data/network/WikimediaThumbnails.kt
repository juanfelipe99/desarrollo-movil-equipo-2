package com.equipo2.vinilos.data.network

import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.size.Dimension
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Pide a Wikimedia la miniatura del ancho en que se va a mostrar cada imagen, en lugar de la que trae la API.
 * La API guarda fotos de artistas de 960 px y originales de Commons de varios MB, y la app las muestra en avatares de
 * 56 dp o en celdas de unos 170 dp. Así se descargan y se decodifican muchos menos píxeles. Si la miniatura no existe,
 * por ejemplo porque la imagen original es más chica, se carga la imagen de la API como antes.
 */
class WikimediaThumbnails : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val url = chain.request.data as? String ?: return chain.proceed()
        val width = (chain.size.width as? Dimension.Pixels)?.px ?: return chain.proceed()
        val thumbnail = wikimediaThumbnail(url, width) ?: return chain.proceed()
        val result = chain.withRequest(chain.request.newBuilder().data(thumbnail).build()).proceed()
        return if (result is ErrorResult) chain.proceed() else result
    }
}

private val WIKIMEDIA_HOSTS = setOf("upload.wikimedia.org", "thumb.wikimedia.org")
private val THUMBNAIL_WIDTHS = listOf(120, 250, 330, 500, 960)
private val THUMBNAIL_SIZE = Regex("/\\d+px-")
private val PHOTO = Regex("\\.jpe?g$", RegexOption.IGNORE_CASE)

/**
 * Miniatura de [url] del ancho permitido por Wikimedia que alcanza para [widthPx], o null si no hay que cambiarla.
 * Cambia las miniaturas de cualquier tamaño y las fotos originales de Commons. Las portadas originales de la Wikipedia
 * en inglés ya son pequeñas y su miniatura pesa más que el original, así que no se cambian, y tampoco los PNG o SVG.
 */
internal fun wikimediaThumbnail(url: String, widthPx: Int): String? {
    val parsed = url.toHttpUrlOrNull() ?: return null
    if (parsed.host !in WIKIMEDIA_HOSTS) return null
    val width = THUMBNAIL_WIDTHS.firstOrNull { it >= widthPx } ?: THUMBNAIL_WIDTHS.last()
    val path = parsed.encodedPath
    val thumbnailPath = when {
        "/thumb/" in path && THUMBNAIL_SIZE.containsMatchIn(path) -> path.replace(THUMBNAIL_SIZE, "/${width}px-")
        path.startsWith("/wikipedia/commons/") && PHOTO.containsMatchIn(path) -> {
            val file = path.substringAfterLast('/')
            path.replaceFirst("/wikipedia/commons/", "/wikipedia/commons/thumb/") + "/${width}px-$file"
        }
        else -> return null
    }
    val thumbnail = "https://upload.wikimedia.org$thumbnailPath"
    return thumbnail.takeIf { it != url }
}
