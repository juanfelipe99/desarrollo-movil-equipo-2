package com.equipo2.vinilos.hu07

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isSelected
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.equipo2.vinilos.R
import com.equipo2.vinilos.support.VinilosRule
import com.equipo2.vinilos.support.chooseInRoleMenu
import com.equipo2.vinilos.support.enterAs
import com.equipo2.vinilos.support.toolbarTitle
import com.equipo2.vinilos.support.waitForNightMode
import com.equipo2.vinilos.support.waitUntil
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** E2E de HU07: selección de rol, menú de rol, tema y botón «+». */
@RunWith(AndroidJUnit4::class)
class HU07RolesTest {

    @get:Rule
    val app = VinilosRule()

    @Test
    fun alAbrir_muestraLaSeleccionDeRol_sinBarras() {
        app.launch()

        onView(withText(R.string.role_select_question)).check(matches(isDisplayed()))
        onView(withId(R.id.visitorButton)).check(matches(isDisplayed()))
        onView(withId(R.id.collectorButton)).check(matches(isDisplayed()))
        onView(withId(R.id.toolbar)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.navBar)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun atrasEnLaSeleccionDeRol_cierraLaApp() {
        app.launch()

        pressBackUnconditionally()

        // Android termina de cerrar la actividad un momento después de volver
        waitUntil { app.state == Lifecycle.State.DESTROYED }
    }

    @Test
    fun visitante_entraAAlbumes_sinElBotonCrear() {
        app.launch()

        enterAs(collector = false)

        onView(toolbarTitle(R.string.album_list_title)).check(matches(isDisplayed()))
        onView(withId(R.id.roleChip)).check(matches(withContentDescription("Cambiar de rol. Modo actual: Visitante")))
        onView(withId(R.id.createAlbumButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun coleccionista_entraAAlbumes_conElBotonCrear() {
        app.launch()

        enterAs(collector = true)

        onView(withId(R.id.roleChip)).check(matches(withContentDescription("Cambiar de rol. Modo actual: Coleccionista")))
        onView(withId(R.id.createAlbumButton)).check(matches(isDisplayed()))
    }

    @Test
    fun menuDeRol_marcaElRolActual_yCambiarAVisitanteAvisaYQuitaElBoton() {
        app.launch()
        enterAs(collector = true)

        onView(withId(R.id.roleChip)).perform(click())
        onView(withText("MODO ACTUAL: COLECCIONISTA")).inRoot(isPlatformPopup()).check(matches(isDisplayed()))
        onView(withId(R.id.optionCollector)).inRoot(isPlatformPopup()).check(matches(isSelected()))
        onView(withId(R.id.optionVisitor)).inRoot(isPlatformPopup()).perform(click())

        onView(withText(R.string.snackbar_role_visitor)).check(matches(isDisplayed()))
        onView(withId(R.id.createAlbumButton)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun menuDeRol_cambiarAColeccionista_avisaYMuestraElBoton() {
        app.launch()
        enterAs(collector = false)

        chooseInRoleMenu(R.id.optionCollector)

        onView(withText(R.string.snackbar_role_collector)).check(matches(isDisplayed()))
        onView(withId(R.id.createAlbumButton)).check(matches(isDisplayed()))
    }

    @Test
    fun menuDeRol_controlDeTema_cambiaEntreOscuroClaroYDelSistema_yMarcaElElegido() {
        app.launch()
        enterAs(collector = false)

        chooseInRoleMenu(R.id.themeDark)
        onView(toolbarTitle(R.string.album_list_title)).check(matches(isDisplayed()))
        waitForNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        onView(withId(R.id.roleChip)).perform(click())
        onView(withId(R.id.themeDark)).inRoot(isPlatformPopup()).check(matches(isSelected()))
        onView(withId(R.id.themeLight)).inRoot(isPlatformPopup()).perform(click())
        waitForNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        chooseInRoleMenu(R.id.themeSystem)
        onView(toolbarTitle(R.string.album_list_title)).check(matches(isDisplayed()))
        waitForNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    @Test
    fun botonDeTema_pasaPorClaroOscuroYDelSistema() {
        app.launch()

        onView(withId(R.id.themeModeButton)).perform(click())
        waitForNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        onView(withId(R.id.themeModeButton)).perform(click())
        waitForNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        onView(withId(R.id.themeModeButton)).perform(click())
        waitForNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    @Test
    fun menuDeRol_volverALaPantallaDeInicio_muestraLaSeleccionDeRol() {
        app.launch()
        enterAs(collector = true)

        chooseInRoleMenu(R.id.optionRoleSelect)

        onView(withText(R.string.role_select_question)).check(matches(isDisplayed()))
        onView(withId(R.id.navBar)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun botonCrear_abreCrearAlbum_sinChipNiBarraInferior() {
        app.launch()

        openCreateAlbum()

        onView(toolbarTitle(R.string.album_create_title)).check(matches(isDisplayed()))
        onView(withId(R.id.roleChipSlot)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.navBar)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun crearAlbum_laXVuelveAAlbumes() {
        app.launch()
        openCreateAlbum()

        onView(withContentDescription(R.string.cd_close_without_saving)).perform(click())

        onView(toolbarTitle(R.string.album_list_title)).check(matches(isDisplayed()))
        onView(withId(R.id.createAlbumButton)).check(matches(isDisplayed()))
    }

    @Test
    fun crearAlbum_cancelarVuelveAAlbumes() {
        app.launch()
        openCreateAlbum()

        onView(withId(R.id.cancelButton)).perform(click())

        onView(toolbarTitle(R.string.album_list_title)).check(matches(isDisplayed()))
        onView(withId(R.id.saveButton)).check(doesNotExist())
    }
}
