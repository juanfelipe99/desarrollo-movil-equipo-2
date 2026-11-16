package com.equipo2.vinilos.support

import androidx.test.espresso.IdlingResource
import okhttp3.Dispatcher

/** Espresso espera mientras haya llamadas a la API en curso. */
class ApiIdlingResource(private val dispatcher: Dispatcher) : IdlingResource {

    @Volatile
    private var callback: IdlingResource.ResourceCallback? = null

    init {
        dispatcher.idleCallback = Runnable { callback?.onTransitionToIdle() }
    }

    override fun getName(): String = "API de Vinilos"

    /**
     * Si una llamada termina justo entre que Espresso pregunta y empieza a esperar el aviso, el aviso de OkHttp ya pasó
     * y Espresso falla con «the resource has transitioned from busy to idle was never sent». Por eso, cada vez que
     * responde que está libre, también lo avisa.
     */
    override fun isIdleNow(): Boolean {
        val idle = dispatcher.runningCallsCount() == 0
        if (idle) callback?.onTransitionToIdle()
        return idle
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        this.callback = callback
    }
}
