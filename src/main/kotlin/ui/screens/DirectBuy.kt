import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import core.services.ApiResponse
import core.config.CompactOutlinedTextField

// Data Models
@Serializable
data class Partner(
    val pUserId: String,
    val name: String,
    val pBusinessId: String,
    val userId: String
)

@Serializable
data class DirectBuyTerm(
    val termId: String,
    val termName: String
)

// ApiResponse is defined in ApiService.kt

@Serializable
data class DirectBuyPayload(
    val termId: String,
    val userId: String,
    val pTermId: String,
    val pBusinessId: String,
    val pUserId: String,
    val ledger: String
)

@Serializable
data class InviteKeyPayload(
    val inviteKey: String
)

// UI State
data class DirectBuyUiState(
    val partners: List<Partner> = emptyList(),
    val terms: List<DirectBuyTerm> = emptyList(),
    val selectedPartner: Partner? = null,
    val selectedTerm: DirectBuyTerm? = null,
    val isLoading: Boolean = false,
    val showInviteModal: Boolean = false,
    val inviteKey: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

// ViewModel
class DirectBuyViewModel {
    private val _uiState = mutableStateOf(DirectBuyUiState())
    val uiState: State<DirectBuyUiState> = _uiState
    
    private val httpClient = HttpClient.newHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        fetchPartners()
    }
    
    fun fetchPartners() {
        coroutineScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create("/v1/account/getPartner"))
                    .GET()
                    .build()
                
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                val apiResponse = json.decodeFromString<ApiResponse<List<Partner>>>(response.body())
                
                if (apiResponse.success && apiResponse.data != null) {
                    _uiState.value = _uiState.value.copy(
                        partners = apiResponse.data,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to fetch partners",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error fetching partners: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    fun fetchTerms(userId: String, businessId: String) {
        coroutineScope.launch {
            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create("/v1/term/getLinkActiveTerms?userId=$userId&businessId=$businessId"))
                    .GET()
                    .build()
                
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                val apiResponse = json.decodeFromString<ApiResponse<List<DirectBuyTerm>>>(response.body())
                
                if (apiResponse.success && apiResponse.data != null) {
                    _uiState.value = _uiState.value.copy(terms = apiResponse.data)
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to fetch terms"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error fetching terms: ${e.message}"
                )
            }
        }
    }
    
    fun directBuy(parentTermId: String, ledger: String, onSuccess: () -> Unit) {
        val selectedPartner = _uiState.value.selectedPartner
        val selectedTerm = _uiState.value.selectedTerm
        
        coroutineScope.launch {
            if (selectedPartner == null || selectedTerm == null) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Please select both partner and term"
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val payload = DirectBuyPayload(
                    termId = parentTermId,
                    userId = selectedPartner.userId,
                    pTermId = selectedTerm.termId,
                    pBusinessId = selectedPartner.pBusinessId,
                    pUserId = selectedPartner.pUserId,
                    ledger = ledger
                )
                
                val requestBody = json.encodeToString(DirectBuyPayload.serializer(), payload)
                val request = HttpRequest.newBuilder()
                    .uri(URI.create("/v1/slip/addSlipsWithB2B"))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .header("Content-Type", "application/json")
                    .build()
                
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                val apiResponse = json.decodeFromString<ApiResponse<String>>(response.body())
                
                if (apiResponse.success) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = if (apiResponse.message?.contains("မရ") == true) {
                            "ရ\n${apiResponse.message}"
                        } else {
                            "ဂဏန်းများအားလုံးရပါသည် စလစ်ထဲတွင် စစ်ကြည့်ပေးပါ"
                        },
                        isLoading = false
                    )
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = apiResponse.message,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Direct Buy error: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    fun confirmInviteKey(onSuccess: () -> Unit) {
        coroutineScope.launch {
            try {
                val payload = InviteKeyPayload(_uiState.value.inviteKey)
                val requestBody = json.encodeToString(InviteKeyPayload.serializer(), payload)
                
                val request = HttpRequest.newBuilder()
                    .uri(URI.create("/v1/account/confirmPartner"))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .header("Content-Type", "application/json")
                    .build()
                
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                val apiResponse = json.decodeFromString<ApiResponse<String>>(response.body())
                
                if (apiResponse.success) {
                    fetchPartners()
                    resetTerms()
                    _uiState.value = _uiState.value.copy(
                        showInviteModal = false,
                        inviteKey = ""
                    )
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "InviteKey failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "InviteKey error: ${e.message}"
                )
            }
        }
    }
    
    fun selectPartner(partner: Partner) {
        _uiState.value = _uiState.value.copy(
            selectedPartner = partner,
            selectedTerm = null,
            terms = emptyList()
        )
        fetchTerms(partner.pUserId, partner.pBusinessId)
    }
    
    fun selectTerm(term: DirectBuyTerm) {
        _uiState.value = _uiState.value.copy(selectedTerm = term)
    }
    
    fun showInviteModal() {
        _uiState.value = _uiState.value.copy(showInviteModal = true)
    }
    
    fun hideInviteModal() {
        _uiState.value = _uiState.value.copy(
            showInviteModal = false,
            inviteKey = ""
        )
    }
    
    fun updateInviteKey(key: String) {
        _uiState.value = _uiState.value.copy(inviteKey = key)
    }
    
    fun resetTerms() {
        _uiState.value = _uiState.value.copy(
            terms = emptyList(),
            selectedTerm = null
        )
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}

// Main Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectBuy(
    parentTermId: String,
    ledger: String,
    onRefreshLedger: () -> Unit,
    viewModel: DirectBuyViewModel = remember { DirectBuyViewModel() }
) {
    val uiState by viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle error and success messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessages()
        }
    }
    
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
            viewModel.clearMessages()
        }
    }
    
    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Partner Selection Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Partner Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            CompactOutlinedTextField(
                                value = uiState.selectedPartner?.name ?: "",
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Select Partner") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                uiState.partners.forEach { partner ->
                                    DropdownMenuItem(
                                        text = { Text(partner.name) },
                                        onClick = {
                                            viewModel.selectPartner(partner)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Add Partner Button
                    FloatingActionButton(
                        onClick = { viewModel.showInviteModal() },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Partner"
                        )
                    }
                }
                
                // Term Selection
                Box(modifier = Modifier.fillMaxWidth()) {
                    var expanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        CompactOutlinedTextField(
                            value = uiState.selectedTerm?.termName ?: "",
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Select Term") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            enabled = uiState.terms.isNotEmpty()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            uiState.terms.forEach { term ->
                                DropdownMenuItem(
                                    text = { Text(term.termName) },
                                    onClick = {
                                        viewModel.selectTerm(term)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Direct Buy Button
                Button(
                    onClick = {
                        viewModel.directBuy(
                            parentTermId = parentTermId,
                            ledger = ledger,
                            onSuccess = onRefreshLedger
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading && 
                             uiState.selectedPartner != null && 
                             uiState.selectedTerm != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "Direct Buy",
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    
    // Invite Key Modal
    if (uiState.showInviteModal) {
        InviteKeyModal(
            inviteKey = uiState.inviteKey,
            onInviteKeyChange = viewModel::updateInviteKey,
            onConfirm = {
                viewModel.confirmInviteKey {
                    // Success callback handled in ViewModel
                }
            },
            onDismiss = viewModel::hideInviteModal
        )
    }
}

// Invite Key Modal Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteKeyModal(
    inviteKey: String,
    onInviteKeyChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Partner",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                
                // Invite Key Input
                CompactOutlinedTextField(
                    value = inviteKey,
                    onValueChange = onInviteKeyChange,
                    label = { Text("Invite Key") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Confirm Button
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = inviteKey.isNotBlank()
                ) {
                    Text(
                        text = "Confirm",
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}