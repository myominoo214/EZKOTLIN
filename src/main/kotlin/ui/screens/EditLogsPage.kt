package ui.screens

import core.services.ApiService
import core.services.UserSession
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*
import data.models.TermOption
import data.models.TermsApiResponse
import ui.components.TermSelectionDropdown

// Data classes for Edit Logs
@Serializable
data class LedgerLogData(
    val logId: String,
    val saleDate: String,
    val last_updated_at: String,
    val actionUserName: String,
    val userName: String,
    val slipId: String,
    val action: String,
    val oldData: List<LogDetailData>,
    val newData: List<LogDetailData>
)

@Serializable
data class LogDetailData(
    val number: String,
    val summary: String,
    val amount: String,
    val discount: String? = null
)

@Serializable
data class LedgerLogsResponse(
    val code: String,
    val status: String,
    val message: String,
    val data: List<LedgerLogData>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLogsContent() {
    var loading by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf<List<LedgerLogData>>(emptyList()) }
    var selectedLogId by remember { mutableStateOf<String?>(null) }
    var oldData by remember { mutableStateOf<List<LogDetailData>>(emptyList()) }
    var newData by remember { mutableStateOf<List<LogDetailData>>(emptyList()) }
    
    // Term selection states
    var termOptions by remember { mutableStateOf<List<TermOption>>(emptyList()) }
    var selectedTerm by remember { mutableStateOf<TermOption?>(null) }
    var isLoadingTerms by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val apiService = remember { ApiService() }
    val userSession = remember { UserSession.getInstance() }
    val scope = rememberCoroutineScope()
    val dateFormatter = remember { SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()) }
    
    // Load terms on component mount
    LaunchedEffect(Unit) {
        scope.launch {
            isLoadingTerms = true
            try {
                val response = apiService.get<TermsApiResponse>(
                    "${ApiService.BASE_URL}/v1/term/getTerms?page=1&limit=1000",
                    userSession.getAuthHeaders()
                )
                
                if (response.success && response.data != null) {
                    val activeTerms = response.data.data.by.filter { term -> term.isFinished != "1" }
                    termOptions = activeTerms.map { term ->
                        TermOption(
                            termId = term.termId,
                            termName = term.termName,
                            shortName = term.shortName,
                            groupId = term.groupId,
                            startDate = term.startDate,
                            endDate = term.endDate,
                            isFinished = term.isFinished,
                            termType = term.termType,
                            winNum = term.winNum
                        )
                    }
                    
                    if (termOptions.isNotEmpty()) {
                        selectedTerm = termOptions.first()
                    }
                } else {
                    errorMessage = response.message ?: "Failed to load terms"
                }
            } catch (e: Exception) {
                errorMessage = "Error loading terms: ${e.message}"
            } finally {
                isLoadingTerms = false
            }
        }
    }
    
    // Load logs when term changes
    LaunchedEffect(selectedTerm) {
        selectedTerm?.let { term ->
            scope.launch {
                loading = true
                try {
                    val response = apiService.get<LedgerLogsResponse>(
                        "${ApiService.BASE_URL}/v1/report/getLedgerLogs?termId=${term.termId}",
                        userSession.getAuthHeaders()
                    )
                    
                    if (response.success && response.data != null) {
                        logs = response.data.data
                        // Auto-select first item if logs are available
                        if (logs.isNotEmpty()) {
                            val firstLog = logs.first()
                            selectedLogId = firstLog.logId
                            oldData = firstLog.oldData
                            newData = firstLog.newData
                        }
                    } else {
                        errorMessage = response.message ?: "Failed to load logs"
                        logs = emptyList()
                    }
                } catch (e: Exception) {
                    errorMessage = "Error loading logs: ${e.message}"
                    logs = emptyList()
                } finally {
                    loading = false
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .padding(2.dp)
    ) {
        // Logs Table Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                // Header Row with မှတ်တမ်း and Term Selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Term Selection Dropdown
                    TermSelectionDropdown(
                        termOptions = termOptions,
                        selectedTerm = selectedTerm,
                        onTermSelected = { term ->
                            selectedTerm = term
                            selectedLogId = null
                            oldData = emptyList()
                            newData = emptyList()
                        },
                        modifier = Modifier.width(300.dp),
                        label = "",
                        // placeholder = "အပါတ်စဉ်",
                        isLoading = isLoadingTerms,
                    )
                }
                
                if (loading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "မှတ်တမ်းမရှိပါ",
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                } else {
                    LazyColumn {
                        // Table Header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF3F4F6))
                                    .padding(8.dp)
                            ) {
                                TableHeaderCell("ရောင်းသည့်နေ့", 0.2f)
                                TableHeaderCell("ပြင်သည့်နေ့", 0.2f)
                                TableHeaderCell("ပြုပြင်သူ", 0.15f)
                                TableHeaderCell("ထိုးသား", 0.15f)
                                TableHeaderCell("စလစ်", 0.15f)
                                TableHeaderCell("မှတ်ချက်", 0.15f)
                            }
                        }
                        
                        // Table Rows
                        items(logs) { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedLogId = if (selectedLogId == log.logId) null else log.logId
                                        if (selectedLogId == log.logId) {
                                            oldData = log.oldData
                                            newData = log.newData
                                        } else {
                                            oldData = emptyList()
                                            newData = emptyList()
                                        }
                                    }
                                    .background(
                                        if (selectedLogId == log.logId) Color(0xFFDCFCE7) 
                                        else Color.Transparent
                                    )
                                    .padding(8.dp)
                            ) {
                                TableCell(formatDate(log.saleDate), 0.2f)
                                TableCell(formatDate(log.last_updated_at), 0.2f)
                                TableCell(log.actionUserName, 0.15f)
                                TableCell(log.userName, 0.15f)
                                TableCell(log.slipId, 0.15f)
                                TableCell(log.action, 0.15f)
                            }
                            
                            Divider(color = Color(0xFFE5E7EB), thickness = 1.dp)
                        }
                    }
                }
            }
        }
        
        // Detail Tables Row
        if (selectedLogId != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Before Edit Table
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "မပြင်ဆင်ခင်",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        DetailTable(oldData)
                    }
                }
                
                // After Edit Table
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "ပြင်ဆင်ပီး",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        DetailTable(newData)
                    }
                }
            }
        }
    }
    

}

