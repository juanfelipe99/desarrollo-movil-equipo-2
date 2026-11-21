package com.equipo2.vinilos.accesibilidad

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.ArtistDetail
import com.equipo2.vinilos.data.model.MusicianCandidate
import com.equipo2.vinilos.data.model.MusicianStatus
import com.equipo2.vinilos.data.model.Prize
import com.equipo2.vinilos.data.model.PrizeCandidate
import com.equipo2.vinilos.data.repository.DataError
import com.equipo2.vinilos.hu01.CatalogAlbumRepository
import com.equipo2.vinilos.hu01.openAlbum
import com.equipo2.vinilos.hu01.testAlbum
import com.equipo2.vinilos.hu02.FailingAlbumDetailRepository
import com.equipo2.vinilos.hu03.InMemoryArtistRepository
import com.equipo2.vinilos.hu03.openArtists
import com.equipo2.vinilos.hu03.testBand
import com.equipo2.vinilos.hu03.testMusician
import com.equipo2.vinilos.hu04.InMemoryArtistDetailRepository
import com.equipo2.vinilos.hu04.openArtist
import com.equipo2.vinilos.hu05.InMemoryCollectorRepository
import com.equipo2.vinilos.hu05.openCollectors
import com.equipo2.vinilos.hu06.InMemoryCollectorDetailRepository
import com.equipo2.vinilos.hu06.openCollector
import com.equipo2.vinilos.hu06.testCollectorDetail
import com.equipo2.vinilos.hu07.tapSave
import com.equipo2.vinilos.hu12.InMemoryBandRepository
import com.equipo2.vinilos.hu12.openBand
import com.equipo2.vinilos.hu12.openMemberAdd
import com.equipo2.vinilos.hu14.InMemoryPrizeRepository
import com.equipo2.vinilos.hu14.openArtistDetail
import com.equipo2.vinilos.hu14.openPrizeAdd
import com.equipo2.vinilos.hu14.tapSavePrize
import com.equipo2.vinilos.hu15.InMemoryArtistAlbumRepository
import com.equipo2.vinilos.hu15.openAlbumAdd
import com.equipo2.vinilos.hu15.openArtistForAlbums
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.enterAs
import com.equipo2.vinilos.support.openTab
import com.equipo2.vinilos.support.scrollToWithBarsHidden
import com.equipo2.vinilos.support.useAlbumRepository
import com.equipo2.vinilos.support.useArtistAlbumRepositories
import com.equipo2.vinilos.support.useArtistRepository
import com.equipo2.vinilos.support.useBandRepositories
import com.equipo2.vinilos.support.useCollectorRepository
import com.equipo2.vinilos.support.usePrizeRepositories
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Auditoría de accesibilidad de lo que AccessibilityAuditTest no recorre: los estados vacío y de error con repositorios
 * en memoria, los formularios con sus mensajes de error, y el rol visitante con las secciones de abajo de cada detalle.
 * Cada pantalla se revisa en tema claro y oscuro y se agrega al mismo informe.
 *
 * Solo corre con el argumento auditoria=si, como AccessibilityAuditTest. No guarda nada en la API.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityStatesAuditTest {

    @get:Rule
    val app = VinilosRule()

    private val auditor = Auditor()

    private val ruben = testMusician(100, "Rubén Blades Bellido de Luna", 1948)
    private val freddie = testMusician(9, "Freddie Mercury", 1946)
    private val queen = testBand(101, "Queen", 1970)
    private val rubenDetail = ArtistDetail(artist = ruben, description = "Cantante, compositor y actor panameño.")
    private val queenDetail = ArtistDetail(artist = queen, description = "Banda británica de rock.", members = listOf(freddie))
    private val grammy = PrizeCandidate(Prize(100, "Grammy Award", "National Academy"))

    @Before
    fun onlyWhenAsked() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("auditoria") == "si")
    }

    // Listados vacíos y con error

    @Test
    fun catalogo_vacio() {
        useAlbumRepository(CatalogAlbumRepository(Result.success(emptyList())))
        app.launch()
        enterAs(collector = true)
        auditor.auditBothThemes("HU01 · Catálogo vacío")
    }

    @Test
    fun catalogo_error() {
        useAlbumRepository(CatalogAlbumRepository(Result.failure(DataError.Network())))
        app.launch()
        enterAs(collector = false)
        auditor.auditBothThemes("HU01 · Catálogo con error de conexión")
    }

    @Test
    fun artistas_vacio() {
        useArtistRepository(InMemoryArtistRepository(Result.success(emptyList())))
        app.launch()
        openArtists()
        auditor.auditBothThemes("HU03 · Artistas vacío")
    }

    @Test
    fun artistas_error() {
        useArtistRepository(InMemoryArtistRepository(Result.failure(DataError.Server(500))))
        app.launch()
        openArtists()
        auditor.auditBothThemes("HU03 · Artistas con error del servidor")
    }

    @Test
    fun coleccionistas_vacio() {
        useCollectorRepository(InMemoryCollectorRepository(Result.success(emptyList())))
        app.launch()
        openCollectors()
        auditor.auditBothThemes("HU05 · Coleccionistas vacío")
    }

    @Test
    fun coleccionistas_error() {
        useCollectorRepository(InMemoryCollectorRepository(Result.failure(DataError.Network())))
        app.launch()
        openCollectors()
        auditor.auditBothThemes("HU05 · Coleccionistas con error de conexión")
    }

    // Detalles con error y sin datos

    @Test
    fun detalleDeAlbum_error() {
        val album = testAlbum(100, "Buscando América", 1984, "Salsa")
        useAlbumRepository(FailingAlbumDetailRepository(CatalogAlbumRepository(Result.success(listOf(album)))))
        app.launch()
        enterAs(collector = false)
        openAlbum(album.name)
        auditor.auditBothThemes("HU02 · Detalle de álbum con error")
    }

    @Test
    fun detalleDeArtista_error() {
        useArtistRepository(InMemoryArtistDetailRepository(listOf(rubenDetail)).apply { failuresBeforeAnswer = 99 })
        app.launch()
        openArtists()
        openArtist(ruben.name)
        auditor.auditBothThemes("HU04 · Detalle de artista con error")
    }

    @Test
    fun detalleDeArtista_sinBandaAlbumesNiPremios_comoColeccionista() {
        useArtistRepository(InMemoryArtistDetailRepository(listOf(rubenDetail)))
        app.launch()
        enterAs(collector = true)
        openTab(R.id.artistsGraph)
        openArtist(ruben.name)
        auditor.auditBothThemes("HU04 · Músico sin banda, álbumes ni premios")
        auditor.auditBothThemes("HU04 · Músico sin banda, álbumes ni premios · abajo") {
            onView(withId(R.id.prizesTitleGroup)).perform(scrollToWithBarsHidden())
        }
    }

    @Test
    fun detalleDeColeccionista_sinDatos() {
        val lucia = testCollectorDetail(id = 102, name = "Lucía Jaramillo", telephone = "", email = "lucia@ejemplo.com")
        useCollectorRepository(InMemoryCollectorDetailRepository(listOf(lucia)))
        app.launch()
        openCollectors()
        openCollector(lucia.collector.name)
        auditor.auditBothThemes("HU06 · Coleccionista sin favoritos, álbumes ni comentarios")
    }

    @Test
    fun detalleDeColeccionista_error() {
        val lucia = testCollectorDetail(id = 102, name = "Lucía Jaramillo")
        useCollectorRepository(InMemoryCollectorDetailRepository(listOf(lucia)).apply { failuresBeforeAnswer = 99 })
        app.launch()
        openCollectors()
        openCollector(lucia.collector.name)
        auditor.auditBothThemes("HU06 · Detalle de coleccionista con error")
    }

    // Pantallas para agregar, vacías y con error

    @Test
    fun agregarPremio_vacio_yGuardarSinElegir() {
        usePrizeRepositories(InMemoryArtistDetailRepository(listOf(rubenDetail)), InMemoryPrizeRepository(emptyList()))
        app.launch()
        openArtistDetail(ruben.name)
        openPrizeAdd()
        tapSavePrize()
        auditor.auditBothThemes("HU14 · Agregar premio sin premios, con los errores de Guardar")
    }

    @Test
    fun agregarPremio_error() {
        val prizes = InMemoryPrizeRepository(listOf(grammy))
        prizes.candidateResponses = listOf(Result.failure(DataError.Network()))
        usePrizeRepositories(InMemoryArtistDetailRepository(listOf(rubenDetail)), prizes)
        app.launch()
        openArtistDetail(ruben.name)
        openPrizeAdd()
        auditor.auditBothThemes("HU14 · Agregar premio con error de conexión")
    }

    @Test
    fun crearPremio_conLosErroresDeCampoVacio() {
        usePrizeRepositories(InMemoryArtistDetailRepository(listOf(rubenDetail)), InMemoryPrizeRepository(listOf(grammy)))
        app.launch()
        openArtistDetail(ruben.name)
        openPrizeAdd()
        onView(withId(R.id.prizeCreateButton)).perform(scrollTo(), click())
        onView(withId(R.id.saveButton)).perform(click())
        auditor.auditBothThemes("HU13 · Crear premio con los errores de campo vacío")
    }

    @Test
    fun agregarIntegrante_sinMusicosDisponibles() {
        useBandRepositories(
            InMemoryArtistDetailRepository(listOf(queenDetail)),
            InMemoryBandRepository(listOf(MusicianCandidate(freddie, MusicianStatus.AlreadyMember))),
        )
        app.launch()
        openBand(queen.name)
        openMemberAdd()
        auditor.auditBothThemes("HU12 · Agregar integrante sin músicos disponibles")
    }

    @Test
    fun agregarAlbum_sinAlbumes() {
        useArtistAlbumRepositories(InMemoryArtistDetailRepository(listOf(rubenDetail)), InMemoryArtistAlbumRepository(emptyList()))
        app.launch()
        openArtistForAlbums(ruben.name)
        openAlbumAdd()
        auditor.auditBothThemes("HU15 · Agregar álbum sin álbumes disponibles")
    }

    @Test
    fun agregarAlbum_error() {
        val albums = InMemoryArtistAlbumRepository(emptyList())
        albums.candidateResponses = listOf(Result.failure(DataError.Network()))
        useArtistAlbumRepositories(InMemoryArtistDetailRepository(listOf(rubenDetail)), albums)
        app.launch()
        openArtistForAlbums(ruben.name)
        openAlbumAdd()
        auditor.auditBothThemes("HU15 · Agregar álbum con error de conexión")
    }

    // Formularios con la API real, sin guardar nada: Guardar con los campos vacíos solo muestra los errores

    @Test
    fun crearAlbum_conLosErroresDeCampoVacio() {
        app.launch()
        enterAs(collector = true)
        onView(withId(R.id.createAlbumButton)).perform(click())
        tapSave()
        auditor.auditBothThemes("HU07 · Crear álbum con los errores de campo vacío")
    }

    @Test
    fun agregarTrack_conLosErroresDeCampoVacio() {
        app.launch()
        enterAs(collector = true)
        openAlbum(ALBUM)
        onView(withId(R.id.addTrackButton)).perform(scrollToWithBarsHidden(), click())
        onView(withId(R.id.trackAddButton)).perform(click())
        auditor.auditBothThemes("HU08 · Agregar track con los errores de campo vacío")
    }

    // Rol visitante con la API real, con las secciones de abajo de cada detalle

    @Test
    fun visitante_recorrido() {
        app.launch()
        auditor.useTheme(night = false)
        enterAs(collector = false)
        auditor.auditBothThemes("Visitante · HU01 · Catálogo")
        openAlbum(ALBUM)
        auditor.auditBothThemes("Visitante · HU02 · Detalle de álbum")
        auditor.auditBothThemes("Visitante · HU02 · Detalle de álbum · tracks") {
            onView(withId(R.id.tracksTitleGroup)).perform(scrollToWithBarsHidden())
        }
        auditor.auditBothThemes("Visitante · HU02 · Detalle de álbum · comentarios") {
            onView(withId(R.id.commentsTitleGroup)).perform(scrollToWithBarsHidden())
        }

        openTab(R.id.artistsGraph)
        openArtist(BAND)
        auditor.auditBothThemes("Visitante · HU04 · Detalle de banda")
        auditor.auditBothThemes("Visitante · HU04 · Detalle de banda · álbumes") {
            onView(withId(R.id.albumsTitleGroup)).perform(scrollToWithBarsHidden())
        }
        auditor.auditBothThemes("Visitante · HU04 · Detalle de banda · premios") {
            onView(withId(R.id.prizesTitleGroup)).perform(scrollToWithBarsHidden())
        }

        openTab(R.id.collectorsGraph)
        openCollector(COLLECTOR)
        auditor.auditBothThemes("Visitante · HU06 · Detalle de coleccionista")
        auditor.auditBothThemes("Visitante · HU06 · Detalle de coleccionista · comentarios") {
            onView(withId(R.id.commentsTitle)).perform(scrollToWithBarsHidden())
        }
    }

    private companion object {
        const val ALBUM = "Buscando América"
        const val BAND = "Queen"
        const val COLLECTOR = "Camilo Cifuentes"
    }
}
