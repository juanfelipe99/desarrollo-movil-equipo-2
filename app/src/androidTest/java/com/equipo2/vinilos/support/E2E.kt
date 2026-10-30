package com.equipo2.vinilos.support

import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.equipo2.vinilos.R
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.textfield.TextInputLayout
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher

/** Título de la barra superior, sin confundirlo con textos iguales de la pantalla. */
fun toolbarTitle(@StringRes title: Int): Matcher<View> = allOf(withText(title), isDescendantOfA(withId(R.id.toolbar)))

/** Campo que muestra este error, o ninguno si es null. */
fun hasFieldError(@StringRes message: Int?): Matcher<View> = object : TypeSafeMatcher<View>() {
    private val expected = message?.let { InstrumentationRegistry.getInstrumentation().targetContext.getString(it) }

    override fun describeTo(description: Description) {
        description.appendText("campo con el error ").appendValue(expected)
    }

    override fun matchesSafely(view: View): Boolean = view is TextInputLayout && view.error?.toString() == expected
}

fun enterAs(collector: Boolean) {
    // Con la fuente grande el botón puede quedar debajo del borde de la pantalla
    onView(withId(if (collector) R.id.collectorButton else R.id.visitorButton)).perform(scrollTo(), click())
}

/** Elige una opción del menú de rol, que es una ventana aparte. */
fun chooseInRoleMenu(@IdRes optionId: Int) {
    onView(withId(R.id.roleChip)).perform(click())
    onView(withId(optionId)).inRoot(isPlatformPopup()).perform(click())
}

/**
 * Baja hasta la vista con las barras escondidas, como quedan cuando el usuario baja.
 * Con las barras a la vista, el final del contenido queda fuera de la pantalla o detrás de la barra inferior.
 */
fun scrollToWithBarsHidden(): ViewAction = object : ViewAction {
    private val scrollTo = scrollTo()

    override fun getConstraints(): Matcher<View> = scrollTo.constraints

    override fun getDescription() = "esconder las barras y ${scrollTo.description}"

    override fun perform(uiController: UiController, view: View) {
        val root = view.rootView
        root.findViewById<AppBarLayout>(R.id.appBar)?.setExpanded(false, false)
        root.findViewById<View>(R.id.bottomBar)?.let { bar ->
            val behavior = (bar.layoutParams as CoordinatorLayout.LayoutParams).behavior
            (behavior as? HideBottomViewOnScrollBehavior<View>)?.slideDown(bar, false)
        }
        uiController.loopMainThreadUntilIdle()
        scrollTo.perform(uiController, view)
    }
}

/** Elige una opción del desplegable, que es una ventana aparte. */
fun pickOption(@IdRes inputId: Int, option: String) {
    onView(withId(inputId)).perform(scrollTo(), click())
    onView(withText(option)).inRoot(isPlatformPopup()).perform(click())
}

/** Espera algo que Espresso no vigila, como un guardado en DataStore. */
fun waitUntil(timeoutMs: Long = 10_000, condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!condition()) {
        check(System.currentTimeMillis() < deadline) { "La condición no se cumplió en ${timeoutMs / 1000} s" }
        Thread.sleep(100)
    }
}

fun waitForNightMode(mode: Int) = waitUntil { AppCompatDelegate.getDefaultNightMode() == mode }
