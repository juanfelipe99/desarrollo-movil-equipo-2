package com.equipo2.vinilos.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.equipo2.vinilos.R
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import kotlin.math.abs
import kotlin.math.sign

/**
 * Cuenta lo que se desplaza en una misma dirección y avisa cuando pasa la tolerancia,
 * así un movimiento corto no esconde ni muestra las barras, como en Reddit.
 */
private class ScrollTolerance(context: Context) {

    private val tolerance = context.resources.getDimensionPixelSize(R.dimen.bars_scroll_tolerance)
    private var direction = 0
    private var distance = 0

    /** Con cada toque nuevo hay que volver a pasar la tolerancia. */
    fun reset() {
        direction = 0
        distance = 0
    }

    /** Suma el desplazamiento y devuelve si ya pasó la tolerancia en esa dirección. */
    fun passed(dy: Int): Boolean {
        if (dy == 0) return distance >= tolerance
        val newDirection = dy.sign
        if (newDirection != direction) {
            direction = newDirection
            distance = 0
        }
        distance += abs(dy)
        return distance >= tolerance
    }
}

/** Barra superior que solo empieza a moverse después de la tolerancia. */
class TolerantAppBarBehavior(context: Context, attrs: AttributeSet?) : AppBarLayout.Behavior(context, attrs) {

    private val tolerance = ScrollTolerance(context)

    override fun onStartNestedScroll(
        parent: CoordinatorLayout,
        child: AppBarLayout,
        directTargetChild: View,
        target: View,
        nestedScrollAxes: Int,
        type: Int,
    ): Boolean {
        // El impulso que sigue al dedo continúa el mismo gesto, no empieza otro
        if (type == ViewCompat.TYPE_TOUCH) tolerance.reset()
        val started = super.onStartNestedScroll(parent, child, directTargetChild, target, nestedScrollAxes, type)

        // Material compara con el alto de la pantalla sin descontar el margen de las barras del sistema,
        // y en teléfonos con barras altas, como Android 5.0, la barra nunca se escondía
        val vertical = nestedScrollAxes and ViewCompat.SCROLL_AXIS_VERTICAL != 0
        val contentHeight = parent.height - parent.paddingTop - parent.paddingBottom
        val fits = contentHeight - directTargetChild.height <= child.height
        return started || (vertical && child.totalScrollRange > 0 && fits)
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: AppBarLayout,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int,
    ) {
        if (!tolerance.passed(dy)) return
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
    }
}

/** Barra inferior que solo se esconde o vuelve después de la tolerancia. */
class TolerantHideBottomViewBehavior(context: Context, attrs: AttributeSet?) :
    HideBottomViewOnScrollBehavior<View>(context, attrs) {

    private val tolerance = ScrollTolerance(context)

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        directTargetChild: View,
        target: View,
        nestedScrollAxes: Int,
        type: Int,
    ): Boolean {
        if (type == ViewCompat.TYPE_TOUCH) tolerance.reset()
        return super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, nestedScrollAxes, type)
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray,
    ) {
        if (!tolerance.passed(dyConsumed)) return
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed)
    }
}
