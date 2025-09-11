package ui.screens
import core.utils.AudioPlayer

import data.models.UserOption
// HotNumber component will be implemented inline
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import core.config.CompactOutlinedTextField
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
// import androidx.compose.ui.tooling.preview.Preview // Not available in desktop Compose
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import androidx.lifecycle.ViewModel // Using custom ViewModel for desktop
// import androidx.lifecycle.viewModelScope // Using custom scope for desktop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.random.Random
import ui.screens.TotalMessage
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import core.services.ApiService
import core.services.UserSession
import core.stores.TempListStore
import core.stores.rememberTempListStore
import core.stores.SelectionStore
import core.stores.rememberSelectionStore
import core.stores.LedgerStore
// Components are in the same package, no need for explicit imports
// Removed HotNumber imports due to retrofit2 dependency issues

// Data Models
@Serializable
data class SaleTerm(
    val termId: String,
    val termName: String,
    val unitPrice: Double,
    val breakAmount: Double,
    val is2D: String,
    val termType: String,
    val shareOption: String? = null
)

@Serializable
data class UserProfile(
    val businessName: String,
    val name: String,
    val userType: String
)

@Serializable
data class BettedUnitsResponse(
    val totalUnits: Int
)

@Serializable
data class ApiResponse<T>(
    val code: String,
    val message: String,
    val data: T
)

@Serializable
data class NestedApiResponse<T>(
    val code: String,
    val message: String,
    val data: NestedData<T>
)

@Serializable
data class NestedData<T>(
    val by: T
)

@Serializable
data class SlipResponse(
    val slipId: String,
    val ledger: List<LedgerEntry>
)

@Serializable
data class LedgerEntry(
    val number: String,
    val amount: Int,
    val summary: String = "",
    val showSummary: String = "",
    val groupId: Int = 0,
    val delete: Boolean = false
)

data class TermOption(
    val value: String,
    val label: String
)

data class SalePageState(
    // selectedTerm and selectedUser moved to global SelectionStore
    val termOptions: List<TermOption> = emptyList(),
    val userOptions: List<UserOption> = emptyList(),
    val businessName: String = "",
    val employeeName: String = "",
    val bettedUnits: String = "0",
    val totalUnit: Int = 0,
    val breakAmount: Double = 0.0,
    val unitPrice: Double = 0.0,
    val is2D: Boolean = true,
    val sendSMS: Boolean = false,
    val isPrintingEnabled: Boolean = false,
    val isAllowExtra: Boolean = false,
    val connectStatus: Boolean = false,
    val isLoading: Boolean = false,
    val apiCalling: Boolean = false,
    val numberInput: String? = null,
    val userProfile: UserProfile? = null,
    val apiUserData: List<ApiUserData> = emptyList(),
    val userList: List<User> = emptyList(),
    val activeTermData: List<SaleTerm> = emptyList(),
    val list: List<LedgerEntry> = emptyList(),
    val modalMode: String? = null,
    val isConfirmModalOpen: Boolean = false,
    val pendingTerm: TermOption? = null,
    val termOpen: Boolean = false,
    val userOpen: Boolean = false,
    val inputValue: String = "",
    val termSearchQuery: String = "",
    val filteredTermOptions: List<TermOption> = emptyList(),
    val selectedTermIndex: Int = -1,
    val isBetAndReminingModalOpen: Boolean = false,
    val betAndReminingNumber: String = "",
    val successMessage: String? = null,
    val showViewModal: Boolean = false,
    val showOptionsModal: Boolean = false
)

// Note: SaleApiService interface removed - using ApiService directly

@Serializable
data class SlipPayload(
    val termId: String,
    val customer: String,
    val status: String,
    val ledger: List<LedgerEntry>,
    val userId: String,
    val deviceName: String,
    val userType: String
)

// Note: MockApiService removed - using ApiService directly with fallback data

// Simplified HotNumber data models
data class SimpleHotNumber(
    val id: String,
    val number: String,
    val termId: String,
    val is2D: Boolean
)

