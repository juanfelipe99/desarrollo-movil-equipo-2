package com.equipo2.vinilos.ui.thememode.controls

import android.widget.ImageButton
import androidx.lifecycle.LifecycleOwner
import com.equipo2.vinilos.R
import com.equipo2.vinilos.data.model.ThemeMode
import com.equipo2.vinilos.ui.common.collectWhileVisible
import com.equipo2.vinilos.ui.common.setClickLabel
import com.equipo2.vinilos.ui.thememode.ThemeModeViewModel
import com.equipo2.vinilos.ui.thememode.iconRes
import com.equipo2.vinilos.ui.thememode.nameRes
import kotlinx.coroutines.flow.filterNotNull

/** Botón de tema: muestra el tema elegido y al tocarlo pasa al siguiente. */
fun ImageButton.bindThemeModeToggle(themeVm: ThemeModeViewModel, lifecycleOwner: LifecycleOwner) {
    lifecycleOwner.collectWhileVisible(themeVm.themeMode.filterNotNull()) { mode -> show(mode, themeVm) }
}

private fun ImageButton.show(mode: ThemeMode, themeVm: ThemeModeViewModel) {
    val next = mode.next
    val current = context.getString(mode.nameRes)

    setImageResource(mode.iconRes)
    contentDescription = context.getString(R.string.cd_theme_mode_toggle, current)
    setClickLabel(context.getString(R.string.action_theme_mode_toggle, context.getString(next.nameRes)))
    setOnClickListener { themeVm.setThemeMode(next) }
}

/** Orden al tocar: del sistema, claro, oscuro y otra vez del sistema. */
private val ThemeMode.next: ThemeMode
    get() = when (this) {
        ThemeMode.SYSTEM -> ThemeMode.LIGHT
        ThemeMode.LIGHT -> ThemeMode.DARK
        ThemeMode.DARK -> ThemeMode.SYSTEM
    }
