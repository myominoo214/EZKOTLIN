package ui.screens

import data.models.UserData
import data.models.UserFormData
import data.models.UserType
import data.models.BetType
import data.models.AgentOption
import data.models.FormValidationState
import core.services.ApiService
import presentation.viewmodels.UserFormViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.*
import io.ktor.client.HttpClient
import core.config.CompactOutlinedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserForm(
    merchantDetail: UserData? = null,
    formType: String = "Agent",
    onClose: () -> Unit = {},
    userApiService: ApiService = ApiService(),
    viewModel: UserFormViewModel
) {
    // Form state
    var formData by remember {
        mutableStateOf(
            merchantDetail?.let {
                UserFormData(
                    name = it.name ?: it.userName ?: "",
                    phoneNumber = "",
                    partner = false,
                    inviteKey = UUID.randomUUID().toString(),
                    agentId = "",
                    discount2D = "",
                    discount3D = "",
                    prize2D = "",
                    prize3D = "",
                    tPrize = "",
                    breakLimit2D = "",
                    breakLimit3D = "",
                    unitPrice = "",
                    hotBreak = "",
                    hotPercentage = "",
                    hotBreak3D = "",
                    hotPercentage3D = "",
                    betType = BetType.PERCENTAGE,
                    betType3D = BetType.PERCENTAGE,
                    access2D = true,
                    access3D = true,
                    host = false,
                    breakAccess = false,
                    hotAccess = false
                )
            } ?: UserFormData(inviteKey = UUID.randomUUID().toString())
        )
    }
    
    var selectedUserType by remember { mutableStateOf(UserType.AGENT) }
    var validationState by remember { mutableStateOf(FormValidationState()) }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedAgent by remember { mutableStateOf<AgentOption?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val uiState by viewModel.uiState.collectAsState()
    val agentOptions by viewModel.agentOptions.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    
    // Load agent options when user type is not Agent
    LaunchedEffect(selectedUserType) {
        if (selectedUserType != UserType.AGENT) {
            viewModel.loadAgentOptions()
        }
    }
    
    // Handle UI state changes
    LaunchedEffect(uiState.shouldClose) {
        if (uiState.shouldClose) {
            viewModel.resetShouldClose()
            onClose()
        }
    }
    
    // Show success/error messages
    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let {
            // Show success toast/snackbar
            viewModel.clearMessages()
        }
        uiState.errorMessage?.let {
            // Show error toast/snackbar
            viewModel.clearMessages()
        }
    }
    
    // Validation function
    fun validateForm(): Boolean {
        val errors = mutableMapOf<String, String?>()
        
        // Name validation
        if (formData.name.isBlank()) {
            errors["name"] = "Name is required"
        } else if (formData.name.length < 2) {
            errors["name"] = "Name must be at least 2 characters"
        }
        
        // Password validation
        if (formData.password.isBlank() && merchantDetail == null) {
            errors["password"] = "Password is required"
        } else if (formData.password.isNotBlank() && formData.password.length < 6) {
            errors["password"] = "Password must be at least 6 characters"
        }
        
        // Phone number validation
        if (formData.phoneNumber.isBlank()) {
            errors["phoneNumber"] = "Phone number is required"
        } else if (!formData.phoneNumber.matches(Regex("^[0-9+\\-\\s()]+$"))) {
            errors["phoneNumber"] = "Invalid phone number format"
        }
        
        // Agent selection validation
        if (selectedUserType != UserType.AGENT && formData.agentId.isNullOrBlank()) {
            errors["agentId"] = "Agent selection is required"
        }
        
        // Validate integer fields with range checks
        val integerFields = mapOf(
            "discount2D" to Triple(formData.discount2D, 0, 100),
            "discount3D" to Triple(formData.discount3D, 0, 100),
            "prize2D" to Triple(formData.prize2D, 0, Int.MAX_VALUE),
            "prize3D" to Triple(formData.prize3D, 0, Int.MAX_VALUE),
            "tPrize" to Triple(formData.tPrize, 0, Int.MAX_VALUE)
        )
        
        integerFields.forEach { (field, triple) ->
            val (value, min, max) = triple
            if (value.isNotBlank()) {
                val numValue = value.toIntOrNull()
                if (numValue == null) {
                    errors[field] = "Must be a valid integer"
                } else if (numValue < min) {
                    errors[field] = "Must be at least $min"
                } else if (numValue > max && max != Int.MAX_VALUE) {
                    errors[field] = "Must be at most $max"
                }
            }
        }
        
        // Validate double fields with range checks
        val doubleFields = mapOf(
            "breakLimit2D" to Triple(formData.breakLimit2D, 0.0, Double.MAX_VALUE),
            "breakLimit3D" to Triple(formData.breakLimit3D, 0.0, Double.MAX_VALUE),
            "unitPrice" to Triple(formData.unitPrice, 0.0, Double.MAX_VALUE)
        )
        
        doubleFields.forEach { (field, triple) ->
            val (value, min, max) = triple
            if (value.isNotBlank()) {
                val numValue = value.toDoubleOrNull()
                if (numValue == null) {
                    errors[field] = "Must be a valid number"
                } else if (numValue < min) {
                    errors[field] = "Must be at least $min"
                } else if (numValue > max && max != Double.MAX_VALUE) {
                    errors[field] = "Must be at most $max"
                }
            }
        }
        
        // Validate hot break/percentage fields (integers only)
        if (formData.betType == BetType.AMOUNT && formData.hotBreak.isNotBlank()) {
            val hotBreakValue = formData.hotBreak.toIntOrNull()
            if (hotBreakValue == null) {
                errors["hotBreak"] = "Must be a valid integer"
            } else if (hotBreakValue < 0) {
                errors["hotBreak"] = "Must be a positive integer"
            }
        }
        
        if (formData.betType == BetType.PERCENTAGE && formData.hotPercentage.isNotBlank()) {
            val hotPercentageValue = formData.hotPercentage.toIntOrNull()
            if (hotPercentageValue == null) {
                errors["hotPercentage"] = "Must be a valid integer"
            } else if (hotPercentageValue < 0 || hotPercentageValue > 100) {
                errors["hotPercentage"] = "Must be between 0 and 100"
            }
        }
        
        if (formData.betType3D == BetType.AMOUNT && formData.hotBreak3D.isNotBlank()) {
            val hotBreak3DValue = formData.hotBreak3D.toIntOrNull()
            if (hotBreak3DValue == null) {
                errors["hotBreak3D"] = "Must be a valid integer"
            } else if (hotBreak3DValue < 0) {
                errors["hotBreak3D"] = "Must be a positive integer"
            }
        }
        
        if (formData.betType3D == BetType.PERCENTAGE && formData.hotPercentage3D.isNotBlank()) {
            val hotPercentage3DValue = formData.hotPercentage3D.toIntOrNull()
            if (hotPercentage3DValue == null) {
                errors["hotPercentage3D"] = "Must be a valid integer"
            } else if (hotPercentage3DValue < 0 || hotPercentage3DValue > 100) {
                errors["hotPercentage3D"] = "Must be between 0 and 100"
            }
        }
        
        validationState = FormValidationState(
            nameError = errors["name"],
            passwordError = errors["password"],
            phoneNumberError = errors["phoneNumber"],
            agentIdError = errors["agentId"],
            discount2DError = errors["discount2D"],
            discount3DError = errors["discount3D"],
            prize2DError = errors["prize2D"],
            prize3DError = errors["prize3D"],
            tPrizeError = errors["tPrize"],
            breakLimit2DError = errors["breakLimit2D"],
            breakLimit3DError = errors["breakLimit3D"],
            unitPriceError = errors["unitPrice"],
            hotBreakError = errors["hotBreak"],
            hotPercentageError = errors["hotPercentage"],
            hotBreak3DError = errors["hotBreak3D"],
            hotPercentage3DError = errors["hotPercentage3D"]
        )
        
        return !validationState.hasErrors
    }
    
    // Handle form submission
    fun handleSubmit() {
        if (validateForm()) {
            if (merchantDetail != null) {
                viewModel.updateUser(merchantDetail.id?.toString() ?: "", formData)
            } else {
                viewModel.createUser(formData, selectedUserType)
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete this user?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        merchantDetail?.let { viewModel.deleteUser(it.id?.toString() ?: "") }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Column(
        modifier = Modifier
            .width(600.dp)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = if (merchantDetail != null) "Edit User" else "Create User",
            style = MaterialTheme.typography.headlineMedium
        )
        
        // User Type Selection (only for new users)
        if (merchantDetail == null) {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        UserType.values().forEach { userType ->
                            Row(
                                modifier = Modifier
                                    .selectable(
                                        selected = selectedUserType == userType,
                                        onClick = { selectedUserType = userType }
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedUserType == userType,
                                    onClick = { selectedUserType = userType }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (userType) {
                                        UserType.AGENT -> "Agent"
                                        UserType.USER -> "User"
                                        UserType.SUB_OWNER -> "Sub-owner"
                                        UserType.EMPLOYEE -> "Employee"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Basic Information
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name field
                CompactOutlinedTextField(
                    value = formData.name,
                    onValueChange = { formData = formData.copy(name = it) },
                    label = { Text("Name") },
                    isError = validationState.nameError != null,
                    
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Password field
                CompactOutlinedTextField(
                    value = formData.password,
                    onValueChange = { formData = formData.copy(password = it) },
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(if (passwordVisible) "👁" else "🙈")
                            }
                        },
                    isError = validationState.passwordError != null,
                    
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Phone number field
                CompactOutlinedTextField(
                    value = formData.phoneNumber,
                    onValueChange = { formData = formData.copy(phoneNumber = it) },
                    label = { Text("Phone Number") },
                    isError = validationState.phoneNumberError != null,
                    
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Partner checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = formData.partner,
                        onCheckedChange = { formData = formData.copy(partner = it) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Partner")
                }
                
                // Invite Key (read-only)
                if (formData.inviteKey.isNotBlank()) {
                    CompactOutlinedTextField(
                        value = formData.inviteKey,
                        onValueChange = { },
                        label = { Text("Invite Key") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(formData.inviteKey))
                                }
                            ) {
                                Text("📋")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Agent Selection (for non-agent users)
        if (selectedUserType != UserType.AGENT) {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Agent Selection",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    // Agent selection dropdown
                    var agentDropdownExpanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = agentDropdownExpanded,
                        onExpandedChange = { agentDropdownExpanded = !agentDropdownExpanded }
                    ) {
                        CompactOutlinedTextField(
                            value = selectedAgent?.label ?: "",
                            onValueChange = { },
                            label = { Text("Select Agent") },
                            readOnly = true,
                            isError = validationState.agentIdError != null,
                            
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = agentDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = agentDropdownExpanded,
                            onDismissRequest = { agentDropdownExpanded = false }
                        ) {
                            agentOptions.forEach { agent ->
                                DropdownMenuItem(
                                    text = { Text(agent.label) },
                                    onClick = {
                                        selectedAgent = agent
                                        formData = formData.copy(agentId = agent.value)
                                        agentDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Financial Information
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactOutlinedTextField(
                        value = formData.discount2D,
                        onValueChange = { formData = formData.copy(discount2D = it) },
                        label = { Text("2D Discount") },
                        isError = validationState.discount2DError != null,
                        
                        modifier = Modifier.weight(1f)
                    )
                    
                    CompactOutlinedTextField(
                        value = formData.discount3D,
                        onValueChange = { formData = formData.copy(discount3D = it) },
                        label = { Text("3D Discount") },
                        isError = validationState.discount3DError != null,
                        
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactOutlinedTextField(
                        value = formData.prize2D,
                        onValueChange = { formData = formData.copy(prize2D = it) },
                        label = { Text("2D Prize") },
                        isError = validationState.prize2DError != null,
                        
                        modifier = Modifier.weight(1f)
                    )
                    
                    CompactOutlinedTextField(
                        value = formData.prize3D,
                        onValueChange = { formData = formData.copy(prize3D = it) },
                        label = { Text("3D Prize") },
                        isError = validationState.prize3DError != null,
                        
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactOutlinedTextField(
                        value = formData.tPrize,
                        onValueChange = { formData = formData.copy(tPrize = it) },
                        label = { Text("T Prize") },
                        isError = validationState.tPrizeError != null,
                        
                        modifier = Modifier.weight(1f)
                    )
                    
                    CompactOutlinedTextField(
                        value = formData.unitPrice,
                        onValueChange = { formData = formData.copy(unitPrice = it) },
                        label = { Text("Unit Price") },
                        isError = validationState.unitPriceError != null,
                        
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactOutlinedTextField(
                        value = formData.breakLimit2D,
                        onValueChange = { formData = formData.copy(breakLimit2D = it) },
                        label = { Text("2D Break Limit") },
                        isError = validationState.breakLimit2DError != null,
                        
                        modifier = Modifier.weight(1f)
                    )
                    
                    CompactOutlinedTextField(
                        value = formData.breakLimit3D,
                        onValueChange = { formData = formData.copy(breakLimit3D = it) },
                        label = { Text("3D Break Limit") },
                        isError = validationState.breakLimit3DError != null,
                        
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Hot Break/Percentage Settings
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Hot Settings",
                    style = MaterialTheme.typography.titleMedium
                )
                
                // 2D Hot Break/Percentage
                Column {
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = formData.betType == BetType.AMOUNT,
                                onClick = { formData = formData.copy(betType = BetType.AMOUNT) }
                            )
                            Text("2D ဟော့ဘရိတ်")
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = formData.betType == BetType.PERCENTAGE,
                                onClick = { formData = formData.copy(betType = BetType.PERCENTAGE) }
                            )
                            Text("2D ဟော့%")
                        }
                    }
                    
                    if (formData.betType == BetType.AMOUNT) {
                        CompactOutlinedTextField(
                            value = formData.hotBreak,
                            onValueChange = { formData = formData.copy(hotBreak = it) },
                            label = { Text("Hot Break Amount") },
                            isError = validationState.hotBreakError != null,
                            
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        CompactOutlinedTextField(
                            value = formData.hotPercentage,
                            onValueChange = { formData = formData.copy(hotPercentage = it) },
                            label = { Text("Hot Percentage") },
                            isError = validationState.hotPercentageError != null,
                            
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 3D Hot Break/Percentage
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = formData.betType3D == BetType.AMOUNT,
                                onClick = { formData = formData.copy(betType3D = BetType.AMOUNT) }
                            )
                            Text("3D ဟော့ဘရိတ်")
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = formData.betType3D == BetType.PERCENTAGE,
                                onClick = { formData = formData.copy(betType3D = BetType.PERCENTAGE) }
                            )
                            Text("3D ဟော့%")
                        }
                    }
                    
                    if (formData.betType3D == BetType.AMOUNT) {
                        CompactOutlinedTextField(
                            value = formData.hotBreak3D,
                            onValueChange = { formData = formData.copy(hotBreak3D = it) },
                            label = { Text("Hot Break 3D Amount") },
                            isError = validationState.hotBreak3DError != null,
                            
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        CompactOutlinedTextField(
                            value = formData.hotPercentage3D,
                            onValueChange = { formData = formData.copy(hotPercentage3D = it) },
                            label = { Text("Hot Percentage 3D") },
                            isError = validationState.hotPercentage3DError != null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        
        // Access Controls
        if (merchantDetail != null) {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = formData.access2D,
                                onCheckedChange = { formData = formData.copy(access2D = it) }
                            )
                            Text("2D")
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = formData.access3D,
                                onCheckedChange = { formData = formData.copy(access3D = it) }
                            )
                            Text("3D")
                        }
                    }
                    
                    if (selectedUserType == UserType.EMPLOYEE) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = formData.host,
                                    onCheckedChange = { formData = formData.copy(host = it) }
                                )
                                Text("Host")
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = formData.breakAccess,
                                    onCheckedChange = { formData = formData.copy(breakAccess = it) }
                                )
                                Text("Break Access")
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = formData.hotAccess,
                                    onCheckedChange = { formData = formData.copy(hotAccess = it) }
                                )
                                Text("Hot Access")
                            }
                        }
                    }
                }
            }
        }
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            if (merchantDetail != null) {
                Button(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            }
            
            Button(
                onClick = ::handleSubmit,
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (merchantDetail != null) "Update" else "Submit")
            }
        }
    }
}