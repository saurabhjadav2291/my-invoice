package com.revishingstudio.invoice.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
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

    BackHandler(enabled = nav.canGoBack) { nav.back() }

    CompositionLocalProvider(LocalActions provides actions) {
        val current = nav.current
        val isTab = current == Screen.Invoices || current == Screen.Clients || current == Screen.Settings
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (isTab) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = current == Screen.Invoices,
                            onClick = { nav.tab(Screen.Invoices) },
                            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                            label = { Text("Invoices") },
                        )
                        NavigationBarItem(
                            selected = current == Screen.Clients,
                            onClick = { nav.tab(Screen.Clients) },
                            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            label = { Text("Clients") },
                        )
                        NavigationBarItem(
                            selected = current == Screen.Settings,
                            onClick = { nav.tab(Screen.Settings) },
                            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                            label = { Text("Settings") },
                        )
                    }
                }
            },
        ) { outer ->
            val modifier = Modifier.padding(bottom = outer.calculateBottomPadding())
            when (current) {
                Screen.Invoices -> InvoicesScreen(nav, modifier)
                Screen.Clients -> ClientsScreen(nav, modifier)
                Screen.Settings -> SettingsScreen(nav, modifier)
                is Screen.InvoiceDetail -> InvoiceDetailScreen(current.id, nav, modifier)
                is Screen.InvoiceEditor -> InvoiceEditorScreen(current.id, nav, modifier)
                is Screen.ClientEditor -> ClientEditorScreen(current.id, nav, modifier)
                is Screen.BankEditor -> BankEditorScreen(current.id, nav, modifier)
            }
        }
    }
}
