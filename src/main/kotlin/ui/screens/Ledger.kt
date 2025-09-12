package ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import core.services.ApiService
import core.services.UserSession
import core.stores.LedgerStore
import core.stores.TempListStore
import data.models.UserOption
import ui.screens.TempListItem
import core.config.CompactOutlinedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Ledger(
    termId: String,
    termName: String,
    apiBreakAmount: Int,
    is2D: Boolean,
    userList: List<UserOption>,
    onFetchActiveTerms: () -> Unit,
    onSetLedgerData: (List<LedgerItem>) -> Unit,
    onRefreshLedgerParent: () -> Unit,
    onNavigateToBuy: () -> Unit,
    onShowViewModal: (Boolean) -> Unit = {},
    onShowOptionsModal: (Boolean) -> Unit = {}
) {
    var state by remember {
        mutableStateOf(
            LedgerState(
                breakAmount = apiBreakAmount,
                tempBreakAmount = apiBreakAmount.toString(),
                is2D = is2D
            )
        )
    }
    
    val ledgerStore = remember { LedgerStore.getInstance() }
    val ledgerStoreState by ledgerStore.state.collectAsState()
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val breakAmountFocusRequester = remember { FocusRequester() }
    
    val userOptions = remember(userList) {
        listOf(UserOption("", "All")) + userList
    }
    
    var userDropdownExpanded by remember { mutableStateOf(false) }

    // Update break amount when API value changes
    LaunchedEffect(apiBreakAmount) {
        if (!state.temporaryBreakAmountCheck) {
            state = state.copy(
                breakAmount = apiBreakAmount,
                tempBreakAmount = apiBreakAmount.toString()
            )
        }
    }

    // Fetch data when dependencies change
    LaunchedEffect(termId, state.selectedPrize, state.selectedUser, state.refreshLedger) {
        if (termId.isNotEmpty()) {
            val result = ledgerStore.fetchLedgerData(
                termId = termId,
                userId = state.selectedUser,
                prize = state.selectedPrize,
                is2D = is2D
            )
            result.onSuccess { ledgerItems ->
                onSetLedgerData(ledgerItems)
                state = state.copy(ledgerData = ledgerItems)
            }.onFailure { error ->
                println("[DEBUG] LedgerStore fetchLedgerData failed: ${error.message}")
            }
        }
    }

    // Fetch prizes
    LaunchedEffect(termId, state.refreshLedger) {
        if (termId.isNotEmpty()) {
            fetchPrizes(termId, is2D) { prizes ->
                state = state.copy(prizes = prizes)
            }
        } else {
            println("[DEBUG] termId is empty, not calling fetchPrizes")
        }
    }

    // Update show data when summary mode changes
    LaunchedEffect(state.showSummary, state.ledgerRow, state.ledgerRowDetail) {
        // This would update the displayed data based on summary mode
    }

    // Keyboard shortcuts
    LaunchedEffect(Unit) {
        // Handle keyboard shortcuts (B, V, O keys)
        // This would be implemented with key event handling
    }

    fun handleChangeBreakAmount() {
        scope.launch {
            try {
                val newAmount = state.tempBreakAmount.toIntOrNull() ?: 0
                
                if (state.temporaryBreakAmountCheck) {
                    // Temporary mode - just update local state without API call
                    state = state.copy(
                        breakAmount = newAmount,
                        refreshLedger = !state.refreshLedger
                    )
                    
                    // Refresh ledger data with temporary amount using LedgerStore
                    val result = ledgerStore.fetchLedgerData(
                        termId = termId,
                        userId = state.selectedUser,
                        prize = state.selectedPrize,
                        is2D = is2D
                    )
                    result.onSuccess { ledgerItems ->
                        onSetLedgerData(ledgerItems)
                        state = state.copy(ledgerData = ledgerItems)
                    }.onFailure { error ->
                        println("[DEBUG] Failed to fetch ledger data in temporary mode: ${error.message}")
                    }
                    
                    // Focus break amount input after delay
                    delay(500)
                    breakAmountFocusRequester.requestFocus()
                } else {
                    // Permanent mode - update via API
                    state = state.copy(loading = true)
                    
                    // API call to update term
                    val apiService = ApiService()
                    val updateRequest = ApiService.UpdateTermRequest(
                        termId = termId.toString(),
                        termName = "",
                        shortName = "",
                        groupId = "",
                        startDate = "",
                        endDate = "",
                        isFinished = "",
                        termType = "",
                        winNum = "",
                        //is2D = 0,
                        unitPrice = 0,
                        breakAmount = newAmount
                    )
                    val response = apiService.updateTerm(updateRequest)
                    if (response.success) {
                        // Update successful - refresh ledger and call onFetchActiveTerms
                        state = state.copy(
                            breakAmount = newAmount,
                            loading = false,
                            refreshLedger = !state.refreshLedger
                        )
                        
                        // Refresh ledger data using LedgerStore
                        val result = ledgerStore.fetchLedgerData(
                            termId = termId,
                            userId = state.selectedUser,
                            prize = state.selectedPrize,
                            is2D = is2D
                        )
                        result.onSuccess { ledgerItems ->
                            onSetLedgerData(ledgerItems)
                            state = state.copy(ledgerData = ledgerItems)
                        }.onFailure { error ->
                            println("[DEBUG] Failed to fetch ledger data: ${error.message}")
                        }
                        
                        // Call onFetchActiveTerms
                        onFetchActiveTerms()
                        
                        // Focus break amount input after delay
                        delay(2000)
                        breakAmountFocusRequester.requestFocus()
                    } else {
                        state = state.copy(loading = false)
                        snackbarHostState.showSnackbar("Failed to update term: ${response.message ?: "Unknown error"}")
                    }
                }
            } catch (e: Exception) {
                println("[DEBUG] Exception in handleChangeBreakAmount: ${e.message}")
                e.printStackTrace()
                state = state.copy(loading = false)
                snackbarHostState.showSnackbar("Error updating break amount: ${e.message}")
            }
        }
    }

    fun handleRefreshLedger() {
        scope.launch {
            try {
                onSetLedgerData(emptyList())
                state = state.copy(
                    ledgerData = emptyList(),
                    refreshLedger = !state.refreshLedger,
                    prizes = state.prizes
                )
                onFetchActiveTerms()
                snackbarHostState.showSnackbar("Ledger refreshed successfully")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Refresh failed: ${e.message}")
            }
        }
    }

    fun handleLedgerTableClick(ledgerItem: LedgerItem) {
        scope.launch {
            try {
                // API call to get customer details
                val userSession = UserSession.getInstance()
                val apiService = ApiService()
                val response = apiService.get<CustomerApiResponseData>(
                    url = "${ApiService.BASE_URL}/v1/ledger/getCustomerByLedgerNumber?termId=${termId}&number=${ledgerItem.number}&keyword=&current=1&limit=10",
                    headers = userSession.getAuthHeaders()
                )
                
                if (response.success && response.data?.code == "200" && response.data.data?.by != null) {
                    val apiCustomers = response.data.data.by
                    
                    // Convert API response to LedgerItem format
                    val customerDetails = apiCustomers.map { customerData ->
                        LedgerItem(
                            number = customerData.number ?: ledgerItem.number,
                            totalAmount = ledgerItem.totalAmount,
                            customer = customerData.customer ?: "",
                            amount = customerData.amount ?: 0,
                            slipId = customerData.slipId ?: "",
                            createdAt = ledgerItem.createdAt
                        )
                    }
                    
                    // Sort by customer name and aggregate amounts for same customers
                    val sortedCustomers = customerDetails.sortedBy { it.customer }
                    val aggregatedCustomers = sortedCustomers.groupBy { ledgerItem: LedgerItem -> ledgerItem.customer }
                        .map { (customer: String, items: List<LedgerItem>) ->
                            val totalAmount = items.sumOf { ledgerItem: LedgerItem -> ledgerItem.amount }
                            items.first().copy(amount = totalAmount)
                        }
                    
                    state = state.copy(
                        ledgerRow = aggregatedCustomers,
                        ledgerRowDetail = customerDetails,
                        showLedgerDetail = true
                    )
                } else {
                    // No data found or API error
                    state = state.copy(
                        ledgerRow = emptyList(),
                        ledgerRowDetail = emptyList(),
                        showLedgerDetail = true
                    )
                }
            } catch (e: Exception) {
                println("[ERROR] Failed to fetch customer details: ${e.message}")
                snackbarHostState.showSnackbar("Error fetching details: ${e.message}")
                // Show empty state on error
                state = state.copy(
                    ledgerRow = emptyList(),
                    ledgerRowDetail = emptyList(),
                    showLedgerDetail = true
                )
            }
        }
    }

    fun handleBuy() {
        // Clear temp list and add extra data
        val tempListStore = TempListStore.getInstance()
        tempListStore.clearList()
        
        // Convert extraData to TempListItems and add to store
        val tempListItems = state.extraData.map { ledgerItem ->
            TempListItem(
                number = ledgerItem.number,
                amount = ledgerItem.amount.toInt().toString(),
                summary = ledgerItem.amount.toInt().toString(),
                showSummary = "1",
                groupId = (Math.random() * 1000000).toInt().toString()
            )
        }
        tempListStore.addItems(tempListItems)
        
        // Set temp list type to BUY
        tempListStore.setListType("BUY")
        
        onNavigateToBuy()
    }

    fun shareData(data: List<LedgerItem>, isTwoColumn: Boolean, isUnit: Boolean) {
        scope.launch {
            val sortedData = data.sortedByDescending { it.amount }
            val rows = sortedData.map { "${it.number}-${it.amount.toInt()}" }
            val formattedContent = if (isTwoColumn) {
                rows.chunked(2).joinToString("\n") { chunk ->
                    val row1 = chunk.getOrNull(0) ?: ""
                    val row2 = chunk.getOrNull(1) ?: ""
                    "$row1 | $row2"
                }
            } else {
                rows.joinToString("\n")
            }
            
            val totalAmount = sortedData.sumOf { it.amount }
            val sms = "$termName\n$formattedContent\nTotal - ${NumberFormat.getInstance().format(totalAmount.toInt())}"
            

            clipboardManager.setText(AnnotatedString(sms))
            
            if (isUnit) {
                state = state.copy(copiedUnit = true)
                delay(2000)
                state = state.copy(copiedUnit = false)
            } else {
                state = state.copy(copiedExtra = true)
                delay(2000)
                state = state.copy(copiedExtra = false)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Header Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Break Amount Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(0.42f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextField(
                                value = state.tempBreakAmount,
                                onValueChange = { newValue ->
                                    val filteredValue = newValue.filter { it.isDigit() }
                                    state = state.copy(tempBreakAmount = filteredValue)
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        handleChangeBreakAmount()
                                        focusManager.clearFocus()
                                    }
                                ),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .border(
                                        width = 1.dp,
                                        color = Color.Gray,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .focusRequester(breakAmountFocusRequester)
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter) {
                                            handleChangeBreakAmount()
                                            focusManager.clearFocus()
                                            true
                                        } else {
                                            false
                                        }
                                    }
                            )
                        }
                        
                        Checkbox(
                            checked = state.temporaryBreakAmountCheck,
                            onCheckedChange = { checked ->
                                state = state.copy(temporaryBreakAmountCheck = checked)
                            },
                            modifier = Modifier.weight(0.05f)
                        )
                        
                        // User Selection Dropdown
                        ExposedDropdownMenuBox(
                            expanded = userDropdownExpanded,
                            onExpandedChange = { userDropdownExpanded = it },
                            modifier = Modifier.weight(0.43f)
                        ) {
                            CompactOutlinedTextField(
                                value = userOptions.find { it.value == state.selectedUser }?.label ?: "All",
                                onValueChange = { },
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = userDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .height(56.dp)
                            )
                            
                            ExposedDropdownMenu(
                                expanded = userDropdownExpanded,
                                onDismissRequest = { userDropdownExpanded = false }
                            ) {
                                userOptions.forEach { user ->
                                    DropdownMenuItem(
                                        text = { Text(user.label) },
                                        onClick = {
                                            state = state.copy(
                                                selectedUser = user.value,
                                                selectedPrize = ""
                                            )
                                            userDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Prize Selection
                    if (state.prizes.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // "All" radio button (default selected)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = state.selectedPrize.isEmpty(),
                                            onClick = { state = state.copy(selectedPrize = "") }
                                        )
                                        Text(
                                            text = "All",
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                    
                                    // Prize radio buttons (excluding "All")
                                    state.prizes.filter { it != "All" }.forEach { prize ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = state.selectedPrize == prize,
                                                onClick = {
                                                    state = state.copy(
                                                        selectedPrize = prize,
                                                        selectedUser = ""
                                                    )
                                                }
                                            )
                                            Text(
                                                text = prize,
                                                modifier = Modifier.padding(start = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onShowViewModal(true) },
                            modifier = Modifier.weight(1f).height(36.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF006400),
                                contentColor = Color.White
                            )
                        ) {
                            Text("View")
                        }
                        
                        Button(
                            onClick = { onShowOptionsModal(true) },
                            modifier = Modifier.weight(1f).height(36.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Blue,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Option")
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFA500))
                                .clickable { handleRefreshLedger() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Button(
                            onClick = { handleBuy() },
                            modifier = Modifier.weight(1f).height(36.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("BUY")
                        }
                    }
                }
            }
            
            // Ledger Table
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 4.dp)
                    .padding(horizontal = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                if (ledgerStoreState.loading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LedgerTableComponent(
                        modifier = Modifier.weight(1f),
                        is2D = is2D,
                        ledgerData = ledgerStoreState.filteredData,
                        breakAmount = state.breakAmount,
                        handleRowClick = { ledgerItem -> handleLedgerTableClick(ledgerItem) },
                        setExtraData = { extraData: List<FormatData> ->
                            state = state.copy(extraData = extraData.map { formatData: FormatData ->
                                val amount = formatData.amount.toIntOrNull() ?: 0
                                LedgerItem(formatData.number, amount, "", amount) 
                            })
                        },
                        setUnitData = { unitData: List<FormatData> ->
                            state = state.copy(unitData = unitData.map { formatData: FormatData ->
                                val amount = formatData.amount.toIntOrNull() ?: 0
                                LedgerItem(formatData.number, amount, "", amount) 
                            })
                        },
                        toTop = "",
                        tempBreakAmount = state.tempBreakAmount.toIntOrNull() ?: 0,
                        isTempBreak = state.temporaryBreakAmountCheck
                    )
                }
                Column(
                    modifier = Modifier.padding(vertical = 2.dp,horizontal=8.dp)
                ){
                    val totalUnit = state.ledgerData.sumOf { item ->
                        val total = item.totalAmount
                        val cap = state.breakAmount
                        if (total <= 0) 0 else minOf(total, cap)
                    }
                    
                    val totalExtra = state.ledgerData.sumOf { item ->
                        val total = item.totalAmount
                        val cap = state.breakAmount
                        if (total > cap) total - cap else 0
                    }
                    
                    val percentage = if (state.breakAmount > 0) {
                        (totalUnit / state.breakAmount).toString().take(5)
                    } else "0"
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ယူနစ်ပေါင်း = ${NumberFormat.getInstance().format(totalUnit.toInt())} ($percentage%)",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Box(
                            modifier = Modifier
                                .clickable { 
                                    shareData(state.unitData, false, true) 
                                }
                                .padding(8.dp)
                                .size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource("copy_icon.svg"),
                                contentDescription = "Copy Unit Data",
                                tint = if (state.copiedUnit) Color.Green else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ကျွံသီးပေါင်း = ${NumberFormat.getInstance().format(totalExtra.toInt())}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Box(
                            modifier = Modifier
                                .clickable { 
                                    shareData(state.extraData, false, false) 
                                }
                                .padding(8.dp)
                                .size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource("copy_icon.svg"),
                                contentDescription = "Copy Extra Data",
                                tint = if (state.copiedExtra) Color.Green else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            
            // Footer Summary
            // Card(
            //     modifier = Modifier
            //         .fillMaxWidth()
            //         .padding(8.dp),
            //     colors = CardDefaults.cardColors(
            //         containerColor = MaterialTheme.colorScheme.secondaryContainer
            //     )
            // ) {
                
            // }
        }
    }
    
    // Ledger Detail Modal
    if (state.showLedgerDetail) {
        Dialog(
            onDismissRequest = { state = state.copy(showLedgerDetail = false) }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { state = state.copy(showLedgerDetail = false) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                    
                    // Summary/Detail Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.showSummary,
                                onClick = { state = state.copy(showSummary = true) }
                            )
                            Text(
                                text = "အချုပ်",
                                modifier = Modifier.padding(start = 4.dp, end = 16.dp)
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !state.showSummary,
                                onClick = { state = state.copy(showSummary = false) }
                            )
                            Text(
                                text = "အသေးစိတ်",
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                    
                    // Detail Table
                    val showData = if (state.showSummary) state.ledgerRow else state.ledgerRowDetail
                    
                    if (showData.isNotEmpty()) {
                        LazyColumn {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "ထိုးသား",
                                        modifier = Modifier.weight(1f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Number",
                                        modifier = Modifier.weight(1f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Unit",
                                        modifier = Modifier.weight(1f),
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "Slip No",
                                        modifier = Modifier.weight(1f),
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Date",
                                        modifier = Modifier.weight(1f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            itemsIndexed(showData) { index, item ->
                                val backgroundColor = if (index % 2 == 0) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(backgroundColor)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = item.customer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = item.number,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = item.amount.toInt().toString(),
                                        modifier = Modifier.weight(1f),
                                        color = if (item.amount < 0) Color.Red else Color(0xFF006400),
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = item.slipId,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = item.createdAt.format(
                                            DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a")
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No Details Available",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "There are currently no details to display.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    

}

// Helper functions
private suspend fun fetchPrizes(
    termId: String,
    is2D: Boolean,
    onPrizesUpdate: (List<String>) -> Unit
) {
    try {
        val userSession = UserSession.getInstance()
        val apiService = ApiService()
        
        val url = "/v1/ledger/getPrizeByTermId?termId=${termId}&is2D=${if (is2D) "1" else "0"}"
        val fullUrl = "${ApiService.BASE_URL}${url}"

        val response = apiService.get<PrizeApiResponseData>(
            url = fullUrl,
            headers = userSession.getAuthHeaders()
        )
 
        if (response.success && response.data?.code == "200" && response.data.data != null) {
            val prizes = listOf("All") + response.data.data
            onPrizesUpdate(prizes)
        } else {
            onPrizesUpdate(listOf("All"))
        }
    } catch (e: Exception) {
        println("[DEBUG] Exception stack trace: ${e.stackTraceToString()}")
        onPrizesUpdate(emptyList())
    }
}