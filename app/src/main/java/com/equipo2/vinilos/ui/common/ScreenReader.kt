package com.equipo2.vinilos.ui.common

import android.text.SpannableString
import android.text.Spanned
import android.text.style.TtsSpan
import android.view.View
import android.widget.TextView
import androidx.core.view.ViewCompat
import com.equipo2.vinilos.R

/*
 * Ayudas para TalkBack. Las versiones de AndroidX funcionan desde Android 5, aunque los atributos de XML
 * equivalentes (screenReaderFocusable y accessibilityHeading) solo existen desde Android 9.
 */

/**
 * TalkBack lee la vista con todos sus hijos en un solo paso, por ejemplo la etiqueta y el valor de un dato o un track.
 * Con false vuelve a leerlos por separado, para cuando la vista queda dentro de otra que ya se lee entera.
 */
fun View.readAsOne(enabled: Boolean = true) {
    ViewCompat.setScreenReaderFocusable(this, enabled)
}

/** TalkBack la anuncia como encabezado, y así se puede saltar de una sección a otra. */
fun View.markAsHeading() {
    ViewCompat.setAccessibilityHeading(this, true)
}

/**
 * Título de sección. Si tiene contador al lado, el título y el contador se leen juntos, por ejemplo
 * «Tracks, 2 canciones, encabezado», en vez de dos pasos.
 */
fun View.markAsSectionTitle() {
    readAsOne()
    markAsHeading()
}

/** El contador de una sección («· 2 canciones»). TalkBack lo lee sin el punto del principio. */
fun TextView.setSectionCount(count: String) {
    text = count
    contentDescription = count.trimStart('·', ' ')
}

/** Una duración «m:ss» que TalkBack lee como duración («5 minutos y 5 segundos») y no como hora. */
fun TextView.setDuration(duration: String) {
    text = duration
    val parts = duration.split(':').mapNotNull { it.trim().toIntOrNull() }
    contentDescription = if (parts.size == 2) {
        val (minutes, seconds) = parts
        val res = resources
        val minutesText = res.getQuantityString(R.plurals.spoken_minutes, minutes, minutes)
        val secondsText = res.getQuantityString(R.plurals.spoken_seconds, seconds, seconds)
        when {
            seconds == 0 -> minutesText
            minutes == 0 -> secondsText
            else -> res.getString(R.string.spoken_duration, minutesText, secondsText)
        }
    } else {
        null
    }
}

/**
 * Un contenedor con desplazamiento que, cuando todo cabe, TalkBack enfocaba entero leyendo el texto de sus hijos
 * antes de leerlos otra vez uno por uno. Mientras no se puede desplazar sale del árbol de accesibilidad y sus hijos
 * quedan igual. Cuando sí se puede (letra grande o pantalla baja) vuelve, porque TalkBack lo necesita para desplazarse.
 */
fun View.hideFromScreenReaderWhileItFits() {
    val update = {
        val scrolls = canScrollVertically(1) || canScrollVertically(-1)
        importantForAccessibility =
            if (scrolls) View.IMPORTANT_FOR_ACCESSIBILITY_AUTO else View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> update() }
    update()
}

/** Un número de teléfono que TalkBack lee como teléfono y no como una cifra de miles de millones. */
fun TextView.setPhoneNumber(number: String) {
    text = SpannableString(number).apply {
        setSpan(TtsSpan.TelephoneBuilder(number).build(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}
