package ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import org.jetbrains.skia.Image as SkiaImage

data class UserProfile(
    val visionCount: Int = 0,
    val vision: Int = 0,
    val limit: Int = 0
)

// Helper function to read bytes from file
fun readBytesFromFile(file: File): ByteArray? {
    return try {
        file.readBytes()
    } catch (e: Exception) {
        null
    }
}

// Helper function to convert file to ImageBitmap
fun fileToImageBitmap(file: File): ImageBitmap? {
    return try {
        val skiaImage = SkiaImage.makeFromEncoded(file.readBytes())
        skiaImage.toComposeImageBitmap()
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TotalMessage(
    userProfile: UserProfile? = null,
    onSubmit: (String, String, String) -> Unit = { _, _, _ -> },
    onVisionApiCall: suspend (String) -> Result<String> = { Result.success("") },
    modifier: Modifier = Modifier,
    viewModel: TotalMessageViewModel = remember { TotalMessageViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Set user profile when component is first composed
    LaunchedEffect(userProfile) {
        userProfile?.let { viewModel.setUserProfile(it) }
    }
    
    // File picker function
    val openFileDialog = {
        val fileDialog = FileDialog(Frame(), "Select Image", FileDialog.LOAD)
        fileDialog.setFilenameFilter { _, name ->
            name.lowercase().endsWith(".jpg") || 
            name.lowercase().endsWith(".jpeg") || 
            name.lowercase().endsWith(".png")
        }
        fileDialog.isVisible = true
        
        if (fileDialog.file != null) {
            val file = File(fileDialog.directory, fileDialog.file)
            readBytesFromFile(file)?.let { bytes ->
                viewModel.uploadImage(bytes)
            }
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with title and vision count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "မက်ဆေ့ပေါင်း",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                uiState.userProfile?.let { profile ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Vision Count:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${profile.visionCount.takeIf { it > 0 } ?: profile.vision.takeIf { it > 0 } ?: profile.limit.takeIf { it > 0 } ?: "N/A"}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            
            // Image Upload Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable { 
                    if (uiState.uploadedImageBase64 == null) {
                        openFileDialog()
                    }
                },
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isDragOver) 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else 
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(
                    width = 2.dp,
                    color = if (uiState.isDragOver) 
                        MaterialTheme.colorScheme.primary
                    else 
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.uploadedImageBase64 != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // For desktop, we'll show a placeholder or implement custom image loading
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Image Uploaded",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                onClick = { 
                                    viewModel.removeImage()
                                    openFileDialog()
                                }
                            ) {
                                    Icon(
                                     Icons.Default.Edit,
                                     contentDescription = null,
                                     modifier = Modifier.size(16.dp)
                                 )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Change")
                                }
                                
                                Button(
                                            onClick = { 
                                                viewModel.extractTextFromImage(onVisionApiCall)
                                            },
                                            enabled = !uiState.isProcessing
                                        ) {
                                            if (uiState.isProcessing) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(
                                                 Icons.Default.Edit,
                                                 contentDescription = null,
                                                 modifier = Modifier.size(16.dp)
                                             )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (uiState.isProcessing) "Processing..." else "Get Text")
                                        }
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "📷",
                                fontSize = 48.sp
                            )
                            
                            Text(
                                text = "Drag and drop an image here, or",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Button(
                                onClick = { openFileDialog() }
                            ) {
                                Icon(
                                     Icons.Default.Add,
                                     contentDescription = null,
                                     modifier = Modifier.size(24.dp)
                                 )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose File")
                            }
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(50)
                                            )
                                    )
                                    Text(
                                        text = "JPG",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                MaterialTheme.colorScheme.secondary,
                                                RoundedCornerShape(50)
                                            )
                                    )
                                    Text(
                                        text = "PNG",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Radio Buttons
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Amount Selection",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        listOf(
                            "1" to "ငွေ",
                            "25" to "25",
                            "100" to "100"
                        ).forEach { (value, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.selectedAmount == value,
                                    onClick = { viewModel.updateSelectedAmount(value) }
                                )
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Find and Replace Section
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Find & Replace",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.findText,
                            onValueChange = { viewModel.updateFindText(it) },
                            label = { Text("Find") },
                            modifier = Modifier.weight(1f)
                        )
                        
                        OutlinedTextField(
                            value = uiState.replaceText,
                            onValueChange = { viewModel.updateReplaceText(it) },
                            label = { Text("Replace") },
                            modifier = Modifier.weight(1f)
                        )
                        
                        Button(
                            onClick = { viewModel.performFindReplace() }
                        ) {
                            Text("Replace")
                        }
                    }
                }
            }
            
            // Text Areas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.parseMessage,
                    onValueChange = { viewModel.updateParseMessage(it) },
                    label = { Text("Parse MSG") },
                    modifier = Modifier
                        .weight(1f)
                        .height(200.dp),
                    maxLines = 14
                )
                
                OutlinedTextField(
                    value = uiState.errorMessage,
                    onValueChange = { viewModel.updateErrorMessage(it) },
                    label = { Text("Error MSG") },
                    modifier = Modifier
                        .weight(1f)
                        .height(200.dp),
                    maxLines = 14,
                    isError = uiState.isError
                )
            }
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.resetForm() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("CANCEL")
                }
                
                Button(
                    onClick = { 
                        // Handle check SMS - could trigger validation
                        viewModel.validateAndSubmit()?.let { (_, _, _) ->
                            // Perform SMS check logic here
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Check SMS")
                }
                
                Button(
                    onClick = { 
                        viewModel.validateAndSubmit()?.let { (amount, parseMsg, errorMsg) ->
                            onSubmit(amount, parseMsg, errorMsg)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("OK")
                }
            }
        }
    }
}