data class HotNumberState(
    val hotNumbers: List<SimpleHotNumber> = emptyList(),
    val inputHotNumber: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

// Hot number API data classes
@Serializable
data class HotNumberData(
    val number: String,
    val createdAt: String? = null
)

@Serializable
data class HotNumberApiData(
    val id: String? = null,
    val number: String,
    val termId: String? = null,
    val is2D: Boolean? = null,
    val createdAt: String? = null
)

@Serializable
data class HotNumberApiRequest(
    val termId: String,
    val number: String,
    val type: String // "2d" or "3d"
)

@Serializable
data class HotNumberApiResponse(
    val code: String,
    val message: String? = null,
    val data: HotNumberResponseData? = null
)

@Serializable
data class HotNumberResponseData(
    val code: String,
    val message: String? = null,
    val by: List<HotNumberData>? = null
)

class SimpleHotNumberViewModel(private val termId: String, private val is2D: Boolean) {
    private val _state = MutableStateFlow(HotNumberState())
    val state: StateFlow<HotNumberState> = _state.asStateFlow()
    
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    init {
        loadHotNumbers()
    }
    
    private fun loadHotNumbers() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val apiService = ApiService()
                val userSession = UserSession.getInstance()
                val type = if (is2D) "2d" else "3d"
                
                println("[DEBUG] SimpleHotNumberViewModel - Fetching hot numbers - termId: $termId, is2D: $is2D, type: $type")
                
                val response = apiService.get<NestedApiResponse<List<HotNumberData>>>(
                    url = "${ApiService.BASE_URL}/v1/ledger/getHotNumbers?termId=$termId&type=$type",
                    headers = userSession.getAuthHeaders()
                )
                
                println("[DEBUG] SimpleHotNumberViewModel - API response - code: ${response.success}")
                println("[DEBUG] SimpleHotNumberViewModel - API response - data: ${response.data}")
                
                if (response.success && response.data?.data?.by != null) {
                    val rawData = response.data.data.by
                    println("[DEBUG] SimpleHotNumberViewModel - Raw hot numbers data: $rawData")
                    println("[DEBUG] SimpleHotNumberViewModel - Raw data size: ${rawData.size}")
                    
                    val hotNumbers = rawData.map { hotNumberData ->
                        SimpleHotNumber(
                            id = "${termId}_${hotNumberData.number}", // Create unique ID using termId and number
                            number = hotNumberData.number,
                            termId = termId,
                            is2D = is2D
                        )
                    }
                    
                    println("[DEBUG] SimpleHotNumberViewModel - Mapped hot numbers: $hotNumbers")
                    println("[DEBUG] SimpleHotNumberViewModel - Mapped hot numbers size: ${hotNumbers.size}")
                    
                    _state.value = _state.value.copy(
                        hotNumbers = hotNumbers,
                        loading = false
                    )
                } else {
                    val errorMsg = response.data?.message ?: "Failed to fetch hot numbers"
                    println("[DEBUG] SimpleHotNumberViewModel - API error: $errorMsg")
                    _state.value = _state.value.copy(
                        error = errorMsg,
                        loading = false
                    )
                }
            } catch (e: Exception) {
                val errorMsg = "Error: ${e.message}"
                println("[DEBUG] SimpleHotNumberViewModel - Exception: $errorMsg")
                _state.value = _state.value.copy(
                    error = errorMsg,
                    loading = false
                )
            }
        }
    }
    
    fun updateInputHotNumber(value: String) {
        val filteredValue = value.filter { it.isDigit() }
        val maxLength = if (is2D) 2 else 3
        if (filteredValue.length <= maxLength) {
            _state.value = _state.value.copy(inputHotNumber = filteredValue, error = null)
        }
    }
    
    fun addHotNumber() {
        val currentState = _state.value
        val number = currentState.inputHotNumber
        
        if (isValidHotNumber(number)) {
            _state.value = currentState.copy(loading = true, error = null)
            
            viewModelScope.launch {
                try {
                    val apiService = ApiService()
                    val userSession = UserSession.getInstance()
                    val type = if (is2D) "2d" else "3d"
                    
                    val request = HotNumberApiRequest(
                        termId = termId,
                        number = number,
                        type = type
                    )
                    
                    println("[DEBUG] SimpleHotNumberViewModel - Adding hot number: $request")
                    
                    val response = apiService.post<HotNumberApiRequest, HotNumberApiResponse>(
                        url = "${ApiService.BASE_URL}/v1/ledger/addHotNumber",
                        body = request,
                        headers = userSession.getAuthHeaders()
                    )
                    
                    println("[DEBUG] SimpleHotNumberViewModel - Add response: $response")
                    
                    if (response.success && response.data?.code == "200") {
                        _state.value = currentState.copy(
                            inputHotNumber = "",
                            loading = false
                        )
                        // Refresh the list
                        loadHotNumbers()
                    } else {
                        val errorMsg = response.data?.message ?: "Failed to add hot number"
                        _state.value = currentState.copy(
                            loading = false,
                            error = errorMsg
                        )
                    }
                } catch (e: Exception) {
                    _state.value = currentState.copy(
                        loading = false,
                        error = "Failed to add hot number: ${e.message}"
                    )
                }
            }
        } else {
            _state.value = currentState.copy(
                error = if (is2D) "Please enter exactly 2 digits" else "Please enter exactly 3 digits"
            )
        }
    }
    
    fun deleteHotNumber(id: String) {
        val currentState = _state.value
        val hotNumber = currentState.hotNumbers.find { it.id == id }
        
        if (hotNumber != null) {
            viewModelScope.launch {
                _state.value = currentState.copy(loading = true, error = null)
                try {
                    val apiService = ApiService()
                    val userSession = UserSession.getInstance()
                    val type = if (is2D) "2d" else "3d"
                    
                    val request = HotNumberApiRequest(
                        termId = termId,
                        number = hotNumber.number,
                        type = type
                    )
                    
                    println("[DEBUG] SimpleHotNumberViewModel - Deleting hot number: $request")
                    
                    val response = apiService.deleteWithBody<HotNumberApiRequest, HotNumberApiResponse>(
                        url = "${ApiService.BASE_URL}/v1/ledger/deleteHotNumber",
                        body = request,
                        headers = userSession.getAuthHeaders()
                    )
                    
                    println("[DEBUG] SimpleHotNumberViewModel - Delete response: $response")
                    
                    if (response.success && response.data?.code == "200") {
                        // Refresh the list
                        loadHotNumbers()
                    } else {
                        val errorMsg = response.data?.message ?: "Failed to delete hot number"
                        _state.value = currentState.copy(
                            loading = false,
                            error = errorMsg
                        )
                    }
                } catch (e: Exception) {
                    _state.value = currentState.copy(
                        loading = false,
                        error = "Failed to delete hot number: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun isValidHotNumber(number: String): Boolean {
        return if (is2D) {
            number.length == 2 && number.all { it.isDigit() }
        } else {
            number.length == 3 && number.all { it.isDigit() }
        }
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
    
    fun refreshHotNumbers() {
        println("[DEBUG] SimpleHotNumberViewModel - Refreshing hot numbers for termId: $termId, is2D: $is2D")
        loadHotNumbers()
    }
}

@Composable
fun SimpleHotNumberComponent(
    viewModel: SimpleHotNumberViewModel,
    is2D: Boolean,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    
    Column(
        modifier = modifier.padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Input Section
        Column(
            modifier = Modifier.padding(2.dp)
        ) {
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CompactOutlinedTextField(
                    value = state.inputHotNumber,
                    onValueChange = viewModel::updateInputHotNumber,
                    label = { Text("နံပါတ်") },
                    singleLine = true,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.error != null
                )
                
                Button(
                    onClick = { viewModel.addHotNumber() },
                    enabled = !state.loading && state.inputHotNumber.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Add")
                    }
                }
            }
            
            // Error message
            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        // Hot Numbers List
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(2.dp)
        ) {
            
            if (state.hotNumbers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hot numbers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(state.hotNumbers) { index, hotNumber ->
                        val backgroundColor = if (index % 2 == 0) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        }
                        SimpleHotNumberRow(
                            hotNumber = hotNumber,
                            onDelete = { viewModel.deleteHotNumber(hotNumber.id) },
                            backgroundColor = backgroundColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleHotNumberRow(
    hotNumber: SimpleHotNumber,
    onDelete: () -> Unit,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = hotNumber.number,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// RealApiService class removed - using ApiService directly

// ViewModel
class SalePageViewModel(
    private val apiService: ApiService = ApiService()
) {
    private val _state = MutableStateFlow(SalePageState())
    val state: StateFlow<SalePageState> = _state.asStateFlow()

    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val selectionStore = SelectionStore.getInstance()
    
    // Reference to hot number view model for refreshing
    private var hotNumberViewModel: SimpleHotNumberViewModel? = null

    val termFocusRequester = FocusRequester()
    val userFocusRequester = FocusRequester()
    
    init {
        loadInitialData()
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            try {
                // Load user profile from UserSession instead of API call
                val userSession = UserSession.getInstance()
                userSession.userProfileData?.let { profileData ->
                    val userProfile = UserProfile(
                        businessName = profileData.businessName ?: "",
                        name = profileData.name ?: "",
                        userType = profileData.userType ?: ""
                    )
                    _state.value = _state.value.copy(
                        userProfile = userProfile,
                        businessName = profileData.businessName ?: "",
                        employeeName = profileData.name ?: ""
                    )
                }
                
                // Load sendSMS, isPrintingEnabled, and isAllowExtra state from persistent storage
                val localSettings = userSession.localSettingsData
                _state.value = _state.value.copy(
                    sendSMS = localSettings.sendSMS,
                    isPrintingEnabled = localSettings.isPrintingEnabled,
                    isAllowExtra = localSettings.isAllowExtra
                )
                
                // Load terms
                val termsResponse = apiService.getActiveTerms(1, 100)
                if (termsResponse.code == "200") {
                    initializeTermData(termsResponse.data)
                }
                
                // Load users
                val usersResponse = apiService.getUserLists(1, 100)
                if (usersResponse.code == "200") {
                    val userOptions = usersResponse.data.map { user ->
                        UserOption(
                            value = user.userId,
                            label = formatUserName(user.name, user.userType)
                        )
                    } // Close HotNumber Column
                    // Convert user data to ApiUserData format
                    val apiUserData = usersResponse.data.map { user ->
                        ApiUserData(
                            userId = user.userId,
                            discount2D = user.discount2D.toDouble() / 100.0, // Convert from percentage to decimal
                            discount3D = user.discount3D.toDouble() / 100.0  // Convert from percentage to decimal
                        )
                    }
                    // Update global state store with user options
                    selectionStore.setUserOptions(userOptions)
                    
                    _state.value = _state.value.copy(
                        userOptions = userOptions,
                        userList = usersResponse.data,
                        apiUserData = apiUserData
                        // Remove selectedUser auto-selection - let global store handle it
                    )
                }
                
            } catch (e: Exception) {
                // Handle error
                println("Error loading initial data: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }
    
    private fun initializeTermData(terms: List<SaleTerm>) {
        val termOptions = terms.map { term ->
            TermOption(
                value = term.termId,
                label = term.termName
            )
        }
        
        // Update global state store with term options
        selectionStore.setTermOptions(termOptions)
        
        // Update state with term data FIRST
        _state.value = _state.value.copy(
            termOptions = termOptions,
            filteredTermOptions = termOptions,
            activeTermData = terms
            // Remove selectedTerm - let global store handle it
        )
        
        // Auto-select first term if no term is currently selected
        if (selectionStore.getSelectedTerm() == null && termOptions.isNotEmpty()) {
            println("[DEBUG] No term selected, auto-selecting first term: ${termOptions.first().label}")
            updateSelectedTerm(termOptions.first())
        } else {
            // If term is already selected, restore its state to SalePage
            selectionStore.getSelectedTerm()?.let { selectedTerm ->
                println("[DEBUG] Restoring selected term state: ${selectedTerm.label}")
                val term = terms.find { it.termId == selectedTerm.value }
                if (term != null) {
                    _state.value = _state.value.copy(
                        is2D = term.is2D == "1",
                        breakAmount = term.breakAmount,
                        unitPrice = term.unitPrice,
                        numberInput = if (term.is2D == "1") "2D" else "3D"
                    )
                    println("[DEBUG] Restored breakAmount: ${term.breakAmount}")
                }
            }
            println("[DEBUG] Term already selected: ${selectionStore.getSelectedTerm()?.label ?: "none"} or no terms available")
        }
    }
    
    private fun formatUserName(name: String, userType: String): String {
        return "$name - $userType"
    }
    
    fun refreshData() {
        loadInitialData()
        // Also refresh hot numbers if available
        hotNumberViewModel?.refreshHotNumbers()
        
        // Refresh ledger data
        viewModelScope.launch {
            try {
                val selectedTerm = selectionStore.getSelectedTerm()
                if (selectedTerm != null) {
                    val ledgerStore = LedgerStore.getInstance()
                    val result = ledgerStore.refreshLedgerData(
                        termId = selectedTerm.value.toString(),
                        is2D = true
                    )
                    if (result.isSuccess) {
                        println("[DEBUG] SalePageViewModel - Ledger data refreshed successfully")
                    } else {
                        println("[DEBUG] SalePageViewModel - Failed to refresh ledger data: ${result.exceptionOrNull()?.message}")
                    }
                } else {
                    println("[DEBUG] SalePageViewModel - No selected term, skipping ledger refresh")
                }
            } catch (e: Exception) {
                println("[DEBUG] SalePageViewModel - Exception during ledger refresh: ${e.message}")
            }
        }
        
        println("[DEBUG] SalePageViewModel - refreshData called, hot numbers and ledgers refreshed")
    }
    
    fun setHotNumberViewModel(viewModel: SimpleHotNumberViewModel?) {
        hotNumberViewModel = viewModel
        println("[DEBUG] SalePageViewModel - Hot number view model reference set: ${viewModel != null}")
    }
    
    fun updateSelectedTerm(termOption: TermOption) {
        println("[DEBUG] updateSelectedTerm called with: ${termOption.label}")
        val term = _state.value.activeTermData.find { it.termId == termOption.value }
        println("[DEBUG] Found term in activeTermData: ${term != null}")
        println("[DEBUG] activeTermData size: ${_state.value.activeTermData.size}")
        
        // Check if tempListStore has data and is2D is different
        val tempListStore = TempListStore.getInstance()
        val hasData = tempListStore.getItemCount() > 0
        val currentIs2D = _state.value.is2D
        val newIs2D = term?.is2D == "1"
        
        println("[DEBUG] TempListStore has data: $hasData, current is2D: $currentIs2D, new is2D: $newIs2D")
        
        if (hasData && currentIs2D != newIs2D) {
            // Show confirmation dialog
            println("[DEBUG] Showing confirmation dialog for term change")
            _state.value = _state.value.copy(
                modalMode = "confirm_term_change",
                pendingTerm = termOption
            )
            return
        }
        
        // Proceed with term change
        proceedWithTermChange(termOption)
    }
    
    private fun proceedWithTermChange(termOption: TermOption) {
        println("[DEBUG] Proceeding with term change: ${termOption.label}")
        val term = _state.value.activeTermData.find { it.termId == termOption.value }
        
        if (term != null) {
            println("[DEBUG] Setting selected term in store: ${termOption.label}")
            // Update global state store
            selectionStore.setSelectedTerm(termOption)
            
            _state.value = _state.value.copy(
                is2D = term.is2D == "1",
                breakAmount = term.breakAmount,
                unitPrice = term.unitPrice,
                numberInput = if (term.is2D == "1") "2D" else "3D",
                termOpen = false
            )
            
            // Fetch betted units if user is selected
            selectionStore.getSelectedUser()?.let { user ->
                fetchBettedUnits(termOption.value, user.value)
            }
        } else {
            println("[DEBUG] Term not found in activeTermData, but setting in store anyway")
            // Still update the selection store even if term details aren't available yet
            selectionStore.setSelectedTerm(termOption)
        }
    }
    
    fun confirmTermChange() {
        println("[DEBUG] Term change confirmed")
        _state.value.pendingTerm?.let { termOption ->
            // Clear tempListStore data
            val tempListStore = TempListStore.getInstance()
            tempListStore.clearList()
            println("[DEBUG] TempListStore cleared")
            
            // Proceed with term change
            proceedWithTermChange(termOption)
            
            // Close modal
            _state.value = _state.value.copy(
                modalMode = null,
                pendingTerm = null
            )
        }
    }
    
    fun updateSelectedUser(userOption: UserOption) {
        // Update global state store
        selectionStore.setSelectedUser(userOption)
        
        _state.value = _state.value.copy(
            userOpen = false,
            numberInput = if (_state.value.is2D) "2D" else "3D"
        )
        
        // Fetch betted units
        selectionStore.getSelectedTerm()?.let { term ->
            fetchBettedUnits(term.value, userOption.value)
        }
    }
    
    private fun fetchBettedUnits(termId: String, userId: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getBettedTotalUnits(termId, userId)
                if (response.code == "200") {
                    _state.value = _state.value.copy(
                        bettedUnits = response.data.totalUnits.toString()
                    )
                }
            } catch (e: Exception) {
                println("Error fetching betted units: ${e.message}")
            }
        }
    }
    
    fun updateTotalUnit(total: Int) {
        _state.value = _state.value.copy(totalUnit = total)
    }
    
    fun updateList(newList: List<LedgerEntry>) {
        _state.value = _state.value.copy(
            list = newList,
            totalUnit = newList.sumOf { it.amount }
        )
    }
    
    fun toggleSendSMS() {
        val newSendSMS = !_state.value.sendSMS
        _state.value = _state.value.copy(sendSMS = newSendSMS)
        
        // Save to persistent storage
        viewModelScope.launch {
            UserSession.getInstance().updateSendSMS(newSendSMS)
        }
    }
    
    fun togglePrinting() {
        val newIsPrintingEnabled = !_state.value.isPrintingEnabled
        _state.value = _state.value.copy(isPrintingEnabled = newIsPrintingEnabled)
        
        // Save to persistent storage
        viewModelScope.launch {
            UserSession.getInstance().updateIsPrintingEnabled(newIsPrintingEnabled)
        }
    }
    
    fun toggleAllowExtra() {
        val newIsAllowExtra = !_state.value.isAllowExtra
        _state.value = _state.value.copy(isAllowExtra = newIsAllowExtra)
        
        // Save to persistent storage
        viewModelScope.launch {
            UserSession.getInstance().updateIsAllowExtra(newIsAllowExtra)
        }
    }
    
    fun openModal(mode: String) {
        _state.value = _state.value.copy(modalMode = mode)
    }
    
    fun closeModal() {
        _state.value = _state.value.copy(modalMode = null)
    }
    
    fun openBetAndReminingModal(number: String) {
        _state.value = _state.value.copy(
            isBetAndReminingModalOpen = true,
            betAndReminingNumber = number
        )
    }
    
    fun closeBetAndReminingModal() {
        _state.value = _state.value.copy(
            isBetAndReminingModalOpen = false,
            betAndReminingNumber = ""
        )
    }
    
    fun clearList() {
        println("[DEBUG] clearList called - starting to clear list and refresh data")
        val tempListStore = TempListStore.getInstance()
        tempListStore.clearList()
        tempListStore.setListType("SELL")
        // Refresh ledgers and hot numbers data
        println("[DEBUG] clearList - about to call refreshData()")
        refreshData()
        println("[DEBUG] clearList - refreshData() completed")
    }
    
    fun handleBuy() {
        handleSave(true)
    }
    
    fun handleDirectBuy() {
        val tempListStore = TempListStore.getInstance()
        val tempListItems = tempListStore.getItems()
        
        // Check if TempListStore is empty
        if (tempListItems.isEmpty()) {
            // Show toast message for empty TempListStore
            println("Toast: TempListStore is empty. Please add items before proceeding.")
            return
        }
        
        // If not empty, open the direct-buy modal
        openModal("direct-buy")
    }
    
    fun handleSave(isBuy: Boolean) {
        println("[DEBUG] handleSave called with isBuy: $isBuy")
        
        val currentState = _state.value
        val selectedUser = selectionStore.getSelectedUser()
        val selectedTerm = selectionStore.getSelectedTerm()
        val tempListStore = TempListStore.getInstance()
        val tempListType = tempListStore.getListType()
        val tempListItems = tempListStore.getItems()
        
        println("[DEBUG] Current state - apiCalling: ${currentState.apiCalling}")
        println("[DEBUG] Selected user: $selectedUser")
        println("[DEBUG] Selected term: $selectedTerm")
        println("[DEBUG] TempList type: $tempListType")
        println("[DEBUG] TempList items count: ${tempListItems.size}")
        println("[DEBUG] TempList items: $tempListItems")
        
        if (currentState.apiCalling || selectedUser == null || selectedTerm == null) {
            println("[DEBUG] Early return - apiCalling: ${currentState.apiCalling}, selectedUser: $selectedUser, selectedTerm: $selectedTerm")
            return
        }
        
        // Check if TempListStore is empty
        if (tempListItems.isEmpty()) {
            // Show toast message for empty TempListStore
            println("[DEBUG] Toast: TempListStore is empty. Please add items before proceeding.")
            return
        }
        
        println("[DEBUG] Starting coroutine for API call")
        viewModelScope.launch {
            println("[DEBUG] Setting apiCalling to true")
            _state.value = _state.value.copy(apiCalling = true)
            
            try {
                val user = currentState.userList.find { it.userId == selectedUser.value }
                val tempListItems = tempListStore.getItems()
                
                println("[DEBUG] Found user: $user")
                println("[DEBUG] TempList items for conversion: $tempListItems")
                
                // Convert TempListItem to LedgerEntry
                val ledgerEntries = tempListItems.map { tempItem ->
                    println("[DEBUG] Converting tempItem: $tempItem")
                    LedgerEntry(
                        number = tempItem.number,
                        amount = tempItem.amount.toIntOrNull() ?: 0,
                        summary = if (tempItem.summary.isBlank()) "Default summary" else tempItem.summary,
                        showSummary = if (tempItem.showSummary.isBlank()) "Default show summary" else tempItem.showSummary,
                        groupId = tempItem.groupId?.toIntOrNull() ?: 1,
                        delete = false
                    )
                }
                
                println("[DEBUG] Converted ledger entries: $ledgerEntries")
                
                val payload = SlipPayload(
                    termId = selectedTerm.value,
                    customer = selectedUser.label,
                    status = if (tempListType == "BUY" || isBuy) "BUY" else "SALE",
                    ledger = ledgerEntries,
                    userId = selectedUser.value,
                    deviceName = "Kotlin-App",
                    userType = user?.userType ?: "customer"
                )
                
                println("[DEBUG] Created payload: $payload")
                
                println("[DEBUG] Determining API endpoint...")
                println("[DEBUG] tempListType: $tempListType")
                println("[DEBUG] isAllowExtra: ${currentState.isAllowExtra}")
                println("[DEBUG] userProfile: ${currentState.userProfile}")
                println("[DEBUG] userType: ${currentState.userProfile?.userType}")
                
                val response = when {
                    tempListType == "SELL" && currentState.isAllowExtra && 
                    (currentState.userProfile?.userType == "employee" || currentState.userProfile?.userType == "owner") -> {
                        println("[DEBUG] Calling addSlips (SELL + allowExtra + employee/owner)")
                        apiService.addSlips(payload)
                    }

                    (tempListType == "SELL" && !currentState.isAllowExtra) || (currentState.userProfile?.userType == "agent" || currentState.userProfile?.userType == "user")  -> {
                        println("[DEBUG] Calling addSlipsWithHotBreak (SELL + !allowExtra OR agent/user)")
                        apiService.addSlipsWithHotBreak(payload)
                    }

                    tempListType == "BUY" || isBuy -> {
                        println("[DEBUG] Calling addSlips (BUY or isBuy=true)")
                        apiService.addSlips(payload)
                    }

                    else -> {
                        println("[DEBUG] Calling addSlips (default case)")
                        apiService.addSlips(payload)
                    }
                }
                
                println("[DEBUG] API response received: $response")
                AudioPlayer.playSuccessSong()
                if (response.code == "200") {
                    println("[DEBUG] Success response received")
                    // Success - play success sound, handle printing, SMS, etc.
                    
                    // Check if this was addSlipsWithHotBreak call
                    val isHotBreakCall = (tempListType == "SELL" && !currentState.isAllowExtra) || 
                        (currentState.userProfile?.userType == "agent" || currentState.userProfile?.userType == "user")
                    
                    println("[DEBUG] Is HotBreak call: $isHotBreakCall")
                    
                    if (isHotBreakCall) {
                        println("[DEBUG] Processing HotBreak success response")
                        println("[DEBUG] Response data: ${response.data}")
                        println("[DEBUG] Response message: ${response.message}")
                        
                        // Build success message like JavaScript version
                        var successMessage = "ရ\n"
                        response.data.ledger.forEach { ledgerEntry ->
                            successMessage += "${ledgerEntry.number}=${ledgerEntry.amount} \n"
                        }
                        
                        println("[DEBUG] Built success message: $successMessage")
                        
                        val finalMessage = if (response.message.contains("မရ")) {
                            "$successMessage${response.message}" 
                        } else {
                            "ဂဏန်းများအားလုံးရပါသည် စလစ်ထဲတွင် စစ်ကြည့်ပေးပါ"
                        }
                        
                        println("[DEBUG] Final message: $finalMessage")
                        
                        // Update state to show success dialog
                        println("[DEBUG] Updating state to show success dialog")
                        _state.value = _state.value.copy(
                            modalMode = "success",
                            isConfirmModalOpen = true,
                            successMessage = finalMessage
                        )
                    }
                    
                    println("[DEBUG] Calling clearList()")
                    clearList()
                } else {
                    println("[DEBUG] Error response - code: ${response.code}, message: ${response.message}")
                    // Error - play fail sound
                }
                
            } catch (e: Exception) {
                println("[DEBUG] Exception caught in save operation: ${e.message}")
                println("[DEBUG] Exception stack trace: ${e.stackTraceToString()}")
            } finally {
                println("[DEBUG] Setting apiCalling to false in finally block")
                _state.value = _state.value.copy(apiCalling = false)
            }
        }
        
        println("[DEBUG] handleSave function completed")
    }
    
    fun setTermOpen(open: Boolean) {
        println("setTermOpen called with: $open")
        _state.value = _state.value.copy(
            termOpen = open,
            selectedTermIndex = if (open && _state.value.filteredTermOptions.isNotEmpty()) 0 else -1
        )
    }
    
    fun setUserOpen(open: Boolean) {
        _state.value = _state.value.copy(userOpen = open)
    }
    
    fun updateInputValue(value: String) {
        _state.value = _state.value.copy(inputValue = value)
    }

    fun updateTermSearchQuery(query: String) {
        val filteredOptions = if (query.isEmpty()) {
            _state.value.termOptions
        } else {
            _state.value.termOptions.filter { 
                it.label.contains(query, ignoreCase = true) 
            }
        }
        _state.value = _state.value.copy(
            termSearchQuery = query,
            filteredTermOptions = filteredOptions,
            selectedTermIndex = if (filteredOptions.isNotEmpty() && _state.value.termOpen) 0 else -1
        )
    }

    fun handleTermEnterKey() {
        println("handleTermEnterKey called")
        val currentState = _state.value
        println("Current state - termOpen: ${currentState.termOpen}, selectedIndex: ${currentState.selectedTermIndex}, filteredOptions size: ${currentState.filteredTermOptions.size}")
        if (!currentState.termOpen) {
            // If dropdown is closed, open it
            println("Opening dropdown")
            setTermOpen(true)
            _state.value = _state.value.copy(selectedTermIndex = 0)
        } else {
            // If dropdown is open and an item is selected, choose it
            val filteredOptions = currentState.filteredTermOptions
            val selectedIndex = currentState.selectedTermIndex
            println("Trying to select item at index $selectedIndex")
            if (selectedIndex >= 0 && selectedIndex < filteredOptions.size) {
                println("Selecting term: ${filteredOptions[selectedIndex].label}")
                updateSelectedTerm(filteredOptions[selectedIndex])
                setTermOpen(false)
                updateTermSearchQuery("")
                _state.value = _state.value.copy(selectedTermIndex = -1)
            } else {
                println("No valid item selected, keeping dropdown open")
            }
        }
    }

    fun handleTermArrowKey(isUp: Boolean) {
        println("handleTermArrowKey called with isUp: $isUp")
        val currentState = _state.value
        println("Arrow key - termOpen: ${currentState.termOpen}, filteredOptions size: ${currentState.filteredTermOptions.size}, currentIndex: ${currentState.selectedTermIndex}")
        if (currentState.termOpen && currentState.filteredTermOptions.isNotEmpty()) {
            val maxIndex = currentState.filteredTermOptions.size - 1
            val newIndex = if (isUp) {
                if (currentState.selectedTermIndex <= 0) maxIndex else currentState.selectedTermIndex - 1
            } else {
                if (currentState.selectedTermIndex >= maxIndex) 0 else currentState.selectedTermIndex + 1
            }
            println("Setting new index to: $newIndex")
            _state.value = _state.value.copy(selectedTermIndex = newIndex)
        } else {
            println("Arrow key ignored - dropdown not open or no options")
        }
    }

    fun focusTermSelect() {
        viewModelScope.launch {
            termFocusRequester.requestFocus()
        }
    }

    fun focusUserSelect() {
        viewModelScope.launch {
            userFocusRequester.requestFocus()
        }
    }
    
    fun focusNumberInputAfterUserSelection() {
        // This function will be called from the TwoDView/ThreeDView components
        // to focus their number input after user selection
        println("[DEBUG] focusNumberInputAfterUserSelection called - is2D: ${_state.value.is2D}")
    }
    
    fun setShowViewModal(show: Boolean) {
        _state.value = _state.value.copy(showViewModal = show)
    }
    
    fun setShowOptionsModal(show: Boolean) {
        _state.value = _state.value.copy(showOptionsModal = show)
    }
    
    fun updateState(update: (SalePageState) -> SalePageState) {
        _state.value = update(_state.value)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalePage(
    apiService: ApiService = ApiService()
) {
    LaunchedEffect(Unit) {
        println("[DEBUG] SalePage composable is being rendered!")
    }
    
    val viewModel = remember { SalePageViewModel(apiService) }
    val state by viewModel.state.collectAsState()
    val selectionStore = rememberSelectionStore()
    val selectionState by selectionStore.state.collectAsState()
    val focusManager = LocalFocusManager.current
    
    println("SalePage recomposed - termOpen: ${state.termOpen}, selectedTermIndex: ${state.selectedTermIndex}")
    
    // Keyboard shortcuts
    LaunchedEffect(Unit) {
        // Global keyboard event handling would be implemented here
    }
    
    LaunchedEffect(Unit) {
        println("[DEBUG] Main Column is rendering!")
    }
    
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .onKeyEvent { keyEvent ->
                    println("Global Column key event: ${keyEvent.key}, type: ${keyEvent.type}, termOpen: ${state.termOpen}")
                    when {
                        keyEvent.key == Key.T && keyEvent.type == KeyEventType.KeyDown -> {
                            println("T key pressed - focusing term select")
                            viewModel.focusTermSelect()
                            viewModel.setTermOpen(true)
                            true
                        }
                        keyEvent.key == Key.U && keyEvent.type == KeyEventType.KeyDown -> {
                            println("U key pressed - focusing user select")
                            viewModel.focusUserSelect()
                            viewModel.setUserOpen(true)
                            true
                        }
                        keyEvent.key == Key.F4 && keyEvent.type == KeyEventType.KeyDown -> {
                            viewModel.handleSave(false)
                            true
                        }
                        // Temporarily disabled global Escape handler to prevent conflicts
                        // keyEvent.key == Key.Escape && keyEvent.type == KeyEventType.KeyDown -> {
                        //     println("Global Escape key pressed - termOpen: ${state.termOpen}, userOpen: ${state.userOpen}")
                        //     if (state.termOpen || state.userOpen) {
                        //         viewModel.setTermOpen(false)
                        //         viewModel.setUserOpen(false)
                        //         focusManager.clearFocus()
                        //         true
                        //     } else {
                        //         false
                        //     }
                        // }
                        // Removed global arrow and enter key handlers to prevent conflicts with dropdown
                        else -> false
                    }
                }
    ) {
        Card(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
            // Left Section - Main Controls (40%)
            SalePageLeftContent(
                state = state,
                selectionState = selectionState,
                viewModel = viewModel,
                onOpenBetAndReminingModal = { number -> viewModel.openBetAndReminingModal(number) },
                modifier = Modifier.weight(0.17f).fillMaxHeight()
            )  
            // TempList Section (25%)
                Column(
                     modifier = Modifier.weight(0.23f).fillMaxHeight().padding(2.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val tempListStore = rememberTempListStore()
                    
                    // Convert LedgerEntry to TempListItem
                    // TempList now displays data directly from TempListStore
                    // Data is added to the store via TwoDView.processNumberEntry() or other components
                    
                    TempList(store = tempListStore)
                }
            } // Close TempList Column
            
            // HotNumber Section (15%)
            
            Column(
                modifier = Modifier.weight(0.08f).fillMaxHeight().padding(1.dp)
            ) {
                if (selectionState.selectedTerm != null) {
                    val hotNumberViewModel = remember(selectionState.selectedTerm!!.value, state.is2D) {
                        SimpleHotNumberViewModel(
                            termId = selectionState.selectedTerm!!.value,
                            is2D = state.is2D
                        )
                    }
                    
                    // Set the hot number view model reference in the main view model
                    LaunchedEffect(hotNumberViewModel) {
                        viewModel.setHotNumberViewModel(hotNumberViewModel)
                    }
                    
                    SimpleHotNumberComponent(
                        viewModel = hotNumberViewModel,
                        is2D = state.is2D,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Clear the hot number view model reference when no term is selected
                    LaunchedEffect(Unit) {
                        viewModel.setHotNumberViewModel(null)
                    }
                    
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Select a term to manage hot numbers",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } // Close HotNumber Column
            
            // Ledger Section (20%)
          
            Column(
                modifier = Modifier.weight(0.2f).fillMaxHeight().padding(1.dp)
            ) {
                LaunchedEffect(state.userProfile, selectionState.selectedTerm) {
                    println("[DEBUG] Ledger Section - userProfile: ${state.userProfile}")
                    println("[DEBUG] Ledger Section - selectedTerm: ${selectionState.selectedTerm}")
                    println("[DEBUG] Ledger Section - userType: ${state.userProfile?.userType}")
                }
                
                if (state.userProfile?.userType == "owner" || state.userProfile?.userType == "employee") {
                    val selectedTerm = selectionState.selectedTerm
                    if (selectedTerm != null) {
                        // Convert UserOption to pages.UserOption
                        val convertedUserList = state.userList.map { user ->
                            UserOption(
                                value = user.userId,
                                label = user.name,
                                userType = user.userType
                            )
                        }
                        
                        Ledger(
                            termId = selectedTerm.value,
                            termName = selectedTerm.label,
                            apiBreakAmount = state.breakAmount.toInt(),
                            is2D = state.is2D,
                            userList = convertedUserList,
                            onFetchActiveTerms = { viewModel.refreshData() },
                            onSetLedgerData = { ledgerItems: List<LedgerItem> -> 
                                val ledgerEntries = ledgerItems.map { item ->
                                    LedgerEntry(
                                        number = item.number,
                                        amount = item.totalAmount.toInt(),
                                        summary = "Ledger entry",
                                        showSummary = "Ledger entry",
                                        groupId = 1
                                    )
                                }
                                viewModel.updateList(ledgerEntries)
                            },
                            onRefreshLedgerParent = { viewModel.refreshData() },
                            onNavigateToBuy = { viewModel.handleBuy() },
                            onShowViewModal = { show: Boolean -> viewModel.setShowViewModal(show) },
                            onShowOptionsModal = { show: Boolean -> viewModel.setShowOptionsModal(show) }
                        )
                    } else {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Select a term to view ledger")
                        }
                    }
                }
            } // Close Ledger Column
        } // Close Row
    } // Close Card
    } // Close main Column

    // Modals
    when (state.modalMode) {
        "total-message" -> {
            TotalMessage(
                is2D = state.is2D,
                unitPrice = state.unitPrice,
                onDismiss = { viewModel.closeModal() }
            )
        }
        "direct-buy" -> {
            AlertDialog(
                onDismissRequest = { viewModel.closeModal() },
                title = { Text("Direct Buy") },
                text = {
                    Column {
                        Text("Term ID: ${selectionState.selectedTerm?.value}")
                        Text("Ledger items: ${state.list.size}")
                        Text("This would show the DirectBuy component")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.closeModal() }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
        "warning_user" -> {
            AlertDialog(
                onDismissRequest = { viewModel.closeModal() },
                title = { Text("သတိပေးချက်") },
                text = { Text("ထိုးသားရွေးပါ") },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.closeModal() }
                    ) {
                        Text("အိုကေ")
                    }
                }
            )
        }
        "confirm_save" -> {
            AlertDialog(
                onDismissRequest = { viewModel.closeModal() },
                title = { Text("သတိပေးချက်!") },
                text = { Text("သိမ်းမှာကျိန်းသေပါသလား") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.closeModal()
                            viewModel.handleSave(false)
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFd33d33)
                        )
                    ) {
                        Text("Yes, save it!")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.closeModal() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF3085d6)
                        )
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
        "confirm_term_change" -> {
            AlertDialog(
                onDismissRequest = { viewModel.closeModal() },
                title = { Text("သတိပေးချက်!") },
                text = { Text("Changing the term will clear your current list. Are you sure?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.confirmTermChange()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFd33d33)
                        )
                    ) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.closeModal() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF3085d6)
                        )
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
        else -> {
            // No modal to show
        }
    } // Close when

    // Success Dialog
    state.successMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { 
                viewModel.updateState { it.copy(successMessage = null) }
            },
            title = { Text("Easi Ledger") },
            text = { 
                Text(
                    text = message,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        viewModel.updateState { it.copy(successMessage = null) }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF3085d6)
                    )
                ) {
                    Text("OK")
                }
            }
        )
    }

        // BetAndRemining Modal
        if (state.isBetAndReminingModalOpen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight(0.6f),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Header with title and close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bet and Remaining Data",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { viewModel.closeBetAndReminingModal() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Close")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // BetAndRemining table component
                    if (selectionState.selectedUser != null && selectionState.selectedTerm != null && state.betAndReminingNumber.isNotEmpty()) {
                        val userOption = UserOption(selectionState.selectedUser!!.value, selectionState.selectedUser!!.label)
                        
                        BetAndRemining(
                            number = state.betAndReminingNumber,
                            user = userOption,
                            termId = selectionState.selectedTerm!!.value
                        )
                    } else {
                        Text(
                            text = "Please select a user, term, and enter a number to view bet and remaining data.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } // Close Column
            } // Close Card
        } // Close Box
        } // Close if (state.isBetAndReminingModalOpen)
        
        // View Modal
        if (state.showViewModal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { viewModel.setShowViewModal(false) }
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.98f)
                        .fillMaxHeight(1f)
                        .clickable(enabled = false) { },
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp
                ) {
                    if (selectionState.selectedTerm != null) {
                        ViewForm(
                            is2D = state.is2D,
                            breakAmount = state.breakAmount.toInt(),
                            termId = selectionState.selectedTerm!!.value,
                            termName = selectionState.selectedTerm!!.label,
                            setBreakAmount = { },
                            handleRefreshLedger = { viewModel.refreshData() }
                        )
                    }
                }
            }
        }
        
        // Options Modal
        if (state.showOptionsModal) {
            Dialog(
                onDismissRequest = { viewModel.setShowOptionsModal(false) },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    modifier = Modifier
                        .widthIn(min = 300.dp, max = 600.dp)
                        .fillMaxHeight(0.9f),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    if (selectionState.selectedTerm != null) {
                        OptionUI(
                            termId = selectionState.selectedTerm!!.value,
                            breakAmount = state.breakAmount.toInt(),
                            onCloseModal = { viewModel.setShowOptionsModal(false) }
                        )
                    }
                }
            }
        }
    } // Close main Box
} // Close SalePage function