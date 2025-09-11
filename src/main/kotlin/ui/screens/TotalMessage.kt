package ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.*
import core.services.UserSession
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import java.util.Base64


import core.stores.TempListStore
import core.stores.rememberTempListStore
import core.config.CompactOutlinedTextField

data class TotalLotteryEntry(
    val num: String,
    val unit: Int,
    val summary: String,
    val discount: Int = 0,
    val groupId: String,
    val groupId2: String,
    val delete: Boolean,
    val showSummary: Boolean
)

@Composable
fun ImageUploadSection(
    uploadedImage: String?,
    isDragOver: Boolean,
    isProcessing: Boolean,
    onImageSelected: (String?) -> Unit,
    onDragStateChanged: (Boolean) -> Unit,
    onGetText: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .border(
                width = 2.dp,
                color = if (isDragOver) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isDragOver) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surface
                ),
            contentAlignment = Alignment.Center
        ) {
            if (uploadedImage != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📷 Image uploaded",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onImageSelected(null) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Change")
                        }
                        Button(
                            onClick = onGetText,
                            enabled = !isProcessing
                        ) {
                            Text(if (isProcessing) "Processing..." else "Get Text")
                        }
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📷",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text = "Select an image file",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            val fileChooser = JFileChooser()
                            fileChooser.fileFilter = FileNameExtensionFilter(
                                "Image files", "jpg", "jpeg", "png"
                            )
                            val result = fileChooser.showOpenDialog(null)
                            if (result == JFileChooser.APPROVE_OPTION) {
                                val selectedFile = fileChooser.selectedFile
                                try {
                                    val imageBytes = selectedFile.readBytes()
                                    val base64String = Base64.getEncoder().encodeToString(imageBytes)
                                    onImageSelected(base64String)
                                } catch (e: Exception) {
                                    // Handle file reading error
                                    println("Error reading file: ${e.message}")
                                }
                            }
                        }
                    ) {
                        Text("Choose File")
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("• JPG", style = MaterialTheme.typography.bodySmall, color = Color.Green)
                        Text("• PNG", style = MaterialTheme.typography.bodySmall, color = Color.Blue)
                    }
                }
            }
        }
    }
}

@Composable
fun AmountSelectionSection(
    selectedAmount: String,
    onAmountChanged: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("1" to "ငွေ", "25" to "25", "100" to "100").forEach { (value, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.selectable(
                    selected = selectedAmount == value,
                    onClick = { onAmountChanged(value) }
                )
            ) {
                RadioButton(
                    selected = selectedAmount == value,
                    onClick = { onAmountChanged(value) }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun FindReplaceSection(
    input1: String,
    input2: String,
    onInput1Changed: (String) -> Unit,
    onInput2Changed: (String) -> Unit,
    onReplace: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactOutlinedTextField(
                value = input1,
                onValueChange = onInput1Changed,
                label = { Text("Find") },
             modifier = Modifier.weight(1f)
         )
         CompactOutlinedTextField(
                value = input2,
                onValueChange = onInput2Changed,
                label = { Text("Replace") },
             modifier = Modifier.weight(1f)
         )
        Button(
            onClick = onReplace,
            modifier = Modifier.height(56.dp)
        ) {
            Text("Replace")
        }
    }
}

@Composable
fun TextAreasSection(
    textarea1: String,
    textarea2: String,
    isError: Boolean,
    onTextarea1Changed: (String) -> Unit,
    onTextarea2Changed: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CompactOutlinedTextField(
            value = textarea1,
            onValueChange = onTextarea1Changed,
            label = { Text("Parse MSG") },
            modifier = Modifier
                .weight(1f)
                .height(200.dp)
        )
        CompactOutlinedTextField(
            value = textarea2,
            onValueChange = onTextarea2Changed,
            label = { Text("Error MSG") },
            modifier = Modifier
                .weight(1f)
                .height(200.dp),
            readOnly = true,
            isError = isError
        )
    }
}

@Composable
fun ActionButtonsSection(
    onCancel: () -> Unit,
    onCheck: () -> Unit,
    onSubmit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("CANCEL")
        }
        Button(
            onClick = onCheck,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            )
        ) {
            Text("Check SMS")
        }
        Button(
            onClick = onSubmit,
            modifier = Modifier.weight(1f)
        ) {
            Text("OK")
        }
    }
}

