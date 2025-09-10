package ui.screens

import core.services.ApiService
import core.services.UserSession
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import data.models.TermOption
import ui.components.TermSelectionDialog
import data.models.SelectionMode
import data.models.UserOption
import ui.components.UserSelectionDropdown
import data.models.UsersApiResponse
import data.models.UsersApiResponseData
import data.models.UserData
import ui.components.fetchUsers
import data.models.TermsApiResponse
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// Data classes for Winners
@Serializable
data class WinnerData(
    val slipId: String,
    val userId: String,
    val customer: String,
    val name: String,
    val amount: Double,
    val TotalAmountWithPrize: Double? = null,
    val TotalTAmountWithPrize: Double? = null,
    val prize2D: Int,
    val prize3D: Int,
    val tPrize: Int,
    val unitPrice: Double? = null,
    val createdAt: String
)

@Serializable
data class WinnerApiResponse(
    val code: String,
    val message: String,
    val data: WinnerResponseData? = null
)

@Serializable
data class WinnerResponseData(
    val winNum: List<WinnerData> = emptyList(),
    val tNum: List<WinnerData> = emptyList()
)

@Serializable
data class SubTotalData(
    val amount: Double = 0.0,
    val TotalAmountWithPrize: Double = 0.0,
    val TotalTAmountWithPrize: Double = 0.0
)

