package ui.screens

import core.services.ApiService
import core.services.UserSession
import data.models.ApiResponse
import ui.screens.Agent
import ui.screens.SlipData as ChangeSlipData
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.components.*
import data.models.TermOption
import data.models.UserOption
import data.models.TermsApiResponse
import data.models.UserData
import data.models.UsersApiResponseData
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ui.screens.Item
import ui.screens.SlipTerm
import ui.screens.SlipDetailProps
import ui.screens.UserProfile
import ui.screens.SlipDetailContent
import core.services.ApiService.PaginationData
import core.services.ApiService.SlipData
import core.services.ApiService.SlipUser
import core.services.ApiService.StatementData
import core.services.ApiService.SlipDetailResponseData
import core.services.ApiService.SlipResponseData
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.TimeZone
import core.config.CompactOutlinedTextField

// API Response data classes - using TermsApiResponse from TermsPage.kt
// Note: SlipTermData is used locally for slip-specific term data

@Serializable
data class SlipTermData(
    val termId: Int,
    val termName: String,
    val shortName: String,
    val groupId: String,
    val startDate: String,
    val endDate: String,
    val isFinished: String,
    val termType: String,
    val winNum: String
)

// UserData and UsersApiResponseData are defined in CommonModels.kt

@Serializable
data class UsersApiResponse(
    val success: Boolean? = null,
    val data: UsersApiResponseData? = null,
    val message: String? = null,
    // Alternative field names for different API responses
    val users: List<UserData>? = null,
    // For nested response structure
    val result: UsersApiResponseData? = null,
    val status: String? = null
)

// PaginationData is defined in TermsPage.kt

// SlipData and SlipUser are now imported from ApiService

@Serializable
data class SlipDetailData(
    val _id: String,
    val number: String,
    val type: String,
    val amount: Int,
    val slipId: String,
    val termId: Int,
    val userId: Int,
    val summary: String? = null,
    val showSummary: String? = null,
    val groupId: String? = null
)

@Serializable
data class SlipDetailResponse(
    val smsCopy: Int,
    val copy: Int,
    val phoneNumber: String = ""
)

// SlipReportData is now replaced with StatementData from ApiService

