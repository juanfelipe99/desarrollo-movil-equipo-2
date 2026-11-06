package com.equipo2.vinilos.hu03

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.Artist
import com.equipo2.vinilos.data.model.ArtistDetail
import com.equipo2.vinilos.data.model.ArtistType
import com.equipo2.vinilos.data.repository.ArtistRepository
import com.equipo2.vinilos.support.enterAs
import kotlinx.coroutines.awaitCancellation
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import java.io.IOException
import java.text.Collator
import java.time.LocalDate
import java.util.Locale

/** Repositorio en memoria que da sus respuestas en orden y repite la última, o que nunca responde. */
class InMemoryArtistRepository(private val responses: List<Result<List<Artist>>>) : ArtistRepository {

    constructor(response: Result<List<Artist>> = Result.success(emptyList())) : this(listOf(response))

    var neverAnswer = false

    private var calls = 0

    override suspend fun getArtists(): Result<List<Artist>> {
        if (neverAnswer) awaitCancellation()
        return responses[minOf(calls++, responses.lastIndex)]
    }

    // Aquí solo se prueba que el detalle se abra; su contenido se prueba en HU04 con otro repositorio
    override suspend fun getArtistDetail(id: Int, type: ArtistType): Result<ArtistDetail> =
        Result.failure(UnsupportedOperationException("Estas pruebas no revisan el contenido del detalle"))
}

fun noConnection(): Result<List<Artist>> = Result.failure(IOException("Sin conexión"))

fun testMusician(id: Int, name: String, birthYear: Int) =
    Artist(id, name, image = "", type = ArtistType.MUSICIAN, date = LocalDate.of(birthYear, 1, 1))

fun testBand(id: Int, name: String, creationYear: Int) =
    Artist(id, name, image = "", type = ArtistType.BAND, date = LocalDate.of(creationYear, 1, 1))

/** Entra como visitante y abre la pestaña Artistas de la barra inferior. */
fun openArtists() {
    enterAs(collector = false)
    onView(withId(R.id.artistsGraph)).perform(click())
}

/** Nombre de una fila del listado, sin confundirlo con otros textos de la pantalla. */
fun artistRowName(name: String): Matcher<View> = allOf(withId(R.id.artistName), withText(name), isDescendantOfA(withId(R.id.artistList)))

/** Orden en que la app muestra los artistas: alfabético sin importar tildes ni mayúsculas. */
fun sortedLikeTheApp(names: List<String>): List<String> {
    val collator = Collator.getInstance(Locale.forLanguageTag("es")).apply { strength = Collator.PRIMARY }
    return names.sortedWith(collator)
}
