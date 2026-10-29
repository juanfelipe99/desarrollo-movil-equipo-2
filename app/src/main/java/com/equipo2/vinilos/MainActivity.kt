package com.equipo2.vinilos

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.equipo2.vinilos.databinding.ActivityMainBinding
import com.equipo2.vinilos.ui.common.AppSnackbar
import com.equipo2.vinilos.ui.common.applySystemBarsPadding
import com.equipo2.vinilos.ui.role.changedMessageRes
import com.equipo2.vinilos.ui.role.chip.RoleChip
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar

/** Única actividad de la app, con la barra superior, las pantallas y la barra de navegación. */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    var snackbarAnchor: View? = null

    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        setUpNavigation(firstLaunch = savedInstanceState == null)
        RoleChip(this, binding.roleChip, onRoleChanged = { role -> showSnackbar(getString(role.changedMessageRes)) })
    }

    /** Muestra un aviso encima de anchor, snackbarAnchor o la barra inferior, el primero visible. */
    fun showSnackbar(message: CharSequence, isError: Boolean = false, anchor: View? = null) {
        // El riel de la vista horizontal está al lado, así que el aviso va abajo del todo
        val bottomNav = binding.navBar.takeIf { it is BottomNavigationView }
        val target = listOf(anchor, snackbarAnchor, bottomNav).firstOrNull { it?.isShown == true }
        snackbar = AppSnackbar.show(binding.root, message, isError, target)
    }

    private fun setUpNavigation(firstLaunch: Boolean) {
        val navController = binding.navHost.getFragment<NavHostFragment>().navController
        binding.toolbar.setupWithNavController(navController, AppBarConfiguration(binding.navBar.menu))
        binding.navBar.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // El aviso es de la pantalla que se deja, y la nueva muestra el suyo después de llegar
            snackbar?.dismiss()
            updateBars(destination.id)
        }

        // Al abrir va a la selección de rol, y al recrearse se queda donde estaba
        if (firstLaunch) navController.navigate(R.id.action_global_roleSelect)
    }

    /** Oculta las barras que no lleva cada pantalla. */
    private fun updateBars(@IdRes destinationId: Int) {
        val roleSelect = destinationId == R.id.roleSelectFragment
        val albumCreate = destinationId == R.id.albumCreateFragment

        binding.toolbar.isVisible = !roleSelect
        binding.toolbarDivider.isVisible = !roleSelect
        binding.roleChipSlot.isVisible = !albumCreate

        binding.navBar.isVisible = !roleSelect && !albumCreate
        binding.navDivider.isVisible = binding.navBar.isVisible

        // Crear álbum lleva una X en lugar de la flecha de volver
        if (albumCreate) {
            binding.toolbar.setNavigationIcon(R.drawable.ic_close)
            binding.toolbar.setNavigationContentDescription(R.string.cd_close_without_saving)
        }
    }
}
