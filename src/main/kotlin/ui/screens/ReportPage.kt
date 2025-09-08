package ui.screens

import core.services.ApiService
import core.services.UserSession
import core.utils.LedgerRow
import core.utils.StatementSummary
import core.utils.aggregateStatementReport
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import data.models.TermData
import data.models.TermsApiResponse
import data.models.PaginationData
import data.models.TermOption
import data.models.GroupedTermOption
import ui.components.TermSelectionDialog
import ui.components.groupTermsByGroupId
import data.models.UserOption
import data.models.UserData
import data.models.UsersApiResponse
import data.models.UsersApiResponseData
import ui.components.fetchUsers
import ui.components.UserSelectionDropdown
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// Data classes

data class TermGroup(
    val label: String,
    val options: List<TermOption>
)



@Serializable
data class ReportData(
    val number: String? = null,
    val winNum: String? = null,
    val amount: Double? = null,
    val discount2D: Int? = null,
    val discount3D: Int? = null,
    val prize2D: Int? = null,
    val prize3D: Int? = null,
    val name: String? = null,
    val customer: String? = null,
    val userId: String? = null,
    val termId: String? = null,
    val termName: String? = null
)

@Serializable
data class ReportApiResponse(
    val code: String? = null,
    val data: List<ReportData>? = null,
    val message: String? = null
)

data class StatementRow(
    val customer: String,
    val userId: String,
    val termId: String,
    val termName: String,
    val SubTotalAmount: String,
    val TotalAmountWithDiscount: String,
    val TotalAmountWithPrize: String,
    val TotalAmountWithoutDiscount: String,
    val TotalTAmountWithoutPrize: String,
    val TotalUnitWithDiscount: String,
    val TotalWinAmountWithoutPrize: String,
    val discountAmount: String,
    val isTotal: Boolean = false
)