@Composable
fun RowScope.TableHeaderCell(text: String, weight: Float) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF374151),
        modifier = Modifier.weight(weight),
        textAlign = TextAlign.Start
    )
}

@Composable
fun RowScope.TableCell(text: String, weight: Float) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = Color(0xFF111827),
        modifier = Modifier.weight(weight),
        textAlign = TextAlign.Start
    )
}

@Composable
fun DetailTable(data: List<LogDetailData>) {
    LazyColumn(
        modifier = Modifier.heightIn(max = 200.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3F4F6))
                    .padding(8.dp)
            ) {
                TableHeaderCell("Number", 0.25f)
                TableHeaderCell("Summary", 0.25f)
                TableHeaderCell("Unit", 0.25f)
                TableHeaderCell("Discount", 0.25f)
            }
        }
        
        // Data rows
        items(data) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                TableCell(item.number, 0.25f)
                TableCell(item.summary, 0.25f)
                TableCell(item.amount, 0.25f)
                TableCell(item.discount ?: "0", 0.25f)
            }
            
            Divider(color = Color(0xFFE5E7EB), thickness = 1.dp)
        }
    }
}

fun formatDate(dateString: String): String {
    return try {
        // Parse UTC timestamp and convert to local timezone
        val instant = java.time.Instant.parse(dateString)
        val zonedDateTime = instant.atZone(java.time.ZoneId.systemDefault())
        
        val outputFormatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a", java.util.Locale.ENGLISH)
        zonedDateTime.format(outputFormatter)
    } catch (e: Exception) {
        dateString
    }
}