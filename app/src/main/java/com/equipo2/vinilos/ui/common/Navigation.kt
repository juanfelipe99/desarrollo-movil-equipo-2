package com.equipo2.vinilos.ui.common

import android.os.Bundle
import androidx.activity.addCallback
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach

/** Navega solo si la acción existe desde la pantalla actual y devuelve si navegó. */
fun NavController.navigateIfPossible(@IdRes actionId: Int, args: Bundle? = null): Boolean {
    // Si la acción no existe desde aquí, ya se navegó con un toque anterior
    if (currentDestination?.getAction(actionId) == null) return false
    navigate(actionId, args)
    return true
}

/** Atrás cierra la app mientras se vea esta pantalla. */
fun Fragment.finishAppOnBack() {
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { requireActivity().finish() }
}

/**
 * Entrada del historial de esta pantalla, mientras se ve. Navigation le pone al fragment el id de su entrada como etiqueta.
 *
 * No se usa currentBackStackEntry porque con el retroceso predictivo de Android 15 o superior, que también usa la tecla
 * Atrás, la pantalla anterior arma su vista antes de volver a ser la actual, y en ese momento la actual es la que se cierra.
 */
val Fragment.ownBackStackEntry: NavBackStackEntry?
    get() = findNavController().visibleEntries.value.lastOrNull { it.id == tag }

/**
 * Recibe el resultado que otra pantalla deja en la entrada de esta, por ejemplo el álbum recién creado.
 * Se borra al leerlo, así no se repite al girar. La entrada se busca cada vez que la pantalla vuelve a verse.
 */
fun <T : Any> Fragment.collectResult(key: String, action: suspend (T) -> Unit) {
    val result = flow {
        val savedState = ownBackStackEntry?.savedStateHandle ?: return@flow
        emitAll(savedState.getStateFlow<T?>(key, null).filterNotNull().onEach { savedState.remove<T>(key) })
    }
    viewLifecycleOwner.collectWhileVisible(result, action)
}