@Composable
fun WinNumContent() {
    var loading by remember { mutableStateOf(false) }
    var winners by remember { mutableStateOf<List<WinnerData>>(emptyList()) }
    var allData by remember { mutableStateOf<List<WinnerData>>(emptyList()) }
    
    // Term selection states
    var termOptions by remember { mutableStateOf<List<TermOption>>(emptyList()) }
    var selectedTerm by remember { mutableStateOf<TermOption?>(null) }
    var showTermDialog by remember { mutableStateOf(false) }
    
    // User selection states
    var userOptions by remember { mutableStateOf<List<UserOption>>(emptyList()) }
    var selectedUser by remember { mutableStateOf<UserOption?>(null) }

    var winNum by remember { mutableStateOf<String?>(null) }
    var is2D by remember { mutableStateOf(true) }
    var unitPrice by remember { mutableStateOf(1.0) }
    var prizes by remember { mutableStateOf<List<Int>>(emptyList()) }
    var selectedPrize by remember { mutableStateOf<Int?>(null) }
    var subTotal by remember { mutableStateOf<SubTotalData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    
    val scope = rememberCoroutineScope()
    val apiService = remember { ApiService() }
    val userSession = remember { UserSession.getInstance() }
    val formatter = remember { NumberFormat.getNumberInstance(Locale.US) }
    
    // Generate T Numbers function
    fun generateTNumbers(varNum: String): List<String> {
        val num = varNum.toIntOrNull() ?: return emptyList()
        var bot = (num - 1).toString().padStart(3, '0')
        var top = (num + 1).toString().padStart(3, '0')
        
        when {
            varNum == "000" -> {
                bot = "999"
                top = "001"
            }
            varNum == "999" -> {
                bot = "998"
                top = "000"
            }
            varNum.startsWith("00") -> {
                bot = (num - 1).toString().padStart(3, '0')
                top = (num + 1).toString().padStart(3, '0')
            }
            varNum.startsWith("0") -> {
                bot = (num - 1).toString().padStart(3, '0')
                top = (num + 1).toString().padStart(3, '0')
            }
        }
        
        val arr = varNum.toCharArray()
        val result = mutableListOf<String>()
        
        // Generate permutations based on digit patterns
        when {
            arr[0] == arr[1] && arr[1] == arr[2] -> {
                // All same digits
                result.add(varNum)
            }
            arr[0] == arr[1] || arr[0] == arr[2] || arr[1] == arr[2] -> {
                // Two same digits
                result.add("${arr[0]}${arr[1]}${arr[2]}")
                result.add("${arr[0]}${arr[2]}${arr[1]}")
                result.add("${arr[2]}${arr[0]}${arr[1]}")
            }
            else -> {
                // All different digits
                result.add("${arr[0]}${arr[1]}${arr[2]}")
                result.add("${arr[0]}${arr[2]}${arr[1]}")
                result.add("${arr[1]}${arr[0]}${arr[2]}")
                result.add("${arr[1]}${arr[2]}${arr[0]}")
                result.add("${arr[2]}${arr[0]}${arr[1]}")
                result.add("${arr[2]}${arr[1]}${arr[0]}")
            }
        }
        
        result.add(bot)
        result.add(top)
        
        return result.distinct().filter { it != varNum }
    }
    
    // Load terms on component mount
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val userSession = UserSession.getInstance()
                val response = apiService.get<TermsApiResponse>(
                    url = "${ApiService.BASE_URL}/v1/term/getTerms?current=1&limit=1000",
                    headers = userSession.getAuthHeaders()
                )
                if (response.success && response.data != null && response.data.code == "200") {
                    val activeTerms = response.data.data.by.filter { it.isFinished != "1" }
                    termOptions = activeTerms.map { term ->
                        TermOption(
                            termId = term.termId,
                            termName = term.termName,
                            shortName = term.shortName,
                            groupId = term.groupId ?: "",
                            startDate = term.startDate,
                            endDate = term.endDate,
                            isFinished = term.isFinished,
                            termType = term.termType,
                            winNum = term.winNum,
                            is2D = term.is2D,
                            unitPrice = term.unitPrice.toDouble()
                        )
                    }
                } else {
                    errorMessage = "Error loading terms: ${response.message ?: "Failed to fetch terms"}"
                }
            } catch (e: Exception) {
                errorMessage = "Error loading terms: ${e.message}"
            }
        }
    }
    
    // Load users on component mount
    LaunchedEffect(Unit) {
        fetchUsers(
            scope = scope,
            apiService = apiService,
            onUsersLoaded = { users ->
                userOptions = users
            },
            onUserSelected = { userId ->
                selectedUser = userOptions.find { it.value == userId }
            }
        )
    }

    // Load winners when term or user changes
    LaunchedEffect(selectedTerm, selectedUser) {
        selectedTerm?.let { term ->
            // Only proceed if winNum is not null or empty
            if (winNum.isNullOrBlank()) {
                errorMessage = "Please select a valid winning number"
                loading = false
                return@let
            }
            
            scope.launch {
                try {
                    loading = true
                    errorMessage = null // Clear previous errors
                    
                    val userId = if (userSession.userProfileData?.userType == "user") {
                        "" // For user accounts, send empty string to get all users
                    } else {
                        selectedUser?.value ?: ""
                    }
                    
                    val tNumbers = if (is2D) "" else generateTNumbers(winNum ?: "").joinToString(",")
                    
                    val apiUrl = "${ApiService.BASE_URL}/v1/report/getWinnerReports?termId=${term.termId}&winNum=${winNum}&tNum=${tNumbers}&userId=${userId}"
                    println("[DEBUG] Winner API URL: $apiUrl")
                    println("[DEBUG] Auth headers: ${userSession.getAuthHeaders()}")
                    
                    val response = apiService.get<WinnerApiResponse>(
                        url = apiUrl,
                        headers = userSession.getAuthHeaders()
                    )
                    
                    println("[DEBUG] Winner API Response - Success: ${response.success}")
                    println("[DEBUG] Winner API Response - Data: ${response.data}")
                    println("[DEBUG] Winner API Response - Message: ${response.message}")
                    println("[DEBUG] Winner API Response - Status Code: ${response.statusCode}")
                    
                    if (response.success && response.data != null && response.data.code == "200" && response.data.data != null) {
                        val responseData = response.data.data!!
                        val winResults: List<WinnerData> = responseData.winNum
                        val tResults: List<WinnerData> = responseData.tNum
                        
                        // Merge win results by slipId and userId
                        val winMap = winResults.groupBy { winner -> "${winner.userId}|${winner.slipId}" }
                            .mapValues { (_, items) ->
                                items.first().copy(amount = items.sumOf { item -> item.amount })
                            }
                        
                        // Merge t results by slipId and userId
                        val tMap = tResults.groupBy { winner -> "${winner.userId}|${winner.slipId}" }
                            .mapValues { (_, items) ->
                                items.first().copy(amount = items.sumOf { item -> item.amount })
                            }
                        
                        // Combine both maps
                        val allKeys = (winMap.keys + tMap.keys).toSet()
                        val combinedResults = allKeys.map { key: String ->
                            val win = winMap[key]
                            val t = tMap[key]
                            val base = win ?: t!!
                            
                            val totalWinAmount = win?.amount ?: 0.0
                            val totalTAmount = t?.amount ?: 0.0
                            
                            val safeUnitPrice = base.unitPrice ?: unitPrice
                            val finalAmount = if (is2D) {
                                totalWinAmount * base.prize2D * safeUnitPrice
                            } else {
                                (totalWinAmount * base.prize3D * safeUnitPrice) + 
                                (totalTAmount * base.tPrize.toDouble() * safeUnitPrice)
                            }
                            
                            base.copy(
                                amount = finalAmount,
                                TotalAmountWithPrize = totalWinAmount,
                                TotalTAmountWithPrize = totalTAmount
                            )
                        }.sortedBy { result: WinnerData -> result.slipId.toIntOrNull() ?: 0 }
                        
                        allData = combinedResults
                        winners = combinedResults
                        
                        // Extract unique prizes
                        prizes = if (is2D) {
                            combinedResults.map { result: WinnerData -> result.prize2D }.distinct()
                        } else {
                            combinedResults.map { result: WinnerData -> result.prize3D }.distinct()
                        }
                        
                        // Calculate subtotals
                        val totals = SubTotalData(
                            amount = combinedResults.sumOf { result: WinnerData -> result.amount },
                            TotalAmountWithPrize = combinedResults.sumOf { result: WinnerData -> result.TotalAmountWithPrize ?: 0.0 },
                            TotalTAmountWithPrize = combinedResults.sumOf { result: WinnerData -> result.TotalTAmountWithPrize ?: 0.0 }
                        )
                        subTotal = totals
                        
                    } else {
                        winners = emptyList()
                        allData = emptyList()
                        val errorMsg = "Error loading winners: ${response.data?.message ?: response.message ?: "Failed to load winners"}"
                        println("[DEBUG] Winner API Error: $errorMsg")
                        println("[DEBUG] Response success: ${response.success}")
                        println("[DEBUG] Response data: ${response.data}")
                        println("[DEBUG] Response data code: ${response.data?.code}")
                        errorMessage = errorMsg
                    }
                } catch (e: Exception) {
                    val errorMsg = "Error loading winners: ${e.message}"
                    println("[DEBUG] Winner API Exception: $errorMsg")
                    println("[DEBUG] Exception stack trace: ${e.stackTraceToString()}")
                    errorMessage = errorMsg
                } finally {
                    loading = false
                }
            }
        }
    }
    
    // Filter winners by selected prize
    LaunchedEffect(selectedPrize, allData) {
        winners = if (selectedPrize != null) {
            allData.filter { 
                if (is2D) it.prize2D == selectedPrize 
                else it.prize3D == selectedPrize 
            }
        } else {
            allData
        }
        
        // Recalculate subtotals for filtered data
        val totals = SubTotalData(
            amount = winners.sumOf { it.amount },
            TotalAmountWithPrize = winners.sumOf { it.TotalAmountWithPrize ?: 0.0 },
            TotalTAmountWithPrize = winners.sumOf { it.TotalTAmountWithPrize ?: 0.0 }
        )
        subTotal = totals
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                
                // Main Control Row - Term dropdown, User dropdown, Labels, and Share button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Term Selection
                    Column(modifier = Modifier.weight(1f)) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable { showTermDialog = true },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (selectedTerm == null) "အပါတ်စဉ်များရွေးချယ်ပါ"
                                               else selectedTerm?.termName ?: "Term selected",
                                        fontSize = 14.sp,
                                        color = if (selectedTerm == null) MaterialTheme.colorScheme.onSurfaceVariant
                                               else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (loading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // User Selection
                    Box(modifier = Modifier.weight(1f)) {
                        UserSelectionDropdown(
                            users = userOptions,
                            selectedUser = selectedUser?.value ?: "",
                            onUserSelected = { userId -> 
                                selectedUser = userOptions.find { it.value == userId }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Prize Filter
                    if (prizes.isNotEmpty()) {
                        Row(
                            modifier = Modifier.weight(1.5f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            
                            // All option
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedPrize == null,
                                    onClick = { selectedPrize = null },
                                    modifier = Modifier.size(12.dp)
                                )
                                Text("All", fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                                
                            }
                            
                            // Prize options
                            prizes.take(2).forEach { prize ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = selectedPrize == prize,
                                        onClick = { selectedPrize = prize },
                                        modifier = Modifier.size(16.dp).padding(start = 4.dp)
                                    )
                                    Text(
                                        text = formatter.format(prize),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Win Number Display
                    winNum?.let { num ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "ပေါက်သီး",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = num,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    // Totals Display
                    subTotal?.let { totals ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "ဒဲ့",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                            Text(
                                text = formatter.format(totals.TotalAmountWithPrize),
                                fontSize = 14.sp
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "တွတ်",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                            Text(
                                text = formatter.format(totals.TotalTAmountWithPrize),
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    // Share Button
                    IconButton(
                        onClick = {
                            // TODO: Implement print functionality
                            println("Print functionality not implemented yet")
                        },
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
        
        // Loading Indicator
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        // Error Message
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        // Winners Table
        if (winners.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "စလစ်",
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "ထိုးသား",
                            modifier = Modifier.weight(3f),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = "ပမာဏ",
                            modifier = Modifier.weight(3f),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = "ဒဲ့(တွတ်)",
                            modifier = Modifier.weight(3f),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End
                        )
                    }
                    
                    Divider()
                    
                    // Table Content
                    LazyColumn {
                        itemsIndexed(winners) { index, winner ->
                            val backgroundColor = if (index % 2 == 0) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(backgroundColor)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = winner.slipId,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (userSession.userProfileData?.userType != "owner") {
                        winner.customer.removePrefix(userSession.userProfileData?.businessName ?: "")
                    } else {
                        winner.name.removePrefix(userSession.userProfileData?.businessName ?: "")
                    },
                                    modifier = Modifier.weight(3f),
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = formatter.format(winner.amount),
                                    modifier = Modifier.weight(3f),
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = "${formatter.format(winner.TotalAmountWithPrize)} (${formatter.format(winner.TotalTAmountWithPrize)})",
                                    modifier = Modifier.weight(3f),
                                    textAlign = TextAlign.End
                                )
                            }
                            Divider()
                        }
                        
                        // Total Row
                        subTotal?.let { totals ->
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "",
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "Total",
                                        modifier = Modifier.weight(3f),
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = formatter.format(totals.amount),
                                        modifier = Modifier.weight(3f),
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "${formatter.format(totals.TotalAmountWithPrize)} (${formatter.format(totals.TotalTAmountWithPrize)})",
                                        modifier = Modifier.weight(3f),
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                    }
                }
            }
        }
    }
    
    // Term Selection Dialog
    if (showTermDialog) {
        TermSelectionDialog(
            termOptions = termOptions,
            selectedTerms = selectedTerm?.let { listOf(it) } ?: emptyList(),
            selectionMode = SelectionMode.SINGLE,
            isLoading = false,
            errorMessage = null,
            onRetry = { },
            onTermsSelected = { selectedTermsList ->
                if (selectedTermsList.isNotEmpty()) {
                    val term = selectedTermsList.first()
                    selectedTerm = term
                    winNum = term.winNum
                    is2D = term.is2D
                    unitPrice = term.unitPrice
                    selectedPrize = null // Reset prize selection when term changes
                }
                showTermDialog = false
            },
            onDismiss = { showTermDialog = false }
        )
    }
}
}