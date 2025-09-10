package ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import core.models.UpdateInfo

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onUpdateAccepted: () -> Unit,
    onUpdateDeclined: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = !updateInfo.mandatory,
            dismissOnClickOutside = !updateInfo.mandatory
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = if (updateInfo.mandatory) "Required Update Available" else "Update Available",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // Version info
                Text(
                    text = "Version ${updateInfo.latestVersion} is now available.",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                // Release notes (if available)
                updateInfo.releaseNotes?.let { notes ->
                    Column {
                        Text(
                            text = "What's New:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = notes,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .verticalScroll(rememberScrollState()),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                // Mandatory update warning
                if (updateInfo.mandatory) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "⚠️ This is a mandatory update and must be installed to continue using the application.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    if (!updateInfo.mandatory) {
                        OutlinedButton(
                            onClick = onUpdateDeclined
                        ) {
                            Text("Later")
                        }
                    }
                    
                    Button(
                        onClick = onUpdateAccepted
                    ) {
                        Text(if (updateInfo.mandatory) "Update Now" else "Update")
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateProgressDialog(
    onDismiss: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                
                Text(
                    text = "Downloading and Installing Update...",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Text(
                    text = "Please wait while the update is being installed. The application will restart automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}