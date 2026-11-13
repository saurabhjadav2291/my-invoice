package com.revishingstudio.invoice.ui

import android.util.Log
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.flow.first

/** Every screen in the app. Screens are kept on a simple back stack. */
sealed interface Screen {
    data object Invoices : Screen
    data object Clients : Screen
    data object Settings : Screen
    data object AccountDetails : Screen
    data object BusinessInfo : Screen
    data class InvoiceDetail(val id: Long) : Screen
    /** id = null creates a new invoice. */
    data class InvoiceEditor(val id: Long?) : Screen
    data class ClientEditor(val id: Long?) : Screen
    data class BankEditor(val id: Long?) : Screen
    /** An invoice PDF shown inside the app. */
    data class PdfPreview(val path: String, val title: String) : Screen
    /** Google Drive backup, backup file export and import, restore. */
    data object Backup : Screen
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
    /** Lives as long as the app's UI, so work started on one screen finishes after leaving it. */
    val scope: CoroutineScope,
    private val snackbar: SnackbarHostState,
) {
    /** True while a [run] with once = true is still working. */
    var busy by mutableStateOf(false)
        private set

    /**
     * Runs [block] off the main thread. With [once], taps that arrive while it is
     * still running are ignored, so a quick double tap on Create makes one invoice.
     */
    fun run(
        successMessage: String? = null,
        onDone: () -> Unit = {},
        once: Boolean = false,
        block: suspend InvoiceRepository.() -> Unit,
    ) {
        if (once) {
            if (busy) return
            busy = true
        }
        scope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { repository.block() } }
            if (once) busy = false
            result.onSuccess {
                onDone()
                successMessage?.let { snackbar.showSnackbar(BannerVisuals(it, BannerKind.Success)) }
            }
            result.onFailure {
                Log.e("MyInvoice", "Action failed", it)
                snackbar.showSnackbar(BannerVisuals(it.friendlyMessage(), BannerKind.Error))
            }
        }
    }

    /** A hint for the person, e.g. a missing field. */
    fun message(text: String, kind: BannerKind = BannerKind.Notice) {
        scope.launch { snackbar.showSnackbar(BannerVisuals(text, kind)) }
    }
}

/** What the alert banner shows: the message, its kind and a quiet second line (the time by default). */
class BannerVisuals(
    override val message: String,
    val kind: BannerKind,
    val detail: String = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Short
}

/**
 * Text for the snackbar. Our own checks (require/check) carry a sentence written
 * for people, so show that; anything else is a bug, and a class name or stack
 * detail means nothing to the reader.
 */
fun Throwable.friendlyMessage(fallback: String = "Something went wrong. Please try again."): String {
    val text = message?.takeIf { it.isNotBlank() }
    return if ((this is IllegalArgumentException || this is IllegalStateException) && text != null) text else fallback
}

val LocalActions = staticCompositionLocalOf<Actions> { error("Actions not provided") }

@Composable
fun InvoiceAppUi(repository: InvoiceRepository) {
    val stack = rememberSaveable(saver = ScreenStackSaver) { mutableStateListOf<Screen>(Screen.Invoices) }
    val nav = remember { Navigator(stack) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val actions = remember { Actions(repository, scope, snackbar) }
    val profile by repository.observeProfile().collectAsStateWithLifecycle(null)
    val clients by repository.observeClients().collectAsStateWithLifecycle(null)
    val context = androidx.compose.ui.platform.LocalContext.current
    val backupPrefs = remember { com.revishingstudio.invoice.backup.BackupPrefs.get(context) }
    val backupStatus by backupPrefs.status.collectAsStateWithLifecycle()
    // A fresh install (no details, no clients) opens on the welcome screen until it is done.
    val freshInstall by androidx.compose.runtime.produceState<Boolean?>(null) {
        value = withContext(Dispatchers.IO) {
            repository.profile() == null && repository.observeClients().first().isEmpty()
        }
    }
    val welcome = freshInstall == true && !backupStatus.welcomeDone

    BackHandler(enabled = nav.canGoBack) { nav.back() }

    val current = nav.current
    val isTab = !welcome && (current == Screen.Invoices || current == Screen.Clients || current == Screen.Settings)
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
                actions.message("First add your name in Account details")
                nav.tab(Screen.Settings)
                nav.go(Screen.AccountDetails)
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
                if (welcome) {
                    WelcomeScreen(onDone = backupPrefs::finishWelcome)
                } else when (current) {
                    Screen.Invoices -> InvoicesScreen(nav)
                    Screen.Clients -> ClientsScreen(nav)
                    Screen.Settings -> SettingsScreen(nav)
                    Screen.AccountDetails -> AccountDetailsScreen(nav)
                    Screen.BusinessInfo -> BusinessInfoScreen(nav)
                    is Screen.InvoiceDetail -> InvoiceDetailScreen(current.id, nav)
                    is Screen.InvoiceEditor -> InvoiceEditorScreen(current.id, nav)
                    is Screen.ClientEditor -> ClientEditorScreen(current.id, nav)
                    is Screen.BankEditor -> BankEditorScreen(current.id, nav)
                    is Screen.PdfPreview -> PdfPreviewScreen(current.path, current.title, nav)
                    Screen.Backup -> BackupScreen(nav)
                }
            }
            if (isTab) {
                // Progressive blur over the status bar only (design.md section 5.5).
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .progressiveBlur(haze, BackgroundTop.copy(alpha = 0.55f), blurAtTop = true, blur = 20.dp),
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
                        modifier = Modifier.padding(top = NavBandHeight - NavBarRowHeight).navigationBarsPadding(),
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
                val visuals = data.visuals as? BannerVisuals
                AlertBanner(
                    title = data.visuals.message,
                    subtitle = visuals?.detail,
                    kind = visuals?.kind ?: BannerKind.Notice,
                    modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 8.dp),
                )
            }
        }
    }
}

/** The blur band behind the tab bar reaches a little above it (about 108 dp in all). */
private val NavBandHeight = 108.dp

/** Bottom padding a tab screen adds so its content can rest above the tab bar. */
val LocalBottomBarPadding = compositionLocalOf { 0.dp }
