package ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import core.services.ApiService
import core.services.UserSession
import core.services.PrinterService
import core.services.PrinterInfo
import core.config.CompactOutlinedTextField

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingContent(onLogout: () -> Unit = {}) {
    val userSession = UserSession.getInstance()
    val userProfile = userSession.userProfileData
    val settings = userSession.settingsData
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    
    // General Settings State
    var isPrintingEnabled by remember { mutableStateOf(settings?.enablePrintingBool ?: false) }
    var isSongEnabled by remember { mutableStateOf(settings?.enableSongBool ?: false) }
    var isTwoColumn by remember { mutableStateOf(settings?.isTwoColumnBool ?: false) }
    var showBusinessName by remember { mutableStateOf(settings?.showBusinessNameBool ?: false) }
    var showTermName by remember { mutableStateOf(settings?.showTermNameBool ?: false) }
    var showEmployeeName by remember { mutableStateOf(settings?.showEmployeeNameBool ?: false) }
    var showPrintTime by remember { mutableStateOf(settings?.showPrintTimeBool ?: false) }
    var printWidth by remember { mutableIntStateOf(userSession.localSettingsData.printWidth) }
    var footerText by remember { mutableStateOf(userSession.localSettingsData.footerText) }
    var fontSize by remember { mutableStateOf(userSession.localSettingsData.fontSize) }
    var selectedPrinter by remember { mutableStateOf(userSession.localSettingsData.selectedPrinter) }
    var isPrinterDropdownExpanded by remember { mutableStateOf(false) }
    var availablePrinters by remember { mutableStateOf<List<PrinterInfo>>(emptyList()) }
    var showSummary by remember { mutableStateOf(userSession.localSettingsData.showSummary) }
    
    // Password Change State
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    // Focus requesters for password fields
    val currentPasswordFocusRequester = remember { FocusRequester() }
    val newPasswordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }
    
    // Owner-specific State
    var marqueeText by remember { mutableStateOf("") }
    var notificationTitle by remember { mutableStateOf("") }
    var notificationMessage by remember { mutableStateOf("") }
    var phoneOne by remember { mutableStateOf(userProfile?.phoneNumber1 ?: "") }
    var phoneTwo by remember { mutableStateOf(userProfile?.phoneNumber2 ?: "") }
    var phoneThree by remember { mutableStateOf(userProfile?.phoneNumber3 ?: "") }
    var encryptionKey by remember { mutableStateOf("") }
    
    // Loading states
    var isLoading by remember { mutableStateOf(false) }
    
    // API Service instance
    val apiService = remember { ApiService() }
    
    // Functions
    suspend fun fetchMarqueeData() {
        try {
            // Fallback to API call
            val response = apiService.getAlertMessage()
             println("Marquee Response: ${response}")
            if (response.success && response.data?.data?.alertMessage != null) {
                marqueeText = response.data.data.alertMessage
                println("Marquee data loaded from API: ${response.data.data.alertMessage}")
            } else {
                println("Marquee data loaded from API: null")
            }
        } catch (e: Exception) {
            println("Error fetching marquee data: ${e.message}")
        }
    }
    
    suspend fun getEncryptionInfo() {
        try {
            val text = "${phoneOne}@#${phoneTwo}@#${phoneThree}"
            val request = ApiService.EncryptionRequest(text = text)
            val response = apiService.getEncryptionInfo(request)
            
            if (response.success) {
                encryptionKey = response.data ?: ""
                println("Encryption key updated successfully")
            } else {
                println("Failed to get encryption info: ${response.message}")
            }
        } catch (e: Exception) {
            println("Error getting encryption info: ${e.message}")
        }
    }
    
    // Load system printers and marquee data on component initialization
    LaunchedEffect(Unit) {
        try {
            val printers = PrinterService.getAvailablePrinters()
            availablePrinters = printers
            
            // Set default printer if available (only if no printer is already selected)
            if (selectedPrinter.isEmpty() || selectedPrinter == "Default Printer") {
                val defaultPrinter = printers.find { it.isDefault }
                if (defaultPrinter != null) {
                    selectedPrinter = defaultPrinter.name
                    userSession.updateSelectedPrinter(defaultPrinter.name)
                } else if (printers.isNotEmpty()) {
                    selectedPrinter = printers.first().name
                    userSession.updateSelectedPrinter(printers.first().name)
                }
            }
        } catch (e: Exception) {
            println("Error loading printers: ${e.message}")
            // Fallback to default printer list
            availablePrinters = listOf(
                PrinterInfo("Default Printer", "Available", true)
            )
            if (selectedPrinter.isEmpty()) {
                selectedPrinter = "Default Printer"
                userSession.updateSelectedPrinter("Default Printer")
            }
        }
        
        // Load marquee data
        fetchMarqueeData()
    }
    
    // Update phone numbers when user profile changes
    LaunchedEffect(userProfile) {
        userProfile?.let { profile ->
            phoneOne = profile.phoneNumber1 ?: ""
            phoneTwo = profile.phoneNumber2 ?: ""
            phoneThree = profile.phoneNumber3 ?: ""
        }
    }
    
    // Get encryption key when phone numbers change
    LaunchedEffect(phoneOne, phoneTwo, phoneThree) {
        if (phoneOne.isNotEmpty() || phoneTwo.isNotEmpty() || phoneThree.isNotEmpty()) {
            getEncryptionInfo()
        }
    }
    
    suspend fun saveSettings() {
        isLoading = true
        try {
            val userId = userProfile?.userId ?: return
            val request = ApiService.UpdateSettingsRequest(
                userId = userId,
                enablePrinting = if (isPrintingEnabled) "1" else "0",
                enableSong = if (isSongEnabled) "1" else "0",
                isTwoColumn = if (isTwoColumn) "1" else "0",
                printWidth = "${printWidth}MM",
                showBusinessName = if (showBusinessName) "1" else "0",
                showEmployeeName = if (showEmployeeName) "1" else "0",
                showTermName = if (showTermName) "1" else "0",
                showPrintTime = if (showPrintTime) "1" else "0"
            )
            val response = apiService.updateSettings(request)
            
            if (response.success) {
                println("Settings saved successfully")
            } else {
                println("Failed to save settings: ${response.message}")
            }
        } catch (e: Exception) {
            println("Error saving settings: ${e.message}")
        } finally {
            isLoading = false
        }
    }
    
    suspend fun changePassword() {
        if (newPassword != confirmPassword) {
            println("Passwords do not match")
            return
        }
        
        isLoading = true
        try {
            val userId = userProfile?.userId ?: return
            val request = ApiService.ChangePasswordRequest(
                userId = userId,
                oldPassword = currentPassword,
                newPassword = newPassword
            )
            val response = apiService.changePassword(request)
            println("Change Password Response: $response")
            
            // Parse the response data to check internal status
            if (response.success && response.data != null) {
                // Try to parse the JSON response data
                val responseData = response.data
                if (responseData.contains("\"code\":\"200\"")) {
                    println("Password changed successfully")
                    currentPassword = ""
                    newPassword = ""
                    confirmPassword = ""
                    
                    // Logout and redirect to login page
                    userSession.logout()
                    onLogout()
                } else {
                    // Extract error message from response
                    val errorMessage = if (responseData.contains("Update failed")) {
                        "Password update failed. Please check your current password."
                    } else {
                        "Failed to change password"
                    }
                    println("Error: $errorMessage")
                }
            } else {
                println("Failed to change password: ${response.message}")
            }
        } catch (e: Exception) {
            println("Error changing password: ${e.message}")
        } finally {
            isLoading = false
        }
    }
    
    suspend fun updateMarquee() {
        isLoading = true
        try {
            val userId = userProfile?.userId ?: return
            val request = ApiService.UpdateMarqueeRequest(
                userId = userId,
                alertMessage = marqueeText
            )
            val response = apiService.updateMarquee(request)
            println("Marquee Response: $response")
            if (response.success) {
                println("Marquee updated successfully")
            } else {
                println("Failed to update marquee: ${response.message}")
            }
        } catch (e: Exception) {
            println("Error updating marquee: ${e.message}")
        } finally {
            isLoading = false
        }
    }
    
    suspend fun sendNotification() {
        isLoading = true
        try {
            val request = ApiService.SendNotificationRequest(
                title = notificationTitle,
                description = notificationMessage
            )
            val response = apiService.sendNotification(request)
            println("Notification Response: $response")
            if (response.success) {
                println("Notification sent successfully")
                notificationTitle = ""
                notificationMessage = ""
            } else {
                println("Failed to send notification: ${response.message}")
            }
        } catch (e: Exception) {
            println("Error sending notification: ${e.message}")
        } finally {
            isLoading = false
        }
    }
    
    suspend fun updatePhoneNumbers() {
        isLoading = true
        try {
            val userId = userProfile?.userId ?: return
            val request = ApiService.UpdateUserRequest(
                userId = userId,
                phoneNumber1 = phoneOne,
                phoneNumber2 = phoneTwo,
                phoneNumber3 = phoneThree
            )
            val response = apiService.updateUser(request)
            println("Update User Response: $response")
            if (response.success) {
                println("Phone numbers updated successfully")
                
                // Call getUserProfile API to get updated profile data
                try {
                    val profileResponse = apiService.getUserProfile(userSession.getAuthHeaders())
                    if (profileResponse.success && profileResponse.data?.code == 200 && profileResponse.data?.data != null) {
                        userSession.setUserProfile(profileResponse.data.data)
                        println("User profile updated successfully after phone number change")
                    } else {
                        println("Failed to get updated user profile: ${profileResponse.data?.message}")
                    }
                } catch (e: Exception) {
                    println("Error getting updated user profile: ${e.message}")
                }
                
                // Update encryption key after phone numbers are updated
                getEncryptionInfo()
            } else {
                println("Failed to update phone numbers: ${response.message}")
            }
        } catch (e: Exception) {
            println("Error updating phone numbers: ${e.message}")
        } finally {
            isLoading = false
        }
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        
        // General Settings and Change Password Cards in same row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Settings Card
            Card(
                modifier = Modifier
                    .weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "General Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF374151),
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // General Settings Checkboxes in FlowRow
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Printing Enabled
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isPrintingEnabled,
                            onCheckedChange = { isPrintingEnabled = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Enable Printing", fontSize = 14.sp)
                    }
                    
                    // Song Enabled
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSongEnabled,
                            onCheckedChange = { isSongEnabled = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Enable Song", fontSize = 14.sp)
                    }
                    
                    // Two Column
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isTwoColumn,
                            onCheckedChange = { isTwoColumn = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Two Column Layout", fontSize = 14.sp)
                    }
                    
                    // Show Business Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showBusinessName,
                            onCheckedChange = { showBusinessName = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Show Business Name", fontSize = 14.sp)
                    }
                    
                    // Show Term Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showTermName,
                            onCheckedChange = { showTermName = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Show Terminal Name", fontSize = 14.sp)
                    }
                    
                    // Show Employee Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showEmployeeName,
                            onCheckedChange = { showEmployeeName = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Show Employee Name", fontSize = 14.sp)
                    }
                    
                    // Show Print Time
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showPrintTime,
                            onCheckedChange = { showPrintTime = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Show Print Time", fontSize = 14.sp)
                    }
                }
                
                // Printer Setting Section
                Text(
                    text = "Printer Setting",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF374151),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Footer Text
                        CompactOutlinedTextField(
                            value = footerText,
                            onValueChange = { 
                                footerText = it
                                userSession.updateFooterText(it)
                            },
                            label = { Text("Footer Text") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )
                        
                        // Font Size
                        CompactOutlinedTextField(
                            value = fontSize,
                            onValueChange = { 
                                fontSize = it
                                userSession.updateFontSize(it)
                            },
                            label = { Text("Print Font Size") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        
                        // Printer List Dropdown
                        ExposedDropdownMenuBox(
                            expanded = isPrinterDropdownExpanded,
                            onExpandedChange = { isPrinterDropdownExpanded = !isPrinterDropdownExpanded },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            CompactOutlinedTextField(
                                value = selectedPrinter,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Select Printer") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = isPrinterDropdownExpanded
                                    )
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = isPrinterDropdownExpanded,
                                onDismissRequest = { isPrinterDropdownExpanded = false }
                            ) {
                                availablePrinters.forEach { printer ->
                                    DropdownMenuItem(
                                        text = { 
                                            Column {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = printer.name,
                                                        fontWeight = if (printer.isDefault) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                    if (printer.isDefault) {
                                                        Text(
                                                            text = "(Default)",
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "Status: ${printer.status}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedPrinter = printer.name
                                            userSession.updateSelectedPrinter(printer.name)
                                            isPrinterDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Page Size Options in FlowRow
                        FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 58mm Radio Button
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = printWidth == 58,
                                onClick = { 
                                    printWidth = 58
                                    userSession.updatePrintWidth(58)
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = printWidth == 58,
                            onClick = { 
                                printWidth = 58
                                userSession.updatePrintWidth(58)
                            }
                        )
                        Text("58mm", modifier = Modifier.padding(start = 4.dp), fontSize = 14.sp)
                    }
                    
                    // 80mm Radio Button
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = printWidth == 80,
                                onClick = { 
                                    printWidth = 80
                                    userSession.updatePrintWidth(80)
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = printWidth == 80,
                            onClick = { 
                                printWidth = 80
                                userSession.updatePrintWidth(80)
                            }
                        )
                        Text("80mm", modifier = Modifier.padding(start = 4.dp), fontSize = 14.sp)
                    }
                    
                    // Show Summary Checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showSummary,
                            onCheckedChange = { 
                                showSummary = it
                                userSession.updateShowSummary(it)
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Show Summary", fontSize = 14.sp)
                    }
                }
                    }
                }
                
                // Save Button
                Button(
                    onClick = {
                        scope.launch {
                            saveSettings()
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
                    Text("Save", color = Color.White)
                }
            }
        }
        
            // Password Change Card
            Card(
                modifier = Modifier
                    .weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Change Password",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF374151),
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                CompactOutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { newPasswordFocusRequester.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(currentPasswordFocusRequester)
                        .padding(bottom = 16.dp)
                )
                
                CompactOutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { confirmPasswordFocusRequester.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(newPasswordFocusRequester)
                        .padding(bottom = 16.dp)
                )
                
                CompactOutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            scope.launch {
                                changePassword()
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(confirmPasswordFocusRequester)
                        .padding(bottom = 24.dp)
                )
                
                Button(
                    onClick = {
                        scope.launch {
                            changePassword()
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
                    Text("Change Password", color = Color.White)
                }
            }
        }
        }
        
        // Owner-specific sections
        if (userProfile?.userType == "owner") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Marquee and Notification Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "စာတန်းပြေး ပြရန်",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF374151),
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        
                        CompactOutlinedTextField(
                            value = marqueeText,
                            onValueChange = { marqueeText = it },
                            label = { Text("Enter marquee text") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .padding(bottom = 16.dp)
                        )
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    updateMarquee()
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981)
                            )
                        ) {
                            Text("SAVE", color = Color.White)
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Notification",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF374151),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        CompactOutlinedTextField(
                            value = notificationTitle,
                            onValueChange = { notificationTitle = it },
                            label = { Text("Enter notification title") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )
                        
                        CompactOutlinedTextField(
                            value = notificationMessage,
                            onValueChange = { notificationMessage = it },
                            label = { Text("Enter notification message") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .padding(bottom = 16.dp)
                        )
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    sendNotification()
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981)
                            )
                        ) {
                            Text("Send Notification", color = Color.White)
                        }
                    }
                }
                
                // Phone Numbers and Encryption Key Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Change Phone Number",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF374151),
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        
                        CompactOutlinedTextField(
                            value = phoneOne,
                            onValueChange = { phoneOne = it },
                            label = { Text("Phone 1") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )
                        
                        CompactOutlinedTextField(
                            value = phoneTwo,
                            onValueChange = { phoneTwo = it },
                            label = { Text("Phone 2") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )
                        
                        CompactOutlinedTextField(
                            value = phoneThree,
                            onValueChange = { phoneThree = it },
                            label = { Text("Phone 3") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    updatePhoneNumbers()
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981)
                            )
                        ) {
                            Text("Change Phone", color = Color.White)
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "Key For SMS Offline",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF374151),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Box {
                            CompactOutlinedTextField(
                                value = encryptionKey,
                                onValueChange = { },
                                label = { Text("Offline Key") },
                                enabled = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                            )
                            
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(encryptionKey))
                                    println("Copied to clipboard!")
                                },
                                modifier = Modifier.align(Alignment.BottomEnd)
                            ) {
                                Icon(
                                    painter = painterResource("copy_icon.svg"),
                                    contentDescription = "Copy",
                                    tint = Color(0xFF6B7280)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}