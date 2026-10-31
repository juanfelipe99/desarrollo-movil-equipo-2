package com.equipo2.vinilos

import android.os.Bundle
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.activity.enableEdgeToEdge
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.equipo2.vinilos.databinding.ActivityMainBinding
import com.equipo2.vinilos.ui.common.AppSnackbar
import com.equipo2.vinilos.ui.common.ScrollsToTop
import com.equipo2.vinilos.ui.common.applySystemBarsPadding
import com.equipo2.vinilos.ui.role.changedMessageRes
import com.equipo2.vinilos.ui.role.chip.RoleChip
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.snackbar.Snackbar

/** Única actividad de la app, con la barra superior, las pantallas y la barra de navegación. */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /** Vista que flota sobre la barra inferior, como el botón «+». Los avisos salen encima y se mueve con las barras. */
    var snackbarAnchor: View? = null
        set(value) {
            field = value
            placeSnackbarAnchor(animate = false)
        }

    private var snackbar: Snackbar? = null

    private var toolbarHidesOnScroll = false
    private var appBarOffset = 0
    private var bottomBarHidden = false

    /** Con TalkBack las barras se quedan fijas, porque si desaparecen el lector no las encuentra. */
    private val barsHideOnScroll by lazy {
        ContextCompat.getSystemService(this, AccessibilityManager::class.java)?.isTouchExplorationEnabled != true
    }

    /** Solo en vertical, en horizontal la navegación es un riel que no se esconde. */
    @Suppress("UNCHECKED_CAST")
    private val bottomBarBehavior: HideBottomViewOnScrollBehavior<View>?
        get() = (binding.bottomBar?.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior
            as? HideBottomViewOnScrollBehavior<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        // Antes de super.onCreate, para que la pantalla de inicio pase al tema de la app
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        setUpBars()
        setUpNavigation(firstLaunch = savedInstanceState == null)
        RoleChip(this, binding.roleChip, onRoleChanged = { role -> showSnackbar(getString(role.changedMessageRes)) })
    }

    /** Muestra un aviso encima de snackbarAnchor o de la barra inferior, el primero visible. */
    fun showSnackbar(message: CharSequence, isError: Boolean = false) {
        // Sin la barra inferior a la vista, por el riel o porque se escondió, el aviso va abajo del todo
        val bottomNav = binding.navBar.takeIf { binding.bottomBar != null && !bottomBarHidden }
        val target = listOf(snackbarAnchor, bottomNav).firstOrNull { it?.isShown == true }
        // En horizontal el aviso va en el área del contenido, sin tapar el riel
        snackbar = AppSnackbar.show(binding.snackbarParent ?: binding.root, message, isError, target)
    }

    private fun setUpBars() {
        binding.appBar.addOnOffsetChangedListener { _, offset ->
            appBarOffset = offset
            placeSnackbarAnchor(animate = false)
        }

        val bottomBar = binding.bottomBar ?: return
        if (!barsHideOnScroll) {
            bottomBar.updateLayoutParams<CoordinatorLayout.LayoutParams> { behavior = null }
            return
        }
        bottomBarBehavior?.addOnScrollStateChangedListener { _, state ->
            bottomBarHidden = state == HideBottomViewOnScrollBehavior.STATE_SCROLLED_DOWN
            placeSnackbarAnchor(animate = true)
        }
    }

    /** Baja la vista flotante con la barra inferior y la sube lo que la pantalla deja fuera mientras se ve la superior. */
    private fun placeSnackbarAnchor(animate: Boolean) {
        val view = snackbarAnchor ?: return
        if (!::binding.isInitialized) return
        // Mientras se ve la barra superior, el contenido sobresale por abajo lo que esa barra puede esconderse
        val belowScreen = if (toolbarHidesOnScroll) binding.appBar.totalScrollRange + appBarOffset else 0
        val bottomBarSpace = if (bottomBarHidden) binding.bottomBar?.height ?: 0 else 0
        val y = (bottomBarSpace - belowScreen).toFloat()

        view.animate().cancel()
        if (animate) view.animate().translationY(y) else view.translationY = y
    }

    private fun setUpNavigation(firstLaunch: Boolean) {
        val navController = binding.navHost.getFragment<NavHostFragment>().navController
        binding.toolbar.setupWithNavController(navController, AppBarConfiguration(binding.navBar.menu))
        binding.navBar.setupWithNavController(navController)
        binding.navBar.setOnItemReselectedListener { item -> onTabReselected(navController, item.itemId) }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // El aviso es de la pantalla que se deja, y la nueva muestra el suyo después de llegar
            snackbar?.dismiss()
            updateBars(destination.id)
        }

        // Al abrir va a la selección de rol, y al recrearse se queda donde estaba
        if (firstLaunch) navController.navigate(R.id.action_global_roleSelect)
    }

    /**
     * Tocar otra vez la pestaña en la que ya se está, como en Reddit o X.
     * Desde el detalle vuelve a la lista, y en la lista sube al inicio y muestra las barras.
     */
    private fun onTabReselected(navController: NavController, @IdRes tabId: Int) {
        // Cada pestaña es un subgrafo y su lista es la primera pantalla
        val list = (navController.graph.findNode(tabId) as NavGraph).startDestinationId
        if (navController.currentDestination?.id != list) {
            navController.popBackStack(list, false)
            return
        }

        showBars(animate = true)
        val screen = binding.navHost.getFragment<NavHostFragment>().childFragmentManager.primaryNavigationFragment
        (screen as? ScrollsToTop)?.scrollToTop()
    }

    /** Oculta las barras que no lleva cada pantalla y muestra las que se habían escondido al bajar. */
    private fun updateBars(@IdRes destinationId: Int) {
        val roleSelect = destinationId == R.id.roleSelectFragment
        val albumCreate = destinationId == R.id.albumCreateFragment

        // Se ocultan la barra y la línea, no el AppBarLayout, porque sin él la pantalla quedaría debajo de la barra de estado
        binding.toolbar.isVisible = !roleSelect
        binding.toolbarDivider.isVisible = !roleSelect
        binding.roleChipSlot.isVisible = !albumCreate
        // En el formulario la X para cerrar debe estar siempre a mano
        setToolbarHidesOnScroll(barsHideOnScroll && !albumCreate)

        binding.navBar.isVisible = !roleSelect && !albumCreate
        binding.navDivider.isVisible = binding.navBar.isVisible
        showBars(animate = false)

        // Crear álbum lleva una X en lugar de la flecha de volver
        if (albumCreate) {
            binding.toolbar.setNavigationIcon(R.drawable.ic_close)
            binding.toolbar.setNavigationContentDescription(R.string.cd_close_without_saving)
        }
    }

    /** Vuelve a mostrar las barras que se escondieron al bajar. */
    private fun showBars(animate: Boolean) {
        binding.appBar.setExpanded(true, animate)
        binding.bottomBar?.let { bottomBarBehavior?.slideUp(it, animate) }
    }

    private fun setToolbarHidesOnScroll(hides: Boolean) {
        toolbarHidesOnScroll = hides
        val flags = if (hides) {
            AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or
                AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
        } else {
            0
        }
        listOf(binding.toolbar, binding.toolbarDivider).forEach { bar ->
            bar.updateLayoutParams<AppBarLayout.LayoutParams> { scrollFlags = flags }
        }
    }
}
