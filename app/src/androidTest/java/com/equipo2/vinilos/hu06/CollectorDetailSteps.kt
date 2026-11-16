package com.equipo2.vinilos.hu06

import android.view.View
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.AlbumStatus
import com.equipo2.vinilos.data.model.Artist
import com.equipo2.vinilos.data.model.Collector
import com.equipo2.vinilos.data.model.CollectorAlbum
import com.equipo2.vinilos.data.model.CollectorComment
import com.equipo2.vinilos.data.model.CollectorDetail
import com.equipo2.vinilos.data.repository.CollectorRepository
import com.equipo2.vinilos.hu05.collectorRowName
import com.equipo2.vinilos.support.isSettled
import com.equipo2.vinilos.support.scrollToWithBarsHidden
import com.equipo2.vinilos.support.waitUntil
import com.equipo2.vinilos.ui.collectors.list.CollectorAdapter
import kotlinx.coroutines.awaitCancellation
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import java.io.IOException

/**
 * Repositorio en memoria con un listado fijo y el detalle de cada coleccionista.
 * Puede fallar las primeras consultas del detalle o no responderlas nunca.
 */
class InMemoryCollectorDetailRepository(private val details: List<CollectorDetail>) : CollectorRepository {

    /** Cuántas consultas del detalle fallan sin conexión antes de responder. */
    var failuresBeforeAnswer = 0

    var neverAnswer = false

    /** Coleccionistas pedidos al detalle, en orden. */
    val detailRequests = mutableListOf<Int>()

    override suspend fun getCollectors(): Result<List<Collector>> = Result.success(details.map { it.collector })

    override suspend fun getCollectorDetail(id: Int): Result<CollectorDetail> {
        detailRequests += id
        if (neverAnswer) awaitCancellation()
        if (detailRequests.size <= failuresBeforeAnswer) return Result.failure(IOException("Sin conexión"))
        return details.firstOrNull { it.collector.id == id }
            ?.let { Result.success(it) }
            ?: Result.failure(IllegalArgumentException("No hay detalle para el coleccionista $id"))
    }
}

fun testCollectorDetail(
    id: Int,
    name: String,
    telephone: String = "3012357936",
    email: String = "coleccionista@ejemplo.com",
    favorites: List<Artist> = emptyList(),
    albums: List<CollectorAlbum> = emptyList(),
    comments: List<CollectorComment> = emptyList(),
) = CollectorDetail(
    collector = Collector(id = id, name = name, email = email, albumCount = albums.size),
    telephone = telephone,
    favorites = favorites,
    albums = albums,
    comments = comments,
)

fun collectorAlbum(id: Int, name: String, price: Int, active: Boolean = true) =
    CollectorAlbum(albumId = id, name = name, cover = "", price = price, status = if (active) AlbumStatus.ACTIVE else AlbumStatus.INACTIVE)

/** Desplaza el listado de coleccionistas hasta el coleccionista y lo toca. */
fun openCollector(name: String) {
    waitUntil {
        runCatching {
            onView(withId(R.id.collectorList)).perform(scrollToCollector(name))
            onView(collectorRowName(name)).check(matches(isDisplayed()))
        }.isSuccess
    }
    onView(collectorRowName(name)).perform(click())
}

private fun scrollToCollector(name: String) = object : ViewAction {
    override fun getConstraints(): Matcher<View> = isAssignableFrom(RecyclerView::class.java)

    override fun getDescription() = "desplazar el listado hasta «$name»"

    override fun perform(uiController: UiController, view: View) {
        val list = view as RecyclerView
        while (!list.isSettled()) uiController.loopMainThreadForAtLeast(50)
        val position = (list.adapter as CollectorAdapter).currentList.indexOfFirst { it.name == name }
        check(position >= 0) { "El listado no tiene el coleccionista «$name»" }
        (list.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
        uiController.loopMainThreadUntilIdle()
    }
}

/**
 * El detalle que se ve es el de este coleccionista. Al volver con Atrás el detalle conserva donde se había bajado,
 * así que primero se sube hasta el nombre.
 */
fun checkShowingCollector(name: String) {
    onView(allOf(withId(R.id.collectorName), withText(name))).perform(scrollTo()).check(matches(isDisplayed()))
}

fun favoriteName(name: String): Matcher<View> =
    allOf(withId(R.id.favoriteName), withText(name), isDescendantOfA(withId(R.id.favoritesList)))

fun collectorAlbumName(name: String): Matcher<View> =
    allOf(withId(R.id.collectorAlbumName), withText(name), isDescendantOfA(withId(R.id.collectorAlbumsList)))

/**
 * Baja hasta la fila completa del álbum en la colección y revisa que muestre su precio y su estado.
 * Se baja hasta la fila y no solo hasta el nombre: en pantallas pequeñas el precio, que va debajo del nombre,
 * queda fuera del borde. El precio y el estado se buscan dentro de esa fila, así no se confunden con los de otro álbum.
 */
fun checkCollectorAlbumRow(name: String, price: String, @StringRes status: Int) {
    onView(allOf(withParent(withId(R.id.collectorAlbumsList)), hasDescendant(collectorAlbumName(name))))
        .perform(scrollToWithBarsHidden())
        .check(matches(allOf(isDisplayed(), hasDescendant(withText(price)), hasDescendant(withText(status)))))
}

fun commentAlbumLink(name: String): Matcher<View> =
    allOf(withId(R.id.commentAlbum), withText(name), isDescendantOfA(withId(R.id.commentsList)))

/** Baja hasta el álbum del comentario, con las barras escondidas para pantallas pequeñas, y lo toca. */
fun openCommentAlbum(name: String) {
    onView(commentAlbumLink(name)).perform(scrollToWithBarsHidden(), click())
}
