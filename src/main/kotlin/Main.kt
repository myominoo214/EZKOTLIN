import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import kotlin.system.exitProcess
import kotlinx.coroutines.launch

// Updated imports for reorganized structure
import LoginPage
import SalesPage
import core.services.UserSession
import core.services.UpdateService
import core.services.UpdateCheckResult
import core.services.ApiService
import core.config.AppTheme
import security.AntiTamperingProtection
import ui.dialogs.UpdateDialog
import ui.dialogs.UpdateProgressDialog

@Composable
@Preview
fun App() {
    var currentScreen by remember { mutableStateOf("login") }
    val userSession = remember { UserSession.getInstance() }
    val updateService = remember { UpdateService(ApiService.httpClient) }
    val coroutineScope = rememberCoroutineScope()
    
    // Update-related state
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showUpdateProgress by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<core.models.UpdateInfo?>(null) }
    
    // Check for updates on app start
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            when (val result = updateService.checkForUpdates()) {
                is UpdateCheckResult.UpdateAvailable -> {
                    updateInfo = result.updateInfo
                    showUpdateDialog = true
                }
                is UpdateCheckResult.Error -> {
                    println("Update check failed: ${result.message}")
                }
                UpdateCheckResult.NoUpdate -> {
                    println("No updates available")
                }
            }
        }
    }
    
    AppTheme {
        when (currentScreen) {
            "login" -> LoginPage(
                onLoginSuccess = { currentScreen = "sales" }
            )
            "sales" -> SalesPage(
                userSession = userSession,
                onLogout = { 
                    userSession.logout()
                    currentScreen = "login" 
                }
            )
        }
        
        // Update dialogs
        if (showUpdateDialog && updateInfo != null) {
            UpdateDialog(
                updateInfo = updateInfo!!,
                onUpdateAccepted = {
                    showUpdateDialog = false
                    showUpdateProgress = true
                    coroutineScope.launch {
                        updateService.downloadAndInstallUpdate(updateInfo!!)
                        // App will exit after successful update
                    }
                },
                onUpdateDeclined = {
                    showUpdateDialog = false
                    updateInfo = null
                },
                onDismiss = {
                    if (!updateInfo!!.mandatory) {
                        showUpdateDialog = false
                        updateInfo = null
                    }
                }
            )
        }
        
        if (showUpdateProgress) {
            UpdateProgressDialog()
        }
    }
}



fun main() {
    // Initialize anti-tampering protection before starting the application
    try {
        AntiTamperingProtection.initialize()
        println("Security protection initialized successfully")
    } catch (e: Exception) {
        println("Failed to initialize security protection: ${e.message}")
        // Continue anyway for development, but in production this should exit
    }
    
    application {
        var showExitDialog by remember { mutableStateOf(false) }
    
    Window(
        onCloseRequest = { showExitDialog = true },
        title = "EZ Ledger",
        state = WindowState(placement = WindowPlacement.Maximized)
    ) {
        App()
        
        // Exit confirmation dialog
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text("Confirm Exit") },
                text = { Text("Are you sure you want to close the application?") },
                confirmButton = {
                    Button(
                        onClick = { 
                            // Cleanup security protection before exit
                            try {
                                AntiTamperingProtection.disableProtection()
                            } catch (e: Exception) {
                                // Silently continue with exit
                            }
                            exitProcess(0) 
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
                    ) {
                        Text("Yes, Exit", color = MaterialTheme.colors.onError)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showExitDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
    }
}