@Composable
fun SlipsContent(onNavigateToSale: () -> Unit = {}) {
    var selectedTermId by remember { mutableStateOf("") }
    var selectedUserId by remember { mutableStateOf("") }
    var slips by remember { mutableStateOf<List<SlipData>>(emptyList()) }
    var selectedSlip by remember { mutableStateOf<SlipData?>(null) }
    var slipDetails by remember { mutableStateOf<SlipDetailProps?>(null) }
    var total by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var loadingDetail by remember { mutableStateOf(false) }
    var showAmount by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<StatementData?>(null) }
    var page by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // Modal states for editing
    var showEditModal by remember { mutableStateOf(false) }
    var editModalType by remember { mutableStateOf("") } // "update-slip" or "change-user"
    var editSlipData by remember { mutableStateOf<SlipData?>(null) }
    
    // Store slip detail data with groupId for updates
    var currentSlipDetailData by remember { mutableStateOf<List<SlipDetailData>>(emptyList()) }
    
    // Delete confirmation dialog states
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var deleteItemIndex by remember { mutableStateOf(-1) }
    var deleteItem by remember { mutableStateOf<Item?>(null) }
    var deleteCallback by remember { mutableStateOf<((Int, Item) -> Unit)?>(null) }
    var currentEditItem by remember { mutableStateOf<Item?>(null) }
    
    // Term and User selection states
    var termOptions by remember { mutableStateOf<List<TermOption>>(emptyList()) }
    var userOptions by remember { mutableStateOf<List<UserOption>>(emptyList()) }
    var isLoadingTerms by remember { mutableStateOf(false) }
    var isLoadingUsers by remember { mutableStateOf(false) }
    var termError by remember { mutableStateOf<String?>(null) }
    var userError by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val apiService = remember { ApiService() }
    
    // Handle delete single slip item
    fun handleDeleteSingleSlip(index: Int, item: Item) {
        coroutineScope.launch {
            try {
                val response = apiService.deleteSlipItem(
                    itemId = item._id,
                    slipId = item.slipId,
                    termId = item.termId,
                    userId = item.userId
                )
                if (response.success) {
                    // Refresh like JS version
                    refreshTrigger++
                } else {
                    // Handle error response
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // Handle update single slip item - matches JavaScript implementation
    suspend fun handleUpdateSingleSlip(number: String, amount: String) {
        try {
            val currentSlipDetails = slipDetails
            val currentItem = currentEditItem ?: return

            // Check if the same number is entered - do nothing if unchanged
            if (currentItem.number == number && currentItem.amount.toString() == amount) {
                showEditModal = false
                editSlipData = null
                currentEditItem = null
                return
            }

            // Check if only amount changed but number is the same
            val isOnlyAmountChanged = currentItem.number == number && currentItem.amount.toString() != amount
            
            if (isOnlyAmountChanged) {
                // For amount-only changes, check if this item is part of a pattern group
                var currentGroupId: String? = null
                
                if (currentSlipDetailData.isNotEmpty()) {
                    val currentItemInDetails = currentSlipDetailData.find { item ->
                        item._id == currentItem._id ||
                        (item.number == currentItem.number && item.amount == currentItem.amount)
                    }
                    currentGroupId = currentItemInDetails?.groupId
                }
                
                if (!currentGroupId.isNullOrEmpty() && currentSlipDetailData.isNotEmpty()) {
                    val itemsWithSameGroupId = currentSlipDetailData.filter { item ->
                        item.groupId == currentGroupId
                    }

                    
                    // Check if this is a pattern group
                    val hasRPattern = itemsWithSameGroupId.any { item ->
                        item.summary?.endsWith("R") == true
                    }
                    val has3DRPattern = itemsWithSameGroupId.any { item ->
                        item.summary?.endsWith("R") == true && 
                        item.number.length == 3
                    }
                    val hasNStarPattern = itemsWithSameGroupId.any { item ->
                        item.summary?.contains("ထိပ်") == true
                    }
                    val hasBreakPattern = itemsWithSameGroupId.any { item ->
                        item.summary?.endsWith("B") == true || item.summary?.contains("ဘရိတ်") == true
                    }
                    val hasStarNPattern = itemsWithSameGroupId.any { item ->
                        item.summary?.contains("ပိတ်") == true
                    }
                    val hasNPPattern = itemsWithSameGroupId.any { item ->
                        item.summary?.contains("ပါ") == true
                    }
                    val hasDoubleNStarPattern = itemsWithSameGroupId.any { item ->
                        item.summary?.matches(Regex("^\\d\\d\\*$")) == true
                    }
                    val hasStartDoubleNPattern = itemsWithSameGroupId.any { item ->
                        item.summary?.matches(Regex("^\\*\\d\\d$")) == true
                    }
                    val hasNStarNPattern = itemsWithSameGroupId.any { item ->
                        item.summary?.matches(Regex("^\\d\\*\\d$")) == true
                    }
                    
                    val isPatternGroup = hasRPattern || has3DRPattern || hasNStarPattern || hasBreakPattern || 
                                       hasStarNPattern || hasNPPattern || hasDoubleNStarPattern || 
                                       hasStartDoubleNPattern || hasNStarNPattern
                    
                    
                    // If it's a pattern group, update all items in the group with new amount
                    if (itemsWithSameGroupId.size > 1 && isPatternGroup) {
                        
                        // Update all items in the group with the new amount
                        val updatedGroupItems = itemsWithSameGroupId.map { item ->
                            ApiService.UpdateSlipItem(
                                _id = item._id,
                                number = item.number,
                                amount = amount.toIntOrNull() ?: 0,
                                showSummary = item.showSummary ?: "",
                                groupId = currentGroupId,
                                summary = item.summary ?: ""
                            )
                        }
                        
                        // Get all items from slip details that are NOT in the current group
                        val otherItems = currentSlipDetailData.filter { item ->
                            item.groupId != currentGroupId
                        }.map { item ->
                            ApiService.UpdateSlipItem(
                                _id = item._id,
                                number = item.number,
                                amount = item.amount,
                                showSummary = item.showSummary ?: "",
                                groupId = item.groupId,
                                summary = item.summary ?: ""
                            )
                        }
                        
                        val updateRequest = ApiService.UpdateSlipsRequest(
                            termId = currentItem.termId,
                            userId = currentItem.userId,
                            slipId = selectedSlip?.slipId ?: currentItem.slipId,
                            ledger = otherItems + updatedGroupItems
                        )
                        
                        val response = apiService.updateSlips(updateRequest)
                        
                        if (response.success) {
                            showEditModal = false
                            editSlipData = null
                            currentEditItem = null
                            refreshTrigger++
                        }
                        return
                    }
                }
            }
            
            // For single items, non-pattern groups, or number changes
            
            // Find the item in slip details to get groupId
            val itemInDetails = if (currentSlipDetailData.isNotEmpty()) {
                currentSlipDetailData.find { item ->
                    item._id == currentItem._id ||
                    (item.number == currentItem.number && item.amount == currentItem.amount)
                }
            } else null
            
            val updateRequest = ApiService.UpdateSlipsRequest(
                termId = currentItem.termId,
                userId = currentItem.userId,
                slipId = selectedSlip?.slipId ?: currentItem.slipId,
                ledger = listOf(
                    ApiService.UpdateSlipItem(
                        _id = currentItem._id,
                        number = number,
                        amount = amount.toIntOrNull() ?: 0,
                        showSummary = currentItem.showSummary,
                        groupId = itemInDetails?.groupId,
                        summary = currentItem.summary
                    )
                )
            )
            
            val response = apiService.updateSlips(updateRequest)
            
            if (response.success) {
                showEditModal = false
                editSlipData = null
                currentEditItem = null
                refreshTrigger++
            }
        } catch (e: Exception) {
            println("[ERROR] Error updating slip: ${e.message}")
            e.printStackTrace()
        }
    }
    
    // Handle delete entire slip
    fun handleDeleteSlip(slip: SlipData) {
        coroutineScope.launch {
            try {
                val response = apiService.deleteSlip(
                    slipId = slip.slipId,
                    termId = slip.termId.toString(),
                    userId = slip.userId.toString()
                )
                if (response.success) {
                    // Reset UI state like Slip.js
                    selectedSlip = null
                    slipDetails = null
                    total = 0
                    slips = emptyList()
                    page = 1
                    refreshTrigger++
                }
            } catch (e: Exception) {
                // Handle error - could show snackbar or toast
            }
        }
    }
    
    // Handle edit slip - opens modal for changing slip user
    fun handleEditSlip(slip: SlipData, mode: String = "change-user") {
        editSlipData = slip
        editModalType = mode
        showEditModal = true
    }
    
    // Fetch terms
    suspend fun fetchTerms() {
        try {
            val userSession = UserSession.getInstance()
            val response = apiService.get<TermsApiResponse>(
                url = "${ApiService.BASE_URL}/v1/term/getTerms?current=1&limit=1000",
                headers = userSession.getAuthHeaders()
            )
            if (response.success && response.data != null) {
                val termsData = response.data.data.by
                val terms = termsData.filter { termData -> termData.isFinished == "0" }
                termOptions = terms.map { termData ->
                    TermOption(
                        termId = termData.termId,
                        termName = termData.termName,
                        shortName = termData.shortName,
                        groupId = termData.groupId,
                        startDate = termData.startDate,
                        endDate = termData.endDate,
                        isFinished = termData.isFinished,
                        termType = termData.termType,
                        winNum = termData.winNum,
                        is2D = termData.is2D,
                        unitPrice = termData.unitPrice
                    )
                }
                if (selectedTermId.isEmpty() && termOptions.isNotEmpty()) {
                    selectedTermId = termOptions.first().termId.toString()
                }
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    // Fetch users
    suspend fun fetchUsers() {
        try {
            val userSession = UserSession.getInstance()
            val response = apiService.get<UsersApiResponse>(
                url = "${ApiService.BASE_URL}/v1/account/getUserLists?current=1&limit=1000",
                headers = userSession.getAuthHeaders()
            )
            // Check if we have a successful response with data
            if (response.success && response.data?.data?.by != null) {
                val apiUsers = response.data?.data?.by ?: emptyList()
                val userList = mutableListOf<UserOption>()
                // Add "All" option at the beginning
                userList.add(UserOption("", "All", ""))
                // Add users from API
                userList.addAll(apiUsers.map { userData ->
                    val userId = userData.userId ?: userData.id ?: 0
                    val userName = userData.userName ?: userData.name ?: userData.username ?: userData.email ?: "Unknown User"
                    val userType = userData.userType ?: userData.type ?: "user"
                    UserOption(
                        value = userId.toString(),
                        label = userName,
                        userType = userType
                    )
                })
                userOptions = userList
            } else {
                // Fallback to sample users if API fails
                val fallbackUsers = listOf(
                    UserOption("", "All"),
                    UserOption("1", "User 1", "user"),
                    UserOption("2", "Agent 1", "agent")
                )
                userOptions = fallbackUsers
            }
        } catch (e: Exception) {
            // Fallback to sample users if API fails
            val fallbackUsers = listOf(
                UserOption("", "All"),
                UserOption("1", "User 1", "user"),
                UserOption("2", "Agent 1", "agent")
            )
            userOptions = fallbackUsers
        }
    }
    

    
    // Fetch slips
    suspend fun fetchSlips(resetList: Boolean = false) {
        if (selectedTermId.isEmpty()) return
        
        if (resetList) {
            loading = true
        } else {
            loadingMore = true
        }
        try {
            val currentPage = if (resetList) 1 else page
            val response = apiService.getSlips(
                termId = selectedTermId,
                userId = selectedUserId,
                page = currentPage,
                limit = 30
            )
            
            // Handle both 200 and 404 responses (404 might mean no more data)
            if (response.success && (response.data?.code == "200" || response.data?.code == "404")) {
                val responseData = response.data?.data?.let { jsonElement ->
                    try {
                        Json.decodeFromJsonElement(SlipResponseData.serializer(), jsonElement)
                    } catch (e: Exception) {
                        null
                    }
                }
                val newSlips = responseData?.by ?: emptyList()
                total = responseData?.pagination?.total ?: 0
                
                if (resetList) {
                    slips = newSlips
                    if (newSlips.isNotEmpty() && selectedSlip == null) {
                        selectedSlip = newSlips.first()
                    }
                } else {
                    slips = slips + newSlips
                }
                
                // Calculate hasMore based on pagination data
                val currentTotal = slips.size
                hasMore = newSlips.isNotEmpty() && currentTotal < total && currentPage < 4
            } else {
                hasMore = false
            }
        } catch (e: Exception) {
            hasMore = false
        } finally {
            loading = false
            loadingMore = false
        }
    }
    
    // Fetch statements/report
    suspend fun fetchStatements() {
        if (selectedTermId.isEmpty() && selectedUserId.isEmpty()) return
        if (!showAmount) return
        
        try {
            val response = apiService.getStatements(selectedTermId, selectedUserId)
            if (response.success && response.data != null && response.data?.code == "200") {
                // Parse the actual response structure
                val responseData = response.data?.data?.let { jsonElement ->
                    try {
                        Json.decodeFromJsonElement(StatementData.serializer(), jsonElement)
                    } catch (e: Exception) {
                        null
                    }
                }
                report = responseData
            } else {
                report = null
            }
        } catch (e: Exception) {
            report = null
        }
    }
    
    // Fetch slip details
    suspend fun fetchSlipDetails(slip: SlipData) {
        loadingDetail = true
        try {
            val response = apiService.getSlipDetails(
                slipId = slip.slipId,
                termId = slip.termId.toString(),
                userId = slip.userId.toString()
            )
            
            if (response.success && response.data?.code == "200") {
                // Parse the actual response structure - slip details come as an array in response.data.data
                val slipDetailsFromApi = response.data?.data?.let { jsonElement ->
                    try {
                        val lenientJson = Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                            encodeDefaults = false
                        }
                        lenientJson.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(SlipDetailResponseData.serializer()), jsonElement)
                    } catch (e: Exception) {
                        emptyList<SlipDetailResponseData>()
                    }
                } ?: emptyList()
                val phoneNumber = response.data?.phoneNumber ?: ""
                
                if (slipDetailsFromApi.isNotEmpty()) {
                    val firstDetail = slipDetailsFromApi.first()
                    val detailResponse = SlipDetailResponse(
                        smsCopy = firstDetail.smsCopy ?: 0,
                        copy = firstDetail.copy ?: 0,
                        phoneNumber = phoneNumber
                    )
                    
                    // Convert API response to SlipDetailData
                    val details = slipDetailsFromApi.map { apiDetail ->
                        SlipDetailData(
                            _id = apiDetail._id,
                            number = apiDetail.number,
                            type = apiDetail.type ?: "",
                            amount = apiDetail.amount,
                            slipId = apiDetail.slipId,
                            termId = apiDetail.termId,
                            userId = apiDetail.userId,
                            summary = apiDetail.summary ?: "",
                            showSummary = apiDetail.showSummary ?: "",
                            groupId = apiDetail.groupId
                        )
                    }
                    
                    // Store the details for use in handleUpdateSingleSlip
                    currentSlipDetailData = details
                    slipDetails = SlipDetailProps(
                        copy = detailResponse.copy.toString(),
                        smsCopy = detailResponse.smsCopy.toString(),
                        slipNumber = slip.slipId,
                        customerName = slip.user.name,
                        totalAmount = slip.totalAmount,
                        items = details.map { item ->
                            Item(
                                _id = item._id,
                                number = item.number,
                                type = item.type,
                                amount = item.amount,
                                slipId = item.slipId,
                                termId = item.termId,
                                userId = item.userId,
                                summary = item.summary ?: "",
                                showSummary = item.showSummary ?: ""
                            )
                        },
                        status = slip.status ?: "Unknown",
                        term = termOptions.map { termOption -> SlipTerm(termOption.termId.toString(), termOption.termName) },
                        termId = selectedTermId,
                        phoneNumber = detailResponse.phoneNumber,
                        userId = slip.user.id.toString(),
                        userRole = UserSession.getInstance().userProfileData?.userType ?: "",
                        userAccess = UserSession.getInstance().userProfileData?.userAccess ?: "",
                        onDelete = { index, item -> handleDeleteSingleSlip(index, item) },
                        slipRefresh = { refreshTrigger++ }
                    )
                }
            } else {
                slipDetails = null
                selectedSlip = null
                refreshTrigger++
            }
        } catch (e: Exception) {
            // Handle error
        } finally {
            loadingDetail = false
        }
    }
    
    // Handle refresh
    fun handleRefresh() {
        slips = emptyList()
        page = 1
        selectedSlip = null
        slipDetails = null
        refreshTrigger++
    }
    
    // Handle term change
    fun handleTermChange(termId: String) {
        selectedSlip = null
        slipDetails = null
        total = 0
        slips = emptyList()
        page = 1
        selectedTermId = termId
        refreshTrigger++
    }
    
    // Handle user change
    fun handleUserChange(userId: String) {
        selectedSlip = null
        slipDetails = null
        total = 0
        slips = emptyList()
        page = 1
        selectedUserId = userId
        refreshTrigger++
    }
    
    // Format currency
    fun formatCurrency(value: Int?): String {
        return if (value != null) {
            NumberFormat.getNumberInstance(Locale.US).format(value)
        } else {
            "0"
        }
    }
    
    // Format user name
    fun formatUserName(name: String, userType: String): String {
        return if (userType.isNotEmpty()) {
            "$name ($userType)"
        } else {
            name
        }
    }
    
    // Initialize data
    LaunchedEffect(Unit) {
        fetchTerms()
        fetchUsers()
    }
    
    // Fetch slips when term/user changes
    LaunchedEffect(selectedTermId, selectedUserId, refreshTrigger) {
        if (selectedTermId.isNotEmpty()) {
            fetchSlips(resetList = true)
        }
    }
    
    // Fetch statements when showAmount changes
    LaunchedEffect(selectedTermId, selectedUserId, showAmount, refreshTrigger) {
        if (showAmount) {
            fetchStatements()
        }
    }
    
    // Fetch slip details when selected slip changes
    LaunchedEffect(selectedSlip, refreshTrigger) {
        selectedSlip?.let { fetchSlipDetails(it) }
    }
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Left Panel - Controls and Summary
        Card(
            modifier = Modifier
                .weight(1.4f)
                .fillMaxHeight()
                .padding(2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
            ) {
                // Term Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TermSelectionDropdown(
                        termOptions = termOptions,
                        selectedTerm = termOptions.find { it.termId.toString() == selectedTermId },
                        onTermSelected = { handleTermChange(it.termId.toString()) }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // User Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserSelectionDropdown(
                        users = userOptions,
                        selectedUser = selectedUserId,
                        onUserSelected = { userId -> handleUserChange(userId) },
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Refresh button
                    IconButton(
                        onClick = { handleRefresh() },
                        modifier = Modifier
                            .background(
                                Color.Blue,
                                RoundedCornerShape(8.dp)
                            )
                            .size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Show Amount Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = showAmount,
                        onCheckedChange = { showAmount = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Show Amount",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Report Summary
                if (showAmount && report != null) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactOutlinedTextField(
                            value = formatCurrency(report?.TotalUnitWithDiscount),
                            onValueChange = { },
                            label = { Text("ယူနစ်ပေါင်း") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        CompactOutlinedTextField(
                            value = formatCurrency(report?.TotalAmountWithoutDiscount),
                            onValueChange = { },
                            label = { Text("ငွေပေါင်း") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        CompactOutlinedTextField(
                            value = formatCurrency(report?.TotalWinAmountWithoutPrize),
                            onValueChange = { },
                            label = { Text("ပေါက်သီး") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        CompactOutlinedTextField(
                            value = formatCurrency(report?.TotalAmountWithPrize),
                            onValueChange = { },
                            label = { Text("လျော်ကြေး") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        CompactOutlinedTextField(
                            value = formatCurrency(report?.SubTotalAmount),
                            onValueChange = { },
                            label = { Text("ပမာဏ") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        
        // Middle Panel - Slips List
        Card(
            modifier = Modifier
                .weight(1.8f)
                .fillMaxHeight()
                .padding(2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Slip: $total",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = "Total Unit: ${NumberFormat.getNumberInstance(Locale.US).format(slips.sumOf { it.totalAmount.toIntOrNull() ?: 0 })}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                
                // Slips List
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(slips) { index, slip ->
                        SlipCard(
                            slip = slip,
                            isSelected = selectedSlip?.slipId == slip.slipId &&
                                    selectedSlip?.termId == slip.termId &&
                                    selectedSlip?.userId == slip.userId,
                            onClick = { selectedSlip = slip },
                            onDelete = { handleDeleteSlip(slip) },
                            onEdit = { handleEditSlip(slip, "change-user") },
                            onUpdateSlip = { handleEditSlip(slip, "update-slip") }
                        )
                        
                        // Load more when reaching the end
                        if (index == slips.size - 1 && hasMore && !loading && !loadingMore) {
                            LaunchedEffect(slips.size, hasMore, loading, loadingMore) {
                                coroutineScope.launch {
                                    page++
                                    fetchSlips(resetList = false)
                                }
                            }
                        }
                    }
                    
                    if (loading || loadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (loading) "Loading slips..." else "Loading more...",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Right Panel - Slip Details
        Card(
            modifier = Modifier
                .weight(3f)
                .fillMaxHeight()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    loadingDetail && selectedSlip != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    selectedSlip == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Select a slip to view its details.")
                        }
                    }
                    slipDetails != null && !loadingDetail -> {
                        SlipDetailContent(
                            props = slipDetails!!.copy(
                                onUpdate = { item ->
                                    // Open update-slip modal with the selected item
                                    // Store the current item for update
                                    currentEditItem = item
                                    editSlipData = selectedSlip?.copy(
                                        slipId = item.number,
                                        totalAmount = item.amount.toString()
                                    )
                                    editModalType = "update-slip"
                                    showEditModal = true
                                },
                                onShowDeleteConfirmation = { index, item, onDelete ->
                                    // Show confirmation dialog and call onDelete if confirmed
                                    showDeleteConfirmation = true
                                    deleteItemIndex = index
                                    deleteItem = item
                                    deleteCallback = onDelete
                                }
                            ),
                            userProfile = UserSession.getInstance().userProfileData?.let {
                                UserProfile(
                                    businessName = it.businessName ?: "",
                                    name = it.name ?: "",
                                    userType = it.userType ?: ""
                                )
                            },
                            showSummary = true,
                            showPrintTime = true,
                            showBusinessName = true,
                            showEmployeeName = true,
                            showTermName = true
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Loading...")
                        }
                    }
                }
            }
        }
        
        // Edit Modal
        if (showEditModal && editSlipData != null) {
            // Modal overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable {
                        showEditModal = false
                        editSlipData = null
                    },
                contentAlignment = Alignment.Center
            ) {
                // Modal content
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .fillMaxHeight(0.8f)
                        .clickable { /* Prevent click through */ },
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    when (editModalType) {
                        "change-user" -> {
                            ChangeSlipUserForm(
                                agentList = userOptions.map { Agent(label = it.label, value = it.value) },
                                defaultUser = editSlipData!!.userId.toString(),
                                termId = editSlipData!!.termId.toString(),
                                slip = ChangeSlipData(
                                    slipId = editSlipData!!.slipId,
                                    userId = editSlipData!!.userId.toString()
                                ),
                                onRefreshSlip = { 
                                    showEditModal = false
                                    editSlipData = null
                                    refreshTrigger++
                                },
                                onCloseModal = {
                                    showEditModal = false
                                    editSlipData = null
                                }
                            )
                        }
                        "update-slip" -> {
                            UpdateSlipSingle(
                                initialNumber = currentEditItem?.number ?: "",
                                initialAmount = currentEditItem?.amount?.toString() ?: "",
                                onUpdate = { number, amount ->
                                    coroutineScope.launch {
                                        handleUpdateSingleSlip(number, amount)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Delete Confirmation Dialog
        if (showDeleteConfirmation && deleteItem != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteConfirmation = false
                    deleteItem = null
                    deleteCallback = null
                },
                title = { Text("Delete Confirmation") },
                text = { Text("Are you sure you want to delete this slip item?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            deleteCallback?.invoke(deleteItemIndex, deleteItem!!)
                            showDeleteConfirmation = false
                            deleteItem = null
                            deleteCallback = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFd33d33)
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmation = false
                            deleteItem = null
                            deleteCallback = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF3085d6)
                        )
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SlipCard(
    slip: SlipData,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onUpdateSlip: () -> Unit
) {
    val userProfile = UserSession.getInstance().userProfileData
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val formattedDate = try {
        val date = dateFormat.parse(slip.createdAt)
        val myanmarTimeZone = TimeZone.getTimeZone("Asia/Yangon")
        val myanmarFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        myanmarFormat.timeZone = myanmarTimeZone
        myanmarFormat.format(date ?: Date())
    } catch (e: Exception) {
        slip.createdAt
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE3F2FD) else Color.White
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, Color.Blue)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Slip ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "စလစ်နံပါတ်",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = slip.slipId,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            // Customer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ထိုးသား",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = slip.user.name + if (!slip.creator.isNullOrEmpty()) " (Host-${slip.creator})" else "",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            // Customer Note
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "မှတ်ချက်",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = slip.customer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            // Device
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Device",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = slip.deviceName ?: "",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            // Date and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                
                if (userProfile != null && 
                    ((userProfile.userType == "employee" && userProfile.userAccess == "1") || 
                     userProfile.userType == "owner")) {
                    Row {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Change User",
                                tint = Color.Blue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        IconButton(
                            onClick = onUpdateSlip,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Update Slip",
                                tint = Color.Green,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}