@Composable
fun TotalMessage(
    is2D: Boolean = true,
    unitPrice: Int = 0,
    onDismiss: () -> Unit = {},
    onSubmit: (List<TotalLotteryEntry>) -> Unit = {}
) {
    val tempListStore = rememberTempListStore()
    val coroutineScope = rememberCoroutineScope()
    var selectedAmount by remember { mutableStateOf("1") }
    var input1 by remember { mutableStateOf("") }
    var input2 by remember { mutableStateOf("") }
    var textarea1 by remember { mutableStateOf("") }
    var textarea2 by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isChecked by remember { mutableStateOf(false) }
    var list by remember { mutableStateOf<List<TotalLotteryEntry>>(emptyList()) }
    var resultList by remember { mutableStateOf<List<LotteryEntry>>(emptyList()) }
    var uploadedImage by remember { mutableStateOf<String?>(null) }
    var isDragOver by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    
    // Get visionCount from UserSession
    val userSession = UserSession.getInstance()
    val visionCount = userSession.userProfileData?.visionCount ?: 0
    
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "မက်ဆေ့ပေါင်း",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    
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
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = visionCount.toString(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Image Upload Section
                ImageUploadSection(
                    uploadedImage = uploadedImage,
                    isDragOver = isDragOver,
                    isProcessing = isProcessing,
                    onImageSelected = { uploadedImage = it },
                    onDragStateChanged = { isDragOver = it },
                    onGetText = {
                        if (uploadedImage != null) {
                            isProcessing = true
                            textarea2 = "Processing image..."
                            // Simulate API call - replace with actual implementation
                            // handleGetText(uploadedImage, textarea1, textarea2, isError, isProcessing)
                        } else {
                            textarea2 = "Please upload an image first"
                            isError = true
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Radio Buttons
                AmountSelectionSection(
                    selectedAmount = selectedAmount,
                    onAmountChanged = { selectedAmount = it }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Find/Replace Section
                FindReplaceSection(
                    input1 = input1,
                    input2 = input2,
                    onInput1Changed = { input1 = it },
                    onInput2Changed = { input2 = it },
                    onReplace = {
                        val updatedText = textarea1.replace(input1, input2)
                        textarea1 = updatedText
                        input1 = ""
                        input2 = ""
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Textareas
                TextAreasSection(
                    textarea1 = textarea1,
                    textarea2 = textarea2,
                    isError = isError,
                    onTextarea1Changed = { textarea1 = it },
                    onTextarea2Changed = { textarea2 = it }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action Buttons
                ActionButtonsSection(
                    onCancel = onDismiss,
                    onCheck = {
                         HelperFunctions.getSMS(
                             textarea1,
                             if (is2D) "2D" else "3D",
                             unitPrice.toInt(),
                             selectedAmount.toIntOrNull() ?: 1
                         ) { result, error ->
                            coroutineScope.launch {
                                println("[DEBUG] getSMS callback - resultList size: ${result.size}")
                                println("[DEBUG] getSMS callback - error: '$error'")
                                resultList = result
                                if (error.isNotEmpty()) {
                                    textarea2 = error
                                    isError = true
                                    isChecked = false
                                    println("[DEBUG] Error set - textarea2: '$textarea2', isError: $isError")
                                } else {
                                    textarea2 = ""
                                    isError = false
                                    isChecked = true
                                    println("[DEBUG] Success - textarea2 cleared, isChecked: $isChecked")
                                }
                                
                            }
                        }
                    },
                    onSubmit = {
                        if (resultList.isNotEmpty()) {
                                
                                
                                // Store resultList in templiststore
                                val tempListItems = resultList.map { entry ->
                                    TempListItem(
                                        number = entry.num,
                                        amount = entry.unit.toString(),
                                        summary = entry.summary,
                                        showSummary = "0",
                                        groupId = entry.groupId
                                    )
                                }
                                println("[DEBUG] Converting ${resultList.size} entries to TempListItems")
                                println("[DEBUG] TempListItems: $tempListItems")
                                tempListStore.addItems(tempListItems)
                                println("[DEBUG] After dispatch - TempListStore item count: ${tempListStore.getItemCount()}")
                                }
                        
                        // Save tempListStore data - data is already stored from resultList processing
                        // The tempListStore will persist the data automatically
                        println("[DEBUG] TempListStore saved with ${tempListStore.getItemCount()} items")
                        
                        isChecked = false
                        textarea1 = ""
                        input1 = ""
                        input2 = ""
                        onDismiss()
                    }
                )
            }
        }
    }
}