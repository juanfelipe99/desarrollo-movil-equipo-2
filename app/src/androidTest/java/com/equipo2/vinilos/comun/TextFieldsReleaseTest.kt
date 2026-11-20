package com.equipo2.vinilos.comun

import android.text.InputFilter
import android.text.SpanWatcher
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.UnderlineSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.equipo2.vinilos.ui.common.release
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Error que encontró Monkey en el Sprint 3 (Android 8). Al cerrar un formulario con el teclado conectado, quitar los
 * spans del campo hace que el teclado quite los suyos del mismo texto en medio del recorrido.
 */
@RunWith(AndroidJUnit4::class)
class TextFieldsReleaseTest {

    @Test
    fun soltarElTexto_quitaTodosLosSpans_aunqueOtroQuiteSpansMientrasTanto() {
        val text = SpannableStringBuilder("Buscando América")
        val composing = List(COMPOSING_SPANS) { UnderlineSpan() }
        // Hace lo mismo que el teclado: al quitarse cualquier span, quita los que le quedan.
        // Va primero, como el SpanController del campo, porque clearSpans recorre del último al primero
        val keyboard = object : SpanWatcher {
            override fun onSpanRemoved(text: Spannable, what: Any, start: Int, end: Int) {
                composing.forEach(text::removeSpan)
            }
            override fun onSpanAdded(text: Spannable, what: Any, start: Int, end: Int) = Unit
            override fun onSpanChanged(text: Spannable, what: Any, ostart: Int, oend: Int, nstart: Int, nend: Int) = Unit
        }
        text.setSpan(keyboard, 0, text.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        composing.forEach { text.setSpan(it, 0, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
        text.filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH))

        text.release()

        assertEquals(0, text.getSpans(0, text.length, Any::class.java).size)
        assertEquals(0, text.filters.size)
        assertEquals("Buscando América", text.toString())
    }

    private companion object {
        const val COMPOSING_SPANS = 5
        const val MAX_LENGTH = 80
    }
}