data class SubTotal(
    val discountAmount: Double = 0.0,
    val TotalAmountWithDiscount: Double = 0.0,
    val TotalAmountWithoutDiscount: Double = 0.0,
    val TotalWinAmountWithoutPrize: Double = 0.0,
    val TotalTAmountWithoutPrize: Double = 0.0,
    val TotalAmountWithTPrize: Double = 0.0,
    val TotalAmountWithPrize: Double = 0.0,
    val SubTotalAmount: Double = 0.0,
    val TotalUnitWithDiscount: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        SettlementsContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementsContent() {
    var loading by remember { mutableStateOf(false) }
    var statement by remember { mutableStateOf<List<StatementRow>>(emptyList()) }
    var subTotal by remember { mutableStateOf<SubTotal?>(null) }
    
    // API and data states
    var termOptions by remember { mutableStateOf<List<TermOption>>(emptyList()) }
    var selectedTerms by remember { mutableStateOf<List<TermOption>>(emptyList()) }
    var users by remember { mutableStateOf<List<UserOption>>(emptyList()) }
    var selectedUser by remember { mutableStateOf("") }
    var showTermDialog by remember { mutableStateOf(false) }
    var isLoadingTerms by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var agentDiscount by remember { mutableStateOf(false) }
    var toPay by remember { mutableStateOf(false) }
    var toReceive by remember { mutableStateOf(false) }
    var isDetail by remember { mutableStateOf(false) }
    
    val formatter = remember { NumberFormat.getNumberInstance(Locale.US) }
    val scope = rememberCoroutineScope()
    val apiService = remember { ApiService() }
    
    // Function to fetch terms from API
    fun fetchTerms() {
        scope.launch {
            isLoadingTerms = true
            errorMessage = null
            try {
                val userSession = UserSession.getInstance()
                val response = apiService.get<TermsApiResponse>(
                    url = "${ApiService.BASE_URL}/v1/term/getTerms?current=1&limit=1000",
                    headers = userSession.getAuthHeaders()
                )
                if (response.success && response.data != null) {
                    termOptions = response.data.data.by.filter { it.isFinished != "1" }.map { termData ->
                        TermOption(
                            termId = termData.termId,
                            termName = termData.termName,
                            shortName = termData.shortName,
                            groupId = termData.groupId,
                            startDate = termData.startDate,
                            endDate = termData.endDate,
                            isFinished = termData.isFinished,
                            termType = termData.termType,
                            winNum = termData.winNum
                        )
                    }
                } else {
                    errorMessage = response.message ?: "Failed to fetch terms"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Network error occurred"
            } finally {
                isLoadingTerms = false
            }
        }
    }
    

    
    // Function to fetch report data for selected terms
    fun fetchReportData() {
        if (selectedTerms.isEmpty()) {
            statement = emptyList()
            return
        }
        
        scope.launch {
            loading = true
            try {
                val userSession = UserSession.getInstance()
                val selectedUserObj = users.find { it.value == selectedUser }
                val selectedUserType = selectedUserObj?.userType ?: ""
                
                val termIds = selectedTerms.map { it.termId }
                val batchSize = 3
                
                // Split termIds into chunks of 3
                val chunks = termIds.chunked(batchSize)
                val allData = mutableListOf<LedgerRow>()
                
                // Process each chunk sequentially
                for (chunk in chunks) {
                    val termIdQuery = chunk.joinToString(",")
                    val url = "${ApiService.BASE_URL}/v1/report/getStatementReports?termId=${termIdQuery}&userId=${selectedUser}&userType=${selectedUserType}"
                    
                    println("[DEBUG] Fetching report data for terms: $termIdQuery")
                    
                    try {
                        val response = apiService.get<ReportApiResponse>(
                            url = url,
                            headers = userSession.getAuthHeaders()
                        )
                        
                        println("[DEBUG] Report API response: ${response.success}")
                        println("[DEBUG] Response data code: ${response.data?.code}")
                        println("[DEBUG] Response data size: ${response.data?.data?.size}")
                        
                        if (response.success && response.data?.code == "200" && response.data.data != null) {
                            // Convert API response data to LedgerRow format
                            val ledgerRows = response.data.data.map { reportData: ReportData ->
                                LedgerRow(
                                    number = reportData.number ?: "",
                                    winNum = reportData.winNum ?: "",
                                    amount = reportData.amount ?: 0.0,
                                    discount2D = reportData.discount2D ?: 0,
                                    discount3D = reportData.discount3D ?: 0,
                                    prize2D = reportData.prize2D ?: 0,
                                    prize3D = reportData.prize3D ?: 0,
                                    name = reportData.name ?: "",
                                    customer = reportData.customer ?: "",
                                    userId = reportData.userId ?: selectedUser,
                                    termId = reportData.termId ?: "",
                                    termName = reportData.termName ?: ""
                                )
                            }
                            allData.addAll(ledgerRows)
                            println("[DEBUG] Added ${ledgerRows.size} ledger rows from this batch")
                        } else {
                            println("[DEBUG] One batch failed or returned unexpected data")
                        }
                    } catch (e: Exception) {
                        println("[DEBUG] Error in batch request: ${e.message}")
                    }
                }
                
                if (allData.isNotEmpty()) {
                    // Get current user profile info
                    val currentUserObj = users.find { it.value == selectedUser }
                    val userType = "owner" // Force owner role for calculation
                    val businessName = "EMKM"
                    
                    // Calculate using LedgerMath - equivalent to aggregateStatementReport
                    println("[DEBUG] Processing ${allData.size} total ledger rows")
                    println("[DEBUG] User type: $userType, Selected user: $selectedUser, Business name: $businessName")
                    val calculatedData = aggregateStatementReport(
                        rows = allData,
                        role = userType,
                        currentUser = selectedUser,
                        name = businessName,
                        userAgentDiscount = agentDiscount
                    )
                    println("[DEBUG] Calculated data size: ${calculatedData.size}")
                    calculatedData.take(3).forEach { println("[DEBUG] Sample calculated: $it") }
                    
                    // Apply filtering based on toPay and toReceive - equivalent to addList function
                    val filteredData = when {
                        toPay && !toReceive -> calculatedData.filter { (it.SubTotalAmount.toDoubleOrNull() ?: 0.0) < 0 }
                        toReceive && !toPay -> calculatedData.filter { (it.SubTotalAmount.toDoubleOrNull() ?: 0.0) > 0 }
                        else -> calculatedData
                    }
                    
                    // Add customer totals and filter - equivalent to addCustomerTotals function
                    val groupedByCustomer = filteredData.groupBy { it.customer }
                    val resultWithTotals = mutableListOf<StatementSummary>()
                    
                    for ((customer, entries) in groupedByCustomer) {
                        resultWithTotals.addAll(entries)
                        
                        // Compute totals for this customer - format with .toFixed(2) like JavaScript
                        val totalRow = StatementSummary(
                            customer = customer,
                            userId = entries.firstOrNull()?.userId ?: "",
                            termId = "",
                            termName = "",
                            SubTotalAmount = String.format("%.2f", entries.sumOf { it.SubTotalAmount.toDoubleOrNull() ?: 0.0 }),
                            TotalAmountWithDiscount = String.format("%.2f", entries.sumOf { it.TotalAmountWithDiscount.toDoubleOrNull() ?: 0.0 }),
                            TotalAmountWithPrize = String.format("%.2f", entries.sumOf { it.TotalAmountWithPrize.toDoubleOrNull() ?: 0.0 }),
                            TotalAmountWithoutDiscount = String.format("%.2f", entries.sumOf { it.TotalAmountWithoutDiscount.toDoubleOrNull() ?: 0.0 }),
                            TotalTAmountWithoutPrize = String.format("%.2f", entries.sumOf { it.TotalTAmountWithoutPrize.toDoubleOrNull() ?: 0.0 }),
                            TotalUnitWithDiscount = String.format("%.2f", entries.sumOf { it.TotalUnitWithDiscount.toDoubleOrNull() ?: 0.0 }),
                            TotalWinAmountWithoutPrize = String.format("%.2f", entries.sumOf { it.TotalWinAmountWithoutPrize.toDoubleOrNull() ?: 0.0 }),
                            discountAmount = String.format("%.2f", entries.sumOf { it.discountAmount.toDoubleOrNull() ?: 0.0 })
                        )
                        resultWithTotals.add(totalRow)
                    }
                    
                    // Filter to show only totals and apply user-specific logic
                    val finalData = if (userType == "agent" || userType == "user") {
                        resultWithTotals.filter { it.termId.isEmpty() }.map { row: StatementSummary -> // isTotal equivalent
                            if (selectedUser == row.userId) {
                                val totalWithDiscount = row.TotalAmountWithDiscount.toDoubleOrNull() ?: 0.0
                                val totalWithPrize = row.TotalAmountWithPrize.toDoubleOrNull() ?: 0.0
                                row.copy(
                                    discountAmount = "0.00",
                                    TotalAmountWithoutDiscount = String.format("%.2f", totalWithDiscount),
                                    SubTotalAmount = String.format("%.2f", totalWithDiscount - totalWithPrize)
                                )
                            } else {
                                row
                            }
                        }
                    } else {
                        resultWithTotals.filter { it.termId.isEmpty() } // isTotal equivalent
                    }
                    
                    // Convert to StatementRow format for display
                    statement = finalData.map { summary: StatementSummary ->
                        StatementRow(
                            customer = summary.customer,
                            userId = summary.userId,
                            termId = summary.termId,
                            termName = summary.termName,
                            SubTotalAmount = summary.SubTotalAmount,
                            TotalAmountWithDiscount = summary.TotalAmountWithDiscount,
                            TotalAmountWithPrize = summary.TotalAmountWithPrize,
                            TotalAmountWithoutDiscount = summary.TotalAmountWithoutDiscount,
                            TotalTAmountWithoutPrize = summary.TotalTAmountWithoutPrize,
                            TotalUnitWithDiscount = summary.TotalUnitWithDiscount,
                            TotalWinAmountWithoutPrize = summary.TotalWinAmountWithoutPrize,
                            discountAmount = summary.discountAmount
                        )
                    }
                    
                    println("[DEBUG] Successfully processed ${statement.size} statement rows")
                } else {
                    statement = emptyList()
                }
                
            } catch (e: Exception) {
                println("[DEBUG] Error fetching report data: ${e.message}")
                e.printStackTrace()
                statement = emptyList()
            } finally {
                loading = false
            }
        }
    }
    
    // Load terms and users on first composition
    LaunchedEffect(Unit) {
        fetchTerms()
        fetchUsers(
            scope = scope,
            apiService = apiService,
            onUsersLoaded = { fetchedUsers -> users = fetchedUsers },
            onUserSelected = { /* Not used in this context */ }
        )
    }
    
    // Fetch report data when selected terms, user, or checkboxes change
    LaunchedEffect(selectedTerms, selectedUser, agentDiscount, toReceive, toPay) {
        if (selectedTerms.isNotEmpty()) {
            fetchReportData()
        }
    }
    
    // Calculate totals when statement changes
    LaunchedEffect(statement) {
        if (statement.isNotEmpty()) {
            val totals = statement.fold(SubTotal()) { acc, row ->
                SubTotal(
                    discountAmount = acc.discountAmount + (row.discountAmount.toDoubleOrNull() ?: 0.0),
                    TotalAmountWithDiscount = acc.TotalAmountWithDiscount + (row.TotalAmountWithDiscount.toDoubleOrNull() ?: 0.0),
                    TotalAmountWithoutDiscount = acc.TotalAmountWithoutDiscount + (row.TotalAmountWithoutDiscount.toDoubleOrNull() ?: 0.0),
                    TotalWinAmountWithoutPrize = acc.TotalWinAmountWithoutPrize + (row.TotalWinAmountWithoutPrize.toDoubleOrNull() ?: 0.0),
                    TotalTAmountWithoutPrize = acc.TotalTAmountWithoutPrize + (row.TotalTAmountWithoutPrize.toDoubleOrNull() ?: 0.0),
                    TotalAmountWithPrize = acc.TotalAmountWithPrize + (row.TotalAmountWithPrize.toDoubleOrNull() ?: 0.0),
                    SubTotalAmount = acc.SubTotalAmount + (row.SubTotalAmount.toDoubleOrNull() ?: 0.0),
                    TotalUnitWithDiscount = acc.TotalUnitWithDiscount + (row.TotalUnitWithDiscount.toDoubleOrNull() ?: 0.0)
                )
            }
            subTotal = totals
        } else {
            subTotal = null
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .fillMaxHeight()
            .padding(8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Controls Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Single Row with all controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Term Multi-Select
                    Column(modifier = Modifier.weight(0.3f)) {
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
                                        text = if (selectedTerms.isEmpty()) "အပါတ်စဉ်များရွေးချယ်ပါ" 
                                               else "${selectedTerms.size} terms selected",
                                        fontSize = 14.sp,
                                        color = if (selectedTerms.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant 
                                               else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isLoadingTerms) {
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
                    Column(modifier = Modifier.weight(0.2f)) {
                        UserSelectionDropdown(
                            users = users,
                            selectedUser = selectedUser,
                            onUserSelected = { selectedUser = it }
                        )
                    }
                    
                    // Checkboxes
                    Row(
                        modifier = Modifier.weight(0.5f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        CheckboxWithLabel(
                            checked = agentDiscount,
                            onCheckedChange = { agentDiscount = it },
                            label = "ကိုယ်စားလှယ်ကော်"
                        )
                        CheckboxWithLabel(
                            checked = toReceive,
                            onCheckedChange = { toReceive = it },
                            label = "ရရန်"
                        )
                        CheckboxWithLabel(
                            checked = toPay,
                            onCheckedChange = { toPay = it },
                            label = "ပေးရန်"
                        )
                        CheckboxWithLabel(
                            checked = isDetail,
                            onCheckedChange = { isDetail = it },
                            label = "Detail"
                        )
                    }
                }
            }
        }
        
        // Data Table Card
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    if (isDetail) {
                        DetailedView(
                            statement = statement,
                            formatter = formatter,
                            onShareClick = { row ->
                                showShareOptions(row, selectedTerms, formatter)
                            },
                            toPay = toPay,
                            toReceive = toReceive
                        )
                    } else {
                        SimplifiedView(
                            statement = statement,
                            formatter = formatter,
                            subTotal = subTotal,
                            onShareClick = { row ->
                                showShareOptions(row, selectedTerms, formatter)
                            },
                            toPay = toPay,
                            toReceive = toReceive
                        )
                    }
                }
            }
        }
        
        // Term Selection Dialog
        if (showTermDialog) {
            TermSelectionDialog(
                termOptions = termOptions,
                selectedTerms = selectedTerms,
                onTermsSelected = { newSelectedTerms ->
                    selectedTerms = newSelectedTerms
                    showTermDialog = false
                },
                onDismiss = { showTermDialog = false },
                isLoading = isLoadingTerms,
                errorMessage = errorMessage,
                onRetry = { fetchTerms() }
            )
        }
    }
}

@Composable
fun CheckboxWithLabel(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DetailedView(
    statement: List<StatementRow>,
    formatter: NumberFormat,
    onShareClick: (StatementRow) -> Unit,
    toPay: Boolean = false,
    toReceive: Boolean = false
) {
    val groupedStatements = statement.groupBy { it.customer }
    
    LazyColumn(
        modifier = Modifier.heightIn(max = 400.dp)
    ) {
        groupedStatements.forEach { (customer, rows) ->
            item {
                Text(
                    text = customer,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                StatementTable(
                    rows = rows,
                    formatter = formatter,
                    onShareClick = onShareClick,
                    showTermColumn = true,
                    toPay = toPay,
                    toReceive = toReceive
                )
            }
        }
    }
}

@Composable
fun SimplifiedView(
    statement: List<StatementRow>,
    formatter: NumberFormat,
    subTotal: SubTotal?,
    onShareClick: (StatementRow) -> Unit,
    toPay: Boolean = false,
    toReceive: Boolean = false
) {
    val groupedStatements = statement.groupBy { it.customer }
    
    Column(
        modifier = Modifier.fillMaxHeight()
    ) {
        Box(
            modifier = Modifier.weight(1f)
        ) {
            StatementTable(
                rows = groupedStatements.map { (customer, rows) ->
                val subtotal = rows.fold(SubTotal()) { acc, row ->
                    SubTotal(
                        discountAmount = acc.discountAmount + (row.discountAmount.toDoubleOrNull() ?: 0.0),
                        TotalAmountWithDiscount = acc.TotalAmountWithDiscount + (row.TotalAmountWithDiscount.toDoubleOrNull() ?: 0.0),
                        TotalAmountWithoutDiscount = acc.TotalAmountWithoutDiscount + (row.TotalAmountWithoutDiscount.toDoubleOrNull() ?: 0.0),
                        TotalWinAmountWithoutPrize = acc.TotalWinAmountWithoutPrize + (row.TotalWinAmountWithoutPrize.toDoubleOrNull() ?: 0.0),
                        TotalTAmountWithoutPrize = acc.TotalTAmountWithoutPrize + (row.TotalTAmountWithoutPrize.toDoubleOrNull() ?: 0.0),
                        TotalAmountWithPrize = acc.TotalAmountWithPrize + (row.TotalAmountWithPrize.toDoubleOrNull() ?: 0.0),
                        SubTotalAmount = acc.SubTotalAmount + (row.SubTotalAmount.toDoubleOrNull() ?: 0.0),
                        TotalUnitWithDiscount = acc.TotalUnitWithDiscount + (row.TotalUnitWithDiscount.toDoubleOrNull() ?: 0.0)
                    )
                }
                
                StatementRow(
                    customer = customer,
                    userId = rows.firstOrNull()?.userId ?: "",
                    termId = "",
                    termName = "",
                    SubTotalAmount = formatter.format(subtotal.SubTotalAmount),
                    TotalAmountWithDiscount = formatter.format(subtotal.TotalAmountWithDiscount),
                    TotalAmountWithPrize = formatter.format(subtotal.TotalAmountWithPrize),
                    TotalAmountWithoutDiscount = formatter.format(subtotal.TotalAmountWithoutDiscount),
                    TotalTAmountWithoutPrize = formatter.format(subtotal.TotalTAmountWithoutPrize),
                    TotalUnitWithDiscount = formatter.format(subtotal.TotalUnitWithDiscount),
                    TotalWinAmountWithoutPrize = formatter.format(subtotal.TotalWinAmountWithoutPrize),
                    discountAmount = formatter.format(subtotal.discountAmount)
                )
            },
            formatter = formatter,
            onShareClick = onShareClick,
            showTermColumn = false,
            toPay = toPay,
            toReceive = toReceive
        )
        }
        
        // Total Row
        subTotal?.let { total ->
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatter.format(total.TotalUnitWithDiscount),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatter.format(total.TotalAmountWithDiscount),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatter.format(total.discountAmount),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatter.format(total.TotalAmountWithoutDiscount),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${formatter.format(total.TotalWinAmountWithoutPrize)}(${formatter.format(total.TotalTAmountWithoutPrize)})",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatter.format(total.TotalAmountWithPrize),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                CompositionLocalProvider(
                    LocalContentColor provides when {
                        toPay -> Color(0xFFFF0000) // Red for "to pay"
                        toReceive -> Color(0xFF008000) // Green for "to receive"
                        else -> MaterialTheme.colorScheme.onSurface // Default color when no filter is applied
                    }
                ) {
                    Text(
                        text = formatter.format(total.SubTotalAmount),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatementTable(
    rows: List<StatementRow>,
    formatter: NumberFormat,
    onShareClick: (StatementRow) -> Unit,
    showTermColumn: Boolean,
    toPay: Boolean = false,
    toReceive: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ထိုးသား",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "ရောင်းကြေး(ယူနစ်)",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "ရောင်းကြေး(ငွေ)",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "ကော်",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "ကော်နှုတ်ပြီး",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "ဒဲ့(တွတ်)",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "လျော်ကြေး",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "ရ/ပေး",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
            if (showTermColumn) {
                Text(
                    text = "အပါတ်စဉ်",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Divider()
        
        // Data Rows - Scrollable
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
        
            items(rows) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = row.customer,
                        fontSize = 12.sp
                    )
                    IconButton(
                        onClick = { onShareClick(row) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = row.TotalUnitWithDiscount,
                    fontSize = 12.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = row.TotalAmountWithDiscount,
                    fontSize = 12.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = row.discountAmount,
                    fontSize = 12.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = row.TotalAmountWithoutDiscount,
                    fontSize = 12.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${row.TotalWinAmountWithoutPrize}(${row.TotalTAmountWithoutPrize})",
                    fontSize = 12.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = row.TotalAmountWithPrize,
                    fontSize = 12.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                CompositionLocalProvider(
                    LocalContentColor provides when {
                        toPay -> Color(0xFFFF0000) // Red for "to pay"
                        toReceive -> Color(0xFF008000) // Green for "to receive"
                        else -> MaterialTheme.colorScheme.onSurface // Default color when no filter is applied
                    }
                ) {
                    Text(
                        text = row.SubTotalAmount,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (showTermColumn) {
                    Text(
                        text = row.termName,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Divider()
            }
        }
    }
}

@Composable
fun WinnersContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "အလျော်စာရင်း Content",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Share functionality (simplified for Kotlin)
fun showShareOptions(
    row: StatementRow,
    selectedTerms: List<TermOption>,
    formatter: NumberFormat
) {
    val text = if (selectedTerms.size == 1) {
        val term = selectedTerms.first()
        "${row.termName}\nအမည် = ${row.customer}\nရောင်းကြေး(ယူနစ်) = ${row.TotalUnitWithDiscount}\nရောင်းကြေး(ငွေ) = ${row.TotalAmountWithDiscount}\nကော် = ${row.discountAmount}\nကော်နှုတ်ပြီး = ${row.TotalAmountWithoutDiscount}\nဒဲ့(တွတ်) = ${row.TotalWinAmountWithoutPrize}(${row.TotalTAmountWithoutPrize})\nလျော်ကြေး = ${row.TotalAmountWithPrize}\nရ/ပေး = ${formatter.format((row.SubTotalAmount.toDoubleOrNull() ?: 0.0) * -1)}"
    } else {
        val termLabels = selectedTerms.joinToString(", ") { "[${it.termName}]" }
        "${termLabels}\nရောင်းကြေး=${row.TotalAmountWithDiscount}\nကော်စုစုပေါင်း=${row.discountAmount}\nကော်နှုတ်ပြီး=${row.TotalAmountWithoutDiscount}\nလျော်ကြေး=${row.TotalAmountWithPrize}\nရ/ပေး=${formatter.format((row.SubTotalAmount.toDoubleOrNull() ?: 0.0) * -1)}"
    }
    
    // In a real implementation, you would show a dialog with share options
    println("Share text: $text")
}

// Note: Using TermData, PaginationData, TermsApiResponseData, and TermsApiResponse from TermsPage.kt