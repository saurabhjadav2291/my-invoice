package com.revishingstudio.invoice.ui

import androidx.activity.compose.BackHandler
import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.material3.Snackbar
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import com.revishingstudio.invoice.core.InvoiceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Every screen in the app. Screens are kept on a simple back stack. */
sealed interface Screen {
    data object Invoices : Screen
    data object Clients : Screen
    data object Settings : Screen
    data class InvoiceDetail(val id: Long) : Screen
    /** id = null creates a new invoice. */
    data class InvoiceEditor(val id: Long?) : Screen
    data class ClientEditor(val id: Long?) : Screen
    data class BankEditor(val id: Long?) : Screen
}

class Navigator(private val stack: MutableList<Screen>) {
    val current: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun go(screen: Screen) {
        stack += screen
    }

    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    /** Switches tab, clearing anything opened on top. */
    fun tab(screen: Screen) {
        stack.clear()
        stack += screen
    }

    /** Replaces the current screen, e.g. editor -> details after saving. */
    fun replace(screen: Screen) {
        stack[stack.lastIndex] = screen
    }
}

/** Runs database work off the main thread and shows any error as a message. */
class Actions(
    val repository: InvoiceRepository,
    private val scope: CoroutineScope,
    private val snackbar: SnackbarHostState,
) {
    fun run(successMessage: String? = null, onDone: () -> Unit = {}, block: suspend InvoiceRepository.() -> Unit) {
        scope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { repository.block() } }
            result.onSuccess {
                onDone()
                successMessage?.let { snackbar.showSnackbar(it) }
            }
            result.onFailure { snackbar.showSnackbar(it.message ?: "Something went wrong") }
        }
    }

    fun message(text: String) {
        scope.launch { snackbar.showSnackbar(text) }
    }
}

val LocalActions = staticCompositionLocalOf<Actions> { error("Actions not provided") }

@Composable
fun InvoiceAppUi(repository: InvoiceRepository) {
    val stack = remember { mutableStateListOf<Screen>(Screen.Invoices) }
    val nav = remember { Navigator(stack) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val actions = remember { Actions(repository, scope, snackbar) }
    val profile by repository.observeProfile().collectAsStateWithLifecycle(null)
    val clients by repository.observeClients().collectAsStateWithLifecycle(null)

    BackHandler(enabled = nav.canGoBack) { nav.back() }

    val current = nav.current
    val isTab = current == Screen.Invoices || current == Screen.Clients || current == Screen.Settings
    val colors = Ui.colors

    // Every screen sits on the light canvas, so the system bar icons are dark.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
        }
    }

    fun newInvoice() {
        when {
            profile == null -> {
                actions.message("First add your details in Settings")
                nav.tab(Screen.Settings)
            }
            clients.isNullOrEmpty() -> {
                actions.message("First add a client")
                nav.go(Screen.ClientEditor(null))
            }
            else -> nav.go(Screen.InvoiceEditor(null))
        }
    }

    val haze = rememberHazeState()
    val systemBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Room the tab lists keep free so their last row can rest above the tab bar.
    val bottomBarPadding = if (isTab) NavBandHeight + systemBottom else 0.dp

    CompositionLocalProvider(
        LocalActions provides actions,
        LocalBottomBarPadding provides bottomBarPadding,
    ) {
        Box(Modifier.fillMaxSize()) {
            // Everything that scrolls is the blur source for the bands and the tab bar.
            AppBackground(Modifier.hazeSource(haze)) {
                when (current) {
                    Screen.Invoices -> InvoicesScreen(nav)
                    Screen.Clients -> ClientsScreen(nav)
                    Screen.Settings -> SettingsScreen(nav)
                    is Screen.InvoiceDetail -> InvoiceDetailScreen(current.id, nav)
                    is Screen.InvoiceEditor -> InvoiceEditorScreen(current.id, nav)
                    is Screen.ClientEditor -> ClientEditorScreen(current.id, nav)
                    is Screen.BankEditor -> BankEditorScreen(current.id, nav)
                }
            }
            if (isTab) {
                // Progressive blur over the status bar only (design.md section 5.5).
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .progressiveBlur(haze, colors.background.copy(alpha = 0.55f), blurAtTop = true, blur = 20.dp),
                )
                // Progressive blur behind the bottom navigation only, with the tab bar on top.
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .progressiveBlur(haze, colors.background.copy(alpha = 0.6f), blurAtTop = false, blur = 24.dp),
                    )
                    FloatingNavBar(
                        listOf(
                            NavItem("Invoices", AppIcons.Invoices, current == Screen.Invoices) { nav.tab(Screen.Invoices) },
                            NavItem("Clients", AppIcons.Clients, current == Screen.Clients) { nav.tab(Screen.Clients) },
                            NavItem("Settings", AppIcons.Settings, current == Screen.Settings) { nav.tab(Screen.Settings) },
                        ),
                        onNewInvoice = ::newInvoice,
                        modifier = Modifier.padding(top = NavBandHeight - NavBarHeight).navigationBarsPadding(),
                    )
                }
            }
            SnackbarHost(
                snackbar,
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomBarPadding)
                    .navigationBarsPadding()
                    .imePadding(),
            ) { data ->
                Snackbar(
                    data,
                    modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = colors.inverse,
                    contentColor = colors.onInverse,
                    actionColor = Color(0xFF8DB4F5),
                )
            }
        }
    }
}

/** Height of the tab bar row: 8 dp above, the 64 dp bar, 12 dp below. */
private val NavBarHeight = 84.dp

/** The blur band behind the tab bar reaches a little above it (about 100 dp in all). */
private val NavBandHeight = 100.dp

/** Bottom padding a tab screen adds so its content can rest above the tab bar. */
val LocalBottomBarPadding = compositionLocalOf { 0.dp }
