package com.equipo2.vinilos.ui.thememode.controls

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.core.view.doOnLayout
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.ThemeMode
import com.equipo2.vinilos.databinding.ViewThemeModeSwitchBinding
import kotlin.math.abs

/** Interruptor de tres posiciones: claro, del sistema y oscuro. La píldora se mueve al tocar o al arrastrarla. */
class ThemeModeSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    var onModeSelected: (ThemeMode) -> Unit = {}

    private val binding = ViewThemeModeSwitchBinding.inflate(LayoutInflater.from(context), this)
    private val thumb = binding.themeThumb
    private val slots = mapOf(
        ThemeMode.LIGHT to binding.themeLight,
        ThemeMode.SYSTEM to binding.themeSystem,
        ThemeMode.DARK to binding.themeDark,
    )
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var mode = ThemeMode.SYSTEM
    private var downX = 0f
    private var dragStart = 0f

    init {
        setBackgroundResource(R.drawable.bg_theme_mode_track)
        // El riel trae el margen de 6 dp como relleno, y los íconos deben ocupar los 48 dp
        setPadding(0, 0, 0, 0)
        slots.forEach { (slotMode, slot) -> slot.setOnClickListener { select(slotMode) } }
    }

    /** Muestra el tema guardado, sin animación. */
    fun show(mode: ThemeMode) {
        this.mode = mode
        markSelected(mode)
        doOnLayout { thumb.translationX = thumbX(mode) }
    }

    // Un arrastre horizontal se lo quita a los íconos y al menú, que también se desplaza
    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> startDrag(event)
            MotionEvent.ACTION_MOVE -> if (abs(event.x - downX) > touchSlop) {
                parent.requestDisallowInterceptTouchEvent(true)
                return true
            }
        }
        return false
    }

    // Cada ícono ya es un botón para TalkBack, el arrastre es solo un atajo
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> startDrag(event)
            MotionEvent.ACTION_MOVE -> {
                val ends = listOf(thumbX(ThemeMode.LIGHT), thumbX(ThemeMode.DARK))
                thumb.translationX = (dragStart + event.x - downX).coerceIn(ends.min(), ends.max())
                markSelected(nearest())
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> select(nearest())
        }
        return true
    }

    private fun startDrag(event: MotionEvent) {
        downX = event.x
        dragStart = thumb.translationX
    }

    /** Lleva la píldora al tema elegido y avisa al terminar, si cambió. */
    private fun select(selected: ThemeMode) {
        markSelected(selected)
        thumb.animate()
            .translationX(thumbX(selected))
            .setDuration(ANIMATION_MS)
            .withEndAction {
                if (selected == mode) return@withEndAction
                mode = selected
                onModeSelected(selected)
            }
    }

    private fun markSelected(selected: ThemeMode) {
        slots.forEach { (slotMode, slot) -> slot.isSelected = slotMode == selected }
    }

    private fun nearest(): ThemeMode = slots.keys.minBy { abs(thumbX(it) - thumb.translationX) }

    /** Posición de la píldora centrada en el ícono del tema. */
    private fun thumbX(mode: ThemeMode): Float {
        val slot = slots.getValue(mode)
        return slot.left + (slot.width - thumb.width) / 2f
    }

    private companion object {
        const val ANIMATION_MS = 180L
    }
}
